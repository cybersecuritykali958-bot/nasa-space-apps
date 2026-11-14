package space.crewpulse.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lets demos jump to mission days (e.g. day 92 solar event). */
class DemoMissionDayStore {
    private val _day = MutableStateFlow<Int?>(null)
    val day: StateFlow<Int?> = _day.asStateFlow()

    fun setDay(day: Int?) {
        _day.value = day
    }
}