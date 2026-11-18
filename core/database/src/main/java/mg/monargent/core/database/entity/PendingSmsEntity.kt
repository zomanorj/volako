package mg.monargent.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// SMS pas reconnus, a verifier par l'utilisateur
@Entity(
    tableName = "pending_sms",
    indices = [Index(value = ["body", "received_at"], unique = true)],
)
data class PendingSmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val body: String,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
)
