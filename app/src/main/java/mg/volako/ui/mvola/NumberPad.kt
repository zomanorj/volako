package mg.volako.ui.mvola

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mg.volako.R
import mg.volako.core.designsystem.component.NumericKeypad
import mg.volako.core.designsystem.theme.AmountTextStyle
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.ussd.YasNumber
import mg.volako.core.designsystem.R as DesignR

// Taper un numero inconnu : gros pave (comme pour les montants), mise en forme en direct,
// nom du repertoire si on le connait, prefixes 034 / 038 en un appui.
@Composable
fun NumberPad(
    names: Map<String, String>,
    onChosen: (Recipient) -> Unit,
    onBack: () -> Unit,
) {
    var digits by rememberSaveable { mutableStateOf("") }
    val number = YasNumber.parse(digits)
    val wrongStart = YasNumber.cannotBeYas(digits)
    val knownName = number?.let { names[it.digits] }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        // le numero en grand, avec des tirets pour les chiffres qui manquent
        Text(
            YasNumber.formatPartial(digits).ifEmpty { "034 ·· ··· ··" },
            style = AmountTextStyle.Hero,
            color = if (digits.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // ligne d'aide : nom connu, erreur, ou rien
        Text(
            when {
                wrongStart -> stringResource(R.string.ussd_number_wrong_start)
                knownName != null -> knownName
                number != null -> stringResource(R.string.ussd_number_unknown)
                else -> " "
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (wrongStart) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (digits.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf("034", "038").forEach { p ->
                    SuggestionChip(onClick = { digits = p }, label = { Text(p) })
                }
            }
        }
        NumericKeypad(
            onDigits = { if (digits.length < 10) digits = (digits + it).take(10) },
            onBackspace = { digits = digits.dropLast(1) },
            backspaceDescription = stringResource(R.string.entry_backspace),
            bottomLeftKey = null,
        )
        Button(
            onClick = { number?.let { onChosen(Recipient(it, knownName)) } },
            enabled = number != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.ussd_continue)) }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ussd_back_to_contacts))
        }
    }
}

// Le presse-papiers contient du texte ? (on regarde seulement le type, sans lire : pas d'alerte Android)
fun clipboardHasText(context: Context): Boolean {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return false
    val d = cm.primaryClipDescription ?: return false
    return d.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || d.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
}

// lu seulement quand l'utilisateur touche "Coller" ; null si ce n'est pas un numero Yas
fun pastedYasNumber(context: Context): YasNumber? {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return null
    val text = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: return null
    // on cherche un numero dans le texte copie ("Mon numero : 034 20 454 85 merci")
    val match = Regex("(\\+?261|0)\\s*3[48](?:[\\s.-]*\\d){7}").find(text)?.value ?: text
    return YasNumber.parse(match)
}

// les deux raccourcis sous la recherche : taper un numero, coller un numero
@Composable
fun NumberShortcuts(showPaste: Boolean, onType: () -> Unit, onPaste: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SuggestionChip(
            onClick = onType,
            label = { Text(stringResource(R.string.ussd_type_number)) },
            icon = { Icon(painterResource(DesignR.drawable.ic_dialpad), null, Modifier.size(18.dp)) },
        )
        if (showPaste) {
            SuggestionChip(
                onClick = onPaste,
                label = { Text(stringResource(R.string.ussd_paste_number)) },
                icon = { Icon(painterResource(DesignR.drawable.ic_content_paste), null, Modifier.size(18.dp)) },
            )
        }
    }
}
