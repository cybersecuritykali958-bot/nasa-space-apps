package space.crewpulse.ui.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.components.StatusDot
import space.crewpulse.ui.components.StatusPill
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.i18n.systemLabel
import space.crewpulse.ui.navigation.Routes

/** Health tab: every body system and indicator, with entry points to check-in and trends. */
@Composable
fun HealthScreen(state: HomeUiState, onIndicator: (String) -> Unit, onNavigate: (String) -> Unit) {
    CrewScaffold(title = stringResource(R.string.tab_health), subtitle = stringResource(R.string.health_subtitle)) { pad ->
        LazyColumn(
            pad.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onNavigate(Routes.CHECKIN) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.CheckCircle, null)
                        Text(stringResource(R.string.qa_checkin), Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = { onNavigate(Routes.TRENDS) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.DateRange, null)
                        Text(stringResource(R.string.qa_trends), Modifier.padding(start = 8.dp))
                    }
                }
            }
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
                            Modifier.fillMaxWidth().clickable { onIndicator(eval.indicator.id) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusDot(eval.status, 10.dp)
                            Text(indicatorName(eval.indicator), modifier = Modifier.padding(start = 10.dp).weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${"%.1f".format(eval.value)} ${eval.indicator.unit}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { Text("", Modifier.padding(bottom = 8.dp)) }
        }
    }
}
