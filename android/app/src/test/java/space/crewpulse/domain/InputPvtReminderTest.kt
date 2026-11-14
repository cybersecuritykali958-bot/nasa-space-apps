package space.crewpulse.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.model.InputConfig
import space.crewpulse.data.model.ValueRange

class InputPvtReminderTest {

    private val indicators = listOf(
        TestFixtures.systolic,
        TestFixtures.systolic.copy(id = "diastolic_bp", name = "Diastolic BP"),
        TestFixtures.systolic.copy(id = "heart_rate", name = "Heart rate", unit = "bpm"),
    )
    private val limits = InputConfig(
        mapOf(
            "systolic_bp" to ValueRange(50.0, 260.0),
            "diastolic_bp" to ValueRange(30.0, 160.0),
            "heart_rate" to ValueRange(20.0, 250.0),
        ),
    )

    @Test
    fun validValuesPassEvenWhenMedicallyRed() {
        val r = InputValidator.validate(mapOf("systolic_bp" to 175.0, "diastolic_bp" to 100.0), indicators, limits)
        assertTrue(r.isValid)
        assertEquals(2, r.accepted.size)
    }

    @Test
    fun typosUnknownIdsAndSwappedPressureAreRejected() {
        val r = InputValidator.validate(
            mapOf("heart_rate" to 900.0, "spo2" to 98.0, "systolic_bp" to 70.0, "diastolic_bp" to 120.0),
            indicators, limits,
        )
        assertFalse(r.isValid)
        assertEquals(setOf("heart_rate", "spo2", "diastolic_bp"), r.errors.keys)
        assertTrue(r.errors.getValue("heart_rate").contains("between 20 and 250"))
        assertTrue(r.accepted.isEmpty())
    }

    @Test
    fun emptyAndNonNumericInputIsRejected() {
        assertFalse(InputValidator.validate(emptyMap(), indicators, limits).isValid)
        assertFalse(InputValidator.validate(mapOf("heart_rate" to Double.NaN), indicators, limits).isValid)
    }

    @Test
    fun pvtScoresLapsesFalseStartsAndSpeed() {
        val session = PvtSession(random = Random(1))
        listOf(250L, 300L, 400L, 500L).forEach(session::recordResponse)
        session.recordResponse(50) // faster than humanly possible -> false start
        session.recordEarlyTap()
        val r = session.result()
        assertEquals(4, r.trials)
        assertEquals(362.5, r.meanReactionMillis, 1e-9)
        assertEquals(350.0, r.medianReactionMillis, 1e-9)
        assertEquals(2, r.lapses) // 400 and 500 are >= 355
        assertEquals(2, r.falseStarts)
        assertEquals((4.0 + 1000.0 / 300 + 2.5 + 2.0) / 4, r.meanSpeedPerSecond, 1e-9)
        assertFalse(r.isReliable)
    }

    @Test
    fun pvtDelaysStayInProtocolRangeAndSessionEndsAfterThreeMinutes() {
        val session = PvtSession(random = Random(7))
        repeat(500) {
            val d = session.nextDelayMillis()
            assertTrue(d in PvtSession.MIN_DELAY_MILLIS..PvtSession.MAX_DELAY_MILLIS)
        }
        assertFalse(session.isFinished(179_999))
        assertTrue(session.isFinished(180_000))
    }

    @Test
    fun reminderListsOnlyDueTasks() {
        val due = MeasurementTask("heart_rate", "Heart rate", 100, "x", TaskPriority.HIGH, 1.0)
        val later = MeasurementTask("glucose", "Glucose", 10_000, "x", TaskPriority.LOW, 5.0)
        val plan = MeasurementPlan(listOf(due), listOf(later), 1.0, 6.0)
        val msg = Reminders.message(plan, nowMillis = 200)!!
        assertEquals("Re-check needed", msg.title)
        assertTrue(msg.text.contains("Heart rate") && !msg.text.contains("Glucose"))
        assertNull(Reminders.message(plan, nowMillis = 50))
    }
}
