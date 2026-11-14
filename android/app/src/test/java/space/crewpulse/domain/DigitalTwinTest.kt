package space.crewpulse.domain

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.model.EnvironmentsFile
import space.crewpulse.data.model.Status

/** Test vectors printed by `python science/digital_twin.py` (end of each scenario). */
class DigitalTwinTest {

    private val envs = Json { ignoreUnknownKeys = true }
        .decodeFromString<EnvironmentsFile>(File("src/main/assets/environments.json").readText())
        .environments.associateBy { it.id }

    private fun end(scenario: String, adherence: Double) =
        DigitalTwin.simulate(DigitalTwin.SCENARIOS.getValue(scenario), envs, adherence).last()

    private fun check(
        scenario: String, adherence: Double, day: Int,
        bone: Double, boneStatus: Status, muscle: Double, muscleStatus: Status,
        dose: Double, doseStatus: Status, reid: Double, extrapolated: Boolean,
    ) {
        val s = end(scenario, adherence)
        val tag = "$scenario a=$adherence"
        assertEquals(tag, day, s.day)
        assertEquals(tag, bone, s.bonePct, 0.005)
        assertEquals(tag, boneStatus, s.boneStatus)
        assertEquals(tag, muscle, s.musclePct, 0.005)
        assertEquals(tag, muscleStatus, s.muscleStatus)
        assertEquals(tag, dose, s.doseMsv, 0.05)
        assertEquals(tag, doseStatus, s.doseStatus)
        assertEquals(tag, reid, s.reidPct, 0.005)
        assertEquals(tag, extrapolated, s.extrapolated)
    }

    @Test
    fun matchesPythonReferenceVectors() {
        check("iss_6mo", 1.0, 180, 97.04, Status.GREEN, 91.00, Status.GREEN, 90.0, Status.GREEN, 0.45, false)
        check("iss_6mo", 0.5, 180, 94.83, Status.YELLOW, 78.66, Status.RED, 90.0, Status.GREEN, 0.45, false)
        check("iss_6mo", 0.0, 180, 92.61, Status.YELLOW, 68.00, Status.RED, 90.0, Status.GREEN, 0.45, false)
        check("mars_transit_180", 1.0, 180, 97.04, Status.GREEN, 91.00, Status.GREEN, 331.2, Status.YELLOW, 1.66, false)
        check("mars_mission", 1.0, 860, 88.99, Status.RED, 70.40, Status.RED, 982.4, Status.RED, 4.91, true)
        check("mars_mission", 0.0, 860, 72.49, Status.RED, 23.80, Status.RED, 982.4, Status.RED, 4.91, true)
        check("two_years_transit", 1.0, 730, 88.01, Status.RED, 68.22, Status.RED, 1343.2, Status.RED, 6.72, true)
        check("two_years_transit", 0.5, 730, 79.02, Status.RED, 37.78, Status.RED, 1343.2, Status.RED, 6.72, true)
    }

    @Test
    fun daysToLimitAndRecoveryMatchReference() {
        assertEquals(1020.0, end("iss_6mo", 1.0).daysToDoseLimit!!, 0.5)
        assertEquals(146.0, end("mars_transit_180", 1.0).daysToDoseLimit!!, 0.5)
        assertEquals(0.0, end("mars_mission", 1.0).daysToDoseLimit!!, 1e-9)

        val landing = end("iss_6mo", 1.0).bonePct
        assertEquals(97.04, DigitalTwin.boneAfterLanding(landing, 0.0), 0.005)
        assertEquals(98.52, DigitalTwin.boneAfterLanding(landing, 255.0), 0.005)
        assertEquals(98.90, DigitalTwin.boneAfterLanding(landing, 365.0), 0.005)
        assertEquals(99.85, DigitalTwin.boneAfterLanding(landing, 1095.0), 0.005)
    }

    @Test
    fun solarEventAddsDoseOnItsDayOnly() {
        val base = DigitalTwin.simulate(listOf(DigitalTwin.Phase("iss", 10)), envs)
        val withEvent = DigitalTwin.simulate(listOf(DigitalTwin.Phase("iss", 10)), envs, extraDoseByDay = mapOf(5 to 30.0))
        assertEquals(base[3].doseMsv, withEvent[3].doseMsv, 1e-9)
        assertEquals(base[4].doseMsv + 30.0, withEvent[4].doseMsv, 1e-9)
        assertFalse(withEvent.last().extrapolated)
        assertTrue(withEvent.all { it.bonePct < 100.0 })
    }
}
