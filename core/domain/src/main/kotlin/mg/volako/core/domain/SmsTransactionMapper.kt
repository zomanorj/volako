package mg.volako.core.domain

import mg.volako.core.model.Account
import mg.volako.core.model.Category
import mg.volako.core.model.SmsReadMethod
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionSource
import mg.volako.core.model.TransactionType
import mg.volako.core.sms.BalanceCheck
import mg.volako.core.sms.OperationKind
import mg.volako.core.sms.ParsedSms
import mg.volako.core.sms.SmsTemplates

// Transforme une operation lue dans un SMS en ligne du carnet
object SmsTransactionMapper {

    // previousSmsBalance = solde MVola du SMS d'avant (null si c'est le premier)
    fun map(op: ParsedSms.Operation, previousSmsBalance: Long?): Transaction {
        val fee = if (feeWasNotCharged(op, previousSmsBalance)) 0 else op.fee

        val (type, account, toAccount, category) = when (op.kind) {
            OperationKind.CREDIT_PURCHASE -> Quad(TransactionType.EXPENSE, Account.MVOLA, null, Category.CREDIT)
            OperationKind.SENT -> Quad(TransactionType.EXPENSE, Account.MVOLA, null, Category.OTHER)
            OperationKind.RECEIVED -> Quad(TransactionType.INCOME, Account.MVOLA, null, Category.INCOME)
            // retrait = l'argent passe de MVola a la poche, depot = l'inverse
            OperationKind.WITHDRAWAL -> Quad(TransactionType.TRANSFER, Account.MVOLA, Account.CASH, Category.TRANSFER)
            OperationKind.DEPOSIT -> Quad(TransactionType.TRANSFER, Account.CASH, Account.MVOLA, Category.TRANSFER)
        }

        return Transaction(
            type = type,
            amount = op.amount,
            fee = fee,
            account = account,
            toAccount = toAccount,
            category = category,
            label = meaningfulReason(op.reason) ?: creditLabel(op),
            counterpartyName = op.counterpartyName,
            counterpartyPhone = op.counterpartyPhone,
            occurredAt = op.occurredAt,
            source = TransactionSource.SMS,
            smsReference = op.reference,
            // un modele appris peut ne pas avoir de solde
            smsBalance = op.balanceAfter.takeIf { it != SmsTemplates.NO_BALANCE },
            readBy = SmsReadMethod.valueOf(op.readBy.name),
        )
    }

    // Cas vu en vrai : le SMS annonce des frais mais le solde montre qu'ils n'ont pas ete preleves.
    // Le solde du SMS fait foi, donc on ne compte pas ces frais.
    private fun feeWasNotCharged(op: ParsedSms.Operation, previous: Long?): Boolean =
        previous != null && op.fee > 0 && op.balanceAfter != SmsTemplates.NO_BALANCE &&
            BalanceCheck.gap(previous, op) == op.fee

    // "Credit YAS" pour soi, "Credit YAS pour 0340000000" pour quelqu'un d'autre
    private fun creditLabel(op: ParsedSms.Operation): String? {
        if (op.kind != OperationKind.CREDIT_PURCHASE) return null
        val name = listOfNotNull("Crédit", op.operator).joinToString(" ")
        return op.counterpartyPhone?.let { "$name pour $it" } ?: name
    }

    // une raison comme "37", "T" ou "78" ne dit rien : on prefere afficher le nom de la personne
    fun meaningfulReason(reason: String?): String? =
        reason?.trim()?.takeIf { r -> r.length >= 2 && r.any(Char::isLetter) }

    private data class Quad(val type: TransactionType, val account: Account, val to: Account?, val category: Category)
}
