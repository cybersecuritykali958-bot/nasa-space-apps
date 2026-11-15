package space.crewpulse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedFingertipTest {

    @Test
    fun estimatorRecoversTheSimulatedRate() {
        for (bpm in listOf(58.0, 72.0, 95.0)) {
            val sim = SimulatedFingertip(bpm)
            val samples = (0 until 30 * 30).map { i -> val t = i / 30.0; (t * 1000).toLong() to sim.sample(t) }
            val e = PulseFromCamera.estimate(samples)!!
            assertEquals("bpm $bpm", bpm, e.bpm, 2.0)
            assertTrue(e.quality >= PulseFromCamera.MIN_QUALITY)
        }
    }
}
