package mg.volako.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mg.volako.R
import mg.volako.core.designsystem.component.IconTile
import mg.volako.core.designsystem.component.MoneyListItem
import mg.volako.core.designsystem.component.formatAriary
import mg.volako.core.designsystem.component.formatPhone
import mg.volako.core.designsystem.theme.AmountTextStyle
import mg.volako.core.designsystem.theme.VolakoTheme
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.domain.SmsTransactionMapper
import mg.volako.core.model.Account
import mg.volako.core.model.Category
import mg.volako.core.model.Transaction
import mg.volako.core.model.TransactionSource
import mg.volako.core.model.TransactionType
import java.time.format.DateTimeFormatter
import mg.volako.core.designsystem.R as DesignR

// Ligne d'operation : glisser vers la droite = modifier, vers la gauche = supprimer, toucher = modifier
@Composable
fun SwipeableTransactionRow(
    transaction: Transaction,
    // nom du repertoire du telephone pour ce numero (prioritaire sur le nom du SMS)
    contactName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()

    // bug corrige : la liste garde l'etat "glisse" d'une ligne supprimee. Apres "Annuler",
    // la ligne revenait deja glissee et etait supprimee de nouveau. On la remet en place.
    LaunchedEffect(transaction.id) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) state.snapTo(SwipeToDismissBoxValue.Settled)
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        backgroundContent = { SwipeBackground(state.dismissDirection) },
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    // on remet tout de suite l'etat a zero pour qu'il ne soit pas memorise
                    scope.launch { state.snapTo(SwipeToDismissBoxValue.Settled) }
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    // la ligne revient a sa place, on ouvre juste la modification
                    scope.launch { state.reset() }
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
    ) {
        TransactionRow(transaction, contactName, Modifier.clickable(onClick = onEdit))
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    // rien a dessiner au repos (sinon la couleur deborde sur les coins arrondis)
    if (direction == SwipeToDismissBoxValue.Settled) return
    val (color, icon, label, alignment) = when (direction) {
        SwipeToDismissBoxValue.EndToStart -> Quad(
            MaterialTheme.colorScheme.errorContainer, DesignR.drawable.ic_delete, R.string.home_swipe_delete, Alignment.CenterEnd,
        )
        else -> Quad(
            MaterialTheme.colorScheme.secondaryContainer, DesignR.drawable.ic_edit, R.string.home_swipe_edit, Alignment.CenterStart,
        )
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = Spacing.lg),
        contentAlignment = alignment,
    ) {
        Icon(painterResource(icon), stringResource(label))
    }
}

@Composable
fun TransactionRow(t: Transaction, contactName: String? = null, modifier: Modifier = Modifier) {
    // le nom que l'utilisateur connait (son repertoire) avant celui ecrit par MVola
    val person = contactName ?: t.counterpartyName
    val f = VolakoTheme.finance
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    // une couleur = un sens : vert pour ce qui entre, ambre pour les fuites, le reste neutre
    val (icon, iconTint) = when {
        t.type == TransactionType.INCOME -> DesignR.drawable.ic_south_west to f.income
        t.type == TransactionType.TRANSFER -> DesignR.drawable.ic_swap_horiz to f.transfer
        t.category == Category.CREDIT -> DesignR.drawable.ic_sim_card to f.leak
        t.account == Account.CASH -> DesignR.drawable.ic_payments to neutral
        else -> DesignR.drawable.ic_north_east to neutral
    }
    val (sign, amountColor) = when (t.type) {
        TransactionType.INCOME -> "+" to f.income
        TransactionType.TRANSFER -> "" to neutral
        TransactionType.EXPENSE -> "−" to MaterialTheme.colorScheme.onSurface
    }
    // credit achete pour un autre numero : titre court + numero lisible en sous-titre
    val creditForOther = t.category == Category.CREDIT && t.counterpartyPhone != null &&
        (t.label == null || t.label!!.startsWith("Crédit"))
    val title = when {
        creditForOther -> stringResource(R.string.home_credit_sent)
        t.type == TransactionType.TRANSFER ->
            stringResource(if (t.toAccount == Account.CASH) R.string.home_withdrawal else R.string.home_deposit)
        t.category == Category.UNTRACKED -> stringResource(
            if (t.type == TransactionType.INCOME) R.string.home_untracked_income else R.string.home_untracked_expense,
        )
        // pour les anciens SMS deja enregistres avec une raison comme "37"
        t.source == TransactionSource.SMS -> SmsTransactionMapper.meaningfulReason(t.label) ?: person ?: "—"
        else -> t.label ?: person ?: "—"
    }
    // sous-titre : la personne (si le titre est deja un libelle), le compte, l'heure
    val account = stringResource(if (t.account == Account.CASH) R.string.home_cash else R.string.home_mvola)
    val subtitle = listOfNotNull(
        t.counterpartyPhone?.takeIf { creditForOther }?.let { stringResource(R.string.home_credit_to, contactName ?: formatPhone(it)) },
        person?.takeIf { it != title && t.type != TransactionType.TRANSFER && !creditForOther },
        // un credit envoye est toujours paye par MVola : on garde la place pour le numero et l'heure
        account.takeIf { !creditForOther },
        t.occurredAt.format(TIME),
    ).joinToString(" · ")

    MoneyListItem(
        leading = { IconTile(icon, tint = iconTint) },
        title = title,
        subtitle = subtitle,
        amount = sign + formatAriary(t.amount),
        amountColor = amountColor,
        footnote = t.fee.takeIf { it > 0 }?.let { stringResource(R.string.home_fee_suffix, formatAriary(it)) },
        footnoteColor = f.leak,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
    )
}

private data class Quad(val color: androidx.compose.ui.graphics.Color, val icon: Int, val label: Int, val alignment: Alignment)

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
