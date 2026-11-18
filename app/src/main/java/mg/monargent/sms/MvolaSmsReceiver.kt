package mg.monargent.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.launch
import mg.monargent.MonArgentApplication
import mg.monargent.core.data.SmsRecordResult
import mg.monargent.core.sms.MvolaSmsParser
import mg.monargent.notification.OperationNotifier
import java.time.LocalDateTime

// Appele par Android a chaque SMS recu, meme si l'app est fermee
class MvolaSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // un long SMS arrive en plusieurs morceaux : on les recolle
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val first = parts.firstOrNull() ?: return
        if (!MvolaSmsParser.isFromMvola(first.originatingAddress)) return
        val body = parts.joinToString("") { it.messageBody.orEmpty() }
        val receivedAt = LocalDateTime.now()

        val app = context.applicationContext as MonArgentApplication
        // goAsync : on a quelques secondes pour ecrire en base (doc officielle)
        val pending = goAsync()
        app.applicationScope.launch {
            try {
                val recorded = app.container.transactionRepository.recordSms(body, receivedAt)
                val transaction = recorded.transaction
                if (recorded.result == SmsRecordResult.ADDED && transaction != null) {
                    OperationNotifier.notify(context, transaction)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
