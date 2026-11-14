package space.crewpulse.domain

import space.crewpulse.data.model.Source
import space.crewpulse.data.model.Status

/** One stored measurement as the engine sees it. */
data class Sample(val timestampMillis: Long, val value: Double, val source: String)

enum class Direction { UP, DOWN }

enum class AlertKind {
    /** Smoothed level (EWMA) left the personal control band. */
    EWMA_SHIFT,

    /** Small persistent shift accumulated over days (CUSUM). */
    CUSUM_DRIFT,

    /** Significant linear trend that will reach a worse zone within the horizon. */
    TREND_TO_THRESHOLD,
}

/** Early warning: something is changing before (or regardless of) a threshold being crossed. */
data class Alert(
    val indicatorId: String,
    val kind: AlertKind,
    val direction: Direction,
    /** When the current out-of-control run started. */
    val sinceMillis: Long,
    /** Size of the shift in personal standard deviations, if applicable. */
    val sigmas: Double? = null,
    val daysToThreshold: Double? = null,
    val thresholdValue: Double? = null,
    val thresholdStatus: Status? = null,
    val message: String,
)

enum class ConfidenceLevel { HIGH, MEDIUM, LOW }

/** How much the latest value of an indicator can be trusted, with human-readable reasons. */
data class Confidence(val level: ConfidenceLevel, val reasons: List<String>)

data class PatternMatch(
    val id: String,
    val name: String,
    val summary: String,
    /** Which conditions matched, e.g. "Sleep: 5.2 h (red)". */
    val evidence: List<String>,
    val riskIds: List<String>,
    val advice: String,
    val sources: List<Source>,
    val verified: Boolean,
)

enum class Responder {
    /** Astronaut handles it with the in-app actions. */
    SELF,
    CREW_MEDICAL_OFFICER,

    /** Earth can answer in time: contact the flight surgeon. */
    FLIGHT_SURGEON,

    /** Earth cannot answer in time: crew follows the onboard protocol, report is queued for ground. */
    ONBOARD_PROTOCOL,
}

data class TriageDecision(
    val status: Status,
    val responder: Responder,
    val headline: String,
    val reason: String,
    /** Signal round trip plus ground handling time, minutes. */
    val earthResponseMin: Double?,
    val responseWindowMin: Double?,
)

enum class Verdict { GO, CAUTION, NO_GO }

data class ReadinessResult(
    val operationId: String,
    val operationName: String,
    val verdict: Verdict,
    val reasons: List<String>,
    val verified: Boolean,
)

/** Everything the UI needs from the engine in one object. */
data class EngineReport(
    val systems: List<SystemStatus>,
    val alerts: List<Alert>,
    val confidence: Map<String, Confidence>,
    val patterns: List<PatternMatch>,
    val triage: TriageDecision,
    val readiness: List<ReadinessResult>,
    val plan: MeasurementPlan? = null,
    val game: GameState? = null,
    val spaceWeather: SpaceWeatherStatus? = null,
    val radiation: RadiationBudget? = null,
) {
    fun evaluation(indicatorId: String): Evaluation? =
        systems.asSequence().flatMap { it.evaluations }.firstOrNull { it.indicator.id == indicatorId }

    fun alertsFor(indicatorId: String): List<Alert> = alerts.filter { it.indicatorId == indicatorId }

    companion object {
        val EMPTY = EngineReport(
            systems = emptyList(),
            alerts = emptyList(),
            confidence = emptyMap(),
            patterns = emptyList(),
            triage = TriageDecision(Status.UNKNOWN, Responder.SELF, "No data yet", "Do a check-in to start.", null, null),
            readiness = emptyList(),
        )
    }
}

internal const val DAY_MILLIS = 24L * 60 * 60 * 1000
