package space.crewpulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import space.crewpulse.data.model.Status

/** Cabin-at-night palette, matching the CrewPulse mockups (docs/DESIGN.md). */
private val DeepSpace = Color(0xFF060D18)
private val CabinPanel = Color(0xFF0D1A2B)
private val CabinElevated = Color(0xFF12233A)
private val Teal = Color(0xFF2DD4BF)
private val SkyBlue = Color(0xFF7DD3FC)
private val Starlight = Color(0xFFE6EDF5)
private val Muted = Color(0xFF8DA2B8)
private val Outline = Color(0xFF26405E)

private val Scheme = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00201C),
    secondary = SkyBlue,
    onSecondary = Color(0xFF0C2035),
    background = DeepSpace,
    onBackground = Starlight,
    surface = CabinPanel,
    onSurface = Starlight,
    surfaceContainerHigh = CabinElevated,
    onSurfaceVariant = Muted,
    outline = Outline,
    outlineVariant = Color(0xFF1C3350),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
)

object StatusColors {
    val green = Color(0xFF34D399)
    val yellow = Color(0xFFF5A524)
    val red = Color(0xFFF26B6B)
    val unknown = Color(0xFF8DA2B8)

    fun of(status: Status): Color = when (status) {
        Status.GREEN -> green
        Status.YELLOW -> yellow
        Status.RED -> red
        Status.UNKNOWN -> unknown
    }
}

object CrewColors {
    val teal = Teal
    val glow = Teal.copy(alpha = 0.35f)
    val commDelayBadge = Color(0xFF1E3A5F)
    val missionAccent = Teal
}

@Composable
fun CrewPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = CrewTypography, content = content)
}
