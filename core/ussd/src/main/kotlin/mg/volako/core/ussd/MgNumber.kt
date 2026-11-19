package mg.volako.core.ussd

// Operateurs mobiles malgaches et leurs prefixes
enum class Operator(val label: String, val prefixes: List<String>) {
    YAS("Yas", listOf("034", "038")),
    ORANGE("Orange", listOf("032", "037")),
    AIRTEL("Airtel", listOf("033")),
}

// Numero mobile malgache toujours garde au format 10 chiffres : 0341234567
@JvmInline
value class MgNumber private constructor(val digits: String) {

    val operator: Operator get() = Operator.entries.first { op -> op.prefixes.any { digits.startsWith(it) } }

    val isYas: Boolean get() = operator == Operator.YAS

    // Pour l'affichage : 034 12 345 67 (comme dans les messages Yas)
    fun pretty(): String =
        "${digits.substring(0, 3)} ${digits.substring(3, 5)} ${digits.substring(5, 8)} ${digits.substring(8)}"

    override fun toString(): String = digits

    companion object {

        // "03420" -> "034 20" : met en forme pendant la saisie (3-2-3-2 comme les messages Yas)
        fun formatPartial(digits: String): String {
            val d = digits.filter { it.isDigit() }.take(10)
            val sizes = listOf(3, 2, 3, 2)
            val parts = mutableListOf<String>()
            var start = 0
            for (size in sizes) {
                if (start >= d.length) break
                parts += d.substring(start, minOf(start + size, d.length))
                start += size
            }
            return parts.joinToString(" ")
        }

        // Numero Yas seulement (credit, offres, retrait : menus Yas). Accepte "034 12 345 67", "+261 34..."
        fun parse(raw: String): MgNumber? = parseAny(raw)?.takeIf { it.isYas }

        // N'importe quel operateur malgache (envoi d'argent MVola vers Orange / Airtel)
        fun parseAny(raw: String): MgNumber? {
            var d = raw.filter { it.isDigit() }
            // +261 34 ... -> 034 ...
            if (d.startsWith("261") && d.length == 12) d = "0" + d.substring(3)
            if (d.length != 10) return null
            if (Operator.entries.none { op -> op.prefixes.any { d.startsWith(it) } }) return null
            return MgNumber(d)
        }
    }
}
