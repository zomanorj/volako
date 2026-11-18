package mg.volako.core.data

import android.content.Context
import mg.volako.core.database.VolakoDatabase

// Fabrique des objets de la couche donnees : l'app n'a pas besoin de savoir qu'on utilise Room
object DataModule {
    fun createTransactionRepository(context: Context): TransactionRepository {
        val database = VolakoDatabase.create(context)
        return OfflineTransactionRepository(
            database.transactionDao(),
            database.pendingSmsDao(),
            database.counterpartyLabelDao(),
            database.smsTemplateDao(),
        )
    }

    fun createSmsImporter(
        context: Context,
        repository: TransactionRepository,
        preferences: UserPreferences,
    ) = SmsImporter(SmsInboxReader(context), repository, preferences)
}
