package space.crewpulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import space.crewpulse.data.model.Status

/** Cabin-at-night palette — see docs/DESIGN.md */
private val DeepSpace = Color(0xFF050D1A)
private val CabinPanel = Color(0xFF122440)
private val CabinElevated = Color(0xFF1A3358)
private val AuroraTeal = Color(0xFF5EEAD4)
private val AuroraBlue = Color(0xFF7DD3FC)
private val Starlight = Color(0xFFE8EEF5)
private val Muted = Color(0xFF94A3B8)
private val Outline = Color(0xFF3D5A7A)

private val Scheme = darkColorScheme(
    primary = AuroraTeal,
    onPrimary = Color(0xFF002A27),
    secondary = AuroraBlue,
    onSecondary = Color(0xFF0C2035),
    background = DeepSpace,
    onBackground = Starlight,
    surface = CabinPanel,
    onSurface = Starlight,
    surfaceContainerHigh = CabinElevated,
    onSurfaceVariant = Muted,
    outline = Outline,
    outlineVariant = Color(0xFF2A4260),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
)

object StatusColors {
    val green = Color(0xFF4ADE80)
    val yellow = Color(0xFFFBBF24)
    val red = Color(0xFFF87171)
    val unknown = Color(0xFF94A3B8)

    fun of(status: Status): Color = when (status) {
        Status.GREEN -> green
        Status.YELLOW -> yellow
        Status.RED -> red
        Status.UNKNOWN -> unknown
    }
}

object CrewColors {
    val commDelayBadge = Color(0xFF2563EB)
    val missionAccent = AuroraTeal
}

@Composable
fun CrewPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = CrewTypography,
        content = content,
    )
}