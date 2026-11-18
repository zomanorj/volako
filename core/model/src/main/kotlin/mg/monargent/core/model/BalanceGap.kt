package mg.monargent.core.model

// Entre deux SMS MVola, le solde ne colle pas : il manque probablement un SMS
data class BalanceGap(
    val before: Transaction,
    val after: Transaction,
    // en Ariary. Negatif = de l'argent est sorti sans SMS
    val amount: Long,
) {
    // identifiant stable pour pouvoir dire "j'ai vu, c'est bon"
    val key: String get() = "${before.smsReference}-${after.smsReference}"
}
