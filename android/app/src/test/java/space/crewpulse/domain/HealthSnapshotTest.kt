package space.crewpulse.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import space.crewpulse.data.model.Band
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.Status

class HealthSnapshotTest {

    private fun indicator(id: String, system: String) = Indicator(
        id = id, name = id, unit = "", system = system,
        bands = listOf(Band(Status.GREEN, max = 100.0), Band(Status.RED, min = 100.0)),
        howToMeasure = "", whySpace = "", actions = emptyMap(), sources = emptyList(),
    )

    @Test
    fun systemTakesWorstIndicatorAndSkipsMissingValues() {
        val indicators = listOf(
            indicator("sbp", "cardiovascular"),
            indicator("hr", "cardiovascular"),
            indicator("glucose", "metabolic"),
        )
        val result = HealthSnapshot.build(indicators, latest = mapOf("sbp" to 50.0, "hr" to 150.0), baselines = emptyMap())

        assertEquals(1, result.size)
        assertEquals("cardiovascular", result[0].systemId)
        assertEquals(Status.RED, result[0].status)
        assertEquals(2, result[0].evaluations.size)
    }

    @Test
    fun systemsFollowBodyFigureOrder() {
        val indicators = listOf(indicator("mood", "behavioral"), indicator("hr", "cardiovascular"))
        val result = HealthSnapshot.build(indicators, mapOf("mood" to 1.0, "hr" to 1.0), emptyMap())
        assertEquals(listOf("cardiovascular", "behavioral"), result.map { it.systemId })
    }
}
