package space.crewpulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val environmentName: String = "",
    val commDelayMin: Double? = null,
    val systems: List<SystemStatus> = emptyList(),
    val risks: List<Risk> = emptyList(),
) {
    fun evaluation(indicatorId: String): Evaluation? =
        systems.asSequence().flatMap { it.evaluations }.firstOrNull { it.indicator.id == indicatorId }
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        runCatching {
            val indicators = container.assets.indicators()
            val mission = container.assets.demoMission()
            val risks = container.assets.risks()
            val env = container.assets.environments().find { it.id == mission.environmentId }
            val today = mission.days.maxBy { it.day }

            val evaluations = indicators.mapNotNull { indicator ->
                today.readings[indicator.id]?.let { value ->
                    StatusEngine.evaluate(indicator, value, mission.baseline[indicator.id])
                }
            }
            val systems = evaluations.groupBy { it.indicator.system }.map { (system, evals) ->
                SystemStatus(system, StatusEngine.worst(evals.map { it.status }), evals)
            }
            HomeUiState(
                loading = false,
                astronaut = mission.astronaut,
                missionDay = today.day,
                environmentName = env?.name ?: mission.environmentId,
                commDelayMin = env?.commDelayMinOneWay,
                systems = systems.sortedBy { it.systemId },
                risks = risks,
            )
        }.onSuccess { _state.value = it }
            .onFailure { _state.value = HomeUiState(loading = false, error = it.message) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(container) }
        }
    }
}