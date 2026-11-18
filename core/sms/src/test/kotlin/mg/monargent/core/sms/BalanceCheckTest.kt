package mg.monargent.core.sms

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

// On rejoue de vraies suites de SMS et on verifie que les soldes s'enchainent
class BalanceCheckTest {

    private val at = LocalDateTime.of(2026, 11, 16, 12, 0)

    private fun op(body: String) = MvolaSmsParser.parse(body, at) as ParsedSms.Operation

    @Test
    fun `suite du 16 novembre sans trou`() {
        val sms = listOf(
            "Achat de credit YAS reussi: 500 Ar. Bonus: 20 pourcent sur ce dernier rechargement. Solde MVola : 388 Ar. Ref: 1",
            "200 000 Ar recu de A 0340000001 le 16/11/26 a 07:39. Raison: 37. Solde: 200 388 Ar. Ref 2",
            "Retrait reussi: 10 000 Ar aupres de B 0380000002 le 16/11/26 a 07:40. Frais: 275 Ar. Solde : 190 113 Ar. Ref: 3.",
            "Achat de credit YAS reussi: 500 Ar. Bonus: 20 pourcent sur ce dernier rechargement. Solde MVola : 189 613 Ar. Ref: 4",
        ).map(::op)

        sms.zipWithNext().forEach { (before, after) ->
            assertEquals("ecart sur la ref ${after.reference}", 0L, BalanceCheck.gap(before.balanceAfter, after))
        }
    }

    @Test
    fun `depot puis deux envois avec frais`() {
        val sms = listOf(
            "1/2 Depot reussi : 2 000 000 Ar aupres de C (0340000004) le 10/02/26 a 14:01. Solde : 2 039 250 Ar. Ref: 5",
            "203 400 Ar envoye a D (0340000005) le 10/02/26 a 14:20. Frais: 1 900 Ar. Raison: T. Solde : 1 833 950 Ar. Ref: 6",
            "1 797 000 Ar envoye a E (0340000006) le 10/02/26 a 14:31. Frais: 3 000 Ar. Raison: 78. Solde : 33 950 Ar. Ref: 7",
        ).map(::op)

        sms.zipWithNext().forEach { (before, after) ->
            assertEquals(0L, BalanceCheck.gap(before.balanceAfter, after))
        }
    }

    @Test
    fun `SMS manquant detecte`() {
        // entre ces deux SMS il manque 1 500 Ar d'operations (vrai cas du 17-18 novembre)
        val avant = op("Retrait reussi: 20 000 Ar aupres de F 0380000007 le 17/11/26 a 13:57. Frais: 550 Ar. Solde : 34 088 Ar. Ref: 8.")
        val apres = op("Achat de credit YAS reussi: 100 Ar. Bonus: 20 pourcent sur ce dernier rechargement. Solde MVola : 32 488 Ar. Ref: 9")
        assertEquals(-1_500L, BalanceCheck.gap(avant.balanceAfter, apres))
    }
}
