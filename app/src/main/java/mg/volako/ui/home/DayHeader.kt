package mg.volako.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import mg.volako.core.designsystem.theme.AmountTextStyle
import androidx.compose.ui.res.stringResource
import mg.volako.R
import mg.volako.core.designsystem.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// "Aujourd'hui", "Hier", puis "Lundi 16 novembre" (l'annee seulement si ce n'est pas l'annee en cours)
@Composable
fun DayHeader(day: LocalDate, modifier: Modifier = Modifier, trailing: String? = null) {
    val today = LocalDate.now()
    val text = when (day) {
        today -> stringResource(R.string.home_today)
        today.minusDays(1) -> stringResource(R.string.home_yesterday)
        else -> day.format(if (day.year == today.year) DAY else DAY_YEAR).replaceFirstChar { it.titlecase(Locale.FRENCH) }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.xl, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        // total du jour (optionnel), discret pour ne pas concurrencer les montants des lignes
        if (trailing != null) {
            Text(trailing, style = AmountTextStyle.Regular.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
private val DAY_YEAR = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
