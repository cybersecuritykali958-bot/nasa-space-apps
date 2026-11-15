package space.crewpulse.ui.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import space.crewpulse.ui.body3d.TurntableBody
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.i18n.systemLabel
import space.crewpulse.ui.navigation.Routes
import space.crewpulse.ui.theme.StatusColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.saveable.rememberSaveable
import space.crewpulse.domain.DigitalTwin
import space.crewpulse.ui.components.TabHeader
import space.crewpulse.ui.twin.TwinScreen

/** Physiological Status tab: current state of each body system, or the digital-twin projection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    state: HomeUiState,
    onIndicator: (String) -> Unit,
    onNavigate: (String) -> Unit,
    projectTwin: (adherence: Double, phases: List<DigitalTwin.Phase>?) -> List<DigitalTwin.State>,
) {
    var projection by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            TabHeader(stringResource(R.string.title_physiology))
            val views = listOf(false to stringResource(R.string.seg_current), true to stringResource(R.string.seg_projection))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                views.forEachIndexed { i, (isProjection, label) ->
                    SegmentedButton(
                        selected = projection == isProjection,
                        onClick = { projection = isProjection },
                        shape = SegmentedButtonDefaults.itemShape(i, views.size),
                        icon = {},
                    ) { Text(label, maxLines = 1) }
                }
            }
        }
        if (projection) {
            TwinScreen(project = projectTwin, onBack = null, embedded = true)
        } else {
            CurrentState(state, onIndicator, onNavigate, Modifier.weight(1f))
        }
    }
}

/** Each body system as a card with its indicators, under the holographic body. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentState(state: HomeUiState, onIndicator: (String) -> Unit, onNavigate: (String) -> Unit, modifier: Modifier) {
    val list = rememberLazyListState()
    var selected by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 20.dp),
        state = list,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FlowRow(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(onClick = { onNavigate(Routes.CHECKIN) }) { Text(stringResource(R.string.qa_checkin)) }
                OutlinedButton(onClick = { onNavigate(Routes.CAMERA_PULSE) }) { Text(stringResource(R.string.qa_camera_pulse)) }
                OutlinedButton(onClick = { onNavigate(Routes.WHO5) }) { Text(stringResource(R.string.qa_who5)) }
                OutlinedButton(onClick = { onNavigate(Routes.TRENDS) }) { Text(stringResource(R.string.qa_trends)) }
            }
        }
        // Holographic body: tap an organ to see that system's status and indicators right below.
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TurntableBody(
                    systems = state.systems,
                    heartRate = state.evaluation("heart_rate")?.value,
                    selected = selected,
                    onSystem = { id -> selected = if (selected == id) null else id },
                    modifier = Modifier.fillMaxWidth().height(380.dp),
                )
                val picked = state.systems.firstOrNull { it.systemId == selected }
                if (picked != null) {
                    SelectedSystemCard(picked, onIndicator, onClose = { selected = null })
                } else {
                    Text(
                        stringResource(R.string.body3d_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

/** Status and indicators of the organ tapped on the body figure. */
@Composable
private fun SelectedSystemCard(system: space.crewpulse.domain.SystemStatus, onIndicator: (String) -> Unit, onClose: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClose),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            ListRow(
                title = systemLabel(system.systemId),
                icon = CrewIcons.system(system.systemId),
                iconTint = StatusColors.of(system.status),
                trailing = { StatusPill(system.status) },
            )
            system.evaluations.forEach { eval ->
                Row(
                    Modifier.fillMaxWidth().clickable { onIndicator(eval.indicator.id) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(indicatorName(eval.indicator), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        "${"%.1f".format(eval.value)} ${eval.indicator.unit}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (eval.status == Status.GREEN) MaterialTheme.colorScheme.onSurface else StatusColors.of(eval.status),
                    )
                }
            }
        }
    }
}
