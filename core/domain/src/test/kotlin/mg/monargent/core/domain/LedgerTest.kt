package mg.monargent.core.domain

import mg.monargent.core.model.Account
import mg.monargent.core.model.Category
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionSource
import mg.monargent.core.model.TransactionType
import mg.monargent.core.sms.MvolaSmsParser
import mg.monargent.core.sms.ParsedSms
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth

class LedgerTest {

    private val at = LocalDateTime.of(2026, 11, 16, 7, 0)

    // rejoue une suite de SMS comme le fera l'import.
    // Chaque SMS est "recu" 1 minute apres le precedent (utile pour le credit qui n'a pas de date)
    private fun import(vararg sms: String): List<Transaction> {
        var previous: Long? = null
        var receivedAt = at
        return sms.map {
            val op = MvolaSmsParser.parse(it, receivedAt.plusMinutes(1)) as ParsedSms.Operation
            receivedAt = op.occurredAt
            SmsTransactionMapper.map(op, previous).also { previous = op.balanceAfter }
        }
    }

    private val novembre = import(
        "Achat de credit YAS reussi: 500 Ar. Bonus: 20 pourcent. Solde MVola : 388 Ar. Ref: 1",
        "200 000 Ar recu de A 0340000001 le 16/11/26 a 07:39. Raison: 37. Solde: 200 388 Ar. Ref 2",
        "Retrait reussi: 10 000 Ar aupres de B 0380000002 le 16/11/26 a 07:40. Frais: 275 Ar. Solde : 190 113 Ar. Ref: 3.",
        "Achat de credit YAS reussi: 500 Ar. Bonus: 20 pourcent. Solde MVola : 189 613 Ar. Ref: 4",
    )

    private fun cash(amount: Long, type: TransactionType) = Transaction(
        type = type, amount = amount, account = Account.CASH, category = Category.OTHER,
        occurredAt = at.plusHours(5), source = TransactionSource.MANUAL,
    )

    @Test
    fun `solde MVola = dernier SMS, especes = retraits moins depenses`() {
        val all = novembre + cash(2_000, TransactionType.EXPENSE)
        val b = Ledger.balances(all)
        assertEquals(189_613L, b.mvola)
        assertEquals(8_000L, b.cash)
        assertEquals(197_613L, b.total)
    }

    @Test
    fun `resume du mois sans compter le retrait comme depense`() {
        val s = Ledger.monthSummary(novembre, YearMonth.of(2026, 11))
        assertEquals(200_000L, s.income)
        // 500 + 500 de credit + 275 de frais. Les 10 000 du retrait ne sont PAS une depense
        assertEquals(1_275L, s.expenses)
        assertEquals(1_000L, s.credit)
        assertEquals(275L, s.fees)
    }

    @Test
    fun `aucun trou dans une suite complete`() {
        assertEquals(emptyList<Any>(), Ledger.balanceGaps(novembre))
    }

    @Test
    fun `trou detecte quand il manque un SMS`() {
        val withHole = import(
            "Retrait reussi: 20 000 Ar aupres de F 0380000007 le 17/11/26 a 13:57. Frais: 550 Ar. Solde : 34 088 Ar. Ref: 8.",
            "Achat de credit YAS reussi: 100 Ar. Bonus: 20 pourcent. Solde MVola : 32 488 Ar. Ref: 9",
        )
        val gaps = Ledger.balanceGaps(withHole)
        assertEquals(1, gaps.size)
        assertEquals(-1_500L, gaps.first().second)
    }

    @Test
    fun `frais non preleves ne creent pas de faux trou`() {
        val t = import(
            "Achat de credit YAS reussi: 1 000 Ar. Bonus: 20 pourcent. Solde MVola : 146 813 Ar. Ref: 10",
            "81 900 Ar envoye a T 0340000003 le 16/11/26 a 10:54. Frais: 819 Ar. Raison: X. Solde: 64 913 Ar. Ref: 11",
        )
        assertEquals(emptyList<Any>(), Ledger.balanceGaps(t))
    }
}
