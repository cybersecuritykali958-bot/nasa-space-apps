package space.crewpulse.ui.checkin

import androidx.compose.runtime.Composable
import space.crewpulse.ui.components.PlaceholderScreen

@Composable
fun CheckInScreen(onBack: () -> Unit) = PlaceholderScreen(
    title = "Daily check-in",
    owner = "@harvio1 (UI), @jarororoor (saving to Room)",
    todo = listOf(
        "Form for 6 indicators: BP, heart rate, glucose, temperature, sleep, mood",
        "Short 'how to measure' hint next to each field",
        "Save via ReadingDao.insert, source = \"manual\"",
        "After saving, return to Home with updated statuses",
    ),
    onBack = onBack,
)
