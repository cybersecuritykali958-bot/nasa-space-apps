package space.crewpulse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import space.crewpulse.data.model.Band
import space.crewpulse.data.model.DeviationRule
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.Status

class StatusEngineTest {

    private val heartRate = Indicator(
        id = "heart_rate",
        name = "Resting heart rate",
        unit = "bpm",
        system = "cardiovascular",
        bands = listOf(
            Band(Status.RED, max = 40.0),
            Band(Status.YELLOW, min = 40.0, max = 50.0),
            Band(Status.GREEN, min = 50.0, max = 100.0),
            Band(Status.YELLOW, min = 100.0, max = 120.0),
            Band(Status.RED, min = 120.0),
        ),
        baselineDeviationPct = DeviationRule(yellow = 15.0, red = 30.0),
        howToMeasure = "",
        whySpace = "",
        actions = emptyMap(),
        sources = emptyList(),
    )

    @Test
    fun rangeBoundsAreMinInclusiveMaxExclusive() {
        assertEquals(Status.GREEN, StatusEngine.byRange(heartRate, 50.0))
        assertEquals(Status.YELLOW, StatusEngine.byRange(heartRate, 100.0))
        assertEquals(Status.RED, StatusEngine.byRange(heartRate, 39.9))
    }

    @Test
    fun baselineDeviationCanRaiseStatusInsideNormalRange() {
        // 80 bpm is normal for the population but +33% for someone with a 60 bpm baseline.
        val eval = StatusEngine.evaluate(heartRate, 80.0, baseline = 60.0)
        assertEquals(Status.GREEN, eval.rangeStatus)
        assertEquals(Status.RED, eval.baselineStatus)
        assertEquals(Status.RED, eval.status)
    }

    @Test
    fun missingBaselineFallsBackToRangeOnly() {
        val eval = StatusEngine.evaluate(heartRate, 110.0, baseline = null)
        assertNull(eval.baselineStatus)
        assertEquals(Status.YELLOW, eval.status)
    }

    @Test
    fun worstPicksHighestSeverity() {
        assertEquals(Status.RED, StatusEngine.worst(listOf(Status.GREEN, Status.RED, Status.YELLOW)))
        assertEquals(Status.UNKNOWN, StatusEngine.worst(emptyList()))
    }
}
