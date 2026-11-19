package mg.volako.core.ussd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// Les codes attendus sont ceux testes sur le vrai reseau Yas le 19/11/2026
class MvolaUssdTest {

    private fun ready(op: UssdOperation): UssdRequest = when (val r = MvolaUssd.build(op)) {
        is UssdResult.Ready -> r.request
        is UssdResult.Refused -> fail("refuse : ${r.error}") as Nothing
    }

    private fun error(op: UssdOperation): UssdError = when (val r = MvolaUssd.build(op)) {
        is UssdResult.Refused -> r.error
        is UssdResult.Ready -> fail("aurait du etre refuse : ${r.request.code}") as Nothing
    }

    @Test
    fun `solde credit`() {
        val r = ready(UssdOperation.CreditBalance)
        assertEquals("#357#", r.code)
        assertEquals(NextOnPhone.RESULT, r.next)
    }

    @Test
    fun `credit pour moi`() {
        val r = ready(UssdOperation.CreditForMe(100))
        assertEquals("#111*1*1*1*1*100#", r.code)
        assertEquals(NextOnPhone.SECRET_CODE, r.next)
    }

    @Test
    fun `credit pour un autre numero`() {
        assertEquals("#111*1*1*2*1*0349605839*100#", ready(UssdOperation.CreditForOther("034 96 058 39", 100)).code)
    }

    @Test
    fun `offre pour moi`() {
        val yelow100 = YasOffers.byId("yelow_100")!!
        assertEquals("#111*1*1*3*3*1#", ready(UssdOperation.OfferForMe(yelow100)).code)
        val netWeek = YasOffers.byId("net_week_3000")!!
        assertEquals("#111*1*1*3*0*4*1*1#", ready(UssdOperation.OfferForMe(netWeek)).code)
    }

    @Test
    fun `offre pour un autre utilise le choix 5 et le menu des autres`() {
        // *5 en tout-en-un (avec *4, Yas achete pour soi !), et Ye'low 200 est le choix 6 ici
        val y200 = YasOffers.byId("yelow_200")!!
        val r = ready(UssdOperation.OfferForOther("034 20 454 85", y200))
        assertEquals("#111*1*1*5*0342045485*3*6#", r.code)
        assertEquals(NextOnPhone.SECRET_CODE, r.next)
        val net200k = YasOffers.byId("net_month_200000")!!
        assertEquals("#111*1*1*5*0342045485*4*2*0*6#", ready(UssdOperation.OfferForOther("0342045485", net200k)).code)
        // et pour moi, la meme offre est ailleurs
        assertEquals("#111*1*1*3*3*0*6#", ready(UssdOperation.OfferForMe(y200)).code)
    }

    @Test
    fun `offre pour un autre refuse un numero pas Yas`() {
        assertEquals(UssdError.INVALID_NUMBER, error(UssdOperation.OfferForOther("0321234567", YasOffers.all.first())))
    }

    @Test
    fun `offre avec le credit telephonique`() {
        val r = ready(UssdOperation.OfferWithAirtime(YasOffers.byId("yelow_100")!!))
        assertEquals("#322*67#", r.code)
        assertEquals(NextOnPhone.INSTANT, r.next)
        assertEquals(UssdError.NO_AIRTIME_CODE, error(UssdOperation.OfferWithAirtime(YasOffers.byId("yelow_up")!!)))
    }

    @Test
    fun `envoi avec description en chiffres`() {
        val r = ready(UssdOperation.Send("0342045485", 100, payRecipientFee = false, description = "2611"))
        assertEquals("#111*1*2*0342045485*100*2*2611#", r.code)
        assertEquals(NextOnPhone.SECRET_CODE, r.next)
    }

    @Test
    fun `envoi en payant les frais de retrait`() {
        val r = ready(UssdOperation.Send("0342045485", 100, payRecipientFee = true, description = "1"))
        assertEquals("#111*1*2*0342045485*100*1*1#", r.code)
    }

    @Test
    fun `envoi sans description laisse Yas la demander`() {
        val r = ready(UssdOperation.Send("0342045485", 5_000, payRecipientFee = false, description = "  "))
        assertEquals("#111*1*2*0342045485*5000*2#", r.code)
        assertEquals(NextOnPhone.DESCRIPTION, r.next)
    }

    @Test
    fun `description avec lettres refusee`() {
        // sur le clavier "test" devient 8378, on refuse plutot que d'envoyer n'importe quoi
        assertEquals(
            UssdError.DESCRIPTION_NOT_DIGITS,
            error(UssdOperation.Send("0342045485", 100, payRecipientFee = false, description = "test")),
        )
    }

    @Test
    fun `envoi vers Orange ou Airtel sans question de frais de retrait`() {
        val orange = ready(UssdOperation.Send("032 12 345 67", 1_000, payRecipientFee = true, description = "5"))
        assertEquals("#111*1*2*0321234567*1000*5#", orange.code)
        val airtel = ready(UssdOperation.Send("0331234567", 1_000, payRecipientFee = false, description = null))
        assertEquals("#111*1*2*0331234567*1000#", airtel.code)
        assertEquals(NextOnPhone.DESCRIPTION, airtel.next)
    }

    @Test
    fun `retrait chez un agent`() {
        assertEquals("#111*1*4*1*0342802836*1000#", ready(UssdOperation.WithdrawAtAgent("0342802836", 1_000)).code)
    }

    @Test
    fun `retrait au DAB`() {
        assertEquals("#111*1*4*2#", ready(UssdOperation.WithdrawAtAtm).code)
    }

    @Test
    fun `montants hors limites refuses`() {
        assertEquals(UssdError.AMOUNT_TOO_SMALL, error(UssdOperation.CreditForMe(50)))
        assertEquals(UssdError.AMOUNT_TOO_SMALL, error(UssdOperation.CreditForMe(0)))
        assertEquals(UssdError.AMOUNT_TOO_BIG, error(UssdOperation.WithdrawAtAgent("0342802836", 20_000_001)))
    }

    @Test
    fun `numero pas Yas refuse`() {
        assertEquals(UssdError.INVALID_MOBILE_NUMBER, error(UssdOperation.Send("0391234567", 100, false, "1")))
        assertEquals(UssdError.INVALID_NUMBER, error(UssdOperation.CreditForOther("0321234567", 100)))
        assertEquals(UssdError.INVALID_NUMBER, error(UssdOperation.CreditForOther("12345", 100)))
    }

    @Test
    fun `le diese est encode pour l'intent`() {
        assertEquals("tel:%23111*1*4*2%23", ready(UssdOperation.WithdrawAtAtm).telUri)
    }

    @Test
    fun `aucun code ne contient de lettre`() {
        // garde-fou : un code compose ne doit avoir que des chiffres, * et #
        val ops = YasOffers.all.flatMap {
            listOf(UssdOperation.OfferForMe(it), UssdOperation.OfferForOther("0342045485", it))
        } + YasOffers.all.filter { it.airtimeCode != null }.map { UssdOperation.OfferWithAirtime(it) } + listOf(
            UssdOperation.CreditBalance,
            UssdOperation.CreditForMe(500),
            UssdOperation.WithdrawAtAtm,
        )
        ops.forEach { assertTrue(ready(it).code.matches(Regex("#[0-9*]+#"))) }
    }
}
