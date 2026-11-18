package mg.monargent.ui.month

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mg.monargent.R
import mg.monargent.core.designsystem.component.LabelValueRow
import mg.monargent.core.designsystem.component.formatAriary
import mg.monargent.core.designsystem.theme.AmountTextStyle
import mg.monargent.core.designsystem.theme.MonArgentTheme
import mg.monargent.core.designsystem.theme.Spacing
import mg.monargent.core.domain.MonthReports
import mg.monargent.core.model.MonthReport
import mg.monargent.core.model.SpendingItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import mg.monargent.core.designsystem.R as DesignR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    onBack: () -> Unit,
    viewModel: MonthViewModel = viewModel(factory = MonthViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    // l'utilisateur choisit ou enregistrer le fichier (Telechargements, Drive...)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            viewModel.export(uri) { count ->
                scope.launch {
                    snackbar.showSnackbar(
                        if (count != null) {
                            resources.getQuantityString(R.plurals.export_done, count, count)
                        } else {
                            resources.getString(R.string.export_failed)
                        },
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.month_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(DesignR.drawable.ic_arrow_back), stringResource(R.string.review_back))
                    }
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("mon-argent-${LocalDate.now()}.csv") }) {
                        Icon(painterResource(DesignR.drawable.ic_download), stringResource(R.string.export_button))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val report = state.report
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item { MonthSwitcher(state, onPrevious = viewModel::previous, onNext = viewModel::next) }
            item { NetCard(report) }
            item { LeaksCard(report) }
            item { Text(stringResource(R.string.month_where), style = MaterialTheme.typography.titleMedium) }
            if (report.topSpending.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.month_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val max = report.topSpending.maxOf { it.total }
                items(report.topSpending, key = { it.label + it.isOther }) { SpendingBar(it, max) }
            }
        }
    }
}

// "< Novembre 2026 >"
@Composable
private fun MonthSwitcher(state: MonthUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, enabled = state.hasPrevious) {
            Icon(painterResource(DesignR.drawable.ic_chevron_left), stringResource(R.string.month_previous))
        }
        Text(
            state.report.month.format(MONTH_FORMAT).replaceFirstChar { it.titlecase(Locale.FRENCH) },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = state.hasNext) {
            Icon(painterResource(DesignR.drawable.ic_chevron_right), stringResource(R.string.month_next))
        }
    }
}

@Composable
private fun NetCard(report: MonthReport) {
    val f = MonArgentTheme.finance
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                stringResource(R.string.month_net),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                (if (report.net > 0) "+" else "") + formatAriary(report.net),
                style = AmountTextStyle.Hero,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            LabelValueRow(stringResource(R.string.home_income), formatAriary(report.summary.income), valueColor = f.income)
            LabelValueRow(stringResource(R.string.home_expenses), formatAriary(report.summary.expenses), valueColor = f.expense)
        }
    }
}

// credit + frais : les deux depenses qu'on ne voit pas passer
@Composable
private fun LeaksCard(report: MonthReport) {
    val f = MonArgentTheme.finance
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.month_leaks), style = MaterialTheme.typography.titleMedium)
            LabelValueRow(stringResource(R.string.home_credit), formatAriary(report.summary.credit), valueColor = f.leak)
            if (report.creditPurchases > 0) {
                Text(
                    pluralStringResource(
                        R.plurals.month_credit_detail,
                        report.creditPurchases,
                        report.creditPurchases,
                        formatAriary(report.summary.credit / report.creditPurchases),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = Spacing.xs))
            LabelValueRow(stringResource(R.string.home_fees), formatAriary(report.summary.fees), valueColor = f.leak)
        }
    }
}

// Barre horizontale : une seule serie, donc une seule couleur et pas de legende.
// Le montant est ecrit en texte a cote (l'info ne passe jamais que par la couleur).
@Composable
private fun SpendingBar(item: SpendingItem, max: Long) {
    val label = spendingLabel(item)
    val times = pluralStringResource(R.plurals.month_times, item.count, item.count)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "$label, ${formatAriary(item.total)}, $times" },
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(times, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatAriary(item.total), style = AmountTextStyle.Regular)
        }
        // fond discret + barre fine aux bouts arrondis, longueur proportionnelle au plus gros poste
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction = (item.total.toFloat() / max).coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MonArgentTheme.finance.expenseMark),
            )
        }
    }
}

@Composable
private fun spendingLabel(item: SpendingItem): String = when {
    item.isOther -> stringResource(R.string.month_item_other)
    item.label == MonthReports.CREDIT -> stringResource(R.string.month_item_credit)
    item.label == MonthReports.FEES -> stringResource(R.string.month_item_fees)
    item.label == MonthReports.UNTRACKED -> stringResource(R.string.month_item_untracked)
    item.label == MonthReports.UNNAMED -> stringResource(R.string.month_item_unnamed)
    else -> item.label
}

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)
