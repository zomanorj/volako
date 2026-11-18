package mg.volako.core.domain

import mg.volako.core.model.Account
import mg.volako.core.model.Category
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionSource
import mg.volako.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth

class MonthReportsTest {

    private val nov = YearMonth.of(2026, 11)
    private val at = LocalDateTime.of(2026, 11, 10, 12, 0)

    private fun expense(amount: Long, label: String?, category: Category = Category.OTHER, fee: Long = 0) = Transaction(
        type = TransactionType.EXPENSE, amount = amount, fee = fee, account = Account.CASH,
        category = category, label = label, occurredAt = at, source = TransactionSource.MANUAL,
    )

    private fun withdrawal(amount: Long, fee: Long) = Transaction(
        type = TransactionType.TRANSFER, amount = amount, fee = fee, account = Account.MVOLA,
        toAccount = Account.CASH, category = Category.TRANSFER, occurredAt = at, source = TransactionSource.SMS,
    )

    @Test
    fun `regroupe par libelle, credit et frais, trie du plus gros au plus petit`() {
        val list = listOf(
            expense(2_000, "Repas"), expense(3_000, "Repas"),
            expense(500, "Crédit YAS", Category.CREDIT), expense(500, "Crédit YAS", Category.CREDIT),
            expense(1_000, "Transport"),
            withdrawal(10_000, fee = 275),
        )
        val r = MonthReports.build(list, nov)
        assertEquals(listOf("Repas", MonthReports.CREDIT, "Transport", MonthReports.FEES), r.topSpending.map { it.label })
        assertEquals(5_000L, r.topSpending.first().total)
        assertEquals(2, r.topSpending.first().count)
        assertEquals(2, r.creditPurchases)
        // le retrait lui-meme n'est jamais un poste de depense, seulement ses frais
        assertTrue(r.topSpending.none { it.total == 10_000L })
    }

    @Test
    fun `au dela du maximum les petits postes vont dans Autres`() {
        val list = (1..8).map { expense(it * 100L, "P$it") }
        val r = MonthReports.build(list, nov, maxItems = 4)
        assertEquals(4, r.topSpending.size)
        val autres = r.topSpending.last()
        assertTrue(autres.isOther)
        // P1..P5 = 100+200+300+400+500
        assertEquals(1_500L, autres.total)
        assertEquals(list.sumOf { it.amount }, r.topSpending.sumOf { it.total })
    }

    @Test
    fun `les autres mois ne comptent pas`() {
        val octobre = expense(9_999, "Vieux").copy(occurredAt = at.minusMonths(1))
        val r = MonthReports.build(listOf(octobre, expense(1_000, "Repas")), nov)
        assertEquals(1, r.topSpending.size)
        assertEquals(listOf(nov, YearMonth.of(2026, 10)), MonthReports.availableMonths(listOf(octobre, expense(1, "x"))))
    }
}
