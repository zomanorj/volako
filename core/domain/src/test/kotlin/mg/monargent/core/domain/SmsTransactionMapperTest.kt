package mg.monargent.core.domain

import mg.monargent.core.model.Account
import mg.monargent.core.model.Category
import mg.monargent.core.model.TransactionType
import mg.monargent.core.sms.MvolaSmsParser
import mg.monargent.core.sms.ParsedSms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class SmsTransactionMapperTest {

    private val at = LocalDateTime.of(2026, 11, 16, 12, 0)
    private fun op(body: String) = MvolaSmsParser.parse(body, at) as ParsedSms.Operation

    @Test
    fun `retrait devient un transfert MVola vers especes avec frais`() {
        val t = SmsTransactionMapper.map(
            op("Retrait reussi: 10 000 Ar aupres de B 0380000002 le 16/11/26 a 07:40. Frais: 275 Ar. Solde : 190 113 Ar. Ref: 3."),
            previousSmsBalance = 200_388,
        )
        assertEquals(TransactionType.TRANSFER, t.type)
        assertEquals(Account.MVOLA, t.account)
        assertEquals(Account.CASH, t.toAccount)
        assertEquals(275L, t.fee)
        assertEquals(-10_275L, t.effectOn(Account.MVOLA))
        assertEquals(10_000L, t.effectOn(Account.CASH))
    }

    @Test
    fun `depot devient un transfert especes vers MVola`() {
        val t = SmsTransactionMapper.map(
            op("1/2 Depot reussi : 2 000 000 Ar aupres de C (0340000004) le 10/02/26 a 14:01. Solde : 2 039 250 Ar. Ref: 5"),
            previousSmsBalance = null,
        )
        assertEquals(Account.CASH, t.account)
        assertEquals(Account.MVOLA, t.toAccount)
        assertEquals(-2_000_000L, t.effectOn(Account.CASH))
    }

    @Test
    fun `achat de credit est une depense credit`() {
        val t = SmsTransactionMapper.map(
            op("Achat de credit YAS reussi: 500 Ar. Bonus: 20 pourcent. Solde MVola : 388 Ar. Ref: 1"),
            previousSmsBalance = 888,
        )
        assertEquals(TransactionType.EXPENSE, t.type)
        assertEquals(Category.CREDIT, t.category)
        assertEquals("Crédit YAS", t.label)
    }

    @Test
    fun `envoi garde la raison comme libelle`() {
        val t = SmsTransactionMapper.map(
            op("81 900 Ar envoye a Toky Rabe 0340000003 le 16/11/26 a 10:54. Frais: 819 Ar. Raison: Chargeur et cache ecran. Solde: 64 094 Ar. Ref: 4"),
            previousSmsBalance = 146_813,
        )
        assertEquals("Chargeur et cache ecran", t.label)
        assertEquals(819L, t.fee)
    }

    @Test
    fun `frais annonces mais pas preleves ne sont pas comptes`() {
        // vrai cas : 146 813 - 81 900 = 64 913, les 819 Ar de frais n'ont pas bouge le solde
        val t = SmsTransactionMapper.map(
            op("81 900 Ar envoye a Toky Rabe 0340000003 le 16/11/26 a 10:54. Frais: 819 Ar. Raison: Chargeur. Solde: 64 913 Ar. Ref: 4"),
            previousSmsBalance = 146_813,
        )
        assertEquals(0L, t.fee)
    }

    @Test
    fun `raison sans sens comme 37 ou T est ignoree`() {
        val recu = SmsTransactionMapper.map(
            op("200 000 Ar recu de A 0340000001 le 16/11/26 a 07:39. Raison: 37. Solde: 200 388 Ar. Ref 2"),
            previousSmsBalance = 388,
        )
        assertNull(recu.label)
        val envoi = SmsTransactionMapper.map(
            op("203 400 Ar envoye a D (0340000005) le 10/02/26 a 14:20. Frais: 1 900 Ar. Raison: T. Solde : 1 833 950 Ar. Ref: 6"),
            previousSmsBalance = 2_039_250,
        )
        assertNull(envoi.label)
    }

    @Test
    fun `argent recu sans raison n a pas de libelle`() {
        val t = SmsTransactionMapper.map(
            op("5 000 Ar recu de Soa 0340000006 le 01/12/26 a 08:00. Solde: 15 000 Ar. Ref: 9"),
            previousSmsBalance = 10_000,
        )
        assertEquals(TransactionType.INCOME, t.type)
        assertNull(t.label)
    }
}
