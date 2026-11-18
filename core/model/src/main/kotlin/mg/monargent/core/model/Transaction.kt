package mg.monargent.core.model

import java.time.LocalDateTime

// Une operation dans le carnet. Montants en Ariary, toujours positifs :
// c'est le type qui dit si l'argent entre ou sort.
data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    // frais payes en plus (retrait, envoi). Comptes comme une depense "Frais MVola"
    val fee: Long = 0,
    // compte qui paie (depense, transfert) ou qui recoit (revenu)
    val account: Account,
    // seulement pour un transfert : le compte qui recoit
    val toAccount: Account? = null,
    val category: Category,
    val label: String? = null,
    val counterpartyName: String? = null,
    val counterpartyPhone: String? = null,
    val occurredAt: LocalDateTime,
    val source: TransactionSource,
    // reference MVola, sert a ne jamais enregistrer deux fois le meme SMS
    val smsReference: String? = null,
    // solde MVola annonce dans le SMS apres l'operation
    val smsBalance: Long? = null,
    // pour un SMS : lu par une regle exacte, un modele appris ou le lecteur universel
    val readBy: SmsReadMethod? = null,
) {
    init {
        require(amount >= 0 && fee >= 0) { "les montants doivent etre positifs" }
        require((type == TransactionType.TRANSFER) == (toAccount != null)) { "toAccount seulement pour un transfert" }
    }

    // De combien cette operation fait bouger un compte donne
    fun effectOn(target: Account): Long {
        var delta = 0L
        when (type) {
            TransactionType.EXPENSE -> if (account == target) delta -= amount + fee
            TransactionType.INCOME -> if (account == target) delta += amount - fee
            TransactionType.TRANSFER -> {
                if (account == target) delta -= amount + fee
                if (toAccount == target) delta += amount
            }
        }
        return delta
    }
}
