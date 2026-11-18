package mg.volako.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mg.volako.VolakoApplication
import mg.volako.core.data.ImportReport
import mg.volako.core.data.SmsImporter
import mg.volako.core.data.UserPreferences

// Les etapes de l'ecran de bienvenue
sealed interface OnboardingState {
    data object Intro : OnboardingState
    data object Denied : OnboardingState
    data object Importing : OnboardingState
    data class Done(val report: ImportReport) : OnboardingState
    data object Error : OnboardingState
}

class OnboardingViewModel(
    private val importer: SmsImporter,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Intro)
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        if (granted) startImport() else _state.value = OnboardingState.Denied
    }

    fun startImport() {
        _state.value = OnboardingState.Importing
        viewModelScope.launch {
            _state.value = runCatching { importer.importNew() }
                .fold(onSuccess = { OnboardingState.Done(it) }, onFailure = { OnboardingState.Error })
        }
    }

    // fin de l'accueil : on ne reverra plus cet ecran
    fun finish() {
        viewModelScope.launch { preferences.setOnboardingDone() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as VolakoApplication).container
                OnboardingViewModel(c.smsImporter, c.preferences)
            }
        }
    }
}
