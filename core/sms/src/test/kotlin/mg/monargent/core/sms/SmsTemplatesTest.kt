package mg.monargent.core.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class SmsTemplatesTest {

    private val at = LocalDateTime.of(2026, 11, 20, 9, 0)

    // un format invente que l'app ne connait pas : l'utilisateur le classe une fois
    private val first = "Achat offre Mora 1 000 Ar pour 0340000004 reussi. Frais: 100 Ar. Solde MVola : 2 900 Ar. Ref: 4444444"

    private val template = SmsTemplates.learn(first, OperationKind.CREDIT_PURCHASE, amount = 1_000, label = "Offre Mora")!!

    @Test
    fun `apprend la place de chaque nombre`() {
        assertEquals(5, template.numberCount)
        assertNotNull(template.feeSlot)
        assertNotNull(template.balanceSlot)
        assertNotNull(template.referenceSlot)
        assertNotNull(template.phoneSlot)
    }

    @Test
    fun `lit le SMS suivant meme si l offre, le numero et les montants changent`() {
        val o = SmsTemplates.match(
            template,
            "Achat offre Net 5 000 Ar pour 0329999999 reussi. Frais: 250 Ar. Solde MVola : 12 750 Ar. Ref: 5555555",
            at,
        )!!
        assertEquals(OperationKind.CREDIT_PURCHASE, o.kind)
        assertEquals(5_000L, o.amount)
        assertEquals(250L, o.fee)
        assertEquals(12_750L, o.balanceAfter)
        assertEquals("5555555", o.reference)
        assertEquals("0329999999", o.counterpartyPhone)
        assertEquals("Offre Mora", o.reason)
        assertEquals(ReadMethod.TEMPLATE, o.readBy)
    }

    @Test
    fun `un SMS d une autre forme ne correspond pas`() {
        assertNull(
            SmsTemplates.match(
                template,
                "Retrait reussi: 10 000 Ar aupres de B 0380000002 le 16/11/26 a 07:40. Frais: 275 Ar. Solde : 190 113 Ar. Ref: 3.",
                at,
            ),
        )
    }

    @Test
    fun `n apprend rien si le montant confirme n est pas dans le SMS`() {
        assertNull(SmsTemplates.learn(first, OperationKind.CREDIT_PURCHASE, amount = 777, label = null))
    }
}
