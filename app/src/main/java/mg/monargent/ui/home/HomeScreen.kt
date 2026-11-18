package mg.monargent.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
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
import mg.monargent.ui.entry.QuickEntrySheet
import mg.monargent.ui.entry.SavedEntry
import mg.monargent.ui.wallet.CountWalletDialog
import java.time.format.DateTimeFormatter
import mg.monargent.core.designsystem.R as DesignR

// Accueil. La version complete (glisser pour modifier, "A verifier"...) arrive a l'etape 6.
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val syncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val newCount by viewModel.newCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showEntry by rememberSaveable { mutableStateOf(false) }
    var showCount by rememberSaveable { mutableStateOf(false) }

    // a chaque retour dans l'app, on va chercher les nouveaux SMS (filet de securite
    // si le recepteur a rate un SMS)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (SmsPermission.isGranted(context)) viewModel.sync()
    }

    // pour ceux qui ont fait l'accueil avant l'etape 4 : on demande une fois
    // la reception des SMS et les notifications
    val liveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onLivePermissionsAsked()
    }
    LaunchedEffect(Unit) {
        val missing = SmsPermission.missingLive(context)
        if (SmsPermission.isGranted(context) && missing.isNotEmpty() && viewModel.shouldAskLivePermissions()) {
            liveLauncher.launch(missing)
        }
    }

    // apres une saisie : on ferme la feuille et on propose "Annuler" quelques secondes
    val onSaved = { entry: SavedEntry ->
        showEntry = false
        scope.launch {
            val text = resources.getString(
                if (entry.type == TransactionType.INCOME) R.string.entry_saved_income else R.string.entry_saved_expense,
                formatAriary(entry.amount),
                entry.label,
            )
            val result = snackbar.showSnackbar(
                message = text,
                actionLabel = resources.getString(R.string.entry_undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.delete(entry.id)
        }
        Unit
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEntry = true },
                icon = { Icon(painterResource(DesignR.drawable.ic_add), null) },
                text = { Text(stringResource(R.string.entry_add)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(
                // de la place en bas pour que le bouton Ajouter ne cache pas la derniere ligne
                contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) }
                item { BalancesCard(state, onCount = { showCount = true }) }
                if (state.balances.cash < 0) {
                    item { NegativeCashCard(state.balances.cash, onCount = { showCount = true }) }
                }
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

    if (showEntry) {
        QuickEntrySheet(onDismiss = { showEntry = false }, onSaved = onSaved)
    }
    if (showCount) {
        CountWalletDialog(
            expectedCash = state.balances.cash,
            onConfirm = { counted ->
                viewModel.countCash(counted)
                showCount = false
            },
            onDismiss = { showCount = false },
        )
    }
}

@Composable
private fun BalancesCard(state: HomeUiState, onCount: () -> Unit) {
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
            TextButton(onClick = onCount) {
                Icon(painterResource(DesignR.drawable.ic_account_balance_wallet), null, Modifier.size(18.dp))
                Text(stringResource(R.string.count_button), Modifier.padding(start = Spacing.sm))
            }
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

// le solde especes ne peut pas etre negatif en vrai : il manque des entrees
@Composable
private fun NegativeCashCard(cash: Long, onCount: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(DesignR.drawable.ic_payments, MonArgentTheme.finance.leak)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.count_negative, formatAriary(cash)), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onCount) { Text(stringResource(R.string.count_button)) }
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
    val title = when {
        t.type == TransactionType.TRANSFER ->
            stringResource(if (t.toAccount == Account.CASH) R.string.home_withdrawal else R.string.home_deposit)
        t.category == Category.UNTRACKED -> stringResource(
            if (t.type == TransactionType.INCOME) R.string.home_untracked_income else R.string.home_untracked_expense,
        )
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
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                // petite icone pour savoir de quel compte il s'agit
                Icon(
                    painterResource(if (t.account == Account.CASH) DesignR.drawable.ic_payments else DesignR.drawable.ic_smartphone),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    t.occurredAt.format(ROW_DATE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
