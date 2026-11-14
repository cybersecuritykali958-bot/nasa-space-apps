package space.crewpulse.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.ScheduleConfig
import space.crewpulse.data.model.Status

enum class TaskPriority { HIGH, NORMAL, LOW }

data class MeasurementTask(
    val indicatorId: String,
    val indicatorName: String,
    val dueAtMillis: Long,
    val reason: String,
    val priority: TaskPriority,
    val minutes: Double,
)

data class MeasurementPlan(
    /** Everything due within the next 24 hours, most urgent first. */
    val next24h: List<MeasurementTask>,
    val later: List<MeasurementTask>,
    val minutesNext24h: Double,
    /** Crew time if every indicator were measured daily. */
    val minutesIfAllDaily: Double,
) {
    val minutesSaved: Double get() = (minutesIfAllDaily - minutesNext24h).coerceAtLeast(0.0)
}

/**
 * Adaptive schedule: measure often what needs attention, rarely what is stable.
 * Crew time is one of the scarcest resources on a mission, so this saves real minutes.
 */
object MeasurementPlanner {

    fun plan(
        indicators: List<Indicator>,
        history: Map<String, List<Sample>>,
        report: EngineReport,
        nowMillis: Long,
        config: ScheduleConfig,
        zone: ZoneId = ZoneId.systemDefault(),
    ): MeasurementPlan {
        val hour = 3_600_000L
        val tasks = indicators.filter { it.id in config.intervalDays }.map { ind ->
            val minutes = config.minutesPerMeasurement[ind.id] ?: 1.0
            val last = history[ind.id]?.maxByOrNull { it.timestampMillis }
            val eval = report.evaluation(ind.id)
            val alert = report.alertsFor(ind.id).firstOrNull()
            val stale = report.confidence[ind.id]?.level == ConfidenceLevel.LOW

            when {
                last == null || eval == null ->
                    MeasurementTask(ind.id, ind.name, nowMillis, "No data yet", TaskPriority.HIGH, minutes)
                eval.status == Status.RED -> MeasurementTask(
                    ind.id, ind.name, last.timestampMillis + (config.redRecheckHours * hour).toLong(),
                    "Red value: confirm with a repeat measurement", TaskPriority.HIGH, minutes,
                )
                stale -> MeasurementTask(ind.id, ind.name, nowMillis, "Last value is stale", TaskPriority.HIGH, minutes)
                eval.status == Status.YELLOW || alert != null -> MeasurementTask(
                    ind.id, ind.name, last.timestampMillis + (config.attentionIntervalDays * DAY_MILLIS).toLong(),
                    if (alert != null) "Early warning: track the trend" else "Yellow value: watch closely",
                    TaskPriority.NORMAL, minutes,
                )
                else -> MeasurementTask(
                    ind.id, ind.name, last.timestampMillis + (config.intervalDays.getValue(ind.id) * DAY_MILLIS).toLong(),
                    "Routine check, stable", TaskPriority.LOW, minutes,
                )
            }
        }.map { task ->
            // Routine and attention checks move to the measurement's natural time of day;
            // urgent ones (no data, red re-check, stale) stay as they are.
            val time = config.timeOfDay[task.indicatorId]?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            if (time == null || task.priority == TaskPriority.HIGH) task
            else task.copy(dueAtMillis = alignToTimeOfDay(task.dueAtMillis, time, zone))
        }.sortedWith(compareBy<MeasurementTask> { it.priority }.thenBy { it.dueAtMillis })

        val (soon, later) = tasks.partition { it.dueAtMillis <= nowMillis + DAY_MILLIS }
        return MeasurementPlan(
            next24h = soon,
            later = later,
            minutesNext24h = soon.sumOf { it.minutes },
            minutesIfAllDaily = tasks.sumOf { it.minutes },
        )
    }

    /** Same calendar day as [millis], at [time]; a time already passed by more than 12 h moves to the next day. */
    fun alignToTimeOfDay(millis: Long, time: LocalTime, zone: ZoneId): Long {
        val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val aligned = day.atTime(time).atZone(zone).toInstant().toEpochMilli()
        return if (aligned < millis - DAY_MILLIS / 2) aligned + DAY_MILLIS else aligned
    }
}
