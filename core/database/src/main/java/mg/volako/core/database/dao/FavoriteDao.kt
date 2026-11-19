package mg.volako.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mg.volako.core.database.entity.FavoriteEntity

@Dao
interface FavoriteDao {

    // les plus recents d'abord
    @Query("SELECT * FROM ussd_favorites ORDER BY created_at DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM ussd_favorites WHERE id = :id")
    suspend fun get(id: Long): FavoriteEntity?

    @Insert
    suspend fun insert(favorite: FavoriteEntity): Long

    @Query("DELETE FROM ussd_favorites WHERE id = :id")
    suspend fun delete(id: Long)
}
