package mg.monargent.core.domain

import mg.monargent.core.model.Category
import mg.monargent.core.model.MonthReport
import mg.monargent.core.model.SpendingItem
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionType
import java.time.YearMonth

// Calcule le resume d'un mois : ou part l'argent
object MonthReports {

    // noms fixes pour les postes "speciaux" (l'ecran les traduit/affiche)
    const val CREDIT = "credit"
    const val FEES = "fees"
    const val UNTRACKED = "untracked"
    const val UNNAMED = "unnamed"

    fun build(transactions: List<Transaction>, month: YearMonth, maxItems: Int = 6): MonthReport {
        val inMonth = transactions.filter { YearMonth.from(it.occurredAt) == month }
        val expenses = inMonth.filter { it.type == TransactionType.EXPENSE }

        // on regroupe les depenses par poste
        val groups = mutableMapOf<String, Pair<Long, Int>>()
        fun add(key: String, amount: Long) {
            if (amount <= 0) return
            val (total, count) = groups[key] ?: (0L to 0)
            groups[key] = (total + amount) to (count + 1)
        }
        expenses.forEach { add(spendingKey(it), it.amount) }
        // les frais de toutes les operations (retraits, envois) forment un seul poste
        inMonth.forEach { add(FEES, it.fee) }

        val sorted = groups.map { (label, v) -> SpendingItem(label, v.first, v.second) }
            .sortedByDescending { it.total }
        // au-dela de maxItems, tout va dans "Autres" (pas 15 barres illisibles)
        val top = if (sorted.size <= maxItems) {
            sorted
        } else {
            val rest = sorted.drop(maxItems - 1)
            sorted.take(maxItems - 1) + SpendingItem("", rest.sumOf { it.total }, rest.sumOf { it.count }, isOther = true)
        }

        return MonthReport(
            summary = Ledger.monthSummary(transactions, month),
            topSpending = top,
            creditPurchases = expenses.count { it.category == Category.CREDIT },
        )
    }

    // les mois qui ont au moins une operation, du plus recent au plus ancien
    fun availableMonths(transactions: List<Transaction>): List<YearMonth> =
        transactions.map { YearMonth.from(it.occurredAt) }.distinct().sortedDescending()

    private fun spendingKey(t: Transaction): String = when (t.category) {
        Category.CREDIT -> CREDIT
        Category.UNTRACKED -> UNTRACKED
        else -> t.label?.trim()?.takeIf { it.isNotEmpty() } ?: t.counterpartyName ?: UNNAMED
    }
}
