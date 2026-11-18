package mg.monargent.core.model

enum class TransactionType {
    EXPENSE,  // depense
    INCOME,   // argent recu
    TRANSFER, // l'argent change de poche (retrait, depot) : ce n'est PAS une depense
}
