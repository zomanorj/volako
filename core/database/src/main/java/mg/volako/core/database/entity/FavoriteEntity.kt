package mg.volako.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mg.volako.core.model.Favorite
import mg.volako.core.model.FavoriteKind

// v4 : operations MVola favorites (jamais de code secret ici)
@Entity(tableName = "ussd_favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: FavoriteKind,
    val amount: Long?,
    val phone: String?,
    val name: String?,
    @ColumnInfo(name = "offer_id") val offerId: String?,
    @ColumnInfo(name = "pay_recipient_fee") val payRecipientFee: Boolean,
    val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

fun FavoriteEntity.asModel() = Favorite(
    id = id,
    kind = kind,
    amount = amount,
    phone = phone,
    name = name,
    offerId = offerId,
    payRecipientFee = payRecipientFee,
    description = description,
    createdAt = createdAt.toLocalDateTime(),
)

fun Favorite.asEntity() = FavoriteEntity(
    id = id,
    kind = kind,
    amount = amount,
    phone = phone,
    name = name,
    offerId = offerId,
    payRecipientFee = payRecipientFee,
    description = description,
    createdAt = createdAt.toEpochMillis(),
)
