package space.crewpulse.ui.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import space.crewpulse.data.model.Band
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.MissionEvent
import space.crewpulse.ui.theme.StatusColors
import kotlin.math.max
import kotlin.math.min

@Composable
fun TrendChart(
    indicator: Indicator,
    points: List<TrendPoint>,
    events: List<MissionEvent>,
    missionDays: Int,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val eventColor = MaterialTheme.colorScheme.secondary
    val (yMin, yMax) = yExtents(indicator, points)

    Canvas(modifier.fillMaxWidth().height(220.dp)) {
        val padL = 8f
        val padR = 8f
        val padT = 12f
        val padB = 20f
        val w = size.width - padL - padR
        val h = size.height - padT - padB

        fun xAt(day: Int): Float = padL + (day - 1f) / max(1, missionDays - 1) * w
        fun yAt(value: Double): Float {
            val t = ((value - yMin) / (yMax - yMin)).toFloat().coerceIn(0f, 1f)
            return padT + (1f - t) * h
        }

        indicator.bands.forEach { band ->
            drawBand(band, yMin, yMax, padT, h, padL, w)
        }

        if (points.size >= 2) {
            val path = Path()
            points.forEachIndexed { i, p ->
                val pt = Offset(xAt(p.day), yAt(p.value))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 4f))
        }
        points.forEach { p ->
            drawCircle(
                color = lineColor,
                radius = 5f,
                center = Offset(xAt(p.day), yAt(p.value)),
            )
        }

        events.forEach { ev ->
            val x = xAt(ev.day)
            drawLine(eventColor, Offset(x, padT), Offset(x, padT + h), strokeWidth = 2f)
        }

        drawLine(gridColor, Offset(padL, padT + h), Offset(padL + w, padT + h), strokeWidth = 1f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBand(
    band: Band,
    yMin: Double,
    yMax: Double,
    padT: Float,
    h: Float,
    padL: Float,
    w: Float,
) {
    val low = band.min ?: yMin
    val high = band.max ?: yMax
    if (high <= low) return
    fun yAt(value: Double): Float {
        val t = ((value - yMin) / (yMax - yMin)).toFloat().coerceIn(0f, 1f)
        return padT + (1f - t) * h
    }
    val top = yAt(high)
    val bottom = yAt(low)
    drawRect(
        color = StatusColors.of(band.status).copy(alpha = 0.22f),
        topLeft = Offset(padL, min(top, bottom)),
        size = androidx.compose.ui.geometry.Size(w, kotlin.math.abs(bottom - top)),
    )
}

private fun yExtents(indicator: Indicator, points: List<TrendPoint>): Pair<Double, Double> {
    val values = points.map { it.value }
    val bandMins = indicator.bands.mapNotNull { it.min }
    val bandMaxs = indicator.bands.mapNotNull { it.max }
    var minV = listOfNotNull(values.minOrNull(), bandMins.minOrNull()).minOrNull() ?: 0.0
    var maxV = listOfNotNull(values.maxOrNull(), bandMaxs.maxOrNull()).maxOrNull() ?: 1.0
  if (maxV <= minV) maxV = minV + 1.0
    val pad = (maxV - minV) * 0.08
    return minV - pad to maxV + pad
}