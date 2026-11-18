package mg.volako.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Regle de la refonte : UNE couleur = UN sens.
// - income : seulement l'argent qui entre
// - leak : seulement les "fuites" (credit, frais)
// - les depenses n'ont PAS de couleur (texte normal + signe moins) -> moins de bruit
@Immutable
data class FinanceColors(
    val income: Color,
    val leak: Color,
    // retraits / depots : discret, ce n'est ni une entree ni une depense
    val transfer: Color,
    // barres des graphiques (valide contraste + luminosite avec l'outil dataviz)
    val chartBar: Color,
    // bandeaux d'alerte (SMS a verifier, format MVola change...)
    val warningContainer: Color,
    val onWarningContainer: Color,
    val warningBorder: Color,
)

internal val LightFinanceColors = FinanceColors(
    income = Color(0xFF1B7F4B),
    leak = Color(0xFF8F5B00),
    transfer = Color(0xFF4C5D8F),
    chartBar = Color(0xFF2E9A83),
    warningContainer = Color(0xFFFFF1D6),
    onWarningContainer = Color(0xFF4A3300),
    warningBorder = Color(0xFFE8C778),
)

internal val DarkFinanceColors = FinanceColors(
    income = Color(0xFF7FD9A2),
    leak = Color(0xFFF5BE5B),
    transfer = Color(0xFFB2C5FF),
    chartBar = Color(0xFF3FA48C),
    warningContainer = Color(0xFF241F12),
    onWarningContainer = Color(0xFFF5D58F),
    warningBorder = Color(0xFF5C4A1E),
)

internal val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }
