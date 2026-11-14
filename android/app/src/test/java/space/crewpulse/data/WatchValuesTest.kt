package space.crewpulse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.ui.checkin.CheckInViewModel
import space.crewpulse.ui.watch.ecgSample

class WatchValuesTest {

    @Test
    fun watchTakesOnlyWatchMeasurableIndicators() {
        val day = mapOf(
            "systolic_bp" to 129.5, "heart_rate" to 71.5, "glucose" to 84.0,
            "body_temp" to 36.8, "sleep_hours" to 7.6, "mood" to 4.0,
        )
        assertEquals(mapOf("heart_rate" to 71.5, "sleep_hours" to 7.6, "body_temp" to 36.8), watchValues(day))
    }

    @Test
    fun missingOrInvalidValuesStayManual() {
        val values = watchValues(mapOf("heart_rate" to Double.NaN, "sleep_hours" to 7.0))
        assertEquals(mapOf("sleep_hours" to 7.0), values)
    }

    @Test
    fun heartRateIsShownInWholeBeats() {
        assertEquals("72", CheckInViewModel.formatWatchValue("heart_rate", 71.5))
        assertEquals("36.8", CheckInViewModel.formatWatchValue("body_temp", 36.8))
        assertEquals("7.0", CheckInViewModel.formatWatchValue("sleep_hours", 7.0))
    }

    @Test
    fun ecgPeaksAtTheRWave() {
        val peak = ecgSample(0.39f)
        assertTrue((0 until 100).all { ecgSample(it / 100f) <= peak })
    }
}
