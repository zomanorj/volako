package mg.volako

import android.app.Application
import mg.volako.notification.OperationNotifier

class VolakoApplication : Application() {
    // un seul conteneur pour toute l'app
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        OperationNotifier.createChannel(this)
    }
}
