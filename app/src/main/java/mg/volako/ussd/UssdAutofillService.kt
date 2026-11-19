package mg.volako.ussd

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo

// Remplit la bulle Yas a la place de l'utilisateur (doc officielle "Create your own accessibility service").
// Regles de securite :
// - seulement l'appli Telephone (config : packageNames), seulement 90 s apres "Lancer" (UssdAutofill)
// - seulement 2 ecrans : "Description du Transfert" et "reprendre la session precedente ?"
// - ecran du code secret : on arrete tout, on ne tape rien
// - on ne lit jamais le contenu d'un champ de saisie
class UssdAutofillService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val job = UssdAutofill.current() ?: return
        if (event?.packageName != PHONE_PACKAGE) return
        val root = rootInActiveWindow ?: return
        if (root.packageName != PHONE_PACKAGE) return

        val screen = visibleText(root)
        when {
            screen.contains("code secret", ignoreCase = true) -> UssdAutofill.disarm()
            screen.contains("reprendre la session", ignoreCase = true) -> reply(root, "2")
            screen.contains("Description du Transfert", ignoreCase = true) &&
                job.description != null && !job.descriptionDone -> {
                if (reply(root, job.description)) UssdAutofill.markDescriptionDone()
            }
        }
    }

    override fun onInterrupt() = Unit

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
        private val SEND_LABELS = listOf("Envoyer", "Send")

        // l'utilisateur a-t-il active le service dans Reglages > Accessibilite ?
        fun isEnabled(context: Context): Boolean {
            val me = ComponentName(context, UssdAutofillService::class.java)
            val am = context.getSystemService(AccessibilityManager::class.java) ?: return false
            return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo.serviceInfo.let { s -> ComponentName(s.packageName, s.name) == me } }
        }
    }
}
