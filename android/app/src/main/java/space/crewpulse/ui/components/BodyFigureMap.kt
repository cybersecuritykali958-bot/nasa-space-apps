package space.crewpulse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import space.crewpulse.data.model.Status
import space.crewpulse.ui.home.SystemStatus
import space.crewpulse.ui.theme.StatusColors

private data class BodyZone(
    val systemId: String,
    val xFrac: Float,
    val yFrac: Float,
    val wFrac: Float,
    val hFrac: Float,
) {
    fun rect(cx: Float, canvasW: Float, canvasH: Float): Rect {
        val width = canvasW * wFrac
        val height = canvasH * hFrac
        val left = cx - width / 2f + (xFrac * canvasW * 0.25f)
        val top = canvasH * yFrac
        return Rect(left, top, left + width, top + height)
    }
}

private val BODY_ZONES = listOf(
    BodyZone("vision", 0f, 0.02f, 0.22f, 0.14f),
    BodyZone("behavioral", 0f, 0.17f, 0.28f, 0.12f),
    BodyZone("cardiovascular", 0f, 0.30f, 0.34f, 0.16f),
    BodyZone("metabolic", 0f, 0.47f, 0.30f, 0.14f),
    BodyZone("immune", 0f, 0.62f, 0.28f, 0.12f),
    BodyZone("musculoskeletal", -0.55f, 0.28f, 0.16f, 0.40f),
    BodyZone("musculoskeletal", 0.55f, 0.28f, 0.16f, 0.40f),
)

@Composable
fun BodyFigureMap(
    systems: List<SystemStatus>,
    onSystemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusById = systems.associate { it.systemId to it.status }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .onSizeChanged {
                canvasSize = Size(it.width.toFloat(), it.height.toFloat())
            }
            .pointerInput(canvasSize, systems) {
                if (canvasSize.width == 0f) return@pointerInput
                detectTapGestures { offset ->
                    val cx = canvasSize.width / 2f
                    BODY_ZONES.firstOrNull { it.rect(cx, canvasSize.width, canvasSize.height).contains(offset) }
                        ?.systemId
                        ?.let(onSystemClick)
                }
            },
    ) {
        val cx = size.width / 2f
        BODY_ZONES.forEach { zone ->
            val rect = zone.rect(cx, size.width, size.height)
            val color = StatusColors.of(statusById[zone.systemId] ?: Status.UNKNOWN)
            drawRoundRect(
                color = color.copy(alpha = 0.55f),
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                cornerRadius = CornerRadius(12f, 12f),
            )
        }
        drawCircle(
            color = StatusColors.of(statusById["radiation"] ?: Status.UNKNOWN).copy(alpha = 0.35f),
            radius = size.width * 0.48f,
            center = Offset(cx, size.height / 2f),
            style = Stroke(width = 8f),
        )
    }
}