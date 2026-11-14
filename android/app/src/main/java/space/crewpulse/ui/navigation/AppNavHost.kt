package space.crewpulse.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import space.crewpulse.ui.components.OrbitLoader
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import space.crewpulse.AppContainer
import space.crewpulse.R
import space.crewpulse.ui.checkin.CheckInScreen
import space.crewpulse.ui.health.HealthScreen
import space.crewpulse.ui.home.HomeScreen
import space.crewpulse.ui.home.HomeViewModel
import space.crewpulse.ui.indicator.IndicatorScreen
import space.crewpulse.ui.mission.MissionScreen
import space.crewpulse.ui.pvt.PvtScreen
import space.crewpulse.ui.readiness.ReadinessScreen
import space.crewpulse.ui.settings.SettingsScreen
import space.crewpulse.ui.trends.TrendsScreen
import space.crewpulse.ui.twin.TwinScreen

/** All destinations. Tabs are shown in the bottom bar; the rest open on top of a tab. */
object Routes {
    const val HOME = "home"
    const val HEALTH = "health"
    const val READINESS = "readiness"
    const val TWIN = "twin"
    const val MISSION = "mission"
    const val CHECKIN = "checkin"
    const val PVT = "pvt"
    const val TRENDS = "trends"
    const val SETTINGS = "settings"
    const val INDICATOR = "indicator/{id}"

    fun indicator(id: String) = "indicator/$id"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, R.string.tab_home, Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.HEALTH, R.string.tab_health, Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    Tab(Routes.READINESS, R.string.tab_readiness, Icons.Outlined.MonitorHeart, Icons.Filled.MonitorHeart),
    Tab(Routes.TWIN, R.string.tab_twin, Icons.Outlined.Groups, Icons.Filled.Groups),
    Tab(Routes.MISSION, R.string.tab_mission, Icons.Outlined.RocketLaunch, Icons.Filled.RocketLaunch),
)

@Composable
fun AppNavHost(container: AppContainer, onLanguageChange: (String) -> Unit) {
    val nav = rememberNavController()
    val home: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by home.state.collectAsStateWithLifecycle()

    // Short loading screen with orbiting planets: at least 1.5 s on start, and while data loads.
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1_500)
        splash = false
    }
    Crossfade(targetState = splash || state.loading, animationSpec = tween(500), label = "splash") { loading ->
        if (loading) {
            OrbitLoader(stringResource(R.string.loading_sync))
        } else {
            MainScaffold(container, nav, home, state, onLanguageChange)
        }
    }
}

@Composable
private fun MainScaffold(
    container: AppContainer,
    nav: NavHostController,
    home: HomeViewModel,
    state: space.crewpulse.ui.home.HomeUiState,
    onLanguageChange: (String) -> Unit,
) {
    val back: () -> Unit = { nav.popBackStack() }
    val go: (String) -> Unit = { nav.navigate(it) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = { BottomBar(nav) },
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            // Tabs
            composable(Routes.HOME) {
                HomeScreen(
                    state = state,
                    onIndicator = { go(Routes.indicator(it)) },
                    onNavigate = { route -> if (TABS.any { it.route == route }) nav.switchTab(route) else go(route) },
                )
            }
            composable(Routes.HEALTH) {
                HealthScreen(state, onIndicator = { go(Routes.indicator(it)) }, onNavigate = go)
            }
            composable(Routes.READINESS) { ReadinessScreen(state, home::refreshSpaceWeather, onBack = null) }
            composable(Routes.TWIN) {
                TwinScreen(project = { a, phases -> home.projectTwin(a, phases = phases) }, onBack = null)
            }
            composable(Routes.MISSION) { MissionScreen(state, home::sendReminderNow, onBack = null) }

            // Detail screens
            composable(Routes.INDICATOR) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                IndicatorScreen(state.evaluation(id), state.history[id].orEmpty(), state.report, state.risks, back)
            }
            composable(Routes.CHECKIN) { CheckInScreen(container, back) }
            composable(Routes.PVT) { PvtScreen(onSave = home::savePvt, onBack = back) }
            composable(Routes.TRENDS) { TrendsScreen(state, onShowDemo = { home.setProfile(space.crewpulse.data.Profile.DEMO) }, onBack = back) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    state = state,
                    currentLanguage = container.settings.language,
                    onLanguage = onLanguageChange,
                    onProfile = home::setProfile,
                    onEnvironment = home::setEnvironment,
                    onBack = back,
                )
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, windowInsets = NavigationBarDefaultsInsets()) {
        TABS.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { nav.switchTab(tab.route) },
                icon = { Icon(if (current == tab.route) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun NavigationBarDefaultsInsets(): WindowInsets = androidx.compose.material3.NavigationBarDefaults.windowInsets

/** Standard bottom-tab navigation: one copy of each tab, state kept when switching. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
