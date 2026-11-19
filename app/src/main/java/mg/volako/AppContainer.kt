package mg.volako

import android.content.Context
import mg.volako.core.data.CsvFileExporter
import mg.volako.core.data.DataModule
import mg.volako.core.data.FavoriteRepository
import mg.volako.core.data.SmsImporter
import mg.volako.core.data.TransactionRepository
import mg.volako.core.data.UserPreferences

// Injection de dependances "a la main" (guide officiel Android : manual DI).
// Suffit pour l'instant, on pourra passer a Hilt si l'app grossit beaucoup.
class AppContainer(context: Context) {
    val preferences by lazy { UserPreferences(context) }

    val transactionRepository: TransactionRepository by lazy {
        DataModule.createTransactionRepository(context)
    }

    val favoriteRepository: FavoriteRepository by lazy { DataModule.createFavoriteRepository(context) }

    val csvExporter by lazy { CsvFileExporter(context, transactionRepository) }

    val smsImporter: SmsImporter by lazy {
        DataModule.createSmsImporter(context, transactionRepository, preferences)
    }
}
