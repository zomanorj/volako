package mg.volako.ui.mvola

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mg.volako.R
import mg.volako.core.designsystem.component.AlertBanner
import mg.volako.core.designsystem.component.NumericKeypad
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.AmountTextStyle
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.ussd.UssdError
import mg.volako.core.designsystem.R as DesignR

// Etape "montant" commune (credit, envoi, retrait) : gros montant, montants rapides, pave, Continuer.
// header = ce qu'on affiche au-dessus du montant (choix moi/autre, destinataire...)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmountStep(
    title: String,
    digits: String,
    onDigitsChange: (String) -> Unit,
    error: UssdError?,
    mvolaBalance: Long,
    quickAmounts: List<Long>,
    onContinue: () -> Unit,
    header: @Composable () -> Unit = {},
) {
    val amount = digits.toLongOrNull() ?: 0L
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        header()
        Text(
            formatAriary(amount),
            style = AmountTextStyle.Hero,
            color = if (amount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // erreur seulement quand il y a deja un montant (pas des le debut)
        if (error != null && amount > 0) {
            Text(
                ussdErrorText(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (error == null && amount > mvolaBalance) {
            AlertBanner(
                icon = DesignR.drawable.ic_report,
                title = stringResource(R.string.ussd_low_balance, formatAriary(mvolaBalance)),
                body = stringResource(R.string.ussd_low_balance_body),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            quickAmounts.forEach { quick ->
                SuggestionChip(onClick = { onDigitsChange(quick.toString()) }, label = { Text(formatAriary(quick)) })
            }
        }
        NumericKeypad(
            onDigits = { val next = (digits + it).trimStart('0'); if (next.length <= MAX_DIGITS) onDigitsChange(next) },
            onBackspace = { onDigitsChange(digits.dropLast(1)) },
            backspaceDescription = stringResource(R.string.entry_backspace),
        )
        Button(
            onClick = onContinue,
            enabled = amount > 0 && error == null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.ussd_continue)) }
    }
}

// 99 999 999 Ar : au-dessus du plafond MVola, l'erreur s'affiche
private const val MAX_DIGITS = 8
