package mg.volako.ui.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mg.volako.R
import mg.volako.core.domain.ContactSearch
import mg.volako.core.designsystem.component.FilterChipItem
import mg.volako.core.designsystem.component.FilterChipRow
import mg.volako.core.designsystem.component.IconTile
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.designsystem.theme.VolakoTheme
import mg.volako.core.domain.OperationFilter
import mg.volako.core.model.Transaction
import mg.volako.ui.home.DayHeader
import mg.volako.ui.home.SwipeableTransactionRow
import mg.volako.core.designsystem.R as DesignR

// Onglet Operations : recherche + filtres + toute la liste groupee par jour (avec le total du jour)
@Composable
fun OperationsScreen(
    contentPadding: PaddingValues,
    onEdit: (Long) -> Unit,
    onDelete: (Transaction) -> Unit,
    viewModel: OperationsViewModel = viewModel(factory = OperationsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focus = LocalFocusManager.current

    // quand on change de filtre ou de recherche, on revient en haut de la liste
    LaunchedEffect(filter, query) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(stringResource(R.string.operations_title), style = MaterialTheme.typography.headlineMedium)
            SearchField(
                value = query,
                onValueChange = viewModel::setQuery,
                onSearch = { focus.clearFocus() },
            )
        }

        FilterChipRow(
            items = filterItems(state.counts),
            selectedKey = filter.name,
            onSelect = { key -> viewModel.setFilter(OperationFilter.valueOf(key)) },
            // un peu d'air sous les pastilles : la liste ne les touche pas en defilant
            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.sm),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
            ),
        ) {
            if (state.days.isEmpty() && !state.isEmptyDatabase) {
                item { NoResult() }
            }
            state.days.forEach { group ->
                item(key = "day-${group.day}") {
                    DayHeader(group.day, trailing = signed(group.net))
                }
                items(group.transactions, key = { it.id }) { t ->
                    SwipeableTransactionRow(
                        transaction = t,
                        contactName = ContactSearch.nameFor(state.contactNames, t.counterpartyPhone),
                        onEdit = { onEdit(t.id) },
                        onDelete = { onDelete(t) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

// champ de recherche en forme de pilule, avec une croix pour tout effacer
@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        placeholder = { Text(stringResource(R.string.ops_search_hint)) },
        leadingIcon = { Icon(painterResource(DesignR.drawable.ic_search), null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(painterResource(DesignR.drawable.ic_close), stringResource(R.string.ops_search_clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(26.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            // pas de trait sous le champ : la pilule suffit
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun filterItems(counts: Map<OperationFilter, Int>): List<FilterChipItem> {
    val f = VolakoTheme.finance
    @Composable
    fun item(filter: OperationFilter, label: Int, icon: Int? = null, tint: Color? = null) =
        FilterChipItem(filter.name, stringResource(label), icon, tint, counts[filter])
    return listOf(
        item(OperationFilter.ALL, R.string.ops_filter_all),
        item(OperationFilter.MVOLA, R.string.ops_filter_mvola, DesignR.drawable.ic_smartphone),
        item(OperationFilter.CASH, R.string.ops_filter_cash, DesignR.drawable.ic_payments),
        item(OperationFilter.INCOME, R.string.ops_filter_income, DesignR.drawable.ic_south_west, f.income),
        item(OperationFilter.CREDIT, R.string.ops_filter_credit, DesignR.drawable.ic_sim_card, f.leak),
        item(OperationFilter.FEES, R.string.ops_filter_fees, DesignR.drawable.ic_percent, f.leak),
    )
}

@Composable
private fun NoResult() {
    Column(
        Modifier.fillMaxWidth().padding(top = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        IconTile(DesignR.drawable.ic_search_off, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 64.dp)
        Text(stringResource(R.string.ops_no_result), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.ops_no_result_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// "+117 006 Ar" / "−4 250 Ar" (formatAriary met deja le moins)
private fun signed(net: Long) = (if (net > 0) "+" else "") + formatAriary(net)
