package mg.volako.core.ussd

// Ce que l'utilisateur veut faire depuis l'onglet MVola.
// Le code secret n'apparait JAMAIS ici : il est tape par l'utilisateur dans la bulle Yas.
sealed interface UssdOperation {

    // Solde du credit telephonique (gratuit)
    data object CreditBalance : UssdOperation

    data class CreditForMe(val amount: Long) : UssdOperation

    data class CreditForOther(val number: String, val amount: Long) : UssdOperation

    data class OfferForMe(val offer: YasOffer) : UssdOperation

    // Pas de code tout-en-un : le menu Yas ne montre pas le beneficiaire avant le code secret.
    // On ouvre seulement le menu, l'utilisateur continue dans la bulle Yas.
    data object OfferForOther : UssdOperation

    // description = null : Yas demandera la description, l'utilisateur la tape avec des lettres
    data class Send(
        val number: String,
        val amount: Long,
        val payRecipientFee: Boolean,
        val description: String? = null,
    ) : UssdOperation

    data class WithdrawAtAgent(val agentNumber: String, val amount: Long) : UssdOperation

    // Retrait au DAB BRED : Yas envoie un code temporaire
    data object WithdrawAtAtm : UssdOperation
}

// Ce que l'utilisateur verra dans la bulle Yas juste apres le lancement
enum class NextOnPhone {
    SECRET_CODE, // Yas demande directement le code secret
    DESCRIPTION, // Yas demande la description du transfert
    MENU,        // l'utilisateur continue dans le menu Yas
    RESULT,      // Yas affiche juste une information
}

// Un code pret a etre compose
data class UssdRequest(val code: String, val next: NextOnPhone) {

    // Pour Intent ACTION_CALL / ACTION_DIAL : le # doit etre encode en %23
    val telUri: String get() = "tel:" + code.replace("#", "%23")
}

// Pourquoi on refuse de fabriquer le code (affiche a l'utilisateur)
enum class UssdError {
    INVALID_NUMBER,          // pas un numero Yas 034 / 038
    AMOUNT_TOO_SMALL,
    AMOUNT_TOO_BIG,
    DESCRIPTION_NOT_DIGITS,  // les lettres deviennent des chiffres sur le clavier (test -> 8378)
    DESCRIPTION_TOO_LONG,
}

sealed interface UssdResult {
    data class Ready(val request: UssdRequest) : UssdResult
    data class Refused(val error: UssdError) : UssdResult
}
