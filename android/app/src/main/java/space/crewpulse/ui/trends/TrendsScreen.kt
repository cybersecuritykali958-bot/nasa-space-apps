package space.crewpulse.ui.trends

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import space.crewpulse.R
import space.crewpulse.data.Profile
import space.crewpulse.data.model.Status
import space.crewpulse.domain.DAY
import space.crewpulse.ui.components.ChartLine
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.components.LineChart
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.theme.StatusColors

/** History per indicator: chart from two measurements on, always the list of recent values. */
@Composable
fun TrendsScreen(state: HomeUiState, onShowDemo: () -> Unit, onBack: () -> Unit) {
    val evaluations = state.systems.flatMap { it.evaluations }
    var chosen by remember { mutableStateOf<String?>(null) }
    // Falls back to the first indicator, also when data arrives after the screen opened.
    val selectedId = chosen?.takeIf { id -> evaluations.any { it.indicator.id == id } } ?: evaluations.firstOrNull()?.indicator?.id

    CrewScaffold(title = stringResource(R.string.trends_title), onBack = onBack) { pad ->
        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (evaluations.isEmpty()) {
                EmptyHint(state.profile, onShowDemo)
                return@Column
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                evaluations.forEach { e ->
                    FilterChip(selectedId == e.indicator.id, { chosen = e.indicator.id }, { Text(indicatorName(e.indicator)) })
                }
            }
            val eval = evaluations.first { it.indicator.id == selectedId }
            val history = state.history[selectedId].orEmpty()
            val unit = eval.indicator.unit

            if (history.size >= 2) {
                val first = history.first().timestampMillis
                val lo = history.minOf { it.value }
                val hi = history.maxOf { it.value }
                val span = (hi - lo).coerceAtLeast(1.0)
                CrewCard {
                    Text(indicatorName(eval.indicator) + " · " + unit, style = MaterialTheme.typography.titleSmall)
                    LineChart(
                        points = history.map { ((it.timestampMillis - first).toFloat() / DAY) to it.value.toFloat() },
                        color = StatusColors.of(eval.status),
                        lines = eval.indicator.bands.flatMap { b ->
                            listOfNotNull(b.min, b.max).map { ChartLine(it.toFloat(), StatusColors.of(if (b.status == Status.GREEN) Status.YELLOW else b.status)) }
                        }.distinctBy { it.y }.filter { it.y in (lo - span).toFloat()..(hi + span).toFloat() },
                        height = 220.dp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(stringResource(R.string.trends_days, history.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                CrewCard {
                    Text(stringResource(R.string.trends_need_two), style = MaterialTheme.typography.bodyLarge)
                    if (state.profile == Profile.ME) {
                        OutlinedButton(onClick = onShowDemo, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.show_demo)) }
                    }
                }
            }

            CrewCard {
                Text(stringResource(R.string.recent_values), style = MaterialTheme.typography.titleSmall)
                val format = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
                history.takeLast(10).reversed().forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(format.format(Date(s.timestampMillis)), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${"%.1f".format(s.value)} $unit", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(profile: Profile, onShowDemo: () -> Unit) {
    CrewCard {
        Text(stringResource(R.string.trends_empty), style = MaterialTheme.typography.bodyLarge)
        if (profile == Profile.ME) {
            OutlinedButton(onClick = onShowDemo, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.show_demo)) }
        }
    }
}
