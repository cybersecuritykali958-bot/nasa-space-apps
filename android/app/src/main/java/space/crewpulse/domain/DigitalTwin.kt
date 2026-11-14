package space.crewpulse.domain

/**
 * Simplified training projections for demo UI.
 * Science refines constants in docs/SCIENCE.md (@LoranSent).
 */
object DigitalTwin {
    const val CAREER_DOSE_LIMIT_MSV = 600.0
    private const val BONE_LOSS_PCT_PER_MONTH = 1.25
    private const val MUSCLE_LOSS_PCT_PER_MONTH = 1.0

    fun boneMassRemainingPct(missionDay: Int, skipExerciseWeeks: Int = 0): Double {
        val months = missionDay / 30.0
        val penalty = skipExerciseWeeks * 0.5
        return (100.0 - (BONE_LOSS_PCT_PER_MONTH + penalty) * months).coerceAtLeast(75.0)
    }

    fun muscleMassRemainingPct(missionDay: Int, skipExerciseWeeks: Int = 0): Double {
        val months = missionDay / 30.0
        val penalty = skipExerciseWeeks * 0.4
        return (100.0 - (MUSCLE_LOSS_PCT_PER_MONTH + penalty) * months).coerceAtLeast(70.0)
    }

    fun doseHeadroomMsv(cumulativeDose: Double): Double =
        (CAREER_DOSE_LIMIT_MSV - cumulativeDose).coerceAtLeast(0.0)
}