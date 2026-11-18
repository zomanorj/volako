package mg.monargent.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mg.monargent.core.database.dao.CounterpartyLabelDao
import mg.monargent.core.database.dao.PendingSmsDao
import mg.monargent.core.database.dao.SmsTemplateDao
import mg.monargent.core.database.dao.TransactionDao
import mg.monargent.core.database.entity.CounterpartyLabelEntity
import mg.monargent.core.database.entity.PendingSmsEntity
import mg.monargent.core.database.entity.SmsTemplateEntity
import mg.monargent.core.database.entity.TransactionEntity

// Base locale. Quand on change une table : version + 1 et une migration (schemas/ dans git)
@Database(
    entities = [
        TransactionEntity::class,
        PendingSmsEntity::class,
        CounterpartyLabelEntity::class,
        SmsTemplateEntity::class,
    ],
    version = 3,
    exportSchema = true,
    // Room genere les migrations tout seul :
    // v2 : table counterparty_labels
    // v3 : table sms_templates + colonne transactions.read_by
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class MonArgentDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingSmsDao(): PendingSmsDao
    abstract fun counterpartyLabelDao(): CounterpartyLabelDao
    abstract fun smsTemplateDao(): SmsTemplateDao

    companion object {
        fun create(context: Context): MonArgentDatabase =
            Room.databaseBuilder(context, MonArgentDatabase::class.java, "mon_argent.db").build()
    }
}
