package space.crewpulse.ui.twin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.data.model.Status
import space.crewpulse.domain.DigitalTwin
import space.crewpulse.ui.components.BodyFigure
import space.crewpulse.ui.components.ChartLine
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.components.LineChart
import space.crewpulse.ui.components.StatusPill
import space.crewpulse.ui.theme.StatusColors

private enum class Scenario { CURRENT, ISS, MARS }

/** Digital Twin: projected bone, muscle and dose over the mission, with exercise adherence. */
@Composable
fun TwinScreen(
    project: (adherence: Double, phases: List<DigitalTwin.Phase>?) -> List<DigitalTwin.State>,
    onBack: (() -> Unit)?,
) {
    var scenario by remember { mutableStateOf(Scenario.MARS) }
    var exercise by remember { mutableStateOf(true) }
    var dayFraction by remember { mutableFloatStateOf(1f) }

    val phases = when (scenario) {
        Scenario.CURRENT -> null
        Scenario.ISS -> DigitalTwin.SCENARIOS["iss_6mo"]
        Scenario.MARS -> DigitalTwin.SCENARIOS["mars_mission"]
    }
    val states = remember(scenario, exercise) { project(if (exercise) 1.0 else 0.0, phases) }

    CrewScaffold(title = stringResource(R.string.twin_title), onBack = onBack) { pad ->
        if (states.isEmpty()) {
            Text(stringResource(R.string.loading), pad.padding(16.dp))
            return@CrewScaffold
        }
        val selected = states[((states.size - 1) * dayFraction).toInt().coerceIn(0, states.size - 1)]
        val extrapolationFrom = DigitalTwin.DATA_LIMIT_DAYS.toFloat().takeIf { states.size > DigitalTwin.DATA_LIMIT_DAYS }

        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(scenario == Scenario.MARS, { scenario = Scenario.MARS }, { Text(stringResource(R.string.scenario_mars)) })
                FilterChip(scenario == Scenario.ISS, { scenario = Scenario.ISS }, { Text(stringResource(R.string.scenario_iss)) })
                FilterChip(scenario == Scenario.CURRENT, { scenario = Scenario.CURRENT }, { Text(stringResource(R.string.scenario_current)) })
            }
            Row(verticalAlignment = Alignment.Top) {
                BodyFigure(
                    statuses = emptyMap(),
                    skeleton = true,
                    modifier = Modifier.width(120.dp),
                )
                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric(stringResource(R.string.bone), "%.1f %%".format(selected.bonePct), selected.boneStatus)
                    Metric(stringResource(R.string.muscle), "%.1f %%".format(selected.musclePct), selected.muscleStatus)
                    Metric(stringResource(R.string.dose), "%.0f mSv".format(selected.doseMsv), selected.doseStatus)
                    Text(stringResource(R.string.reid, selected.reidPct), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (selected.extrapolated) {
                Text(stringResource(R.string.extrapolation), color = StatusColors.yellow, style = MaterialTheme.typography.labelLarge)
            }

            ChartCard(stringResource(R.string.bone), states.map { it.day.toFloat() to it.bonePct.toFloat() }, selected.boneStatus,
                listOf(ChartLine(DigitalTwin.BONE_GREEN_MIN.toFloat(), StatusColors.yellow), ChartLine(DigitalTwin.BONE_RED_BELOW.toFloat(), StatusColors.red)),
                extrapolationFrom, selected.day.toFloat())
            ChartCard(stringResource(R.string.muscle), states.map { it.day.toFloat() to it.musclePct.toFloat() }, selected.muscleStatus,
                listOf(ChartLine(DigitalTwin.MUSCLE_GREEN_MIN.toFloat(), StatusColors.yellow), ChartLine(DigitalTwin.MUSCLE_RED_BELOW.toFloat(), StatusColors.red)),
                extrapolationFrom, selected.day.toFloat())
            ChartCard(stringResource(R.string.dose), states.map { it.day.toFloat() to it.doseMsv.toFloat() }, selected.doseStatus,
                listOf(ChartLine((DigitalTwin.DOSE_LIMIT_MSV * DigitalTwin.DOSE_YELLOW_FRACTION).toFloat(), StatusColors.yellow),
                    ChartLine(DigitalTwin.DOSE_LIMIT_MSV.toFloat(), StatusColors.red)),
                extrapolationFrom, selected.day.toFloat())

            CrewCard {
                Text(stringResource(R.string.mission_day, selected.day, states.size), style = MaterialTheme.typography.titleSmall)
                Slider(value = dayFraction, onValueChange = { dayFraction = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.adherence), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(checked = exercise, onCheckedChange = { exercise = it })
                }
            }
            Text(stringResource(R.string.twin_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun Metric(label: String, value: String, status: Status) {
    CrewCard {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            StatusPill(status)
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    points: List<Pair<Float, Float>>,
    status: Status,
    lines: List<ChartLine>,
    extrapolationFrom: Float?,
    marker: Float,
) {
    CrewCard {
        Text(title, style = MaterialTheme.typography.titleSmall)
        LineChart(
            points = points,
            color = StatusColors.of(status),
            lines = lines,
            dashedFromX = extrapolationFrom,
            markerX = marker,
            height = 110.dp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
