package mg.monargent.ui.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mg.monargent.R
import mg.monargent.core.data.SmsRecordResult
import mg.monargent.core.designsystem.component.AccountRow
import mg.monargent.core.designsystem.component.formatAriary
import mg.monargent.core.designsystem.theme.AmountTextStyle
import mg.monargent.core.designsystem.theme.MonArgentTheme
import mg.monargent.core.designsystem.theme.Spacing
import mg.monargent.core.model.Account
import mg.monargent.core.model.Category
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionType
import mg.monargent.core.sms.MvolaSmsParser
import mg.monargent.core.sms.ParsedSms
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import mg.monargent.core.designsystem.R as DesignR

// Ecran temporaire (etape 2) pour tester la base et les calculs sur le telephone.
// Sera remplace par le vrai accueil a l'etape 6.
@Composable
fun LabScreen(viewModel: LabViewModel = viewModel(factory = LabViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
    var body by rememberSaveable { mutableStateOf("") }
    val preview = remember(body) { if (body.isBlank()) null else MvolaSmsParser.parse(body, LocalDateTime.now()) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item { Text(stringResource(R.string.smstest_title), style = MaterialTheme.typography.headlineSmall) }
            item { BalancesCard(state) }
            item { MonthCard(state) }

            item {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it; viewModel.clearResult() },
                    placeholder = { Text(stringResource(R.string.smstest_hint)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = {
                        scope.launch {
                            clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.let {
                                body = it.toString()
                                viewModel.clearResult()
                            }
                        }
                    }) {
                        Icon(painterResource(DesignR.drawable.ic_content_paste), null, Modifier.size(18.dp))
                        Text(stringResource(R.string.smstest_paste), Modifier.padding(start = Spacing.sm))
                    }
                    Button(onClick = { viewModel.record(body) }, enabled = preview != null && preview != ParsedSms.Ignored) {
                        Text(stringResource(R.string.lab_save))
                    }
                }
            }
            lastResult?.let { result -> item { ResultText(result) } }
            preview?.let { item { SmsPreview(it) } }

            item { Text(stringResource(R.string.lab_operations), style = MaterialTheme.typography.titleMedium) }
            if (state.transactions.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.lab_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.transactions, key = { it.id }) { t ->
                TransactionRow(t, onDelete = { viewModel.delete(t.id) })
            }
        }
    }
}

@Composable
private fun BalancesCard(state: LabUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(Spacing.xl)) {
            Text(stringResource(R.string.lab_total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(formatAriary(state.balances.total), style = AmountTextStyle.Hero, color = MaterialTheme.colorScheme.onPrimaryContainer)
            AccountRow(DesignR.drawable.ic_smartphone, stringResource(R.string.lab_mvola), formatAriary(state.balances.mvola))
            AccountRow(DesignR.drawable.ic_payments, stringResource(R.string.lab_cash), formatAriary(state.balances.cash))
            if (state.pendingCount > 0) {
                Text(stringResource(R.string.lab_pending_count, state.pendingCount), color = MonArgentTheme.finance.leak, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.gapCount > 0) {
                Text(stringResource(R.string.lab_gaps_count, state.gapCount), color = MonArgentTheme.finance.leak, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MonthCard(state: LabUiState) {
    val f = MonArgentTheme.finance
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.lab_month), style = MaterialTheme.typography.titleMedium)
            InfoRow(R.string.lab_income, formatAriary(state.month.income), f.income)
            InfoRow(R.string.lab_expenses, formatAriary(state.month.expenses), f.expense)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(R.string.lab_credit, formatAriary(state.month.credit), f.leak)
            InfoRow(R.string.lab_fees, formatAriary(state.month.fees), f.leak)
        }
    }
}

@Composable
private fun ResultText(result: SmsRecordResult) {
    val (text, color) = when (result) {
        SmsRecordResult.ADDED -> R.string.lab_result_added to MonArgentTheme.finance.income
        SmsRecordResult.DUPLICATE -> R.string.lab_result_duplicate to MonArgentTheme.finance.leak
        SmsRecordResult.PENDING -> R.string.lab_result_pending to MonArgentTheme.finance.leak
        SmsRecordResult.IGNORED -> R.string.lab_result_ignored to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(stringResource(text), color = color, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun TransactionRow(t: Transaction, onDelete: () -> Unit) {
    val f = MonArgentTheme.finance
    val (icon, color, sign) = when {
        t.type == TransactionType.INCOME -> Triple(DesignR.drawable.ic_south_west, f.income, "+")
        t.type == TransactionType.TRANSFER -> Triple(DesignR.drawable.ic_swap_horiz, f.transfer, "")
        t.category == Category.CREDIT -> Triple(DesignR.drawable.ic_sim_card, f.leak, "−")
        else -> Triple(DesignR.drawable.ic_north_east, f.expense, "−")
    }
    val title = when (t.type) {
        TransactionType.TRANSFER -> stringResource(if (t.toAccount == Account.CASH) R.string.lab_withdrawal else R.string.lab_deposit)
        else -> t.label ?: t.counterpartyName ?: "—"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        IconBadge(icon, color)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                t.occurredAt.format(ROW_DATE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(sign + formatAriary(t.amount), style = AmountTextStyle.Regular, color = color)
            if (t.fee > 0) {
                Text(stringResource(R.string.lab_fee_suffix, formatAriary(t.fee)), style = MaterialTheme.typography.bodySmall, color = f.leak)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(painterResource(DesignR.drawable.ic_delete), stringResource(R.string.lab_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val ROW_DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm")
