package mg.volako.ui.mvola

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import mg.volako.core.ussd.MvolaUssd
import mg.volako.core.ussd.UssdOperation
import mg.volako.core.ussd.UssdRequest
import mg.volako.core.ussd.UssdResult
import mg.volako.core.designsystem.R as DesignR

// Credit pour mon numero : montant au pave -> resume -> Lancer
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreditSheet(
    mvolaBalance: Long,
    onLaunch: (UssdRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var digits by rememberSaveable { mutableStateOf("") }
    var confirming by rememberSaveable { mutableStateOf(false) }
    val amount = digits.toLongOrNull() ?: 0L
    val result = MvolaUssd.build(UssdOperation.CreditForMe(amount))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding()
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (confirming && result is UssdResult.Ready) {
                UssdConfirm(
                    rows = listOf(
                        stringResource(R.string.ussd_row_operation) to stringResource(R.string.ussd_op_credit_me),
                        stringResource(R.string.ussd_row_amount) to formatAriary(amount),
                        stringResource(R.string.ussd_row_paid_with) to stringResource(R.string.mvola_title),
                        stringResource(R.string.ussd_row_fee) to stringResource(R.string.ussd_fee_none),
                    ),
                    request = result.request,
                    onLaunch = { onLaunch(result.request) },
                    onEdit = { confirming = false },
                )
                return@Column
            }

            Text(stringResource(R.string.ussd_credit_title), style = MaterialTheme.typography.titleLarge)
            Text(
                formatAriary(amount),
                style = AmountTextStyle.Hero,
                color = if (amount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            // erreur seulement quand il y a deja un montant (pas des le debut)
            val error = (result as? UssdResult.Refused)?.error?.takeIf { amount > 0 }
            if (error != null) {
                Text(
                    ussdErrorText(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (amount > mvolaBalance && result is UssdResult.Ready) {
                AlertBanner(
                    icon = DesignR.drawable.ic_report,
                    title = stringResource(R.string.ussd_low_balance, formatAriary(mvolaBalance)),
                    body = stringResource(R.string.ussd_low_balance_body),
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                QUICK_AMOUNTS.forEach { quick ->
                    SuggestionChip(onClick = { digits = quick.toString() }, label = { Text(formatAriary(quick)) })
                }
            }

            NumericKeypad(
                onDigits = { if ((digits + it).trimStart('0').length <= MAX_DIGITS) digits = (digits + it).trimStart('0') },
                onBackspace = { digits = digits.dropLast(1) },
                backspaceDescription = stringResource(R.string.entry_backspace),
            )
            Button(
                onClick = { confirming = true },
                enabled = result is UssdResult.Ready,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(stringResource(R.string.ussd_continue)) }
        }
    }
}

// les montants les plus courants pour le credit
private val QUICK_AMOUNTS = listOf(500L, 1_000L, 2_000L, 5_000L)

private const val MAX_DIGITS = 8
