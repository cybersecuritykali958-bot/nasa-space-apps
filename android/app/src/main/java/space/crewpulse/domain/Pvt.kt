package space.crewpulse.domain

import kotlin.random.Random

/**
 * Psychomotor Vigilance Test, brief 3-minute version (PVT-B).
 *
 * Basner, Mollicone & Dinges 2011 (doi:10.1016/j.actaastro.2011.07.015): 3-minute PVT-B with
 * 1-4 s inter-stimulus intervals; a lapse is a response of 355 ms or slower; a response faster
 * than 100 ms counts as a false start. Basner et al. 2015 use the PVT in NASA's Cognition battery.
 *
 * The UI owns the clock and the screen; this class owns the protocol and the scoring:
 *   val session = PvtSession()
 *   delay(session.nextDelayMillis())   // then show the stimulus and start a timer
 *   session.recordResponse(rtMillis)   // on tap; or recordEarlyTap() if tapped before the stimulus
 *   if (session.isFinished(elapsedMillis)) home.savePvt(session.result())
 */
class PvtSession(
    val durationMillis: Long = DURATION_MILLIS,
    private val random: Random = Random.Default,
) {
    private val reactionTimes = mutableListOf<Long>()
    private var falseStarts = 0

    /** Random wait before the next stimulus. */
    fun nextDelayMillis(): Long = random.nextLong(MIN_DELAY_MILLIS, MAX_DELAY_MILLIS + 1)

    /** Tap after the stimulus, measured from stimulus onset. Too fast to be a real reaction = false start. */
    fun recordResponse(reactionMillis: Long) {
        if (reactionMillis < FALSE_START_MILLIS) falseStarts++ else reactionTimes += reactionMillis
    }

    /** Tap while no stimulus was shown. */
    fun recordEarlyTap() {
        falseStarts++
    }

    fun isFinished(elapsedMillis: Long): Boolean = elapsedMillis >= durationMillis

    fun result(): PvtResult = PvtScoring.score(reactionTimes, falseStarts)

    companion object {
        const val DURATION_MILLIS = 180_000L
        const val MIN_DELAY_MILLIS = 1_000L
        const val MAX_DELAY_MILLIS = 4_000L
        const val FALSE_START_MILLIS = 100L
        const val LAPSE_MILLIS = 355L
    }
}

data class PvtResult(
    val trials: Int,
    val meanReactionMillis: Double,
    val medianReactionMillis: Double,
    /** Responses of 355 ms or slower. Basner & Dinges 2011: more sensitive to sleep loss than the mean. */
    val lapses: Int,
    val falseStarts: Int,
    /** Mean response speed 1/RT, in 1/s. Also more sensitive than the mean RT. */
    val meanSpeedPerSecond: Double,
) {
    /** Too few valid responses to trust the result. */
    val isReliable: Boolean get() = trials >= MIN_TRIALS

    companion object {
        const val MIN_TRIALS = 10
    }
}

object PvtScoring {
    fun score(reactionTimes: List<Long>, falseStarts: Int): PvtResult {
        if (reactionTimes.isEmpty()) return PvtResult(0, 0.0, 0.0, 0, falseStarts, 0.0)
        val sorted = reactionTimes.sorted()
        val mid = sorted.size / 2
        val median = if (sorted.size % 2 == 1) sorted[mid].toDouble() else (sorted[mid - 1] + sorted[mid]) / 2.0
        return PvtResult(
            trials = reactionTimes.size,
            meanReactionMillis = reactionTimes.average(),
            medianReactionMillis = median,
            lapses = reactionTimes.count { it >= PvtSession.LAPSE_MILLIS },
            falseStarts = falseStarts,
            meanSpeedPerSecond = reactionTimes.map { 1000.0 / it }.average(),
        )
    }
}
