package mg.monargent.core.data

import java.time.LocalDateTime

// Un SMS tel qu'il sort de la boite de reception
data class RawSms(
    val body: String,
    val receivedAt: LocalDateTime,
)
