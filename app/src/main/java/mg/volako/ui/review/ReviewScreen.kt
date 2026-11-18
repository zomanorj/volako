package mg.volako.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mg.volako.R
import mg.volako.core.designsystem.component.IconTile
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.VolakoTheme
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.model.BalanceGap
import mg.volako.core.model.PendingSms
import java.time.format.DateTimeFormatter
import mg.volako.core.designsystem.R as DesignR

// "A verifier" : SMS MVola pas compris + ecarts de solde
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var classifyingId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(DesignR.drawable.ic_arrow_back), stringResource(R.string.review_back))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.pending.isEmpty() && state.gaps.isEmpty()) {
            AllGood(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.pending.isNotEmpty()) {
                item { SectionTitle(R.string.review_unknown_title, R.string.review_unknown_hint) }
                items(state.pending, key = { "sms-${it.id}" }) { sms ->
                    PendingCard(sms, onIgnore = { viewModel.ignore(sms) }, onClassify = { classifyingId = sms.id })
                }
            }
            if (state.gaps.isNotEmpty()) {
                item { SectionTitle(R.string.review_gaps_title, R.string.review_gap_hint) }
                items(state.gaps, key = { "gap-${it.key}" }) { gap ->
                    GapCard(gap, onOk = { viewModel.acknowledge(gap) })
                }
            }
        }
    }

    val classifying = classifyingId?.let { id -> state.pending.find { it.id == id } }
    if (classifying != null) {
        ClassifyDialog(
            sms = classifying,
            onConfirm = { type, amount, label ->
                viewModel.classify(classifying, type, amount, label)
                classifyingId = null
            },
            onDismiss = { classifyingId = null },
        )
    }
}

@Composable
private fun SectionTitle(title: Int, hint: Int) {
    Column(Modifier.padding(top = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PendingCard(sms: PendingSms, onIgnore: () -> Unit, onClassify: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                sms.receivedAt.format(DATE),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // on peut selectionner le texte pour le copier (et me l'envoyer)
            SelectionContainer { Text(sms.body, style = MaterialTheme.typography.bodyMedium) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End)) {
                TextButton(onClick = onIgnore) { Text(stringResource(R.string.review_ignore)) }
                FilledTonalButton(onClick = onClassify) { Text(stringResource(R.string.review_classify)) }
            }
        }
    }
}

@Composable
private fun GapCard(gap: BalanceGap, onOk: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(DesignR.drawable.ic_report, VolakoTheme.finance.leak)
            Text(
                stringResource(
                    R.string.review_gap_text,
                    (if (gap.amount > 0) "+" else "") + formatAriary(gap.amount),
                    gap.before.occurredAt.format(DATE),
                    gap.after.occurredAt.format(DATE),
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOk) { Text(stringResource(R.string.review_gap_ok)) }
        }
    }
}

@Composable
private fun AllGood(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
    ) {
        IconTile(DesignR.drawable.ic_task_alt, VolakoTheme.finance.income, size = 64.dp)
        Text(stringResource(R.string.review_all_good), style = MaterialTheme.typography.titleMedium)
    }
}

private val DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm")
