package mg.volako.core.ussd

// Fabrique les codes USSD MVola "tout-en-un".
// Chaque chemin a ete verifie sur le vrai reseau Yas le 19/11/2026,
// en s'arretant a la demande du code secret (jamais tape).
object MvolaUssd {

    const val MIN_AMOUNT = 100L          // plus petit montant teste
    const val MAX_AMOUNT = 20_000_000L   // plafond MVola par operation (mvola.mg)
    // limite Yas (dit par l'utilisateur le 19/11/2026) : 20 caracteres, sans espace
    const val MAX_DESCRIPTION = 20
    // frais Yas pour un credit ou une offre vers un autre numero (mvola.mg, quel que soit le montant)
    const val OTHER_NUMBER_FEE = 150L

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
            ready(code(OFFER_MENU, *op.offer.selfPath.toTypedArray()), NextOnPhone.SECRET_CODE)

        // ATTENTION : en tout-en-un c'est *5 (pas *4 comme dans le menu affiche).
        // Avec *4, Yas ignore le numero et achete l'offre pour soi (teste le 19/11/2026).
        is UssdOperation.OfferForOther -> withNumber(op.number) { n ->
            ready(code(MVOLA, "1", "5", n, *op.offer.otherPath.toTypedArray()), NextOnPhone.SECRET_CODE)
        }

        is UssdOperation.OfferWithAirtime -> op.offer.airtimeCode
            ?.let { ready(it, NextOnPhone.INSTANT) }
            ?: refused(UssdError.NO_AIRTIME_CODE)

        // l'envoi marche aussi vers Orange et Airtel
        is UssdOperation.Send -> MgNumber.parseAny(op.number)?.let { send(it, op) } ?: refused(UssdError.INVALID_MOBILE_NUMBER)

        is UssdOperation.WithdrawAtAgent -> withNumber(op.agentNumber) { n ->
            checkAmount(op.amount) ?: ready(code(MVOLA, "4", "1", n, op.amount), NextOnPhone.SECRET_CODE)
        }

        UssdOperation.WithdrawAtAtm -> ready(code(MVOLA, "4", "2"), NextOnPhone.SECRET_CODE)
    }

    // Envoi vers Yas : numero, montant, frais de retrait (1 oui / 2 non), description.
    // Vers Orange / Airtel : pas de question sur les frais de retrait (dit par l'utilisateur, 19/11/2026).
    private fun send(n: MgNumber, op: UssdOperation.Send): UssdResult {
        checkAmount(op.amount)?.let { return it }
        val steps = buildList<Any> {
            add(MVOLA); add("2"); add(n); add(op.amount)
            if (n.isYas) add(if (op.payRecipientFee) "1" else "2")
        }
        val desc = op.description?.trim().orEmpty()
        if (desc.any { it.isWhitespace() }) return refused(UssdError.DESCRIPTION_HAS_SPACE)
        if (desc.length > MAX_DESCRIPTION) return refused(UssdError.DESCRIPTION_TOO_LONG)
        return when {
            desc.isEmpty() -> ready(code(*steps.toTypedArray()), NextOnPhone.DESCRIPTION)
            // que des chiffres : directement dans le code (le plus rapide)
            desc.all { it.isDigit() } -> ready(code(*(steps + desc).toTypedArray()), NextOnPhone.SECRET_CODE)
            // avec des lettres : le code s'arrete a la description, l'app la tapera dans la bulle
            else -> UssdResult.Ready(UssdRequest(code(*steps.toTypedArray()), NextOnPhone.DESCRIPTION, fillDescription = desc))
        }
    }

    private inline fun withNumber(raw: String, next: (MgNumber) -> UssdResult): UssdResult {
        val n = MgNumber.parse(raw) ?: return refused(UssdError.INVALID_NUMBER)
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
