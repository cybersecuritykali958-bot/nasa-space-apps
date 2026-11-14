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
import space.crewpulse.ui.home.HomeRoutes
import space.crewpulse.ui.home.HomeScreen
import space.crewpulse.ui.home.HomeViewModel
import space.crewpulse.ui.indicator.IndicatorScreen
import space.crewpulse.ui.mission.MissionScreen
import space.crewpulse.ui.pvt.PvtScreen
import space.crewpulse.ui.readiness.ReadinessScreen
import space.crewpulse.ui.trends.TrendsScreen
import space.crewpulse.ui.twin.TwinScreen

object Routes {
    const val HOME = "home"
    const val INDICATOR = "indicator/{id}"

    fun indicator(id: String) = "indicator/$id"
}

@Composable
fun AppNavHost(container: AppContainer, onLanguageChange: (String) -> Unit) {
    val nav = rememberNavController()
    val home: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by home.state.collectAsStateWithLifecycle()
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                state = state,
                currentLanguage = container.settings.language,
                onIndicator = { nav.navigate(Routes.indicator(it)) },
                onNavigate = { nav.navigate(it) },
                onProfile = home::setProfile,
                onEnvironment = home::setEnvironment,
                onLanguage = onLanguageChange,
            )
        }
        composable(Routes.INDICATOR) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            IndicatorScreen(state.evaluation(id), state.history[id].orEmpty(), state.report, state.risks, back)
        }
        composable(HomeRoutes.CHECKIN) { CheckInScreen(container, back) }
        composable(HomeRoutes.TRENDS) { TrendsScreen(state, back) }
        composable(HomeRoutes.PVT) { PvtScreen(onSave = home::savePvt, onBack = back) }
        composable(HomeRoutes.TWIN) {
            TwinScreen(project = { a, phases -> home.projectTwin(a, phases = phases) }, onBack = back)
        }
        composable(HomeRoutes.READINESS) { ReadinessScreen(state, home::refreshSpaceWeather, back) }
        composable(HomeRoutes.MISSION) { MissionScreen(state, home::sendReminderNow, back) }
    }
}
