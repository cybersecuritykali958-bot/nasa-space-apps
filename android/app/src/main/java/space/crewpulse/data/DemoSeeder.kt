package space.crewpulse.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import space.crewpulse.data.db.ReadingDao
import space.crewpulse.data.db.ReadingEntity
import space.crewpulse.data.model.DemoMission

/**
 * Copies the bundled demo mission into Room, so demo and user data go through the same code.
 * The last mission day becomes today; each measurement gets its natural time of day from
 * engine_rules.json (`schedule.timeOfDay`), and measurements later than "now" are not stored yet,
 * so today's plan looks like a real day in progress. Only demo rows are replaced on a new seed version.
 */
class DemoSeeder(
    private val assets: AssetRepository,
    private val readingDao: ReadingDao,
    private val baselines: BaselineRepository,
    private val settings: SettingsStore,
) {
    suspend fun ensureSeeded() {
        if (readingDao.demoCount() > 0 && settings.demoSeedVersion == SEED_VERSION) return
        readingDao.deleteBySource("demo")
        val mission = assets.demoMission()
        val times = assets.engineRules().schedule.timeOfDay
        val now = System.currentTimeMillis()
        val rows = demoReadingsAtTimes(mission, LocalDate.now(), times, ZoneId.systemDefault())
            .filter { it.timestampMillis <= now }
        readingDao.insertAll(rows)
        baselines.saveMissionBaseline(mission.baseline)
        settings.demoSeedVersion = SEED_VERSION
    }

    companion object {
        /** Bump when the way demo data is laid out changes, to re-seed existing installs. */
        const val SEED_VERSION = 2
    }
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000
private val DEFAULT_TIME: LocalTime = LocalTime.of(8, 0)

/** One list of readings per mission day; the last day lands on [lastDayMillis]. */
fun demoReadings(mission: DemoMission, lastDayMillis: Long) =
    mission.days.map { day ->
        val at = lastDayMillis - (mission.days.maxOf { it.day } - day.day) * DAY_MILLIS
        checkInToReadings(day.readings, at, source = "demo")
    }

/** Demo readings with each indicator at its natural time of day; the last mission day is [lastDay]. */
fun demoReadingsAtTimes(
    mission: DemoMission,
    lastDay: LocalDate,
    timeOfDay: Map<String, String>,
    zone: ZoneId,
): List<ReadingEntity> {
    val parsed = timeOfDay.mapValues { (_, v) -> runCatching { LocalTime.parse(v) }.getOrDefault(DEFAULT_TIME) }
    val lastIndex = mission.days.maxOf { it.day }
    return mission.days.flatMap { day ->
        val date = lastDay.minusDays((lastIndex - day.day).toLong())
        day.readings.filterValues { it.isFinite() }.map { (id, value) ->
            val at = date.atTime(parsed[id] ?: DEFAULT_TIME).atZone(zone).toInstant().toEpochMilli()
            ReadingEntity(indicatorId = id, value = value, timestampMillis = at, source = "demo")
        }
    }
}
