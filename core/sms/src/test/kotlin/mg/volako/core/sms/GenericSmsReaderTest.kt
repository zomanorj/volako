package mg.volako.core.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

// Formats INVENTES : on simule un YAS qui change ses SMS
class GenericSmsReaderTest {

    @Test
    fun `trouve montant, frais, solde, ref et personne dans un nouveau format`() {
        val c = GenericSmsReader.read(
            "YAS Money : paiement de 5 000 Ar chez SHOPRITE 0340000001. Cout : 50 Ar. Nouveau solde 25 950 Ar. ID 1234567",
        )!!
        assertEquals(5_000L, c.amount)
        assertEquals(50L, c.fee)
        assertEquals(25_950L, c.balance)
        assertEquals("1234567", c.reference)
        assertEquals("0340000001", c.phone)
        assertEquals("SHOPRITE", c.name)
        assertEquals(OperationKind.SENT, c.hint)
    }

    @Test
    fun `ordre inverse et date en fin de phrase`() {
        val c = GenericSmsReader.read(
            "Solde: 35 000 Ar. Vous avez recu 10 000 Ar de Soa 0340000002 le 19/11/26 a 08:15. Ref: 2222222",
        )!!
        assertEquals(10_000L, c.amount)
        assertEquals(35_000L, c.balance)
        assertEquals(OperationKind.RECEIVED, c.hint)
        assertEquals(LocalDateTime.of(2026, 11, 19, 8, 15), c.occurredAt)
    }

    @Test
    fun `malgache`() {
        val c = GenericSmsReader.read("Nahazo 15 000 Ar avy amin'i Rabe 0340000005 ianao. Ambiny: 40 000 Ar. Ref: 5555555")!!
        assertEquals(15_000L, c.amount)
        assertEquals(40_000L, c.balance)
        assertEquals("Rabe", c.name)
        assertEquals(OperationKind.RECEIVED, c.hint)
    }

    @Test
    fun `sans reference on en fabrique une stable`() {
        val body = "Paiement de 2 000 Ar chez EPICERIE. Solde 8 000 Ar"
        val a = GenericSmsReader.read(body)!!
        assertTrue(a.referenceIsSynthetic)
        assertEquals(a.reference, GenericSmsReader.read(body)!!.reference)
    }

    @Test
    fun `info de solde sans montant en Ar ne donne rien`() {
        assertNull(GenericSmsReader.read("Le 18/11/26 a 16:53, solde MVola : 3 588. Rechargez votre compte. Ref: 11000000002."))
    }
}
