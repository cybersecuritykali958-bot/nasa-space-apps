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
import java.util.Locale
import kotlin.math.roundToInt
import space.crewpulse.AppContainer
import space.crewpulse.data.Profile
import space.crewpulse.data.model.Indicator
import space.crewpulse.domain.InputValidator
import space.crewpulse.domain.Who5

data class CheckInField(
    val indicator: Indicator,
    val valueText: String = "",
    val hintExpanded: Boolean = false,
    val error: String? = null,
    /** Value was filled in from the connected watch and not edited since. */
    val fromWatch: Boolean = false,
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
    /** Optional WHO-5 well-being test, opened from the mood card. */
    val who5Expanded: Boolean = false,
    val who5Answers: List<Int?> = List(Who5.ITEMS) { null },
    /** The test is open and only partly answered: finish it or close it before saving. */
    val who5Incomplete: Boolean = false,
) {
    val who5Score: Double? get() = Who5.percent(who5Answers)
}

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
                // Values from the connected watch are pre-filled; everything else stays manual.
                val fromWatch = container.watch.snapshot.value?.values.orEmpty()
                CheckInUiState(
                    loading = false,
                    bloodPressure = BloodPressureState(CheckInField(sys), CheckInField(dia)),
                    fields = others.map { ind ->
                        fromWatch[ind.id]?.let { CheckInField(ind, valueText = formatWatchValue(ind.id, it), fromWatch = true) }
                            ?: CheckInField(ind)
                    },
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
            s.copy(fields = s.fields.mapIndexed { i, f -> if (i == index) f.copy(valueText = text, error = null, fromWatch = false) else f }, savedMessage = null)
        }
    }

    fun setMood(score: Int) {
        _state.update { s ->
            val idx = s.fields.indexOfFirst { it.indicator.id == INDICATOR_MOOD }
            if (idx < 0) return@update s
            s.copy(fields = s.fields.mapIndexed { i, f -> if (i == idx) f.copy(valueText = score.toString(), error = null) else f }, savedMessage = null)
        }
    }

    fun toggleWho5() = _state.update { it.copy(who5Expanded = !it.who5Expanded, who5Incomplete = false) }

    fun setWho5Answer(item: Int, value: Int) = _state.update { s ->
        s.copy(who5Answers = s.who5Answers.mapIndexed { i, v -> if (i == item) value else v }, who5Incomplete = false, savedMessage = null)
    }

    fun toggleFieldHint(index: Int) {
        _state.update { s -> s.copy(fields = s.fields.mapIndexed { i, f -> if (i == index) f.copy(hintExpanded = !f.hintExpanded) else f }) }
    }

    fun consumeSavedMessage() = _state.update { it.copy(savedMessage = null) }

    fun submit() {
        val s0 = _state.value
        val answered = s0.who5Answers.count { it != null }
        if (s0.who5Expanded && answered in 1 until Who5.ITEMS) {
            _state.update { it.copy(who5Incomplete = true) }
            return
        }
        // A finished WHO-5 is saved with the check-in; an untouched or closed test is skipped.
        val values = (validate() ?: return) + listOfNotNull(s0.who5Score?.let { "who5" to it })
        viewModelScope.launch {
            // Backend validation (plausibility limits), then save to Room and show the user's own data.
            val result = InputValidator.validate(values.toMap(), container.assets.indicators(), container.assets.engineRules().input)
            if (!result.isValid) {
                _state.update { s ->
                    s.copy(
                        bloodPressure = s.bloodPressure.copy(
                            systolic = s.bloodPressure.systolic?.let { it.copy(error = result.errors[it.indicator.id] ?: it.error) },
                            diastolic = s.bloodPressure.diastolic?.let { it.copy(error = result.errors[it.indicator.id] ?: it.error) },
                        ),
                        fields = s.fields.map { f -> f.copy(error = result.errors[f.indicator.id] ?: f.error) },
                    )
                }
                return@launch
            }
            // Watch values keep source "watch" so their origin stays visible in the data.
            val watchIds = _state.value.fields.filter { it.fromWatch }.map { it.indicator.id }.toSet()
            val at = System.currentTimeMillis()
            val (watch, manual) = result.accepted.entries.partition { it.key in watchIds }
            container.readings.saveCheckIn(manual.associate { it.key to it.value }, at, source = "manual")
            if (watch.isNotEmpty()) container.readings.saveCheckIn(watch.associate { it.key to it.value }, at, source = "watch")
            container.settings.setProfile(Profile.ME)
            _state.update { it.copy(savedMessage = SAVED) }
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
        /** Marker for the screen, which shows the translated "saved" message. */
        const val SAVED = "saved"
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

        /** Watches show whole beats per minute; other values keep one decimal. */
        internal fun formatWatchValue(id: String, value: Double): String =
            if (id == "heart_rate") value.roundToInt().toString() else String.format(Locale.US, "%.1f", value)

        private fun parseMood(raw: String): Double? =
            raw.trim().toIntOrNull()?.takeIf { it in 1..5 }?.toDouble()
    }
}