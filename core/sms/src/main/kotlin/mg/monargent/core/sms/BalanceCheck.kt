package mg.monargent.core.sms

// Verifie que le solde annonce par le SMS colle avec le solde precedent.
// Si ca ne colle pas, c'est qu'il manque un SMS (supprime, pas recu...)
object BalanceCheck {

    // 0 = tout va bien, sinon c'est l'ecart en Ariary (positif = argent en plus non explique)
    fun gap(previousBalance: Long, operation: ParsedSms.Operation): Long =
        operation.balanceAfter - (previousBalance + operation.balanceChange)
}
