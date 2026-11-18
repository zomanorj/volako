package mg.monargent.core.domain

import mg.monargent.core.sms.OperationKind
import mg.monargent.core.sms.ParsedSms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

// Le lecteur universel n'enregistre QUE si le calcul du solde confirme
class SmartSmsInterpreterTest {

    private val at = LocalDateTime.of(2026, 11, 19, 12, 0)

    private fun read(body: String, previous: Long?) = SmartSmsInterpreter.interpret(body, at, previous)

    @Test
    fun `nouveau format de paiement verifie par le solde`() {
        val o = read(
            "YAS Money : paiement de 5 000 Ar chez SHOPRITE 0340000001. Cout : 50 Ar. Nouveau solde 25 950 Ar. ID 1234567",
            previous = 31_000,
        ) as ParsedSms.Operation
        assertEquals(OperationKind.SENT, o.kind)
        assertEquals(5_000L, o.amount)
        assertEquals(50L, o.fee)
        assertEquals("SHOPRITE", o.counterpartyName)
    }

    @Test
    fun `frais non ecrits deduits du solde`() {
        val o = read(
            "Retrait de 20 000 Ar effectue chez AGENT 0380000003. Votre solde est de 9 450 Ar. Ref 3333333",
            previous = 30_000,
        ) as ParsedSms.Operation
        assertEquals(OperationKind.WITHDRAWAL, o.kind)
        assertEquals(550L, o.fee)
    }

    @Test
    fun `achat d offre pour un autre numero`() {
        val o = read(
            "Achat offre Mora 1 000 Ar pour 0340000004 reussi. Frais 0 Ar. Solde MVola 2 000 Ar. Ref: 4444444",
            previous = 3_000,
        ) as ParsedSms.Operation
        assertEquals(OperationKind.CREDIT_PURCHASE, o.kind)
        assertEquals(1_000L, o.amount)
        assertEquals("0340000004", o.counterpartyPhone)
    }

    @Test
    fun `argent recu en malgache`() {
        val o = read("Nahazo 15 000 Ar avy amin'i Rabe 0340000005 ianao. Ambiny: 40 000 Ar. Ref: 5555555", previous = 25_000)
            as ParsedSms.Operation
        assertEquals(OperationKind.RECEIVED, o.kind)
        assertEquals(15_000L, o.amount)
    }

    @Test
    fun `solde qui ne colle pas va dans A verifier`() {
        val r = read(
            "YAS Money : paiement de 5 000 Ar chez SHOPRITE 0340000001. Cout : 50 Ar. Nouveau solde 25 950 Ar. ID 1234567",
            previous = 40_000,
        )
        assertTrue(r is ParsedSms.Unknown)
    }

    @Test
    fun `les mots disent recu mais le solde baisse - contradiction, A verifier`() {
        val r = read("Vous avez recu 10 000 Ar de Soa 0340000002. Solde: 15 000 Ar. Ref: 2222222", previous = 25_000)
        assertTrue(r is ParsedSms.Unknown)
    }

    @Test
    fun `sans solde d avant on ne devine pas`() {
        val r = read("YAS Money : paiement de 5 000 Ar chez X. Nouveau solde 25 950 Ar. ID 1234567", previous = null)
        assertTrue(r !is ParsedSms.Operation)
    }

    @Test
    fun `publicite avec un montant mais sans solde reste ignoree`() {
        assertEquals(ParsedSms.Ignored, read("Gagnez 1 000 000 Ar avec MVola ce mois-ci !", previous = 10_000))
    }

    @Test
    fun `les formats connus passent toujours par les regles exactes`() {
        val o = read("Achat de credit YAS reussi: 500 Ar. Bonus: 20 pourcent. Solde MVola : 388 Ar. Ref: 1", previous = 9_999)
            as ParsedSms.Operation
        assertEquals(OperationKind.CREDIT_PURCHASE, o.kind)
        assertEquals("YAS", o.operator)
    }
}
