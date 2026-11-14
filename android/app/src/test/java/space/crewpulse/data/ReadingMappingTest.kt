package space.crewpulse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.model.DemoMission
import space.crewpulse.data.model.MissionDay

class ReadingMappingTest {

    @Test
    fun checkInSharesTimestampAndDropsInvalidValues() {
        val rows = checkInToReadings(mapOf("heart_rate" to 64.0, "glucose" to Double.NaN), atMillis = 1000L)
        assertEquals(1, rows.size)
        assertEquals("manual", rows[0].source)
        assertEquals(1000L, rows[0].timestampMillis)
    }

    @Test
    fun lastDemoDayLandsOnGivenTimestamp() {
        val mission = DemoMission(
            version = 1, astronaut = "A", environmentId = "iss", synthetic = true, baseline = emptyMap(),
            days = listOf(MissionDay(1, mapOf("hr" to 60.0)), MissionDay(3, mapOf("hr" to 70.0))),
        )
        val day = 24L * 60 * 60 * 1000
        val rows = demoReadings(mission, lastDayMillis = 10 * day).flatten()

        assertEquals(10 * day, rows.single { it.value == 70.0 }.timestampMillis)
        assertEquals(8 * day, rows.single { it.value == 60.0 }.timestampMillis)
        assertTrue(rows.all { it.source == "demo" })
    }
}
