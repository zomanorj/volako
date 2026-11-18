package mg.volako.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mg.volako.R
import mg.volako.core.designsystem.component.NumericKeypad
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.AmountTextStyle
import mg.volako.core.designsystem.theme.VolakoTheme
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.model.TransactionType
import mg.volako.core.designsystem.R as DesignR

// Saisie d'une depense ou entree en especes : montant au pave, puis 1 touche sur un libelle = enregistre
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickEntrySheet(
    onDismiss: () -> Unit,
    onSaved: (SavedEntry) -> Unit,
    viewModel: QuickEntryViewModel = viewModel(factory = QuickEntryViewModel.Factory),
) {
    val type by viewModel.type.collectAsStateWithLifecycle()
    val frequent by viewModel.frequentLabels.collectAsStateWithLifecycle()
    var digits by rememberSaveable { mutableStateOf("") }
    var customMode by rememberSaveable { mutableStateOf(false) }
    var customLabel by rememberSaveable { mutableStateOf("") }

    val amount = digits.toLongOrNull() ?: 0L
    val defaults = stringArrayResource(
        if (type == TransactionType.EXPENSE) R.array.entry_default_expense_labels else R.array.entry_default_income_labels,
    )
    // libelles frequents d'abord, completes par ceux par defaut
    val labels = (frequent + defaults).distinct().take(QuickEntryViewModel.MAX_LABELS)
    val color = if (type == TransactionType.EXPENSE) VolakoTheme.finance.expense else VolakoTheme.finance.income

    val save = { label: String ->
        if (amount > 0 && label.isNotBlank()) {
            viewModel.save(amount, label) { onSaved(it) }
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            val types = listOf(TransactionType.EXPENSE to R.string.entry_expense, TransactionType.INCOME to R.string.entry_income)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                types.forEachIndexed { index, (t, label) ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = { viewModel.setType(t) },
                        shape = SegmentedButtonDefaults.itemShape(index, types.size),
                    ) { Text(stringResource(label)) }
                }
            }

            Text(
                text = (if (type == TransactionType.EXPENSE && amount > 0) "−" else "") + formatAriary(amount),
                style = AmountTextStyle.Hero,
                color = if (amount > 0) color else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.entry_cash_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            if (customMode) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it.take(40) },
                        label = { Text(stringResource(R.string.entry_other_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { save(customLabel) }),
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { save(customLabel) }, enabled = amount > 0 && customLabel.isNotBlank()) {
                        Text(stringResource(R.string.entry_save))
                    }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    labels.forEach { label ->
                        SuggestionChip(onClick = { save(label) }, label = { Text(label) }, enabled = amount > 0)
                    }
                    SuggestionChip(
                        onClick = { customMode = true },
                        label = { Text(stringResource(R.string.entry_other)) },
                        icon = { Icon(painterResource(DesignR.drawable.ic_edit), null, Modifier.size(18.dp)) },
                    )
                }
            }

            NumericKeypad(
                onDigits = { if ((digits + it).trimStart('0').length <= MAX_DIGITS) digits = (digits + it).trimStart('0') },
                onBackspace = { digits = digits.dropLast(1) },
                backspaceDescription = stringResource(R.string.entry_backspace),
            )
        }
    }
}

// 999 999 999 Ar max, largement suffisant
private const val MAX_DIGITS = 9
