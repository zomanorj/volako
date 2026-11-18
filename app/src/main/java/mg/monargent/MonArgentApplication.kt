package mg.monargent

import android.app.Application
import mg.monargent.notification.OperationNotifier

class MonArgentApplication : Application() {
    // un seul conteneur pour toute l'app
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        OperationNotifier.createChannel(this)
    }
}
