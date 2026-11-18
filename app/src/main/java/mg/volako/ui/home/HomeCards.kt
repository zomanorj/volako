package mg.volako.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mg.volako.R
import mg.volako.core.designsystem.component.AccountRow
import mg.volako.core.designsystem.component.AlertBanner
import mg.volako.core.designsystem.component.SectionHeader
import mg.volako.core.designsystem.component.StatTile
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.component.formatNumber
import mg.volako.core.designsystem.theme.AmountTextStyle
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.designsystem.theme.VolakoTheme
import java.time.format.DateTimeFormatter
import java.util.Locale
import mg.volako.core.designsystem.R as DesignR

// Les blocs du haut de l'accueil (l'organisation complete change a l'etape 2 de la refonte)

@Composable
fun BalancesCard(state: HomeUiState, onCount: () -> Unit) {
    val onCard = MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(Spacing.xl)) {
            Text(stringResource(R.string.home_total), style = MaterialTheme.typography.bodyMedium, color = onCard)
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

// "2 SMS a verifier · 1 ecart de solde" : un bandeau qu'on touche pour ouvrir l'ecran
@Composable
fun ReviewCard(pendingCount: Int, gapCount: Int, onOpen: () -> Unit) {
    val parts = listOfNotNull(
        pendingCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.home_review_sms, it, it) },
        gapCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.home_review_gaps, it, it) },
    )
    AlertBanner(
        icon = DesignR.drawable.ic_report,
        title = parts.joinToString(" · "),
        trailingIcon = DesignR.drawable.ic_chevron_right,
        onClick = onOpen,
    )
}

// le solde especes ne peut pas etre negatif en vrai : il manque des entrees
@Composable
fun NegativeCashCard(cash: Long, onCount: () -> Unit) {
    AlertBanner(
        icon = DesignR.drawable.ic_payments,
        title = stringResource(R.string.count_negative, formatAriary(cash)),
        actionLabel = stringResource(R.string.count_button),
        onAction = onCount,
    )
}

// MVola a change ses SMS : on previent, l'app s'adapte toute seule
@Composable
fun FormatAlertCard(onDismiss: () -> Unit) {
    AlertBanner(
        icon = DesignR.drawable.ic_autorenew,
        title = stringResource(R.string.format_alert_title),
        body = stringResource(R.string.format_alert_body),
        actionLabel = stringResource(R.string.format_alert_ok),
        onAction = onDismiss,
    )
}

// Le mois en 3 chiffres. Les fuites (credit, frais) sont les seules en ambre.
@Composable
fun MonthCard(state: HomeUiState, onOpen: () -> Unit) {
    val f = VolakoTheme.finance
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(
            title = state.month.month.format(MONTH).replaceFirstChar { it.titlecase(Locale.FRENCH) },
            actionLabel = stringResource(R.string.month_open),
            onAction = onOpen,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatTile(
                label = stringResource(R.string.home_expenses),
                value = formatNumber(state.month.expenses),
                modifier = Modifier.weight(1f),
                onClick = onOpen,
            )
            StatTile(
                label = stringResource(R.string.home_credit),
                value = formatNumber(state.month.credit),
                valueColor = f.leak,
                modifier = Modifier.weight(1f),
                onClick = onOpen,
            )
            StatTile(
                label = stringResource(R.string.home_fees),
                value = formatNumber(state.month.fees),
                valueColor = f.leak,
                modifier = Modifier.weight(1f),
                onClick = onOpen,
            )
        }
    }
}

private val MONTH = DateTimeFormatter.ofPattern("MMMM", Locale.FRENCH)
