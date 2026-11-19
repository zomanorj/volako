package mg.volako.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mg.volako.core.database.dao.CounterpartyLabelDao
import mg.volako.core.database.dao.FavoriteDao
import mg.volako.core.database.dao.PendingSmsDao
import mg.volako.core.database.dao.SmsTemplateDao
import mg.volako.core.database.dao.TransactionDao
import mg.volako.core.database.entity.CounterpartyLabelEntity
import mg.volako.core.database.entity.FavoriteEntity
import mg.volako.core.database.entity.PendingSmsEntity
import mg.volako.core.database.entity.SmsTemplateEntity
import mg.volako.core.database.entity.TransactionEntity

// Base locale. Quand on change une table : version + 1 et une migration (schemas/ dans git)
@Database(
    entities = [
        TransactionEntity::class,
        PendingSmsEntity::class,
        CounterpartyLabelEntity::class,
        SmsTemplateEntity::class,
        FavoriteEntity::class,
    ],
    version = 4,
    exportSchema = true,
    // Room genere les migrations tout seul :
    // v2 : table counterparty_labels
    // v3 : table sms_templates + colonne transactions.read_by
    // v4 : table ussd_favorites (onglet MVola)
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class VolakoDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingSmsDao(): PendingSmsDao
    abstract fun counterpartyLabelDao(): CounterpartyLabelDao
    abstract fun smsTemplateDao(): SmsTemplateDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        fun create(context: Context): VolakoDatabase =
            Room.databaseBuilder(context, VolakoDatabase::class.java, "volako.db").build()
    }
}
