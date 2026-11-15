package space.crewpulse.domain

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Demo signal for the camera pulse screen on devices without a usable camera (e.g. the emulator):
 * mean red value of a fingertip frame with a pulse wave, slow drift as the finger settles,
 * slight beat-to-beat variation and sensor noise. Clearly labelled as simulated in the UI.
 */
class SimulatedFingertip(private val bpm: Double, seed: Int = 42) {
    private val rnd = Random(seed)
    private var phase = 0.0
    private var lastSeconds = 0.0
    private var rate = bpm / 60.0

    /** Next sample at [seconds] since the start. Call with increasing times. */
    fun sample(seconds: Double): Double {
        val dt = (seconds - lastSeconds).coerceAtLeast(0.0)
        lastSeconds = seconds
        // About 2 % beat-to-beat variation around the target rate.
        rate += (bpm / 60.0 - rate) * 0.05 + rnd.nextDouble(-0.002, 0.002)
        phase += 2 * PI * rate * dt
        // Blood volume rises with each beat, so absorbed light rises and red brightness drops.
        val pulse = sin(phase) + 0.4 * sin(2 * phase + 0.8)
        val settle = 6.0 * exp(-seconds / 4)
        return 190 - 1.5 * pulse + settle + 0.5 * sin(2 * PI * 0.15 * seconds) + rnd.nextDouble(-0.4, 0.4)
    }
}
