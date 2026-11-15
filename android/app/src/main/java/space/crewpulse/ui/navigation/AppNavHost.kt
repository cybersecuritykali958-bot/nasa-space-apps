package space.crewpulse.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import space.crewpulse.ui.components.SpaceBackground
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import space.crewpulse.ui.components.LanguageControl
import space.crewpulse.ui.components.LocalLanguageControl
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import space.crewpulse.ui.pvt.PvtScreen
import space.crewpulse.ui.readiness.ReadinessScreen
import space.crewpulse.ui.settings.SettingsScreen
import space.crewpulse.ui.trends.TrendsScreen
import space.crewpulse.ui.watch.WatchConnectScreen
import space.crewpulse.ui.login.BadgeLoginScreen

/** All destinations. Tabs are shown in the bottom bar; the rest open on top of a tab. */
object Routes {
    const val HOME = "home"
    const val HEALTH = "health"
    const val READINESS = "readiness"
    const val CHECKIN = "checkin"
    const val PVT = "pvt"
    const val TRENDS = "trends"
    const val SETTINGS = "settings"
    const val WATCH = "watch"
    const val INDICATOR = "indicator/{id}"

    fun indicator(id: String) = "indicator/$id"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, R.string.tab_operations, Icons.Outlined.RocketLaunch, Icons.Filled.RocketLaunch),
    Tab(Routes.HEALTH, R.string.tab_physiology, Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    Tab(Routes.READINESS, R.string.tab_readiness, Icons.Outlined.MonitorHeart, Icons.Filled.MonitorHeart),
    Tab(Routes.SETTINGS, R.string.tab_profile, Icons.Outlined.Person, Icons.Filled.Person),
)

private enum class LaunchStage { LOADING, LOGIN, WATCH, MAIN }

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
    // Watch connection is offered on every launch (demo flow); saveable so rotation does not repeat it.
    var loginDone by rememberSaveable { mutableStateOf(false) }
    var watchDone by rememberSaveable { mutableStateOf(false) }
    val stage = when {
        splash || state.loading -> LaunchStage.LOADING
        !loginDone -> LaunchStage.LOGIN
        !watchDone -> LaunchStage.WATCH
        else -> LaunchStage.MAIN
    }
    Crossfade(targetState = stage, animationSpec = tween(500), label = "launch") { s ->
        when (s) {
            LaunchStage.LOADING -> OrbitLoader(stringResource(R.string.loading_sync))
            LaunchStage.LOGIN -> BadgeLoginScreen(container.badges, onSignedIn = { loginDone = true })
            LaunchStage.WATCH -> WatchConnectScreen(container.watch, onDone = { watchDone = true })
            LaunchStage.MAIN -> MainScaffold(container, nav, home, state, onLanguageChange) {
                container.badges.signOut()
                loginDone = false
                watchDone = false
            }
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
    onSignOut: () -> Unit,
) {
    val back: () -> Unit = { nav.popBackStack() }
    val go: (String) -> Unit = { nav.navigate(it) }
    val overall = space.crewpulse.domain.StatusEngine.worst(state.systems.map { it.status })
    val language = LanguageControl(container.settings.language, onLanguageChange)
    CompositionLocalProvider(LocalLanguageControl provides language) {
    Box(Modifier.fillMaxSize()) {
    SpaceBackground(overall)
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = { BottomBar(nav) },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 12 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(250)) { it / 12 } },
        ) {
            // Tabs
            composable(Routes.HOME) {
                val user by container.badges.current.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    userName = user?.name?.substringBefore(' '),
                    onIndicator = { go(Routes.indicator(it)) },
                    onNavigate = { route -> if (TABS.any { it.route == route }) nav.switchTab(route) else go(route) },
                    onReminder = home::sendReminderNow,
                )
            }
            composable(Routes.HEALTH) {
                HealthScreen(
                    state,
                    onIndicator = { go(Routes.indicator(it)) },
                    onNavigate = go,
                    projectTwin = { a, phases -> home.projectTwin(a, phases = phases) },
                )
            }
            composable(Routes.READINESS) { ReadinessScreen(state, home::refreshSpaceWeather, onBack = null) }
            composable(Routes.SETTINGS) {
                val watch by container.watch.snapshot.collectAsStateWithLifecycle()
                val user by container.badges.current.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    member = user,
                    onProfile = home::setProfile,
                    onEnvironment = home::setEnvironment,
                    watchConnected = watch != null,
                    onWatchConnect = { go(Routes.WATCH) },
                    onWatchDisconnect = container.watch::disconnect,
                    onSignOut = {
                        nav.switchTab(Routes.HOME)
                        onSignOut()
                    },
                )
            }

            // Detail screens
            composable(Routes.INDICATOR) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                IndicatorScreen(state.evaluation(id), state.history[id].orEmpty(), state.report, state.risks, back)
            }
            composable(Routes.CHECKIN) { CheckInScreen(container, back) }
            composable(Routes.PVT) { PvtScreen(onSave = home::savePvt, onBack = back) }
            composable(Routes.TRENDS) { TrendsScreen(state, onShowDemo = { home.setProfile(space.crewpulse.data.Profile.DEMO) }, onBack = back) }
            composable(Routes.WATCH) {
                WatchConnectScreen(container.watch, onDone = back, drawBackground = false)
            }
        }
    }
    }
    }
}

/** Floating capsule navigation; the highlight slides to the selected tab. */
@Composable
private fun BottomBar(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val selected = TABS.indexOfFirst { it.route == current }
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(64.dp)) {
                val itemWidth = maxWidth / TABS.size
                val x by animateDpAsState(itemWidth * selected.coerceAtLeast(0), tween(300), label = "tab")
                if (selected >= 0) {
                    Box(
                        Modifier
                            .offset(x = x)
                            .width(itemWidth)
                            .fillMaxHeight()
                            .padding(6.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(22.dp)),
                    )
                }
                Row(Modifier.fillMaxSize()) {
                    TABS.forEachIndexed { i, tab ->
                        val on = i == selected
                        val tint by animateColorAsState(
                            if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            tween(300), label = "tint",
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { nav.switchTab(tab.route) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(if (on) tab.selectedIcon else tab.icon, contentDescription = null, tint = tint)
                            Text(
                                stringResource(tab.label),
                                style = MaterialTheme.typography.labelSmall,
                                color = tint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Standard bottom-tab navigation: one copy of each tab, state kept when switching. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
