package space.crewpulse.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BaselineDao {
    @Upsert
    suspend fun upsert(baselines: List<BaselineEntity>)

    @Query("SELECT * FROM baselines WHERE profile = :profile")
    fun observe(profile: String): Flow<List<BaselineEntity>>

    @Query("DELETE FROM baselines WHERE profile = :profile AND indicatorId = :indicatorId")
    suspend fun delete(profile: String, indicatorId: String)
}
