package space.crewpulse.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
import space.crewpulse.data.demoReadingsAtTimes
import space.crewpulse.data.model.DemoMission
import space.crewpulse.data.model.MissionDay

class TimeOfDayTest {

    private val zone = ZoneOffset.UTC
    private fun at(date: LocalDate, h: Int, m: Int = 0) = date.atTime(h, m).toInstant(zone).toEpochMilli()

    @Test
    fun routineTaskMovesToItsTimeOfDay() {
        val d = LocalDate.of(2026, 11, 15)
        // Measured yesterday 08:00 + 1 day = today 08:00 -> the PVT belongs at 13:00 today.
        assertEquals(at(d, 13), MeasurementPlanner.alignToTimeOfDay(at(d, 8), LocalTime.of(13, 0), zone))
        // Mood measured last night 21:00 + 1 day -> tonight 21:00.
        assertEquals(at(d, 21), MeasurementPlanner.alignToTimeOfDay(at(d, 21), LocalTime.of(21, 0), zone))
        // A time already passed by more than 12 h goes to the next day.
        assertEquals(at(d.plusDays(1), 7), MeasurementPlanner.alignToTimeOfDay(at(d, 21), LocalTime.of(7, 0), zone))
    }

    @Test
    fun demoReadingsGetNaturalTimesAndDates() {
        val mission = DemoMission(
            version = 1, astronaut = "A", environmentId = "iss", synthetic = true, baseline = emptyMap(),
            days = listOf(MissionDay(1, mapOf("heart_rate" to 60.0, "mood" to 4.0)), MissionDay(2, mapOf("heart_rate" to 61.0))),
        )
        val last = LocalDate.of(2026, 11, 15)
        val rows = demoReadingsAtTimes(mission, last, mapOf("heart_rate" to "07:00", "mood" to "21:00"), zone)
        assertEquals(at(last.minusDays(1), 7), rows.single { it.value == 60.0 }.timestampMillis)
        assertEquals(at(last.minusDays(1), 21), rows.single { it.indicatorId == "mood" }.timestampMillis)
        assertEquals(at(last, 7), rows.single { it.value == 61.0 }.timestampMillis)
    }
}
