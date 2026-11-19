package mg.volako.ui.mvola

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mg.volako.R
import mg.volako.core.designsystem.component.AlertBanner
import mg.volako.core.designsystem.component.FilterChipItem
import mg.volako.core.designsystem.component.FilterChipRow
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.AmountTextStyle
import mg.volako.core.designsystem.theme.Sizes
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.domain.RecentContact
import mg.volako.core.model.Favorite
import mg.volako.core.ussd.MvolaUssd
import mg.volako.core.ussd.OfferFamily
import mg.volako.core.ussd.UssdOperation
import mg.volako.core.ussd.UssdRequest
import mg.volako.core.ussd.UssdResult
import mg.volako.core.ussd.YasNumber
import mg.volako.core.ussd.YasOffer
import mg.volako.core.ussd.YasOffers
import mg.volako.core.designsystem.R as DesignR

// Offre : pour moi ou pour un autre, payee avec MVola ou (pour moi) avec le credit telephonique.
// pour un autre : numero -> offre -> resume ; pour moi : offre -> resume
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferSheet(
    mvolaBalance: Long,
    recents: List<RecentContact>,
    onLaunch: (UssdRequest) -> Unit,
    onSaveFavorite: (Favorite) -> Unit,
    onDismiss: () -> Unit,
) {
    var forOther by rememberSaveable { mutableStateOf(false) }
    var withAirtime by rememberSaveable { mutableStateOf(false) }
    var otherNumber by rememberSaveable { mutableStateOf<String?>(null) }
    var otherName by rememberSaveable { mutableStateOf<String?>(null) }
    var family by rememberSaveable { mutableStateOf(OfferFamily.YELOW) }
    // on garde l'id (simple a sauvegarder si Android recree l'ecran)
    var chosenId by rememberSaveable { mutableStateOf<String?>(null) }

    val chosen = chosenId?.let(YasOffers::byId)
    val recipient = otherNumber?.let(YasNumber::parse)?.let { Recipient(it, otherName) }
    // le credit telephonique ne marche que pour soi (pas de code #322 pour un autre)
    val payAirtime = withAirtime && !forOther

    fun operationFor(offer: YasOffer): UssdOperation = when {
        forOther -> UssdOperation.OfferForOther(recipient?.number?.digits.orEmpty(), offer)
        payAirtime -> UssdOperation.OfferWithAirtime(offer)
        else -> UssdOperation.OfferForMe(offer)
    }

    val header = @Composable {
        Toggle(
            selectedSecond = forOther,
            first = R.string.ussd_for_me,
            second = R.string.ussd_for_other,
            onChange = { forOther = it },
        )
        if (!forOther) {
            Text(stringResource(R.string.ussd_pay_with), style = MaterialTheme.typography.titleSmall)
            Toggle(
                selectedSecond = withAirtime,
                first = R.string.mvola_title,
                second = R.string.ussd_pay_airtime,
                onChange = { withAirtime = it },
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(bottom = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            val operation = chosen?.let(::operationFor)
            val ready = operation?.let { MvolaUssd.build(it) as? UssdResult.Ready }

            when {
                // 1) resume
                chosen != null && ready != null -> Column(
                    Modifier.padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    if (!payAirtime && chosen.price + (if (forOther) MvolaUssd.OTHER_NUMBER_FEE else 0) > mvolaBalance) {
                        AlertBanner(
                            icon = DesignR.drawable.ic_report,
                            title = stringResource(R.string.ussd_low_balance, formatAriary(mvolaBalance)),
                            body = stringResource(R.string.ussd_low_balance_body),
                        )
                    }
                    UssdConfirm(
                        rows = buildList {
                            add(stringResource(R.string.ussd_row_operation) to stringResource(if (forOther) R.string.ussd_op_offer_other else R.string.ussd_op_offer_me))
                            if (forOther && recipient != null) add(stringResource(R.string.ussd_row_to) to recipient.label())
                            add(stringResource(R.string.ussd_row_offer) to chosen.name)
                            add(stringResource(R.string.ussd_row_content) to chosen.content)
                            add(stringResource(R.string.ussd_row_price) to formatAriary(chosen.price))
                            add(stringResource(R.string.ussd_row_paid_with) to stringResource(if (payAirtime) R.string.ussd_pay_airtime else R.string.mvola_title))
                            if (forOther) add(stringResource(R.string.ussd_row_fee) to formatAriary(MvolaUssd.OTHER_NUMBER_FEE))
                        },
                        request = ready.request,
                        onLaunch = { onLaunch(ready.request) },
                        onEdit = { chosenId = null },
                        onSaveFavorite = { favoriteOf(operation, recipient?.name)?.let(onSaveFavorite) },
                    )
                }

                // 2) pour un autre : d'abord le numero
                forOther && recipient == null -> Column(Modifier.padding(horizontal = Spacing.lg)) {
                    RecipientStep(
                        title = stringResource(R.string.ussd_offer_other_title),
                        recents = recents,
                        onChosen = {
                            otherNumber = it.number.digits
                            otherName = it.name
                        },
                        header = header,
                    )
                }

                // 3) choix de l'offre
                else -> {
                    Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Text(
                            stringResource(if (forOther) R.string.ussd_offer_other_title else R.string.ussd_offer_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        header()
                        if (forOther && recipient != null) RecipientChip(recipient, onClick = { otherNumber = null })
                    }
                    FilterChipRow(
                        items = OfferFamily.entries.map { FilterChipItem(key = it.name, label = it.label) },
                        selectedKey = family.name,
                        onSelect = { family = OfferFamily.valueOf(it) },
                        contentPadding = PaddingValues(horizontal = Spacing.lg),
                    )
                    Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        // avec le credit : seulement les offres dont on connait le code #322
                        YasOffers.of(family)
                            .filter { !payAirtime || it.airtimeCode != null }
                            .forEach { offer -> OfferRow(offer, onClick = { chosenId = offer.id }) }
                    }
                }
            }
        }
    }
}

// deux choix cote a cote (pour moi / pour un autre, MVola / credit)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toggle(selectedSecond: Boolean, first: Int, second: Int, onChange: (Boolean) -> Unit) {
    val options = listOf(false to first, true to second)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selectedSecond == value,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun OfferRow(offer: YasOffer, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .heightIn(min = Sizes.listItemMinHeight)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(Modifier.weight(1f)) {
                Text(offer.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    offer.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(formatAriary(offer.price), style = AmountTextStyle.Regular)
        }
    }
}
