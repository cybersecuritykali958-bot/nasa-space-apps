package space.crewpulse.ui.trends

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import space.crewpulse.AppContainer
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrendsScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: TrendsViewModel = viewModel(factory = TrendsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    CrewScaffold(title = "Trends", subtitle = "180-day demo mission", onBack = onBack) { padding ->
        when {
            state.error != null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = padding.padding(16.dp),
            )
            state.loading && state.indicator == null -> CircularProgressIndicator(Modifier.padding(24.dp))
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .then(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.picker.forEach { ind ->
                        FilterChip(
                            selected = state.selectedId == ind.id,
                            onClick = { vm.select(ind.id) },
                            label = { Text(ind.name, maxLines = 1) },
                        )
                    }
                }
                val indicator = state.indicator
                if (indicator != null && state.points.isNotEmpty()) {
                    CrewCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(indicator.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Shaded bands = green / yellow / red ranges from science data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TrendChart(
                                indicator = indicator,
                                points = state.points,
                                events = state.events,
                                missionDays = state.missionDays,
                            )
                            Text(
                                "Vertical lines = mission events (sleep loss, SPE, fever, …)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (state.events.isNotEmpty()) {
                    CrewCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Events", style = MaterialTheme.typography.titleSmall)
                            state.events.forEach { ev ->
                                Text(
                                    "Day ${ev.day}: ${ev.note}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}