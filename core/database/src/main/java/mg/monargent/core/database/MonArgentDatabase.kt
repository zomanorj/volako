package mg.monargent.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mg.monargent.core.database.dao.CounterpartyLabelDao
import mg.monargent.core.database.dao.PendingSmsDao
import mg.monargent.core.database.dao.TransactionDao
import mg.monargent.core.database.entity.CounterpartyLabelEntity
import mg.monargent.core.database.entity.PendingSmsEntity
import mg.monargent.core.database.entity.TransactionEntity

// Base locale. Quand on change une table : version + 1 et une migration (schemas/ dans git)
@Database(
    entities = [TransactionEntity::class, PendingSmsEntity::class, CounterpartyLabelEntity::class],
    version = 2,
    exportSchema = true,
    // v2 : ajout de la table counterparty_labels (Room genere la migration tout seul)
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class MonArgentDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingSmsDao(): PendingSmsDao
    abstract fun counterpartyLabelDao(): CounterpartyLabelDao

    companion object {
        fun create(context: Context): MonArgentDatabase =
            Room.databaseBuilder(context, MonArgentDatabase::class.java, "mon_argent.db").build()
    }
}
