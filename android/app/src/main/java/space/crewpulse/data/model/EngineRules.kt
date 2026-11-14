package space.crewpulse.data.model

import kotlinx.serialization.Serializable

// Kotlin mirror of assets/engine_rules.json: every tunable number and rule of the health engine.
// Science edits the JSON; code changes are only needed for new condition types.

@Serializable
data class EngineRules(
    val version: Int,
    val earlyWarning: EarlyWarningConfig,
    val confidence: ConfidenceConfig,
    val triage: TriageConfig,
    val patterns: List<PatternRule>,
    val readiness: List<ReadinessRule>,
    val schedule: ScheduleConfig = ScheduleConfig(),
    val gamification: GamificationConfig = GamificationConfig(),
    val spaceWeather: SpaceWeatherConfig = SpaceWeatherConfig(),
    val input: InputConfig = InputConfig(),
)

/** Statistical process control settings. Defaults are textbook values (see sources). */
@Serializable
data class EarlyWarningConfig(
    /** Readings needed before any early-warning check runs. */
    val minHistory: Int = 7,
    /** First N readings form the personal reference (mean and spread). */
    val baselineWindow: Int = 14,
    /** EWMA smoothing weight, 0..1. Smaller reacts slower but catches smaller shifts. */
    val ewmaLambda: Double = 0.2,
    /** EWMA control limit width in sigmas of the EWMA statistic. */
    val ewmaL: Double = 3.0,
    /** CUSUM allowance in personal sigmas. */
    val cusumK: Double = 0.5,
    /** CUSUM decision threshold in personal sigmas. */
    val cusumH: Double = 5.0,
    /** Days of history used for the linear trend. */
    val trendWindowDays: Int = 14,
    /** Only forecast threshold crossings this many days ahead. */
    val trendHorizonDays: Int = 30,
    /** Minimum |t| of the trend slope, filters out noise. */
    val trendMinT: Double = 3.0,
    val sources: List<Source> = emptyList(),
)

@Serializable
data class ConfidenceConfig(
    /** A reading older than this is stale. */
    val staleAfterHours: Double = 48.0,
    /** Reading sources that are estimates rather than direct measurements. */
    val estimatedSources: List<String> = listOf("camera"),
)

@Serializable
data class TriageConfig(
    /** How fast a situation of this status needs a response, in minutes. */
    val responseWindowMin: Map<Status, Double>,
    /** Time the ground team needs to read and answer, on top of the signal delay. */
    val groundHandlingMin: Double,
    val sources: List<Source> = emptyList(),
    val verified: Boolean = false,
)

/**
 * One check on one indicator. Every field that is set must hold.
 * Deviation is signed percent from the personal baseline (+ above, - below).
 */
@Serializable
data class Condition(
    val indicator: String,
    val minStatus: Status? = null,
    val statusIn: List<Status>? = null,
    val minDeviationPct: Double? = null,
    val maxDeviationPct: Double? = null,
    /** true: the indicator must have an early-warning alert. */
    val hasAlert: Boolean? = null,
)

/** Matches when all `all` conditions hold and at least `minAny` of `any` hold. */
@Serializable
data class PatternRule(
    val id: String,
    val name: String,
    val summary: String,
    val all: List<Condition> = emptyList(),
    val any: List<Condition> = emptyList(),
    val minAny: Int = 1,
    val riskIds: List<String> = emptyList(),
    val advice: String,
    val sources: List<Source> = emptyList(),
    val verified: Boolean = false,
)

/** Per-indicator gate: at `cautionAt` or worse -> CAUTION, at `noGoAt` or worse -> NO_GO. */
@Serializable
data class ReadinessGate(
    val indicator: String,
    val cautionAt: Status = Status.YELLOW,
    val noGoAt: Status = Status.RED,
)

@Serializable
data class ReadinessRule(
    val id: String,
    val name: String,
    val gates: List<ReadinessGate>,
    /** true: a space weather WARNING means NO-GO, a WATCH means CAUTION (e.g. spacewalks). */
    val spaceWeatherGate: Boolean = false,
    val sources: List<Source> = emptyList(),
    val verified: Boolean = false,
)

/** Adaptive measurement schedule. Indicators without an interval are not scheduled (e.g. dosimeter). */
@Serializable
data class ScheduleConfig(
    /** Routine interval when everything is green and stable. */
    val intervalDays: Map<String, Double> = emptyMap(),
    /** Crew time one measurement takes, used to show time saved. */
    val minutesPerMeasurement: Map<String, Double> = emptyMap(),
    /** Interval for yellow indicators or indicators with an early warning. */
    val attentionIntervalDays: Double = 1.0,
    /** Re-check delay after a red value. */
    val redRecheckHours: Double = 1.0,
    /** Natural time of day for each measurement, "HH:mm" local time (e.g. resting heart rate right after waking). */
    val timeOfDay: Map<String, String> = emptyMap(),
)

@Serializable
data class GamificationConfig(
    val pointsPerCheckInDay: Int = 10,
    val pointsPerMeasurement: Int = 2,
    val fullCheckInBonus: Int = 20,
    /** A day with all of these measured counts as a full check-in. */
    val coreIndicators: List<String> = emptyList(),
    val pointsPerLevel: Int = 250,
    val levelTitles: List<String> = listOf("Cadet"),
)

@Serializable
data class SpaceWeatherConfig(
    /** A solar energetic particle event this recent means: go to the shielded area. */
    val sepWarningHours: Double = 48.0,
    /** A fast CME this recent means: watch for particle events. */
    val cmeWatchHours: Double = 72.0,
    val cmeFastSpeedKms: Double = 1000.0,
    /** X-class flare this recent raises a watch. */
    val flareWatchHours: Double = 48.0,
    /** Career effective dose limit, mSv. */
    val careerLimitMsv: Double = 600.0,
    val sources: List<Source> = emptyList(),
)

/** Normalized NASA DONKI events, as stored in assets/donki_cache.json and the on-device cache. */
@Serializable
data class SpaceWeatherFile(
    val version: Int,
    /** ISO-8601 time of the fetch. */
    val fetchedAt: String,
    val source: String = "NASA DONKI",
    val events: List<SpaceWeatherEventDto>,
)

@Serializable
data class SpaceWeatherEventDto(
    /** "SEP", "CME" or "FLR". */
    val type: String,
    val id: String,
    /** ISO-8601, UTC. */
    val time: String,
    /** Short human detail: flare class, CME speed, instruments. */
    val detail: String = "",
    /** CME speed in km/s, when known. */
    val speedKms: Double? = null,
    /** Flare class such as "X1.2", when known. */
    val flareClass: String? = null,
    val link: String = "",
)

@Serializable
data class ValueRange(val min: Double, val max: Double)

/** Plausibility limits against typos (not medical thresholds). */
@Serializable
data class InputConfig(val limits: Map<String, ValueRange> = emptyMap())
