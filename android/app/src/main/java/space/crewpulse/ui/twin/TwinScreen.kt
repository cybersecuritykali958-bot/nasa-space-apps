package space.crewpulse.ui.twin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.data.model.Status
import space.crewpulse.domain.DigitalTwin
import space.crewpulse.ui.components.AnimatedNumber
import space.crewpulse.ui.components.ChartLine
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.LineChart
import space.crewpulse.ui.theme.StatusColors
import kotlin.math.roundToInt

private enum class Scenario { MARS, ISS, CURRENT }

/** Digital Twin: fixed controls on top (scenario, values, day, exercise), calm charts below. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwinScreen(
    project: (adherence: Double, phases: List<DigitalTwin.Phase>?) -> List<DigitalTwin.State>,
    onBack: (() -> Unit)?,
    embedded: Boolean = false,
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
    val coach = remember(scenario) { DigitalTwin.coach { a -> project(a, phases) } }

    // Embedded under the Physiology header: no own title or status-bar inset.
    Column(if (embedded) Modifier.fillMaxSize() else Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!embedded) {
                Text(stringResource(R.string.twin_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
            }
            val labels = listOf(
                Scenario.MARS to stringResource(R.string.scenario_mars),
                Scenario.ISS to stringResource(R.string.scenario_iss),
                Scenario.CURRENT to stringResource(R.string.scenario_current),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                labels.forEachIndexed { i, (s, label) ->
                    SegmentedButton(
                        selected = scenario == s,
                        onClick = { scenario = s },
                        shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                        icon = {},
                    ) { Text(label, maxLines = 1) }
                }
            }
            if (states.isEmpty()) {
                Text(stringResource(R.string.loading))
                return@Column
            }
            val selected = states[((states.size - 1) * dayFraction).toInt().coerceIn(0, states.size - 1)]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(Icons.Outlined.Healing, stringResource(R.string.bone), selected.bonePct, { "%.0f%%".format(it) }, selected.boneStatus, Modifier.weight(1f))
                Tile(Icons.Outlined.FitnessCenter, stringResource(R.string.muscle), selected.musclePct, { "%.0f%%".format(it) }, selected.muscleStatus, Modifier.weight(1f))
                Tile(Icons.Outlined.WbSunny, stringResource(R.string.dose), selected.doseMsv, { "%.0f".format(it) }, selected.doseStatus, Modifier.weight(1f), unit = "mSv")
            }
            Text(stringResource(R.string.mission_day, selected.day, states.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = dayFraction, onValueChange = { dayFraction = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.adherence), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = exercise, onCheckedChange = { exercise = it })
            }
        }

        if (states.isNotEmpty()) {
            val selected = states[((states.size - 1) * dayFraction).toInt().coerceIn(0, states.size - 1)]
            val extrapolationFrom = DigitalTwin.DATA_LIMIT_DAYS.toFloat().takeIf { states.size > DigitalTwin.DATA_LIMIT_DAYS }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                coach?.let { CoachCard(it) }
                Chart(stringResource(R.string.bone), states.map { it.day.toFloat() to it.bonePct.toFloat() }, selected.boneStatus,
                    listOf(ChartLine(DigitalTwin.BONE_GREEN_MIN.toFloat(), StatusColors.yellow), ChartLine(DigitalTwin.BONE_RED_BELOW.toFloat(), StatusColors.red)),
                    extrapolationFrom, selected.day.toFloat())
                Chart(stringResource(R.string.muscle), states.map { it.day.toFloat() to it.musclePct.toFloat() }, selected.muscleStatus,
                    listOf(ChartLine(DigitalTwin.MUSCLE_GREEN_MIN.toFloat(), StatusColors.yellow), ChartLine(DigitalTwin.MUSCLE_RED_BELOW.toFloat(), StatusColors.red)),
                    extrapolationFrom, selected.day.toFloat())
                Chart(stringResource(R.string.dose), states.map { it.day.toFloat() to it.doseMsv.toFloat() }, selected.doseStatus,
                    listOf(ChartLine((DigitalTwin.DOSE_LIMIT_MSV * DigitalTwin.DOSE_YELLOW_FRACTION).toFloat(), StatusColors.yellow),
                        ChartLine(DigitalTwin.DOSE_LIMIT_MSV.toFloat(), StatusColors.red)),
                    extrapolationFrom, selected.day.toFloat())
                Text(stringResource(R.string.reid, selected.reidPct), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (selected.extrapolated) {
                    Text(stringResource(R.string.extrapolation), color = StatusColors.yellow, style = MaterialTheme.typography.bodyMedium)
                }
                Text(stringResource(R.string.twin_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }
}

/** Inverse Digital Twin: how much of the exercise protocol this scenario needs. */
@Composable
private fun CoachCard(c: DigitalTwin.Coach) {
    CrewCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.coach_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
        }
        val pct = { a: Double -> (a * 100).roundToInt() }
        if (c.forGreen != null) {
            Text(
                stringResource(R.string.coach_green, pct(c.forGreen), c.lastDay),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (c.forNoRed != null) {
            Text(
                stringResource(R.string.coach_no_red, pct(c.forNoRed)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (c.forNoRed == null && c.firstRedDayAtFull != null) {
            Text(
                stringResource(R.string.coach_impossible, c.firstRedDayAtFull),
                style = MaterialTheme.typography.bodyLarge,
                color = StatusColors.red,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else if (c.forGreen == null && c.firstRedDayAtFull == null) {
            Text(
                stringResource(R.string.coach_yellow_only, c.lastDay),
                style = MaterialTheme.typography.bodyLarge,
                color = StatusColors.yellow,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            stringResource(R.string.coach_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Tile(icon: ImageVector, label: String, value: Double, format: (Double) -> String, status: Status, modifier: Modifier, unit: String? = null) {
    CrewCard(modifier = modifier) {
        Icon(icon, null, tint = if (status == Status.GREEN) MaterialTheme.colorScheme.primary else StatusColors.of(status))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedNumber(value, format, MaterialTheme.typography.titleLarge, durationMillis = 300)
            if (unit != null) Text(" $unit", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Box(Modifier.padding(start = 6.dp).size(8.dp).background(StatusColors.of(status), CircleShape))
        }
    }
}

@Composable
private fun Chart(title: String, points: List<Pair<Float, Float>>, status: Status, lines: List<ChartLine>, extrapolationFrom: Float?, marker: Float) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        LineChart(
            points = points,
            color = if (status == Status.GREEN) MaterialTheme.colorScheme.primary else StatusColors.of(status),
            lines = lines,
            dashedFromX = extrapolationFrom,
            markerX = marker,
            height = 90.dp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
