package mg.monargent.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Un modele de SMS appris (voir SmsTemplates). Les mots sont stockes colles avec un separateur.
@Entity(tableName = "sms_templates")
data class SmsTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val words: String,
    @ColumnInfo(name = "number_count") val numberCount: Int,
    @ColumnInfo(name = "amount_slot") val amountSlot: Int,
    @ColumnInfo(name = "fee_slot") val feeSlot: Int?,
    @ColumnInfo(name = "balance_slot") val balanceSlot: Int?,
    @ColumnInfo(name = "reference_slot") val referenceSlot: Int?,
    @ColumnInfo(name = "phone_slot") val phoneSlot: Int?,
    val kind: String,
    val label: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
