package mg.volako.core.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mg.volako.core.domain.RecentContacts
import mg.volako.core.model.PhoneContact

// Lit le repertoire du telephone (permission READ_CONTACTS). Rien n'est copie dans la base :
// on relit le repertoire a chaque changement, donc les noms sont toujours les memes que sur le telephone.
class PhoneContactsSource(private val context: Context) {

    // relance la lecture (ex : juste apres que l'utilisateur a donne la permission)
    private val refresh = MutableStateFlow(0)

    fun refresh() {
        refresh.value++
    }

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeContacts(): Flow<List<PhoneContact>> = refresh.flatMapLatest { changes() }
        .map { if (hasPermission()) read() else emptyList() }
        .flowOn(Dispatchers.IO)

    // un signal au debut, puis a chaque modification du repertoire
    private fun changes(): Flow<Unit> = callbackFlow {
        trySend(Unit)
        if (!hasPermission()) {
            awaitClose()
            return@callbackFlow
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.conflate()

    private fun read(): List<PhoneContact> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        val result = mutableListOf<PhoneContact>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0)?.trim().orEmpty()
                // numeros malgaches seulement, au format 0341234567
                val phone = RecentContacts.normalize(c.getString(1)) ?: continue
                if (name.isNotEmpty()) result += PhoneContact(name, phone)
            }
        }
        return result.distinctBy { it.phone to it.name }
    }
}
