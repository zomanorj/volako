package mg.monargent.core.data

// Bilan d'un import, affiche a l'utilisateur
data class ImportReport(
    val added: Int = 0,
    val duplicates: Int = 0,
    val pending: Int = 0,
    val ignored: Int = 0,
) {
    val total: Int get() = added + duplicates + pending + ignored
}
