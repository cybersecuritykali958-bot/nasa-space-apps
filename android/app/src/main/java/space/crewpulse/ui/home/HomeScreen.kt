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
import space.crewpulse.ui.components.DayRing
import space.crewpulse.ui.components.MissionTrajectory
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
    // Where the mission is: animated route with the ship at today's position.
    val envId = state.environment?.id
    val planned = PLANNED_DAYS[envId] ?: 0
    MissionTrajectory(
        environmentId = envId,
        progress = if (planned > 0) state.missionDay.toFloat() / planned else 0f,
        caption = if ((state.commDelayMin ?: 0.0) < 1.0) {
            stringResource(R.string.home_subtitle_live, environmentName(envId, state.environmentName), state.missionDay)
        } else {
            stringResource(
                R.string.home_subtitle,
                environmentName(envId, state.environmentName),
                state.missionDay,
                "%.0f".format(state.commDelayMin ?: 0.0),
            )
        },
        details = if (planned > 0 && envId == "mars_transit") stringResource(R.string.mission_details_transit, state.missionDay, planned, state.commDelayMin ?: 0.0)
        else stringResource(R.string.mission_details_other, state.commDelayMin ?: 0.0),
    )

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
    val plan = state.report.plan
    val (done, total) = todayProgress(state)
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.todays_plan), style = MaterialTheme.typography.titleLarge)
            Text(
                if (total > 0 && done >= total) stringResource(R.string.day_done) else stringResource(R.string.day_progress, done, total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DayRing(done, total)
    }
    if (plan == null || plan.next24h.isEmpty()) {
        Text(stringResource(R.string.all_done), color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        val time = DateFormat.getTimeInstance(DateFormat.SHORT)
        val dayEnd = startOfToday() + DAY_MS
        plan.next24h.forEach { task ->
            val ind = state.evaluation(task.indicatorId)?.indicator
            val doneToday = state.history[task.indicatorId].orEmpty().any { it.timestampMillis >= startOfToday() }
            val at = time.format(Date(task.dueAtMillis))
            CardRow(
                title = ind?.let { indicatorName(it) } ?: task.indicatorName,
                subtitle = when {
                    doneToday && task.dueAtMillis >= dayEnd -> stringResource(R.string.done_today_next, at)
                    task.dueAtMillis >= dayEnd -> stringResource(R.string.tomorrow_at, at)
                    else -> at
                },
                icon = CrewIcons.indicator(task.indicatorId),
                trailing = if (doneToday) ({ StatusMark(Status.GREEN) }) else null,
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

/** Typical durations, for the route picture only (a Mars transfer usually takes 6 to 9 months). */
private val PLANNED_DAYS = mapOf("mars_transit" to 260, "iss" to 180)

/** Scheduled measurements done today vs due today (or already done today). */
private fun todayProgress(state: HomeUiState): Pair<Int, Int> {
    val plan = state.report.plan ?: return 0 to 0
    val dayStart = startOfToday()
    val dayEnd = dayStart + DAY_MS
    val tasks = plan.next24h + plan.later
    val measured = tasks.map { it.indicatorId }.filter { id -> state.history[id].orEmpty().any { it.timestampMillis >= dayStart } }.toSet()
    val dueToday = tasks.filter { it.dueAtMillis < dayEnd }.map { it.indicatorId }.toSet()
    val all = measured + dueToday
    return measured.size to all.size
}

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
