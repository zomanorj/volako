package mg.volako.ui.home

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
import mg.volako.VolakoApplication
import mg.volako.core.data.SmsImporter
import mg.volako.core.data.TransactionRepository
import mg.volako.core.data.UserPreferences
import mg.volako.core.domain.CashCount
import mg.volako.core.domain.FormatWatch
import mg.volako.core.domain.Ledger
import mg.volako.core.model.Balances
import mg.volako.core.model.MonthSummary
import mg.volako.core.model.Transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

data class HomeUiState(
    val balances: Balances = Balances(0, 0),
    val month: MonthSummary = MonthSummary(YearMonth.now(), 0, 0, 0, 0),
    // operations groupees par jour, du plus recent au plus ancien
    val days: List<Pair<LocalDate, List<Transaction>>> = emptyList(),
    val pendingCount: Int = 0,
    val gapCount: Int = 0,
    // date du dernier SMS "nouveau format" pas encore vu par l'utilisateur (null = pas d'alerte)
    val formatChangeAt: LocalDateTime? = null,
)

class HomeViewModel(
    private val repository: TransactionRepository,
    private val importer: SmsImporter,
    private val preferences: UserPreferences,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeTransactions(),
        repository.observePendingSms(),
        preferences.acknowledgedGaps,
        preferences.formatAlertSeenAt,
    ) { transactions, pending, acknowledged, alertSeenAt ->
        val change = FormatWatch.latestChange(transactions, pending, LocalDateTime.now())
        HomeUiState(
            formatChangeAt = change?.takeIf { it.toEpochMillis() > alertSeenAt },
            balances = Ledger.balances(transactions),
            month = Ledger.monthSummary(transactions, YearMonth.now()),
            days = transactions.groupBy { it.occurredAt.toLocalDate() }.toList(),
            pendingCount = pending.size,
            gapCount = Ledger.balanceGaps(transactions).count { it.key !in acknowledged },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // relit les nouveaux SMS MVola (appele a chaque fois que l'app revient au premier plan)
    fun sync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            runCatching { importer.importNew() }
            _isSyncing.value = false
        }
    }

    fun dismissFormatAlert() {
        val at = uiState.value.formatChangeAt ?: return
        viewModelScope.launch { preferences.setFormatAlertSeenAt(at.toEpochMillis()) }
    }

    private fun LocalDateTime.toEpochMillis() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // "Compter mon portefeuille" : on enregistre l'ecart entre l'app et la realite
    fun countCash(counted: Long) {
        val adjustment = CashCount.adjustment(uiState.value.balances.cash, counted, LocalDateTime.now()) ?: return
        viewModelScope.launch { repository.addManual(adjustment) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun restore(transaction: Transaction) {
        viewModelScope.launch { repository.restore(transaction) }
    }

    // rememberForPhone : appliquer ce libelle a toutes les operations de ce numero
    fun update(transaction: Transaction, rememberForPhone: Boolean) {
        viewModelScope.launch {
            val phone = transaction.counterpartyPhone
            val label = transaction.label
            repository.update(transaction)
            if (rememberForPhone && phone != null && !label.isNullOrBlank()) {
                repository.rememberCounterpartyLabel(phone, label)
            }
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
                val c = (this[APPLICATION_KEY] as VolakoApplication).container
                HomeViewModel(c.transactionRepository, c.smsImporter, c.preferences)
            }
        }
    }
}
