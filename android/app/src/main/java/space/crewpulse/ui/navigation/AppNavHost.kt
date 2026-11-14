package space.crewpulse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import space.crewpulse.AppContainer
import space.crewpulse.ui.checkin.CheckInScreen
import space.crewpulse.ui.home.HomeScreen
import space.crewpulse.ui.home.HomeViewModel
import space.crewpulse.ui.indicator.IndicatorScreen
import space.crewpulse.ui.pvt.PvtScreen
import space.crewpulse.ui.trends.TrendsScreen
import space.crewpulse.ui.twin.TwinScreen

object Routes {
    const val HOME = "home"
    const val INDICATOR = "indicator/{id}"
    const val CHECKIN = "checkin"
    const val TRENDS = "trends"
    const val PVT = "pvt"
    const val TWIN = "twin"

    fun indicator(id: String) = "indicator/$id"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    val home: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val homeState by home.state.collectAsStateWithLifecycle()
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                state = homeState,
                onIndicator = { nav.navigate(Routes.indicator(it)) },
                onNavigate = { nav.navigate(it) },
            )
        }
        composable(Routes.INDICATOR) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            IndicatorScreen(homeState.evaluation(id), homeState.risks, back)
        }
        composable(Routes.CHECKIN) { CheckInScreen(container, back) }
        composable(Routes.TRENDS) { TrendsScreen(back) }
        composable(Routes.PVT) { PvtScreen(back) }
        composable(Routes.TWIN) { TwinScreen(back) }
    }
}