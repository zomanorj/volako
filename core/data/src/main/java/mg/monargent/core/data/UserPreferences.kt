package mg.monargent.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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

    private companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_SMS_DATE = longPreferencesKey("last_imported_sms_date")
    }
}
