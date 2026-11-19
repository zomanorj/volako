package mg.volako.ussd

import android.os.SystemClock

// Ce que le service a le droit de faire, juste apres un appui sur "Lancer".
// Rien n'est arme = le service ne fait rien du tout (il ne regarde meme pas l'ecran).
object UssdAutofill {

    // assez pour que Yas reponde, pas plus (le reste du temps le service dort)
    private const val ARMED_FOR_MS = 90_000L

    data class Job(val description: String?, val until: Long, val descriptionDone: Boolean = false)

    @Volatile
    private var job: Job? = null

    fun arm(description: String?) {
        job = Job(description, SystemClock.elapsedRealtime() + ARMED_FOR_MS)
        UssdAutofillService.wake()
    }

    // null si rien a faire ou si le delai est passe
    fun current(): Job? = job?.takeIf { SystemClock.elapsedRealtime() < it.until }

    fun markDescriptionDone() {
        job = job?.copy(descriptionDone = true)
    }

    fun disarm() {
        job = null
    }
}
