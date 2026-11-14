package space.crewpulse.ui.indicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WarningAmber
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
import space.crewpulse.domain.DAY
import space.crewpulse.domain.Direction
import space.crewpulse.domain.EarlyWarning
import space.crewpulse.domain.EngineReport
import space.crewpulse.domain.Evaluation
import space.crewpulse.domain.Sample
import space.crewpulse.ui.components.CardRow
import space.crewpulse.ui.components.ChartLine
import space.crewpulse.ui.components.ConfidencePill
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.components.LineChart
import space.crewpulse.ui.components.StatusPill
import space.crewpulse.ui.i18n.alertText
import space.crewpulse.ui.i18n.indicatorAction
import space.crewpulse.ui.i18n.indicatorHow
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.i18n.indicatorWhy
import space.crewpulse.ui.i18n.riskName
import space.crewpulse.ui.theme.StatusColors

/** One indicator: big value, calm chart with threshold and forecast, then explanation rows. */
@Composable
fun IndicatorScreen(
    evaluation: Evaluation?,
    history: List<Sample>,
    report: EngineReport,
    risks: List<Risk>,
    onBack: () -> Unit,
) {
    val name = evaluation?.indicator?.let { indicatorName(it) } ?: stringResource(R.string.why_title)
    CrewScaffold(title = name, onBack = onBack) { pad ->
        if (evaluation == null) {
            Text(stringResource(R.string.no_data), pad.padding(20.dp))
            return@CrewScaffold
        }
        val ind = evaluation.indicator
        val alerts = report.alertsFor(ind.id)
        val trend = alerts.firstOrNull { it.kind == AlertKind.TREND_TO_THRESHOLD }

        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%.1f".format(evaluation.value), style = MaterialTheme.typography.displaySmall)
                Text(" ${ind.unit}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                StatusPill(evaluation.status)
            }
            evaluation.deviationPct?.let {
                Text(stringResource(R.string.deviation, it), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val recent = history.takeLast(30)
            if (recent.size >= 2) {
                val last = recent.last().timestampMillis
                val boundary = EarlyWarning.nextWorseBoundary(ind, evaluation.value, trend?.direction ?: Direction.UP)
                LineChart(
                    points = recent.map { ((it.timestampMillis - last).toFloat() / DAY) to it.value.toFloat() },
                    color = MaterialTheme.colorScheme.primary,
                    lines = listOfNotNull(boundary?.let { ChartLine(it.first.toFloat(), StatusColors.of(it.second)) }),
                    forecast = trend?.let { t -> (t.daysToThreshold ?: 0.0).toFloat() to (t.thresholdValue ?: evaluation.value).toFloat() },
                    height = 180.dp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                trend?.daysToThreshold?.let {
                    Text(stringResource(R.string.threshold_in, it), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }

            alerts.forEach { a ->
                CardRow(title = stringResource(R.string.early_warning), subtitle = alertText(a, name), icon = Icons.Outlined.WarningAmber, iconTint = StatusColors.yellow)
            }
            CardRow(title = stringResource(R.string.what_to_do), subtitle = indicatorAction(ind, evaluation.status), icon = Icons.Outlined.Lightbulb, iconTint = StatusColors.yellow)
            CardRow(title = stringResource(R.string.why_space), subtitle = indicatorWhy(ind), icon = Icons.Outlined.Info)
            CardRow(title = stringResource(R.string.how_to_measure), subtitle = indicatorHow(ind), icon = Icons.Outlined.Straighten)
            val linked = risks.filter { it.id in ind.riskIds }
            if (linked.isNotEmpty()) {
                CardRow(
                    title = stringResource(R.string.hrp_risks),
                    subtitle = linked.map { riskName(it) }.joinToString("\n"),
                    icon = Icons.Outlined.PriorityHigh,
                )
            }
            report.confidence[ind.id]?.let { c ->
                CardRow(
                    title = stringResource(R.string.confidence),
                    subtitle = c.reasons.joinToString("\n").ifEmpty { null },
                    trailing = { ConfidencePill(c.level) },
                )
            }
            CardRow(
                title = stringResource(R.string.sources),
                subtitle = ind.sources.joinToString("\n") { it.title },
                icon = Icons.AutoMirrored.Outlined.MenuBook,
            )
            if (!ind.verified) {
                Text(stringResource(R.string.pending_review), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Text("", Modifier.padding(bottom = 12.dp))
        }
    }
}
