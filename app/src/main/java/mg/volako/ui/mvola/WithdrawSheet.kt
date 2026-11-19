package mg.volako.ui.mvola

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mg.volako.R
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.domain.RecentContact
import mg.volako.core.ussd.MvolaUssd
import mg.volako.core.ussd.UssdOperation
import mg.volako.core.ussd.UssdRequest
import mg.volako.core.ussd.UssdResult
import mg.volako.core.ussd.YasNumber

// Retirer : chez un agent (agent -> montant -> resume) ou au distributeur BRED (resume direct)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSheet(
    mvolaBalance: Long,
    agents: List<RecentContact>,
    onLaunch: (UssdRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var atAtm by rememberSaveable { mutableStateOf(false) }
    var agentNumber by rememberSaveable { mutableStateOf<String?>(null) }
    var agentName by rememberSaveable { mutableStateOf<String?>(null) }
    var digits by rememberSaveable { mutableStateOf("") }
    var confirming by rememberSaveable { mutableStateOf(false) }

    val amount = digits.toLongOrNull() ?: 0L
    val agent = agentNumber?.let(YasNumber::parse)?.let { Recipient(it, agentName) }
    val result = MvolaUssd.build(
        if (atAtm) UssdOperation.WithdrawAtAtm else UssdOperation.WithdrawAtAgent(agent?.number?.digits.orEmpty(), amount),
    )
    val where = @Composable { Where(atAtm, onChange = { atAtm = it }) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding()
                .padding(bottom = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            when {
                // DAB : rien a saisir, Yas envoie un code temporaire
                atAtm && result is UssdResult.Ready -> {
                    where()
                    UssdConfirm(
                        rows = listOf(
                            stringResource(R.string.ussd_row_operation) to stringResource(R.string.ussd_op_withdraw_atm),
                            stringResource(R.string.ussd_row_how) to stringResource(R.string.ussd_atm_how),
                        ),
                        request = result.request,
                        onLaunch = { onLaunch(result.request) },
                        onEdit = onDismiss,
                    )
                }

                confirming && result is UssdResult.Ready && agent != null -> UssdConfirm(
                    rows = listOf(
                        stringResource(R.string.ussd_row_operation) to stringResource(R.string.ussd_op_withdraw_agent),
                        stringResource(R.string.ussd_row_agent) to agent.label(),
                        stringResource(R.string.ussd_row_amount) to formatAriary(amount),
                        stringResource(R.string.ussd_row_fee) to stringResource(R.string.ussd_fee_from_balance),
                    ),
                    request = result.request,
                    onLaunch = { onLaunch(result.request) },
                    onEdit = { confirming = false },
                )

                agent == null -> RecipientStep(
                    title = stringResource(R.string.ussd_withdraw_title),
                    recents = agents,
                    onChosen = {
                        agentNumber = it.number.digits
                        agentName = it.name
                    },
                    header = where,
                    numberLabel = stringResource(R.string.ussd_agent_number),
                )

                else -> AmountStep(
                    title = stringResource(R.string.ussd_withdraw_title),
                    digits = digits,
                    onDigitsChange = { digits = it },
                    error = (result as? UssdResult.Refused)?.error,
                    mvolaBalance = mvolaBalance,
                    quickAmounts = QUICK_AMOUNTS,
                    onContinue = { confirming = true },
                    header = {
                        where()
                        RecipientChip(agent, onClick = { agentNumber = null })
                    },
                )
            }
        }
    }
}

// "Chez un agent / Distributeur BRED"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Where(atAtm: Boolean, onChange: (Boolean) -> Unit) {
    val options = listOf(false to R.string.ussd_at_agent, true to R.string.ussd_at_atm)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = atAtm == value,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

private val QUICK_AMOUNTS = listOf(10_000L, 20_000L, 50_000L, 100_000L)
