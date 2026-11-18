package mg.monargent.core.data

// Importe les SMS MVola pas encore lus : tout l'historique la 1re fois, puis seulement les nouveaux
class SmsImporter(
    private val reader: SmsInboxReader,
    private val repository: TransactionRepository,
    private val preferences: UserPreferences,
) {
    suspend fun importNew(): ImportReport {
        val since = preferences.lastImportedSmsDate()
        val sms = reader.readMvola(since)
        if (sms.isEmpty()) return ImportReport()

        val report = repository.importSms(sms.map { it.second })
        // on retient le plus recent pour la prochaine fois
        preferences.setLastImportedSmsDate(sms.maxOf { it.first })
        return report
    }
}
