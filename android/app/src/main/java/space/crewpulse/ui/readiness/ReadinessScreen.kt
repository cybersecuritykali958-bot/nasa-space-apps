package space.crewpulse.ui.readiness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.domain.Responder
import space.crewpulse.domain.SpaceWeatherLevel
import space.crewpulse.domain.Verdict
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.components.VerdictPill
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.i18n.durationText
import space.crewpulse.ui.i18n.readinessItemText
import space.crewpulse.ui.i18n.responderLabel
import space.crewpulse.ui.i18n.triageReason
import space.crewpulse.ui.theme.StatusColors

/** Triage with communication delay, GO / NO-GO for operations, space weather and radiation budget. */
@Composable
fun ReadinessScreen(state: HomeUiState, onRefreshWeather: () -> Unit, onBack: (() -> Unit)?) {
    val report = state.report
    val t = report.triage
    CrewScaffold(title = stringResource(R.string.readiness_title), onBack = onBack) { pad ->
        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CrewCard {
                Text(stringResource(R.string.time_to_earth), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TimeTile(stringResource(R.string.earth_response), t.earthResponseMin, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    Text("vs", Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TimeTile(stringResource(R.string.response_window), t.responseWindowMin, StatusColors.yellow, Modifier.weight(1f))
                }
                val color = when (t.responder) {
                    Responder.ONBOARD_PROTOCOL -> StatusColors.red
                    Responder.FLIGHT_SURGEON, Responder.CREW_MEDICAL_OFFICER -> StatusColors.yellow
                    Responder.SELF -> StatusColors.green
                }
                Text(
                    "↓  " + responderLabel(t.responder),
                    color = color,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                )
                Text(triageReason(state.report), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            report.readiness.forEach { r ->
                val accent = when (r.verdict) {
                    Verdict.GO -> StatusColors.green
                    Verdict.CAUTION -> StatusColors.yellow
                    Verdict.NO_GO -> StatusColors.red
                }
                CrewCard(accent = accent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (r.operationId == "eva") stringResource(R.string.op_eva) else if (r.operationId == "landing") stringResource(R.string.op_landing) else r.operationName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        VerdictPill(r.verdict)
                    }
                    r.items.forEach { item ->
                        val itemColor = when (item.verdict) {
                            Verdict.GO -> StatusColors.green
                            Verdict.CAUTION -> StatusColors.yellow
                            Verdict.NO_GO -> StatusColors.red
                        }
                        Row(Modifier.padding(top = 6.dp)) {
                            Icon(
                                if (item.verdict == Verdict.GO) Icons.Filled.Check else Icons.Filled.Warning,
                                contentDescription = null, tint = itemColor,
                            )
                            Text(readinessItemText(item, state), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            report.spaceWeather?.let { sw ->
                val color = when (sw.level) {
                    SpaceWeatherLevel.WARNING -> StatusColors.red
                    SpaceWeatherLevel.WATCH -> StatusColors.yellow
                    SpaceWeatherLevel.QUIET -> StatusColors.green
                }
                CrewCard(accent = color) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("☀", style = MaterialTheme.typography.headlineMedium, color = StatusColors.yellow)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(stringResource(R.string.space_weather), style = MaterialTheme.typography.titleSmall)
                            Text(
                                when (sw.level) {
                                    SpaceWeatherLevel.WARNING -> stringResource(R.string.sw_warning)
                                    SpaceWeatherLevel.WATCH -> stringResource(R.string.sw_watch)
                                    SpaceWeatherLevel.QUIET -> stringResource(R.string.sw_quiet)
                                },
                                color = color, style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    sw.latestSep?.let {
                        Text(
                            stringResource(R.string.last_sep, java.text.DateFormat.getDateInstance().format(java.util.Date(it.timeMillis))),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(stringResource(R.string.data_source, sw.dataSource), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onRefreshWeather) { Text(stringResource(R.string.refresh)) }
                }
            }

            report.radiation?.let { b ->
                CrewCard {
                    Text(stringResource(R.string.radiation_budget), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.rad_used, b.usedMsv, b.limitMsv), style = MaterialTheme.typography.titleLarge)
                    b.daysLeft?.let {
                        Text(stringResource(R.string.rad_days_left, it), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("", Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun TimeTile(label: String, minutes: Double?, color: Color, modifier: Modifier) {
    CrewCard(modifier = modifier, accent = color) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val (value, unit) = durationText(minutes)
        Text(value, style = MaterialTheme.typography.displaySmall, color = color)
        Text(unit, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
