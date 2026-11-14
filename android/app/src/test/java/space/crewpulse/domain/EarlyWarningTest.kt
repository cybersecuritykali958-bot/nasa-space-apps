package space.crewpulse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.model.Status
import space.crewpulse.domain.TestFixtures.daily
import space.crewpulse.domain.TestFixtures.noise
import space.crewpulse.domain.TestFixtures.systolic

class EarlyWarningTest {

    @Test
    fun stableNoisyValuesRaiseNoAlert() {
        val history = daily(List(40) { 118.0 + noise(it) })
        assertTrue(EarlyWarning.analyze(systolic, history, 118.0, TestFixtures.ewConfig).isEmpty())
    }

    @Test
    fun slowDriftIsCaughtWhileStillInRange() {
        // 20 stable days, then +0.4 mmHg/day: still green (< 130) at the end.
        val history = daily(List(45) { i -> 118.0 + noise(i) + if (i >= 20) 0.4 * (i - 20) else 0.0 })
        assertEquals(Status.GREEN, StatusEngine.byRange(systolic, history.last().value))

        val alerts = EarlyWarning.analyze(systolic, history, 118.0, TestFixtures.ewConfig)
        assertTrue("expected a drift alert, got $alerts", alerts.any { it.kind != AlertKind.TREND_TO_THRESHOLD })
        assertTrue(alerts.all { it.direction == Direction.UP })
    }

    @Test
    fun trendForecastsDaysToThreshold() {
        val history = daily(List(14) { i -> 120.0 + 0.5 * i })
        val alert = EarlyWarning.trend(systolic, history, TestFixtures.ewConfig)
        assertNotNull(alert)
        alert!!
        assertEquals(130.0, alert.thresholdValue!!, 1e-9)
        assertEquals(Status.YELLOW, alert.thresholdStatus)
        // Latest value 126.5, slope 0.5/day -> 7 days.
        assertEquals(7.0, alert.daysToThreshold!!, 1e-6)
    }

    @Test
    fun tooLittleHistoryIsSkipped() {
        assertTrue(EarlyWarning.analyze(systolic, daily(listOf(150.0, 160.0, 170.0)), null, TestFixtures.ewConfig).isEmpty())
    }

    @Test
    fun boundaryLookupGoesToNextWorseZone() {
        assertEquals(130.0 to Status.YELLOW, EarlyWarning.nextWorseBoundary(systolic, 120.0, Direction.UP))
        assertEquals(90.0 to Status.RED, EarlyWarning.nextWorseBoundary(systolic, 120.0, Direction.DOWN))
        assertEquals(140.0 to Status.RED, EarlyWarning.nextWorseBoundary(systolic, 135.0, Direction.UP))
    }

    @Test
    fun shortSpikeDoesNotLeaveAStaleDriftAlert() {
        // 30 normal days, 3 very high days, then 20 normal days again.
        val values = List(30) { 118.0 + noise(it) } + List(3) { 160.0 } + List(20) { 118.0 + noise(it) }
        val alerts = EarlyWarning.analyze(systolic, daily(values), 118.0, TestFixtures.ewConfig)
        assertTrue("stale alerts: $alerts", alerts.none { it.kind == AlertKind.CUSUM_DRIFT })
    }
}
