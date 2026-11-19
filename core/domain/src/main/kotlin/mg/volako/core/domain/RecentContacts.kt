package mg.volako.core.domain

import mg.volako.core.model.Account
import mg.volako.core.model.Category
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionType
import java.time.LocalDateTime

// Pour quoi on cherche des numeros
enum class ContactKind {
    PEOPLE, // personnes a qui on envoie ou de qui on recoit de l'argent
    CREDIT, // numeros pour lesquels on achete du credit
    AGENT,  // agents MVola chez qui on retire
}

// Un numero deja vu dans les SMS MVola, avec le dernier nom connu
data class RecentContact(
    val phone: String,
    val name: String?,
    val count: Int,
    val lastAt: LocalDateTime,
)

// Retrouve les numeros utiles a partir de l'historique (logique pure, testee).
// Les plus souvent utilises d'abord, puis les plus recents.
object RecentContacts {

    fun from(all: List<Transaction>, kind: ContactKind, limit: Int = 6): List<RecentContact> =
        all.filter { matches(it, kind) }
            .mapNotNull { t -> normalize(t.counterpartyPhone)?.let { it to t } }
            .groupBy({ it.first }, { it.second })
            .map { (phone, list) ->
                val latest = list.maxBy { it.occurredAt }
                RecentContact(
                    phone = phone,
                    // le nom le plus recent (un nom peut manquer sur certains SMS)
                    name = list.sortedByDescending { it.occurredAt }.firstNotNullOfOrNull { it.counterpartyName?.trim()?.ifEmpty { null } },
                    count = list.size,
                    lastAt = latest.occurredAt,
                )
            }
            .sortedWith(compareByDescending<RecentContact> { it.count }.thenByDescending { it.lastAt })
            .take(limit)

    private fun matches(t: Transaction, kind: ContactKind): Boolean = when (kind) {
        ContactKind.PEOPLE -> t.account == Account.MVOLA &&
            (t.type == TransactionType.EXPENSE || t.type == TransactionType.INCOME) &&
            t.category != Category.CREDIT
        ContactKind.CREDIT -> t.category == Category.CREDIT
        // retrait = transfert MVola -> especes
        ContactKind.AGENT -> t.type == TransactionType.TRANSFER && t.account == Account.MVOLA && t.toAccount == Account.CASH
    }

    // "(034) 20 454 85", "+261342045485" -> "0342045485" ; null si ce n'est pas un numero malgache
    fun normalize(raw: String?): String? {
        var d = raw?.filter(Char::isDigit) ?: return null
        if (d.startsWith("261") && d.length == 12) d = "0" + d.substring(3)
        return d.takeIf { it.length == 10 && it.startsWith("03") }
    }
}
