package mg.volako.ui.mvola

import mg.volako.core.model.Favorite
import mg.volako.core.model.FavoriteKind
import mg.volako.core.ussd.UssdOperation
import mg.volako.core.ussd.YasOffers
import java.time.LocalDateTime

// Conversion favori <-> operation USSD (code pur, teste dans FavoriteMappingTest)

// null si le favori ne peut plus marcher (offre retiree du catalogue...)
fun Favorite.toOperation(): UssdOperation? = when (kind) {
    FavoriteKind.SEND -> UssdOperation.Send(phone.orEmpty(), amount ?: 0, payRecipientFee, description)
    FavoriteKind.CREDIT_ME -> UssdOperation.CreditForMe(amount ?: 0)
    FavoriteKind.CREDIT_OTHER -> UssdOperation.CreditForOther(phone.orEmpty(), amount ?: 0)
    FavoriteKind.WITHDRAW_AGENT -> UssdOperation.WithdrawAtAgent(phone.orEmpty(), amount ?: 0)
    FavoriteKind.OFFER_ME -> offerId?.let(YasOffers::byId)?.let { UssdOperation.OfferForMe(it) }
}

// fabrique le favori a partir de ce que l'utilisateur vient de preparer
fun favoriteOf(op: UssdOperation, name: String? = null): Favorite? {
    val now = LocalDateTime.now()
    return when (op) {
        is UssdOperation.Send -> Favorite(
            kind = FavoriteKind.SEND, amount = op.amount, phone = op.number, name = name,
            payRecipientFee = op.payRecipientFee, description = op.description?.trim()?.ifEmpty { null }, createdAt = now,
        )
        is UssdOperation.CreditForMe -> Favorite(kind = FavoriteKind.CREDIT_ME, amount = op.amount, createdAt = now)
        is UssdOperation.CreditForOther -> Favorite(
            kind = FavoriteKind.CREDIT_OTHER, amount = op.amount, phone = op.number, name = name, createdAt = now,
        )
        is UssdOperation.WithdrawAtAgent -> Favorite(
            kind = FavoriteKind.WITHDRAW_AGENT, amount = op.amount, phone = op.agentNumber, name = name, createdAt = now,
        )
        is UssdOperation.OfferForMe -> Favorite(kind = FavoriteKind.OFFER_ME, offerId = op.offer.id, createdAt = now)
        // rien a retenir : pas de montant ni de numero
        UssdOperation.CreditBalance, UssdOperation.OfferForOther, UssdOperation.WithdrawAtAtm -> null
    }
}

// deux favoris identiques ne servent a rien
fun Favorite.sameAs(other: Favorite) =
    kind == other.kind && amount == other.amount && phone == other.phone &&
        offerId == other.offerId && payRecipientFee == other.payRecipientFee && description == other.description
