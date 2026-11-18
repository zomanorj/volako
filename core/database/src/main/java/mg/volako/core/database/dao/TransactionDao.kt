package mg.volako.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import mg.volako.core.database.entity.TransactionEntity
import mg.volako.core.model.TransactionType

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

    // libelles les plus utilises dans les saisies a la main, pour les proposer en 1 touche
    @Query(
        """
        SELECT label FROM transactions
        WHERE source = 'MANUAL' AND type = :type AND label IS NOT NULL AND label != ''
        GROUP BY label ORDER BY COUNT(*) DESC, MAX(occurred_at) DESC
        LIMIT :limit
        """,
    )
    fun observeFrequentLabels(type: TransactionType, limit: Int): Flow<List<String>>

    @Update
    suspend fun update(transaction: TransactionEntity)

    // corrige l'heure d'une operation SMS deja enregistree (relecture apres une correction)
    @Query("UPDATE transactions SET occurred_at = :occurredAt WHERE sms_reference = :reference AND source = 'SMS'")
    suspend fun retimeByReference(reference: String, occurredAt: Long)

    // applique un libelle a toutes les operations SMS avec ce numero (sauf retraits/depots et credit)
    @Query(
        "UPDATE transactions SET label = :label " +
            "WHERE counterparty_phone = :phone AND type != 'TRANSFER' AND category != 'credit'",
    )
    suspend fun relabelByPhone(phone: String, label: String)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}
