package space.crewpulse.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import space.crewpulse.AppContainer
import space.crewpulse.ui.components.OnboardingDialog
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
    const val TRENDS_INDICATOR = "trends/{indicatorId}"

    fun trendsFor(indicatorId: String) = "trends/$indicatorId"
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
            var showOnboarding by remember { mutableStateOf(!container.onboarding.hasSeenOnboarding()) }
            if (showOnboarding) {
                OnboardingDialog {
                    container.onboarding.markOnboardingSeen()
                    showOnboarding = false
                }
            }
            HomeScreen(
                state = homeState,
                isRefreshing = homeState.refreshing,
                onRefresh = home::refresh,
                onDemoDay = home::setDemoDay,
                onIndicator = { nav.navigate(Routes.indicator(it)) },
                onNavigate = { nav.navigate(it) },
                onTrend = { nav.navigate(Routes.trendsFor(it)) },
            )
        }
        composable(Routes.INDICATOR) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            IndicatorScreen(
                evaluation = homeState.evaluation(id),
                risks = homeState.risks,
                onBack = back,
                onViewTrend = { nav.navigate(Routes.trendsFor(it)) },
            )
        }
        composable(Routes.CHECKIN) { CheckInScreen(container, back) }
        composable(Routes.TRENDS) { TrendsScreen(container, back, initialIndicatorId = null) }
        composable(
            route = Routes.TRENDS_INDICATOR,
            arguments = listOf(navArgument("indicatorId") { type = NavType.StringType }),
        ) { entry ->
            TrendsScreen(container, back, entry.arguments?.getString("indicatorId"))
        }
        composable(Routes.PVT) { PvtScreen(container, back) }
        composable(Routes.TWIN) { TwinScreen(container, back) }
    }
}