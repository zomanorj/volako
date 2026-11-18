package mg.volako.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Couleurs "metier" que Material 3 n'a pas : entree, sortie, transfert, fuite (frais + credit)
// On les met a part pour pouvoir les changer sans toucher au reste du theme
@Immutable
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val leak: Color,
    // pour les barres des graphiques : valide (contraste, luminosite) sur le fond de chaque theme
    val expenseMark: Color,
)

internal val LightFinanceColors = FinanceColors(
    income = Color(0xFF1B7F4B),
    expense = Color(0xFFB3261E),
    transfer = Color(0xFF3B5BA9),
    leak = Color(0xFF9A6200),
    expenseMark = Color(0xFFB3261E),
)

internal val DarkFinanceColors = FinanceColors(
    income = Color(0xFF7FD9A2),
    expense = Color(0xFFFFB4A9),
    transfer = Color(0xFFB2C5FF),
    leak = Color(0xFFF5BE5B),
    expenseMark = Color(0xFFD9534A),
)

internal val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }
