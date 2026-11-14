package space.crewpulse.ui.pvt

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import space.crewpulse.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

const val PVT_DEMO_TRIALS = 8
const val PVT_LAPSE_MS = 500L

enum class PvtPhase {
    Intro, Countdown, Waiting, TapNow, TooEarly, BetweenTrials, Summary,
}

data class PvtUiState(
    val phase: PvtPhase = PvtPhase.Intro,
    val trial: Int = 0,
    val trialsTotal: Int = PVT_DEMO_TRIALS,
    val lastMs: Long? = null,
    val samples: List<Long> = emptyList(),
    val lapses: Int = 0,
    val message: String = "",
)

/** Demo-length PVT; full 3-minute run can replace [PVT_DEMO_TRIALS] later (@jarororoor). */
class PvtViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(PvtUiState())
    val state: StateFlow<PvtUiState> = _state

    private var trialJob: Job? = null
    private var stimulusAt: Long = 0L

    fun start() {
        trialJob?.cancel()
        _state.value = PvtUiState(phase = PvtPhase.Countdown, message = "Get ready…")
        trialJob = viewModelScope.launch {
            delay(1_500)
            runTrial(1)
        }
    }

    fun onScreenTap() {
        val st = _state.value
        when (st.phase) {
            PvtPhase.Waiting -> {
                trialJob?.cancel()
                _state.update { it.copy(phase = PvtPhase.TooEarly, message = "Too soon — wait for the stimulus") }
                viewModelScope.launch {
                    delay(1_200)
                    if (_state.value.phase == PvtPhase.TooEarly) runTrial(st.trial)
                }
            }
            PvtPhase.TapNow -> {
                val ms = SystemClock.elapsedRealtime() - stimulusAt
                val lapse = ms >= PVT_LAPSE_MS
                val samples = st.samples + ms
                _state.update {
                    it.copy(
                        phase = PvtPhase.BetweenTrials,
                        lastMs = ms,
                        samples = samples,
                        lapses = it.lapses + if (lapse) 1 else 0,
                        message = "${ms} ms" + if (lapse) " (lapse)" else "",
                    )
                }
                trialJob = viewModelScope.launch {
                    delay(800)
                    if (st.trial >= st.trialsTotal) finish(samples)
                    else runTrial(st.trial + 1)
                }
            }
            else -> Unit
        }
    }

    fun reset() {
        trialJob?.cancel()
        _state.value = PvtUiState()
    }

    private suspend fun runTrial(index: Int) {
        _state.update {
            it.copy(phase = PvtPhase.Waiting, trial = index, message = "Wait for green… ($index/${it.trialsTotal})")
        }
        delay(Random.nextLong(MIN_DELAY_MS, MAX_DELAY_MS))
        stimulusAt = SystemClock.elapsedRealtime()
        _state.update { it.copy(phase = PvtPhase.TapNow, message = "Tap!") }
    }

    private fun finish(samples: List<Long>) {
        val mean = if (samples.isEmpty()) 0 else samples.average().toLong()
        if (mean > 0) {
            container.manualSession.apply(listOf("pvt_mean_rt" to mean.toDouble()))
        }
        _state.update {
            it.copy(
                phase = PvtPhase.Summary,
                message = "Mean ${mean} ms · lapses ${it.lapses}/${samples.size}",
            )
        }
    }

    companion object {
        private const val MIN_DELAY_MS = 2_000L
        private const val MAX_DELAY_MS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { PvtViewModel(container) }
        }
    }
}