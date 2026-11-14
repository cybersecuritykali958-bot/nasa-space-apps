package space.crewpulse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.data.model.Status
import space.crewpulse.domain.EngineReport
import space.crewpulse.domain.Evaluation
import space.crewpulse.domain.Responder
import space.crewpulse.ui.i18n.indicatorAction
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.theme.StatusColors

/** Numbered, concrete steps for the triage decision. */
@Composable
fun triageSteps(report: EngineReport): List<String> {
    val t = report.triage
    val earth = t.earthResponseMin ?: 0.0
    val flagged = flaggedEvaluations(report).isNotEmpty()
    return when (t.responder) {
        Responder.ONBOARD_PROTOCOL -> listOf(
            stringResource(R.string.step_stop),
            stringResource(R.string.step_onboard_cmo),
            stringResource(R.string.step_remeasure_15),
            stringResource(R.string.step_onboard_report, earth),
        )
        Responder.FLIGHT_SURGEON -> listOf(
            stringResource(R.string.step_stop),
            stringResource(R.string.step_surgeon_call, earth),
            stringResource(R.string.step_surgeon_wait),
            stringResource(R.string.step_remeasure_15),
        )
        Responder.CREW_MEDICAL_OFFICER -> listOf(
            stringResource(R.string.step_cmo_remeasure),
            stringResource(R.string.step_cmo_tell),
            stringResource(R.string.step_follow_advice),
            stringResource(R.string.step_cmo_tomorrow),
        )
        Responder.SELF -> if (flagged) {
            listOf(stringResource(R.string.step_follow_advice), stringResource(R.string.step_self_plan))
        } else {
            listOf(stringResource(R.string.step_ok))
        }
    }
}

/** Indicators that are yellow or red right now, worst first. */
fun flaggedEvaluations(report: EngineReport): List<Evaluation> =
    report.systems.flatMap { it.evaluations }
        .filter { it.status == Status.YELLOW || it.status == Status.RED }
        .sortedByDescending { if (it.status == Status.RED) 2 else 1 }

/** Full plan: numbered steps, flagged values with their advice, and the symptoms rule. */
@Composable
fun TriagePlan(report: EngineReport, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.steps_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        triageSteps(report).forEachIndexed { i, step -> NumberedStep(i + 1, step) }

        val flagged = flaggedEvaluations(report)
        if (flagged.isNotEmpty()) {
            Text(
                stringResource(R.string.flagged_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
            flagged.forEach { e ->
                Column(Modifier.padding(start = 4.dp)) {
                    Row {
                        StatusDot(e.status, 12.dp)
                        Text(
                            "${indicatorName(e.indicator)}: ${"%.1f".format(e.value)} ${e.indicator.unit}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        indicatorAction(e.indicator, e.status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp),
                    )
                }
            }
        }

        Row(Modifier.padding(top = 6.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = StatusColors.red)
            Text(
                stringResource(R.string.symptoms_rule),
                style = MaterialTheme.typography.bodyMedium,
                color = StatusColors.red,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun NumberedStep(n: Int, text: String) {
    Row {
        Text("$n.", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
