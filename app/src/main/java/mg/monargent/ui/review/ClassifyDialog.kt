package mg.monargent.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import mg.monargent.R
import mg.monargent.core.designsystem.theme.Spacing
import mg.monargent.core.model.PendingSms
import mg.monargent.core.data.ClassifyChoice
import mg.monargent.core.sms.GenericSmsReader
import mg.monargent.core.sms.MvolaSmsParser
import mg.monargent.core.sms.OperationKind

// Transformer un SMS non reconnu en operation MVola : type, montant (pre-rempli), libelle
@Composable
fun ClassifyDialog(
    sms: PendingSms,
    onConfirm: (ClassifyChoice, Long, String) -> Unit,
    onDismiss: () -> Unit,
) {
    // proposition du lecteur universel (montant, sens, personne) : il reste a confirmer
    val clues = remember(sms.body) { GenericSmsReader.read(sms.body) }
    val suggestedType = when (clues?.hint) {
        OperationKind.RECEIVED, OperationKind.DEPOSIT -> ClassifyChoice.INCOME
        OperationKind.WITHDRAWAL -> ClassifyChoice.WITHDRAWAL
        else -> ClassifyChoice.EXPENSE
    }
    var type by rememberSaveable { mutableStateOf(suggestedType) }
    var amountText by rememberSaveable {
        mutableStateOf((clues?.amount ?: MvolaSmsParser.firstAmount(sms.body))?.toString().orEmpty())
    }
    var label by rememberSaveable { mutableStateOf(clues?.name.orEmpty()) }
    val amount = amountText.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.classify_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                val types = listOf(
                    ClassifyChoice.EXPENSE to R.string.entry_expense,
                    ClassifyChoice.INCOME to R.string.entry_income,
                    ClassifyChoice.WITHDRAWAL to R.string.classify_withdrawal,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    types.forEachIndexed { index, (t, text) ->
                        SegmentedButton(
                            selected = type == t,
                            onClick = { type = t },
                            shape = SegmentedButtonDefaults.itemShape(index, types.size),
                        ) { Text(stringResource(text)) }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(9) },
                    label = { Text(stringResource(R.string.edit_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(40) },
                    label = { Text(stringResource(R.string.edit_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text(
                    stringResource(R.string.classify_learn_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let { onConfirm(type, it, label) } },
                enabled = amount != null && amount > 0,
            ) { Text(stringResource(R.string.classify_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.count_cancel)) } },
    )
}
