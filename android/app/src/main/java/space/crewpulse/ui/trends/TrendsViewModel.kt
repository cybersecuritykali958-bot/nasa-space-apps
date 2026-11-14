package space.crewpulse.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import space.crewpulse.AppContainer
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.MissionEvent

private const val TRENDS_DEFAULT_ID = "heart_rate"

private val TREND_INDICATOR_IDS = listOf(
    "heart_rate",
    "sleep_hours",
    "systolic_bp",
    "glucose",
    "body_temp",
    "mood",
)

data class TrendPoint(val day: Int, val value: Double)

data class TrendsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val picker: List<Indicator> = emptyList(),
    val selectedId: String = TRENDS_DEFAULT_ID,
    val indicator: Indicator? = null,
    val points: List<TrendPoint> = emptyList(),
    val events: List<MissionEvent> = emptyList(),
    val missionDays: Int = 0,
)

class TrendsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(TrendsUiState())
    val state: StateFlow<TrendsUiState> = _state

    init {
        viewModelScope.launch {
            runCatching {
                val mission = container.assets.demoMission()
                val byId = container.assets.indicators().associateBy { it.id }
                val picker = TREND_INDICATOR_IDS.mapNotNull { byId[it] }
                require(picker.isNotEmpty()) { "No trend indicators in assets" }
                TrendsUiState(
                    loading = false,
                    picker = picker,
                    selectedId = TRENDS_DEFAULT_ID,
                    events = mission.events,
                    missionDays = mission.days.maxOf { it.day },
                ).also { loadSeries(it, mission.days, byId) }
            }.onSuccess { _state.value = it }
                .onFailure { _state.value = TrendsUiState(loading = false, error = it.message) }
        }
    }

    fun select(id: String) {
        _state.update { current ->
            if (current.selectedId == id) return@update current
            current.copy(selectedId = id, loading = true)
        }
        viewModelScope.launch {
            runCatching {
                val mission = container.assets.demoMission()
                val byId = container.assets.indicators().associateBy { it.id }
                loadSeries(_state.value.copy(selectedId = id), mission.days, byId)
            }.onSuccess { _state.value = it.copy(loading = false) }
                .onFailure {
                    _state.update { s -> s.copy(loading = false, error = it.message) }
                }
        }
    }

    private fun loadSeries(
        base: TrendsUiState,
        days: List<space.crewpulse.data.model.MissionDay>,
        byId: Map<String, Indicator>,
    ): TrendsUiState {
        val indicator = byId[base.selectedId] ?: error("Unknown indicator ${base.selectedId}")
        val points = days.mapNotNull { d ->
            d.readings[indicator.id]?.let { TrendPoint(d.day, it) }
        }.sortedBy { it.day }
        return base.copy(indicator = indicator, points = points, error = null)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { TrendsViewModel(container) }
        }
    }
}