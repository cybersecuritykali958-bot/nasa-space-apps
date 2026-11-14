package space.crewpulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import space.crewpulse.data.model.Status

// Placeholder palette. Design owns the final colors (docs/DESIGN.md).
private val Night = Color(0xFF0B1D3A)
private val Panel = Color(0xFF13294B)
private val Teal = Color(0xFF4FD1C5)

private val Scheme = darkColorScheme(
    primary = Teal,
    background = Night,
    surface = Panel,
    surfaceVariant = Panel,
)

object StatusColors {
    val green = Color(0xFF38A169)
    val yellow = Color(0xFFD69E2E)
    val red = Color(0xFFE53E3E)
    val unknown = Color(0xFF718096)

    fun of(status: Status): Color = when (status) {
        Status.GREEN -> green
        Status.YELLOW -> yellow
        Status.RED -> red
        Status.UNKNOWN -> unknown
    }
}

@Composable
fun CrewPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
