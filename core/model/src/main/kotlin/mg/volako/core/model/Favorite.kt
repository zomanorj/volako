package mg.volako.core.model

import java.time.LocalDateTime

// Ce qu'un favori refait
enum class FavoriteKind {
    SEND,           // envoyer de l'argent a un numero
    CREDIT_ME,      // credit pour mon numero
    CREDIT_OTHER,   // credit pour un autre numero
    WITHDRAW_AGENT, // retrait chez un agent
    OFFER_ME,       // offre pour mon numero
}

// Une operation MVola enregistree pour la refaire en un appui (le code secret n'est JAMAIS garde)
data class Favorite(
    val id: Long = 0,
    val kind: FavoriteKind,
    val amount: Long? = null,
    val phone: String? = null,
    val name: String? = null,
    val offerId: String? = null,
    val payRecipientFee: Boolean = false,
    val description: String? = null,
    val createdAt: LocalDateTime,
)
