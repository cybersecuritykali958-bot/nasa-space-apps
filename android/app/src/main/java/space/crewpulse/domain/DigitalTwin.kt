package space.crewpulse.domain

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import space.crewpulse.data.model.Environment
import space.crewpulse.data.model.Status

/**
 * Digital Twin: projected hip bone density, leg strength and radiation dose over a mission.
 *
 * Kotlin port of science/digital_twin.py, which is the source of truth for the formulas.
 * DigitalTwinTest checks that this port reproduces its test vectors. Constants and their sources
 * are documented in docs/SCIENCE.md, section "Digital Twin".
 *
 * Per mission day in an environment with gravity g:
 *   unloading u = 1 - g
 *   bone   -= rate(a) * u / 30.44        (% of pre-flight hip BMD)
 *   muscle *= exp(-k(a) * u)             (% of pre-flight leg strength)
 *   dose   += radiationMsvPerDay (+ solar particle events)
 * where a in [0, 1] is resistive-exercise adherence (1 = full ARED protocol).
 */
object DigitalTwin {

    private const val DAYS_PER_MONTH = 30.44

    // NASA HRP evidence report: 1-1.5 %/month without countermeasures (midpoint).
    const val BONE_RATE_NO_CM = 1.25

    // NASA/SP-20250000273 (ARED era 0.5-1.0 %/month); Smith et al. 2012.
    const val BONE_RATE_ARED = 0.5

    // Sibonga et al. 2007: 50 % recovery in 255 days at the trochanter.
    const val BONE_RECOVERY_HALF_LIFE_DAYS = 255.0

    // NASA-STD-3001 Vol. 1 Rev B: hip >= 95 %, femoral neck >= 90 % of pre-mission BMD.
    const val BONE_GREEN_MIN = 95.0
    const val BONE_RED_BELOW = 90.0

    // NASA/SP-20250000273: -9 % leg strength after ~6 months with ARED.
    // Trappe et al. 2009: -32 % calf peak power after 6 months, worst-case proxy.
    private const val MISSION_6MO_DAYS = 180.0
    val K_MUSCLE_ARED = -ln(1 - 0.09) / MISSION_6MO_DAYS
    val K_MUSCLE_LOW = -ln(1 - 0.32) / MISSION_6MO_DAYS

    // NASA-STD-3001 Vol. 1 Rev B: strength >= 80 % of baseline; yellow at half the margin (team rule).
    const val MUSCLE_GREEN_MIN = 90.0
    const val MUSCLE_RED_BELOW = 80.0

    // NASA-STD-3001 Vol. 1: career limit 600 mSv, set at 3 % REID.
    const val DOSE_LIMIT_MSV = 600.0
    const val REID_AT_LIMIT_PCT = 3.0
    const val DOSE_YELLOW_FRACTION = 0.5 // ALARA, team rule
    const val DOSE_RED_FRACTION = 0.9

    // LeBlanc et al. 2000: human bone data cover flights up to 14.4 months.
    const val DATA_LIMIT_DAYS = 438

    data class Phase(val environmentId: String, val days: Int)

    data class State(
        val day: Int,
        val environmentId: String,
        val bonePct: Double,
        val musclePct: Double,
        val doseMsv: Double,
        val boneStatus: Status,
        val muscleStatus: Status,
        val doseStatus: Status,
        val reidPct: Double,
        /** Days left to the career dose limit at this environment's rate; null if the rate is 0. */
        val daysToDoseLimit: Double?,
        /** No human data beyond 14.4 months: the UI should show this as an extrapolation. */
        val extrapolated: Boolean,
    )

    /** Ready-made scenarios matching science/digital_twin.py. */
    val SCENARIOS: Map<String, List<Phase>> = linkedMapOf(
        "iss_6mo" to listOf(Phase("iss", 180)),
        "mars_transit_180" to listOf(Phase("mars_transit", 180)),
        "mars_mission" to listOf(Phase("mars_transit", 180), Phase("mars_surface", 500), Phase("mars_transit", 180)),
        "two_years_transit" to listOf(Phase("mars_transit", 730)),
    )

    fun boneRatePctPerMonth(adherence: Double) = lerp(BONE_RATE_NO_CM, BONE_RATE_ARED, adherence)

    fun muscleKPerDay(adherence: Double) = lerp(K_MUSCLE_LOW, K_MUSCLE_ARED, adherence)

    /** Day-by-day states. Day N is the end of mission day N (day 0, pre-flight, is not included). */
    fun simulate(
        phases: List<Phase>,
        environments: Map<String, Environment>,
        adherence: Double = 1.0,
        startDoseMsv: Double = 0.0,
        extraDoseByDay: Map<Int, Double> = emptyMap(),
    ): List<State> {
        var bone = 100.0
        var muscle = 100.0
        var dose = startDoseMsv
        val rateBone = boneRatePctPerMonth(adherence)
        val kMuscle = muscleKPerDay(adherence)
        val states = ArrayList<State>()
        var day = 0
        for (phase in phases) {
            val env = environments[phase.environmentId]
                ?: error("Unknown environment ${phase.environmentId}")
            val unloading = 1.0 - env.gravityG
            repeat(phase.days) {
                day += 1
                bone -= rateBone * unloading / DAYS_PER_MONTH
                muscle *= exp(-kMuscle * unloading)
                dose += env.radiationMsvPerDay + (extraDoseByDay[day] ?: 0.0)
                val rate = env.radiationMsvPerDay
                states += State(
                    day = day,
                    environmentId = phase.environmentId,
                    bonePct = bone,
                    musclePct = muscle,
                    doseMsv = dose,
                    boneStatus = higherIsBetter(bone, BONE_GREEN_MIN, BONE_RED_BELOW),
                    muscleStatus = higherIsBetter(muscle, MUSCLE_GREEN_MIN, MUSCLE_RED_BELOW),
                    doseStatus = doseStatus(dose),
                    reidPct = reidPct(dose),
                    daysToDoseLimit = if (rate > 0) (DOSE_LIMIT_MSV - dose).coerceAtLeast(0.0) / rate else null,
                    extrapolated = day > DATA_LIMIT_DAYS,
                )
            }
        }
        return states
    }

    /** Bone recovery after landing on Earth (Sibonga et al. 2007). */
    fun boneAfterLanding(boneAtLandingPct: Double, daysAfterLanding: Double): Double {
        val loss = 100.0 - boneAtLandingPct
        return 100.0 - loss * 0.5.pow(daysAfterLanding / BONE_RECOVERY_HALF_LIFE_DAYS)
    }

    /** Linear approximation anchored at the NASA limit: 600 mSv -> 3 % REID. */
    fun reidPct(doseMsv: Double) = REID_AT_LIMIT_PCT * doseMsv / DOSE_LIMIT_MSV

    fun doseStatus(doseMsv: Double): Status = when {
        doseMsv >= DOSE_LIMIT_MSV * DOSE_RED_FRACTION -> Status.RED
        doseMsv >= DOSE_LIMIT_MSV * DOSE_YELLOW_FRACTION -> Status.YELLOW
        else -> Status.GREEN
    }

    private fun higherIsBetter(value: Double, greenMin: Double, redBelow: Double): Status = when {
        value < redBelow -> Status.RED
        value >= greenMin -> Status.GREEN
        else -> Status.YELLOW
    }

    /**
     * Countermeasure coach: the inverse of [simulate]. Given a scenario, finds the smallest share of the ARED
     * exercise protocol (0..1) that keeps bone and muscle at or above a limit until the last mission day.
     */
    data class Coach(
        /** Smallest adherence that keeps bone and muscle green on the last day; null if 100 % is not enough. */
        val forGreen: Double?,
        /** Smallest adherence that keeps bone and muscle out of red on the last day; null if 100 % is not enough. */
        val forNoRed: Double?,
        /** With the full protocol, the first day bone or muscle turns red; null if it never does. */
        val firstRedDayAtFull: Int?,
        val lastDay: Int,
    )

    /** [simulateAt] runs the scenario for a given adherence, e.g. `{ a -> simulate(phases, envs, a) }`. */
    fun coach(simulateAt: (Double) -> List<State>): Coach? {
        val full = simulateAt(1.0)
        val last = full.lastOrNull() ?: return null
        return Coach(
            forGreen = minAdherence(simulateAt) { it.bonePct >= BONE_GREEN_MIN && it.musclePct >= MUSCLE_GREEN_MIN },
            forNoRed = minAdherence(simulateAt) { it.bonePct >= BONE_RED_BELOW && it.musclePct >= MUSCLE_RED_BELOW },
            firstRedDayAtFull = full.firstOrNull { it.boneStatus == Status.RED || it.muscleStatus == Status.RED }?.day,
            lastDay = last.day,
        )
    }

    /**
     * Bone and muscle only fall in flight, and they fall more slowly with more exercise, so the last day is the
     * worst day and the result is monotonic in adherence: bisection finds the threshold. Rounded up to 1 %.
     */
    internal fun minAdherence(simulateAt: (Double) -> List<State>, ok: (State) -> Boolean): Double? {
        fun passes(a: Double) = simulateAt(a).lastOrNull()?.let(ok) ?: false
        if (!passes(1.0)) return null
        if (passes(0.0)) return 0.0
        var lo = 0.0
        var hi = 1.0
        repeat(14) {
            val mid = (lo + hi) / 2
            if (passes(mid)) hi = mid else lo = mid
        }
        return (kotlin.math.ceil(hi * 100) / 100).coerceAtMost(1.0)
    }

    private fun lerp(low: Double, high: Double, adherence: Double): Double {
        val a = adherence.coerceIn(0.0, 1.0)
        return low + (high - low) * a
    }
}
