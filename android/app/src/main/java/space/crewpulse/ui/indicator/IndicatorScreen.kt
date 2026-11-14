package space.crewpulse.ui.indicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.crewpulse.data.model.Risk
import space.crewpulse.domain.Evaluation
import space.crewpulse.ui.components.StatusDot

/** The "Why?" card: what the value means, how to measure it, what to do, and the source. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorScreen(evaluation: Evaluation?, risks: List<Risk>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(evaluation?.indicator?.name ?: "Indicator") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        if (evaluation == null) {
            Text("No data", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val ind = evaluation.indicator
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(evaluation.status, 22.dp)
                Text(
                    "${"%.1f".format(evaluation.value)} ${ind.unit}",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            evaluation.deviationPct?.let {
                Text("Deviation from your pre-flight baseline: ${"%+.0f".format(it)}%")
            }
            Section("What to do", ind.actions[evaluation.status] ?: "—")
            Section("Why it matters in space", ind.whySpace)
            Section("How to measure", ind.howToMeasure)
            val linked = risks.filter { it.id in ind.riskIds }
            if (linked.isNotEmpty()) {
                Section("NASA HRP risks", linked.joinToString("\n") { "• ${it.name}" })
            }
            Section("Sources", ind.sources.joinToString("\n") { "• ${it.title}\n  ${it.url}" })
            if (!ind.verified) {
                Text("Thresholds pending science review", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
