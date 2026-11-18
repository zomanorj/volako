package mg.monargent.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import mg.monargent.MainActivity
import mg.monargent.R
import mg.monargent.core.designsystem.component.formatAriary
import mg.monargent.core.model.Account
import mg.monargent.core.model.Category
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionType
import mg.monargent.core.designsystem.R as DesignR

// Petite notification discrete a chaque operation MVola detectee
object OperationNotifier {

    private const val CHANNEL_ID = "mvola_operations"

    // le canal doit exister avant la 1re notif. L'utilisateur peut le couper dans les reglages
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW, // pas de son, juste l'info
        ).apply { description = context.getString(R.string.notif_channel_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notify(context: Context, t: Transaction) {
        // Android 13+ : sans la permission on ne fait rien
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val details = listOfNotNull(
            t.fee.takeIf { it > 0 }?.let { context.getString(R.string.notif_fee, formatAriary(it)) },
            t.smsBalance?.let { context.getString(R.string.notif_balance, formatAriary(it)) },
        ).joinToString(" · ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(DesignR.drawable.ic_account_balance_wallet)
            .setContentTitle(title(context, t))
            .setContentText(details)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        // id = id de l'operation, comme ca chaque operation a sa notif
        NotificationManagerCompat.from(context).notify(t.id.toInt(), notification)
    }

    private fun title(context: Context, t: Transaction): String {
        val amount = formatAriary(t.amount)
        return when (t.type) {
            TransactionType.TRANSFER -> context.getString(
                if (t.toAccount == Account.CASH) R.string.notif_withdrawal else R.string.notif_deposit,
                amount,
            )
            TransactionType.INCOME -> context.getString(
                R.string.notif_income, amount, t.counterpartyName ?: context.getString(R.string.notif_received),
            )
            TransactionType.EXPENSE -> {
                val what = when {
                    t.category == Category.CREDIT -> context.getString(R.string.notif_credit)
                    else -> t.label ?: t.counterpartyName ?: context.getString(R.string.notif_payment)
                }
                context.getString(R.string.notif_expense, amount, what)
            }
        }
    }
}
