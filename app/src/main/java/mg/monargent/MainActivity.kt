package mg.monargent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import mg.monargent.core.designsystem.theme.MonArgentTheme
import mg.monargent.ui.SetupScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // doit etre appele avant super.onCreate (doc officielle SplashScreen)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // l'app dessine sous la barre d'etat et la barre de navigation
        enableEdgeToEdge()
        setContent {
            MonArgentTheme {
                SetupScreen()
            }
        }
    }
}
