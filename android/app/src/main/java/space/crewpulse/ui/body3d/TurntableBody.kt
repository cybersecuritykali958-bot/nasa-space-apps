package space.crewpulse.ui.body3d

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.floor
import kotlin.math.min
import space.crewpulse.data.model.Status
import space.crewpulse.domain.SystemStatus
import space.crewpulse.ui.theme.CrewColors
import space.crewpulse.ui.theme.StatusColors
import space.crewpulse.ui.watch.beatKick
import space.crewpulse.ui.watch.rememberFrameClock

/*
 * Pre-rendered holographic body turntable.
 * Frames come from tools/body_turntable/render_turntable.py: each sheet is a grid of frames and
 * every color channel holds one layer (body shell, radiation halo or one body system).
 * Here each channel is turned into alpha and tinted with that system's status color, then the
 * layers are added on top of each other (hologram look). Drag rotates, tap picks a system.
 */

@Serializable
private data class TurntableMeta(
    val frames: Int,
    val cols: Int,
    val rows: Int,
    val frameWidth: Int,
    val frameHeight: Int,
    val layers: List<Layer>,
) {
    @Serializable
    data class Layer(val id: String, val sheet: Int, val channel: String)
}

private class Turntable(val meta: TurntableMeta, val sheets: List<Bitmap>) {
    val images: List<ImageBitmap> = sheets.map { it.asImageBitmap() }
}

private const val DIR = "body_turntable"
private const val BODY = "body"
private const val RADIATION = "radiation"

private suspend fun loadTurntable(context: Context): Turntable? = withContext(Dispatchers.IO) {
    runCatching {
        val meta = Json { ignoreUnknownKeys = true }
            .decodeFromString<TurntableMeta>(context.assets.open("$DIR/meta.json").bufferedReader().use { it.readText() })
        val count = meta.layers.maxOf { it.sheet } + 1
        val sheets = (0 until count).map { i -> context.assets.open("$DIR/sheet_$i.png").use { BitmapFactory.decodeStream(it) } }
        Turntable(meta, sheets)
    }.getOrNull()
}

/** Channel of the sheet → alpha, tinted with [color]. Offsets in ColorMatrix are 0..255. */
private fun channelTint(channel: String, color: Color): ColorFilter {
    val r = if (channel == "r") 1f else 0f
    val g = if (channel == "g") 1f else 0f
    val b = if (channel == "b") 1f else 0f
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0f, 0f, 0f, 0f, color.red * 255f,
                0f, 0f, 0f, 0f, color.green * 255f,
                0f, 0f, 0f, 0f, color.blue * 255f,
                r, g, b, 0f, 0f,
            ),
        ),
    )
}

/**
 * Rotatable holographic body. [systems] color the organs; [heartRate] (bpm) makes the heart beat;
 * [selected] is highlighted; [onSystem] receives the system id of a tapped organ.
 */
@Composable
fun TurntableBody(
    systems: List<SystemStatus>,
    heartRate: Double?,
    selected: String?,
    onSystem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val turntable by produceState<Turntable?>(null) { value = loadTurntable(context) }
    val statusOf = remember(systems) { systems.associate { it.systemId to it.status } }

    // Rotation in degrees; slow auto-spin, paused for a moment after the user drags.
    var angle by remember { mutableFloatStateOf(0f) }
    var lastTouch by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var prev = withFrameMillis { it }
        while (true) {
            withFrameMillis { t ->
                if (t - lastTouch > 2_500) angle += (t - prev) * 0.012f
                prev = t
                now = t
            }
        }
    }
    val clock = rememberFrameClock()
    val scan = remember { Animatable(0f) }
    LaunchedEffect(turntable) { if (turntable != null) scan.animateTo(1f, tween(1_800, easing = LinearEasing)) }
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(1_400), RepeatMode.Reverse), label = "red")
    var size by remember { mutableStateOf(IntSize.Zero) }

    val tt = turntable ?: return
    val meta = tt.meta
    val step = 360f / meta.frames

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onDragStart = { lastTouch = Long.MAX_VALUE }, onDragEnd = { lastTouch = now }) { _, dx ->
                    angle -= dx * 0.4f
                }
            }
            .pointerInput(tt) {
                detectTapGestures { tap -> hitTest(tt, angle, tap, size)?.let(onSystem) }
            },
    ) {
        size = IntSize(this.size.width.toInt(), this.size.height.toInt())
        val pos = ((angle / step) % meta.frames + meta.frames) % meta.frames
        val f0 = floor(pos).toInt()
        val f1 = (f0 + 1) % meta.frames
        val blend = pos - f0
        val (dstOffset, dstSize) = fit(meta, this.size.width, this.size.height)
        val scanY = scan.value * this.size.height

        meta.layers.forEach { layer ->
            val base = when (layer.id) {
                BODY -> CrewColors.teal
                RADIATION -> StatusColors.of(statusOf[RADIATION] ?: Status.UNKNOWN)
                else -> StatusColors.of(statusOf[layer.id] ?: Status.UNKNOWN)
            }
            var alpha = when (layer.id) {
                BODY -> 0.4f
                RADIATION -> 0.16f
                else -> 1f
            }
            val status = statusOf[layer.id]
            if (status == Status.RED) alpha *= 0.55f + 0.45f * pulse
            if (layer.id == "cardiovascular" && heartRate != null) alpha *= 0.7f + 0.3f * beatKick(clock.value, heartRate)
            if (selected != null && layer.id != BODY && layer.id != selected) alpha *= 0.35f
            val filter = channelTint(layer.channel, base)
            // Organs appear under the scan line on first show; the shell is visible right away.
            val clipBottom = if (layer.id == BODY) this.size.height else scanY
            clipRect(bottom = clipBottom) {
                drawFrame(tt, layer.sheet, f0, dstOffset, dstSize, alpha * (1 - blend), filter)
                drawFrame(tt, layer.sheet, f1, dstOffset, dstSize, alpha * blend, filter)
            }
        }
        if (scan.value < 1f) {
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, CrewColors.teal.copy(alpha = 0.5f), Color.Transparent), scanY - 24f, scanY + 24f),
                topLeft = Offset(0f, scanY - 24f),
                size = androidx.compose.ui.geometry.Size(this.size.width, 48f),
            )
        }
        // Subtle moving scan lines over the hologram.
        val lineGap = 6f
        val shift = (now / 40f) % lineGap
        var y = shift
        while (y < this.size.height) {
            drawLine(Color.Black.copy(alpha = 0.12f), Offset(0f, y), Offset(this.size.width, y), 1f)
            y += lineGap
        }
    }
}

/** Frame placement: keep aspect ratio, centered in the canvas. */
private fun fit(meta: TurntableMeta, w: Float, h: Float): Pair<IntOffset, IntSize> {
    val scale = min(w / meta.frameWidth, h / meta.frameHeight)
    val dw = (meta.frameWidth * scale).toInt()
    val dh = (meta.frameHeight * scale).toInt()
    return IntOffset(((w - dw) / 2).toInt(), ((h - dh) / 2).toInt()) to IntSize(dw, dh)
}

private fun DrawScope.drawFrame(tt: Turntable, sheet: Int, frame: Int, dst: IntOffset, dstSize: IntSize, alpha: Float, filter: ColorFilter) {
    if (alpha <= 0.01f) return
    val m = tt.meta
    drawImage(
        image = tt.images[sheet],
        srcOffset = IntOffset((frame % m.cols) * m.frameWidth, (frame / m.cols) * m.frameHeight),
        srcSize = IntSize(m.frameWidth, m.frameHeight),
        dstOffset = dst,
        dstSize = dstSize,
        alpha = alpha.coerceIn(0f, 1f),
        colorFilter = filter,
        blendMode = BlendMode.Plus,
    )
}

/** Which organ is under the finger: the strongest organ layer at that pixel of the current frame. */
private fun hitTest(tt: Turntable, angle: Float, tap: Offset, canvas: IntSize): String? {
    val m = tt.meta
    val (dst, dstSize) = fit(m, canvas.width.toFloat(), canvas.height.toFloat())
    val fx = ((tap.x - dst.x) / dstSize.width * m.frameWidth).toInt()
    val fy = ((tap.y - dst.y) / dstSize.height * m.frameHeight).toInt()
    if (fx !in 0 until m.frameWidth || fy !in 0 until m.frameHeight) return null
    val step = 360f / m.frames
    val frame = Math.round(((angle / step) % m.frames + m.frames) % m.frames) % m.frames
    val ox = (frame % m.cols) * m.frameWidth
    val oy = (frame / m.cols) * m.frameHeight
    // Search a small neighbourhood so small organs (eyes, lymph nodes) are easy to hit.
    var best: String? = null
    var bestValue = 60
    for (layer in m.layers) {
        if (layer.id == BODY || layer.id == RADIATION) continue
        val bmp = tt.sheets[layer.sheet]
        for (dy in -6..6 step 3) for (dx in -6..6 step 3) {
            val x = (ox + fx + dx).coerceIn(ox, ox + m.frameWidth - 1)
            val y = (oy + fy + dy).coerceIn(oy, oy + m.frameHeight - 1)
            val px = bmp.getPixel(x, y)
            val v = when (layer.channel) {
                "r" -> (px shr 16) and 0xFF
                "g" -> (px shr 8) and 0xFF
                else -> px and 0xFF
            }
            if (v > bestValue) {
                bestValue = v
                best = layer.id
            }
        }
    }
    return best
}
