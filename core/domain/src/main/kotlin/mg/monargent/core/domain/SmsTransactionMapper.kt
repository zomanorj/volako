package mg.monargent.core.domain

import mg.monargent.core.model.Account
import mg.monargent.core.model.Category
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionSource
import mg.monargent.core.model.TransactionType
import mg.monargent.core.sms.BalanceCheck
import mg.monargent.core.sms.OperationKind
import mg.monargent.core.sms.ParsedSms

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
            label = meaningfulReason(op.reason) ?: op.operator?.let { "Crédit $it" },
            counterpartyName = op.counterpartyName,
            counterpartyPhone = op.counterpartyPhone,
            occurredAt = op.occurredAt,
            source = TransactionSource.SMS,
            smsReference = op.reference,
            smsBalance = op.balanceAfter,
        )
    }

    // Cas vu en vrai : le SMS annonce des frais mais le solde montre qu'ils n'ont pas ete preleves.
    // Le solde du SMS fait foi, donc on ne compte pas ces frais.
    private fun feeWasNotCharged(op: ParsedSms.Operation, previous: Long?): Boolean =
        previous != null && op.fee > 0 && BalanceCheck.gap(previous, op) == op.fee

    // une raison comme "37", "T" ou "78" ne dit rien : on prefere afficher le nom de la personne
    fun meaningfulReason(reason: String?): String? =
        reason?.trim()?.takeIf { r -> r.length >= 2 && r.any(Char::isLetter) }

    private data class Quad(val type: TransactionType, val account: Account, val to: Account?, val category: Category)
}
