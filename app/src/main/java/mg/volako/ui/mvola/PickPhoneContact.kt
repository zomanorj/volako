package mg.volako.ui.mvola

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContract

// Ouvre le repertoire du telephone pour choisir UN numero (doc officielle "Common intents" :
// ACTION_PICK sur Phone.CONTENT_URI). Pas besoin de la permission READ_CONTACTS :
// Android nous donne acces seulement au contact choisi.
class PickPhoneContact : ActivityResultContract<Unit, Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

data class PickedContact(val name: String?, val number: String)

// lit le nom et le numero du contact choisi (null si le contact a disparu entre temps)
fun readPickedContact(context: Context, uri: Uri): PickedContact? {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
    )
    return context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
        if (!c.moveToFirst()) return@use null
        val number = c.getString(0) ?: return@use null
        PickedContact(name = c.getString(1), number = number)
    }
}
