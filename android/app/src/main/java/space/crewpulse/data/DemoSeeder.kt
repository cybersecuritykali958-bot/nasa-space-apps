package space.crewpulse.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import space.crewpulse.data.db.ReadingDao
import space.crewpulse.data.model.DemoMission

/**
 * Copies the bundled demo mission into Room once, so demo and user data go through the same code.
 * The last mission day is mapped to today at 08:00 local time.
 */
class DemoSeeder(
    private val assets: AssetRepository,
    private val readingDao: ReadingDao,
    private val baselines: BaselineRepository,
) {
    suspend fun ensureSeeded() {
        if (readingDao.demoCount() > 0) return
        val mission = assets.demoMission()
        readingDao.insertAll(demoReadings(mission, todayMorningMillis()).flatten())
        baselines.saveMissionBaseline(mission.baseline)
    }

    private fun todayMorningMillis(): Long =
        LocalDate.now().atTime(LocalTime.of(8, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** One list of readings per mission day; the last day lands on [lastDayMillis]. */
fun demoReadings(mission: DemoMission, lastDayMillis: Long) =
    mission.days.map { day ->
        val at = lastDayMillis - (mission.days.maxOf { it.day } - day.day) * DAY_MILLIS
        checkInToReadings(day.readings, at, source = "demo")
    }
