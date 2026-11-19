package mg.volako.ui.mvola

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mg.volako.R
import mg.volako.core.designsystem.component.LabelValueRow
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.Sizes
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.ussd.MvolaUssd
import mg.volako.core.ussd.NextOnPhone
import mg.volako.core.ussd.UssdError
import mg.volako.core.ussd.UssdRequest
import mg.volako.core.designsystem.R as DesignR

// Le resume affiche juste avant de lancer : l'utilisateur relit, puis "Lancer"
@Composable
fun UssdConfirm(
    rows: List<Pair<String, String>>,
    request: UssdRequest,
    onLaunch: () -> Unit,
    onEdit: () -> Unit,
    launchEnabled: Boolean = true,
    // reglages modifiables juste avant de lancer (ex : frais de retrait, description)
    options: @Composable () -> Unit = {},
    // null = pas de bouton favori (ex : on regarde deja un favori)
    onSaveFavorite: (() -> Unit)? = null,
    editLabel: String = stringResource(R.string.ussd_edit),
    footer: @Composable () -> Unit = {},
) {
    // le code change (montant, description...) : le bouton favori redevient actif
    var saved by rememberSaveable(request.code) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(stringResource(R.string.ussd_confirm_title), style = MaterialTheme.typography.titleLarge)

        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                rows.forEach { (label, value) -> LabelValueRow(label, value) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // le code reste visible : l'utilisateur peut verifier ou le taper lui-meme
                LabelValueRow(stringResource(R.string.ussd_row_code), request.code)
            }
        }

        options()

        // le rappel n'a de sens que si on peut lancer (sinon il decrit un code qui ne partira pas)
        if (launchEnabled) {
            when (request.next) {
                NextOnPhone.SECRET_CODE -> SecretNote(stringResource(R.string.ussd_next_secret))
                NextOnPhone.DESCRIPTION -> SecretNote(stringResource(R.string.ussd_next_description))
                NextOnPhone.MENU, NextOnPhone.RESULT -> Unit
            }
        }

        Button(onClick = onLaunch, enabled = launchEnabled, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(painterResource(DesignR.drawable.ic_dialpad), null, Modifier.size(20.dp))
            Text(stringResource(R.string.ussd_launch), Modifier.padding(start = Spacing.sm))
        }
        if (onSaveFavorite != null) {
            OutlinedButton(
                onClick = {
                    onSaveFavorite()
                    saved = true
                },
                enabled = launchEnabled && !saved,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(
                    painterResource(if (saved) DesignR.drawable.ic_bookmark_added else DesignR.drawable.ic_star),
                    null,
                    Modifier.size(18.dp),
                )
                Text(
                    stringResource(if (saved) R.string.fav_saved else R.string.fav_add),
                    Modifier.padding(start = Spacing.sm),
                )
            }
        }
        TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text(editLabel) }
        footer()
    }
}

// petit rappel avec un cadenas : le code secret reste dans la fenetre Yas
@Composable
fun SecretNote(text: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Top) {
        Icon(
            painterResource(DesignR.drawable.ic_lock),
            null,
            Modifier.size(Sizes.icon * 0.8f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// message a afficher quand le moteur refuse de fabriquer le code
@Composable
fun ussdErrorText(error: UssdError): String = when (error) {
    UssdError.INVALID_NUMBER -> stringResource(R.string.ussd_error_number)
    UssdError.AMOUNT_TOO_SMALL -> stringResource(R.string.ussd_error_small, formatAriary(MvolaUssd.MIN_AMOUNT))
    UssdError.AMOUNT_TOO_BIG -> stringResource(R.string.ussd_error_big, formatAriary(MvolaUssd.MAX_AMOUNT))
    UssdError.DESCRIPTION_NOT_DIGITS -> stringResource(R.string.ussd_error_desc_digits)
    UssdError.DESCRIPTION_TOO_LONG -> stringResource(R.string.ussd_error_desc_long)
}
