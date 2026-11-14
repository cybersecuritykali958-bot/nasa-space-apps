package space.crewpulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.crewpulse.AppContainer
import space.crewpulse.data.Profile
import space.crewpulse.data.model.DemoMission
import space.crewpulse.data.model.EngineRules
import space.crewpulse.data.model.Environment
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.Risk
import space.crewpulse.domain.EngineBenchmark
import space.crewpulse.domain.EngineInput
import space.crewpulse.domain.EngineReport
import space.crewpulse.domain.Evaluation
import space.crewpulse.domain.HealthEngine
import space.crewpulse.domain.Sample
import space.crewpulse.domain.SystemStatus

/**
 * Everything the screens need. `report` is the full engine output; see docs/BACKEND.md.
 * Note: the space-weather refresh state lives in `report.spaceWeather.dataSource`.
 */
data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: Profile = Profile.DEMO,
    val astronaut: String = "",
    val missionDay: Int = 0,
    val environment: Environment? = null,
    val environments: List<Environment> = emptyList(),
    val risks: List<Risk> = emptyList(),
    val report: EngineReport = EngineReport.EMPTY,
    /** Result of runBenchmark(), null until it ran. */
    val benchmark: EngineBenchmark.Report? = null,
) {
    val systems: List<SystemStatus> get() = report.systems

    fun evaluation(indicatorId: String): Evaluation? = report.evaluation(indicatorId)
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private var static: Static? = null

    init {
        viewModelScope.launch {
            runCatching {
                container.demoSeeder.ensureSeeded()
                container.spaceWeather.load()
                Static(
                    indicators = container.assets.indicators(),
                    risks = container.assets.risks(),
                    environments = container.assets.environments(),
                    rules = container.assets.engineRules(),
                    mission = container.assets.demoMission(),
                )
            }.onSuccess {
                static = it
                launch { container.spaceWeather.refresh() }
                observe(it)
            }.onFailure { e -> _state.value = HomeUiState(loading = false, error = e.message) }
        }
    }

    /** Switch between the demo astronaut and the user's own data. Saved across launches. */
    fun setProfile(profile: Profile) = container.settings.setProfile(profile)

    /** Switch mission environment (id from environments.json). Changes radiation and triage. */
    fun setEnvironment(id: String) = container.settings.setEnvironment(id)

    /** Saves a check-in, e.g. mapOf("heart_rate" to 64.0). Home updates automatically. */
    fun saveCheckIn(values: Map<String, Double>) = viewModelScope.launch {
        container.readings.saveCheckIn(values)
    }

    /** Fetches fresh NASA DONKI events. Safe to call offline: keeps the cached data. */
    fun refreshSpaceWeather() = viewModelScope.launch { container.spaceWeather.refresh() }

    /** Replays the demo mission and measures early-warning quality (engine self-validation). */
    fun runBenchmark() = viewModelScope.launch {
        val s = static ?: return@launch
        val result = withContext(Dispatchers.Default) { EngineBenchmark.run(s.indicators, s.mission, s.rules) }
        _state.update { it.copy(benchmark = result) }
    }

    private data class Static(
        val indicators: List<Indicator>,
        val risks: List<Risk>,
        val environments: List<Environment>,
        val rules: EngineRules,
        val mission: DemoMission,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observe(s: Static) {
        combine(container.settings.profile, container.settings.environmentId, container.spaceWeather.state) { p, e, w ->
            Triple(p, e, w)
        }.flatMapLatest { (profile, envId, weather) ->
            combine(container.readings.observeHistory(profile), container.baselines.observe(profile)) { history, baselines ->
                val samples = history.mapValues { (_, list) ->
                    list.map { Sample(it.timestampMillis, it.value, it.source) }
                }
                val first = samples.values.flatten().minOfOrNull { it.timestampMillis }
                val last = samples.values.flatten().maxOfOrNull { it.timestampMillis }
                val environment = s.environments.firstOrNull { it.id == envId }
                // Demo data ends on its own last day; real data is judged against the clock.
                val now = if (profile == Profile.DEMO) last ?: System.currentTimeMillis() else System.currentTimeMillis()

                val report = HealthEngine.analyze(
                    EngineInput(s.indicators, samples, baselines, environment, s.rules, now, weather),
                )
                _state.value.copy(
                    loading = false,
                    error = null,
                    profile = profile,
                    astronaut = if (profile == Profile.DEMO) s.mission.astronaut else "You",
                    missionDay = missionDay(first, last),
                    environment = environment,
                    environments = s.environments,
                    risks = s.risks,
                    report = report,
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .catch { e -> _state.update { it.copy(loading = false, error = e.message) } }
            .collect { _state.value = it }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(container) }
        }
    }
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

internal fun missionDay(firstTs: Long?, lastTs: Long?): Int =
    if (firstTs == null || lastTs == null) 0 else ((lastTs - firstTs) / DAY_MILLIS).toInt() + 1
