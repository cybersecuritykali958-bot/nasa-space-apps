package space.crewpulse.ui.pvt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.theme.StatusColors

@Composable
fun PvtScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: PvtViewModel = viewModel(factory = PvtViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    LaunchedEffect(state.phase) {
        when (state.phase) {
            PvtPhase.TapNow -> view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            PvtPhase.TooEarly -> view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            PvtPhase.Summary -> view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            else -> Unit
        }
    }

    CrewScaffold(
        title = "Reaction test (PVT)",
        subtitle = "Psychomotor vigilance · demo $PVT_DEMO_TRIALS trials",
        onBack = onBack,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .then(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CrewCard {
                Text(
                    "Tap only when the panel turns green. Early taps reset the trial. Lapse = slower than $PVT_LAPSE_MS ms.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val panelColor = when (state.phase) {
                PvtPhase.TapNow -> StatusColors.green
                PvtPhase.Waiting, PvtPhase.Countdown -> MaterialTheme.colorScheme.surfaceContainerHigh
                PvtPhase.TooEarly -> StatusColors.red
                PvtPhase.Summary -> StatusColors.yellow
                else -> MaterialTheme.colorScheme.background
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 200.dp)
                    .background(panelColor.copy(alpha = if (state.phase == PvtPhase.Intro) 0.5f else 0.85f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = vm::onScreenTap,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when (state.phase) {
                        PvtPhase.Intro -> "Press Start"
                        PvtPhase.Summary -> state.message
                        else -> state.message
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
            if (state.phase == PvtPhase.Intro) {
                Button(onClick = vm::start, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("Start test")
                }
            } else if (state.phase == PvtPhase.Summary) {
                Button(onClick = vm::reset, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("Run again")
                }
                state.samples.takeIf { it.isNotEmpty() }?.let { samples ->
                    val mean = samples.average().toLong()
                    Text(
                        "Demo mean: $mean ms — applied to dashboard (Room later)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}