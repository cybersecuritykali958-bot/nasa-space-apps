package space.crewpulse.data

import kotlinx.coroutines.flow.first
import space.crewpulse.data.db.ReadingEntity
import space.crewpulse.domain.EngineInput
import space.crewpulse.domain.EngineReport
import space.crewpulse.domain.HealthEngine
import space.crewpulse.domain.Sample

/** Converts stored readings into engine samples. Shared by the ViewModel and background work. */
fun Map<String, List<ReadingEntity>>.toSamples(): Map<String, List<Sample>> =
    mapValues { (_, list) -> list.map { Sample(it.timestampMillis, it.value, it.source) } }

/** One-shot engine run outside the UI (e.g. reminders), with the same inputs as the Home screen. */
class ReportBuilder(
    private val assets: AssetRepository,
    private val readings: ReadingRepository,
    private val baselines: BaselineRepository,
    private val settings: SettingsStore,
    private val spaceWeather: SpaceWeatherRepository,
) {
    suspend fun build(profile: Profile, nowMillis: Long = System.currentTimeMillis()): EngineReport {
        val history = readings.observeHistory(profile).first().toSamples()
        val environment = assets.environments().firstOrNull { it.id == settings.environmentId.value }
        if (spaceWeather.state.value == null) spaceWeather.load()
        return HealthEngine.analyze(
            EngineInput(
                indicators = assets.indicators(),
                history = history,
                baselines = baselines.observe(profile).first(),
                environment = environment,
                rules = assets.engineRules(),
                nowMillis = nowMillis,
                spaceWeather = spaceWeather.state.value,
            ),
        )
    }
}
