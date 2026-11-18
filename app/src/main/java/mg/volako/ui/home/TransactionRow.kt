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
import mg.volako.core.designsystem.component.IconBadge
import mg.volako.core.designsystem.component.formatAriary
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
        TransactionRow(transaction, Modifier.clickable(onClick = onEdit))
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
fun TransactionRow(t: Transaction, modifier: Modifier = Modifier) {
    val f = VolakoTheme.finance
    val (icon, color, sign) = when {
        t.type == TransactionType.INCOME -> Triple(DesignR.drawable.ic_south_west, f.income, "+")
        t.type == TransactionType.TRANSFER -> Triple(DesignR.drawable.ic_swap_horiz, f.transfer, "")
        t.category == Category.CREDIT -> Triple(DesignR.drawable.ic_sim_card, f.leak, "−")
        else -> Triple(DesignR.drawable.ic_north_east, f.expense, "−")
    }
    val title = when {
        t.type == TransactionType.TRANSFER ->
            stringResource(if (t.toAccount == Account.CASH) R.string.home_withdrawal else R.string.home_deposit)
        t.category == Category.UNTRACKED -> stringResource(
            if (t.type == TransactionType.INCOME) R.string.home_untracked_income else R.string.home_untracked_expense,
        )
        // pour les anciens SMS deja enregistres avec une raison comme "37"
        t.source == TransactionSource.SMS -> SmsTransactionMapper.meaningfulReason(t.label) ?: t.counterpartyName ?: "—"
        else -> t.label ?: t.counterpartyName ?: "—"
    }
    // sous-titre : la personne (si le titre est deja un libelle) + l'heure
    val subtitle = listOfNotNull(
        t.counterpartyName?.takeIf { it != title && t.type != TransactionType.TRANSFER },
        t.occurredAt.format(TIME),
    ).joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        IconBadge(icon, color)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                // petite icone pour savoir de quel compte il s'agit
                Icon(
                    painterResource(if (t.account == Account.CASH) DesignR.drawable.ic_payments else DesignR.drawable.ic_smartphone),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(sign + formatAriary(t.amount), style = AmountTextStyle.Regular, color = color)
            if (t.fee > 0) {
                Text(
                    stringResource(R.string.home_fee_suffix, formatAriary(t.fee)),
                    style = MaterialTheme.typography.bodySmall,
                    color = f.leak,
                )
            }
        }
    }
}

private data class Quad(val color: androidx.compose.ui.graphics.Color, val icon: Int, val label: Int, val alignment: Alignment)

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
