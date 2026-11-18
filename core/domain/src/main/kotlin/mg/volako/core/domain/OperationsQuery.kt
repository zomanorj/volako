package mg.volako.core.domain

import mg.volako.core.model.Account
import mg.volako.core.model.Category
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionType
import java.text.Normalizer
import java.time.LocalDate

// Les filtres de l'ecran Operations
enum class OperationFilter { ALL, MVOLA, CASH, INCOME, CREDIT, FEES }

// Un jour de la liste : ses operations + combien d'argent est entre/sorti ce jour-la
data class DayGroup(val day: LocalDate, val transactions: List<Transaction>, val net: Long)

// Recherche + filtres + regroupement par jour (logique pure, testee)
object OperationsQuery {

    fun matches(t: Transaction, filter: OperationFilter): Boolean = when (filter) {
        OperationFilter.ALL -> true
        // un retrait concerne les deux comptes, un depot aussi
        OperationFilter.MVOLA -> t.account == Account.MVOLA || t.toAccount == Account.MVOLA
        OperationFilter.CASH -> t.account == Account.CASH || t.toAccount == Account.CASH
        OperationFilter.INCOME -> t.type == TransactionType.INCOME
        OperationFilter.CREDIT -> t.category == Category.CREDIT
        OperationFilter.FEES -> t.fee > 0
    }

    // cherche dans le libelle, la personne, le numero et le montant.
    // Sans accents ni majuscules ; pour les chiffres on ignore les espaces ("4 000" = "4000")
    fun matchesSearch(t: Transaction, query: String): Boolean {
        val q = normalize(query)
        if (q.isEmpty()) return true
        val digits = q.filter(Char::isDigit)
        val onlyDigits = digits.isNotEmpty() && q.all { it.isDigit() || it == ' ' }
        if (onlyDigits) {
            return t.amount.toString().contains(digits) ||
                (t.fee > 0 && t.fee.toString().contains(digits)) ||
                (t.counterpartyPhone?.contains(digits) == true)
        }
        val haystack = listOfNotNull(t.label, t.counterpartyName, t.counterpartyPhone, categoryWord(t))
            .joinToString(" ") { normalize(it) }
        return q.split(' ').filter { it.isNotEmpty() }.all { haystack.contains(it) }
    }

    fun apply(all: List<Transaction>, filter: OperationFilter, query: String): List<Transaction> =
        all.filter { matches(it, filter) && matchesSearch(it, query) }

    // nombre d'operations par filtre (avec la recherche en cours), pour les compteurs des pastilles
    fun counts(all: List<Transaction>, query: String): Map<OperationFilter, Int> {
        val searched = all.filter { matchesSearch(it, query) }
        return OperationFilter.entries.associateWith { f -> searched.count { matches(it, f) } }
    }

    // argent gagne ou perdu par ces operations (un retrait ne compte que pour ses frais)
    fun net(transactions: List<Transaction>): Long = transactions.sumOf { t ->
        when (t.type) {
            TransactionType.INCOME -> t.amount - t.fee
            TransactionType.EXPENSE -> -(t.amount + t.fee)
            TransactionType.TRANSFER -> -t.fee
        }
    }

    // liste deja triee du plus recent au plus ancien -> groupes par jour dans le meme ordre
    fun groupByDay(transactions: List<Transaction>): List<DayGroup> =
        transactions.groupBy { it.occurredAt.toLocalDate() }
            .map { (day, list) -> DayGroup(day, list, net(list)) }

    // mots implicites pour que "credit", "retrait", "frais" trouvent aussi les bonnes lignes
    private fun categoryWord(t: Transaction): String? = when {
        t.category == Category.CREDIT -> "credit"
        t.type == TransactionType.TRANSFER && t.toAccount == Account.CASH -> "retrait"
        t.type == TransactionType.TRANSFER -> "depot"
        t.category == Category.UNTRACKED -> "non notees"
        else -> null
    }?.let { if (t.fee > 0) "$it frais" else it } ?: if (t.fee > 0) "frais" else null

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
            .lowercase().replace(Regex("\\s+"), " ").trim()
}
