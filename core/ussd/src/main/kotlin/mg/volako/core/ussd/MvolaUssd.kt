package mg.volako.core.ussd

// Fabrique les codes USSD MVola "tout-en-un".
// Chaque chemin a ete verifie sur le vrai reseau Yas le 19/11/2026,
// en s'arretant a la demande du code secret (jamais tape).
object MvolaUssd {

    const val MIN_AMOUNT = 100L          // plus petit montant teste
    const val MAX_AMOUNT = 20_000_000L   // plafond MVola par operation (mvola.mg)
    const val MAX_DESCRIPTION = 20

    private const val MVOLA = "111*1"
    private const val OFFER_MENU = "$MVOLA*1*3"

    fun build(op: UssdOperation): UssdResult = when (op) {
        UssdOperation.CreditBalance -> ready("#357#", NextOnPhone.RESULT)

        is UssdOperation.CreditForMe -> checkAmount(op.amount)
            ?: ready(code(MVOLA, "1", "1", "1", op.amount), NextOnPhone.SECRET_CODE)

        is UssdOperation.CreditForOther -> withNumber(op.number) { n ->
            checkAmount(op.amount) ?: ready(code(MVOLA, "1", "2", "1", n, op.amount), NextOnPhone.SECRET_CODE)
        }

        is UssdOperation.OfferForMe ->
            ready(code(OFFER_MENU, *op.offer.menuPath.toTypedArray()), NextOnPhone.SECRET_CODE)

        UssdOperation.OfferForOther -> ready(code(MVOLA, "1", "4"), NextOnPhone.MENU)

        is UssdOperation.Send -> withNumber(op.number) { n -> send(n, op) }

        is UssdOperation.WithdrawAtAgent -> withNumber(op.agentNumber) { n ->
            checkAmount(op.amount) ?: ready(code(MVOLA, "4", "1", n, op.amount), NextOnPhone.SECRET_CODE)
        }

        UssdOperation.WithdrawAtAtm -> ready(code(MVOLA, "4", "2"), NextOnPhone.SECRET_CODE)
    }

    // Envoi : numero, montant, frais de retrait (1 oui / 2 non), puis description
    private fun send(n: YasNumber, op: UssdOperation.Send): UssdResult {
        checkAmount(op.amount)?.let { return it }
        val fee = if (op.payRecipientFee) "1" else "2"
        val desc = op.description?.trim().orEmpty()
        if (desc.isEmpty()) {
            return ready(code(MVOLA, "2", n, op.amount, fee), NextOnPhone.DESCRIPTION)
        }
        if (!desc.all { it.isDigit() }) return refused(UssdError.DESCRIPTION_NOT_DIGITS)
        if (desc.length > MAX_DESCRIPTION) return refused(UssdError.DESCRIPTION_TOO_LONG)
        return ready(code(MVOLA, "2", n, op.amount, fee, desc), NextOnPhone.SECRET_CODE)
    }

    private inline fun withNumber(raw: String, next: (YasNumber) -> UssdResult): UssdResult {
        val n = YasNumber.parse(raw) ?: return refused(UssdError.INVALID_NUMBER)
        return next(n)
    }

    private fun checkAmount(amount: Long): UssdResult? = when {
        amount < MIN_AMOUNT -> refused(UssdError.AMOUNT_TOO_SMALL)
        amount > MAX_AMOUNT -> refused(UssdError.AMOUNT_TOO_BIG)
        else -> null
    }

    // code("111*1", "2", numero, 100) -> "#111*1*2*0341234567*100#"
    private fun code(vararg parts: Any): String = parts.joinToString("*", prefix = "#", postfix = "#")

    private fun ready(code: String, next: NextOnPhone) = UssdResult.Ready(UssdRequest(code, next))

    private fun refused(error: UssdError) = UssdResult.Refused(error)
}
