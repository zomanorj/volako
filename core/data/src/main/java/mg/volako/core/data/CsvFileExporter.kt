package mg.volako.core.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import mg.volako.core.domain.CsvExport

// Ecrit toutes les operations dans le fichier choisi par l'utilisateur (Storage Access Framework :
// pas besoin de permission de stockage)
class CsvFileExporter(
    private val context: Context,
    private val repository: TransactionRepository,
) {
    // retourne le nombre d'operations exportees
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val transactions = repository.observeTransactions().first()
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("fichier impossible a ouvrir")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(CsvExport.toCsv(transactions)) }
        transactions.size
    }
}
