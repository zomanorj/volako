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

    // plusieurs d'un coup (import de l'historique). -1 pour chaque doublon ignore
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    // dernier SMS avant une date, pour connaitre le solde MVola d'avant
    @Query(
        """
        SELECT * FROM transactions WHERE sms_balance IS NOT NULL AND occurred_at <= :before
        ORDER BY occurred_at DESC, CAST(sms_reference AS INTEGER) DESC
        LIMIT 1
        """,
    )
    suspend fun lastSmsBefore(before: Long): TransactionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE sms_reference = :reference)")
    suspend fun existsByReference(reference: String): Boolean

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}
