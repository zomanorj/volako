package mg.monargent.ui.lab

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mg.monargent.R
import mg.monargent.core.designsystem.component.formatAriary
import mg.monargent.core.designsystem.theme.AmountTextStyle
import mg.monargent.core.designsystem.theme.MonArgentTheme
import mg.monargent.core.designsystem.theme.Spacing
import mg.monargent.core.sms.OperationKind
import mg.monargent.core.sms.ParsedSms
import java.time.format.DateTimeFormatter
import mg.monargent.core.designsystem.R as DesignR

// Apercu de ce que le parseur comprend d'un SMS
@Composable
internal fun SmsPreview(result: ParsedSms) {
    when (result) {
        is ParsedSms.Operation -> OperationCard(result)
        is ParsedSms.Unknown -> MessageCard(
            icon = DesignR.drawable.ic_help,
            title = R.string.smstest_unknown_title,
            body = R.string.smstest_unknown_body,
            color = MonArgentTheme.finance.leak,
        )
        ParsedSms.Ignored -> MessageCard(
            icon = DesignR.drawable.ic_block,
            title = R.string.smstest_ignored_title,
            body = R.string.smstest_ignored_body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OperationCard(op: ParsedSms.Operation) {
    val (icon, label, color) = kindStyle(op.kind)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                IconBadge(icon, color)
                Text(stringResource(label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Text(formatAriary(op.amount), style = AmountTextStyle.Hero, color = color)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (op.fee > 0) InfoRow(R.string.smstest_fee, formatAriary(op.fee), MonArgentTheme.finance.leak)
            InfoRow(R.string.smstest_balance, formatAriary(op.balanceAfter))
            op.counterpartyName?.let { name ->
                InfoRow(R.string.smstest_person, listOfNotNull(name, op.counterpartyPhone).joinToString(" · "))
            }
            op.reason?.let { InfoRow(R.string.smstest_reason, it) }
            op.operator?.let { InfoRow(R.string.smstest_operator, it) }
            InfoRow(R.string.smstest_date, op.occurredAt.format(DATE_FORMAT))
            InfoRow(R.string.smstest_ref, op.reference)
        }
    }
}

@Composable
private fun MessageCard(@DrawableRes icon: Int, @StringRes title: Int, @StringRes body: Int, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(icon, color)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun IconBadge(@DrawableRes icon: Int, color: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun InfoRow(@StringRes label: Int, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

// icone + texte + couleur pour chaque type d'operation
@Composable
private fun kindStyle(kind: OperationKind): Triple<Int, Int, Color> {
    val f = MonArgentTheme.finance
    return when (kind) {
        OperationKind.CREDIT_PURCHASE -> Triple(DesignR.drawable.ic_sim_card, R.string.smstest_kind_credit, f.leak)
        OperationKind.RECEIVED -> Triple(DesignR.drawable.ic_south_west, R.string.smstest_kind_received, f.income)
        OperationKind.SENT -> Triple(DesignR.drawable.ic_north_east, R.string.smstest_kind_sent, f.expense)
        OperationKind.WITHDRAWAL -> Triple(DesignR.drawable.ic_local_atm, R.string.smstest_kind_withdrawal, f.transfer)
        OperationKind.DEPOSIT -> Triple(DesignR.drawable.ic_account_balance, R.string.smstest_kind_deposit, f.transfer)
    }
}

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
