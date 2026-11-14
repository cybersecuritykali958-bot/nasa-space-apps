package space.crewpulse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    @Query("DELETE FROM readings WHERE source = :source")
    suspend fun deleteBySource(source: String)
}
