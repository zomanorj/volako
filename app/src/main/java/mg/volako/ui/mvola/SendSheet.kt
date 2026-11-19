package mg.volako.ui.mvola

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import mg.volako.R
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.domain.RecentContact
import mg.volako.core.ussd.MvolaUssd
import mg.volako.core.ussd.UssdOperation
import mg.volako.core.ussd.UssdRequest
import mg.volako.core.ussd.UssdResult
import mg.volako.core.ussd.YasNumber

// Envoyer de l'argent : destinataire -> montant -> resume (frais de retrait, description) -> Lancer
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendSheet(
    mvolaBalance: Long,
    recents: List<RecentContact>,
    onLaunch: (UssdRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var number by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var digits by rememberSaveable { mutableStateOf("") }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var payFee by rememberSaveable { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf("") }

    val amount = digits.toLongOrNull() ?: 0L
    val recipient = number?.let(YasNumber::parse)?.let { Recipient(it, name) }
    val result = MvolaUssd.build(
        UssdOperation.Send(recipient?.number?.digits.orEmpty(), amount, payRecipientFee = payFee, description = description),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            when {
                recipient == null -> RecipientStep(
                    title = stringResource(R.string.ussd_send_title),
                    recents = recents,
                    onChosen = {
                        number = it.number.digits
                        name = it.name
                    },
                )

                confirming -> {
                    // la description peut rendre le code invalide : on garde le dernier code valide pour l'affichage
                    val request = (result as? UssdResult.Ready)?.request
                        ?: (MvolaUssd.build(UssdOperation.Send(recipient.number.digits, amount, payFee, null)) as UssdResult.Ready).request
                    UssdConfirm(
                        rows = listOf(
                            stringResource(R.string.ussd_row_operation) to stringResource(R.string.ussd_op_send),
                            stringResource(R.string.ussd_row_to) to recipient.label(),
                            stringResource(R.string.ussd_row_amount) to formatAriary(amount),
                            stringResource(R.string.ussd_row_fee) to stringResource(R.string.ussd_fee_shown_by_yas),
                        ),
                        request = request,
                        launchEnabled = result is UssdResult.Ready,
                        onLaunch = { (result as? UssdResult.Ready)?.let { onLaunch(it.request) } },
                        onEdit = { confirming = false },
                        options = {
                            SendOptions(
                                payFee = payFee,
                                onPayFee = { payFee = it },
                                description = description,
                                onDescription = { description = it },
                                error = (result as? UssdResult.Refused)?.error?.let { ussdErrorText(it) },
                            )
                        },
                    )
                }

                else -> AmountStep(
                    title = stringResource(R.string.ussd_send_title),
                    digits = digits,
                    onDigitsChange = { digits = it },
                    // ici seul le montant compte (la description se regle a l'etape suivante)
                    error = (MvolaUssd.build(UssdOperation.Send(recipient.number.digits, amount, payFee, null)) as? UssdResult.Refused)?.error,
                    mvolaBalance = mvolaBalance,
                    quickAmounts = QUICK_AMOUNTS,
                    onContinue = { confirming = true },
                    header = { RecipientChip(recipient, onClick = { number = null }) },
                )
            }
        }
    }
}

// frais de retrait du destinataire + description (chiffres seulement, sinon Yas les transforme)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendOptions(
    payFee: Boolean,
    onPayFee: (Boolean) -> Unit,
    description: String,
    onDescription: (String) -> Unit,
    error: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(stringResource(R.string.ussd_pay_fee_question), style = MaterialTheme.typography.titleSmall)
        val options = listOf(false to R.string.ussd_no, true to R.string.ussd_yes)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = payFee == value,
                    onClick = { onPayFee(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(stringResource(label)) }
            }
        }
        OutlinedTextField(
            value = description,
            // on garde tout ce qui est tape pour montrer l'erreur si ce ne sont pas des chiffres
            onValueChange = { onDescription(it.take(MvolaUssd.MAX_DESCRIPTION + 5)) },
            label = { Text(stringResource(R.string.ussd_description_label)) },
            singleLine = true,
            isError = error != null,
            supportingText = { Text(error ?: stringResource(R.string.ussd_description_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val QUICK_AMOUNTS = listOf(1_000L, 5_000L, 10_000L, 20_000L)
