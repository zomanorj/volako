package mg.monargent.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mg.monargent.core.database.entity.TransactionEntity

@Dao
interface TransactionDao {

    // les plus recentes en premier
    @Query("SELECT * FROM transactions ORDER BY occurred_at DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    // IGNORE : si la ref MVola existe deja, rien n'est insere et on recoit -1
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    // dernier SMS enregistre, pour connaitre le solde MVola d'avant
    @Query(
        """
        SELECT * FROM transactions WHERE sms_balance IS NOT NULL
        ORDER BY occurred_at DESC, CAST(sms_reference AS INTEGER) DESC
        LIMIT 1
        """,
    )
    suspend fun lastSms(): TransactionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE sms_reference = :reference)")
    suspend fun existsByReference(reference: String): Boolean

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}
