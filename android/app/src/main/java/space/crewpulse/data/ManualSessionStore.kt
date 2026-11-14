package space.crewpulse.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory check-in values until Room persistence lands (@jarororoor). */
class ManualSessionStore {
    private val _readings = MutableStateFlow<Map<String, Double>>(emptyMap())
    val readings: StateFlow<Map<String, Double>> = _readings.asStateFlow()

    fun apply(entries: List<Pair<String, Double>>) {
        if (entries.isEmpty()) return
        _readings.value = _readings.value + entries.toMap()
    }
}