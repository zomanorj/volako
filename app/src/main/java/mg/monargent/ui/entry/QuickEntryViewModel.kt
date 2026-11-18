package mg.monargent.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mg.monargent.MonArgentApplication
import mg.monargent.core.data.TransactionRepository
import mg.monargent.core.model.Account
import mg.monargent.core.model.Category
import mg.monargent.core.model.Transaction
import mg.monargent.core.model.TransactionSource
import mg.monargent.core.model.TransactionType
import java.time.LocalDateTime

// Ce que la feuille de saisie renvoie a l'accueil apres un enregistrement (pour "Annuler")
data class SavedEntry(val id: Long, val type: TransactionType, val amount: Long, val label: String)

class QuickEntryViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _type = MutableStateFlow(TransactionType.EXPENSE)
    val type: StateFlow<TransactionType> = _type.asStateFlow()

    // libelles deja utilises, les plus frequents d'abord
    @OptIn(ExperimentalCoroutinesApi::class)
    val frequentLabels: StateFlow<List<String>> = _type
        .flatMapLatest { repository.observeFrequentLabels(it, MAX_LABELS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setType(type: TransactionType) {
        _type.value = type
    }

    fun save(amount: Long, label: String, onSaved: (SavedEntry) -> Unit) {
        val type = _type.value
        viewModelScope.launch {
            val transaction = Transaction(
                type = type,
                amount = amount,
                account = Account.CASH,
                category = if (type == TransactionType.INCOME) Category.INCOME else Category.OTHER,
                label = label.trim(),
                occurredAt = LocalDateTime.now(),
                source = TransactionSource.MANUAL,
            )
            val id = repository.addManual(transaction)
            onSaved(SavedEntry(id, type, amount, label.trim()))
        }
    }

    companion object {
        const val MAX_LABELS = 6

        val Factory = viewModelFactory {
            initializer {
                QuickEntryViewModel((this[APPLICATION_KEY] as MonArgentApplication).container.transactionRepository)
            }
        }
    }
}
