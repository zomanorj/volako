package mg.volako.core.domain

import mg.volako.core.model.Account
import mg.volako.core.model.Category
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionSource
import mg.volako.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class RecentContactsTest {

    private val t0 = LocalDateTime.of(2026, 11, 10, 9, 0)

    private fun sent(phone: String?, name: String?, daysAgo: Long) = Transaction(
        type = TransactionType.EXPENSE, amount = 1_000, fee = 70, account = Account.MVOLA, category = Category.OTHER,
        counterpartyName = name, counterpartyPhone = phone, occurredAt = t0.minusDays(daysAgo), source = TransactionSource.SMS,
    )

    private fun received(phone: String, name: String) = Transaction(
        type = TransactionType.INCOME, amount = 5_000, account = Account.MVOLA, category = Category.INCOME,
        counterpartyName = name, counterpartyPhone = phone, occurredAt = t0, source = TransactionSource.SMS,
    )

    private fun credit(phone: String) = Transaction(
        type = TransactionType.EXPENSE, amount = 100, fee = 150, account = Account.MVOLA, category = Category.CREDIT,
        counterpartyPhone = phone, occurredAt = t0, source = TransactionSource.SMS,
    )

    private fun withdrawal(phone: String, name: String) = Transaction(
        type = TransactionType.TRANSFER, amount = 10_000, fee = 275, account = Account.MVOLA, toAccount = Account.CASH,
        category = Category.TRANSFER, counterpartyName = name, counterpartyPhone = phone, occurredAt = t0,
        source = TransactionSource.SMS,
    )

    @Test
    fun `les plus frequents d'abord puis les plus recents`() {
        val all = listOf(
            sent("0342045485", "Elyse", 1),
            sent("(034) 20 454 85", "Elyse", 5),
            sent("0381112233", "Soa", 0),
            received("0349998877", "Jean"),
        )
        val list = RecentContacts.from(all, ContactKind.PEOPLE)
        assertEquals(listOf("0342045485", "0381112233", "0349998877"), list.map { it.phone })
        assertEquals(2, list.first().count)
    }

    @Test
    fun `garde le nom le plus recent connu`() {
        val all = listOf(sent("0342045485", "Elyse R.", 3), sent("0342045485", null, 0))
        assertEquals("Elyse R.", RecentContacts.from(all, ContactKind.PEOPLE).single().name)
    }

    @Test
    fun `chaque usage a ses propres numeros`() {
        val all = listOf(sent("0342045485", "Elyse", 0), credit("0345358300"), withdrawal("0342802836", "RASOARIMANANA"))
        assertEquals(listOf("0342045485"), RecentContacts.from(all, ContactKind.PEOPLE).map { it.phone })
        assertEquals(listOf("0345358300"), RecentContacts.from(all, ContactKind.CREDIT).map { it.phone })
        assertEquals(listOf("0342802836"), RecentContacts.from(all, ContactKind.AGENT).map { it.phone })
    }

    @Test
    fun `ignore les operations sans numero et respecte la limite`() {
        val all = listOf(sent(null, "Marchand", 0)) + (0 until 10).map { sent("03400000$it$it", "P$it", it.toLong()) }
        val list = RecentContacts.from(all, ContactKind.PEOPLE, limit = 3)
        assertEquals(3, list.size)
    }

    @Test
    fun `normalisation des numeros`() {
        assertEquals("0342045485", RecentContacts.normalize("+261 34 20 454 85"))
        assertEquals("0342045485", RecentContacts.normalize("(034) 20-454-85"))
        assertNull(RecentContacts.normalize("12345"))
        assertNull(RecentContacts.normalize(null))
    }
}
