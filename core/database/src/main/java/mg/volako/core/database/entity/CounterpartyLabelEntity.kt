package mg.volako.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// "Toky lalaina 0340000000" -> "Telephone / accessoires" : libelle retenu pour un numero
@Entity(tableName = "counterparty_labels")
data class CounterpartyLabelEntity(
    @PrimaryKey val phone: String,
    val label: String,
)
