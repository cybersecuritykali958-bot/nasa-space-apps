package space.crewpulse.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import space.crewpulse.data.db.BaselineDao
import space.crewpulse.data.db.BaselineEntity
import space.crewpulse.domain.BaselineCalculator

class BaselineRepository(
    private val dao: BaselineDao,
    private val readings: ReadingRepository,
) {
    /**
     * Baseline per indicator.
     * DEMO: values stored from the mission file.
     * ME: manual value if set, otherwise the mean of the first check-ins.
     */
    fun observe(profile: Profile): Flow<Map<String, Double>> {
        val stored = dao.observe(profile.name).map { list -> list.associate { it.indicatorId to it.value } }
        if (profile == Profile.DEMO) return stored
        return combine(stored, readings.observeHistory(Profile.ME)) { manual, history ->
            BaselineCalculator.fromFirstReadings(history.mapValues { (_, list) -> list.map { it.value } }) + manual
        }
    }

    suspend fun setManual(indicatorId: String, value: Double) {
        dao.upsert(listOf(BaselineEntity(Profile.ME.name, indicatorId, value, "manual")))
    }

    suspend fun clearManual(indicatorId: String) {
        dao.delete(Profile.ME.name, indicatorId)
    }

    internal suspend fun saveMissionBaseline(values: Map<String, Double>) {
        dao.upsert(values.map { (id, v) -> BaselineEntity(Profile.DEMO.name, id, v, "mission") })
    }
}
