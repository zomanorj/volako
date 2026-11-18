package mg.volako.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import mg.volako.core.designsystem.theme.Sizes

// Icone dans un carre arrondi au fond neutre. Seule l'icone porte la couleur (le sens),
// le fond reste discret pour ne pas surcharger les listes.
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.iconTile,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(container, RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        // decoratif : le texte a cote dit deja de quoi il s'agit
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size / 2))
    }
}
