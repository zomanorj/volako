package mg.volako.core.sms

import java.text.Normalizer
import java.time.LocalDateTime

// Comment un SMS a ete compris (pour savoir quand MVola change ses formats)
enum class ReadMethod {
    EXACT,    // regle ecrite a la main (formats connus)
    TEMPLATE, // modele appris d'une confirmation de l'utilisateur
    GENERIC,  // lecteur universel verifie par le solde
}

// La "forme" d'un SMS appris : les mots fixes + la place de chaque nombre.
// Ex: "achat offre <w> <N> ar pour <N> . frais <N> ar . solde mvola <N> ar . ref <N>"
data class SmsTemplate(
    val id: Long = 0,
    // les mots du SMS, les nombres remplaces par "#"
    val words: List<String>,
    // nombre de nombres dans le SMS (doit etre pareil pour correspondre)
    val numberCount: Int,
    val amountSlot: Int,
    val feeSlot: Int?,
    val balanceSlot: Int?,
    val referenceSlot: Int?,
    val phoneSlot: Int?,
    val kind: OperationKind,
    // libelle choisi par l'utilisateur, repris pour les SMS suivants
    val label: String?,
)

object SmsTemplates {

    // a partir de 75% de mots en commun (dans le meme ordre), c'est la meme forme
    private const val MIN_SIMILARITY = 0.75

    // apprend la forme d'un SMS que l'utilisateur vient de classer. null si on ne retrouve pas
    // le montant qu'il a confirme dans le texte (dans ce cas on n'apprend rien).
    // previousBalance aide a trouver le solde meme si le SMS utilise un mot inconnu
    fun learn(
        body: String,
        kind: OperationKind,
        amount: Long,
        label: String?,
        previousBalance: Long? = null,
    ): SmsTemplate? {
        val shape = shape(body)
        val amountSlot = shape.numbers.indexOfFirst { it.value == amount }.takeIf { it >= 0 } ?: return null
        val clues = GenericSmsReader.read(body)

        fun slotOf(value: String?, exclude: Set<Int>): Int? = value?.let { v ->
            shape.numbers.indices.firstOrNull { it !in exclude && shape.numbers[it].raw == v }
        }
        fun slotOf(value: Long?, exclude: Set<Int>): Int? = value?.let { v ->
            shape.numbers.indices.firstOrNull { it !in exclude && shape.numbers[it].value == v }
        }

        // le solde : soit trouve par mot-cle, soit le nombre qui vaut "ancien solde +/- montant (+ frais)"
        val balanceSlot = slotOf(clues?.balance, setOf(amountSlot))
            ?: previousBalance?.let { prev -> balanceByArithmetic(shape, amountSlot, prev, kind) }
        val feeSlot = slotOf(clues?.fee, setOfNotNull(amountSlot, balanceSlot))
            ?: feeByArithmetic(shape, amountSlot, balanceSlot, previousBalance)
        val used = setOfNotNull(amountSlot, balanceSlot, feeSlot)
        val referenceSlot = clues?.takeIf { !it.referenceIsSynthetic }?.let { slotOf(it.reference, used) }
        val phoneSlot = slotOf(clues?.phone, used + setOfNotNull(referenceSlot))

        return SmsTemplate(
            words = shape.words,
            numberCount = shape.numbers.size,
            amountSlot = amountSlot,
            feeSlot = feeSlot,
            balanceSlot = balanceSlot,
            referenceSlot = referenceSlot,
            phoneSlot = phoneSlot,
            kind = kind,
            label = label?.trim()?.ifEmpty { null },
        )
    }

    private fun isIncoming(kind: OperationKind) = kind == OperationKind.RECEIVED || kind == OperationKind.DEPOSIT

    private fun balanceByArithmetic(shape: Shape, amountSlot: Int, previous: Long, kind: OperationKind): Int? {
        val amount = shape.numbers[amountSlot].value
        return shape.numbers.indices.firstOrNull { i ->
            if (i == amountSlot) return@firstOrNull false
            val delta = shape.numbers[i].value - previous
            if (isIncoming(kind)) {
                delta == amount
            } else {
                // sortie : -montant, ou -(montant + frais) avec des frais qui sont aussi dans le SMS
                delta == -amount || shape.numbers.any { it.value > 0 && delta == -(amount + it.value) }
            }
        }
    }

    private fun feeByArithmetic(shape: Shape, amountSlot: Int, balanceSlot: Int?, previous: Long?): Int? {
        if (balanceSlot == null || previous == null) return null
        val extra = previous - shape.numbers[balanceSlot].value - shape.numbers[amountSlot].value
        if (extra <= 0) return null
        return shape.numbers.indices.firstOrNull { it != amountSlot && it != balanceSlot && shape.numbers[it].value == extra }
    }

    // le SMS a-t-il la meme forme qu'un modele ? Si oui on lit les nombres a leur place
    fun match(template: SmsTemplate, body: String, receivedAt: LocalDateTime): ParsedSms.Operation? {
        val shape = shape(body)
        if (shape.numbers.size != template.numberCount) return null
        if (similarity(template.words, shape.words) < MIN_SIMILARITY) return null

        val n = shape.numbers
        val clues = GenericSmsReader.read(body)
        return ParsedSms.Operation(
            kind = template.kind,
            amount = n[template.amountSlot].value,
            fee = template.feeSlot?.let { n[it].value } ?: 0,
            // pas de solde dans ce type de SMS : on ne peut pas le verifier, on garde -1
            balanceAfter = template.balanceSlot?.let { n[it].value } ?: NO_BALANCE,
            reference = template.referenceSlot?.let { n[it].raw } ?: clues?.reference ?: return null,
            occurredAt = MvolaSmsParser.bestTime(clues?.occurredAt, receivedAt),
            counterpartyName = clues?.name,
            counterpartyPhone = template.phoneSlot?.let { n[it].raw },
            reason = template.label,
            readBy = ReadMethod.TEMPLATE,
        )
    }

    // valeur speciale : ce SMS n'annonce pas de solde
    const val NO_BALANCE = -1L

    // ---- forme d'un SMS ----

    private data class Number(val raw: String, val value: Long)
    private data class Shape(val words: List<String>, val numbers: List<Number>)

    private fun shape(body: String): Shape {
        var text = Normalizer.normalize(body, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[\\s\\u00A0\\u202F]+"), " ").lowercase()
        // dates et heures : un seul "mot" fixe, elles changent a chaque SMS
        text = text.replace(DATE, " @date ").replace(TIME, " @time ")
        val numbers = NUMBER.findAll(text).map { Number(it.value.replace(" ", ""), it.value.replace(" ", "").toLong()) }.toList()
        val words = NUMBER.replace(text, " # ").split(Regex("[^a-z0-9#@]+")).filter { it.isNotEmpty() }
        return Shape(words, numbers)
    }

    // part de mots communs dans le meme ordre (plus longue sous-suite commune)
    private fun similarity(a: List<String>, b: List<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val dp = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in 1..a.size) for (j in 1..b.size) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1] + 1 else maxOf(dp[i - 1][j], dp[i][j - 1])
        }
        return dp[a.size][b.size].toDouble() / maxOf(a.size, b.size)
    }

    private val DATE = Regex("\\d{1,2}/\\d{1,2}/\\d{2,4}")
    private val TIME = Regex("\\d{1,2}[:h]\\d{2}")
    // "2 000 000" est UN nombre (espaces des milliers), un numero de telephone aussi
    private val NUMBER = Regex("\\d{1,3}(?: \\d{3})+(?!\\d)|\\d+")
}
