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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import mg.volako.R
import mg.volako.core.designsystem.component.NumericKeypad
import mg.volako.core.designsystem.theme.AmountTextStyle
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.ussd.YasNumber
import mg.volako.core.designsystem.R as DesignR

// Taper un numero inconnu : on choisit le prefixe (034 deja choisi), puis on tape
// SEULEMENT les 7 chiffres qui suivent, sur un gros pave (comme pour les montants).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberPad(
    names: Map<String, String>,
    onChosen: (Recipient) -> Unit,
    onBack: () -> Unit,
    prefixes: List<String> = listOf("034", "038"),
) {
    var prefix by rememberSaveable { mutableStateOf(prefixes.first()) }
    // les 7 chiffres apres le prefixe
    var rest by rememberSaveable { mutableStateOf("") }
    val number = YasNumber.parse(prefix + rest)
    val knownName = number?.let { names[it.digits] }
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        // 1) le prefixe : un vrai choix, deja rempli
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            prefixes.forEachIndexed { index, p ->
                SegmentedButton(
                    selected = prefix == p,
                    onClick = { prefix = p },
                    shape = SegmentedButtonDefaults.itemShape(index, prefixes.size),
                ) { Text(p) }
            }
        }

        // 2) le numero en grand : prefixe + chiffres tapes + points pour ce qui manque
        Text(
            buildAnnotatedString {
                val typed = group(prefix + rest)
                val full = group(prefix + rest.padEnd(REST_LENGTH, PLACEHOLDER))
                withStyle(SpanStyle(color = colors.onSurface)) { append(typed) }
                withStyle(SpanStyle(color = colors.outline)) { append(full.drop(typed.length)) }
            },
            style = AmountTextStyle.Hero.copy(fontSize = 38.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )

        // 3) une seule ligne d'aide, toujours a la meme place (l'ecran ne saute pas)
        Text(
            when {
                knownName != null -> knownName
                number != null -> stringResource(R.string.ussd_number_unknown)
                else -> stringResource(R.string.ussd_number_hint, prefix)
            },
            style = MaterialTheme.typography.titleSmall,
            color = if (knownName != null) colors.primary else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        NumericKeypad(
            onDigits = { if (rest.length < REST_LENGTH) rest = (rest + it).take(REST_LENGTH) },
            onBackspace = { rest = rest.dropLast(1) },
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

// un numero = prefixe (3 chiffres) + 7 chiffres
private const val REST_LENGTH = 7
private const val PLACEHOLDER = '•'

// "034204••" -> "034 20 4•• ••" : decoupage 3-2-3-2, en gardant les points des chiffres manquants
private fun group(s: String): String {
    val sizes = listOf(3, 2, 3, 2)
    var start = 0
    return sizes.mapNotNull { size ->
        if (start >= s.length) return@mapNotNull null
        s.substring(start, minOf(start + size, s.length)).also { start += size }
    }.joinToString(" ")
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
