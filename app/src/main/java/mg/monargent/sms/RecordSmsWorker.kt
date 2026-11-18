package mg.monargent.sms

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import mg.monargent.MonArgentApplication
import mg.monargent.R
import mg.monargent.core.data.SmsRecordResult
import mg.monargent.notification.OperationNotifier
import java.time.LocalDateTime
import mg.monargent.core.designsystem.R as DesignR

// Enregistre un SMS MVola en base, en dehors du recepteur.
// Comme ca le recepteur rend la main a Android tout de suite et ne retarde jamais
// la livraison des autres SMS.
class RecordSmsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        val receivedAt = inputData.getString(KEY_RECEIVED_AT)?.let(LocalDateTime::parse) ?: LocalDateTime.now()

        val app = applicationContext as MonArgentApplication
        val recorded = app.container.transactionRepository.recordSms(body, receivedAt)
        val transaction = recorded.transaction
        if (recorded.result == SmsRecordResult.ADDED && transaction != null) {
            OperationNotifier.notify(applicationContext, transaction)
        }
        return Result.success()
    }

    // obligatoire pour setExpedited avant Android 12 (doc officielle WorkManager)
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, OperationNotifier.CHANNEL_ID)
            .setSmallIcon(DesignR.drawable.ic_account_balance_wallet)
            .setContentTitle(applicationContext.getString(R.string.notif_recording))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return ForegroundInfo(FOREGROUND_ID, notification)
    }

    companion object {
        private const val KEY_BODY = "body"
        private const val KEY_RECEIVED_AT = "received_at"
        private const val FOREGROUND_ID = Int.MAX_VALUE

        fun enqueue(context: Context, body: String, receivedAt: LocalDateTime) {
            val request = OneTimeWorkRequestBuilder<RecordSmsWorker>()
                .setInputData(workDataOf(KEY_BODY to body, KEY_RECEIVED_AT to receivedAt.toString()))
                // le plus vite possible, sinon en travail normal si le quota est epuise
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
