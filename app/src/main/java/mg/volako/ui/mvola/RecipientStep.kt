package mg.volako.ui.mvola

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mg.volako.R
import mg.volako.core.designsystem.component.SectionHeader
import mg.volako.core.designsystem.theme.Sizes
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.domain.ContactSearch
import mg.volako.core.domain.ContactSuggestion
import mg.volako.core.domain.RecentContact
import mg.volako.core.model.PhoneContact
import mg.volako.core.ussd.YasNumber
import mg.volako.core.designsystem.R as DesignR

// Le destinataire choisi (le nom est juste pour l'affichage, Yas montrera le vrai nom)
data class Recipient(val number: YasNumber, val name: String?) {
    fun label(): String = name?.let { "$it · ${number.pretty()}" } ?: number.pretty()
}

// Ce qu'il faut pour proposer des numeros : les recents (SMS) + le repertoire du telephone
data class PeoplePicker(
    val recents: List<RecentContact>,
    val contacts: List<PhoneContact>,
    val contactsAllowed: Boolean,
    val onContactsAllowed: () -> Unit,
)

// Choisir un numero : un seul champ (nom ou numero), suggestions recentes puis repertoire
@Composable
fun RecipientStep(
    title: String,
    people: PeoplePicker,
    onChosen: (Recipient) -> Unit,
    header: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    // pave numerique pour un numero qui n'est pas dans le repertoire
    var padMode by rememberSaveable { mutableStateOf(false) }
    val pasteNotFound = stringResource(R.string.ussd_paste_none)
    // seulement les numeros Yas (les autres operateurs ne passent pas par ces menus)
    val results = remember(query, people.recents, people.contacts) {
        ContactSearch.search(query, people.recents, people.contacts, accept = { YasNumber.parse(it) != null })
    }
    val typedNumber = YasNumber.parse(query)
    val names = remember(people.contacts) { ContactSearch.namesByPhone(people.contacts) }
    val looksLikeNumber = query.count(Char::isDigit) >= 10 && query.none(Char::isLetter)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        header()
        if (padMode) {
            NumberPad(names = names, onChosen = onChosen, onBack = { padMode = false })
            return@Column
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(40) },
            placeholder = { Text(stringResource(R.string.ussd_search_people)) },
            singleLine = true,
            isError = looksLikeNumber && typedNumber == null,
            supportingText = { if (looksLikeNumber && typedNumber == null) Text(stringResource(R.string.ussd_error_number)) },
            leadingIcon = { Icon(painterResource(DesignR.drawable.ic_search), null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(painterResource(DesignR.drawable.ic_close), stringResource(R.string.ussd_clear))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        // bien visible : on peut toujours taper ou coller un numero qui n'est pas un contact
        NumberShortcuts(
            showPaste = remember { clipboardHasText(context) },
            onType = { padMode = true },
            onPaste = {
                val pasted = pastedYasNumber(context)
                if (pasted != null) {
                    onChosen(Recipient(pasted, names[pasted.digits]))
                } else {
                    Toast.makeText(context, pasteNotFound, Toast.LENGTH_SHORT).show()
                }
            },
        )

        // un numero complet tape a la main : on le propose tout en haut
        if (typedNumber != null) {
            PersonRow(
                name = names[typedNumber.digits] ?: stringResource(R.string.ussd_use_number),
                number = typedNumber.pretty(),
                onClick = { onChosen(Recipient(typedNumber, names[typedNumber.digits])) },
                highlighted = true,
            )
        }

        if (!people.contactsAllowed) ContactsPermissionCard(people.onContactsAllowed, onPicked = onChosen)

        if (results.suggestions.isNotEmpty()) {
            SectionHeader(stringResource(R.string.ussd_suggestions))
            PeopleList(results.suggestions, onChosen)
        }
        if (results.contacts.isNotEmpty()) {
            SectionHeader(stringResource(R.string.ussd_contacts))
            PeopleList(results.contacts, onChosen)
        }
        if (query.isNotBlank() && typedNumber == null && results.suggestions.isEmpty() && results.contacts.isEmpty()) {
            Text(
                stringResource(R.string.ussd_no_match),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { padMode = true }) { Text(stringResource(R.string.ussd_type_number)) }
        }
    }
}

@Composable
private fun PeopleList(list: List<ContactSuggestion>, onChosen: (Recipient) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        list.forEach { s ->
            val number = YasNumber.parse(s.phone) ?: return@forEach
            PersonRow(
                name = s.name ?: number.pretty(),
                number = if (s.name != null) number.pretty() else null,
                onClick = { onChosen(Recipient(number, s.name)) },
            )
        }
    }
}

// une personne : pastille avec l'initiale, nom en gros, numero en dessous
@Composable
private fun PersonRow(name: String, number: String?, onClick: () -> Unit, highlighted: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (highlighted) colors.primaryContainer else colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .heightIn(min = Sizes.listItemMinHeight)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Initial(name)
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (number != null) {
                    Text(number, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

// rond avec la premiere lettre du nom (plus facile a reperer qu'une icone toute pareille)
@Composable
private fun Initial(name: String) {
    val letter = name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "#"
    Box(
        Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

// sans la permission : on explique et on propose quand meme le selecteur Android
@Composable
private fun ContactsPermissionCard(onAllowed: () -> Unit, onPicked: (Recipient) -> Unit) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onAllowed() }
    val picker = rememberLauncherForActivityResult(PickPhoneContact()) { uri ->
        val picked = uri?.let { readPickedContact(context, it) } ?: return@rememberLauncherForActivityResult
        YasNumber.parse(picked.number)?.let { onPicked(Recipient(it, picked.name)) }
    }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(DesignR.drawable.ic_contacts), null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.ussd_contacts_title), style = MaterialTheme.typography.titleSmall)
            }
            Text(
                stringResource(R.string.ussd_contacts_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(onClick = { permission.launch(Manifest.permission.READ_CONTACTS) }) {
                    Text(stringResource(R.string.ussd_contacts_allow))
                }
                TextButton(onClick = { picker.launch(Unit) }) { Text(stringResource(R.string.ussd_pick_contact)) }
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
