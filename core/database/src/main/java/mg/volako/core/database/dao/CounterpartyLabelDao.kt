package mg.volako.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import mg.volako.core.database.entity.CounterpartyLabelEntity

@Dao
interface CounterpartyLabelDao {

    @Upsert
    suspend fun upsert(rule: CounterpartyLabelEntity)

    @Query("SELECT * FROM counterparty_labels")
    suspend fun getAll(): List<CounterpartyLabelEntity>

    @Query("SELECT label FROM counterparty_labels WHERE phone = :phone")
    suspend fun labelFor(phone: String): String?
}
