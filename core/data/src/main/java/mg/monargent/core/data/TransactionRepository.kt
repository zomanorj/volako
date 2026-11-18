package mg.monargent.core.data

import kotlinx.coroutines.flow.Flow
import mg.monargent.core.model.Balances
import mg.monargent.core.model.MonthSummary
import mg.monargent.core.model.PendingSms
import mg.monargent.core.model.Transaction
import java.time.LocalDateTime
import java.time.YearMonth

// Tout ce que l'app peut faire avec les donnees. Les ecrans ne parlent qu'a cette interface,
// comme ca on pourra changer le stockage (ou ajouter une synchro) sans toucher a l'UI.
interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeBalances(): Flow<Balances>
    fun observeMonthSummary(month: YearMonth): Flow<MonthSummary>
    fun observePendingSms(): Flow<List<PendingSms>>

    // un seul SMS (recu en direct)
    suspend fun recordSms(body: String, receivedAt: LocalDateTime): RecordedSms

    // import en lot (historique) : bien plus rapide que recordSms un par un
    suspend fun importSms(messages: List<RawSms>): ImportReport

    suspend fun addManual(transaction: Transaction)
    suspend fun delete(id: Long)
    suspend fun dismissPendingSms(id: Long)
}
