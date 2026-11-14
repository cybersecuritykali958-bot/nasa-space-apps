package space.crewpulse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Demo readings are the ones with source = 'demo'; everything else belongs to the user.
 * `demo` parameters select which of the two sets a query works on.
 */
@Dao
interface ReadingDao {
    @Insert
    suspend fun insert(reading: ReadingEntity): Long

    @Insert
    suspend fun insertAll(readings: List<ReadingEntity>)

    @Query("SELECT * FROM readings WHERE indicatorId = :indicatorId ORDER BY timestampMillis")
    fun observe(indicatorId: String): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE indicatorId = :indicatorId ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun latest(indicatorId: String): ReadingEntity?

    /** Latest reading of every indicator for one profile. */
    @Query(
        """
        SELECT r.* FROM readings r
        INNER JOIN (
            SELECT indicatorId, MAX(timestampMillis) AS maxTs FROM readings
            WHERE (source = 'demo') = :demo
            GROUP BY indicatorId
        ) latest ON r.indicatorId = latest.indicatorId AND r.timestampMillis = latest.maxTs
        WHERE (r.source = 'demo') = :demo
        """,
    )
    fun observeLatest(demo: Boolean): Flow<List<ReadingEntity>>

    /** Full history of one profile, oldest first. */
    @Query("SELECT * FROM readings WHERE (source = 'demo') = :demo ORDER BY timestampMillis")
    fun observeAll(demo: Boolean): Flow<List<ReadingEntity>>

    @Query("SELECT MIN(timestampMillis) FROM readings WHERE (source = 'demo') = :demo")
    fun observeFirstTimestamp(demo: Boolean): Flow<Long?>

    @Query("SELECT COUNT(*) FROM readings WHERE source = 'demo'")
    suspend fun demoCount(): Int

    @Query("DELETE FROM readings WHERE source = :source")
    suspend fun deleteBySource(source: String)
}
