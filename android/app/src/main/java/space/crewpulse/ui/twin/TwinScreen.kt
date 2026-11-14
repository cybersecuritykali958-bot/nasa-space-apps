package space.crewpulse.ui.twin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import space.crewpulse.AppContainer
import space.crewpulse.domain.DigitalTwin
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TwinScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: TwinViewModel = viewModel(factory = TwinViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    CrewScaffold(title = "Digital Twin", subtitle = "What-if · training demo", onBack = onBack) { padding ->
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = padding.padding(16.dp))
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .then(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CrewCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Mission day: ${state.missionDay.toInt()}", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = state.missionDay,
                            onValueChange = vm::setDay,
                            valueRange = 1f..state.maxDay.toFloat(),
                        )
                    }
                }
                Text("Environment (what-if)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.environments.forEach { env ->
                        FilterChip(
                            selected = state.selectedEnvId == env.id,
                            onClick = { vm.setEnvironment(env.id) },
                            label = { Text(env.name, maxLines = 1) },
                        )
                    }
                }
                state.selectedEnv?.let { env ->
                    CrewCard {
                        Text(
                            "g = ${env.gravityG} · ~${env.radiationMsvPerDay} mSv/day · comm ${env.commDelayMinOneWay} min",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                Text("Skip exercise (weeks)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 2, 4).forEach { w ->
                        FilterChip(
                            selected = state.skipExerciseWeeks == w,
                            onClick = { vm.setSkipExerciseWeeks(w) },
                            label = { Text(if (w == 0) "Normal" else "$w wks") },
                        )
                    }
                }
                MetricCard("Bone mass (model)", "%.1f %% of pre-flight".format(state.bonePct))
                MetricCard("Muscle mass (model)", "%.1f %% of pre-flight".format(state.musclePct))
                MetricCard("Cumulative dose (demo data)", "%.1f mSv".format(state.cumulativeDoseMsv))
                MetricCard(
                    "Career dose headroom",
                    "%.0f mSv left (NASA limit ${DigitalTwin.CAREER_DOSE_LIMIT_MSV.toInt()} mSv)".format(state.doseHeadroom),
                )
                CrewCard {
                    Text(
                        "Science team refines formulas in docs/SCIENCE.md. Radiation line follows synthetic demo_mission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String) {
    CrewCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}