package space.crewpulse.ui.indicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.data.model.Risk
import space.crewpulse.domain.AlertKind
import space.crewpulse.domain.Confidence
import space.crewpulse.domain.DAY
import space.crewpulse.domain.EarlyWarning
import space.crewpulse.domain.EngineReport
import space.crewpulse.domain.Evaluation
import space.crewpulse.domain.Sample
import space.crewpulse.ui.components.ChartLine
import space.crewpulse.ui.components.ConfidencePill
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.components.InfoRow
import space.crewpulse.ui.components.LineChart
import space.crewpulse.ui.components.StatusPill
import space.crewpulse.ui.i18n.alertText
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.theme.StatusColors

/** "Why this matters": value, trend chart with threshold and forecast, advice, sources, confidence. */
@Composable
fun IndicatorScreen(
    evaluation: Evaluation?,
    history: List<Sample>,
    report: EngineReport,
    risks: List<Risk>,
    onBack: () -> Unit,
) {
    CrewScaffold(title = stringResource(R.string.why_title), onBack = onBack) { pad ->
        if (evaluation == null) {
            Text(stringResource(R.string.no_data), pad.padding(16.dp))
            return@CrewScaffold
        }
        val ind = evaluation.indicator
        val name = indicatorName(ind)
        val alerts = report.alertsFor(ind.id)
        val trend = alerts.firstOrNull { it.kind == AlertKind.TREND_TO_THRESHOLD }
        val confidence: Confidence? = report.confidence[ind.id]

        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CrewCard {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${"%.1f".format(evaluation.value)}", style = MaterialTheme.typography.displaySmall)
                    Text(" ${ind.unit}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    StatusPill(evaluation.status)
                }
                evaluation.deviationPct?.let {
                    Text(stringResource(R.string.deviation, it), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val recent = history.takeLast(30)
                if (recent.size >= 2) {
                    val last = recent.last().timestampMillis
                    val points = recent.map { ((it.timestampMillis - last).toFloat() / DAY) to it.value.toFloat() }
                    val boundary = EarlyWarning.nextWorseBoundary(
                        ind, evaluation.value,
                        trend?.direction ?: space.crewpulse.domain.Direction.UP,
                    )
                    LineChart(
                        points = points,
                        color = StatusColors.of(evaluation.status),
                        lines = listOfNotNull(boundary?.let { ChartLine(it.first.toFloat(), StatusColors.of(it.second)) }),
                        forecast = trend?.let { t ->
                            (t.daysToThreshold ?: 0.0).toFloat() to (t.thresholdValue ?: evaluation.value).toFloat()
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(stringResource(R.string.chart_last_30), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                trend?.daysToThreshold?.let {
                    Text(stringResource(R.string.threshold_in, it), color = StatusColors.yellow, style = MaterialTheme.typography.labelLarge)
                }
            }

            alerts.forEach { a ->
                InfoRow(Icons.Filled.Warning, stringResource(R.string.early_warning), alertText(a, name))
            }
            InfoRow(Icons.Filled.CheckCircle, stringResource(R.string.what_to_do), ind.actions[evaluation.status] ?: "—")
            InfoRow(Icons.Filled.Info, stringResource(R.string.why_space), ind.whySpace)
            InfoRow(Icons.Filled.Build, stringResource(R.string.how_to_measure), ind.howToMeasure)
            val linked = risks.filter { it.id in ind.riskIds }
            if (linked.isNotEmpty()) {
                InfoRow(Icons.Filled.Info, stringResource(R.string.hrp_risks), linked.joinToString("\n") { "• ${it.name}" })
            }
            InfoRow(
                Icons.Filled.List, stringResource(R.string.sources),
                ind.sources.mapIndexed { i, s -> "${i + 1}. ${s.title}" }.joinToString("\n"),
            )
            confidence?.let { c ->
                CrewCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.confidence), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        ConfidencePill(c.level)
                    }
                    c.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (!ind.verified) {
                Text(stringResource(R.string.pending_review), color = MaterialTheme.colorScheme.error)
            }
            Text("", Modifier.padding(bottom = 16.dp))
        }
    }
}
