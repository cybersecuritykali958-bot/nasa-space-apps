package space.crewpulse.domain

import space.crewpulse.data.model.DemoMission
import space.crewpulse.data.model.EngineRules
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.Status

/**
 * Replays a mission day by day, as if the engine ran every morning, and measures how well
 * early warning works against what actually happened later:
 *
 * - an "event" is a run of non-green days lasting at least [persistDays];
 * - it is "caught" if an alert run was active as the event arrived;
 * - lead time is the number of days from the start of that run to the event (0 if the run started
 *   more than [lookbackDays] earlier, e.g. left over from a previous event);
 * - alerts are debounced like events: a run must last [persistDays] days, runs closer than
 *   [mergeGapDays] are merged;
 * - a "false alarm" is an alert run with no event within [lookbackDays] after it started.
 *
 * Sudden events (one bad night) cannot be predicted and show up with lead time 0. That is expected
 * and reported honestly rather than hidden.
 */
object EngineBenchmark {

    data class IndicatorResult(
        val indicatorId: String,
        val events: Int,
        val caught: Int,
        val leadTimesDays: List<Int>,
        val falseAlarms: Int,
    )

    data class Report(
        val days: Int,
        val results: List<IndicatorResult>,
    ) {
        val events = results.sumOf { it.events }
        val caught = results.sumOf { it.caught }
        val earlyCatches = results.sumOf { r -> r.leadTimesDays.count { it > 0 } }
        val meanLeadDays: Double = results.flatMap { r -> r.leadTimesDays.filter { it > 0 } }.average().let { if (it.isNaN()) 0.0 else it }
        val falseAlarms = results.sumOf { it.falseAlarms }

        fun summary(): String =
            "Replayed $days days: caught $caught of $events events, $earlyCatches before the threshold " +
                "(mean lead ${"%.1f".format(meanLeadDays)} days), $falseAlarms false alarm(s)."
    }

    fun run(
        indicators: List<Indicator>,
        mission: DemoMission,
        rules: EngineRules,
        persistDays: Int = 2,
        lookbackDays: Int = 30,
        mergeGapDays: Int = 2,
    ): Report {
        val days = mission.days.sortedBy { it.day }
        val results = indicators.mapNotNull { ind ->
            val series = days.mapNotNull { d -> d.readings[ind.id]?.let { d.day to it } }
            if (series.size < rules.earlyWarning.minHistory) return@mapNotNull null
            val baseline = mission.baseline[ind.id]

            // Day-by-day replay: alert state and status as the engine would have seen them.
            val alertOn = BooleanArray(series.size)
            val bad = BooleanArray(series.size)
            for (i in series.indices) {
                val history = series.take(i + 1).map { (day, v) -> Sample(day * DAY_MILLIS, v, "demo") }
                alertOn[i] = EarlyWarning.analyze(ind, history, baseline, rules.earlyWarning).isNotEmpty()
                bad[i] = StatusEngine.evaluate(ind, series[i].second, baseline).status.let {
                    it == Status.YELLOW || it == Status.RED
                }
            }

            // Events are debounced the same way as alerts, so a value hovering at the threshold is one event.
            val eventIdx = alarmRuns(bad, persistDays, mergeGapDays).map { it.first }
            val runs = alarmRuns(alertOn, persistDays, mergeGapDays)

            val leads = mutableListOf<Int>()
            for (e in eventIdx) {
                // The warning that was active as the event approached.
                val run = runs.lastOrNull { it.first <= e && it.last >= e - mergeGapDays } ?: continue
                val earliestCounted = (e - lookbackDays).coerceAtLeast(0)
                // A run that started long before (e.g. left over from an earlier event) earns no lead time.
                leads += if (run.first >= earliestCounted) series[e].first - series[run.first].first else 0
            }

            val falseAlarms = runs.count { run ->
                !bad[run.first] && eventIdx.none { e -> series[e].first - series[run.first].first in 0..lookbackDays }
            }
            IndicatorResult(ind.id, eventIdx.size, leads.size, leads, falseAlarms)
        }
        return Report(days.size, results)
    }

    /** Index ranges where the alert was on, merged across short gaps, keeping runs of at least [minLength]. */
    internal fun alarmRuns(on: BooleanArray, minLength: Int, mergeGap: Int): List<IntRange> {
        val raw = mutableListOf<IntRange>()
        var i = 0
        while (i < on.size) {
            if (!on[i]) { i++; continue }
            val start = i
            while (i < on.size && on[i]) i++
            raw += start until i
        }
        val merged = mutableListOf<IntRange>()
        for (r in raw) {
            val prev = merged.lastOrNull()
            if (prev != null && r.first - prev.last - 1 <= mergeGap) {
                merged[merged.lastIndex] = prev.first..r.last
            } else {
                merged += r
            }
        }
        return merged.filter { it.last - it.first + 1 >= minLength }
    }
}
