package mg.monargent.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mg.monargent.core.database.dao.PendingSmsDao
import mg.monargent.core.database.dao.TransactionDao
import mg.monargent.core.database.entity.asEntity
import mg.monargent.core.database.entity.asModel
import mg.monargent.core.domain.Ledger
import mg.monargent.core.domain.SmsTransactionMapper
import mg.monargent.core.model.Balances
import mg.monargent.core.model.MonthSummary
import mg.monargent.core.model.PendingSms
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionSource
import mg.monargent.core.sms.MvolaSmsParser
import mg.monargent.core.sms.ParsedSms
import java.time.LocalDateTime
import java.time.YearMonth

// Version 100% locale (tout est sur le telephone)
class OfflineTransactionRepository(
    private val transactionDao: TransactionDao,
    private val pendingSmsDao: PendingSmsDao,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.asModel() } }

    override fun observeBalances(): Flow<Balances> =
        observeTransactions().map(Ledger::balances)

    override fun observeMonthSummary(month: YearMonth): Flow<MonthSummary> =
        observeTransactions().map { Ledger.monthSummary(it, month) }

    override fun observePendingSms(): Flow<List<PendingSms>> =
        pendingSmsDao.observeAll().map { list -> list.map { it.asModel() } }

    override suspend fun recordSms(body: String, receivedAt: LocalDateTime): SmsRecordResult =
        when (val parsed = MvolaSmsParser.parse(body, receivedAt)) {
            ParsedSms.Ignored -> SmsRecordResult.IGNORED
            is ParsedSms.Unknown -> {
                pendingSmsDao.insert(PendingSms(body = body, receivedAt = receivedAt).asEntity())
                SmsRecordResult.PENDING
            }
            is ParsedSms.Operation -> {
                if (transactionDao.existsByReference(parsed.reference)) {
                    SmsRecordResult.DUPLICATE
                } else {
                    // solde du SMS d'avant, pour la regle des frais non preleves
                    val previous = transactionDao.lastSms()?.smsBalance
                    val transaction = SmsTransactionMapper.map(parsed, previous)
                    val id = transactionDao.insert(transaction.asEntity())
                    if (id == -1L) SmsRecordResult.DUPLICATE else SmsRecordResult.ADDED
                }
            }
        }

    override suspend fun addManual(transaction: Transaction) {
        transactionDao.insert(transaction.copy(id = 0, source = TransactionSource.MANUAL).asEntity())
    }

    override suspend fun delete(id: Long) = transactionDao.delete(id)

    override suspend fun dismissPendingSms(id: Long) = pendingSmsDao.delete(id)
}
