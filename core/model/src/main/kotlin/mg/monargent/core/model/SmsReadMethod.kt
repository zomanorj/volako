package mg.monargent.core.model

// Comment un SMS MVola a ete compris
enum class SmsReadMethod {
    EXACT,    // format connu
    TEMPLATE, // modele appris d'une confirmation
    GENERIC,  // lecteur universel, verifie par le solde
}
