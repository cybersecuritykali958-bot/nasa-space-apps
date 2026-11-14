package space.crewpulse.ui.pvt

import androidx.compose.runtime.Composable
import space.crewpulse.ui.components.PlaceholderScreen

@Composable
fun PvtScreen(onBack: () -> Unit) = PlaceholderScreen(
    title = "Reaction test (PVT)",
    owner = "@jarororoor (timing logic), @harvio1 (UI)",
    todo = listOf(
        "3-minute psychomotor vigilance test, random 2-10 s delays",
        "Measure reaction time in ms, count lapses (> 500 ms)",
        "Compare with personal baseline, save as indicator \"pvt_mean_rt\"",
    ),
    onBack = onBack,
)
