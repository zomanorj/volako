package mg.monargent.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import mg.monargent.core.model.Account
import mg.monargent.core.model.TransactionSource
import mg.monargent.core.model.TransactionType

// Table des operations. La ref MVola est unique : un SMS ne peut pas etre enregistre deux fois
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["sms_reference"], unique = true),
        Index(value = ["occurred_at"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val fee: Long,
    val account: Account,
    @ColumnInfo(name = "to_account") val toAccount: Account?,
    // on stocke la cle texte de la categorie (ex: "credit")
    val category: String,
    val label: String?,
    @ColumnInfo(name = "counterparty_name") val counterpartyName: String?,
    @ColumnInfo(name = "counterparty_phone") val counterpartyPhone: String?,
    // date en millisecondes (heure locale du telephone)
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    val source: TransactionSource,
    @ColumnInfo(name = "sms_reference") val smsReference: String?,
    @ColumnInfo(name = "sms_balance") val smsBalance: Long?,
)
