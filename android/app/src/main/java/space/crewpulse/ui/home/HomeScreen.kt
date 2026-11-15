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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import space.crewpulse.ui.components.TabHeader
import space.crewpulse.ui.mission.Commendations
import space.crewpulse.ui.mission.GameSummary
import space.crewpulse.ui.i18n.alertText
import space.crewpulse.ui.i18n.environmentName
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.i18n.responderLabel
import space.crewpulse.ui.navigation.Routes
import space.crewpulse.ui.theme.StatusColors

/**
 * Daily Operations: mission position, gamification, one health status line and today's protocol.
 * Measurements live in the Physiology tab.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onIndicator: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onReminder: () -> Boolean,
    userName: String? = null,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TabHeader(
            title = stringResource(R.string.title_operations),
            subtitle = userName?.let { "${greeting()}, $it" } ?: greeting(),
        )

        when {
            state.loading -> Text(stringResource(R.string.loading))
            state.error != null -> Text(stringResource(R.string.data_error, state.error), color = MaterialTheme.colorScheme.error)
            else -> Briefing(state, onIndicator, onNavigate, onReminder)
        }
    }
}

@Composable
private fun Briefing(state: HomeUiState, onIndicator: (String) -> Unit, onNavigate: (String) -> Unit, onReminder: () -> Boolean) {
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

    // Gamification first: level, points, continuity.
    state.report.game?.let { GameSummary(it) }

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
    if (plan != null) {
        PlanByPartOfDay(state, onNavigate)
        ReminderRow(plan, onReminder)
    }

    state.report.game?.let { Commendations(it) }

    Text(
        stringResource(R.string.demo_data_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
    )
}

/** Time saved by the adaptive schedule, and a button that sends the next reminder now (demo). */
@Composable
private fun ReminderRow(plan: space.crewpulse.domain.MeasurementPlan, onReminder: () -> Boolean) {
    var result by remember { mutableStateOf<Boolean?>(null) }
    Text(
        stringResource(R.string.minutes_saved, plan.minutesNext24h, plan.minutesSaved),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    OutlinedButton(onClick = { result = onReminder() }) {
        Icon(Icons.Outlined.NotificationsNone, contentDescription = null)
        Text(stringResource(R.string.send_reminder), Modifier.padding(start = 8.dp))
    }
    result?.let {
        Text(stringResource(if (it) R.string.reminder_sent else R.string.reminder_none), style = MaterialTheme.typography.bodyMedium)
    }
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

/** Today's scheduled measurements: done today, and still to do today (overdue included). */
private data class TodayPlan(
    val done: List<space.crewpulse.domain.MeasurementTask>,
    val remaining: List<space.crewpulse.domain.MeasurementTask>,
    val next: space.crewpulse.domain.MeasurementTask?,
)

private fun todayPlan(state: HomeUiState): TodayPlan {
    val plan = state.report.plan ?: return TodayPlan(emptyList(), emptyList(), null)
    val dayStart = startOfToday()
    val dayEnd = dayStart + DAY_MS
    val tasks = (plan.next24h + plan.later).sortedBy { it.dueAtMillis }
    val doneIds = tasks.map { it.indicatorId }.filter { id -> state.history[id].orEmpty().any { it.timestampMillis >= dayStart } }.toSet()
    val done = tasks.filter { it.indicatorId in doneIds }
    val remaining = tasks.filter { it.indicatorId !in doneIds && it.dueAtMillis < dayEnd }
    val next = tasks.firstOrNull { it.dueAtMillis >= dayEnd }
    return TodayPlan(done, remaining, next)
}

private fun todayProgress(state: HomeUiState): Pair<Int, Int> {
    val p = todayPlan(state)
    return p.done.size to (p.done.size + p.remaining.size)
}

private enum class PartOfDay { MORNING, DAY, EVENING }

private fun partOf(millis: Long): PartOfDay = when (Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)) {
    in 0..11 -> PartOfDay.MORNING
    in 12..16 -> PartOfDay.DAY
    else -> PartOfDay.EVENING
}

/** Plan grouped into morning / day / evening; done items collapse into one row. */
@Composable
private fun PlanByPartOfDay(state: HomeUiState, onNavigate: (String) -> Unit) {
    val p = todayPlan(state)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT)
    val now = System.currentTimeMillis()

    p.remaining.groupBy { partOf(it.dueAtMillis) }.toSortedMap().forEach { (part, tasks) ->
        Text(
            stringResource(
                when (part) {
                    PartOfDay.MORNING -> R.string.part_morning
                    PartOfDay.DAY -> R.string.part_day
                    PartOfDay.EVENING -> R.string.part_evening
                },
            ),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
        tasks.forEach { task ->
            val ind = state.evaluation(task.indicatorId)?.indicator
            val at = time.format(Date(task.dueAtMillis))
            val overdue = task.dueAtMillis < now - 30 * 60 * 1000
            CardRow(
                title = ind?.let { indicatorName(it) } ?: indicatorName(task.indicatorId, task.indicatorName),
                subtitle = if (overdue) stringResource(R.string.overdue_at, at) else at,
                icon = CrewIcons.indicator(task.indicatorId),
                iconTint = if (overdue) StatusColors.yellow else MaterialTheme.colorScheme.primary,
                onClick = {
                    onNavigate(
                        when (task.indicatorId) {
                            "pvt_mean_rt" -> Routes.PVT
                            "who5" -> Routes.WHO5
                            else -> Routes.CHECKIN
                        },
                    )
                },
            )
        }
    }

    if (p.done.isNotEmpty()) {
        CardRow(
            title = stringResource(R.string.done_today_count, p.done.size),
            subtitle = p.done.map { t -> state.evaluation(t.indicatorId)?.indicator?.let { indicatorName(it) } ?: indicatorName(t.indicatorId, t.indicatorName) }.joinToString(" · "),
            leading = { StatusMark(Status.GREEN) },
            onClick = { onNavigate(Routes.HEALTH) },
        )
    }
    if (p.remaining.isEmpty()) {
        p.next?.let {
            Text(
                stringResource(R.string.next_tomorrow, time.format(Date(it.dueAtMillis))),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
