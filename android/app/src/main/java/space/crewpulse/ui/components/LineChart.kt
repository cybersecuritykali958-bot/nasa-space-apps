package space.crewpulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A horizontal line drawn across the chart, e.g. a threshold. */
data class ChartLine(val y: Float, val color: Color)

/**
 * Minimal line chart without extra libraries.
 * [points] are (x, y) in data units; [dashedFromX] draws the rest of the series dashed
 * (forecast or extrapolation); [markerX] draws a vertical marker (selected day).
 */
@Composable
fun LineChart(
    points: List<Pair<Float, Float>>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    lines: List<ChartLine> = emptyList(),
    dashedFromX: Float? = null,
    markerX: Float? = null,
    forecast: Pair<Float, Float>? = null,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (points.size < 2) return@Canvas
        clipRect(right = size.width * reveal.value) {
        val ys = points.map { it.second } + lines.map { it.y } + listOfNotNull(forecast?.second)
        val xs = points.map { it.first } + listOfNotNull(forecast?.first)
        val minX = xs.min()
        val maxX = xs.max().let { if (it == minX) it + 1 else it }
        var minY = ys.min()
        var maxY = ys.max()
        val pad = ((maxY - minY).takeIf { it > 0 } ?: 1f) * 0.1f
        minY -= pad
        maxY += pad
        fun px(x: Float) = (x - minX) / (maxX - minX) * size.width
        fun py(y: Float) = size.height - (y - minY) / (maxY - minY) * size.height

        val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
        lines.forEach { l ->
            drawLine(l.color.copy(alpha = 0.7f), Offset(0f, py(l.y)), Offset(size.width, py(l.y)), strokeWidth = 2f, pathEffect = dash)
        }

        val solid = Path()
        val dashed = Path()
        points.forEachIndexed { i, (x, y) ->
            val inDashed = dashedFromX != null && x >= dashedFromX
            val target = if (inDashed) dashed else solid
            if (i == 0 || (inDashed && dashed.isEmpty)) {
                if (inDashed && i > 0) dashed.moveTo(px(points[i - 1].first), py(points[i - 1].second))
                else target.moveTo(px(x), py(y))
                if (inDashed && i > 0) dashed.lineTo(px(x), py(y))
            } else {
                target.lineTo(px(x), py(y))
            }
        }
        drawPath(solid, color, style = Stroke(width = 3f))
        if (!dashed.isEmpty) drawPath(dashed, color.copy(alpha = 0.8f), style = Stroke(width = 4f, pathEffect = dash))

        forecast?.let { (fx, fy) ->
            val (lx, ly) = points.last()
            drawLine(color.copy(alpha = 0.8f), Offset(px(lx), py(ly)), Offset(px(fx), py(fy)), strokeWidth = 3f, pathEffect = dash)
            drawCircle(color, 6f, Offset(px(fx), py(fy)))
        }
        markerX?.let { m ->
            drawLine(Color.White.copy(alpha = 0.6f), Offset(px(m), 0f), Offset(px(m), size.height), strokeWidth = 2f)
        }
        }
    }
}
