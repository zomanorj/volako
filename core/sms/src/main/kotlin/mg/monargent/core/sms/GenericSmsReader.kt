package mg.monargent.core.sms

import java.security.MessageDigest
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Ce qu'on a pu extraire d'un SMS qu'aucune regle exacte ne connait.
// Rien n'est sur ici : c'est le calcul du solde (couche domaine) qui decide si on peut s'y fier.
data class SmsClues(
    val amount: Long,
    val fee: Long?,
    // nouveau solde annonce, null si le SMS n'en parle pas
    val balance: Long?,
    val reference: String,
    // vrai si la ref a ete fabriquee (le SMS n'en avait pas)
    val referenceIsSynthetic: Boolean,
    val phone: String?,
    val name: String?,
    val occurredAt: LocalDateTime?,
    // ce que les mots suggerent (null = on ne sait pas)
    val hint: OperationKind?,
)

// Lecteur "universel" : ne depend pas de la formulation exacte. Il cherche des indices
// (montants, mots-cles, solde, reference, numero) dans n'importe quel ordre.
object GenericSmsReader {

    fun read(body: String): SmsClues? {
        val text = normalize(body)

        // le solde : un montant juste apres "solde", "nouveau solde", "reste"... ("Ar" pas obligatoire)
        val balanceMatch = BALANCE.find(text)
        val balance = balanceMatch?.groupValues?.get(1)?.let(::toLong)

        // tous les "xxx Ar" du texte, sauf celui du solde
        val amounts = AMOUNT.findAll(text)
            .filter { m -> balanceMatch == null || m.range.first !in balanceMatch.range }
            .toList()
        if (amounts.isEmpty()) return null

        // un montant precede d'un mot comme "frais" ou "cout" = les frais
        val feeMatch = amounts.firstOrNull { m -> FEE_WORDS.containsMatchIn(text.window(m.range.first)) }
        val main = amounts.firstOrNull { it != feeMatch } ?: return null

        val reference = REFERENCE.find(text)?.groupValues?.get(1)
        val phone = PHONE.find(text)?.value?.takeIf { it != reference }

        return SmsClues(
            amount = toLong(main.groupValues[1]),
            fee = feeMatch?.groupValues?.get(1)?.let(::toLong),
            balance = balance,
            reference = reference ?: syntheticReference(text),
            referenceIsSynthetic = reference == null,
            phone = phone,
            name = phone?.let { nameBefore(text, it) },
            occurredAt = date(text),
            hint = hint(text),
        )
    }

    // ---- mots-cles (francais + malgache). A completer avec de vrais SMS quand on en voit ----

    private fun hint(text: String): OperationKind? = when {
        WITHDRAWAL_WORDS.containsMatchIn(text) -> OperationKind.WITHDRAWAL
        DEPOSIT_WORDS.containsMatchIn(text) -> OperationKind.DEPOSIT
        CREDIT_WORDS.containsMatchIn(text) -> OperationKind.CREDIT_PURCHASE
        RECEIVED_WORDS.containsMatchIn(text) -> OperationKind.RECEIVED
        SENT_WORDS.containsMatchIn(text) -> OperationKind.SENT
        else -> null
    }

    private val WITHDRAWAL_WORDS = words("retrait", "retire", "misintona", "nalaina")
    private val DEPOSIT_WORDS = words("depot", "depose", "napetraka")
    private val CREDIT_WORDS = words("credit", "offre", "forfait", "pass", "bundle", "airtime", "fahana")
    private val RECEIVED_WORDS = words("recu", "recus", "credite", "reception", "voaray", "nahazo", "received")
    private val SENT_WORDS = words(
        "envoye", "envoi", "transfere", "transfert", "paye", "paiement", "achat", "debite",
        "nalefa", "nandefa", "nividy", "nandoa", "sent", "paid",
    )
    private val FEE_WORDS = words("frais", "cout", "commission", "sarany", "fee", "fees")

    private fun words(vararg w: String) = Regex("\\b(" + w.joinToString("|") + ")\\b", RegexOption.IGNORE_CASE)

    // ---- extraction ----

    private const val AMT = "(\\d{1,3}(?: \\d{3})+|\\d+)"
    private val AMOUNT = Regex("$AMT ?(?:Ar|MGA|Ariary)\\b", RegexOption.IGNORE_CASE)
    private val BALANCE = Regex(
        "(?:nouveau solde|solde(?: mvola| disponible| actuel)?|reste|ambiny|balance)\\s*(?:est de|:|=)?\\s*$AMT",
        RegexOption.IGNORE_CASE,
    )
    private val REFERENCE = Regex(
        "\\b(?:ref|reference|id|transaction|trans id|txn)\\b\\s*(?:n°|no|:|#)?\\s*(\\d{6,})",
        RegexOption.IGNORE_CASE,
    )
    private val PHONE = Regex("(?<!\\d)(?:\\+?261|0)3\\d{8}(?!\\d)")
    private val DATE = Regex("(\\d{1,2})/(\\d{1,2})/(\\d{2,4})\\D{1,5}(\\d{1,2})[:h](\\d{2})")

    // les ~30 caracteres avant un montant, pour savoir ce qu'il represente
    private fun String.window(index: Int) = substring((index - 30).coerceAtLeast(0), index)

    // "... a Toky Rabe 0340000000 ..." -> "Toky Rabe" (au plus 4 mots juste avant le numero)
    private fun nameBefore(text: String, phone: String): String? {
        val before = text.substringBefore(phone).trimEnd(' ', '(')
        val words = before.split(' ').takeLast(4)
        val start = words.indexOfLast { it.lowercase() in LINK_WORDS }
        val name = (if (start >= 0) words.drop(start + 1) else emptyList()).joinToString(" ").trim()
        return name.takeIf { it.isNotEmpty() && it.any(Char::isLetter) }
    }

    private val LINK_WORDS = setOf("a", "de", "pour", "chez", "aupres", "vers", "amin'i", "tamin'i", "ho")

    private fun date(text: String): LocalDateTime? {
        val m = DATE.find(text) ?: return null
        return runCatching {
            val (d, mo, y, h, mi) = m.destructured
            val year = y.toInt().let { if (it < 100) 2000 + it else it }
            LocalDateTime.of(LocalDate.of(year, mo.toInt(), d.toInt()), LocalTime.of(h.toInt(), mi.toInt()))
        }.getOrNull()
    }

    // pas de ref dans le SMS : on en fabrique une stable a partir du texte (pour eviter les doublons)
    private fun syntheticReference(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return "sms-" + digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun toLong(value: String): Long = value.replace(" ", "").toLong()

    private fun normalize(raw: String): String {
        val noAccents = Normalizer.normalize(raw, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return noAccents.replace(Regex("[\\s\\u00A0\\u202F]+"), " ").trim()
    }
}
