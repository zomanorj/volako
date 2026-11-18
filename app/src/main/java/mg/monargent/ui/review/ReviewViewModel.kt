package mg.monargent.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mg.monargent.MonArgentApplication
import mg.monargent.core.data.TransactionRepository
import mg.monargent.core.data.UserPreferences
import mg.monargent.core.domain.Ledger
import mg.monargent.core.model.BalanceGap
import mg.monargent.core.model.PendingSms
import mg.monargent.core.model.TransactionType

data class ReviewUiState(
    val pending: List<PendingSms> = emptyList(),
    val gaps: List<BalanceGap> = emptyList(),
)

class ReviewViewModel(
    private val repository: TransactionRepository,
    private val preferences: UserPreferences,
) : ViewModel() {

    val uiState: StateFlow<ReviewUiState> = combine(
        repository.observePendingSms(),
        repository.observeTransactions(),
        preferences.acknowledgedGaps,
    ) { pending, transactions, acknowledged ->
        ReviewUiState(
            pending = pending,
            // les plus recents d'abord
            gaps = Ledger.balanceGaps(transactions).filter { it.key !in acknowledged }.reversed(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun ignore(sms: PendingSms) {
        viewModelScope.launch { repository.dismissPendingSms(sms.id) }
    }

    fun classify(sms: PendingSms, type: TransactionType, amount: Long, label: String) {
        viewModelScope.launch { repository.classifyPendingSms(sms.id, type, amount, label) }
    }

    fun acknowledge(gap: BalanceGap) {
        viewModelScope.launch { preferences.acknowledgeGap(gap.key) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as MonArgentApplication).container
                ReviewViewModel(c.transactionRepository, c.preferences)
            }
        }
    }
}
