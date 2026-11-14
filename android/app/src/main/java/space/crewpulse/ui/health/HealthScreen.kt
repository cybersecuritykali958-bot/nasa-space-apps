package space.crewpulse.ui.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.data.model.Status
import space.crewpulse.ui.components.CrewIcons
import space.crewpulse.ui.components.ListRow
import space.crewpulse.ui.components.StatusPill
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.i18n.systemLabel
import space.crewpulse.ui.navigation.Routes
import space.crewpulse.ui.theme.StatusColors

/** Health tab: each body system as a card with its indicators. */
@Composable
fun HealthScreen(state: HomeUiState, onIndicator: (String) -> Unit, onNavigate: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tab_health), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                Button(onClick = { onNavigate(Routes.CHECKIN) }) { Text(stringResource(R.string.qa_checkin)) }
                OutlinedButton(onClick = { onNavigate(Routes.TRENDS) }, modifier = Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.qa_trends))
                }
            }
        }
        items(state.systems, key = { it.systemId }) { system ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    ListRow(
                        title = systemLabel(system.systemId),
                        icon = CrewIcons.system(system.systemId),
                        iconTint = if (system.status == Status.GREEN) MaterialTheme.colorScheme.primary else StatusColors.of(system.status),
                        trailing = { if (system.status != Status.GREEN) StatusPill(system.status) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    system.evaluations.forEach { eval ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onIndicator(eval.indicator.id) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                indicatorName(eval.indicator),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${"%.1f".format(eval.value)} ${eval.indicator.unit}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (eval.status == Status.GREEN) MaterialTheme.colorScheme.onSurface else StatusColors.of(eval.status),
                            )
                        }
                    }
                    Text("", Modifier.padding(bottom = 4.dp))
                }
            }
        }
        item { Text("", Modifier.padding(bottom = 8.dp)) }
    }
}
