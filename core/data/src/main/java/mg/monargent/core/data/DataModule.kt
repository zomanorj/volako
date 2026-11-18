package mg.monargent.core.data

import android.content.Context
import mg.monargent.core.database.MonArgentDatabase

// Fabrique du depot : l'app n'a pas besoin de savoir qu'on utilise Room
object DataModule {
    fun createTransactionRepository(context: Context): TransactionRepository {
        val database = MonArgentDatabase.create(context)
        return OfflineTransactionRepository(database.transactionDao(), database.pendingSmsDao())
    }
}
