package mg.volako.ui.operations

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
import mg.volako.VolakoApplication
import mg.volako.core.data.TransactionRepository
import mg.volako.core.domain.DayGroup
import mg.volako.core.domain.OperationFilter
import mg.volako.core.domain.OperationsQuery

data class OperationsUiState(
    val days: List<DayGroup> = emptyList(),
    val counts: Map<OperationFilter, Int> = emptyMap(),
    val isEmptyDatabase: Boolean = true,
)

// Garde le filtre et la recherche (ils restent si on change d'onglet et qu'on revient)
class OperationsViewModel(repository: TransactionRepository) : ViewModel() {

    private val _filter = MutableStateFlow(OperationFilter.ALL)
    val filter: StateFlow<OperationFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<OperationsUiState> = combine(repository.observeTransactions(), _filter, _query) { all, f, q ->
        OperationsUiState(
            days = OperationsQuery.groupByDay(OperationsQuery.apply(all, f, q)),
            counts = OperationsQuery.counts(all, q),
            isEmptyDatabase = all.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OperationsUiState())

    fun setFilter(filter: OperationFilter) {
        _filter.value = filter
    }

    fun setQuery(query: String) {
        _query.value = query.take(60)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                OperationsViewModel((this[APPLICATION_KEY] as VolakoApplication).container.transactionRepository)
            }
        }
    }
}
