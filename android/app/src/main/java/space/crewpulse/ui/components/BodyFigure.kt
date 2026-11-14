package space.crewpulse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import space.crewpulse.data.model.Status
import space.crewpulse.ui.theme.CrewColors
import space.crewpulse.ui.theme.StatusColors

/**
 * Stylized glowing body silhouette. Each body system is drawn as a zone colored by its status:
 * brain (behavioral), eyes (vision), heart (cardiovascular), abdomen (metabolic), lymph nodes
 * (immune), legs and spine (musculoskeletal) and a halo around the figure (radiation).
 * [skeleton] draws bones instead of zones, for the Digital Twin.
 */
@Composable
fun BodyFigure(
    statuses: Map<String, Status>,
    modifier: Modifier = Modifier,
    skeleton: Boolean = false,
) {
    Canvas(modifier.fillMaxWidth().aspectRatio(0.62f)) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val line = CrewColors.teal
        val stroke = Stroke(width = w * 0.012f)

        // Radiation halo.
        val halo = statuses["radiation"]
        if (halo != null && !skeleton) {
            drawOval(
                color = StatusColors.of(halo).copy(alpha = 0.55f),
                topLeft = Offset(cx - w * 0.42f, h * 0.02f),
                size = Size(w * 0.84f, h * 0.96f),
                style = Stroke(width = w * 0.01f),
            )
        }

        // Soft body glow.
        drawOval(
            brush = Brush.radialGradient(listOf(CrewColors.glow, Color.Transparent), center = Offset(cx, h * 0.42f), radius = w * 0.5f),
            topLeft = Offset(cx - w * 0.45f, 0f),
            size = Size(w * 0.9f, h),
        )

        // Head, neck, torso, arms, legs.
        drawCircle(line, radius = w * 0.075f, center = Offset(cx, h * 0.09f), style = stroke)
        drawLine(line, Offset(cx, h * 0.15f), Offset(cx, h * 0.19f), strokeWidth = stroke.width)
        drawRoundRect(
            color = line, topLeft = Offset(cx - w * 0.15f, h * 0.19f), size = Size(w * 0.30f, h * 0.33f),
            cornerRadius = CornerRadius(w * 0.08f), style = stroke,
        )
        limb(Offset(cx - w * 0.15f, h * 0.21f), Offset(cx - w * 0.27f, h * 0.48f), line, stroke.width)
        limb(Offset(cx + w * 0.15f, h * 0.21f), Offset(cx + w * 0.27f, h * 0.48f), line, stroke.width)
        limb(Offset(cx - w * 0.08f, h * 0.52f), Offset(cx - w * 0.11f, h * 0.95f), line, stroke.width)
        limb(Offset(cx + w * 0.08f, h * 0.52f), Offset(cx + w * 0.11f, h * 0.95f), line, stroke.width)

        if (skeleton) {
            drawSkeleton(cx, w, h, line)
            return@Canvas
        }

        zone(statuses["behavioral"], Offset(cx, h * 0.075f), w * 0.045f)
        statuses["vision"]?.let {
            drawCircle(StatusColors.of(it), w * 0.012f, Offset(cx - w * 0.028f, h * 0.095f))
            drawCircle(StatusColors.of(it), w * 0.012f, Offset(cx + w * 0.028f, h * 0.095f))
        }
        zone(statuses["cardiovascular"], Offset(cx + w * 0.04f, h * 0.27f), w * 0.05f)
        zone(statuses["immune"], Offset(cx - w * 0.07f, h * 0.23f), w * 0.022f)
        zone(statuses["immune"], Offset(cx + w * 0.07f, h * 0.23f), w * 0.022f)
        zone(statuses["metabolic"], Offset(cx, h * 0.40f), w * 0.045f)
        statuses["musculoskeletal"]?.let { s ->
            val c = StatusColors.of(s).copy(alpha = 0.7f)
            limb(Offset(cx - w * 0.09f, h * 0.60f), Offset(cx - w * 0.105f, h * 0.85f), c, w * 0.03f)
            limb(Offset(cx + w * 0.09f, h * 0.60f), Offset(cx + w * 0.105f, h * 0.85f), c, w * 0.03f)
        }
    }
}

private fun DrawScope.limb(from: Offset, to: Offset, color: Color, width: Float) =
    drawLine(color, from, to, strokeWidth = width, cap = androidx.compose.ui.graphics.StrokeCap.Round)

private fun DrawScope.zone(status: Status?, center: Offset, radius: Float) {
    status ?: return
    val c = StatusColors.of(status)
    drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.9f), c.copy(alpha = 0f)), center = center, radius = radius * 2.2f), radius * 2.2f, center)
    drawCircle(c, radius * 0.7f, center)
}

private fun DrawScope.drawSkeleton(cx: Float, w: Float, h: Float, color: Color) {
    val bone = color.copy(alpha = 0.8f)
    val t = w * 0.008f
    drawLine(bone, Offset(cx, h * 0.17f), Offset(cx, h * 0.52f), strokeWidth = t * 1.5f) // spine
    for (i in 0 until 6) { // ribs
        val y = h * (0.23f + i * 0.035f)
        drawLine(bone, Offset(cx - w * 0.11f, y + h * 0.01f), Offset(cx, y), strokeWidth = t)
        drawLine(bone, Offset(cx + w * 0.11f, y + h * 0.01f), Offset(cx, y), strokeWidth = t)
    }
    drawOval(bone, Offset(cx - w * 0.1f, h * 0.49f), Size(w * 0.2f, h * 0.06f), style = Stroke(t)) // pelvis
    drawLine(bone, Offset(cx - w * 0.07f, h * 0.55f), Offset(cx - w * 0.1f, h * 0.73f), strokeWidth = t * 2) // femurs
    drawLine(bone, Offset(cx + w * 0.07f, h * 0.55f), Offset(cx + w * 0.1f, h * 0.73f), strokeWidth = t * 2)
    drawLine(bone, Offset(cx - w * 0.1f, h * 0.75f), Offset(cx - w * 0.11f, h * 0.93f), strokeWidth = t * 1.5f)
    drawLine(bone, Offset(cx + w * 0.1f, h * 0.75f), Offset(cx + w * 0.11f, h * 0.93f), strokeWidth = t * 1.5f)
}
