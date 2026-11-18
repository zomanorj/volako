package mg.monargent.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mg.monargent.core.database.entity.PendingSmsEntity

@Dao
interface PendingSmsDao {

    @Query("SELECT * FROM pending_sms ORDER BY received_at DESC")
    fun observeAll(): Flow<List<PendingSmsEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sms: PendingSmsEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sms: List<PendingSmsEntity>): List<Long>

    // le meme SMS peut arriver par le recepteur ET par l'import (heures un peu differentes)
    @Query("SELECT EXISTS(SELECT 1 FROM pending_sms WHERE body = :body)")
    suspend fun existsByBody(body: String): Boolean

    @Query("SELECT * FROM pending_sms WHERE id = :id")
    suspend fun getById(id: Long): PendingSmsEntity?

    @Query("DELETE FROM pending_sms WHERE id = :id")
    suspend fun delete(id: Long)
}
