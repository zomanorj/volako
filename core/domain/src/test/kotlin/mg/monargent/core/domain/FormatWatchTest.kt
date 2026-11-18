package mg.monargent.core.domain

import mg.monargent.core.model.Account
import mg.monargent.core.model.Category
import mg.monargent.core.model.PendingSms
import mg.monargent.core.model.SmsReadMethod
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionSource
import mg.monargent.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class FormatWatchTest {

    private val now = LocalDateTime.of(2026, 11, 20, 12, 0)

    private fun sms(daysAgo: Long, readBy: SmsReadMethod) = Transaction(
        type = TransactionType.EXPENSE, amount = 100, account = Account.MVOLA, category = Category.OTHER,
        occurredAt = now.minusDays(daysAgo), source = TransactionSource.SMS, readBy = readBy,
    )

    @Test
    fun `rien si tout est lu par les regles exactes`() {
        assertNull(FormatWatch.latestChange(List(5) { sms(1, SmsReadMethod.EXACT) }, emptyList(), now))
    }

    @Test
    fun `un seul SMS bizarre ne suffit pas`() {
        assertNull(FormatWatch.latestChange(listOf(sms(1, SmsReadMethod.GENERIC)), emptyList(), now))
    }

    @Test
    fun `deux SMS recents non reconnus declenchent l alerte`() {
        val pending = PendingSms(body = "x", receivedAt = now.minusHours(2))
        val r = FormatWatch.latestChange(listOf(sms(3, SmsReadMethod.TEMPLATE)), listOf(pending), now)
        assertEquals(now.minusHours(2), r)
    }

    @Test
    fun `les vieux SMS ne comptent pas`() {
        assertNull(FormatWatch.latestChange(List(3) { sms(30, SmsReadMethod.GENERIC) }, emptyList(), now))
    }
}
