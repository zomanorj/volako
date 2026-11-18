package mg.monargent.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mg.monargent.R
import mg.monargent.core.designsystem.component.formatAriary
import mg.monargent.core.designsystem.theme.Spacing
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionType
import mg.monargent.ui.SmsPermission
import mg.monargent.ui.edit.EditTransactionSheet
import mg.monargent.ui.entry.QuickEntrySheet
import mg.monargent.ui.entry.SavedEntry
import mg.monargent.ui.wallet.CountWalletDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import mg.monargent.core.designsystem.R as DesignR

@Composable
fun HomeScreen(
    onOpenReview: () -> Unit,
    onOpenMonth: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val syncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showEntry by rememberSaveable { mutableStateOf(false) }
    var showCount by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    // a chaque retour dans l'app, on va chercher les nouveaux SMS (filet de securite)
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

    // message en bas avec "Annuler"
    fun showUndo(message: String, onUndo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = resources.getString(R.string.entry_undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    val onSaved = { entry: SavedEntry ->
        showEntry = false
        val text = resources.getString(
            if (entry.type == TransactionType.INCOME) R.string.entry_saved_income else R.string.entry_saved_expense,
            formatAriary(entry.amount),
            entry.label,
        )
        showUndo(text) { viewModel.delete(entry.id) }
    }

    val deleteWithUndo = { t: Transaction ->
        viewModel.delete(t.id)
        showUndo(resources.getString(R.string.home_deleted)) { viewModel.restore(t) }
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
            ) {
                item { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) }
                item { Spacer(Spacing.lg) }
                item { BalancesCard(state, onCount = { showCount = true }) }
                if (state.formatChangeAt != null) {
                    item { Spacer(Spacing.md) }
                    item { FormatAlertCard(onDismiss = viewModel::dismissFormatAlert) }
                }
                if (state.pendingCount > 0 || state.gapCount > 0) {
                    item { Spacer(Spacing.md) }
                    item { ReviewCard(state.pendingCount, state.gapCount, onOpenReview) }
                }
                if (state.balances.cash < 0) {
                    item { Spacer(Spacing.md) }
                    item { NegativeCashCard(state.balances.cash, onCount = { showCount = true }) }
                }
                item { Spacer(Spacing.md) }
                item { MonthCard(state, onOpen = onOpenMonth) }

                if (state.days.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.home_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.xl),
                        )
                    }
                }
                state.days.forEach { (day, transactions) ->
                    item(key = "day-$day") { DayHeader(day) }
                    items(transactions, key = { it.id }) { t ->
                        SwipeableTransactionRow(
                            transaction = t,
                            onEdit = { editingId = t.id },
                            onDelete = { deleteWithUndo(t) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
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
    // on retrouve l'operation par son id (elle peut avoir change entre temps)
    val editing = editingId?.let { id -> state.days.firstNotNullOfOrNull { (_, list) -> list.find { it.id == id } } }
    if (editing != null) {
        EditTransactionSheet(
            transaction = editing,
            onSave = { updated, rememberForPhone ->
                viewModel.update(updated, rememberForPhone)
                editingId = null
            },
            onDelete = {
                editingId = null
                deleteWithUndo(editing)
            },
            onDismiss = { editingId = null },
        )
    }
}

@Composable
private fun Spacer(height: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = height))
}

// "Aujourd'hui", "Hier", puis "Lundi 16 novembre"
@Composable
private fun DayHeader(day: LocalDate) {
    val today = LocalDate.now()
    val text = when (day) {
        today -> stringResource(R.string.home_today)
        today.minusDays(1) -> stringResource(R.string.home_yesterday)
        // l'annee seulement si ce n'est pas l'annee en cours
        else -> day.format(if (day.year == today.year) DAY_FORMAT else DAY_YEAR_FORMAT)
            .replaceFirstChar { it.titlecase(Locale.FRENCH) }
    }
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xl, bottom = Spacing.xs),
    )
}

private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
private val DAY_YEAR_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
