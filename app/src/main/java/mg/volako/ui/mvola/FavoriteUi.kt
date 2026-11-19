package mg.volako.ui.mvola

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mg.volako.R
import mg.volako.core.designsystem.component.IconTile
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.theme.Sizes
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.model.Favorite
import mg.volako.core.model.FavoriteKind
import mg.volako.core.ussd.MvolaUssd
import mg.volako.core.ussd.UssdResult
import mg.volako.core.ussd.YasNumber
import mg.volako.core.ussd.YasOffers
import mg.volako.core.designsystem.R as DesignR

@DrawableRes
private fun Favorite.icon(): Int = when (kind) {
    FavoriteKind.SEND -> DesignR.drawable.ic_send_money
    FavoriteKind.CREDIT_ME, FavoriteKind.CREDIT_OTHER -> DesignR.drawable.ic_sim_card
    FavoriteKind.WITHDRAW_AGENT -> DesignR.drawable.ic_local_atm
    FavoriteKind.OFFER_ME, FavoriteKind.OFFER_OTHER, FavoriteKind.OFFER_AIRTIME -> DesignR.drawable.ic_signal_cellular_alt
}

// "Envoyer 50 000 Ar"
@Composable
fun Favorite.title(): String {
    val a = formatAriary(amount ?: 0)
    return when (kind) {
        FavoriteKind.SEND -> stringResource(R.string.fav_send, a)
        FavoriteKind.CREDIT_ME -> stringResource(R.string.fav_credit_me, a)
        FavoriteKind.CREDIT_OTHER -> stringResource(R.string.fav_credit_other, a)
        FavoriteKind.WITHDRAW_AGENT -> stringResource(R.string.fav_withdraw, a)
        FavoriteKind.OFFER_ME, FavoriteKind.OFFER_OTHER, FavoriteKind.OFFER_AIRTIME ->
            stringResource(R.string.fav_offer, offerId?.let(YasOffers::byId)?.name ?: offerId.orEmpty())
    }
}

// "Elyse · 034 20 454 85" ou "Pour mon numero"
@Composable
fun Favorite.subtitle(): String {
    val number = phone?.let(YasNumber::parse)
    return when {
        number != null -> name?.let { "$it · ${number.pretty()}" } ?: number.pretty()
        kind == FavoriteKind.OFFER_ME -> offerId?.let(YasOffers::byId)?.let { "${formatAriary(it.price)} · ${stringResource(R.string.fav_for_me)}" }
            ?: stringResource(R.string.fav_for_me)
        kind == FavoriteKind.OFFER_AIRTIME -> offerId?.let(YasOffers::byId)?.let { "${formatAriary(it.price)} · ${stringResource(R.string.ussd_paid_airtime)}" }
            ?: stringResource(R.string.ussd_paid_airtime)
        else -> stringResource(R.string.fav_for_me)
    }
}

// --- section "Favoris" de l'onglet ---

@Composable
fun FavoritesList(favorites: List<Favorite>, onOpen: (Favorite) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        favorites.forEach { fav -> FavoriteRow(fav, onClick = { onOpen(fav) }) }
    }
}

@Composable
private fun FavoriteRow(favorite: Favorite, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .heightIn(min = Sizes.listItemMinHeight)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconTile(favorite.icon(), tint = MaterialTheme.colorScheme.primary, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(favorite.title(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    favorite.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painterResource(DesignR.drawable.ic_chevron_right),
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- un favori ouvert : resume direct -> Lancer (ou supprimer) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteSheet(
    favorite: Favorite,
    onLaunch: (mg.volako.core.ussd.UssdRequest) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ready = favorite.toOperation()?.let { MvolaUssd.build(it) as? UssdResult.Ready }
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
            val deleteButton = @Composable {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.fav_delete), color = MaterialTheme.colorScheme.error)
                }
            }
            if (ready == null) {
                Text(stringResource(R.string.fav_invalid), style = MaterialTheme.typography.bodyLarge)
                deleteButton()
                return@Column
            }
            UssdConfirm(
                rows = favoriteRows(favorite),
                request = ready.request,
                onLaunch = { onLaunch(ready.request) },
                onEdit = onDismiss,
                editLabel = stringResource(R.string.fav_close),
                footer = deleteButton,
            )
        }
    }
}

@Composable
private fun favoriteRows(f: Favorite): List<Pair<String, String>> = buildList {
    val op = stringResource(R.string.ussd_row_operation)
    when (f.kind) {
        FavoriteKind.SEND -> {
            add(op to stringResource(R.string.ussd_op_send))
            add(stringResource(R.string.ussd_row_to) to f.subtitle())
            add(stringResource(R.string.ussd_row_amount) to formatAriary(f.amount ?: 0))
            add(stringResource(R.string.ussd_pay_fee_question) to stringResource(if (f.payRecipientFee) R.string.ussd_yes else R.string.ussd_no))
            f.description?.let { add(stringResource(R.string.ussd_description_label) to it) }
        }
        FavoriteKind.CREDIT_ME -> {
            add(op to stringResource(R.string.ussd_op_credit_me))
            add(stringResource(R.string.ussd_row_amount) to formatAriary(f.amount ?: 0))
        }
        FavoriteKind.CREDIT_OTHER -> {
            add(op to stringResource(R.string.ussd_op_credit_other))
            add(stringResource(R.string.ussd_row_to) to f.subtitle())
            add(stringResource(R.string.ussd_row_amount) to formatAriary(f.amount ?: 0))
            add(stringResource(R.string.ussd_row_fee) to formatAriary(MvolaUssd.OTHER_NUMBER_FEE))
        }
        FavoriteKind.WITHDRAW_AGENT -> {
            add(op to stringResource(R.string.ussd_op_withdraw_agent))
            add(stringResource(R.string.ussd_row_agent) to f.subtitle())
            add(stringResource(R.string.ussd_row_amount) to formatAriary(f.amount ?: 0))
        }
        FavoriteKind.OFFER_ME, FavoriteKind.OFFER_OTHER, FavoriteKind.OFFER_AIRTIME -> {
            val offer = f.offerId?.let(YasOffers::byId)
            add(op to stringResource(if (f.kind == FavoriteKind.OFFER_OTHER) R.string.ussd_op_offer_other else R.string.ussd_op_offer_me))
            if (f.kind == FavoriteKind.OFFER_OTHER) add(stringResource(R.string.ussd_row_to) to f.subtitle())
            add(stringResource(R.string.ussd_row_offer) to (offer?.name ?: f.offerId.orEmpty()))
            offer?.let {
                add(stringResource(R.string.ussd_row_content) to it.content)
                add(stringResource(R.string.ussd_row_price) to formatAriary(it.price))
            }
            add(stringResource(R.string.ussd_row_paid_with) to stringResource(if (f.kind == FavoriteKind.OFFER_AIRTIME) R.string.ussd_pay_airtime else R.string.mvola_title))
            if (f.kind == FavoriteKind.OFFER_OTHER) add(stringResource(R.string.ussd_row_fee) to formatAriary(MvolaUssd.OTHER_NUMBER_FEE))
        }
    }
}
