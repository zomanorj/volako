package mg.volako.core.domain

import mg.volako.core.model.Account
import mg.volako.core.model.Category
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionSource
import mg.volako.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class OperationsQueryTest {

    private val day1 = LocalDateTime.of(2026, 11, 18, 10, 0)
    private val day2 = LocalDateTime.of(2026, 11, 16, 10, 0)

    private val credit = Transaction(
        id = 1, type = TransactionType.EXPENSE, amount = 100, fee = 150, account = Account.MVOLA,
        category = Category.CREDIT, label = "Crédit YAS pour 0345358300", counterpartyPhone = "0345358300",
        occurredAt = day1, source = TransactionSource.SMS,
    )
    private val pain = Transaction(
        id = 2, type = TransactionType.EXPENSE, amount = 4_000, account = Account.CASH,
        category = Category.OTHER, label = "Pain", occurredAt = day1.minusHours(1), source = TransactionSource.MANUAL,
    )
    private val retrait = Transaction(
        id = 3, type = TransactionType.TRANSFER, amount = 10_000, fee = 275, account = Account.MVOLA,
        toAccount = Account.CASH, category = Category.TRANSFER, occurredAt = day2, source = TransactionSource.SMS,
    )
    private val recu = Transaction(
        id = 4, type = TransactionType.INCOME, amount = 200_000, account = Account.MVOLA,
        category = Category.INCOME, counterpartyName = "Soa Rabe", occurredAt = day2.minusHours(1),
        source = TransactionSource.SMS,
    )
    private val all = listOf(credit, pain, retrait, recu)

    @Test
    fun `filtres`() {
        assertEquals(listOf(credit, retrait, recu), OperationsQuery.apply(all, OperationFilter.MVOLA, ""))
        // le retrait concerne aussi les especes
        assertEquals(listOf(pain, retrait), OperationsQuery.apply(all, OperationFilter.CASH, ""))
        assertEquals(listOf(recu), OperationsQuery.apply(all, OperationFilter.INCOME, ""))
        assertEquals(listOf(credit), OperationsQuery.apply(all, OperationFilter.CREDIT, ""))
        assertEquals(listOf(credit, retrait), OperationsQuery.apply(all, OperationFilter.FEES, ""))
    }

    @Test
    fun `recherche sans accents ni majuscules`() {
        assertEquals(listOf(credit), OperationsQuery.apply(all, OperationFilter.ALL, "CREDIT"))
        assertEquals(listOf(recu), OperationsQuery.apply(all, OperationFilter.ALL, "soa"))
        assertEquals(listOf(retrait), OperationsQuery.apply(all, OperationFilter.ALL, "retrait"))
    }

    @Test
    fun `recherche par montant ou numero, espaces ignores`() {
        assertEquals(listOf(pain), OperationsQuery.apply(all, OperationFilter.ALL, "4 000"))
        assertEquals(listOf(credit), OperationsQuery.apply(all, OperationFilter.ALL, "034 53"))
    }

    @Test
    fun `un montant est cherche exactement`() {
        // vrai cas : "4000" trouvait aussi un retrait de 40 000
        val gros = pain.copy(id = 9, amount = 40_000, label = "Gros")
        assertEquals(listOf(pain), OperationsQuery.apply(all + gros, OperationFilter.ALL, "4000"))
        // les frais aussi
        assertEquals(listOf(retrait), OperationsQuery.apply(all, OperationFilter.ALL, "275"))
    }

    @Test
    fun `compteurs des pastilles suivent la recherche`() {
        val c = OperationsQuery.counts(all, "")
        assertEquals(4, c[OperationFilter.ALL])
        assertEquals(2, c[OperationFilter.FEES])
        assertEquals(1, OperationsQuery.counts(all, "pain")[OperationFilter.ALL])
    }

    @Test
    fun `total par jour, un retrait ne compte que ses frais`() {
        val groups = OperationsQuery.groupByDay(all)
        assertEquals(2, groups.size)
        assertEquals(-(100 + 150 + 4_000L), groups[0].net)
        assertEquals(200_000L - 275, groups[1].net)
    }

    @Test
    fun `la recherche trouve aussi le nom du repertoire`() {
        val names = mapOf("0345358300" to "Maman")
        assertEquals(listOf(credit), OperationsQuery.apply(all, OperationFilter.ALL, "maman", names))
        assertEquals(emptyList<Transaction>(), OperationsQuery.apply(all, OperationFilter.ALL, "maman"))
    }
}
