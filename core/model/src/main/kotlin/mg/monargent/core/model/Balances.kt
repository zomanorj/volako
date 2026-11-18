package mg.monargent.core.model

// Soldes affiches sur l'accueil
data class Balances(val mvola: Long, val cash: Long) {
    val total: Long get() = mvola + cash
}
