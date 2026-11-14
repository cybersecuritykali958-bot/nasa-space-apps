package space.crewpulse.ui.home

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import space.crewpulse.R
import space.crewpulse.data.model.Status
import space.crewpulse.domain.SpaceWeatherLevel
import space.crewpulse.domain.StatusEngine
import space.crewpulse.ui.components.CardRow
import space.crewpulse.ui.components.CrewIcons
import space.crewpulse.ui.i18n.alertText
import space.crewpulse.ui.i18n.environmentName
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.i18n.responderLabel
import space.crewpulse.ui.navigation.Routes
import space.crewpulse.ui.theme.StatusColors

/**
 * Home is a daily briefing: who and where, one status line, and today's plan.
 * Measurements live in the Health tab.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onIndicator: (String) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.headlineMedium)
                if (!state.loading && state.error == null) {
                    Text(
                        stringResource(
                            R.string.home_subtitle,
                            environmentName(state.environment?.id, state.environmentName),
                            state.missionDay,
                            "%.0f".format(state.commDelayMin ?: 0.0),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
            }
        }

        when {
            state.loading -> Text(stringResource(R.string.loading))
            state.error != null -> Text(stringResource(R.string.data_error, state.error), color = MaterialTheme.colorScheme.error)
            else -> Briefing(state, onIndicator, onNavigate)
        }
    }
}

@Composable
private fun Briefing(state: HomeUiState, onIndicator: (String) -> Unit, onNavigate: (String) -> Unit) {
    // One status line.
    val overall = StatusEngine.worst(state.systems.map { it.status })
    val attention = state.systems.count { it.status == Status.YELLOW || it.status == Status.RED }
    val ok = attention == 0
    CardRow(
        title = if (ok) stringResource(R.string.all_nominal) else stringResource(R.string.systems_attention, attention),
        subtitle = if (ok) null else responderLabel(state.report.triage.responder),
        leading = { StatusMark(overall) },
        onClick = { onNavigate(if (ok) Routes.HEALTH else Routes.READINESS) },
        modifier = Modifier.padding(top = 8.dp),
    )

    // One extra line only when something is really happening.
    val sw = state.report.spaceWeather?.takeIf { it.level == SpaceWeatherLevel.WARNING }
    val alert = state.report.alerts.firstOrNull()
    if (sw != null) {
        CardRow(
            title = stringResource(R.string.sw_warning),
            leading = { StatusMark(Status.RED) },
            onClick = { onNavigate(Routes.READINESS) },
        )
    } else if (alert != null) {
        val ind = state.evaluation(alert.indicatorId)?.indicator
        CardRow(
            title = alertText(alert, ind?.let { indicatorName(it) } ?: alert.indicatorId),
            leading = { StatusMark(Status.YELLOW) },
            onClick = { onIndicator(alert.indicatorId) },
        )
    }

    // Today's plan.
    Text(stringResource(R.string.todays_plan), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
    val plan = state.report.plan
    if (plan == null || plan.next24h.isEmpty()) {
        Text(stringResource(R.string.all_done), color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        val time = DateFormat.getTimeInstance(DateFormat.SHORT)
        plan.next24h.forEach { task ->
            val ind = state.evaluation(task.indicatorId)?.indicator
            CardRow(
                title = ind?.let { indicatorName(it) } ?: task.indicatorName,
                subtitle = time.format(Date(task.dueAtMillis)),
                icon = CrewIcons.indicator(task.indicatorId),
                onClick = { onNavigate(if (task.indicatorId == "pvt_mean_rt") Routes.PVT else Routes.CHECKIN) },
            )
        }
    }

    Text(
        stringResource(R.string.demo_data_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
    )
}

/** Round status mark: green check, or amber / red exclamation. */
@Composable
fun StatusMark(status: Status) {
    Box(Modifier.size(32.dp).background(StatusColors.of(status), CircleShape), contentAlignment = Alignment.Center) {
        Icon(
            if (status == Status.GREEN) Icons.Filled.Check else Icons.Filled.PriorityHigh,
            contentDescription = null,
            tint = StatusColors.onChip,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun greeting(): String = stringResource(
    when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    },
)
