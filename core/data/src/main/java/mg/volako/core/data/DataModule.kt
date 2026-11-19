package mg.volako.core.data

import android.content.Context
import mg.volako.core.database.VolakoDatabase

// Fabrique des objets de la couche donnees : l'app n'a pas besoin de savoir qu'on utilise Room
object DataModule {

    // une seule base pour toute l'app (conseil de la doc Room : ouvrir une base coute cher)
    @Volatile
    private var database: VolakoDatabase? = null

    private fun database(context: Context): VolakoDatabase =
        database ?: synchronized(this) {
            database ?: VolakoDatabase.create(context.applicationContext).also { database = it }
        }

    fun createTransactionRepository(context: Context): TransactionRepository {
        val db = database(context)
        return OfflineTransactionRepository(
            db.transactionDao(),
            db.pendingSmsDao(),
            db.counterpartyLabelDao(),
            db.smsTemplateDao(),
        )
    }

    fun createFavoriteRepository(context: Context): FavoriteRepository =
        OfflineFavoriteRepository(database(context).favoriteDao())

    fun createSmsImporter(
        context: Context,
        repository: TransactionRepository,
        preferences: UserPreferences,
    ) = SmsImporter(SmsInboxReader(context), repository, preferences)
}
