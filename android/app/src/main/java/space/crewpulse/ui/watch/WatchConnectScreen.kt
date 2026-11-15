package space.crewpulse.ui.watch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import space.crewpulse.R
import space.crewpulse.data.WatchRepository
import space.crewpulse.data.WatchSnapshot
import space.crewpulse.data.model.Status
import space.crewpulse.ui.checkin.CheckInViewModel
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.SpaceBackground
import space.crewpulse.ui.i18n.indicatorName

private enum class Phase { INTRO, SCANNING, DOCKING, CONNECTED }

/**
 * Smartwatch connection flow: intro → radar search → docking → live signal and synced values.
 * Shown on every app launch for the demo; "Enter manually" skips it and nothing is read from a watch.
 */
@Composable
fun WatchConnectScreen(watch: WatchRepository, onDone: () -> Unit, drawBackground: Boolean = true) {
    var phase by rememberSaveable { mutableStateOf(Phase.INTRO) }
    val snapshot by watch.snapshot.collectAsStateWithLifecycle()
    val skip = {
        watch.disconnect()
        onDone()
    }

    Box(Modifier.fillMaxSize()) {
        if (drawBackground) SpaceBackground(Status.GREEN)
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "watch-phase",
            ) { p ->
                when (p) {
                    Phase.INTRO -> IntroPhase()
                    Phase.SCANNING -> ScanningPhase(onDevice = { phase = Phase.DOCKING })
                    Phase.DOCKING -> DockingPhase(watch, onDocked = { phase = Phase.CONNECTED })
                    Phase.CONNECTED -> ConnectedPhase(snapshot)
                }
            }
            when (phase) {
                Phase.INTRO -> {
                    PrimaryButton(stringResource(R.string.watch_connect)) { phase = Phase.SCANNING }
                    SkipButton(skip)
                }
                Phase.SCANNING, Phase.DOCKING -> SkipButton(skip)
                Phase.CONNECTED -> PrimaryButton(stringResource(R.string.watch_continue), onDone)
            }
        }
    }
}

@Composable
private fun IntroPhase() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WatchHero(Modifier.size(240.dp))
        Title(stringResource(R.string.watch_title))
        Text(
            stringResource(R.string.watch_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScanningPhase(onDevice: () -> Unit) {
    var found by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        delay(2_200)
        found = true
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RadarScan(found, Modifier.size(260.dp))
        Text(
            stringResource(if (found) R.string.watch_tap_device else R.string.watch_scanning),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        AnimatedVisibility(found, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }) {
            DeviceCard(onDevice)
        }
    }
}

@Composable
private fun DeviceCard(onClick: () -> Unit) {
    CrewCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Watch, null, tint = MaterialTheme.colorScheme.primary) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.watch_device_name), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.watch_simulated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.Bluetooth, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DockingPhase(watch: WatchRepository, onDocked: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        watch.connectDemo()
        progress.animateTo(1f, tween(2_800, easing = FastOutSlowInEasing))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(900)
        onDocked()
    }
    val p = progress.value
    val stage = when {
        p < 0.55f -> R.string.watch_stage_approach
        p < 1f -> R.string.watch_stage_capture
        else -> R.string.watch_stage_docked
    }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DockingScene(p, Modifier.fillMaxWidth().height(320.dp))
        AnimatedContent(stage, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "stage") { s ->
            Text(
                stringResource(s),
                style = MaterialTheme.typography.titleLarge,
                color = if (s == R.string.watch_stage_docked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ConnectedPhase(snapshot: WatchSnapshot?) {
    val values = snapshot?.values.orEmpty().toList()
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(values.size) {
        delay(600)
        while (shown < values.size) {
            shown++
            delay(450)
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Title(stringResource(R.string.watch_connected))
        }
        LiveSignalCard(snapshot?.values?.get("heart_rate"))
        Text(
            stringResource(R.string.watch_synced),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        values.forEachIndexed { i, (id, value) ->
            AnimatedVisibility(i < shown, enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 2 }) {
                SyncedRow(id, value)
            }
        }
        if (snapshot?.simulated == true) {
            Text(
                stringResource(R.string.watch_demo_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveSignalCard(heartRate: Double?) {
    // The trace needs a rate to scroll at; without a watch value it only animates, no number is shown.
    val bpm = heartRate ?: 60.0
    val clock = rememberFrameClock()
    CrewCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Filled.Favorite, null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(30.dp).graphicsLayer {
                    val s = 1f + 0.25f * beatKick(clock.value, bpm)
                    scaleX = s
                    scaleY = s
                },
            )
            if (heartRate != null) {
                Text(
                    CheckInViewModel.formatWatchValue("heart_rate", heartRate),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stringResource(R.string.watch_bpm), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.watch_live_signal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        EcgTrace(bpm, clock, Modifier.fillMaxWidth().height(110.dp))
    }
}

@Composable
private fun SyncedRow(id: String, value: Double) {
    val (icon, format) = METRIC_UI[id] ?: return
    CrewCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
            Text(indicatorName(id, id), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                stringResource(format, CheckInViewModel.formatWatchValue(id, value)),
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

private val METRIC_UI: Map<String, Pair<ImageVector, Int>> = mapOf(
    "heart_rate" to (Icons.Filled.Favorite to R.string.watch_value_heart_rate),
    "sleep_hours" to (Icons.Filled.Bedtime to R.string.watch_value_sleep_hours),
    "body_temp" to (Icons.Filled.Thermostat to R.string.watch_value_body_temp),
)

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(28.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(R.string.watch_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
