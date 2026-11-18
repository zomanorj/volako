package mg.monargent.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mg.monargent.R
import mg.monargent.core.designsystem.component.AccountRow
import mg.monargent.core.designsystem.component.IconBadge
import mg.monargent.core.designsystem.component.LabelValueRow
import mg.monargent.core.designsystem.component.formatAriary
import mg.monargent.core.designsystem.theme.AmountTextStyle
import mg.monargent.core.designsystem.theme.MonArgentTheme
import mg.monargent.core.designsystem.theme.Spacing
import mg.monargent.core.model.Account
import mg.monargent.core.model.Category
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionType
import mg.monargent.ui.SmsPermission
import java.time.format.DateTimeFormatter
import mg.monargent.core.designsystem.R as DesignR

// Accueil (version simple de l'etape 3). La version complete arrive a l'etape 6.
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val syncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val newCount by viewModel.newCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // a chaque retour dans l'app, on va chercher les nouveaux SMS
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (SmsPermission.isGranted(context)) viewModel.sync()
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) }
                item { BalancesCard(state) }
                if (newCount > 0) {
                    item {
                        Text(
                            stringResource(R.string.home_new_operations, newCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MonArgentTheme.finance.income,
                        )
                    }
                }
                item { MonthCard(state) }
                item { Text(stringResource(R.string.home_operations), style = MaterialTheme.typography.titleMedium) }
                if (state.transactions.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.home_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.transactions, key = { it.id }) { TransactionRow(it) }
            }
        }
    }
}

@Composable
private fun BalancesCard(state: HomeUiState) {
    val onCard = MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(Spacing.xl)) {
            Text(stringResource(R.string.home_total), style = MaterialTheme.typography.labelLarge, color = onCard)
            Text(formatAriary(state.balances.total), style = AmountTextStyle.Hero, color = onCard)
            AccountRow(DesignR.drawable.ic_smartphone, stringResource(R.string.home_mvola), formatAriary(state.balances.mvola))
            AccountRow(DesignR.drawable.ic_payments, stringResource(R.string.home_cash), formatAriary(state.balances.cash))
            if (state.pendingCount > 0) {
                Text(
                    stringResource(R.string.home_pending_count, state.pendingCount),
                    color = MonArgentTheme.finance.leak,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.gapCount > 0) {
                Text(
                    stringResource(R.string.home_gaps_count, state.gapCount),
                    color = MonArgentTheme.finance.leak,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MonthCard(state: HomeUiState) {
    val f = MonArgentTheme.finance
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.home_month), style = MaterialTheme.typography.titleMedium)
            LabelValueRow(stringResource(R.string.home_income), formatAriary(state.month.income), valueColor = f.income)
            LabelValueRow(stringResource(R.string.home_expenses), formatAriary(state.month.expenses), valueColor = f.expense)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LabelValueRow(stringResource(R.string.home_credit), formatAriary(state.month.credit), valueColor = f.leak)
            LabelValueRow(stringResource(R.string.home_fees), formatAriary(state.month.fees), valueColor = f.leak)
        }
    }
}

@Composable
private fun TransactionRow(t: Transaction) {
    val f = MonArgentTheme.finance
    val (icon, color, sign) = when {
        t.type == TransactionType.INCOME -> Triple(DesignR.drawable.ic_south_west, f.income, "+")
        t.type == TransactionType.TRANSFER -> Triple(DesignR.drawable.ic_swap_horiz, f.transfer, "")
        t.category == Category.CREDIT -> Triple(DesignR.drawable.ic_sim_card, f.leak, "−")
        else -> Triple(DesignR.drawable.ic_north_east, f.expense, "−")
    }
    val title = when (t.type) {
        TransactionType.TRANSFER ->
            stringResource(if (t.toAccount == Account.CASH) R.string.home_withdrawal else R.string.home_deposit)
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
                Text(
                    stringResource(R.string.home_fee_suffix, formatAriary(t.fee)),
                    style = MaterialTheme.typography.bodySmall,
                    color = f.leak,
                )
            }
        }
    }
}

private val ROW_DATE = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")
