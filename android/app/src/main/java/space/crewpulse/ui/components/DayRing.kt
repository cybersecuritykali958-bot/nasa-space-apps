package space.crewpulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import space.crewpulse.ui.theme.StatusColors

/** Progress ring for today's measurements; plays a small burst when the day is complete. */
@Composable
fun DayRing(done: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total == 0) 1f else done.toFloat() / total
    val animated by animateFloatAsState(fraction, tween(900), label = "ring")
    val burst = remember { Animatable(0f) }
    LaunchedEffect(done, total) {
        if (total > 0 && done >= total) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(1_200))
        }
    }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent = MaterialTheme.colorScheme.primary
    val colors = listOf(accent, StatusColors.yellow, StatusColors.green, MaterialTheme.colorScheme.secondary)

    Box(modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(72.dp)) {
            val stroke = Stroke(width = 9f, cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = stroke)
            drawArc(accent, -90f, 360f * animated, false, style = stroke)
            val b = burst.value
            if (b > 0f && b < 1f) {
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0)
                    val r = size.minDimension * (0.5f + 0.35f * b)
                    drawCircle(colors[i % colors.size].copy(alpha = 1f - b), 4f, center + Offset((cos(a) * r).toFloat(), (sin(a) * r).toFloat()))
                }
            }
        }
        Text("$done/$total", style = MaterialTheme.typography.titleMedium)
    }
}
