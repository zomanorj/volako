package mg.volako.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mg.volako.core.designsystem.R
import mg.volako.core.designsystem.theme.Spacing

// Un filtre de la barre (Tout, MVola, Especes, Credit...)
data class FilterChipItem(
    val key: String,
    val label: String,
    @DrawableRes val icon: Int? = null,
    val iconTint: Color? = null,
    val count: Int? = null,
)

// Barre de filtres soignee : pastilles avec icone + compteur, la selectionnee est remplie
// avec une coche. Pas de barre de defilement, un fondu a droite montre qu'il y en a d'autres.
@Composable
fun FilterChipRow(
    items: List<FilterChipItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.lg),
) {
    val listState = rememberLazyListState()
    // le filtre choisi est toujours entierement visible (sinon il peut finir sous le fondu)
    val selectedIndex = items.indexOfFirst { it.key == selectedKey }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.find { it.index == selectedIndex }
        val end = listState.layoutInfo.viewportEndOffset - FADE_PX_GUESS
        if (visible == null || visible.offset < 0 || visible.offset + visible.size > end) {
            listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
        }
    }
    LazyRow(
        state = listState,
        modifier = modifier
            // le fondu : on efface doucement le bord droit, seulement s'il reste des pastilles a voir
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                if (!listState.canScrollForward) return@drawWithContent
                val fade = 48.dp.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Black,
                        1f to Color.Transparent,
                        startX = size.width - fade,
                        endX = size.width,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items, key = { it.key }) { item ->
            FilterPill(item, selected = item.key == selectedKey, onClick = { onSelect(item.key) })
        }
    }
}

// largeur approximative du fondu en pixels (pour decider s'il faut faire defiler)
private const val FADE_PX_GUESS = 140

@Composable
private fun FilterPill(item: FilterChipItem, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainer, label = "chip-bg")
    val content by animateColorAsState(if (selected) scheme.onPrimary else scheme.onSurface, label = "chip-fg")
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = RoundedCornerShape(22.dp),
        color = container,
        border = if (selected) null else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when {
                selected -> Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp), tint = content)
                item.icon != null -> Icon(painterResource(item.icon), null, Modifier.size(18.dp), tint = item.iconTint ?: content)
            }
            Text(item.label, style = MaterialTheme.typography.labelLarge, color = content)
            if (item.count != null) {
                Box(
                    Modifier
                        .height(22.dp)
                        .widthIn(min = 22.dp)
                        .background(
                            if (selected) content.copy(alpha = 0.16f) else scheme.surfaceContainerHighest,
                            RoundedCornerShape(11.dp),
                        )
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) content else scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
