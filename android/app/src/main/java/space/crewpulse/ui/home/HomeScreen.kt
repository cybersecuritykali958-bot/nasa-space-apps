package space.crewpulse.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.data.Profile
import space.crewpulse.data.model.Environment
import space.crewpulse.data.model.Status
import space.crewpulse.domain.SpaceWeatherLevel
import space.crewpulse.ui.components.BodyFigure
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.StatusDot
import space.crewpulse.ui.components.StatusPill
import space.crewpulse.ui.i18n.LocaleHelper
import space.crewpulse.ui.i18n.alertText
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.i18n.responderLabel
import space.crewpulse.ui.i18n.systemLabel
import space.crewpulse.ui.theme.StatusColors

/** Navigation targets used by the Home screen. */
object HomeRoutes {
    const val CHECKIN = "checkin"
    const val PVT = "pvt"
    const val TWIN = "twin"
    const val TRENDS = "trends"
    const val READINESS = "readiness"
    const val MISSION = "mission"
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    currentLanguage: String,
    onIndicator: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onProfile: (Profile) -> Unit,
    onEnvironment: (String) -> Unit,
    onLanguage: (String) -> Unit,
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(currentLanguage, onLanguage) }
        when {
            state.loading -> item { Text(stringResource(R.string.loading)) }
            state.error != null -> item {
                Text(stringResource(R.string.data_error, state.error), color = MaterialTheme.colorScheme.error)
            }
            else -> {
                item { MissionBanner(state, onEnvironment) }
                item { ProfileSwitch(state.profile, onProfile) }
                item {
                    BodyFigure(
                        statuses = state.systems.associate { it.systemId to it.status },
                        modifier = Modifier.padding(horizontal = 48.dp),
                    )
                }
                item { WhatToDoCard(state, onNavigate) }
                state.report.alerts.firstOrNull()?.let { alert ->
                    item {
                        val ind = state.evaluation(alert.indicatorId)?.indicator
                        WarningBanner(
                            alertText(alert, ind?.let { indicatorName(it) } ?: alert.indicatorId),
                        ) { onIndicator(alert.indicatorId) }
                    }
                }
                state.report.spaceWeather?.takeIf { it.level != SpaceWeatherLevel.QUIET }?.let { sw ->
                    item {
                        WarningBanner(
                            if (sw.level == SpaceWeatherLevel.WARNING) stringResource(R.string.sw_warning) else stringResource(R.string.sw_watch),
                            red = sw.level == SpaceWeatherLevel.WARNING,
                        ) { onNavigate(HomeRoutes.READINESS) }
                    }
                }
                item { QuickActionsRow(onNavigate) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NavCard(stringResource(R.string.readiness_title), Icons.Filled.CheckCircle, Modifier.weight(1f)) {
                            onNavigate(HomeRoutes.READINESS)
                        }
                        NavCard(stringResource(R.string.mission_title), Icons.Filled.Star, Modifier.weight(1f)) {
                            onNavigate(HomeRoutes.MISSION)
                        }
                    }
                }
                item { Text(stringResource(R.string.body_systems), style = MaterialTheme.typography.titleMedium) }
                items(state.systems, key = { it.systemId }) { system ->
                    CrewCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(system.status, 16.dp)
                            Text(
                                systemLabel(system.systemId),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 10.dp).weight(1f),
                            )
                            StatusPill(system.status)
                        }
                        system.evaluations.forEach { eval ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onIndicator(eval.indicator.id) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatusDot(eval.status, 10.dp)
                                Text(
                                    indicatorName(eval.indicator),
                                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    "${"%.1f".format(eval.value)} ${eval.indicator.unit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.demo_data_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(currentLanguage: String, onLanguage: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { open = true }) {
                Text(currentLanguage.uppercase(), style = MaterialTheme.typography.labelLarge)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                LocaleHelper.LANGUAGES.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.nativeName) },
                        onClick = {
                            open = false
                            if (lang.code != currentLanguage) onLanguage(lang.code)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionBanner(state: HomeUiState, onEnvironment: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.clickable { open = true },
        ) {
            Text(
                stringResource(
                    R.string.home_banner,
                    state.environmentName,
                    state.missionDay,
                    "%.0f".format(state.commDelayMin ?: 0.0),
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            state.environments.forEach { env: Environment ->
                DropdownMenuItem(text = { Text(env.name) }, onClick = { open = false; onEnvironment(env.id) })
            }
        }
    }
}

@Composable
private fun ProfileSwitch(profile: Profile, onProfile: (Profile) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = profile == Profile.DEMO,
            onClick = { onProfile(Profile.DEMO) },
            label = { Text(stringResource(R.string.profile_demo)) },
        )
        FilterChip(
            selected = profile == Profile.ME,
            onClick = { onProfile(Profile.ME) },
            label = { Text(stringResource(R.string.profile_me)) },
        )
    }
}

@Composable
private fun WhatToDoCard(state: HomeUiState, onNavigate: (String) -> Unit) {
    val t = state.report.triage
    CrewCard(
        accent = StatusColors.of(t.status).takeIf { t.status != Status.GREEN },
        modifier = Modifier.clickable { onNavigate(HomeRoutes.READINESS) },
    ) {
        Text(stringResource(R.string.what_to_do), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(responderLabel(t.responder), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
        Text(t.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.report.patterns.firstOrNull()?.let { p ->
            Text(p.advice, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun WarningBanner(text: String, red: Boolean = false, onClick: () -> Unit) {
    val color = if (red) StatusColors.red else StatusColors.yellow
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = color)
            Text(text, modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

@Composable
private fun QuickActionsRow(onNavigate: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        RoundAction(stringResource(R.string.qa_checkin), Icons.Filled.CheckCircle) { onNavigate(HomeRoutes.CHECKIN) }
        RoundAction(stringResource(R.string.qa_pvt), Icons.Filled.PlayArrow) { onNavigate(HomeRoutes.PVT) }
        RoundAction(stringResource(R.string.qa_twin), Icons.Filled.Person) { onNavigate(HomeRoutes.TWIN) }
        RoundAction(stringResource(R.string.qa_trends), Icons.Filled.DateRange) { onNavigate(HomeRoutes.TRENDS) }
    }
}

@Composable
private fun RoundAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable(onClick = onClick).padding(vertical = 4.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

@Composable
private fun NavCard(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    CrewCard(modifier = modifier.clickable(onClick = onClick)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    }
}
