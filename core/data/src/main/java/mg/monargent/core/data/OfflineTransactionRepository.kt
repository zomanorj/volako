package mg.monargent.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mg.monargent.core.database.dao.CounterpartyLabelDao
import mg.monargent.core.database.dao.PendingSmsDao
import mg.monargent.core.database.entity.CounterpartyLabelEntity
import mg.monargent.core.database.dao.TransactionDao
import mg.monargent.core.database.entity.asEntity
import mg.monargent.core.database.entity.asModel
import mg.monargent.core.database.entity.toEpochMillis
import mg.monargent.core.database.entity.toLocalDateTime
import mg.monargent.core.domain.Ledger
import mg.monargent.core.domain.SmsTransactionMapper
import mg.monargent.core.model.Account
import mg.monargent.core.model.Balances
import mg.monargent.core.model.Category
import mg.monargent.core.model.MonthSummary
import mg.monargent.core.model.PendingSms
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionSource
import mg.monargent.core.model.TransactionType
import mg.monargent.core.sms.MvolaSmsParser
import mg.monargent.core.sms.ParsedSms
import java.time.LocalDateTime
import java.time.YearMonth

// Version 100% locale (tout est sur le telephone)
class OfflineTransactionRepository(
    private val transactionDao: TransactionDao,
    private val pendingSmsDao: PendingSmsDao,
    private val counterpartyLabelDao: CounterpartyLabelDao,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.asModel() } }

    override fun observeBalances(): Flow<Balances> =
        observeTransactions().map(Ledger::balances)

    override fun observeMonthSummary(month: YearMonth): Flow<MonthSummary> =
        observeTransactions().map { Ledger.monthSummary(it, month) }

    override fun observePendingSms(): Flow<List<PendingSms>> =
        pendingSmsDao.observeAll().map { list -> list.map { it.asModel() } }

    override suspend fun recordSms(body: String, receivedAt: LocalDateTime): RecordedSms =
        when (val parsed = MvolaSmsParser.parse(body, receivedAt)) {
            ParsedSms.Ignored -> RecordedSms(SmsRecordResult.IGNORED)
            is ParsedSms.Unknown -> {
                if (!pendingSmsDao.existsByBody(body)) {
                    pendingSmsDao.insert(PendingSms(body = body, receivedAt = receivedAt).asEntity())
                }
                RecordedSms(SmsRecordResult.PENDING)
            }
            is ParsedSms.Operation -> {
                if (transactionDao.existsByReference(parsed.reference)) {
                    RecordedSms(SmsRecordResult.DUPLICATE)
                } else {
                    // solde du SMS d'avant, pour la regle des frais non preleves
                    val previous = transactionDao.lastSmsBefore(parsed.occurredAt.toEpochMillis())?.smsBalance
                    val transaction = withRememberedLabel(SmsTransactionMapper.map(parsed, previous))
                    val id = transactionDao.insert(transaction.asEntity())
                    if (id == -1L) {
                        RecordedSms(SmsRecordResult.DUPLICATE)
                    } else {
                        RecordedSms(SmsRecordResult.ADDED, transaction.copy(id = id))
                    }
                }
            }
        }

    override suspend fun importSms(messages: List<RawSms>): ImportReport {
        val operations = mutableListOf<ParsedSms.Operation>()
        val unknown = mutableListOf<RawSms>()
        var ignored = 0
        for (sms in messages) {
            when (val parsed = MvolaSmsParser.parse(sms.body, sms.receivedAt)) {
                is ParsedSms.Operation -> operations += parsed
                is ParsedSms.Unknown -> unknown += sms
                ParsedSms.Ignored -> ignored++
            }
        }

        // ordre chronologique pour enchainer les soldes (meme tri que Ledger)
        operations.sortWith(compareBy({ it.occurredAt }, { it.reference.toLongOrNull() ?: Long.MAX_VALUE }))
        var previous = operations.firstOrNull()?.let {
            transactionDao.lastSmsBefore(it.occurredAt.toEpochMillis())?.smsBalance
        }
        val rules = counterpartyLabelDao.getAll().associate { it.phone to it.label }
        val entities = operations.map { op ->
            val transaction = SmsTransactionMapper.map(op, previous).also { previous = op.balanceAfter }
            withRememberedLabel(transaction, rules).asEntity()
        }

        val ids = transactionDao.insertAll(entities)
        val newUnknown = unknown.distinctBy { it.body }.filterNot { pendingSmsDao.existsByBody(it.body) }
        pendingSmsDao.insertAll(newUnknown.map { PendingSms(body = it.body, receivedAt = it.receivedAt).asEntity() })

        val added = ids.count { it != -1L }
        return ImportReport(
            added = added,
            duplicates = ids.size - added,
            pending = unknown.size,
            ignored = ignored,
        )
    }

    override fun observeFrequentLabels(type: TransactionType, limit: Int): Flow<List<String>> =
        transactionDao.observeFrequentLabels(type, limit)

    override suspend fun reprocessPendingSms(): Int {
        var resolved = 0
        for (pending in pendingSmsDao.getAll()) {
            val receivedAt = pending.receivedAt.toLocalDateTime()
            when (MvolaSmsParser.parse(pending.body, receivedAt)) {
                is ParsedSms.Unknown -> Unit // toujours inconnu, on le laisse
                else -> {
                    // maintenant reconnu (ou ignore) : on l'enregistre et on le retire de "A verifier"
                    pendingSmsDao.delete(pending.id)
                    if (recordSms(pending.body, receivedAt).result == SmsRecordResult.ADDED) resolved++
                }
            }
        }
        return resolved
    }

    override suspend fun addManual(transaction: Transaction): Long =
        transactionDao.insert(transaction.copy(id = 0, source = TransactionSource.MANUAL).asEntity())

    override suspend fun update(transaction: Transaction) = transactionDao.update(transaction.asEntity())

    override suspend fun restore(transaction: Transaction) {
        transactionDao.insert(transaction.asEntity())
    }

    override suspend fun rememberCounterpartyLabel(phone: String, label: String) {
        counterpartyLabelDao.upsert(CounterpartyLabelEntity(phone, label.trim()))
        transactionDao.relabelByPhone(phone, label.trim())
    }

    override suspend fun classifyPendingSms(pendingId: Long, type: TransactionType, amount: Long, label: String) {
        val pending = pendingSmsDao.getById(pendingId) ?: return
        val transaction = Transaction(
            type = type,
            amount = amount,
            account = Account.MVOLA,
            category = if (type == TransactionType.INCOME) Category.INCOME else Category.OTHER,
            label = label.trim().ifEmpty { null },
            // a la date du SMS : le solde MVola reste celui du dernier SMS reconnu
            occurredAt = pending.receivedAt.toLocalDateTime(),
            source = TransactionSource.MANUAL,
        )
        transactionDao.insert(transaction.asEntity())
        pendingSmsDao.delete(pendingId)
    }

    // si l'utilisateur a deja nomme ce numero, on reprend son libelle
    private suspend fun withRememberedLabel(
        transaction: Transaction,
        rules: Map<String, String>? = null,
    ): Transaction {
        val phone = transaction.counterpartyPhone ?: return transaction
        if (transaction.type == TransactionType.TRANSFER || transaction.category == Category.CREDIT) return transaction
        val label = rules?.get(phone) ?: counterpartyLabelDao.labelFor(phone) ?: return transaction
        return transaction.copy(label = label)
    }

    override suspend fun delete(id: Long) = transactionDao.delete(id)

    override suspend fun dismissPendingSms(id: Long) = pendingSmsDao.delete(id)
}
