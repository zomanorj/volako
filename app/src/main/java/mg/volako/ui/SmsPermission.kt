package mg.volako.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// Petit utilitaire pour les permissions SMS et notifications
object SmsPermission {
    const val READ = Manifest.permission.READ_SMS

    // permissions pour la detection en direct (etape 4)
    val LIVE: Array<String> = buildList {
        add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    fun isGranted(context: Context, permission: String = READ): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun missingLive(context: Context): Array<String> = LIVE.filterNot { isGranted(context, it) }.toTypedArray()
}
