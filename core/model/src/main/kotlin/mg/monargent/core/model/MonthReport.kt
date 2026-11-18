package mg.monargent.core.model

import java.time.YearMonth

// Un poste de depense : "Repas", "Credit", "Frais MVola", "Toky Rabe"...
data class SpendingItem(
    val label: String,
    val total: Long,
    val count: Int,
    // vrai pour le regroupement "Autres"
    val isOther: Boolean = false,
)

// Tout ce qu'affiche l'ecran "Resume du mois"
data class MonthReport(
    val summary: MonthSummary,
    // trie du plus gros au plus petit
    val topSpending: List<SpendingItem>,
    // nombre d'achats de credit dans le mois (pour voir si un forfait serait mieux)
    val creditPurchases: Int,
) {
    val net: Long get() = summary.income - summary.expenses
    val month: YearMonth get() = summary.month
}
