package mg.monargent.core.sms

import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Transforme le texte d'un SMS MVola en operation.
// Formats releves sur de vrais SMS (nov. 2026), voir MvolaSmsParserTest.
object MvolaSmsParser {

    const val SENDER = "MVola"

    // a augmenter a chaque nouveau format reconnu : l'app relit alors tout l'historique une fois
    // v2 : achat de credit pour un autre numero (avec frais)
    // v3 : lecteur universel verifie par le solde (GenericSmsReader + SmartSmsInterpreter)
    const val VERSION = 3

    // ---- morceaux de regex (declares en premier car les regles les utilisent) ----

    // nom puis numero, avec ou sans parentheses : "MARIE ANICK 4 0380000000" ou "IDEE-ALL (0340000000)"
    private const val WHO = "(?<name>.+?) \\(?(?<phone>\\+?\\d{6,})\\)?"
    private const val WHEN = "le (?<date>\\d{1,2}/\\d{1,2}/\\d{2,4}) a (?<time>\\d{1,2}:\\d{2})"
    private val FEE_OPT = "(?: Frais ?: ${amount("fee")} Ar\\.)?"
    private const val REASON_OPT = "(?: Raison ?: (?<reason>.*?)\\.)?"
    private val BALANCE = "Solde(?: MVola)? ?: ${amount("balance")} Ar"
    private const val REF = "Ref ?:? ?(?<ref>\\d+)"

    private val ACCENTS = Regex("\\p{Mn}+")
    private val SPACES = Regex("[\\s\\u00A0\\u202F]+")
    private val PART_PREFIX = Regex("^(\\d)/(\\d) ")
    private val FIRST_AMOUNT = Regex("(\\d{1,3}(?: \\d{3})*) ?Ar\\b", RegexOption.IGNORE_CASE)
    private val LOOKS_LIKE_OPERATION = Regex("\\d Ar\\b.*\\bRef\\b", RegexOption.IGNORE_CASE)

    fun isFromMvola(address: String?): Boolean = address.equals(SENDER, ignoreCase = true)

    // receivedAt = heure de reception du SMS, utilisee si le texte n'a pas de date (achat de credit)
    fun parse(body: String, receivedAt: LocalDateTime): ParsedSms {
        val text = normalize(body)

        // "2/2 Les depots MVola sont toujours GRATUITS..." : suite d'un SMS, rien a lire
        val part = PART_PREFIX.find(text)
        val content = if (part != null) text.substring(part.range.last + 1) else text
        val isFollowUpPart = part != null && part.groupValues[1].toInt() > 1

        for (rule in rules) {
            val match = rule.regex.find(content) ?: continue
            return rule.build(match, receivedAt)
        }

        // pas reconnu : si ca parle d'argent avec une reference, on le garde pour verification
        return if (!isFollowUpPart && LOOKS_LIKE_OPERATION.containsMatchIn(content)) {
            ParsedSms.Unknown(body)
        } else {
            ParsedSms.Ignored
        }
    }

    // premier montant "xxx Ar" du texte, pour pre-remplir le classement d'un SMS inconnu
    fun firstAmount(body: String): Long? =
        FIRST_AMOUNT.find(normalize(body))?.groupValues?.get(1)?.let(::toLong)

    // ---- normalisation ----

    // enleve les accents (recu / reçu) et met les espaces bizarres en espace simple
    private fun normalize(raw: String): String {
        val noAccents = Normalizer.normalize(raw, Normalizer.Form.NFD).replace(ACCENTS, "")
        return noAccents.replace(SPACES, " ").trim()
    }

    // ---- regles ----

    private class Rule(val regex: Regex, val build: (MatchResult, LocalDateTime) -> ParsedSms.Operation)

    private val rules = listOf(
        // Achat de credit YAS reussi: 500 Ar. Bonus: ... Solde MVola : 388 Ar. Ref: 10909436462
        // Achat de credit YAS reussi: 100 Ar pour 0340000000. Frais: 150 Ar. Solde MVola : 3 588 Ar. Ref: ...
        Rule(
            regex(
                "^Achat de credit (?<operator>\\S+) reussi ?: ${amount("amount")} Ar" +
                    "(?: pour (?<phone>\\+?\\d{6,}))?\\.$FEE_OPT.*? $BALANCE\\.? $REF",
            ),
        ) { m, at ->
            ParsedSms.Operation(
                kind = OperationKind.CREDIT_PURCHASE,
                amount = m.long("amount"),
                fee = m.text("fee")?.let(::toLong) ?: 0,
                balanceAfter = m.long("balance"),
                reference = m.text("ref")!!,
                occurredAt = at,
                // rempli seulement si le credit est pour quelqu'un d'autre
                counterpartyPhone = m.text("phone"),
                operator = m.text("operator"),
            )
        },
        // 200 000 Ar recu de NOM 0340000000 le 16/11/26 a 07:39. Raison: 37. Solde: 200 388 Ar. Ref 10909856405
        Rule(regex("^${amount("amount")} Ar recu de $WHO $WHEN\\.$FEE_OPT$REASON_OPT $BALANCE\\.? $REF")) { m, at ->
            operation(OperationKind.RECEIVED, m, at)
        },
        // 81 900 Ar envoye a NOM 0340000000 le ... Frais: 819 Ar. Raison: ... Solde: 64 913 Ar. Ref: 10925498148
        Rule(regex("^${amount("amount")} Ar envoye a $WHO $WHEN\\.$FEE_OPT$REASON_OPT $BALANCE\\.? $REF")) { m, at ->
            operation(OperationKind.SENT, m, at)
        },
        // Retrait reussi: 10 000 Ar aupres de NOM 0380000000 le ... Frais: 275 Ar. Solde : 190 113 Ar. Ref: ...
        Rule(regex("^Retrait reussi ?: ${amount("amount")} Ar aupres de $WHO $WHEN\\.$FEE_OPT $BALANCE\\.? $REF")) { m, at ->
            operation(OperationKind.WITHDRAWAL, m, at)
        },
        // Depot reussi : 2 000 000 Ar aupres de NOM (0340000000) le ... Solde : 2 039 250 Ar. Ref: ...
        Rule(regex("^Depot reussi ?: ${amount("amount")} Ar aupres de $WHO $WHEN\\.$FEE_OPT $BALANCE\\.? $REF")) { m, at ->
            operation(OperationKind.DEPOSIT, m, at)
        },
    )

    // les 4 formats "avec date et personne" se construisent pareil
    private fun operation(kind: OperationKind, m: MatchResult, receivedAt: LocalDateTime) =
        ParsedSms.Operation(
            kind = kind,
            amount = m.long("amount"),
            fee = m.text("fee")?.let(::toLong) ?: 0,
            balanceAfter = m.long("balance"),
            reference = m.text("ref")!!,
            occurredAt = parseDate(m.text("date")!!, m.text("time")!!) ?: receivedAt,
            counterpartyName = m.text("name")?.trim(),
            counterpartyPhone = m.text("phone"),
            reason = m.text("reason")?.trim()?.ifEmpty { null },
        )

    // "16/11/26" + "07:39" -> 2026-11-16T07:39
    private fun parseDate(date: String, time: String): LocalDateTime? = runCatching {
        val (d, mo, y) = date.split("/").map { it.toInt() }
        val (h, mi) = time.split(":").map { it.toInt() }
        val year = if (y < 100) 2000 + y else y
        LocalDateTime.of(LocalDate.of(year, mo, d), LocalTime.of(h, mi))
    }.getOrNull()

    // ---- petits outils ----

    // montant avec espaces entre les milliers : "2 000 000"
    private fun amount(name: String) = "(?<$name>\\d{1,3}(?: \\d{3})*)"

    private fun regex(pattern: String) = Regex(pattern, RegexOption.IGNORE_CASE)

    private fun toLong(value: String): Long = value.replace(" ", "").toLong()

    // null si le groupe n'existe pas dans cette regle (ex: pas de "reason" pour un retrait)
    private fun MatchResult.text(name: String): String? =
        runCatching { groups[name]?.value }.getOrNull()

    private fun MatchResult.long(name: String): Long = toLong(text(name)!!)
}
