package mg.monargent.core.database.entity

import mg.monargent.core.model.Category
import mg.monargent.core.model.PendingSms
import mg.monargent.core.model.Transaction
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// Conversions base <-> modele. On garde la base separee du modele pour pouvoir
// changer l'un sans casser l'autre.

fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun TransactionEntity.asModel() = Transaction(
    id = id,
    type = type,
    amount = amount,
    fee = fee,
    account = account,
    toAccount = toAccount,
    category = Category.fromKey(category),
    label = label,
    counterpartyName = counterpartyName,
    counterpartyPhone = counterpartyPhone,
    occurredAt = occurredAt.toLocalDateTime(),
    source = source,
    smsReference = smsReference,
    smsBalance = smsBalance,
)

fun Transaction.asEntity() = TransactionEntity(
    id = id,
    type = type,
    amount = amount,
    fee = fee,
    account = account,
    toAccount = toAccount,
    category = category.key,
    label = label,
    counterpartyName = counterpartyName,
    counterpartyPhone = counterpartyPhone,
    occurredAt = occurredAt.toEpochMillis(),
    source = source,
    smsReference = smsReference,
    smsBalance = smsBalance,
)

fun PendingSmsEntity.asModel() = PendingSms(id = id, body = body, receivedAt = receivedAt.toLocalDateTime())

fun PendingSms.asEntity() = PendingSmsEntity(id = id, body = body, receivedAt = receivedAt.toEpochMillis())
