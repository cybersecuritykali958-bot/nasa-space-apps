package space.crewpulse.domain

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.model.GamificationConfig
import space.crewpulse.data.model.ScheduleConfig
import space.crewpulse.data.model.SpaceWeatherConfig

class PlannerGameWeatherTest {

    private val hour = 3_600_000L

    @Test
    fun plannerMeasuresProblemsSoonAndStableThingsRarely() {
        val ind = TestFixtures.systolic
        val glucose = ind.copy(id = "glucose", name = "Glucose")
        val history = mapOf(
            "systolic_bp" to listOf(Sample(0, 150.0, "manual")), // red
            "glucose" to listOf(Sample(0, 100.0, "manual")), // green, weekly
        )
        val report = HealthEngine.analyze(
            EngineInput(listOf(ind, glucose), history, emptyMap(), null, TestFixtures.rules, nowMillis = 0),
        )
        val plan = MeasurementPlanner.plan(
            listOf(ind, glucose), history, report, 0,
            ScheduleConfig(intervalDays = mapOf("systolic_bp" to 3.0, "glucose" to 7.0), minutesPerMeasurement = mapOf("systolic_bp" to 3.0, "glucose" to 5.0)),
        )
        assertEquals(listOf("systolic_bp"), plan.next24h.map { it.indicatorId })
        assertEquals(TaskPriority.HIGH, plan.next24h.single().priority)
        assertEquals(1 * hour, plan.next24h.single().dueAtMillis)
        assertEquals(listOf("glucose"), plan.later.map { it.indicatorId })
        assertEquals(5.0, plan.minutesSaved, 1e-9)
    }

    @Test
    fun streaksAndBadgesComeFromHistory() {
        val start = LocalDate.of(2026, 11, 1)
        fun ms(d: LocalDate) = d.atTime(8, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        // 7 consecutive days, a gap, then 2 days ending today.
        val days = (0L..6L).map { start.plusDays(it) } + listOf(start.plusDays(9), start.plusDays(10))
        val history = mapOf(
            "mood" to days.map { Sample(ms(it), 4.0, "manual") },
            "sleep_hours" to days.map { Sample(ms(it), 7.5, "manual") },
        )
        val plan = MeasurementPlan(emptyList(), emptyList(), 0.0, 0.0)
        val game = Gamification.compute(
            history, plan, ms(start.plusDays(10)), ZoneOffset.UTC,
            GamificationConfig(coreIndicators = listOf("mood", "sleep_hours"), pointsPerLevel = 100, levelTitles = listOf("Cadet", "Specialist")),
        )
        assertEquals(2, game.currentStreakDays)
        assertEquals(7, game.bestStreakDays)
        assertTrue("7-day streak" in game.badges && "Full check-in" in game.badges)
        // 9 days x 10 + 18 measurements x 2 + 9 full days x 20 = 306
        assertEquals(306, game.points)
        assertEquals("Specialist", game.levelTitle)
    }

    @Test
    fun recentParticleEventMeansShelterAndNoGoForSpacewalk() {
        val now = 100 * hour
        val events = listOf(SpaceWeatherEvent(SpaceWeatherType.SEP, "sep1", now - 5 * hour, "SEP"))
        val status = SpaceWeather.assess(events, now, SpaceWeatherConfig(), "bundled", null)
        assertEquals(SpaceWeatherLevel.WARNING, status.level)

        val eva = Readiness.check(
            space.crewpulse.data.model.ReadinessRule("eva", "EVA", emptyList(), spaceWeatherGate = true),
            emptyMap(), emptyList(), status,
        )
        assertEquals(Verdict.NO_GO, eva.verdict)

        val old = SpaceWeather.assess(events, now + 100 * hour, SpaceWeatherConfig(), "bundled", null)
        assertEquals(SpaceWeatherLevel.QUIET, old.level)
    }

    @Test
    fun radiationBudgetCountsDaysToCareerLimit() {
        val b = SpaceWeather.budget(361.0, TestFixtures.env("mars", 12.0).copy(radiationMsvPerDay = 1.84), SpaceWeatherConfig())!!
        assertEquals(239.0, b.remainingMsv, 1e-9)
        assertEquals(239.0 / 1.84, b.daysLeft!!, 1e-9)
    }
}
