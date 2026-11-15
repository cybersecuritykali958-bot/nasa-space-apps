package space.crewpulse.domain

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseFromCameraTest {

    /** Pulse-like wave (sharp systolic peak plus harmonic), slow drift, noise and uneven frame times. */
    private fun synthetic(bpm: Double, seconds: Double = 30.0, noise: Double = 0.3, seed: Int = 1): List<Pair<Long, Double>> {
        val rnd = Random(seed)
        val f = bpm / 60.0
        val out = ArrayList<Pair<Long, Double>>()
        var t = 0.0
        while (t < seconds) {
            val beat = sin(2 * PI * f * t) + 0.4 * sin(4 * PI * f * t + 0.8)
            val drift = 3.0 * (1 - exp(-t / 8)) + 0.5 * sin(2 * PI * 0.1 * t)
            out += (t * 1000).toLong() to (180 + beat + drift + noise * rnd.nextDouble(-1.0, 1.0))
            t += (1.0 / 30) * rnd.nextDouble(0.8, 1.2)
        }
        return out
    }

    @Test
    fun findsTheRateAcrossTheRange() {
        for (bpm in listOf(48.0, 62.0, 75.0, 98.0, 130.0, 170.0)) {
            val e = PulseFromCamera.estimate(synthetic(bpm))!!
            assertEquals("bpm $bpm", bpm, e.bpm, 2.0)
            assertTrue("quality at $bpm = ${e.quality}", e.quality >= PulseFromCamera.MIN_QUALITY)
        }
    }

    @Test
    fun noiseOnlyHasLowQuality() {
        val rnd = Random(7)
        val samples = (0 until 900).map { (it * 33L) to 180 + rnd.nextDouble(-1.0, 1.0) }
        val e = PulseFromCamera.estimate(samples)
        assertTrue("noise gave $e", e == null || e.quality < PulseFromCamera.MIN_QUALITY)
    }

    @Test
    fun tooShortRecordingGivesNothing() {
        assertNull(PulseFromCamera.estimate(synthetic(70.0, seconds = 5.0)))
    }
}
