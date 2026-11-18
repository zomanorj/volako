package mg.monargent.core.sms

import java.time.LocalDateTime

// Les 5 types d'operations MVola qu'on sait reconnaitre
enum class OperationKind {
    CREDIT_PURCHASE, // achat de credit telephonique
    RECEIVED,        // argent recu
    SENT,            // envoi ou paiement marchand
    WITHDRAWAL,      // retrait chez un agent (MVola -> especes)
    DEPOSIT,         // depot chez un agent (especes -> MVola)
}

// Resultat de la lecture d'un SMS
sealed interface ParsedSms {

    // Une operation MVola bien reconnue. Les montants sont en Ariary (pas de centimes)
    data class Operation(
        val kind: OperationKind,
        val amount: Long,
        val fee: Long,
        val balanceAfter: Long,
        val reference: String,
        val occurredAt: LocalDateTime,
        val counterpartyName: String? = null,
        val counterpartyPhone: String? = null,
        val reason: String? = null,
        val operator: String? = null,
        // regle exacte, modele appris ou lecteur universel
        val readBy: ReadMethod = ReadMethod.EXACT,
    ) : ParsedSms {

        // De combien le solde MVola doit bouger avec cette operation
        val balanceChange: Long
            get() = when (kind) {
                OperationKind.RECEIVED, OperationKind.DEPOSIT -> amount - fee
                OperationKind.CREDIT_PURCHASE,
                OperationKind.SENT,
                OperationKind.WITHDRAWAL -> -(amount + fee)
            }
    }

    // SMS MVola sans operation (pub, 2e partie d'un SMS, info...)
    data object Ignored : ParsedSms

    // Ressemble a une operation mais format inconnu -> liste "A verifier"
    data class Unknown(val body: String) : ParsedSms
}
