package space.crewpulse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BaselineCalculatorTest {

    @Test
    fun usesMeanOfFirstThreeReadingsOnly() {
        val result = BaselineCalculator.fromFirstReadings(mapOf("heart_rate" to listOf(60.0, 62.0, 64.0, 120.0)))
        assertEquals(62.0, result.getValue("heart_rate"), 1e-9)
    }

    @Test
    fun noBaselineUntilEnoughReadings() {
        val result = BaselineCalculator.fromFirstReadings(mapOf("glucose" to listOf(90.0, 92.0)))
        assertFalse(result.containsKey("glucose"))
    }
}
