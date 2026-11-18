package mg.monargent

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mg.monargent.notification.OperationNotifier

class MonArgentApplication : Application() {
    // un seul conteneur pour toute l'app
    lateinit var container: AppContainer
        private set

    // pour le travail qui doit survivre a un ecran (ex: SMS recu app fermee)
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        OperationNotifier.createChannel(this)
    }
}
