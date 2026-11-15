package space.crewpulse.domain

import kotlin.math.sqrt

/**
 * Heart rate from contact photoplethysmography: the fingertip covers the rear camera and the flash, and each
 * heartbeat slightly changes how much light the skin absorbs. The input is the mean red value of each frame.
 *
 * Method: resample to a fixed rate, remove slow drift (2 s moving average) and pixel noise (0.2 s moving average),
 * then take the lag with the strongest autocorrelation between 40 and 200 beats per minute. The autocorrelation
 * at that lag doubles as a signal-quality score.
 */
object PulseFromCamera {

    data class Estimate(val bpm: Double, val quality: Double)

    const val RATE_HZ = 30.0
    const val MIN_BPM = 40.0
    const val MAX_BPM = 200.0
    const val MIN_SECONDS = 10.0

    /** Below this the waveform is not periodic enough to trust (finger moved, no contact, no flash). */
    const val MIN_QUALITY = 0.5

    /** [samples]: frame time in milliseconds to mean red value. Returns null if there is too little data. */
    fun estimate(samples: List<Pair<Long, Double>>): Estimate? {
        val signal = filtered(samples) ?: return null
        val minLag = (RATE_HZ * 60 / MAX_BPM).toInt()
        val maxLag = (RATE_HZ * 60 / MIN_BPM).toInt()
        if (signal.size < maxLag * 3) return null
        val r = DoubleArray(maxLag + 2) { lag -> if (lag < minLag - 1) 0.0 else autocorrelation(signal, lag) }

        // Local maxima in range; prefer the shortest lag that is nearly as strong as the best one,
        // so the second harmonic (two beats) is not taken for one slow beat.
        val peaks = (minLag..maxLag).filter { r[it] > r[it - 1] && r[it] >= r[it + 1] }
        if (peaks.isEmpty()) return null
        val best = peaks.maxOf { r[it] }
        val lag = peaks.first { r[it] >= 0.85 * best }

        // Parabolic interpolation around the peak for sub-sample precision.
        val (a, b, c) = Triple(r[lag - 1], r[lag], r[lag + 1])
        val denom = a - 2 * b + c
        val shift = if (denom != 0.0) (0.5 * (a - c) / denom).coerceIn(-0.5, 0.5) else 0.0
        return Estimate(bpm = 60 * RATE_HZ / (lag + shift), quality = b)
    }

    /** Detrended, smoothed signal at [RATE_HZ]; also used to draw the live waveform. */
    fun filtered(samples: List<Pair<Long, Double>>, minSeconds: Double = MIN_SECONDS): DoubleArray? {
        val raw = resample(samples, minSeconds) ?: return null
        val trend = movingAverage(raw, (2.0 * RATE_HZ).toInt())
        val detrended = DoubleArray(raw.size) { raw[it] - trend[it] }
        return movingAverage(detrended, (0.2 * RATE_HZ).toInt().coerceAtLeast(1))
    }

    private fun resample(samples: List<Pair<Long, Double>>, minSeconds: Double): DoubleArray? {
        val s = samples.sortedBy { it.first }
        if (s.size < 2) return null
        val t0 = s.first().first
        val seconds = (s.last().first - t0) / 1000.0
        if (seconds < minSeconds) return null
        val n = (seconds * RATE_HZ).toInt()
        val out = DoubleArray(n)
        var j = 0
        for (i in 0 until n) {
            val t = t0 + i * 1000.0 / RATE_HZ
            while (j < s.size - 2 && s[j + 1].first < t) j++
            val (ta, va) = s[j]
            val (tb, vb) = s[j + 1]
            val f = if (tb == ta) 0.0 else ((t - ta) / (tb - ta)).coerceIn(0.0, 1.0)
            out[i] = va + (vb - va) * f
        }
        return out
    }

    private fun movingAverage(x: DoubleArray, window: Int): DoubleArray {
        val half = window / 2
        val prefix = DoubleArray(x.size + 1)
        for (i in x.indices) prefix[i + 1] = prefix[i] + x[i]
        return DoubleArray(x.size) { i ->
            val lo = (i - half).coerceAtLeast(0)
            val hi = (i + half + 1).coerceAtMost(x.size)
            (prefix[hi] - prefix[lo]) / (hi - lo)
        }
    }

    private fun autocorrelation(x: DoubleArray, lag: Int): Double {
        val n = x.size - lag
        if (n <= 1) return 0.0
        val a = x.copyOfRange(0, n)
        val b = x.copyOfRange(lag, lag + n)
        val ma = a.average()
        val mb = b.average()
        var num = 0.0
        var da = 0.0
        var db = 0.0
        for (i in 0 until n) {
            num += (a[i] - ma) * (b[i] - mb)
            da += (a[i] - ma) * (a[i] - ma)
            db += (b[i] - mb) * (b[i] - mb)
        }
        return if (da == 0.0 || db == 0.0) 0.0 else num / sqrt(da * db)
    }
}
