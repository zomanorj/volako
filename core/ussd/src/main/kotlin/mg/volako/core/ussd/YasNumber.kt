package mg.volako.core.ussd

// Numero Yas (034 ou 038) toujours garde au format 10 chiffres : 0341234567
@JvmInline
value class YasNumber private constructor(val digits: String) {

    // Pour l'affichage : 034 12 345 67 (comme dans les messages Yas)
    fun pretty(): String =
        "${digits.substring(0, 3)} ${digits.substring(3, 5)} ${digits.substring(5, 8)} ${digits.substring(8)}"

    override fun toString(): String = digits

    companion object {
        private val YAS_PREFIXES = listOf("034", "038")

        // Accepte "034 12 345 67", "(034) 1234567", "+261341234567"... null si ce n'est pas un numero Yas
        fun parse(raw: String): YasNumber? {
            var d = raw.filter { it.isDigit() }
            // +261 34 ... -> 034 ...
            if (d.startsWith("261") && d.length == 12) d = "0" + d.substring(3)
            if (d.length != 10) return null
            if (YAS_PREFIXES.none { d.startsWith(it) }) return null
            return YasNumber(d)
        }
    }
}
