package mg.volako.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mg.volako.R
import mg.volako.core.domain.ContactSearch
import mg.volako.core.designsystem.component.SectionHeader
import mg.volako.core.designsystem.component.StatTile
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.AmountTextStyle
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.model.Transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import mg.volako.core.designsystem.R as DesignR

// Onglet Accueil : l'essentiel en un coup d'oeil. Le detail est dans les onglets Operations et Resume.
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenReview: () -> Unit,
    onOpenSummary: () -> Unit,
    onOpenOperations: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Transaction) -> Unit,
    onCount: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val syncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth().statusBarsPadding())
        LazyColumn(
            modifier = Modifier.statusBarsPadding(),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.xl,
                // de la place pour le bouton Ajouter au-dessus de la barre d'onglets
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            item { Header() }
            item { Total(state) }
            item { AccountTiles(state, onCount) }

            // les alertes, en bandeaux compacts, seulement s'il y en a
            if (state.formatChangeAt != null) item { FormatAlertCard(onDismiss = viewModel::dismissFormatAlert) }
            if (state.pendingCount > 0 || state.gapCount > 0) {
                item { ReviewCard(state.pendingCount, state.gapCount, onOpenReview) }
            }
            if (state.balances.cash < 0) item { NegativeCashCard(state.balances.cash, onCount) }

            item { MonthCard(state, onOpen = onOpenSummary) }

            item {
                Column {
                    SectionHeader(
                        title = stringResource(R.string.home_latest),
                        actionLabel = stringResource(R.string.home_see_all).takeIf { state.latest.isNotEmpty() },
                        onAction = onOpenOperations,
                    )
                    if (state.latest.isEmpty()) {
                        Text(
                            stringResource(R.string.home_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.latest.forEach { t ->
                        SwipeableTransactionRow(
                            transaction = t,
                            contactName = ContactSearch.nameFor(state.contactNames, t.counterpartyPhone),
                            onEdit = { onEdit(t.id) },
                            onDelete = { onDelete(t) },
                        )
                    }
                }
            }
        }
    }
}

// "Volako" + la date du jour
@Composable
private fun Header() {
    Column {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Text(
            LocalDate.now().format(TODAY).replaceFirstChar { it.titlecase(Locale.FRENCH) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// le chiffre le plus important de l'app, en tres grand
@Composable
private fun Total(state: HomeUiState) {
    Column {
        Text(
            stringResource(R.string.home_available),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(formatAriary(state.balances.total), style = AmountTextStyle.Hero)
    }
}

// MVola et Especes cote a cote. "Compter" directement sur la tuile Especes.
@Composable
private fun AccountTiles(state: HomeUiState, onCount: () -> Unit) {
    // les deux tuiles ont toujours la meme hauteur (alignement net)
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        StatTile(
            label = stringResource(R.string.home_mvola),
            value = formatAriary(state.balances.mvola),
            icon = DesignR.drawable.ic_smartphone,
            valueStyle = AmountTextStyle.Large,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            label = stringResource(R.string.home_cash),
            value = formatAriary(state.balances.cash),
            icon = DesignR.drawable.ic_payments,
            valueStyle = AmountTextStyle.Large,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            action = {
                OutlinedButton(onClick = onCount, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Text(stringResource(R.string.count_short), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

private val TODAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
