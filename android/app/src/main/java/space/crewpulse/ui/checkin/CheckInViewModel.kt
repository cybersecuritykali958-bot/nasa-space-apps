package space.crewpulse.ui.checkin

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

data class CheckInField(
    val indicator: Indicator,
    val valueText: String = "",
    val hintExpanded: Boolean = false,
    val error: String? = null,
)

data class BloodPressureState(
    val systolic: CheckInField? = null,
    val diastolic: CheckInField? = null,
)

data class CheckInUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val bloodPressure: BloodPressureState = BloodPressureState(),
    val fields: List<CheckInField> = emptyList(),
    val savedMessage: String? = null,
)

class CheckInViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(CheckInUiState())
    val state: StateFlow<CheckInUiState> = _state

    init {
        viewModelScope.launch {
            runCatching {
                val byId = container.assets.indicators().associateBy { it.id }
                val sys = byId[INDICATOR_SYSTOLIC] ?: error("Missing $INDICATOR_SYSTOLIC")
                val dia = byId[INDICATOR_DIASTOLIC] ?: error("Missing $INDICATOR_DIASTOLIC")
                val others = CHECK_IN_INDICATOR_IDS.map { byId[it] ?: error("Missing $it") }
                CheckInUiState(
                    loading = false,
                    bloodPressure = BloodPressureState(CheckInField(sys), CheckInField(dia)),
                    fields = others.map { CheckInField(it) },
                )
            }.onSuccess { _state.value = it }
                .onFailure { _state.value = CheckInUiState(loading = false, loadError = it.message) }
        }
    }

    fun updateSystolic(text: String) = updateBp { it.copy(systolic = it.systolic?.copy(valueText = text, error = null)) }
    fun updateDiastolic(text: String) = updateBp { it.copy(diastolic = it.diastolic?.copy(valueText = text, error = null)) }
    fun toggleSystolicHint() = updateBp { it.copy(systolic = it.systolic?.copy(hintExpanded = it.systolic?.hintExpanded != true)) }
    fun toggleDiastolicHint() = updateBp { it.copy(diastolic = it.diastolic?.copy(hintExpanded = it.diastolic?.hintExpanded != true)) }

    fun updateField(index: Int, text: String) {
        _state.update { s ->
            s.copy(fields = s.fields.mapIndexed { i, f -> if (i == index) f.copy(valueText = text, error = null) else f }, savedMessage = null)
        }
    }

    fun setMood(score: Int) {
        _state.update { s ->
            val idx = s.fields.indexOfFirst { it.indicator.id == INDICATOR_MOOD }
            if (idx < 0) return@update s
            s.copy(fields = s.fields.mapIndexed { i, f -> if (i == idx) f.copy(valueText = score.toString(), error = null) else f }, savedMessage = null)
        }
    }

    fun toggleFieldHint(index: Int) {
        _state.update { s -> s.copy(fields = s.fields.mapIndexed { i, f -> if (i == index) f.copy(hintExpanded = !f.hintExpanded) else f }) }
    }

    fun consumeSavedMessage() = _state.update { it.copy(savedMessage = null) }

    fun submit() {
        val parsed = validate() ?: return
        container.manualSession.apply(parsed)
        _state.update {
            it.copy(savedMessage = "Check-in applied to Home (${parsed.size} readings). Room sync next.")
        }
    }

    private fun validate(): List<Pair<String, Double>>? {
        var ok = true
        var bp = _state.value.bloodPressure
        val sysVal = parsePositive(bp.systolic?.valueText.orEmpty())
        val diaVal = parsePositive(bp.diastolic?.valueText.orEmpty())
        when {
            sysVal == null -> { ok = false; bp = bp.copy(systolic = bp.systolic?.copy(error = "Enter systolic value")) }
            diaVal == null -> { ok = false; bp = bp.copy(diastolic = bp.diastolic?.copy(error = "Enter diastolic value")) }
            sysVal < diaVal -> {
                ok = false
                val msg = "Systolic should be ≥ diastolic"
                bp = bp.copy(systolic = bp.systolic?.copy(error = msg), diastolic = bp.diastolic?.copy(error = msg))
            }
        }
        val updatedFields = _state.value.fields.map { field ->
            val valid = when (field.indicator.id) {
                INDICATOR_MOOD -> parseMood(field.valueText) != null
                else -> parsePositive(field.valueText) != null
            }
            if (!valid) { ok = false; field.copy(error = "Enter a valid value") } else field.copy(error = null)
        }
        _state.update { it.copy(bloodPressure = bp, fields = updatedFields) }
        if (!ok) return null
        return buildList {
            add(INDICATOR_SYSTOLIC to sysVal!!)
            add(INDICATOR_DIASTOLIC to diaVal!!)
            updatedFields.forEach { add(it.indicator.id to parseFieldValue(it)) }
        }
    }

    private fun parseFieldValue(field: CheckInField): Double =
        if (field.indicator.id == INDICATOR_MOOD) parseMood(field.valueText)!! else parsePositive(field.valueText)!!

    private fun updateBp(transform: (BloodPressureState) -> BloodPressureState) {
        _state.update { it.copy(bloodPressure = transform(it.bloodPressure), savedMessage = null) }
    }

    companion object {
        private const val INDICATOR_SYSTOLIC = "systolic_bp"
        private const val INDICATOR_DIASTOLIC = "diastolic_bp"
        private const val INDICATOR_MOOD = "mood"
        private val CHECK_IN_INDICATOR_IDS = listOf("heart_rate", "glucose", "body_temp", "sleep_hours", INDICATOR_MOOD)

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { CheckInViewModel(container) }
        }

        private fun parsePositive(raw: String): Double? {
            val t = raw.trim()
            if (t.isEmpty()) return null
            return t.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
        }

        private fun parseMood(raw: String): Double? =
            raw.trim().toIntOrNull()?.takeIf { it in 1..5 }?.toDouble()
    }
}