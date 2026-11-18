package mg.monargent.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import mg.monargent.ui.home.HomeScreen
import mg.monargent.ui.review.ReviewScreen

// Les ecrans de l'app. Serializable : la pile survit si Android tue l'app en arriere-plan
@Serializable
data object HomeKey : NavKey

@Serializable
data object ReviewKey : NavKey

// Navigation 3 (doc officielle) : une simple pile d'ecrans
@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(HomeKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<HomeKey> { HomeScreen(onOpenReview = { backStack.add(ReviewKey) }) }
            entry<ReviewKey> { ReviewScreen(onBack = { backStack.removeLastOrNull() }) }
        },
    )
}
