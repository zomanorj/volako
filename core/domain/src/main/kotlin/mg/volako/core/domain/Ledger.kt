package mg.volako.core.domain

import mg.volako.core.model.Account
import mg.volako.core.model.BalanceGap
import mg.volako.core.model.Balances
import mg.volako.core.model.Category
import mg.volako.core.model.MonthSummary
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionSource
import mg.volako.core.model.TransactionType
import java.time.YearMonth

// Tous les calculs a partir de la liste des operations
object Ledger {

    // ordre chronologique. A la meme minute, la ref MVola (qui augmente avec le temps) departage
    val chronological: Comparator<Transaction> = compareBy<Transaction>(
        { it.occurredAt },
        { it.smsReference?.toLongOrNull() ?: Long.MAX_VALUE },
        { it.id },
    )

    fun balances(transactions: List<Transaction>): Balances =
        Balances(mvola = mvolaBalance(transactions), cash = sumEffects(transactions, Account.CASH))

    // MVola : le dernier SMS donne le vrai solde, on ajoute juste les saisies manuelles faites apres
    private fun mvolaBalance(transactions: List<Transaction>): Long {
        val sorted = transactions.sortedWith(chronological)
        val lastSmsIndex = sorted.indexOfLast { it.smsBalance != null }
        if (lastSmsIndex == -1) return sumEffects(sorted, Account.MVOLA)

        val afterLastSms = sorted.drop(lastSmsIndex + 1).filter { it.source == TransactionSource.MANUAL }
        return sorted[lastSmsIndex].smsBalance!! + sumEffects(afterLastSms, Account.MVOLA)
    }

    private fun sumEffects(transactions: List<Transaction>, account: Account): Long =
        transactions.sumOf { it.effectOn(account) }

    fun monthSummary(transactions: List<Transaction>, month: YearMonth): MonthSummary {
        val inMonth = transactions.filter { YearMonth.from(it.occurredAt) == month }
        val expenses = inMonth.filter { it.type == TransactionType.EXPENSE }
        val fees = inMonth.sumOf { it.fee }
        return MonthSummary(
            month = month,
            income = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            expenses = expenses.sumOf { it.amount } + fees,
            credit = expenses.filter { it.category == Category.CREDIT }.sumOf { it.amount },
            fees = fees,
        )
    }

    // Les SMS dont le solde ne colle pas avec le SMS d'avant -> il manque probablement un SMS
    fun balanceGaps(transactions: List<Transaction>): List<BalanceGap> {
        val sms = transactions.filter { it.smsBalance != null }.sortedWith(chronological)
        return sms.zipWithNext().mapNotNull { (before, after) ->
            val gap = after.smsBalance!! - (before.smsBalance!! + after.effectOn(Account.MVOLA))
            if (gap != 0L) BalanceGap(before, after, gap) else null
        }
    }
}
