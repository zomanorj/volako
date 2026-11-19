package mg.volako.ui.mvola

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import mg.volako.R
import mg.volako.core.ussd.UssdRequest

// Compose un code USSD.
// Avec la permission "Appels" : la fenetre Yas s'ouvre directement (ACTION_CALL).
// Sans : on ouvre le clavier du telephone avec le code deja tape (ACTION_DIAL, aucune permission).
@Composable
fun rememberUssdLauncher(): (UssdRequest) -> Unit {
    val context = LocalContext.current
    // le code a lancer pendant que la demande de permission est affichee
    var pendingCode by rememberSaveable { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingCode?.let { dial(context, it, direct = granted) }
        pendingCode = null
    }
    return { request ->
        if (hasCallPermission(context)) {
            dial(context, request.code, direct = true)
        } else {
            pendingCode = request.code
            permission.launch(Manifest.permission.CALL_PHONE)
        }
    }
}

private fun hasCallPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

private fun dial(context: Context, code: String, direct: Boolean) {
    // fromParts encode le # en %23 (sinon Android coupe le numero au #)
    val uri = Uri.fromParts("tel", code, null)
    val first = Intent(if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri)
    try {
        context.startActivity(first)
    } catch (_: SecurityException) {
        openDialer(context, uri)
    } catch (_: ActivityNotFoundException) {
        openDialer(context, uri)
    }
}

// plan B : le clavier du telephone, l'utilisateur appuie sur appeler
private fun openDialer(context: Context, uri: Uri) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.ussd_no_phone, Toast.LENGTH_LONG).show()
    }
}
