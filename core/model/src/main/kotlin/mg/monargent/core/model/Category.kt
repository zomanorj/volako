package mg.monargent.core.model

// Categories de base. On stocke la "key" en base, comme ca on peut renommer l'affichage
// ou ajouter des categories plus tard sans casser les donnees.
enum class Category(val key: String) {
    CREDIT("credit"),
    TRANSFER("transfer"),
    INCOME("income"),
    OTHER("other");

    companion object {
        fun fromKey(key: String?): Category = entries.firstOrNull { it.key == key } ?: OTHER
    }
}
