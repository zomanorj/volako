package mg.monargent.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import mg.monargent.R
import mg.monargent.core.designsystem.component.LabelValueRow
import mg.monargent.core.designsystem.component.formatAriary
import mg.monargent.core.designsystem.theme.Spacing
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionSource
import mg.monargent.core.model.TransactionType
import java.time.format.DateTimeFormatter

// Modifier une operation. Le montant d'un SMS ne se change pas (c'est MVola qui fait foi),
// seulement son libelle.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: Transaction,
    onSave: (Transaction, rememberForPhone: Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isManual = transaction.source == TransactionSource.MANUAL
    var label by rememberSaveable { mutableStateOf(transaction.label.orEmpty()) }
    var amountText by rememberSaveable { mutableStateOf(transaction.amount.toString()) }
    // on propose de retenir le libelle seulement pour un envoi/reception avec un numero
    val canRemember = !isManual && transaction.counterpartyPhone != null && transaction.type != TransactionType.TRANSFER
    var remember by rememberSaveable { mutableStateOf(canRemember) }
    val amount = if (isManual) amountText.toLongOrNull() else transaction.amount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.edit_title), style = MaterialTheme.typography.titleLarge)
            Text(
                transaction.occurredAt.format(DATE),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isManual) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(9) },
                    label = { Text(stringResource(R.string.edit_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LabelValueRow(stringResource(R.string.edit_from_sms), formatAriary(transaction.amount))
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = { Text(stringResource(R.string.edit_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            if (canRemember) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remember, onCheckedChange = { remember = it })
                    Text(
                        stringResource(R.string.edit_remember, transaction.counterpartyName ?: transaction.counterpartyPhone.orEmpty()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.edit_delete)) }
                Button(
                    onClick = {
                        val updated = transaction.copy(label = label.trim().ifEmpty { null }, amount = amount ?: transaction.amount)
                        onSave(updated, remember && canRemember)
                    },
                    enabled = amount != null && amount > 0,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.edit_save)) }
            }
        }
    }
}

private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
