package space.crewpulse.domain

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import space.crewpulse.data.model.EarlyWarningConfig
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.Status

/**
 * Early warning on one indicator's history, using classic statistical process control
 * against the astronaut's own reference period instead of population norms:
 *
 * - EWMA: smoothed level leaves the personal control band (sudden or medium shifts).
 * - CUSUM: small shifts that add up over many days (slow drift).
 * - Trend: significant linear slope that will reach a worse zone within the horizon.
 *
 * All three describe the state at the latest sample.
 */
object EarlyWarning {

    data class PersonalStats(val mean: Double, val sd: Double, val referenceSize: Int)

    fun analyze(
        indicator: Indicator,
        history: List<Sample>,
        baseline: Double?,
        config: EarlyWarningConfig,
    ): List<Alert> {
        val samples = history.sortedBy { it.timestampMillis }
        if (samples.size < config.minHistory) return emptyList()

        val alerts = mutableListOf<Alert>()
        if (!indicator.cumulative) {
            personalStats(samples, baseline, config)?.let { stats ->
                ewma(indicator, samples, stats, config)?.let(alerts::add)
                cusum(indicator, samples, stats, config)?.let(alerts::add)
            }
        }
        trend(indicator, samples, config)?.let(alerts::add)
        return alerts
    }

    /** Mean and spread of the first readings. The stored baseline, if any, is used as the mean. */
    fun personalStats(samples: List<Sample>, baseline: Double?, config: EarlyWarningConfig): PersonalStats? {
        val size = minOf(config.baselineWindow, samples.size / 2)
        if (size < 3) return null
        val ref = samples.take(size).map { it.value }
        val refMean = ref.average()
        val sd = sqrt(ref.sumOf { (it - refMean).pow(2) } / (ref.size - 1))
        if (sd < 1e-9) return null
        return PersonalStats(mean = baseline ?: refMean, sd = sd, referenceSize = size)
    }

    fun ewma(indicator: Indicator, samples: List<Sample>, stats: PersonalStats, config: EarlyWarningConfig): Alert? {
        val lambda = config.ewmaLambda
        val sigmaZ = stats.sd * sqrt(lambda / (2 - lambda))
        var z = stats.mean
        var runStart: Long? = null
        for (i in stats.referenceSize until samples.size) {
            z = lambda * samples[i].value + (1 - lambda) * z
            val out = abs(z - stats.mean) > config.ewmaL * sigmaZ
            runStart = if (out) runStart ?: samples[i].timestampMillis else null
        }
        val since = runStart ?: return null
        val dir = if (z > stats.mean) Direction.UP else Direction.DOWN
        val sigmas = (z - stats.mean) / stats.sd
        return Alert(
            indicatorId = indicator.id,
            kind = AlertKind.EWMA_SHIFT,
            direction = dir,
            sinceMillis = since,
            sigmas = sigmas,
            message = "${indicator.name} has shifted ${dir.word()} your personal baseline " +
                "(${"%+.1f".format(sigmas)} SD of your usual variation).",
        )
    }

    fun cusum(indicator: Indicator, samples: List<Sample>, stats: PersonalStats, config: EarlyWarningConfig): Alert? {
        val z = samples.map { (it.value - stats.mean) / stats.sd }
        var high = 0.0
        var low = 0.0
        var runStart: Long? = null
        for (i in stats.referenceSize until samples.size) {
            high = maxOf(0.0, high + z[i] - config.cusumK)
            low = maxOf(0.0, low - z[i] - config.cusumK)

            // Restart after an episode ends. Without this, a short spike (three days of fever) keeps the
            // sum above the threshold for weeks while it slowly decays, reporting a drift that is over.
            val recent = z.subList(maxOf(0, i - RECENT_FOR_DRIFT + 1), i + 1).average()
            if (high > config.cusumH && recent <= config.cusumK) high = 0.0
            if (low > config.cusumH && recent >= -config.cusumK) low = 0.0

            val out = high > config.cusumH || low > config.cusumH
            runStart = if (out) runStart ?: samples[i].timestampMillis else null
        }
        val since = runStart ?: return null
        val dir = if (high >= low) Direction.UP else Direction.DOWN
        val days = (samples.last().timestampMillis - since) / DAY_MILLIS
        return Alert(
            indicatorId = indicator.id,
            kind = AlertKind.CUSUM_DRIFT,
            direction = dir,
            sinceMillis = since,
            sigmas = maxOf(high, low),
            message = "${indicator.name} shows a slow, persistent drift ${dir.word()} your baseline" +
                (if (days > 0) " for $days days." else "."),
        )
    }

    fun trend(indicator: Indicator, samples: List<Sample>, config: EarlyWarningConfig): Alert? {
        val last = samples.last().timestampMillis
        val window = samples.filter { it.timestampMillis >= last - config.trendWindowDays * DAY_MILLIS }
        if (window.size < 5) return null

        val xs = window.map { (it.timestampMillis - last).toDouble() / DAY_MILLIS }
        val fit = linearFit(xs, window.map { it.value }) ?: return null
        if (abs(fit.t) < config.trendMinT || fit.slope == 0.0) return null

        val current = fit.intercept // fitted value at the latest sample (x = 0)
        val dir = if (fit.slope > 0) Direction.UP else Direction.DOWN
        val (threshold, status) = nextWorseBoundary(indicator, current, dir) ?: return null
        val days = (threshold - current) / fit.slope
        if (days <= 0 || days > config.trendHorizonDays) return null

        return Alert(
            indicatorId = indicator.id,
            kind = AlertKind.TREND_TO_THRESHOLD,
            direction = dir,
            sinceMillis = window.first().timestampMillis,
            daysToThreshold = days,
            thresholdValue = threshold,
            thresholdStatus = status,
            message = "${indicator.name} is trending ${if (dir == Direction.UP) "up" else "down"} " +
                "(${"%+.2f".format(fit.slope)} ${indicator.unit}/day) and will reach the " +
                "${status.name.lowercase()} zone ($threshold ${indicator.unit}) in about ${"%.0f".format(days)} days.",
        )
    }

    data class Fit(val slope: Double, val intercept: Double, val t: Double)

    /** Ordinary least squares with the t statistic of the slope. */
    fun linearFit(xs: List<Double>, ys: List<Double>): Fit? {
        val n = xs.size
        if (n < 3) return null
        val mx = xs.average()
        val my = ys.average()
        val sxx = xs.sumOf { (it - mx).pow(2) }
        if (sxx == 0.0) return null
        val slope = xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) } / sxx
        val intercept = my - slope * mx
        val sse = xs.indices.sumOf { (ys[it] - (intercept + slope * xs[it])).pow(2) }
        val se = sqrt(sse / (n - 2)) / sqrt(sxx)
        val t = if (se == 0.0) Double.POSITIVE_INFINITY * slope.compareTo(0.0) else slope / se
        return Fit(slope, intercept, t)
    }

    /** Nearest band edge in [dir] that leads into a worse status than the current one. */
    fun nextWorseBoundary(indicator: Indicator, value: Double, dir: Direction): Pair<Double, Status>? {
        val current = StatusEngine.severity(StatusEngine.byRange(indicator, value))
        return when (dir) {
            Direction.UP -> indicator.bands
                .filter { b -> b.min != null && b.min > value && StatusEngine.severity(b.status) > current }
                .minByOrNull { it.min!! }?.let { it.min!! to it.status }
            Direction.DOWN -> indicator.bands
                .filter { b -> b.max != null && b.max <= value && StatusEngine.severity(b.status) > current }
                .maxByOrNull { it.max!! }?.let { it.max!! to it.status }
        }
    }

    /** A CUSUM episode ends when the mean of this many latest readings is back within the allowance. */
    private const val RECENT_FOR_DRIFT = 3

    private fun Direction.word() = if (this == Direction.UP) "above" else "below"
}
