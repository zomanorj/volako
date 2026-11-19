package mg.volako.ussd

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

// Remplit la bulle Yas a la place de l'utilisateur (doc officielle "Create your own accessibility service").
// Regles de securite :
// - seulement l'appli Telephone (config : packageNames), seulement 90 s apres "Lancer" (UssdAutofill)
// - seulement 2 ecrans : "Description du Transfert" et "reprendre la session precedente ?"
// - ecran du code secret : on arrete tout, on ne tape rien
// - on ne lit jamais le contenu d'un champ de saisie
class UssdAutofillService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    // vu sur Samsung : la reponse Yas arrive sans evenement d'accessibilite.
    // Donc pendant les 90 s apres "Lancer", on regarde la bulle 2 fois par seconde.
    private val tick = object : Runnable {
        override fun run() {
            if (UssdAutofill.current() == null) return
            check()
            handler.postDelayed(this, POLL_MS)
        }
    }

    // le dernier ecran auquel on a repondu (pour ne jamais repondre deux fois au meme)
    private var answeredScreen: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = true
        instance = this
    }

    override fun onDestroy() {
        connected = false
        instance = null
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName == PHONE_PACKAGE) check()
    }

    // appele quand l'utilisateur touche "Lancer"
    private fun startWatching() {
        answeredScreen = null
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun check() {
        val job = UssdAutofill.current() ?: return
        // la bulle Yas n'est pas toujours la fenetre "active" (Volako reste dessous) :
        // on cherche la fenetre de l'appli Telephone parmi toutes les fenetres
        val root = phoneRoot() ?: return
        val screen = visibleText(root)
        if (screen == answeredScreen) return
        when {
            screen.contains("code secret", ignoreCase = true) -> {
                Log.d(TAG, "ecran du code secret : on s'arrete")
                UssdAutofill.disarm()
            }
            screen.contains("reprendre la session", ignoreCase = true) -> {
                if (reply(root, "2")) answeredScreen = screen
                Log.d(TAG, "reprendre la session : reponse 2")
            }
            screen.contains("Description du Transfert", ignoreCase = true) &&
                job.description != null && !job.descriptionDone -> {
                val done = reply(root, job.description)
                Log.d(TAG, "description tapee : $done")
                if (done) {
                    answeredScreen = screen
                    UssdAutofill.markDescriptionDone()
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    private fun phoneRoot(): AccessibilityNodeInfo? =
        rootInActiveWindow?.takeIf { it.packageName == PHONE_PACKAGE }
            ?: windows.firstNotNullOfOrNull { w -> w.root?.takeIf { it.packageName == PHONE_PACKAGE } }

    // le texte affiche par la bulle (jamais ce qui est tape dans un champ)
    private fun visibleText(node: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()
        fun walk(n: AccessibilityNodeInfo) {
            if (!n.isEditable) n.text?.let { parts += it.toString() }
            for (i in 0 until n.childCount) n.getChild(i)?.let(::walk)
        }
        walk(node)
        return parts.joinToString(" ")
    }

    // tape le texte dans le champ de la bulle puis touche "Envoyer"
    private fun reply(root: AccessibilityNodeInfo, text: String): Boolean {
        val field = findEditable(root) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        if (!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return false
        // bouton positif de la boite de dialogue Android, sinon par son texte
        val send = root.findAccessibilityNodeInfosByViewId("android:id/button1").firstOrNull()
            ?: SEND_LABELS.firstNotNullOfOrNull { label ->
                root.findAccessibilityNodeInfosByText(label).firstOrNull { it.isClickable }
            }
            ?: return false
        return send.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findEditable(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (n.isEditable) return n
        for (i in 0 until n.childCount) n.getChild(i)?.let(::findEditable)?.let { return it }
        return null
    }

    companion object {
        // l'appli qui affiche les bulles USSD (verifie sur le S26+)
        const val PHONE_PACKAGE = "com.android.phone"
        private const val TAG = "VolakoAutofill"
        private const val POLL_MS = 500L

        @Volatile
        private var instance: UssdAutofillService? = null

        // reveille le service juste apres "Lancer" (il tourne dans le meme processus que l'app)
        fun wake() {
            instance?.let { s -> s.handler.post { s.startWatching() } }
        }
        private val SEND_LABELS = listOf("Envoyer", "Send")

        // vrai quand Android a vraiment branche le service (pas seulement coche dans les Reglages)
        @Volatile
        var connected: Boolean = false
            private set

        // le service peut vraiment remplir la bulle maintenant
        fun isEnabled(context: Context): Boolean = state(context) == AutofillState.ON

        fun state(context: Context): AutofillState {
            // seul signal fiable : le service lui-meme dit qu'Android l'a branche
            // (la liste d'Android compte aussi les services coches mais pas lances)
            if (connected) return AutofillState.ON
            val me = ComponentName(context, UssdAutofillService::class.java)
            // coche par l'utilisateur mais pas branche : vu sur Samsung apres une mise a jour de l'app
            val checked = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?.split(':')?.any { ComponentName.unflattenFromString(it) == me } == true
            return if (checked) AutofillState.PAUSED else AutofillState.OFF
        }
    }
}

// OFF : pas active ; ON : pret ; PAUSED : active dans les Reglages mais Android ne l'a pas relance
enum class AutofillState { OFF, ON, PAUSED }
