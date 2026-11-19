package mg.volako.core.domain

import mg.volako.core.model.PhoneContact
import java.text.Normalizer

// Une personne proposee dans "choisir un numero"
data class ContactSuggestion(
    val phone: String,
    val name: String?,
    // deja utilise dans MVola (envoi, credit, retrait...)
    val recent: Boolean,
)

// Ce qu'on affiche sous le champ de recherche
data class ContactResults(
    val suggestions: List<ContactSuggestion>,
    val contacts: List<ContactSuggestion>,
)

// Recherche dans les numeros recents + le repertoire (logique pure, testee).
// Le nom du repertoire passe toujours avant le nom vu dans les SMS : c'est celui que l'utilisateur connait.
object ContactSearch {

    // numero -> nom du repertoire (le premier nom trouve si un numero est enregistre deux fois)
    fun namesByPhone(contacts: List<PhoneContact>): Map<String, String> =
        contacts.groupBy { it.phone }.mapValues { (_, list) -> list.first().name }

    fun search(
        query: String,
        recents: List<RecentContact>,
        contacts: List<PhoneContact>,
        accept: (String) -> Boolean = { true },
        maxSuggestions: Int = 5,
        maxContacts: Int = 60,
    ): ContactResults {
        val names = namesByPhone(contacts)
        val recentPhones = recents.map { it.phone }.toSet()

        val suggestions = recents
            .filter { accept(it.phone) }
            .map { ContactSuggestion(it.phone, names[it.phone] ?: it.name, recent = true) }
            .filter { matches(it, query) }
            .take(maxSuggestions)

        val shown = suggestions.map { it.phone }.toSet()
        val others = contacts
            .filter { accept(it.phone) && it.phone !in shown }
            .distinctBy { it.phone }
            .map { ContactSuggestion(it.phone, it.name, recent = it.phone in recentPhones) }
            .filter { matches(it, query) }
            .sortedBy { normalize(it.name.orEmpty()) }
            .take(maxContacts)

        return ContactResults(suggestions, others)
    }

    // nom (sans accents ni majuscules, chaque mot tape doit apparaitre) ou chiffres (partie du numero)
    fun matches(s: ContactSuggestion, query: String): Boolean {
        val q = normalize(query)
        if (q.isEmpty()) return true
        val digits = q.filter(Char::isDigit)
        if (digits.isNotEmpty() && q.all { it.isDigit() || it == ' ' || it == '+' }) {
            // "+261 34..." ou "34 20..." : on compare sans le 0 / 261 du debut
            val wanted = digits.removePrefix("261").removePrefix("0")
            return s.phone.removePrefix("0").contains(wanted)
        }
        val name = normalize(s.name.orEmpty())
        return q.split(' ').filter { it.isNotEmpty() }.all { name.contains(it) }
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
            .lowercase().replace(Regex("\\s+"), " ").trim()
}
