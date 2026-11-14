package space.crewpulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import space.crewpulse.data.model.Status

/** Calm Material 3 dark palette: graphite surfaces, one soft teal accent, desaturated statuses. */
private val Graphite = Color(0xFF1B1E21)
private val Card = Color(0xFF24272B)
private val CardHigh = Color(0xFF2B2F34)
private val Teal = Color(0xFF6BD3C3)
private val Text = Color(0xFFE3E5E8)
private val TextMuted = Color(0xFF9AA0A6)
private val Line = Color(0xFF363B41)

private val Scheme = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF0D2B27),
    primaryContainer = Color(0xFF1F3B37),
    onPrimaryContainer = Teal,
    secondary = Color(0xFF9CC9E8),
    onSecondary = Color(0xFF0E2433),
    background = Graphite,
    onBackground = Text,
    surface = Graphite,
    onSurface = Text,
    surfaceContainer = Card,
    surfaceContainerHigh = Card,
    surfaceContainerHighest = CardHigh,
    surfaceVariant = CardHigh,
    onSurfaceVariant = TextMuted,
    outline = Line,
    outlineVariant = Color(0xFF2E3338),
    error = Color(0xFFE58A8A),
    onError = Color(0xFF3B1414),
)

object StatusColors {
    val green = Color(0xFF7FC8A0)
    val yellow = Color(0xFFE6B566)
    val red = Color(0xFFE58A8A)
    val unknown = Color(0xFF9AA0A6)

    /** Dark text for filled status chips. */
    val onChip = Color(0xFF1B1E21)

    fun of(status: Status): Color = when (status) {
        Status.GREEN -> green
        Status.YELLOW -> yellow
        Status.RED -> red
        Status.UNKNOWN -> unknown
    }
}

object CrewColors {
    val teal = Teal
    val glow = Teal.copy(alpha = 0.18f)
    val commDelayBadge = CardHigh
    val missionAccent = Teal
}

@Composable
fun CrewPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = CrewTypography, content = content)
}
