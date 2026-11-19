package mg.volako.ui.mvola

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mg.volako.R
import mg.volako.core.designsystem.component.IconTile
import mg.volako.core.designsystem.component.SectionHeader
import mg.volako.core.designsystem.theme.Sizes
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.domain.RecentContact
import mg.volako.core.ussd.YasNumber
import mg.volako.core.designsystem.R as DesignR

// Le destinataire choisi (le nom est juste pour l'affichage, Yas montrera le vrai nom)
data class Recipient(val number: YasNumber, val name: String?) {
    fun label(): String = name?.let { "$it · ${number.pretty()}" } ?: number.pretty()
}

// Choisir un numero : le taper, le prendre dans le repertoire, ou toucher un numero recent
@Composable
fun RecipientStep(
    title: String,
    recents: List<RecentContact>,
    onChosen: (Recipient) -> Unit,
    header: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    var typed by rememberSaveable { mutableStateOf("") }
    var typedName by rememberSaveable { mutableStateOf<String?>(null) }
    val parsed = YasNumber.parse(typed)
    // on ne montre l'erreur que quand le numero semble complet
    val showError = parsed == null && typed.count(Char::isDigit) >= 10

    val picker = rememberLauncherForActivityResult(PickPhoneContact()) { uri ->
        val picked = uri?.let { readPickedContact(context, it) } ?: return@rememberLauncherForActivityResult
        typed = picked.number
        typedName = picked.name
        // numero Yas valide : on passe directement a l'etape suivante
        YasNumber.parse(picked.number)?.let { onChosen(Recipient(it, picked.name)) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        header()
        OutlinedTextField(
            value = typed,
            onValueChange = {
                typed = it.filter { c -> c.isDigit() || c == ' ' || c == '+' }.take(16)
                typedName = null
            },
            label = { Text(stringResource(R.string.ussd_number_label)) },
            singleLine = true,
            isError = showError,
            supportingText = { if (showError) Text(stringResource(R.string.ussd_error_number)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { parsed?.let { onChosen(Recipient(it, typedName)) } }),
            trailingIcon = {
                IconButton(onClick = { picker.launch(Unit) }) {
                    Icon(painterResource(DesignR.drawable.ic_contacts), stringResource(R.string.ussd_pick_contact))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { parsed?.let { onChosen(Recipient(it, typedName)) } },
            enabled = parsed != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.ussd_continue)) }

        // seulement les numeros Yas (les autres operateurs ne passent pas par ces menus)
        val yasRecents = recents.mapNotNull { r -> YasNumber.parse(r.phone)?.let { Recipient(it, r.name) } }
        if (yasRecents.isNotEmpty()) {
            SectionHeader(stringResource(R.string.ussd_recents))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                yasRecents.forEach { r -> RecentRow(r, onClick = { onChosen(r) }) }
            }
        }
    }
}

@Composable
private fun RecentRow(recipient: Recipient, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .heightIn(min = Sizes.listItemMinHeight)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconTile(DesignR.drawable.ic_person, tint = MaterialTheme.colorScheme.primary, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    recipient.name ?: recipient.number.pretty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (recipient.name != null) {
                    Text(
                        recipient.number.pretty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// rappel du destinataire en haut de l'etape montant (toucher = changer)
@Composable
fun RecipientChip(recipient: Recipient, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(recipient.label(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(painterResource(DesignR.drawable.ic_person), null, Modifier.size(18.dp)) },
        trailingIcon = { Icon(painterResource(DesignR.drawable.ic_edit), stringResource(R.string.ussd_change), Modifier.size(18.dp)) },
    )
}
