package mg.volako.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mg.volako.core.database.dao.CounterpartyLabelDao
import mg.volako.core.database.dao.PendingSmsDao
import mg.volako.core.database.dao.SmsTemplateDao
import mg.volako.core.database.entity.CounterpartyLabelEntity
import mg.volako.core.database.dao.TransactionDao
import mg.volako.core.database.entity.asEntity
import mg.volako.core.database.entity.asModel
import mg.volako.core.database.entity.toEpochMillis
import mg.volako.core.database.entity.toLocalDateTime
import mg.volako.core.domain.Ledger
import mg.volako.core.domain.SmartSmsInterpreter
import mg.volako.core.domain.SmsTransactionMapper
import mg.volako.core.model.Account
import mg.volako.core.model.Balances
import mg.volako.core.model.Category
import mg.volako.core.model.MonthSummary
import mg.volako.core.model.PendingSms
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionSource
import mg.volako.core.model.TransactionType
import mg.volako.core.sms.GenericSmsReader
import mg.volako.core.sms.MvolaSmsParser
import mg.volako.core.sms.OperationKind
import mg.volako.core.sms.SmsTemplates
import mg.volako.core.sms.ParsedSms
import java.time.LocalDateTime
import java.time.YearMonth

// Version 100% locale (tout est sur le telephone)
class OfflineTransactionRepository(
    private val transactionDao: TransactionDao,
    private val pendingSmsDao: PendingSmsDao,
    private val counterpartyLabelDao: CounterpartyLabelDao,
    private val smsTemplateDao: SmsTemplateDao,
) : TransactionRepository {

    private suspend fun templates() = smsTemplateDao.getAll().map { it.asModel() }

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.asModel() } }

    override fun observeBalances(): Flow<Balances> =
        observeTransactions().map(Ledger::balances)

    override fun observeMonthSummary(month: YearMonth): Flow<MonthSummary> =
        observeTransactions().map { Ledger.monthSummary(it, month) }

    override fun observePendingSms(): Flow<List<PendingSms>> =
        pendingSmsDao.observeAll().map { list -> list.map { it.asModel() } }

    override suspend fun recordSms(body: String, receivedAt: LocalDateTime): RecordedSms {
        // solde du dernier SMS avant celui-ci : sert a verifier un format inconnu
        val previousAtReception = transactionDao.lastSmsBefore(receivedAt.toEpochMillis())?.smsBalance
        return when (val parsed = SmartSmsInterpreter.interpret(body, receivedAt, previousAtReception, templates())) {
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
    }

    override suspend fun importSms(messages: List<RawSms>): ImportReport {
        val operations = mutableListOf<ParsedSms.Operation>()
        val unknown = mutableListOf<RawSms>()
        var ignored = 0
        for (sms in messages) {
            when (val parsed = MvolaSmsParser.parse(sms.body, sms.receivedAt)) {
                is ParsedSms.Operation -> operations += parsed
                is ParsedSms.Unknown -> unknown += sms
                // ignore par les regles exactes, mais il a un montant en Ar : peut-etre
                // un nouveau format (sans "Ref" par exemple), on laisse le lecteur universel essayer
                ParsedSms.Ignored -> if (GenericSmsReader.read(sms.body) != null) unknown += sms else ignored++
            }
        }

        // les SMS inconnus : on essaie le lecteur universel, verifie par le solde du SMS d'avant.
        // On les traite dans l'ordre pour que deux nouveaux SMS a la suite se verifient entre eux.
        val stillUnknown = mutableListOf<RawSms>()
        val templates = templates()
        for (sms in unknown.sortedBy { it.receivedAt }) {
            val previous = operations.filter { !it.occurredAt.isAfter(sms.receivedAt) }.maxByOrNull { it.occurredAt }
                ?.balanceAfter
                ?: transactionDao.lastSmsBefore(sms.receivedAt.toEpochMillis())?.smsBalance
            when (val parsed = SmartSmsInterpreter.interpret(sms.body, sms.receivedAt, previous, templates)) {
                is ParsedSms.Operation -> operations += parsed
                is ParsedSms.Unknown -> stillUnknown += sms
                ParsedSms.Ignored -> ignored++
            }
        }
        unknown.clear()
        unknown += stillUnknown

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
        val templates = templates()
        // du plus ancien au plus recent, pour que les soldes s'enchainent
        for (pending in pendingSmsDao.getAll().sortedBy { it.receivedAt }) {
            val receivedAt = pending.receivedAt.toLocalDateTime()
            val previous = transactionDao.lastSmsBefore(pending.receivedAt)?.smsBalance
            when (SmartSmsInterpreter.interpret(pending.body, receivedAt, previous, templates)) {
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

    override suspend fun classifyPendingSms(pendingId: Long, choice: ClassifyChoice, amount: Long, label: String) {
        val pending = pendingSmsDao.getById(pendingId) ?: return
        val receivedAt = pending.receivedAt.toLocalDateTime()
        val previous = transactionDao.lastSmsBefore(pending.receivedAt)?.smsBalance
        val kind = kindFor(choice, pending.body)

        // 1. on apprend la forme de ce SMS
        val template = SmsTemplates.learn(pending.body, kind, amount, label, previous)
        if (template != null) smsTemplateDao.insert(template.asEntity(System.currentTimeMillis()))

        // 2. on le lit avec son modele : on garde ainsi la ref et le solde du SMS
        val op = template?.let { SmsTemplates.match(it, pending.body, receivedAt) }?.takeIf { it.amount == amount }
        if (op != null) {
            transactionDao.insert(SmsTransactionMapper.map(op, previous).asEntity())
        } else {
            // le montant tape n'est pas dans le SMS : simple saisie a la main sur MVola
            transactionDao.insert(manualFromChoice(choice, amount, label, receivedAt).asEntity())
        }
        pendingSmsDao.delete(pendingId)

        // 3. les autres SMS "a verifier" de la meme forme se rangent tout seuls
        reprocessPendingSms()
    }

    // le choix de l'utilisateur + les mots du SMS -> type precis (credit, depot...)
    private fun kindFor(choice: ClassifyChoice, body: String): OperationKind {
        val hint = GenericSmsReader.read(body)?.hint
        return when (choice) {
            ClassifyChoice.WITHDRAWAL -> OperationKind.WITHDRAWAL
            ClassifyChoice.INCOME -> if (hint == OperationKind.DEPOSIT) OperationKind.DEPOSIT else OperationKind.RECEIVED
            ClassifyChoice.EXPENSE -> if (hint == OperationKind.CREDIT_PURCHASE) OperationKind.CREDIT_PURCHASE else OperationKind.SENT
        }
    }

    private fun manualFromChoice(choice: ClassifyChoice, amount: Long, label: String, at: LocalDateTime) = Transaction(
        type = when (choice) {
            ClassifyChoice.EXPENSE -> TransactionType.EXPENSE
            ClassifyChoice.INCOME -> TransactionType.INCOME
            ClassifyChoice.WITHDRAWAL -> TransactionType.TRANSFER
        },
        amount = amount,
        account = Account.MVOLA,
        toAccount = if (choice == ClassifyChoice.WITHDRAWAL) Account.CASH else null,
        category = when (choice) {
            ClassifyChoice.INCOME -> Category.INCOME
            ClassifyChoice.WITHDRAWAL -> Category.TRANSFER
            ClassifyChoice.EXPENSE -> Category.OTHER
        },
        label = label.trim().ifEmpty { null },
        // a la date du SMS : le solde MVola reste celui du dernier SMS reconnu
        occurredAt = at,
        source = TransactionSource.MANUAL,
    )

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
