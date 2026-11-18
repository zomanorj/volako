package mg.volako.core.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

// SMS copies de vrais messages MVola. Noms, numeros et refs changes, montants gardes.
class MvolaSmsParserTest {

    private val receivedAt = LocalDateTime.of(2026, 11, 18, 9, 39)

    private fun op(body: String) = MvolaSmsParser.parse(body, receivedAt) as ParsedSms.Operation

    @Test
    fun `achat de credit`() {
        val o = op("Achat de credit YAS reussi: 500 Ar. Bonus: 20 pourcent sur ce dernier rechargement. Solde MVola : 388 Ar. Ref: 10900000001")
        assertEquals(OperationKind.CREDIT_PURCHASE, o.kind)
        assertEquals(500L, o.amount)
        assertEquals(0L, o.fee)
        assertEquals(388L, o.balanceAfter)
        assertEquals("10900000001", o.reference)
        assertEquals("YAS", o.operator)
        // pas de date dans ce SMS -> on prend l'heure de reception
        assertEquals(receivedAt, o.occurredAt)
    }

    @Test
    fun `achat de credit pour un autre numero avec frais`() {
        val o = op("Achat de credit YAS reussi: 100 Ar pour 0340000099. Frais: 150 Ar. Solde MVola : 3 838 Ar. Ref: 11000000001")
        assertEquals(OperationKind.CREDIT_PURCHASE, o.kind)
        assertEquals(100L, o.amount)
        assertEquals(150L, o.fee)
        assertEquals("0340000099", o.counterpartyPhone)
        assertEquals(3_838L, o.balanceAfter)
        assertEquals("YAS", o.operator)
    }

    @Test
    fun `info de solde avec Ref reste ignoree`() {
        val r = MvolaSmsParser.parse(
            "Le 18/11/26 a 16:53, solde MVola : 3 588. Rechargez votre compte Yas via MVola et gagnez un bonus immediatement de 20 pourcent. Ref: 11000000002.",
            receivedAt,
        )
        assertEquals(ParsedSms.Ignored, r)
    }

    @Test
    fun `argent recu sans deux points apres Ref`() {
        val o = op("200 000 Ar recu de RakotoJean 0340000001 le 16/11/26 a 07:39. Raison: 37. Solde: 200 388 Ar. Ref 10900000002")
        assertEquals(OperationKind.RECEIVED, o.kind)
        assertEquals(200_000L, o.amount)
        assertEquals(200_388L, o.balanceAfter)
        assertEquals("RakotoJean", o.counterpartyName)
        assertEquals("0340000001", o.counterpartyPhone)
        assertEquals("37", o.reason)
        assertEquals(LocalDateTime.of(2026, 11, 16, 7, 39), o.occurredAt)
        assertEquals("10900000002", o.reference)
    }

    @Test
    fun `retrait avec chiffre dans le nom de l agent`() {
        val o = op("Retrait reussi: 10 000 Ar aupres de MARIE RASOA 4 0380000002 le 16/11/26 a 07:40. Frais: 275 Ar. Solde : 190 113 Ar. Ref: 10900000003.")
        assertEquals(OperationKind.WITHDRAWAL, o.kind)
        assertEquals(10_000L, o.amount)
        assertEquals(275L, o.fee)
        assertEquals(190_113L, o.balanceAfter)
        assertEquals("MARIE RASOA 4", o.counterpartyName)
        assertEquals("0380000002", o.counterpartyPhone)
    }

    @Test
    fun `envoi avec raison qui contient des espaces`() {
        val o = op("81 900 Ar envoye a Toky Rabe 0340000003 le 16/11/26 a 10:54. Frais: 819 Ar. Raison: Chargeur et cache ecran. Solde: 64 913 Ar. Ref: 10900000004")
        assertEquals(OperationKind.SENT, o.kind)
        assertEquals(81_900L, o.amount)
        assertEquals(819L, o.fee)
        assertEquals("Chargeur et cache ecran", o.reason)
        assertEquals("Toky Rabe", o.counterpartyName)
        assertEquals(64_913L, o.balanceAfter)
    }

    @Test
    fun `depot en deux parties avec numero entre parentheses`() {
        val o = op("1/2 Depot reussi : 2 000 000 Ar aupres de AGENT-ALL (0340000004) le 10/02/26 a 14:01. Solde : 2 039 250 Ar. Ref: 770000001")
        assertEquals(OperationKind.DEPOSIT, o.kind)
        assertEquals(2_000_000L, o.amount)
        assertEquals(0L, o.fee)
        assertEquals("AGENT-ALL", o.counterpartyName)
        assertEquals("0340000004", o.counterpartyPhone)
        assertEquals(LocalDateTime.of(2026, 2, 10, 14, 1), o.occurredAt)

        val suite = MvolaSmsParser.parse("2/2 Les depots MVola sont toujours GRATUITS. Si on vous demande des frais supplementaires, signalez-le nous en appelant le 807.", receivedAt)
        assertEquals(ParsedSms.Ignored, suite)
    }

    @Test
    fun `envoi ancien format avec parentheses`() {
        val o = op("203 400 Ar envoye a Hery Rakoto (0340000005) le 10/02/26 a 14:20. Frais: 1 900 Ar. Raison: T. Solde : 1 833 950 Ar. Ref: 770000002")
        assertEquals(203_400L, o.amount)
        assertEquals(1_900L, o.fee)
        assertEquals("T", o.reason)
        assertEquals(1_833_950L, o.balanceAfter)
    }

    @Test
    fun `accents et espaces insecables acceptes`() {
        val o = op("5 000 Ar reçu de Soa 0340000006 le 01/12/26 à 08:00. Solde: 15 000 Ar. Ref: 1")
        assertEquals(OperationKind.RECEIVED, o.kind)
        assertEquals(5_000L, o.amount)
        assertNull(o.reason)
        assertEquals(15_000L, o.balanceAfter)
    }

    @Test
    fun `publicite ignoree`() {
        val r = MvolaSmsParser.parse("Utilisez / telechargez l'App MVola maintenant. Lien : https://example.com/go", receivedAt)
        assertEquals(ParsedSms.Ignored, r)
    }

    @Test
    fun `format inconnu mis a verifier`() {
        val body = "Vous avez transfere 31 300 Ar a Soa 0340000007 le 10/02/26 a 22:34. Solde : 2 650 Ar. Ref: 770000003"
        val r = MvolaSmsParser.parse(body, receivedAt)
        assertTrue(r is ParsedSms.Unknown)
        assertEquals(body, (r as ParsedSms.Unknown).body)
    }

    @Test
    fun `premier montant d un SMS inconnu`() {
        assertEquals(31_300L, MvolaSmsParser.firstAmount("Vous avez transfere 31 300 Ar a Soa. Solde : 2 650 Ar"))
        assertNull(MvolaSmsParser.firstAmount("Utilisez l'App MVola"))
    }

    @Test
    fun `expediteur MVola reconnu`() {
        assertTrue(MvolaSmsParser.isFromMvola("MVola"))
        assertTrue(MvolaSmsParser.isFromMvola("MVOLA"))
        assertTrue(!MvolaSmsParser.isFromMvola("Telma"))
        assertTrue(!MvolaSmsParser.isFromMvola(null))
    }
}
