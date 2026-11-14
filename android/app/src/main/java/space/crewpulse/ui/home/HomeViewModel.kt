package space.crewpulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import space.crewpulse.AppContainer
import space.crewpulse.data.model.Risk
import space.crewpulse.data.model.Status
import space.crewpulse.domain.Evaluation
import space.crewpulse.domain.StatusEngine

data class SystemStatus(val systemId: String, val status: Status, val evaluations: List<Evaluation>)

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val astronaut: String = "",
    val missionDay: Int = 0,
    val demoDaySelection: Int? = null,
    val environmentName: String = "",
    val commDelayMin: Double? = null,
    val systems: List<SystemStatus> = emptyList(),
    val risks: List<Risk> = emptyList(),
    val alerts: List<String> = emptyList(),
    val includesManualCheckIn: Boolean = false,
    val refreshing: Boolean = false,
) {
    fun evaluation(indicatorId: String): Evaluation? =
        systems.asSequence().flatMap { it.evaluations }.firstOrNull { it.indicator.id == indicatorId }
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        refresh()
        viewModelScope.launch {
            container.manualSession.readings.drop(1).collect { refresh() }
        }
        viewModelScope.launch {
            container.demoDay.day.drop(1).collect { refresh() }
        }
    }

    fun refresh() = viewModelScope.launch {
        val hadData = _state.value.systems.isNotEmpty()
        _state.update {
            it.copy(loading = !hadData, refreshing = hadData)
        }
        runCatching {
            val indicators = container.assets.indicators()
            val mission = container.assets.demoMission()
            val risks = container.assets.risks()
            val env = container.assets.environments().find { it.id == mission.environmentId }
            val dayOverride = container.demoDay.day.value
            val today = dayOverride
                ?.let { d -> mission.days.find { it.day == d } }
                ?: mission.days.maxBy { it.day }
            val manual = container.manualSession.readings.value
            val mergedReadings = today.readings + manual

            val evaluations = indicators.mapNotNull { indicator ->
                mergedReadings[indicator.id]?.let { value ->
                    StatusEngine.evaluate(indicator, value, mission.baseline[indicator.id])
                }
            }
            val systems = evaluations.groupBy { it.indicator.system }.map { (system, evals) ->
                SystemStatus(system, StatusEngine.worst(evals.map { it.status }), evals)
            }
            val alerts = buildList {
                mission.events.filter { it.day == today.day }.forEach { add(it.note) }
                mission.events
                    .filter { it.type == "solar_particle_event" && today.day >= it.day }
                    .maxByOrNull { it.day }
                    ?.let { add("Radiation watch (since day ${it.day}): ${it.note}") }
            }
            HomeUiState(
                loading = false,
                astronaut = mission.astronaut,
                missionDay = today.day,
                demoDaySelection = dayOverride,
                environmentName = env?.name ?: mission.environmentId,
                commDelayMin = env?.commDelayMinOneWay,
                systems = systems.sortedBy { it.systemId },
                risks = risks,
                alerts = alerts,
                includesManualCheckIn = manual.isNotEmpty(),
                refreshing = false,
            )
        }.onSuccess { _state.value = it }
            .onFailure {
                _state.value = _state.value.copy(loading = false, refreshing = false, error = it.message)
            }
    }

    fun setDemoDay(day: Int?) {
        container.demoDay.setDay(day)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(container) }
        }
    }
}