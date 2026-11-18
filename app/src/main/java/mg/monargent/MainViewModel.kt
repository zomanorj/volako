package mg.monargent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import mg.monargent.core.data.UserPreferences

// Decide quel ecran montrer au demarrage. null = pas encore charge (on garde le splash)
class MainViewModel(preferences: UserPreferences) : ViewModel() {

    val onboardingDone: StateFlow<Boolean?> =
        preferences.onboardingDone.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                MainViewModel((this[APPLICATION_KEY] as MonArgentApplication).container.preferences)
            }
        }
    }
}
