package mg.volako.core.domain

import mg.volako.core.model.Account
import mg.volako.core.model.Category
import mg.volako.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class CashCountTest {

    private val at = LocalDateTime.of(2026, 11, 18, 18, 0)

    @Test
    fun `rien a corriger quand le compte est bon`() {
        assertNull(CashCount.adjustment(expectedCash = 12_000, countedCash = 12_000, at = at))
    }

    @Test
    fun `moins que prevu devient une depense non notee`() {
        val t = CashCount.adjustment(expectedCash = 50_000, countedCash = 8_500, at = at)!!
        assertEquals(TransactionType.EXPENSE, t.type)
        assertEquals(41_500L, t.amount)
        assertEquals(Category.UNTRACKED, t.category)
        // apres correction le solde especes = ce qu'on a compte
        assertEquals(8_500L, 50_000 + t.effectOn(Account.CASH))
    }

    @Test
    fun `plus que prevu devient une entree non notee`() {
        val t = CashCount.adjustment(expectedCash = -3_000, countedCash = 2_000, at = at)!!
        assertEquals(TransactionType.INCOME, t.type)
        assertEquals(5_000L, t.amount)
        assertEquals(2_000L, -3_000 + t.effectOn(Account.CASH))
    }
}
