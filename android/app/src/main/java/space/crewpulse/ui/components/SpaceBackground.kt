package space.crewpulse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import space.crewpulse.data.model.Status
import space.crewpulse.ui.theme.CrewColors
import space.crewpulse.ui.theme.StatusColors

/**
 * Quiet animated backdrop: a few slowly drifting, twinkling stars and two soft glows whose
 * color follows the overall health status (teal when fine, amber or red when not).
 * Everything stays at low opacity so text keeps full contrast.
 */
@Composable
fun SpaceBackground(status: Status, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bg")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(90_000, easing = LinearEasing)), label = "drift")
    val twinkle by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "twinkle")
    val sway by t.animateFloat(0f, 1f, infiniteRepeatable(tween(30_000), RepeatMode.Reverse), label = "sway")
    val glow by animateColorAsState(
        when (status) {
            Status.YELLOW -> StatusColors.yellow
            Status.RED -> StatusColors.red
            else -> CrewColors.teal
        },
        tween(1_500),
        label = "glow",
    )

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Two soft glows, slowly moving.
        val a = Offset(w * (0.15f + 0.2f * sway), h * (0.12f + 0.06f * sin(sway * 6.28f)))
        val b = Offset(w * (0.9f - 0.25f * sway), h * (0.75f + 0.05f * cos(sway * 6.28f)))
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.09f), Color.Transparent), a, w * 0.75f), w * 0.75f, a)
        drawCircle(Brush.radialGradient(listOf(CrewColors.teal.copy(alpha = 0.05f), Color.Transparent), b, w * 0.8f), w * 0.8f, b)

        // Stars drifting slowly upward; positions are fixed per star so nothing jumps.
        for (i in 0 until 55) {
            val x = ((i * 73 + 17) % 101) / 101f * w
            val baseY = ((i * 37 + 11) % 97) / 97f
            val speed = 0.5f + (i % 3) * 0.25f
            val y = ((baseY - drift * speed) % 1f + 1f) % 1f * h
            val tw = 0.10f + 0.20f * ((sin(twinkle * 6.283f + i * 1.3f) + 1f) / 2f)
            drawCircle(Color.White.copy(alpha = tw), if (i % 7 == 0) 2.0f else 1.2f, Offset(x, y))
        }
    }
}
