package mg.monargent.core.designsystem.component

// 200388 -> "200 388 Ar" (espace fine insecable pour que "Ar" ne parte pas a la ligne)
fun formatAriary(amount: Long): String {
    val digits = kotlin.math.abs(amount).toString().reversed().chunked(3).joinToString(" ").reversed()
    val sign = if (amount < 0) "−" else ""
    return "$sign$digits Ar"
}
