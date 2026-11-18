package mg.monargent.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// Petit utilitaire pour savoir si on a le droit de lire les SMS
object SmsPermission {
    const val READ = Manifest.permission.READ_SMS

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, READ) == PackageManager.PERMISSION_GRANTED
}
