package mg.volako.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import mg.volako.ui.SmsPermission
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mg.volako.R
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionType
import mg.volako.ui.edit.EditTransactionSheet
import mg.volako.ui.entry.QuickEntrySheet
import mg.volako.ui.entry.SavedEntry
import mg.volako.ui.home.HomeScreen
import mg.volako.ui.home.HomeViewModel
import mg.volako.ui.month.MonthScreen
import mg.volako.ui.mvola.MvolaEntry
import mg.volako.ui.mvola.MvolaScreen
import mg.volako.ui.operations.OperationsScreen
import mg.volako.ui.review.ReviewScreen
import mg.volako.ui.wallet.CountWalletDialog
import mg.volako.core.designsystem.R as DesignR

// Les ecrans. Serializable : la pile survit si Android tue l'app en arriere-plan
@Serializable
data object HomeKey : NavKey

@Serializable
data object OperationsKey : NavKey

@Serializable
data object MonthKey : NavKey

@Serializable
data object MvolaKey : NavKey

@Serializable
data object ReviewKey : NavKey

// les 4 onglets de la barre du bas
private enum class Tab(val key: NavKey, @StringRes val label: Int, @DrawableRes val icon: Int) {
    HOME(HomeKey, R.string.nav_home, DesignR.drawable.ic_home),
    OPERATIONS(OperationsKey, R.string.nav_operations, DesignR.drawable.ic_format_list_bulleted),
    SUMMARY(MonthKey, R.string.nav_summary, DesignR.drawable.ic_bar_chart),
    MVOLA(MvolaKey, R.string.nav_mvola, DesignR.drawable.ic_send_to_mobile),
}

// Le cadre de l'app : barre d'onglets + bouton Ajouter + fenetres partagees (saisie, modification,
// comptage) + messages "Annuler". Les ecrans ne font qu'afficher et appeler ces actions.
@Composable
fun VolakoApp(
    entry: MvolaEntry? = null,
    onEntryHandled: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val backStack = rememberNavBackStack(HomeKey)
    val current = backStack.lastOrNull()
    val currentTab = Tab.entries.firstOrNull { it.key == current }

    val state by homeViewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var showEntry by rememberSaveable { mutableStateOf(false) }
    var showCount by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    // a chaque retour dans l'app (quel que soit l'onglet) : on lit les nouveaux SMS (filet de securite)
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (SmsPermission.isGranted(context)) homeViewModel.sync()
    }
    // pour ceux qui ont fait l'accueil avant la detection en direct : on demande une fois
    val liveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        homeViewModel.onLivePermissionsAsked()
    }
    LaunchedEffect(Unit) {
        val missing = SmsPermission.missingLive(context)
        if (SmsPermission.isGranted(context) && missing.isNotEmpty() && homeViewModel.shouldAskLivePermissions()) {
            liveLauncher.launch(missing)
        }
    }

    // un onglet = Accueil + cet onglet (le retour ramene toujours a l'accueil)
    fun select(tab: Tab) {
        backStack.clear()
        backStack.add(HomeKey)
        if (tab != Tab.HOME) backStack.add(tab.key)
    }

    // arrivee par un raccourci de l'icone : on va sur l'onglet MVola, l'ecran ouvre la bonne fenetre
    LaunchedEffect(entry) {
        if (entry != null && currentTab != Tab.MVOLA) select(Tab.MVOLA)
    }

    fun showUndo(message: String, onUndo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, resources.getString(R.string.entry_undo), duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    val deleteWithUndo = { t: Transaction ->
        homeViewModel.delete(t.id)
        showUndo(resources.getString(R.string.home_deleted)) { homeViewModel.restore(t) }
    }

    val onSaved = { entry: SavedEntry ->
        showEntry = false
        val text = resources.getString(
            if (entry.type == TransactionType.INCOME) R.string.entry_saved_income else R.string.entry_saved_expense,
            formatAriary(entry.amount),
            entry.label,
        )
        showUndo(text) { homeViewModel.delete(entry.id) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        // "Ajouter" sur l'accueil et les operations (pas sur le resume ni "a verifier")
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentTab == Tab.HOME || currentTab == Tab.OPERATIONS,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showEntry = true },
                    icon = { Icon(painterResource(DesignR.drawable.ic_add), null) },
                    text = { Text(stringResource(R.string.entry_add)) },
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = currentTab != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { select(tab) },
                            icon = { Icon(painterResource(tab.icon), null) },
                            label = { Text(stringResource(tab.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        // le cadre ne gere que le bas (barre d'onglets / barre de navigation du telephone).
        // Le haut (barre d'etat) est gere par chaque ecran.
        contentWindowInsets = WindowInsets.navigationBars,
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.consumeWindowInsets(padding),
            entryProvider = entryProvider {
                entry<HomeKey> {
                    HomeScreen(
                        contentPadding = padding,
                        onOpenReview = { backStack.add(ReviewKey) },
                        onOpenSummary = { select(Tab.SUMMARY) },
                        onOpenOperations = { select(Tab.OPERATIONS) },
                        onEdit = { editingId = it },
                        onDelete = deleteWithUndo,
                        onCount = { showCount = true },
                    )
                }
                entry<OperationsKey> {
                    OperationsScreen(contentPadding = padding, onEdit = { editingId = it }, onDelete = deleteWithUndo)
                }
                entry<MonthKey> { MonthScreen(modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) }
                entry<MvolaKey> {
                    MvolaScreen(
                        contentPadding = padding,
                        mvolaBalance = state.balances.mvola,
                        entry = entry,
                        onEntryHandled = onEntryHandled,
                    )
                }
                entry<ReviewKey> { ReviewScreen(onBack = { backStack.removeLastOrNull() }) }
            },
        )
    }

    if (showEntry) QuickEntrySheet(onDismiss = { showEntry = false }, onSaved = onSaved)
    if (showCount) {
        CountWalletDialog(
            expectedCash = state.balances.cash,
            onConfirm = { counted ->
                homeViewModel.countCash(counted)
                showCount = false
            },
            onDismiss = { showCount = false },
        )
    }
    // on retrouve l'operation par son id (elle peut avoir change entre temps)
    val editing = editingId?.let(homeViewModel::find)
    if (editing != null) {
        EditTransactionSheet(
            transaction = editing,
            onSave = { updated, rememberForPhone ->
                homeViewModel.update(updated, rememberForPhone)
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
