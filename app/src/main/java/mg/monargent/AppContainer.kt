package mg.monargent

import android.content.Context
import mg.monargent.core.data.DataModule
import mg.monargent.core.data.SmsImporter
import mg.monargent.core.data.TransactionRepository
import mg.monargent.core.data.UserPreferences

// Injection de dependances "a la main" (guide officiel Android : manual DI).
// Suffit pour l'instant, on pourra passer a Hilt si l'app grossit beaucoup.
class AppContainer(context: Context) {
    val preferences by lazy { UserPreferences(context) }

    val transactionRepository: TransactionRepository by lazy {
        DataModule.createTransactionRepository(context)
    }

    val smsImporter: SmsImporter by lazy {
        DataModule.createSmsImporter(context, transactionRepository, preferences)
    }
}
