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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.crewpulse.domain.BodySystems
import space.crewpulse.ui.components.StatusDot

// TODO(@harvio1): replace the system list with the interactive body figure from Figma.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onIndicator: (String) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("CrewPulse") }) }) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                when {
                    state.loading -> Text("Loading…")
                    state.error != null -> Text("Data error: ${state.error}", color = MaterialTheme.colorScheme.error)
                    else -> Text(
                        "${state.astronaut} · mission day ${state.missionDay} (synthetic demo data)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("checkin" to "Check-in", "pvt" to "PVT", "twin" to "Twin", "trends" to "Trends")
                        .forEach { (route, label) ->
                            OutlinedButton(onClick = { onNavigate(route) }) { Text(label) }
                        }
                }
            }
            items(state.systems, key = { it.systemId }) { system ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(system.status, 18.dp)
                            Text(
                                BodySystems.label(system.systemId),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                        system.evaluations.forEach { eval ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onIndicator(eval.indicator.id) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatusDot(eval.status)
                                Text(
                                    "${eval.indicator.name}: ${"%.1f".format(eval.value)} ${eval.indicator.unit}",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
