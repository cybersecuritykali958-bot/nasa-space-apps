package space.crewpulse.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import space.crewpulse.data.model.GamificationConfig

data class GameState(
    val points: Int,
    val level: Int,
    val levelTitle: String,
    val pointsToNextLevel: Int,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val badges: List<String>,
    /** Today's tasks from the measurement plan, phrased as a mission. */
    val dailyMission: String,
    val dailyMissionPoints: Int,
)

/**
 * Engagement layer computed from the measurement history, so it needs no extra storage
 * and cannot drift out of sync with the real data.
 */
object Gamification {

    fun compute(
        history: Map<String, List<Sample>>,
        plan: MeasurementPlan,
        nowMillis: Long,
        zone: ZoneId,
        config: GamificationConfig,
    ): GameState {
        val all = history.values.flatten()
        fun dayOf(ts: Long): LocalDate = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()

        // Which indicators were measured on each day.
        val measuredByDay: Map<LocalDate, Set<String>> = history
            .flatMap { (id, samples) -> samples.map { dayOf(it.timestampMillis) to id } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
        val days = measuredByDay.keys.sorted()

        val fullDays = if (config.coreIndicators.isEmpty()) 0
        else measuredByDay.values.count { it.containsAll(config.coreIndicators) }
        val points = days.size * config.pointsPerCheckInDay +
            all.size * config.pointsPerMeasurement +
            fullDays * config.fullCheckInBonus

        val levelIndex = points / config.pointsPerLevel
        val title = config.levelTitles.getOrElse(levelIndex) { config.levelTitles.lastOrNull() ?: "Crew" }

        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val current = streakEndingAt(days.toSet(), today).takeIf { it > 0 }
            ?: streakEndingAt(days.toSet(), today.minusDays(1))
        val best = bestStreak(days)

        val badges = buildList {
            if (best >= 7) add("7-day streak")
            if (best >= 30) add("30-day streak")
            if (best >= 100) add("100-day streak")
            if (fullDays > 0) add("Full check-in")
            if (history["pvt_mean_rt"].orEmpty().isNotEmpty()) add("Reaction tester")
        }

        val tasks = plan.next24h
        val missionPoints = tasks.size * config.pointsPerMeasurement + if (tasks.isNotEmpty()) config.pointsPerCheckInDay else 0
        val mission = if (tasks.isEmpty()) {
            "All set for today. Keep the streak tomorrow."
        } else {
            "Today's mission: ${tasks.joinToString { it.indicatorName }} (~${"%.0f".format(plan.minutesNext24h)} min)."
        }

        return GameState(
            points = points,
            level = levelIndex + 1,
            levelTitle = title,
            pointsToNextLevel = config.pointsPerLevel - points % config.pointsPerLevel,
            currentStreakDays = current,
            bestStreakDays = best,
            badges = badges,
            dailyMission = mission,
            dailyMissionPoints = missionPoints,
        )
    }

    internal fun streakEndingAt(days: Set<LocalDate>, end: LocalDate): Int {
        var n = 0
        var d = end
        while (d in days) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    internal fun bestStreak(sortedDays: List<LocalDate>): Int {
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in sortedDays) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }
}
