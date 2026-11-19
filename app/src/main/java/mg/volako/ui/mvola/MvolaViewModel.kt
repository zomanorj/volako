package mg.volako.ui.mvola

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import mg.volako.VolakoApplication
import mg.volako.core.data.FavoriteRepository
import mg.volako.core.data.PhoneContactsSource
import mg.volako.core.domain.ContactSearch
import mg.volako.core.data.TransactionRepository
import mg.volako.core.domain.ContactKind
import mg.volako.core.domain.RecentContact
import mg.volako.core.domain.RecentContacts
import mg.volako.core.model.Favorite
import mg.volako.core.model.PhoneContact

// Numeros proposes dans l'onglet MVola, retrouves dans l'historique des SMS
data class MvolaUiState(
    val people: List<RecentContact> = emptyList(),
    val creditTargets: List<RecentContact> = emptyList(),
    val agents: List<RecentContact> = emptyList(),
    val favorites: List<Favorite> = emptyList(),
    // repertoire du telephone (vide sans la permission)
    val contacts: List<PhoneContact> = emptyList(),
    val contactsAllowed: Boolean = false,
)

class MvolaViewModel(
    repository: TransactionRepository,
    private val favorites: FavoriteRepository,
    private val phoneContacts: PhoneContactsSource,
) : ViewModel() {

    val uiState: StateFlow<MvolaUiState> = combine(
        repository.observeTransactions(),
        favorites.observeFavorites(),
        phoneContacts.observeContacts(),
    ) { all, favs, contacts ->
        // le nom du repertoire remplace celui des SMS (c'est le nom que l'utilisateur connait)
        val names = ContactSearch.namesByPhone(contacts)
        fun List<RecentContact>.named() = map { r -> names[r.phone]?.let { r.copy(name = it) } ?: r }
        MvolaUiState(
            people = RecentContacts.from(all, ContactKind.PEOPLE).named(),
            // les numeros deja recharges d'abord, completes par les personnes connues
            creditTargets = (RecentContacts.from(all, ContactKind.CREDIT) + RecentContacts.from(all, ContactKind.PEOPLE))
                .distinctBy { it.phone }
                .take(6)
                .named(),
            agents = RecentContacts.from(all, ContactKind.AGENT).named(),
            favorites = favs.map { f -> f.phone?.let(RecentContacts::normalize)?.let(names::get)?.let { f.copy(name = it) } ?: f },
            contacts = contacts,
            contactsAllowed = phoneContacts.hasPermission(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MvolaUiState())

    // pas de doublon : si le meme favori existe deja, on ne fait rien
    fun addFavorite(favorite: Favorite) {
        if (uiState.value.favorites.any { it.sameAs(favorite) }) return
        viewModelScope.launch { favorites.add(favorite) }
    }

    // a appeler juste apres que l'utilisateur a donne la permission des contacts
    fun onContactsPermissionChanged() = phoneContacts.refresh()

    fun deleteFavorite(id: Long) {
        viewModelScope.launch { favorites.delete(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as VolakoApplication).container
                MvolaViewModel(container.transactionRepository, container.favoriteRepository, container.phoneContacts)
            }
        }
    }
}
