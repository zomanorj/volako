package mg.monargent.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mg.monargent.core.database.dao.PendingSmsDao
import mg.monargent.core.database.dao.TransactionDao
import mg.monargent.core.database.entity.PendingSmsEntity
import mg.monargent.core.database.entity.TransactionEntity

// Base locale. Quand on change une table : version + 1 et une migration (schemas/ dans git)
@Database(
    entities = [TransactionEntity::class, PendingSmsEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MonArgentDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingSmsDao(): PendingSmsDao

    companion object {
        fun create(context: Context): MonArgentDatabase =
            Room.databaseBuilder(context, MonArgentDatabase::class.java, "mon_argent.db").build()
    }
}
