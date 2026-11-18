package mg.monargent

import android.content.Context
import mg.monargent.core.data.DataModule
import mg.monargent.core.data.TransactionRepository

// Injection de dependances "a la main" (guide officiel Android : manual DI).
// Suffit pour l'instant, on pourra passer a Hilt si l'app grossit beaucoup.
class AppContainer(context: Context) {
    val transactionRepository: TransactionRepository by lazy {
        DataModule.createTransactionRepository(context)
    }
}
