package mg.monargent.ui.lab

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
import mg.monargent.core.data.SmsRecordResult
import mg.monargent.core.data.TransactionRepository
import mg.monargent.core.domain.Ledger
import mg.monargent.core.model.Balances
import mg.monargent.core.model.MonthSummary
import mg.monargent.core.model.Transaction
import java.time.LocalDateTime
import java.time.YearMonth

data class LabUiState(
    val balances: Balances = Balances(0, 0),
    val month: MonthSummary = MonthSummary(YearMonth.now(), 0, 0, 0, 0),
    val transactions: List<Transaction> = emptyList(),
    val pendingCount: Int = 0,
    val gapCount: Int = 0,
)

// ViewModel du labo (temporaire, etape 2) : garde l'etat de l'ecran meme si on tourne le telephone
class LabViewModel(private val repository: TransactionRepository) : ViewModel() {

    val uiState: StateFlow<LabUiState> = combine(
        repository.observeTransactions(),
        repository.observeMonthSummary(YearMonth.now()),
        repository.observePendingSms(),
    ) { transactions, month, pending ->
        LabUiState(
            balances = Ledger.balances(transactions),
            month = month,
            transactions = transactions,
            pendingCount = pending.size,
            gapCount = Ledger.balanceGaps(transactions).size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LabUiState())

    // resultat du dernier "Enregistrer", affiche sous le bouton
    private val _lastResult = MutableStateFlow<SmsRecordResult?>(null)
    val lastResult: StateFlow<SmsRecordResult?> = _lastResult.asStateFlow()

    fun record(body: String) {
        viewModelScope.launch {
            _lastResult.value = repository.recordSms(body, LocalDateTime.now())
        }
    }

    fun clearResult() {
        _lastResult.value = null
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MonArgentApplication
                LabViewModel(app.container.transactionRepository)
            }
        }
    }
}
