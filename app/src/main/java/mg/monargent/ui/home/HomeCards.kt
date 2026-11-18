package mg.monargent.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mg.monargent.R
import mg.monargent.core.designsystem.component.AccountRow
import mg.monargent.core.designsystem.component.IconBadge
import mg.monargent.core.designsystem.component.LabelValueRow
import mg.monargent.core.designsystem.component.formatAriary
import mg.monargent.core.designsystem.theme.AmountTextStyle
import mg.monargent.core.designsystem.theme.MonArgentTheme
import mg.monargent.core.designsystem.theme.Spacing
import mg.monargent.core.designsystem.R as DesignR

// Les cartes du haut de l'accueil

@Composable
fun BalancesCard(state: HomeUiState, onCount: () -> Unit) {
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
        }
    }
}

// carte "A verifier" : visible seulement s'il y a quelque chose a regarder
@Composable
fun ReviewCard(pendingCount: Int, gapCount: Int, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconBadge(DesignR.drawable.ic_report, MonArgentTheme.finance.leak)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_review_title), style = MaterialTheme.typography.titleMedium)
                val parts = listOfNotNull(
                    pendingCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.home_review_sms, it, it) },
                    gapCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.home_review_gaps, it, it) },
                )
                Text(
                    parts.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// le solde especes ne peut pas etre negatif en vrai : il manque des entrees
@Composable
fun NegativeCashCard(cash: Long, onCount: () -> Unit) {
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
fun MonthCard(state: HomeUiState) {
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
