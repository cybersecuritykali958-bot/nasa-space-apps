package space.crewpulse.domain

import kotlin.math.abs
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.Status

data class Evaluation(
    val indicator: Indicator,
    val value: Double,
    val status: Status,
    val rangeStatus: Status,
    /** Null when the indicator has no baseline rule or no baseline value is known. */
    val baselineStatus: Status?,
    val deviationPct: Double?,
)

/**
 * Two independent checks, the worse one wins:
 * 1. population range from indicators.json bands;
 * 2. percent deviation from the astronaut's own pre-flight baseline.
 */
object StatusEngine {

    fun evaluate(indicator: Indicator, value: Double, baseline: Double?): Evaluation {
        val range = byRange(indicator, value)
        val deviation = deviationPct(value, baseline)
        val rule = indicator.baselineDeviationPct
        val byBaseline = if (rule != null && deviation != null) {
            when {
                abs(deviation) >= rule.red -> Status.RED
                abs(deviation) >= rule.yellow -> Status.YELLOW
                else -> Status.GREEN
            }
        } else {
            null
        }
        return Evaluation(
            indicator = indicator,
            value = value,
            status = worst(listOfNotNull(range, byBaseline)),
            rangeStatus = range,
            baselineStatus = byBaseline,
            deviationPct = deviation,
        )
    }

    fun byRange(indicator: Indicator, value: Double): Status =
        indicator.bands.firstOrNull { band ->
            (band.min == null || value >= band.min) && (band.max == null || value < band.max)
        }?.status ?: Status.UNKNOWN

    fun deviationPct(value: Double, baseline: Double?): Double? =
        if (baseline == null || baseline == 0.0) null else (value - baseline) / baseline * 100.0

    fun worst(statuses: Collection<Status>): Status =
        statuses.maxByOrNull { severity(it) } ?: Status.UNKNOWN

    private fun severity(status: Status): Int = when (status) {
        Status.UNKNOWN -> 0
        Status.GREEN -> 1
        Status.YELLOW -> 2
        Status.RED -> 3
    }
}
