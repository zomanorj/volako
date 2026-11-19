package mg.volako

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mg.volako.notification.OperationNotifier
import mg.volako.ui.mvola.FavoriteShortcuts

class VolakoApplication : Application() {
    // un seul conteneur pour toute l'app
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        OperationNotifier.createChannel(this)
        // les favoris suivent tout seuls dans l'appui long sur l'icone
        appScope.launch {
            container.favoriteRepository.observeFavorites().distinctUntilChanged().collect { favorites ->
                // Android limite les mises a jour en arriere-plan : un echec n'est pas grave
                runCatching { FavoriteShortcuts.publish(this@VolakoApplication, favorites) }
            }
        }
    }

    // vit aussi longtemps que l'app (pour les petites taches de fond)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
