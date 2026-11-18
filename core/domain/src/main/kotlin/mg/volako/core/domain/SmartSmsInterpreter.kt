package mg.volako.core.domain

import mg.volako.core.sms.BalanceCheck
import mg.volako.core.sms.GenericSmsReader
import mg.volako.core.sms.MvolaSmsParser
import mg.volako.core.sms.OperationKind
import mg.volako.core.sms.ParsedSms
import mg.volako.core.sms.ReadMethod
import mg.volako.core.sms.SmsClues
import mg.volako.core.sms.SmsTemplate
import mg.volako.core.sms.SmsTemplates
import java.time.LocalDateTime

// Lit n'importe quel SMS MVola, meme si YAS change la formulation.
// 1) regles exactes (formats connus)
// 2) modeles appris des confirmations de l'utilisateur
// 3) lecteur universel
// Pour 2 et 3 : si le SMS donne un solde, il doit coller (ancien solde + mouvement = nouveau).
// Sinon -> "A verifier". Comme ca l'app ne se trompe jamais en silence.
object SmartSmsInterpreter {

    fun interpret(
        body: String,
        receivedAt: LocalDateTime,
        previousBalance: Long?,
        templates: List<SmsTemplate> = emptyList(),
    ): ParsedSms {
        val exact = MvolaSmsParser.parse(body, receivedAt)
        if (exact is ParsedSms.Operation) return exact

        // un modele appris correspond ?
        for (template in templates) {
            val op = SmsTemplates.match(template, body, receivedAt) ?: continue
            val hasBalance = op.balanceAfter != SmsTemplates.NO_BALANCE
            // le modele dit ou sont les nombres, le solde confirme qu'on a bien lu
            if (hasBalance && previousBalance != null && BalanceCheck.gap(previousBalance, op) != 0L) {
                return ParsedSms.Unknown(body)
            }
            return op
        }

        // pas de solde dans le SMS ou pas de solde d'avant : impossible de verifier, on ne devine pas.
        // Mais on n'ignore JAMAIS en silence un SMS qui ressemble a une operation : "A verifier"
        val clues = GenericSmsReader.read(body) ?: return exact
        val balance = clues.balance
        if (balance == null || previousBalance == null || clues.amount <= 0) {
            return if (exact == ParsedSms.Ignored && looksLikeOperation(clues)) ParsedSms.Unknown(body) else exact
        }

        val verified = verify(clues, balance - previousBalance) ?: return ParsedSms.Unknown(body)
        return ParsedSms.Operation(
            kind = verified.first,
            amount = clues.amount,
            fee = verified.second,
            balanceAfter = balance,
            reference = clues.reference,
            occurredAt = MvolaSmsParser.bestTime(clues.occurredAt, receivedAt),
            counterpartyName = clues.name,
            counterpartyPhone = clues.phone,
            readBy = ReadMethod.GENERIC,
        )
    }

    // un montant + (une vraie reference, un numero ou un solde) = surement une operation.
    // Une pub n'a en general qu'un montant ("Gagnez 1 000 000 Ar")
    private fun looksLikeOperation(c: SmsClues) = c.balance != null || !c.referenceIsSynthetic || c.phone != null

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
