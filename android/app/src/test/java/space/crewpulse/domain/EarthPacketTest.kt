package space.crewpulse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.model.Status

class EarthPacketTest {

    private val yellow = Evaluation(TestFixtures.systolic, 135.0, Status.YELLOW, Status.YELLOW, Status.YELLOW, 12.4)
    private val green = Evaluation(TestFixtures.systolic.copy(id = "hr", name = "Heart rate", unit = "bpm"), 62.0, Status.GREEN, Status.GREEN, null, null)

    private val report = EngineReport.EMPTY.copy(
        systems = listOf(SystemStatus("cardiovascular", Status.YELLOW, listOf(yellow, green))),
        triage = TriageDecision(Status.YELLOW, Responder.SELF, "Follow the actions", "One system needs attention.", 34.0, 1440.0),
    )

    @Test
    fun listsOnlyOffNominalValuesInPlainAscii() {
        val text = EarthPacket.build(report, "Alex Rivera", "CP-0001", 180, "Mars transit", 0L)
        assertTrue(text.contains("- Systolic BP: 135.0 mmHg YELLOW, +12% vs baseline"))
        assertFalse(text.contains("Heart rate:"))
        assertTrue(text.contains("Crew: Alex Rivera (CP-0001)"))
        assertTrue(text.contains("Generated 1970-01-01 00:00 UTC"))
        assertTrue(text.contains("Measured 2 indicators"))
        assertTrue(text.all { it.code < 128 })
    }

    @Test
    fun detectorsAgreeingOnOneTrendGiveOneLine() {
        val alerts = listOf(
            Alert("systolic_bp", AlertKind.EWMA_SHIFT, Direction.UP, 0L, daysToThreshold = 9.0, message = ""),
            Alert("systolic_bp", AlertKind.CUSUM_DRIFT, Direction.UP, 0L, daysToThreshold = 7.0, message = ""),
        )
        val text = EarthPacket.build(report.copy(alerts = alerts), null, null, 1, "ISS", 0L)
        assertTrue(text.contains("Early warnings (1):"))
        assertTrue(text.contains("- Systolic BP: trend UP, threshold in ~7 days"))
    }

    @Test
    fun timingUsesTriageRoundTrip() {
        val t = EarthPacket.timing(report, 12.0)!!
        assertEquals(12.0, t.oneWayMin, 1e-9)
        assertEquals(34.0, t.earliestReplyMin!!, 1e-9)
    }
}
