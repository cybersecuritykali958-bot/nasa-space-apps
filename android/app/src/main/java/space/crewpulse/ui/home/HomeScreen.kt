package space.crewpulse.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.crewpulse.domain.BodySystems
import space.crewpulse.ui.components.BodyFigurePlaceholder
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.components.MissionAlertBanner
import space.crewpulse.ui.components.MissionBanner
import space.crewpulse.ui.components.QuickAction
import space.crewpulse.ui.components.QuickActions
import space.crewpulse.ui.components.StatusDot
import space.crewpulse.ui.components.StatusLabel
import space.crewpulse.ui.navigation.Routes
import space.crewpulse.ui.trends.hasTrendChart
import space.crewpulse.ui.theme.StatusColors

@Composable
fun HomeScreen(
    state: HomeUiState,
    onIndicator: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onTrend: (String) -> Unit,
) {
    CrewScaffold(title = "CrewPulse", subtitle = "Astronaut health monitoring") { paddingModifier ->
        when {
            state.loading -> {
                Column(
                    Modifier.fillMaxSize().then(paddingModifier),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Loading mission data…", Modifier.padding(top = 16.dp))
                }
            }
            state.error != null -> {
                Text(
                    "Data error: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = paddingModifier.padding(16.dp),
                )
            }
            else -> LazyColumn(
                Modifier
                    .fillMaxSize()
                    .then(paddingModifier)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    state.alerts.forEach { msg ->
                        MissionAlertBanner(
                            title = "Mission alert",
                            body = msg,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                item {
                    MissionBanner(
                        astronaut = state.astronaut,
                        missionDay = state.missionDay,
                        environmentName = state.environmentName,
                        commDelayMin = state.commDelayMin?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) },
                    )
                }
                if (state.includesManualCheckIn) {
                    item {
                        CrewCard(accent = MaterialTheme.colorScheme.primary) {
                            Text(
                                "Dashboard includes your latest check-in.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                item {
                    QuickActions(
                        actions = listOf(
                            QuickAction(Routes.CHECKIN, "Check-in", "Daily vitals"),
                            QuickAction(Routes.PVT, "PVT", "Reaction test"),
                            QuickAction(Routes.TWIN, "Twin", "Mission twin"),
                            QuickAction(Routes.TRENDS, "Trends", "History"),
                        ),
                        onNavigate = onNavigate,
                    )
                }
                item {
                    BodyFigurePlaceholder(
                        systems = state.systems,
                        onSystemClick = { systemId ->
                            state.systems
                                .find { it.systemId == systemId }
                                ?.evaluations
                                ?.firstOrNull()
                                ?.let { onIndicator(it.indicator.id) }
                        },
                    )
                }
                item {
                    Text("Indicators by system", style = MaterialTheme.typography.titleSmall)
                }
                items(state.systems, key = { it.systemId }) { system ->
                    CrewCard(accent = StatusColors.of(system.status)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(BodySystems.label(system.systemId), style = MaterialTheme.typography.titleMedium)
                                StatusLabel(system.status)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            system.evaluations.forEach { eval ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    StatusDot(eval.status)
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .clickable { onIndicator(eval.indicator.id) }
                                            .padding(start = 10.dp),
                                    ) {
                                        Text(eval.indicator.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            "${formatReading(eval.indicator.id, eval.value, eval.indicator.unit)} · Why?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    if (hasTrendChart(eval.indicator.id)) {
                                        TextButton(onClick = { onTrend(eval.indicator.id) }) {
                                            Text("Trend")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatReading(id: String, value: Double, unit: String): String = when (id) {
    "mood" -> "${value.toInt()} / 5"
    else -> "%.1f $unit".format(value)
}