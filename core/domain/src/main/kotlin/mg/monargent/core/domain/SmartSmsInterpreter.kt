package mg.monargent.core.domain

import mg.monargent.core.sms.GenericSmsReader
import mg.monargent.core.sms.MvolaSmsParser
import mg.monargent.core.sms.OperationKind
import mg.monargent.core.sms.ParsedSms
import mg.monargent.core.sms.SmsClues
import java.time.LocalDateTime

// Lit n'importe quel SMS MVola, meme si YAS change la formulation.
// 1) regles exactes (formats connus)  2) sinon lecteur universel, MAIS seulement si le calcul
// du solde confirme : ancien solde + mouvement = nouveau solde. Sinon -> "A verifier".
// Comme ca l'app ne se trompe jamais en silence.
object SmartSmsInterpreter {

    fun interpret(body: String, receivedAt: LocalDateTime, previousBalance: Long?): ParsedSms {
        val exact = MvolaSmsParser.parse(body, receivedAt)
        if (exact is ParsedSms.Operation) return exact

        // pas de solde dans le SMS ou pas de solde d'avant : impossible de verifier, on ne devine pas
        val clues = GenericSmsReader.read(body) ?: return exact
        val balance = clues.balance ?: return exact
        if (previousBalance == null || clues.amount <= 0) return exact

        val verified = verify(clues, balance - previousBalance) ?: return ParsedSms.Unknown(body)
        return ParsedSms.Operation(
            kind = verified.first,
            amount = clues.amount,
            fee = verified.second,
            balanceAfter = balance,
            reference = clues.reference,
            occurredAt = clues.occurredAt ?: receivedAt,
            counterpartyName = clues.name,
            counterpartyPhone = clues.phone,
        )
    }

    // retourne (type, frais) si les chiffres collent, sinon null
    private fun verify(c: SmsClues, delta: Long): Pair<OperationKind, Long>? {
        val fee = c.fee ?: 0
        val isOut = when {
            delta == -(c.amount + fee) -> true
            fee > 0 && delta == -c.amount -> return outKind(c)?.let { it to 0L } // frais annonces mais pas preleves
            delta == c.amount - fee || delta == c.amount -> false
            // pas de frais ecrits mais le solde montre un petit supplement : ce sont des frais
            c.fee == null && delta < -c.amount && (-delta - c.amount) <= maxOf(1_000, c.amount / 10) ->
                return outKind(c)?.let { it to (-delta - c.amount) }
            else -> return null
        }
        return if (isOut) {
            outKind(c)?.let { it to fee }
        } else {
            inKind(c)?.let { it to (if (delta == c.amount) 0L else fee) }
        }
    }

    // l'argent sort : les mots precisent le type. Si les mots disent "recu", c'est contradictoire
    private fun outKind(c: SmsClues): OperationKind? = when (c.hint) {
        OperationKind.WITHDRAWAL, OperationKind.CREDIT_PURCHASE, OperationKind.SENT -> c.hint
        null -> OperationKind.SENT
        else -> null
    }

    private fun inKind(c: SmsClues): OperationKind? = when (c.hint) {
        OperationKind.DEPOSIT -> OperationKind.DEPOSIT
        OperationKind.RECEIVED, null -> OperationKind.RECEIVED
        else -> null
    }
}
