package space.crewpulse.domain

import space.crewpulse.data.model.Environment
import space.crewpulse.data.model.SpaceWeatherConfig

enum class SpaceWeatherType { SEP, CME, FLR }

/** One NASA DONKI event, already parsed. */
data class SpaceWeatherEvent(
    val type: SpaceWeatherType,
    val id: String,
    val timeMillis: Long,
    val detail: String,
    val speedKms: Double? = null,
    val flareClass: String? = null,
    val link: String = "",
)

enum class SpaceWeatherLevel { QUIET, WATCH, WARNING }

data class SpaceWeatherStatus(
    val level: SpaceWeatherLevel,
    val headline: String,
    val message: String,
    val latestSep: SpaceWeatherEvent?,
    val recentFastCmes: Int,
    /** "live", "cache" or "bundled": where the events came from. */
    val dataSource: String,
    val fetchedAtMillis: Long?,
)

data class RadiationBudget(
    val usedMsv: Double,
    val limitMsv: Double,
    val remainingMsv: Double,
    val doseRateMsvPerDay: Double,
    /** Days until the career limit at the current environment's dose rate. */
    val daysLeft: Double?,
)

/** Turns real NASA DONKI events into an action for the crew, plus the career dose budget. */
object SpaceWeather {

    fun assess(
        events: List<SpaceWeatherEvent>,
        nowMillis: Long,
        config: SpaceWeatherConfig,
        dataSource: String,
        fetchedAtMillis: Long?,
    ): SpaceWeatherStatus {
        val hour = 3_600_000L
        val past = events.filter { it.timeMillis <= nowMillis }
        val latestSep = past.filter { it.type == SpaceWeatherType.SEP }.maxByOrNull { it.timeMillis }
        val recentSep = latestSep?.takeIf { nowMillis - it.timeMillis <= config.sepWarningHours * hour }
        val fastCmes = past.filter {
            it.type == SpaceWeatherType.CME && (it.speedKms ?: 0.0) >= config.cmeFastSpeedKms &&
                nowMillis - it.timeMillis <= config.cmeWatchHours * hour
        }
        val xFlares = past.filter {
            it.type == SpaceWeatherType.FLR && it.flareClass?.startsWith("X") == true &&
                nowMillis - it.timeMillis <= config.flareWatchHours * hour
        }

        return when {
            recentSep != null -> SpaceWeatherStatus(
                SpaceWeatherLevel.WARNING, "Solar particle event: go to the shielded area",
                "NASA DONKI reports a solar energetic particle event. Move to the most shielded module, " +
                    "postpone spacewalks, keep your dosimeter on.",
                latestSep, fastCmes.size, dataSource, fetchedAtMillis,
            )
            fastCmes.isNotEmpty() || xFlares.isNotEmpty() -> SpaceWeatherStatus(
                SpaceWeatherLevel.WATCH, "Space weather watch",
                buildList {
                    if (fastCmes.isNotEmpty()) add("${fastCmes.size} fast coronal mass ejection(s)")
                    if (xFlares.isNotEmpty()) add("X-class flare ${xFlares.last().flareClass}")
                }.joinToString(" and ") + " recently. Particle events may follow: plan spacewalks with care.",
                latestSep, fastCmes.size, dataSource, fetchedAtMillis,
            )
            else -> SpaceWeatherStatus(
                SpaceWeatherLevel.QUIET, "Space weather quiet",
                "No recent solar particle events or fast CMEs in NASA DONKI.",
                latestSep, 0, dataSource, fetchedAtMillis,
            )
        }
    }

    fun budget(cumulativeDoseMsv: Double?, environment: Environment?, config: SpaceWeatherConfig): RadiationBudget? {
        val used = cumulativeDoseMsv ?: return null
        val rate = environment?.radiationMsvPerDay ?: 0.0
        val remaining = (config.careerLimitMsv - used).coerceAtLeast(0.0)
        return RadiationBudget(
            usedMsv = used,
            limitMsv = config.careerLimitMsv,
            remainingMsv = remaining,
            doseRateMsvPerDay = rate,
            daysLeft = if (rate > 0) remaining / rate else null,
        )
    }
}

/** Space weather events plus where they came from, as passed into the engine. */
data class SpaceWeatherInput(
    val events: List<SpaceWeatherEvent>,
    /** "live", "cache" or "bundled". */
    val source: String,
    val fetchedAtMillis: Long?,
)
