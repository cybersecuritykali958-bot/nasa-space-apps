package space.crewpulse.ui.trends

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.data.model.Status
import space.crewpulse.domain.DAY
import space.crewpulse.ui.components.ChartLine
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.components.LineChart
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.theme.StatusColors

/** Full history per indicator with the edges of the colored ranges. */
@Composable
fun TrendsScreen(state: HomeUiState, onBack: () -> Unit) {
    val evaluations = state.systems.flatMap { it.evaluations }
    var selectedId by remember { mutableStateOf(evaluations.firstOrNull()?.indicator?.id) }

    CrewScaffold(title = stringResource(R.string.trends_title), onBack = onBack) { pad ->
        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                evaluations.forEach { e ->
                    FilterChip(selectedId == e.indicator.id, { selectedId = e.indicator.id }, { Text(indicatorName(e.indicator)) })
                }
            }
            val eval = evaluations.firstOrNull { it.indicator.id == selectedId }
            val history = state.history[selectedId].orEmpty()
            if (eval == null || history.size < 2) {
                Text(stringResource(R.string.no_data))
                return@Column
            }
            val first = history.first().timestampMillis
            CrewCard {
                Text(indicatorName(eval.indicator) + " · " + eval.indicator.unit, style = MaterialTheme.typography.titleSmall)
                LineChart(
                    points = history.map { ((it.timestampMillis - first).toFloat() / DAY) to it.value.toFloat() },
                    color = StatusColors.of(eval.status),
                    lines = eval.indicator.bands.flatMap { b ->
                        listOfNotNull(b.min, b.max).map { ChartLine(it.toFloat(), StatusColors.of(if (b.status == Status.GREEN) Status.YELLOW else b.status)) }
                    }.distinctBy { it.y }.filter { l -> l.y in (history.minOf { it.value } * 0.8).toFloat()..(history.maxOf { it.value } * 1.2).toFloat() },
                    height = 220.dp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(stringResource(R.string.trends_days, history.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
