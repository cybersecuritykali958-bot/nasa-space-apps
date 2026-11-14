package space.crewpulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import space.crewpulse.AppContainer
import space.crewpulse.data.Profile
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.Risk
import space.crewpulse.domain.Evaluation
import space.crewpulse.domain.HealthSnapshot
import space.crewpulse.domain.SystemStatus

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: Profile = Profile.DEMO,
    val astronaut: String = "",
    val missionDay: Int = 0,
    val systems: List<SystemStatus> = emptyList(),
    val risks: List<Risk> = emptyList(),
) {
    fun evaluation(indicatorId: String): Evaluation? =
        systems.asSequence().flatMap { it.evaluations }.firstOrNull { it.indicator.id == indicatorId }
}

/** Home state: latest readings from Room -> StatusEngine, for the selected profile. */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        viewModelScope.launch {
            runCatching {
                container.demoSeeder.ensureSeeded()
                Static(
                    indicators = container.assets.indicators(),
                    risks = container.assets.risks(),
                    demoName = container.assets.demoMission().astronaut,
                )
            }.onSuccess { observe(it) }
                .onFailure { _state.value = HomeUiState(loading = false, error = it.message) }
        }
    }

    /** Switch between the demo astronaut and the user's own data. Saved across launches. */
    fun setProfile(profile: Profile) = container.settings.setProfile(profile)

    /** Saves a check-in; Home updates automatically through the Room flow. */
    fun saveCheckIn(values: Map<String, Double>) = viewModelScope.launch {
        container.readings.saveCheckIn(values)
    }

    private data class Static(val indicators: List<Indicator>, val risks: List<Risk>, val demoName: String)

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observe(static: Static) {
        container.settings.profile
            .flatMapLatest { profile ->
                combine(
                    container.readings.observeLatest(profile),
                    container.baselines.observe(profile),
                    container.readings.observeFirstTimestamp(profile),
                ) { latest, baselines, firstTs ->
                    val lastTs = latest.values.maxOfOrNull { it.timestampMillis }
                    HomeUiState(
                        loading = false,
                        profile = profile,
                        astronaut = if (profile == Profile.DEMO) static.demoName else "You",
                        missionDay = missionDay(firstTs, lastTs),
                        systems = HealthSnapshot.build(
                            static.indicators,
                            latest.mapValues { it.value.value },
                            baselines,
                        ),
                        risks = static.risks,
                    )
                }
            }
            .catch { _state.value = HomeUiState(loading = false, error = it.message) }
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
