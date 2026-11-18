package mg.volako.core.designsystem.theme

import androidx.compose.ui.unit.dp

// Grille de 4dp : on utilise toujours ces valeurs pour garder des espacements reguliers
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

// Tailles qui reviennent partout (accessibilite : cibles >= 48 dp)
object Sizes {
    val touchTarget = 48.dp
    val listItemMinHeight = 64.dp
    val iconTile = 44.dp
    val icon = 22.dp
}
