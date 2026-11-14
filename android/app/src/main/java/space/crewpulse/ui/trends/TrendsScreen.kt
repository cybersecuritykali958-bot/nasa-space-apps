package space.crewpulse.ui.trends

import androidx.compose.runtime.Composable
import space.crewpulse.ui.components.PlaceholderScreen

@Composable
fun TrendsScreen(onBack: () -> Unit) = PlaceholderScreen(
    title = "Trends",
    owner = "@harvio1",
    todo = listOf(
        "Line chart per indicator over the 180-day demo mission",
        "Colored bands for green / yellow / red ranges",
        "Markers for mission events (solar storm, poor sleep, ...)",
    ),
    onBack = onBack,
)
