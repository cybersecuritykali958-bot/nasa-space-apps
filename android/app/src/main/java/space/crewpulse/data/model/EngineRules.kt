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
    val sources: List<Source> = emptyList(),
    val verified: Boolean = false,
)
