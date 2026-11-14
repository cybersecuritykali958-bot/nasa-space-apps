package space.crewpulse.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Indicators a smartwatch can measure; the rest of the check-in stays manual. */
val WATCH_INDICATORS = listOf("heart_rate", "sleep_hours", "body_temp")

/** What the connected watch reported at sync time. */
data class WatchSnapshot(
    val simulated: Boolean,
    /** Indicator id → value, only for [WATCH_INDICATORS] the watch provided. */
    val values: Map<String, Double>,
    val syncedAtMillis: Long,
)

/** Picks the watch-measurable values out of one set of readings. Pure, for unit tests. */
fun watchValues(readings: Map<String, Double>): Map<String, Double> =
    WATCH_INDICATORS.mapNotNull { id -> readings[id]?.takeIf { it.isFinite() }?.let { id to it } }.toMap()

/**
 * Smartwatch connection for this app session. The connection is shown again on every launch
 * (demo flow), so it is kept in memory only.
 *
 * The demo watch is simulated: it replays the last day of the synthetic demo mission, so the
 * numbers come from the same labeled data set as the demo astronaut and nothing is invented.
 */
class WatchRepository(private val assets: AssetRepository) {

    private val _snapshot = MutableStateFlow<WatchSnapshot?>(null)
    val snapshot: StateFlow<WatchSnapshot?> = _snapshot

    suspend fun connectDemo(nowMillis: Long = System.currentTimeMillis()): WatchSnapshot {
        val lastDay = assets.demoMission().days.maxByOrNull { it.day }
        val snapshot = WatchSnapshot(
            simulated = true,
            values = watchValues(lastDay?.readings.orEmpty()),
            syncedAtMillis = nowMillis,
        )
        _snapshot.value = snapshot
        return snapshot
    }

    fun disconnect() {
        _snapshot.value = null
    }
}
