package mg.volako.core.designsystem.component

// 200388 -> "200 388 Ar" (espace insecable pour que "Ar" ne parte pas a la ligne)
fun formatAriary(amount: Long): String = formatNumber(amount) + " Ar"

// 200388 -> "200 388" (pour les petites tuiles, ou "Ar" est sous-entendu).
// Espace fine insecable entre les milliers.
fun formatNumber(amount: Long): String {
    val digits = kotlin.math.abs(amount).toString().reversed().chunked(3).joinToString(" ").reversed()
    val sign = if (amount < 0) "−" else ""
    return sign + digits
}
