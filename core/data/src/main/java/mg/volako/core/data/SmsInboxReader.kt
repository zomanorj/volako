package mg.volako.core.data

import android.content.Context
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mg.volako.core.database.entity.toLocalDateTime
import mg.volako.core.sms.MvolaSmsParser

// Lit les SMS MVola deja recus (il faut la permission READ_SMS)
class SmsInboxReader(private val context: Context) {

    // sinceMillis : on ne lit que les SMS plus recents (0 = tout l'historique)
    suspend fun readMvola(sinceMillis: Long): List<Pair<Long, RawSms>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<Long, RawSms>>()
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} > ?",
            arrayOf(sinceMillis.toString()),
            "${Telephony.Sms.DATE} ASC",
        )?.use { cursor ->
            val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                // on filtre ici plutot qu'en SQL : l'expediteur peut etre "MVola" ou "MVOLA"
                if (!MvolaSmsParser.isFromMvola(cursor.getString(address))) continue
                val millis = cursor.getLong(date)
                result += millis to RawSms(cursor.getString(body).orEmpty(), millis.toLocalDateTime())
            }
        }
        result
    }
}
