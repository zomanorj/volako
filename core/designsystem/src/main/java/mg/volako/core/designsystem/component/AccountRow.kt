package mg.volako.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import mg.volako.core.designsystem.theme.AmountTextStyle
import mg.volako.core.designsystem.theme.Spacing

// Une ligne "icone + nom du compte + solde", reutilisee sur l'accueil
@Composable
fun AccountRow(
    @DrawableRes icon: Int,
    name: String,
    amount: String,
    modifier: Modifier = Modifier,
    // sur une carte coloree, le carre d'icone doit etre une teinte de la carte (pas un trou sombre)
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tileContainer: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        IconTile(icon, tint = contentColor, container = tileContainer)
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
        Text(text = amount, style = AmountTextStyle.Regular, color = contentColor)
    }
}
