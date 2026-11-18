package mg.monargent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mg.monargent.core.designsystem.theme.MonArgentTheme
import mg.monargent.ui.home.HomeScreen
import mg.monargent.ui.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // doit etre appele avant super.onCreate (doc officielle SplashScreen)
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // on garde le splash tant qu'on ne sait pas quel ecran afficher (evite un flash)
        splash.setKeepOnScreenCondition { mainViewModel.onboardingDone.value == null }
        enableEdgeToEdge()

        setContent {
            MonArgentTheme {
                val onboardingDone by mainViewModel.onboardingDone.collectAsStateWithLifecycle()
                when (onboardingDone) {
                    null -> Unit
                    false -> OnboardingScreen()
                    true -> HomeScreen()
                }
            }
        }
    }
}
