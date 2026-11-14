package space.crewpulse.ui.pvt

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import space.crewpulse.R
import space.crewpulse.domain.PvtResult
import space.crewpulse.domain.PvtSession
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.theme.CrewColors

private enum class Phase { IDLE, WAITING, STIMULUS, DONE }

/** 3-minute reaction test (PVT-B). The screen owns the clock; PvtSession owns protocol and scoring. */
@Composable
fun PvtScreen(onSave: (PvtResult) -> Boolean, onBack: () -> Unit) {
    var session by remember { mutableStateOf(PvtSession()) }
    var phase by remember { mutableStateOf(Phase.IDLE) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var shownAt by remember { mutableLongStateOf(0L) }
    var counter by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var result by remember { mutableStateOf<PvtResult?>(null) }
    var saved by remember { mutableStateOf<Boolean?>(null) }
    var trial by remember { mutableLongStateOf(0L) } // bumps to schedule the next stimulus

    // Wait a random interval, then show the stimulus.
    LaunchedEffect(phase, trial) {
        if (phase != Phase.WAITING) return@LaunchedEffect
        delay(session.nextDelayMillis())
        elapsed = SystemClock.elapsedRealtime() - startedAt
        if (session.isFinished(elapsed)) {
            result = session.result()
            phase = Phase.DONE
        } else {
            shownAt = SystemClock.elapsedRealtime()
            phase = Phase.STIMULUS
        }
    }
    // Running millisecond counter while the stimulus is shown.
    LaunchedEffect(phase) {
        while (phase == Phase.STIMULUS) {
            withFrameMillis { counter = SystemClock.elapsedRealtime() - shownAt }
        }
    }

    fun onTap() {
        when (phase) {
            Phase.WAITING -> session.recordEarlyTap()
            Phase.STIMULUS -> {
                session.recordResponse(SystemClock.elapsedRealtime() - shownAt)
                phase = Phase.WAITING
                trial++
            }
            else -> Unit
        }
    }

    CrewScaffold(title = stringResource(R.string.pvt_title), onBack = onBack) { pad ->
        Column(
            pad.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (phase) {
                Phase.IDLE -> {
                    Text(stringResource(R.string.pvt_intro), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Button(onClick = {
                        session = PvtSession()
                        startedAt = SystemClock.elapsedRealtime()
                        saved = null
                        phase = Phase.WAITING
                        trial++
                    }) { Text(stringResource(R.string.pvt_start)) }
                }
                Phase.WAITING, Phase.STIMULUS -> {
                    Text(stringResource(R.string.pvt_3min), style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                    LinearProgressIndicator(
                        progress = { (elapsed.toFloat() / PvtSession.DURATION_MILLIS).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        Modifier.weight(1f).fillMaxWidth().clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTap() },
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val active = phase == Phase.STIMULUS
                        Surface(
                            shape = CircleShape,
                            color = if (active) CrewColors.teal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(6.dp, if (active) CrewColors.teal else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.size(220.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (active) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$counter", style = MaterialTheme.typography.displaySmall, color = CrewColors.teal)
                                        Text("ms", color = CrewColors.teal)
                                    }
                                } else {
                                    Text(stringResource(R.string.pvt_wait), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Text(stringResource(R.string.pvt_tap_hint), textAlign = TextAlign.Center)
                }
                Phase.DONE -> {
                    val r = result ?: return@Column
                    Text(stringResource(R.string.pvt_done), style = MaterialTheme.typography.headlineMedium)
                    CrewCard {
                        Row2(stringResource(R.string.pvt_mean), "%.0f ms".format(r.meanReactionMillis))
                        Row2(stringResource(R.string.pvt_lapses), "${r.lapses}")
                        Row2(stringResource(R.string.pvt_false), "${r.falseStarts}")
                        Row2(stringResource(R.string.pvt_trials), "${r.trials}")
                    }
                    when (saved) {
                        true -> Text(stringResource(R.string.pvt_saved), color = MaterialTheme.colorScheme.primary)
                        false -> Text(stringResource(R.string.pvt_unreliable), color = MaterialTheme.colorScheme.error)
                        null -> Button(onClick = { saved = onSave(r) }) { Text(stringResource(R.string.pvt_save)) }
                    }
                    OutlinedButton(onClick = { phase = Phase.IDLE }) { Text(stringResource(R.string.pvt_again)) }
                }
            }
        }
    }
}

@Composable
private fun Row2(label: String, value: String) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
