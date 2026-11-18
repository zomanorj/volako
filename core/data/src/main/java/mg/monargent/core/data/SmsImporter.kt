package mg.monargent.core.data

import mg.monargent.core.sms.MvolaSmsParser

// Importe les SMS MVola pas encore lus : tout l'historique la 1re fois, puis seulement les nouveaux.
// Si le lecteur a appris un nouveau format depuis, on relit TOUT une fois (les doublons sont
// ignores grace a la ref MVola) pour recuperer les SMS qu'on ne savait pas lire avant.
class SmsImporter(
    private val reader: SmsInboxReader,
    private val repository: TransactionRepository,
    private val preferences: UserPreferences,
) {
    suspend fun importNew(): ImportReport {
        val parserChanged = preferences.parserVersion() < MvolaSmsParser.VERSION
        val since = if (parserChanged) 0L else preferences.lastImportedSmsDate()

        val sms = reader.readMvola(since)
        val report = if (sms.isEmpty()) ImportReport() else repository.importSms(sms.map { it.second })

        if (parserChanged) {
            // les SMS "a verifier" sont peut-etre lisibles maintenant
            repository.reprocessPendingSms()
            preferences.setParserVersion(MvolaSmsParser.VERSION)
        }
        // on retient le plus recent pour la prochaine fois
        sms.maxOfOrNull { it.first }?.let { latest ->
            if (latest > preferences.lastImportedSmsDate()) preferences.setLastImportedSmsDate(latest)
        }
        return report
    }
}
