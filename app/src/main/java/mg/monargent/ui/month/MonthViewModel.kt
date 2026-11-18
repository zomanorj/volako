package mg.monargent.ui.month

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mg.monargent.MonArgentApplication
import mg.monargent.core.data.CsvFileExporter
import mg.monargent.core.data.TransactionRepository
import mg.monargent.core.domain.MonthReports
import mg.monargent.core.model.MonthReport
import java.time.YearMonth

data class MonthUiState(
    val report: MonthReport = MonthReports.build(emptyList(), YearMonth.now()),
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
)

class MonthViewModel(
    repository: TransactionRepository,
    private val exporter: CsvFileExporter,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<MonthUiState> = combine(repository.observeTransactions(), month) { transactions, m ->
        val months = MonthReports.availableMonths(transactions)
        MonthUiState(
            report = MonthReports.build(transactions, m),
            // on peut reculer tant qu'il y a des operations plus anciennes
            hasPrevious = months.any { it < m },
            hasNext = m < YearMonth.now(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthUiState())

    fun previous() {
        month.value = month.value.minusMonths(1)
    }

    fun next() {
        if (month.value < YearMonth.now()) month.value = month.value.plusMonths(1)
    }

    // onResult(nombre d'operations) ou null si erreur
    fun export(uri: Uri, onResult: (Int?) -> Unit) {
        viewModelScope.launch { onResult(runCatching { exporter.export(uri) }.getOrNull()) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as MonArgentApplication).container
                MonthViewModel(c.transactionRepository, c.csvExporter)
            }
        }
    }
}
