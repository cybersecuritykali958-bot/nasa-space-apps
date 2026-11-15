package space.crewpulse.domain

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import space.crewpulse.data.model.EnvironmentsFile

/**
 * Expected values follow from the model formulas.
 * ISS 180 days: muscle 100 · exp(−k(a) · 180) ≥ 90 needs a ≥ (0.38566 − 0.10536) / (0.38566 − 0.09431) = 0.962;
 * muscle ≥ 80 needs a ≥ (0.38566 − 0.22314) / 0.29135 = 0.558; bone needs less in both cases.
 */
class TwinCoachTest {

    private val envs = Json { ignoreUnknownKeys = true }
        .decodeFromString<EnvironmentsFile>(File("src/main/assets/environments.json").readText())
        .environments.associateBy { it.id }

    private fun coach(scenario: String) =
        DigitalTwin.coach { a -> DigitalTwin.simulate(DigitalTwin.SCENARIOS.getValue(scenario), envs, a) }!!

    @Test
    fun issSixMonthsNeedsMostOfTheProtocolToStayGreen() {
        val c = coach("iss_6mo")
        assertEquals(0.97, c.forGreen!!, 1e-9)
        assertEquals(0.56, c.forNoRed!!, 1e-9)
        assertNull(c.firstRedDayAtFull)
        assertEquals(180, c.lastDay)
    }

    @Test
    fun fullMarsMissionIsNotCoveredEvenByTheFullProtocol() {
        val c = coach("mars_mission")
        assertNull(c.forGreen)
        assertNull(c.forNoRed)
        assertNotNull(c.firstRedDayAtFull)
        val atFull = DigitalTwin.simulate(DigitalTwin.SCENARIOS.getValue("mars_mission"), envs, 1.0)
        val day = c.firstRedDayAtFull!!
        assertEquals(DigitalTwin.BONE_RED_BELOW > atFull[day - 1].bonePct || DigitalTwin.MUSCLE_RED_BELOW > atFull[day - 1].musclePct, true)
    }

    @Test
    fun resultIsTheThresholdRoundedUp() {
        val sim = { a: Double -> DigitalTwin.simulate(DigitalTwin.SCENARIOS.getValue("iss_6mo"), envs, a) }
        val a = DigitalTwin.minAdherence(sim) { it.musclePct >= DigitalTwin.MUSCLE_GREEN_MIN }!!
        assertEquals(true, sim(a).last().musclePct >= DigitalTwin.MUSCLE_GREEN_MIN)
        assertEquals(true, sim(a - 0.01).last().musclePct < DigitalTwin.MUSCLE_GREEN_MIN)
    }
}
