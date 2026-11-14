package space.crewpulse.ui.twin

import androidx.compose.runtime.Composable
import space.crewpulse.ui.components.PlaceholderScreen

@Composable
fun TwinScreen(onBack: () -> Unit) = PlaceholderScreen(
    title = "Digital Twin",
    owner = "@LoranSent (model), @harvio1 (UI)",
    todo = listOf(
        "Mission day slider: 0 - 900 days",
        "Projected bone density, muscle mass, cumulative radiation dose",
        "Environment switch: ISS / Moon / Mars transit / Mars surface",
        "What-if: skip exercise for 2 weeks",
    ),
    onBack = onBack,
)
