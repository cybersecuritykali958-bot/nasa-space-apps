package space.crewpulse.ui.indicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import space.crewpulse.data.model.Risk
import space.crewpulse.data.model.Source
import space.crewpulse.domain.BodySystems
import space.crewpulse.domain.Evaluation
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.components.StatusLabel
import space.crewpulse.ui.theme.CrewColors
import space.crewpulse.ui.theme.StatusColors

@Composable
fun IndicatorScreen(evaluation: Evaluation?, risks: List<Risk>, onBack: () -> Unit) {
    CrewScaffold(
        title = "Why?",
        subtitle = evaluation?.indicator?.name ?: "Indicator",
        onBack = onBack,
    ) { paddingModifier ->
        if (evaluation == null) {
            Text(
                "No reading for this indicator. Complete check-in or open a value from Home.",
                Modifier.padding(16.dp).then(paddingModifier),
            )
            return@CrewScaffold
        }
        val ind = evaluation.indicator
        Column(
            Modifier
                .fillMaxSize()
                .then(paddingModifier)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CrewCard(accent = StatusColors.of(evaluation.status)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(BodySystems.label(ind.system), style = MaterialTheme.typography.titleSmall, color = CrewColors.missionAccent)
                    Text(formatValue(ind.id, evaluation.value, ind.unit), style = MaterialTheme.typography.displaySmall)
                    StatusLabel(evaluation.status)
                    evaluation.deviationPct?.let { pct ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Baseline deviation: ${"%+.0f".format(pct)}%", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            ActionCard(evaluation)
            CrewCard { SectionText("Why it matters in space", ind.whySpace) }
            CrewCard { SectionText("How to measure", ind.howToMeasure) }
            risks.filter { it.id in ind.riskIds }.takeIf { it.isNotEmpty() }?.let { linked ->
                CrewCard {
                    SectionText(
                        "NASA HRP risks",
                        linked.joinToString("\n\n") { "• ${it.name}\n  ${it.summary}" },
                    )
                }
            }
            SourcesCard(ind.sources)
            if (!ind.verified) {
                Text("Thresholds pending science review", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ActionCard(evaluation: Evaluation) {
    val ind = evaluation.indicator
    CrewCard(accent = StatusColors.of(evaluation.status)) {
        SectionText("What to do", ind.actions[evaluation.status] ?: "No guidance for this status.")
    }
}

@Composable
private fun SectionText(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SourcesCard(sources: List<Source>) {
    val uriHandler = LocalUriHandler.current
    CrewCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Sources", style = MaterialTheme.typography.titleSmall)
            sources.forEach { source ->
                TextButton(onClick = { uriHandler.openUri(source.url) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(source.title, style = MaterialTheme.typography.bodyMedium)
                        Text(source.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun formatValue(indicatorId: String, value: Double, unit: String): String = when (indicatorId) {
    "mood" -> "${value.toInt()} / 5"
    "sleep_hours" -> "%.1f h".format(value)
    else -> "%.1f $unit".format(value)
}