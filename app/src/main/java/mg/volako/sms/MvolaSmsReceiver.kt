package mg.volako.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import mg.volako.core.sms.MvolaSmsParser
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// Appele par Android a chaque SMS recu, meme si l'app est fermee.
// IMPORTANT : Android passe le SMS aux apps une par une et attend que chacune ait fini
// avant de livrer le SMS suivant. Donc ici on fait le strict minimum et on rend la main
// tout de suite. Le vrai travail (base de donnees) est fait par RecordSmsWorker.
class MvolaSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        // filet de securite : quoi qu'il arrive, on ne fait jamais planter la reception
        runCatching {
            // un long SMS arrive en plusieurs morceaux : on les recolle
            val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val first = parts.firstOrNull() ?: return
            if (!MvolaSmsParser.isFromMvola(first.originatingAddress)) return
            val body = parts.joinToString("") { it.messageBody.orEmpty() }
            // heure d'envoi par le reseau (juste meme si le SMS arrive en retard)
            val sentAt = first.timestampMillis.takeIf { it > 0 }
                ?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
                ?: LocalDateTime.now()
            RecordSmsWorker.enqueue(context, body, sentAt)
        }.onFailure { Log.e(TAG, "SMS MVola non transmis au worker", it) }
    }

    private companion object {
        const val TAG = "MvolaSmsReceiver"
    }
}
