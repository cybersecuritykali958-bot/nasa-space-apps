package space.crewpulse.ui.twin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import space.crewpulse.AppContainer
import space.crewpulse.data.model.Environment
import space.crewpulse.domain.DigitalTwin

data class TwinUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val missionDay: Float = 1f,
    val maxDay: Int = 180,
    val environments: List<Environment> = emptyList(),
    val selectedEnvId: String = "mars_transit",
    val skipExerciseWeeks: Int = 0,
    val cumulativeDoseMsv: Double = 0.0,
    val bonePct: Double = 100.0,
    val musclePct: Double = 100.0,
    val doseHeadroom: Double = DigitalTwin.CAREER_DOSE_LIMIT_MSV,
) {
    val selectedEnv: Environment? get() = environments.find { it.id == selectedEnvId }
}

class TwinViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(TwinUiState())
    val state: StateFlow<TwinUiState> = _state

    private var doseByDay: Map<Int, Double> = emptyMap()

    init {
        viewModelScope.launch {
            runCatching {
                val mission = container.assets.demoMission()
                val envs = container.assets.environments()
                doseByDay = mission.days.associate { it.day to (it.readings["cumulative_dose"] ?: 0.0) }
                val maxDay = mission.days.maxOf { it.day }
                val initialDay = container.demoDay.day.value ?: maxDay
                TwinUiState(
                    loading = false,
                    missionDay = initialDay.toFloat(),
                    maxDay = maxDay,
                    environments = envs,
                    selectedEnvId = mission.environmentId,
                ).also { applyDay(it, initialDay) }
            }.onSuccess { _state.value = it }
                .onFailure { _state.value = TwinUiState(loading = false, error = it.message) }
        }
        viewModelScope.launch {
            container.demoDay.day.drop(1).collect { override ->
                val d = override ?: _state.value.maxDay
                _state.update { applyDay(it, d).copy(missionDay = d.toFloat()) }
            }
        }
    }

    fun setDay(day: Float) {
        val d = day.toInt().coerceIn(1, _state.value.maxDay)
        container.demoDay.setDay(d)
        _state.update { applyDay(it, d).copy(missionDay = d.toFloat()) }
    }

    fun setEnvironment(id: String) = _state.update { it.copy(selectedEnvId = id) }

    fun setSkipExerciseWeeks(weeks: Int) {
        _state.update { s ->
            applyDay(s, s.missionDay.toInt(), weeks).copy(skipExerciseWeeks = weeks.coerceIn(0, 8))
        }
    }

    private fun applyDay(base: TwinUiState, day: Int, skipWeeks: Int = base.skipExerciseWeeks): TwinUiState {
        val dose = doseByDay[day] ?: 0.0
        return base.copy(
            bonePct = DigitalTwin.boneMassRemainingPct(day, skipWeeks),
            musclePct = DigitalTwin.muscleMassRemainingPct(day, skipWeeks),
            cumulativeDoseMsv = dose,
            doseHeadroom = DigitalTwin.doseHeadroomMsv(dose),
        )
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { TwinViewModel(container) }
        }
    }
}