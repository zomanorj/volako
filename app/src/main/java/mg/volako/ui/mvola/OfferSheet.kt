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
import mg.volako.core.model.Favorite
import mg.volako.core.ussd.MvolaUssd
import mg.volako.core.ussd.OfferFamily
import mg.volako.core.ussd.UssdOperation
import mg.volako.core.ussd.UssdRequest
import mg.volako.core.ussd.UssdResult
import mg.volako.core.ussd.YasOffer
import mg.volako.core.ussd.YasOffers
import mg.volako.core.designsystem.R as DesignR

// Offre pour mon numero : famille -> offre -> resume -> Lancer
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferSheet(
    mvolaBalance: Long,
    onLaunch: (UssdRequest) -> Unit,
    onSaveFavorite: (Favorite) -> Unit,
    onDismiss: () -> Unit,
) {
    var family by rememberSaveable { mutableStateOf(OfferFamily.YELOW) }
    // on garde l'id (simple a sauvegarder si Android recree l'ecran)
    var chosenId by rememberSaveable { mutableStateOf<String?>(null) }
    val chosen = chosenId?.let(YasOffers::byId)

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
            val ready = chosen?.let { MvolaUssd.build(UssdOperation.OfferForMe(it)) as? UssdResult.Ready }
            if (chosen != null && ready != null) {
                Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    if (chosen.price > mvolaBalance) {
                        AlertBanner(
                            icon = DesignR.drawable.ic_report,
                            title = stringResource(R.string.ussd_low_balance, formatAriary(mvolaBalance)),
                            body = stringResource(R.string.ussd_low_balance_body),
                        )
                    }
                    UssdConfirm(
                        rows = listOf(
                            stringResource(R.string.ussd_row_operation) to stringResource(R.string.ussd_op_offer_me),
                            stringResource(R.string.ussd_row_offer) to chosen.name,
                            stringResource(R.string.ussd_row_content) to chosen.content,
                            stringResource(R.string.ussd_row_price) to formatAriary(chosen.price),
                            stringResource(R.string.ussd_row_paid_with) to stringResource(R.string.mvola_title),
                        ),
                        request = ready.request,
                        onLaunch = { onLaunch(ready.request) },
                        onEdit = { chosenId = null },
                        onSaveFavorite = { favoriteOf(UssdOperation.OfferForMe(chosen))?.let(onSaveFavorite) },
                    )
                }
                return@Column
            }

            Text(
                stringResource(R.string.ussd_offer_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            FilterChipRow(
                items = OfferFamily.entries.map { FilterChipItem(key = it.name, label = it.label) },
                selectedKey = family.name,
                onSelect = { family = OfferFamily.valueOf(it) },
                contentPadding = PaddingValues(horizontal = Spacing.lg),
            )
            Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                YasOffers.of(family).forEach { offer -> OfferRow(offer, onClick = { chosenId = offer.id }) }
            }
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
