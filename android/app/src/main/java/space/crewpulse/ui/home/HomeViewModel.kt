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
import space.crewpulse.data.reminders.ReminderWorker
import space.crewpulse.data.toSamples
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
import space.crewpulse.domain.DigitalTwin
import space.crewpulse.domain.InputValidator
import space.crewpulse.domain.PvtResult
import space.crewpulse.domain.Reminders
import space.crewpulse.domain.ValidationResult
import space.crewpulse.domain.Sample

/** Kept for screens written against the skeleton, where SystemStatus lived in ui.home. */
typealias SystemStatus = space.crewpulse.domain.SystemStatus

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
    /** Measurement history per indicator, oldest first (for charts). */
    val history: Map<String, List<Sample>> = emptyMap(),
    /** Result of runBenchmark(), null until it ran. */
    val benchmark: EngineBenchmark.Report? = null,
) {
    val systems: List<SystemStatus> get() = report.systems

    /** For the mission banner. */
    val environmentName: String get() = environment?.name.orEmpty()
    val commDelayMin: Double? get() = environment?.commDelayMinOneWay

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

    /**
     * Validates and saves a check-in, e.g. mapOf("heart_rate" to 64.0). Home updates automatically.
     * Returns the validation result right away: show `errors` next to the fields. Nothing is saved
     * unless every value is valid, so the user can fix a typo without a half-saved check-in.
     */
    fun saveCheckIn(values: Map<String, Double>): ValidationResult {
        val s = static ?: return notReady()
        val result = InputValidator.validate(values, s.indicators, s.rules.input)
        if (result.isValid) viewModelScope.launch { container.readings.saveCheckIn(result.accepted) }
        return result
    }

    /** Sets the user's pre-flight baseline for one indicator (e.g. from the pre-flight medical). */
    fun setBaseline(indicatorId: String, value: Double): ValidationResult {
        val s = static ?: return notReady()
        val result = InputValidator.validate(mapOf(indicatorId to value), s.indicators, s.rules.input)
        if (result.isValid) viewModelScope.launch { container.baselines.setManual(indicatorId, value) }
        return result
    }

    /** Back to the automatic baseline (mean of the first check-ins). */
    fun clearBaseline(indicatorId: String) = viewModelScope.launch { container.baselines.clearManual(indicatorId) }

    /** Saves a finished PVT as the "pvt_mean_rt" reading. Unreliable runs (too few responses) are not saved. */
    fun savePvt(result: PvtResult): Boolean {
        if (!result.isReliable) return false
        viewModelScope.launch {
            container.readings.saveCheckIn(mapOf("pvt_mean_rt" to result.meanReactionMillis), source = "pvt")
        }
        return true
    }

    /** Saves a camera (contact PPG) heart rate. Marked as source "camera", so confidence treats it as estimated. */
    fun saveCameraPulse(bpm: Double): Boolean {
        val s = static ?: return false
        val result = InputValidator.validate(mapOf("heart_rate" to bpm), s.indicators, s.rules.input)
        if (!result.isValid) return false
        viewModelScope.launch { container.readings.saveCheckIn(result.accepted, source = "camera") }
        return true
    }

    /** Saves a WHO-5 well-being score (0-100 %). */
    fun saveWho5(percent: Double): Boolean = saveCheckIn(mapOf("who5" to percent)).isValid

    /**
     * Digital Twin projection. Defaults to the current environment for [days] days.
     * For a full mission pass phases, e.g. DigitalTwin.SCENARIOS["mars_mission"].
     */
    fun projectTwin(
        adherence: Double,
        days: Int = 900,
        phases: List<DigitalTwin.Phase>? = null,
    ): List<DigitalTwin.State> {
        val s = static ?: return emptyList()
        val envId = _state.value.environment?.id ?: return emptyList()
        return DigitalTwin.simulate(
            phases = phases ?: listOf(DigitalTwin.Phase(envId, days)),
            environments = s.environments.associateBy { it.id },
            adherence = adherence,
        )
    }

    /** Posts the reminder for the current plan right now (for the demo; needs notification permission). */
    fun sendReminderNow(): Boolean {
        val plan = _state.value.report.plan ?: return false
        val msg = Reminders.message(plan, Long.MAX_VALUE) ?: return false
        ReminderWorker.notify(container.appContext, msg.title, msg.text)
        return true
    }

    private fun notReady() = ValidationResult(emptyMap(), mapOf("_" to "Still loading, try again in a moment."))

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
                val samples = history.toSamples()
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
                    history = samples,
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
