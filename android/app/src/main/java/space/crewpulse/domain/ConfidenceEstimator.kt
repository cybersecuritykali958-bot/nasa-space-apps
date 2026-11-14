package space.crewpulse.domain

import space.crewpulse.data.model.ConfidenceConfig

/** Rates how far the latest value of an indicator can be trusted. The worst reason sets the level. */
object ConfidenceEstimator {

    fun estimate(
        history: List<Sample>,
        hasBaseline: Boolean,
        nowMillis: Long,
        config: ConfidenceConfig,
    ): Confidence {
        val latest = history.maxByOrNull { it.timestampMillis }
            ?: return Confidence(ConfidenceLevel.LOW, listOf("No measurements yet."))

        val reasons = mutableListOf<Pair<ConfidenceLevel, String>>()
        val ageHours = (nowMillis - latest.timestampMillis) / 3_600_000.0
        if (ageHours > config.staleAfterHours) {
            reasons += ConfidenceLevel.LOW to "Last measured ${"%.0f".format(ageHours)} h ago, please re-measure."
        }
        if (latest.source in config.estimatedSources) {
            reasons += ConfidenceLevel.MEDIUM to "Estimated by ${latest.source}, not a direct measurement."
        }
        if (!hasBaseline) {
            reasons += ConfidenceLevel.MEDIUM to "No personal baseline yet, only population ranges apply."
        }
        if (history.size < 3) {
            reasons += ConfidenceLevel.MEDIUM to "Only ${history.size} measurement(s) so far."
        }
        val level = reasons.maxOfOrNull { it.first } ?: ConfidenceLevel.HIGH
        // Enum order is HIGH < MEDIUM < LOW, so maxOf picks the least confident.
        return Confidence(level, reasons.map { it.second })
    }
}
