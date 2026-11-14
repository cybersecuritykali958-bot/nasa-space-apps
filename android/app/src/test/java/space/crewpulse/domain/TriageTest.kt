package space.crewpulse.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import space.crewpulse.data.model.Status
import space.crewpulse.domain.TestFixtures.env
import space.crewpulse.domain.TestFixtures.triageConfig

class TriageTest {

    private fun systems(vararg statuses: Status) =
        statuses.mapIndexed { i, s -> SystemStatus("s$i", s, emptyList()) }

    @Test
    fun redOnIssGoesToFlightSurgeon() {
        val d = Triage.decide(systems(Status.RED), emptyList(), emptyList(), env("iss", 0.0), triageConfig)
        assertEquals(Responder.FLIGHT_SURGEON, d.responder)
        assertEquals(10.0, d.earthResponseMin!!, 1e-9)
    }

    @Test
    fun redOnMarsUsesOnboardProtocolBecauseEarthIsTooFar() {
        // 2 x 12 min signal + 10 min handling = 34 min > 30 min window.
        val d = Triage.decide(systems(Status.RED), emptyList(), emptyList(), env("mars", 12.0), triageConfig)
        assertEquals(Responder.ONBOARD_PROTOCOL, d.responder)
    }

    @Test
    fun severalYellowSystemsEscalateToCrewMedicalOfficer() {
        val d = Triage.decide(systems(Status.YELLOW, Status.YELLOW, Status.GREEN), emptyList(), emptyList(), null, triageConfig)
        assertEquals(Responder.CREW_MEDICAL_OFFICER, d.responder)
    }

    @Test
    fun allGreenStaysWithAstronaut() {
        val d = Triage.decide(systems(Status.GREEN), emptyList(), emptyList(), null, triageConfig)
        assertEquals(Responder.SELF, d.responder)
    }
}
