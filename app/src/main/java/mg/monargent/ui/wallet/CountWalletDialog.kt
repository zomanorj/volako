package mg.monargent.ui.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import mg.monargent.R
import mg.monargent.core.designsystem.component.formatAriary
import mg.monargent.core.designsystem.theme.Spacing
import mg.monargent.core.designsystem.R as DesignR

// On tape ce qu'on a vraiment en poche, l'app corrige l'ecart
@Composable
fun CountWalletDialog(
    expectedCash: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val counted = text.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(DesignR.drawable.ic_account_balance_wallet), null) },
        title = { Text(stringResource(R.string.count_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(stringResource(R.string.count_body, formatAriary(expectedCash)))
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter(Char::isDigit).take(9) },
                    label = { Text(stringResource(R.string.count_field)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { counted?.let(onConfirm) }, enabled = counted != null) {
                Text(stringResource(R.string.count_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.count_cancel)) }
        },
    )
}
