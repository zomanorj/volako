package mg.volako.core.domain

import mg.volako.core.model.Account
import mg.volako.core.model.Category
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionSource
import mg.volako.core.model.TransactionType
import java.time.LocalDateTime

// "Compter mon portefeuille" : on compare ce que l'app pense avec ce qu'il y a vraiment
object CashCount {

    // null si tout colle, sinon l'operation qui corrige l'ecart
    fun adjustment(expectedCash: Long, countedCash: Long, at: LocalDateTime): Transaction? {
        require(countedCash >= 0) { "on ne peut pas avoir moins de 0 Ar en poche" }
        val diff = countedCash - expectedCash
        if (diff == 0L) return null
        return Transaction(
            // moins que prevu = des depenses pas notees, plus que prevu = des entrees pas notees
            type = if (diff < 0) TransactionType.EXPENSE else TransactionType.INCOME,
            amount = kotlin.math.abs(diff),
            account = Account.CASH,
            category = Category.UNTRACKED,
            occurredAt = at,
            source = TransactionSource.MANUAL,
        )
    }
}
