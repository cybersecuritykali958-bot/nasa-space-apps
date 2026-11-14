package space.crewpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Kotlin mirror of the JSON files in assets/. The format is described in docs/DATA_CONTRACT.md.
// Change both together and only with team lead approval.

@Serializable
enum class Status {
    @SerialName("green") GREEN,
    @SerialName("yellow") YELLOW,
    @SerialName("red") RED,
    @SerialName("unknown") UNKNOWN,
}

@Serializable
data class Source(val title: String, val url: String)

/** Value range with a status. `min` is inclusive, `max` is exclusive, null means unbounded. */
@Serializable
data class Band(val status: Status, val min: Double? = null, val max: Double? = null)

/** Percent deviation from the personal pre-flight baseline that triggers yellow / red. */
@Serializable
data class DeviationRule(val yellow: Double, val red: Double)

@Serializable
data class Indicator(
    val id: String,
    val name: String,
    val unit: String,
    val system: String,
    val bands: List<Band>,
    val baselineDeviationPct: DeviationRule? = null,
    val howToMeasure: String,
    val whySpace: String,
    val actions: Map<Status, String>,
    val riskIds: List<String> = emptyList(),
    val sources: List<Source>,
    val verified: Boolean = false,
    /** Monotonic running total (e.g. radiation dose): skip shift detection, keep trend forecast. */
    val cumulative: Boolean = false,
)

@Serializable
data class IndicatorsFile(val version: Int, val indicators: List<Indicator>)

@Serializable
data class Environment(
    val id: String,
    val name: String,
    val gravityG: Double,
    val radiationMsvPerDay: Double,
    val commDelayMinOneWay: Double,
    val sources: List<Source>,
)

@Serializable
data class EnvironmentsFile(val version: Int, val environments: List<Environment>)

@Serializable
data class Risk(
    val id: String,
    val name: String,
    val hazard: String,
    val system: String,
    val summary: String,
    val url: String,
)

@Serializable
data class RisksFile(val version: Int, val risks: List<Risk>)

@Serializable
data class MissionDay(val day: Int, val readings: Map<String, Double>)

@Serializable
data class MissionEvent(val day: Int, val type: String, val note: String)

@Serializable
data class DemoMission(
    val version: Int,
    val astronaut: String,
    val environmentId: String,
    val synthetic: Boolean,
    val baseline: Map<String, Double>,
    val days: List<MissionDay>,
    val events: List<MissionEvent> = emptyList(),
)
