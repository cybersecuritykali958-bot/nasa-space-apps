package space.crewpulse.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import space.crewpulse.data.db.ReadingDao
import space.crewpulse.data.db.ReadingEntity

/** Entry point for saving and reading measurements. UI code should call this, not the DAO. */
class ReadingRepository(private val dao: ReadingDao) {

    /** Saves one check-in: all values share the same timestamp. */
    suspend fun saveCheckIn(
        values: Map<String, Double>,
        atMillis: Long = System.currentTimeMillis(),
        source: String = "manual",
    ) {
        dao.insertAll(checkInToReadings(values, atMillis, source))
    }

    /** Latest value per indicator for the given profile. */
    fun observeLatest(profile: Profile): Flow<Map<String, ReadingEntity>> =
        dao.observeLatest(profile == Profile.DEMO).map { list -> list.associateBy { it.indicatorId } }

    /** Full history grouped by indicator, oldest first. */
    fun observeHistory(profile: Profile): Flow<Map<String, List<ReadingEntity>>> =
        dao.observeAll(profile == Profile.DEMO).map { list -> list.groupBy { it.indicatorId } }

    fun observeFirstTimestamp(profile: Profile): Flow<Long?> =
        dao.observeFirstTimestamp(profile == Profile.DEMO)
}

/** Pure mapping, kept outside the class so it can be unit-tested without a database. */
fun checkInToReadings(values: Map<String, Double>, atMillis: Long, source: String = "manual"): List<ReadingEntity> =
    values.filterValues { it.isFinite() }.map { (id, value) ->
        ReadingEntity(indicatorId = id, value = value, timestampMillis = atMillis, source = source)
    }
