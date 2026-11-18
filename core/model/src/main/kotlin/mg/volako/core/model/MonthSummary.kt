package mg.volako.core.model

import java.time.YearMonth

// Resume d'un mois. "credit" et "fees" sont les deux fuites a surveiller.
data class MonthSummary(
    val month: YearMonth,
    val income: Long,
    // toutes les sorties d'argent : depenses + tous les frais (les transferts ne comptent pas)
    val expenses: Long,
    val credit: Long,
    val fees: Long,
)
