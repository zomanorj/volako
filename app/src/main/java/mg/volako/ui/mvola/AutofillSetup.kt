package mg.volako.ui.mvola

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import mg.volako.R
import mg.volako.ussd.AutofillState
import mg.volako.ussd.UssdAutofillService
import mg.volako.core.designsystem.R as DesignR

// Etat du remplissage auto (relu a chaque retour dans l'app, ex : depuis les Reglages)
@Composable
fun rememberAutofillState(): AutofillState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(UssdAutofillService.state(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state = UssdAutofillService.state(context) }
    return state
}

@Composable
fun rememberAutofillEnabled(): Boolean = rememberAutofillState() == AutofillState.ON

// On explique AVANT d'envoyer vers les Reglages (permission sensible : l'utilisateur doit comprendre)
@Composable
fun AutofillDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val paused = rememberAutofillState() == AutofillState.PAUSED
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(DesignR.drawable.ic_lock), null) },
        title = { Text(stringResource(if (paused) R.string.autofill_paused_title else R.string.autofill_dialog_title)) },
        text = { Text(stringResource(if (paused) R.string.autofill_paused_body else R.string.autofill_dialog_body)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text(stringResource(R.string.autofill_dialog_open)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.autofill_dialog_later)) } },
    )
}
