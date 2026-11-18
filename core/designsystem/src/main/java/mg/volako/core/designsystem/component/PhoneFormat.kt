package mg.volako.core.designsystem.component

// "0345358300" -> "034 53 583 00" (format malgache, bien plus lisible)
fun formatPhone(phone: String): String {
    val digits = phone.filter(Char::isDigit)
    if (digits.length != 10 || !digits.startsWith("0")) return phone
    return "${digits.substring(0, 3)} ${digits.substring(3, 5)} ${digits.substring(5, 8)} ${digits.substring(8)}"
}
