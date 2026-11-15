package space.crewpulse.ui.readiness

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FlightLand
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.ui.components.TabHeader
import androidx.compose.material.icons.automirrored.outlined.Send
import space.crewpulse.data.model.Status
import space.crewpulse.domain.SpaceWeatherLevel
import space.crewpulse.domain.Verdict
import space.crewpulse.ui.components.AnimatedNumber
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.ListRow
import space.crewpulse.ui.components.TriagePlan
import space.crewpulse.ui.components.VerdictPill
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.home.StatusMark
import space.crewpulse.ui.i18n.durationText
import space.crewpulse.ui.i18n.readinessItemText
import space.crewpulse.ui.i18n.responderLabel
import space.crewpulse.ui.i18n.triageReason
import space.crewpulse.ui.theme.StatusColors

/** Triage with communication delay, step-by-step plan, GO / NO-GO, space weather, radiation budget. */
@Composable
fun ReadinessScreen(state: HomeUiState, onRefreshWeather: () -> Unit, onBack: (() -> Unit)?, onEarthReport: () -> Unit = {}) {
    val report = state.report
    val t = report.triage
    var help by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TabHeader(stringResource(R.string.title_fitness))

        // Earth response vs response window, and the decision.
        CrewCard {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                TimeColumn(stringResource(R.string.earth_response), t.earthResponseMin, Modifier.weight(1f))
                VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
                TimeColumn(stringResource(R.string.response_window), t.responseWindowMin, Modifier.weight(1f))
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusMark(t.status.takeIf { it != Status.UNKNOWN } ?: Status.GREEN)
                Text(responderLabel(t.responder), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
            }
            Text(triageReason(report), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            OutlinedButton(onClick = onEarthReport, modifier = Modifier.padding(top = 10.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                Text(stringResource(R.string.earth_open), Modifier.padding(start = 8.dp))
            }
        }

        // Concrete steps.
        CrewCard { TriagePlan(report) }

        // Operations.
        report.readiness.forEach { r ->
            CrewCard {
                ListRow(
                    title = when (r.operationId) {
                        "eva" -> stringResource(R.string.op_eva)
                        "landing" -> stringResource(R.string.op_landing)
                        else -> r.operationName
                    },
                    subtitle = when (r.operationId) {
                        "eva" -> stringResource(R.string.op_eva_sub)
                        "landing" -> stringResource(R.string.op_landing_sub)
                        else -> null
                    },
                    icon = if (r.operationId == "landing") Icons.Outlined.FlightLand else Icons.AutoMirrored.Outlined.DirectionsWalk,
                    iconTint = when (r.verdict) {
                        Verdict.GO -> MaterialTheme.colorScheme.primary
                        Verdict.CAUTION -> StatusColors.yellow
                        Verdict.NO_GO -> StatusColors.red
                    },
                    trailing = { VerdictPill(r.verdict) },
                )
                r.items.forEach { item ->
                    Text(
                        "• " + readinessItemText(item, state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 44.dp, bottom = 4.dp),
                    )
                }
            }
        }

        // Space weather and radiation.
        report.spaceWeather?.let { sw ->
            CrewCard {
                ListRow(
                    title = stringResource(R.string.space_weather),
                    subtitle = when (sw.level) {
                        SpaceWeatherLevel.WARNING -> stringResource(R.string.sw_warning)
                        SpaceWeatherLevel.WATCH -> stringResource(R.string.sw_watch)
                        SpaceWeatherLevel.QUIET -> stringResource(R.string.sw_quiet)
                    } + "\n" + stringResource(R.string.data_source, sw.dataSource),
                    icon = Icons.Outlined.WbSunny,
                    iconTint = StatusColors.yellow,
                )
                TextButton(onClick = onRefreshWeather) { Text(stringResource(R.string.refresh)) }
            }
        }
        report.radiation?.let { b ->
            CrewCard {
                ListRow(
                    title = stringResource(R.string.radiation_budget),
                    subtitle = stringResource(R.string.rad_used, b.usedMsv, b.limitMsv) +
                        (b.daysLeft?.let { "\n" + stringResource(R.string.rad_days_left, it) } ?: ""),
                    icon = Icons.Outlined.Shield,
                )
            }
        }

        // How to read this screen, on demand.
        CrewCard(modifier = Modifier.clickable { help = !help }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.how_to_read), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp).weight(1f))
                Icon(if (help) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (help) HowToRead(state)
        }
        Text("", Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun TimeColumn(label: String, minutes: Double?, modifier: Modifier) {
    val (value, unit) = durationText(minutes)
    val hours = (minutes ?: 0.0) >= 120
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            if (minutes == null) Text(value, style = MaterialTheme.typography.displaySmall)
            else AnimatedNumber(if (hours) minutes / 60 else minutes, { "%.0f".format(it) }, MaterialTheme.typography.displaySmall)
            Text(" $unit", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

@Composable
private fun HowToRead(state: HomeUiState) {
    val t = state.report.triage
    val delay = state.environment?.commDelayMinOneWay ?: 0.0
    val handling = ((t.earthResponseMin ?: 0.0) - 2 * delay).coerceAtLeast(0.0)
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Bullet(stringResource(R.string.explain_earth, delay, 2 * delay, handling, t.earthResponseMin ?: 0.0))
        Bullet(
            when (t.status) {
                Status.RED -> stringResource(R.string.explain_window_red)
                Status.YELLOW -> stringResource(R.string.explain_window_yellow)
                else -> stringResource(R.string.explain_window_none)
            },
        )
        Bullet(stringResource(R.string.explain_rule))
        Legend(Verdict.GO, stringResource(R.string.legend_go))
        Legend(Verdict.CAUTION, stringResource(R.string.legend_caution))
        Legend(Verdict.NO_GO, stringResource(R.string.legend_nogo))
    }
}

@Composable
private fun Legend(v: Verdict, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        VerdictPill(v)
        Text(text, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•", Modifier.width(16.dp), color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

