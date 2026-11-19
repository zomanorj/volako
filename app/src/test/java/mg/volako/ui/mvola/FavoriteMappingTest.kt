package mg.volako.ui.mvola

import mg.volako.core.model.Favorite
import mg.volako.core.model.FavoriteKind
import mg.volako.core.ussd.MvolaUssd
import mg.volako.core.ussd.UssdOperation
import mg.volako.core.ussd.UssdResult
import mg.volako.core.ussd.YasOffers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class FavoriteMappingTest {

    private fun codeOf(op: UssdOperation) = (MvolaUssd.build(op) as UssdResult.Ready).request.code

    // un favori doit redonner exactement le meme code que l'operation d'origine
    @Test
    fun `aller retour garde le meme code`() {
        val ops = listOf(
            UssdOperation.Send("0342045485", 50_000, payRecipientFee = true, description = "11"),
            UssdOperation.Send("0342045485", 1_000, payRecipientFee = false, description = null),
            UssdOperation.CreditForMe(1_000),
            UssdOperation.CreditForOther("0342045485", 500),
            UssdOperation.WithdrawAtAgent("0342802836", 20_000),
            UssdOperation.OfferForMe(YasOffers.byId("net_week_3000")!!),
        )
        ops.forEach { op ->
            val fav = favoriteOf(op, name = "Elyse")!!
            assertEquals(codeOf(op), codeOf(fav.toOperation()!!))
        }
    }

    @Test
    fun `rien a retenir pour le solde, le DAB ou l'offre pour un autre`() {
        assertNull(favoriteOf(UssdOperation.CreditBalance))
        assertNull(favoriteOf(UssdOperation.WithdrawAtAtm))
        assertNull(favoriteOf(UssdOperation.OfferForOther))
    }

    @Test
    fun `une offre retiree du catalogue ne donne plus d'operation`() {
        val old = Favorite(kind = FavoriteKind.OFFER_ME, offerId = "offre_disparue", createdAt = LocalDateTime.now())
        assertNull(old.toOperation())
    }

    @Test
    fun `doublons reconnus`() {
        val a = favoriteOf(UssdOperation.Send("0342045485", 50_000, false, null), "Elyse")!!
        val b = favoriteOf(UssdOperation.Send("0342045485", 50_000, false, null), "Autre nom")!!
        val c = favoriteOf(UssdOperation.Send("0342045485", 60_000, false, null), "Elyse")!!
        assertTrue(a.sameAs(b))
        assertFalse(a.sameAs(c))
    }
}
