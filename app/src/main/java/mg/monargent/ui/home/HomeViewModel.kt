package mg.monargent.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mg.monargent.MonArgentApplication
import mg.monargent.core.data.SmsImporter
import mg.monargent.core.data.TransactionRepository
import mg.monargent.core.data.UserPreferences
import mg.monargent.core.domain.Ledger
import mg.monargent.core.model.Balances
import mg.monargent.core.model.MonthSummary
import mg.monargent.core.model.Transaction
import java.time.YearMonth

data class HomeUiState(
    val balances: Balances = Balances(0, 0),
    val month: MonthSummary = MonthSummary(YearMonth.now(), 0, 0, 0, 0),
    val transactions: List<Transaction> = emptyList(),
    val pendingCount: Int = 0,
    val gapCount: Int = 0,
)

class HomeViewModel(
    repository: TransactionRepository,
    private val importer: SmsImporter,
    private val preferences: UserPreferences,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeTransactions(),
        repository.observePendingSms(),
    ) { transactions, pending ->
        HomeUiState(
            balances = Ledger.balances(transactions),
            month = Ledger.monthSummary(transactions, YearMonth.now()),
            transactions = transactions,
            pendingCount = pending.size,
            gapCount = Ledger.balanceGaps(transactions).size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // nombre d'operations ajoutees par la derniere synchro (pour un petit message)
    private val _newCount = MutableStateFlow(0)
    val newCount: StateFlow<Int> = _newCount.asStateFlow()

    // relit les nouveaux SMS MVola (appele a chaque fois que l'app revient au premier plan)
    fun sync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            runCatching { importer.importNew() }.onSuccess { _newCount.value = it.added }
            _isSyncing.value = false
        }
    }

    // vrai la 1re fois seulement : l'ecran demande alors les permissions "en direct"
    suspend fun shouldAskLivePermissions(): Boolean = !preferences.livePermissionsAsked()

    fun onLivePermissionsAsked() {
        viewModelScope.launch { preferences.setLivePermissionsAsked() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as MonArgentApplication).container
                HomeViewModel(c.transactionRepository, c.smsImporter, c.preferences)
            }
        }
    }
}
