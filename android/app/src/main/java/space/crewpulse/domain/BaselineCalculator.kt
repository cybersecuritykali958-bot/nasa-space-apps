package space.crewpulse.domain

object BaselineCalculator {
    /** How many first check-ins form the automatic baseline. */
    const val FIRST_READINGS = 3

    /**
     * Mean of the first [FIRST_READINGS] values per indicator.
     * Indicators with fewer readings get no baseline yet, so only the population range applies.
     */
    fun fromFirstReadings(history: Map<String, List<Double>>, n: Int = FIRST_READINGS): Map<String, Double> =
        history.filterValues { it.size >= n }.mapValues { (_, values) -> values.take(n).average() }
}
