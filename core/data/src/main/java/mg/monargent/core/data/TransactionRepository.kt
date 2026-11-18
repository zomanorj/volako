package mg.monargent.core.data

import kotlinx.coroutines.flow.Flow
import mg.monargent.core.model.Balances
import mg.monargent.core.model.MonthSummary
import mg.monargent.core.model.PendingSms
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionType
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

    // libelles frequents des saisies a la main (depense ou entree)
    fun observeFrequentLabels(type: TransactionType, limit: Int): Flow<List<String>>

    // retourne l'id, pour pouvoir annuler
    // relit les SMS "a verifier" avec le lecteur actuel (apres l'ajout d'un nouveau format)
    suspend fun reprocessPendingSms(): Int

    suspend fun addManual(transaction: Transaction): Long
    // modifier une operation (libelle, montant d'une saisie manuelle...)
    suspend fun update(transaction: Transaction)

    // remettre une operation supprimee (bouton "Annuler")
    suspend fun restore(transaction: Transaction)

    // retenir un libelle pour un numero et l'appliquer a toutes ses operations
    suspend fun rememberCounterpartyLabel(phone: String, label: String)

    // classer un SMS "a verifier" : l'app apprend sa forme pour lire seule les suivants
    suspend fun classifyPendingSms(pendingId: Long, choice: ClassifyChoice, amount: Long, label: String)

    suspend fun delete(id: Long)
    suspend fun dismissPendingSms(id: Long)
}
