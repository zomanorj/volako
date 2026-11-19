package mg.volako.ui.mvola

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import mg.volako.core.model.Favorite
import mg.volako.core.ussd.MvolaUssd
import mg.volako.core.ussd.UssdOperation
import mg.volako.core.ussd.UssdRequest
import mg.volako.core.ussd.UssdResult
import mg.volako.core.ussd.MgNumber

// Credit : pour moi, ou pour un autre numero (destinataire -> montant -> resume -> Lancer)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditSheet(
    mvolaBalance: Long,
    people: PeoplePicker,
    onLaunch: (UssdRequest) -> Unit,
    onSaveFavorite: (Favorite) -> Unit,
    onDismiss: () -> Unit,
) {
    var forOther by rememberSaveable { mutableStateOf(false) }
    // destinataire garde en texte simple (sauvegarde facile si Android recree l'ecran)
    var otherNumber by rememberSaveable { mutableStateOf<String?>(null) }
    var otherName by rememberSaveable { mutableStateOf<String?>(null) }
    var digits by rememberSaveable { mutableStateOf("") }
    var confirming by rememberSaveable { mutableStateOf(false) }

    val amount = digits.toLongOrNull() ?: 0L
    val recipient = otherNumber?.let(MgNumber::parse)?.let { Recipient(it, otherName) }
    val operation =
        if (forOther) UssdOperation.CreditForOther(recipient?.number?.digits.orEmpty(), amount) else UssdOperation.CreditForMe(amount)
    val result = MvolaUssd.build(operation)

    val scroll = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding()
                .padding(bottom = Spacing.lg)
                // l'etape "choisir un numero" a sa propre liste qui defile (LazyColumn) : pas de double defilement
                .then(if (forOther && recipient == null) Modifier else Modifier.verticalScroll(scroll)),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            when {
                confirming && result is UssdResult.Ready -> UssdConfirm(
                    rows = buildList {
                        add(stringResource(R.string.ussd_row_operation) to stringResource(if (forOther) R.string.ussd_op_credit_other else R.string.ussd_op_credit_me))
                        if (recipient != null && forOther) add(stringResource(R.string.ussd_row_to) to recipient.label())
                        add(stringResource(R.string.ussd_row_amount) to formatAriary(amount))
                        add(stringResource(R.string.ussd_row_paid_with) to stringResource(R.string.mvola_title))
                        add(stringResource(R.string.ussd_row_fee) to if (forOther) formatAriary(MvolaUssd.OTHER_NUMBER_FEE) else stringResource(R.string.ussd_fee_none))
                    },
                    request = result.request,
                    onLaunch = { onLaunch(result.request) },
                    onEdit = { confirming = false },
                    onSaveFavorite = { favoriteOf(operation, recipient?.name)?.let(onSaveFavorite) },
                )

                forOther && recipient == null -> RecipientStep(
                    title = stringResource(R.string.ussd_credit_other_title),
                    people = people,
                    onChosen = {
                        otherNumber = it.number.digits
                        otherName = it.name
                    },
                    header = { ForWhom(forOther, onChange = { forOther = it }) },
                )

                else -> AmountStep(
                    title = stringResource(if (forOther) R.string.ussd_credit_other_title else R.string.ussd_credit_title),
                    digits = digits,
                    onDigitsChange = { digits = it },
                    error = (result as? UssdResult.Refused)?.error,
                    // pour un autre numero, Yas ajoute 150 Ar de frais
                    mvolaBalance = if (forOther) mvolaBalance - MvolaUssd.OTHER_NUMBER_FEE else mvolaBalance,
                    quickAmounts = QUICK_AMOUNTS,
                    onContinue = { confirming = true },
                    header = {
                        ForWhom(forOther, onChange = { forOther = it })
                        if (forOther && recipient != null) {
                            RecipientChip(recipient, onClick = { otherNumber = null })
                        }
                    },
                )
            }
        }
    }
}

// "Pour moi / Pour un autre"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForWhom(forOther: Boolean, onChange: (Boolean) -> Unit) {
    val options = listOf(false to R.string.ussd_for_me, true to R.string.ussd_for_other)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = forOther == value,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(stringResource(label)) }
        }
    }
}


private val QUICK_AMOUNTS = listOf(500L, 1_000L, 2_000L, 5_000L)
