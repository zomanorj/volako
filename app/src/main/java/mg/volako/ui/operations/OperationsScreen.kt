package mg.volako.ui.operations

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mg.volako.R
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.model.Transaction
import mg.volako.ui.home.DayHeader
import mg.volako.ui.home.HomeViewModel
import mg.volako.ui.home.SwipeableTransactionRow

// Onglet Operations : toute la liste, groupee par jour.
// (recherche, filtres et total par jour : etape 3 de la refonte)
@Composable
fun OperationsScreen(
    contentPadding: PaddingValues,
    onEdit: (Long) -> Unit,
    onDelete: (Transaction) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.xl,
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
    ) {
        item {
            Text(
                stringResource(R.string.operations_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = Spacing.sm),
            )
        }
        if (state.days.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.days.forEach { (day, transactions) ->
            item(key = "day-$day") { DayHeader(day) }
            items(transactions, key = { it.id }) { t ->
                SwipeableTransactionRow(
                    transaction = t,
                    onEdit = { onEdit(t.id) },
                    onDelete = { onDelete(t) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}
