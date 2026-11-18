package mg.volako.core.model

import java.time.LocalDateTime

// SMS MVola qu'on n'a pas su lire : il attend dans "A verifier"
data class PendingSms(
    val id: Long = 0,
    val body: String,
    val receivedAt: LocalDateTime,
)
