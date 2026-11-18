package mg.volako.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mg.volako.core.designsystem.theme.Sizes
import mg.volako.core.designsystem.theme.Spacing
import mg.volako.core.designsystem.theme.VolakoTheme

// Bandeau compact pour ce qui demande l'attention (SMS a verifier, format change, especes negatives).
// Toucher le bandeau = onClick ; actionLabel = petit bouton optionnel (ex: "Compris").
@Composable
fun AlertBanner(
    @DrawableRes icon: Int,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    @DrawableRes trailingIcon: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    val f = VolakoTheme.finance
    val content: @Composable () -> Unit = {
        Row(
            Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(painterResource(icon), null, Modifier.size(Sizes.icon), tint = f.leak)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = f.onWarningContainer)
                if (body != null) {
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = f.onWarningContainer)
                }
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) { Text(actionLabel, color = f.onWarningContainer) }
                }
            }
            if (trailingIcon != null) {
                Icon(painterResource(trailingIcon), null, Modifier.size(20.dp), tint = f.onWarningContainer)
            }
        }
    }
    val shape = MaterialTheme.shapes.medium
    val border = BorderStroke(1.dp, f.warningBorder)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = f.warningContainer, border = border) {
            content()
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = f.warningContainer, border = border) { content() }
    }
}
