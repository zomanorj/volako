package mg.volako.core.domain

import mg.volako.core.model.PendingSms
import mg.volako.core.model.SmsReadMethod
import mg.volako.core.model.Transaction
import java.time.LocalDateTime

// Surveille si MVola a change ses SMS : plusieurs SMS recents que les regles exactes
// n'ont pas reconnus (lus par un modele, le lecteur universel, ou encore a verifier)
object FormatWatch {

    // date du SMS "nouveau format" le plus recent, ou null s'il n'y a pas de raison de s'inquieter
    fun latestChange(
        transactions: List<Transaction>,
        pending: List<PendingSms>,
        now: LocalDateTime,
        windowDays: Long = 14,
        threshold: Int = 2,
    ): LocalDateTime? {
        val since = now.minusDays(windowDays)
        val dates = transactions
            .filter { it.readBy == SmsReadMethod.TEMPLATE || it.readBy == SmsReadMethod.GENERIC }
            .map { it.occurredAt } +
            pending.map { it.receivedAt }
        val recent = dates.filter { it.isAfter(since) }
        return if (recent.size >= threshold) recent.max() else null
    }
}
