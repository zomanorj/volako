package mg.monargent.core.data

// Ce qui s'est passe quand on a donne un SMS au depot
enum class SmsRecordResult {
    ADDED,     // nouvelle operation enregistree
    DUPLICATE, // deja enregistree (meme ref MVola)
    PENDING,   // format inconnu, mis dans "A verifier"
    IGNORED,   // pas une operation (pub, info)
}
