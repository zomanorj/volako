package mg.volako.ui.mvola

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mg.volako.VolakoApplication
import mg.volako.core.data.TransactionRepository
import mg.volako.core.domain.ContactKind
import mg.volako.core.domain.RecentContact
import mg.volako.core.domain.RecentContacts

// Numeros proposes dans l'onglet MVola, retrouves dans l'historique des SMS
data class MvolaUiState(
    val people: List<RecentContact> = emptyList(),
    val creditTargets: List<RecentContact> = emptyList(),
    val agents: List<RecentContact> = emptyList(),
)

class MvolaViewModel(repository: TransactionRepository) : ViewModel() {

    val uiState: StateFlow<MvolaUiState> = repository.observeTransactions().map { all ->
        MvolaUiState(
            people = RecentContacts.from(all, ContactKind.PEOPLE),
            // les numeros deja recharges d'abord, completes par les personnes connues
            creditTargets = (RecentContacts.from(all, ContactKind.CREDIT) + RecentContacts.from(all, ContactKind.PEOPLE))
                .distinctBy { it.phone }
                .take(6),
            agents = RecentContacts.from(all, ContactKind.AGENT),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MvolaUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                MvolaViewModel((this[APPLICATION_KEY] as VolakoApplication).container.transactionRepository)
            }
        }
    }
}
