package mg.monargent.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// une seule instance de DataStore par fichier (regle de la doc officielle)
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

// Petits reglages gardes entre deux lancements
class UserPreferences(private val context: Context) {

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    // date (ms) du dernier SMS importe, pour ne relire que les nouveaux
    suspend fun lastImportedSmsDate(): Long = context.dataStore.data.first()[LAST_SMS_DATE] ?: 0L

    suspend fun setLastImportedSmsDate(millis: Long) {
        context.dataStore.edit { it[LAST_SMS_DATE] = millis }
    }

    // version du lecteur de SMS utilisee lors du dernier import complet
    suspend fun parserVersion(): Int = context.dataStore.data.first()[PARSER_VERSION] ?: 1

    suspend fun setParserVersion(version: Int) {
        context.dataStore.edit { it[PARSER_VERSION] = version }
    }

    // pour ne demander qu'une fois les permissions "reception SMS" et "notifications"
    suspend fun livePermissionsAsked(): Boolean = context.dataStore.data.first()[LIVE_PERMS_ASKED] ?: false

    suspend fun setLivePermissionsAsked() {
        context.dataStore.edit { it[LIVE_PERMS_ASKED] = true }
    }

    // ecarts de solde que l'utilisateur a deja vus ("Compris")
    val acknowledgedGaps: Flow<Set<String>> = context.dataStore.data.map { it[ACK_GAPS].orEmpty() }

    suspend fun acknowledgeGap(key: String) {
        context.dataStore.edit { it[ACK_GAPS] = it[ACK_GAPS].orEmpty() + key }
    }

    private companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_SMS_DATE = longPreferencesKey("last_imported_sms_date")
        val LIVE_PERMS_ASKED = booleanPreferencesKey("live_permissions_asked")
        val ACK_GAPS = stringSetPreferencesKey("acknowledged_gaps")
        val PARSER_VERSION = intPreferencesKey("parser_version")
    }
}
