package space.crewpulse.ui.watch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import space.crewpulse.ui.theme.CrewColors

private val WatchBody = Color(0xFF2B2F34)
private val WatchScreen = Color(0xFF101315)
private val Strap = Color(0xFF3A4047)
private val Ink = Color(0xFFE3E5E8)

/** A round smartwatch seen from the front, with a small heart-rate trace on its screen. */
internal fun DrawScope.watch(center: Offset, r: Float, accent: Color = CrewColors.teal) {
    val strapW = r * 1.05f
    drawRoundRect(Strap, Offset(center.x - strapW / 2, center.y - r * 1.9f), Size(strapW, r * 1.2f), CornerRadius(r * 0.25f))
    drawRoundRect(Strap, Offset(center.x - strapW / 2, center.y + r * 0.7f), Size(strapW, r * 1.2f), CornerRadius(r * 0.25f))
    drawCircle(WatchBody, r, center)
    drawCircle(Ink.copy(alpha = 0.25f), r, center, style = Stroke(r * 0.06f))
    drawCircle(WatchScreen, r * 0.8f, center)
    val trace = Path().apply {
        moveTo(center.x - r * 0.55f, center.y)
        lineTo(center.x - r * 0.2f, center.y)
        lineTo(center.x - r * 0.08f, center.y - r * 0.35f)
        lineTo(center.x + r * 0.05f, center.y + r * 0.25f)
        lineTo(center.x + r * 0.15f, center.y)
        lineTo(center.x + r * 0.55f, center.y)
    }
    drawPath(trace, accent, style = Stroke(r * 0.07f, cap = StrokeCap.Round))
}

/** A phone outline; the app's "station" the watch docks to. */
internal fun DrawScope.phone(center: Offset, h: Float, accent: Color) {
    val w = h * 0.52f
    val tl = Offset(center.x - w / 2, center.y - h / 2)
    drawRoundRect(WatchBody, tl, Size(w, h), CornerRadius(h * 0.12f))
    drawRoundRect(accent.copy(alpha = 0.7f), tl, Size(w, h), CornerRadius(h * 0.12f), style = Stroke(h * 0.025f))
    drawRoundRect(
        Brush.verticalGradient(listOf(accent.copy(alpha = 0.25f), WatchScreen), tl.y, tl.y + h),
        tl + Offset(w * 0.1f, h * 0.08f), Size(w * 0.8f, h * 0.84f), CornerRadius(h * 0.06f),
    )
}

/** Intro art: the watch slowly floating on an orbit ring. */
@Composable
internal fun WatchHero(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "hero")
    val float by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(2_400), RepeatMode.Reverse), label = "float")
    val orbit by t.animateFloat(0f, 360f, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "orbit")
    val accent = CrewColors.teal
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.17f
        val ring = size.minDimension * 0.42f
        drawCircle(accent.copy(alpha = 0.18f), ring, c, style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f))))
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent), c, ring), ring, c)
        val a = Math.toRadians(orbit.toDouble())
        drawCircle(accent, size.minDimension * 0.018f, Offset(c.x + ring * cos(a).toFloat(), c.y + ring * sin(a).toFloat()))
        watch(c + Offset(0f, float * size.minDimension * 0.03f), r)
    }
}

/** Radar sweep from the phone; the found device appears as a blip on the outer ring. */
@Composable
internal fun RadarScan(found: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "radar")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing)), label = "wave")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2_000, easing = LinearEasing)), label = "sweep")
    val blink by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "blink")
    val blip by animateFloatAsState(if (found) 1f else 0f, tween(600), label = "blip")
    val accent = CrewColors.teal
    Canvas(modifier) {
        val c = center
        val max = size.minDimension * 0.48f
        listOf(0.35f, 0.7f, 1f).forEach { drawCircle(accent.copy(alpha = 0.12f), max * it, c, style = Stroke(1.5f)) }
        for (i in 0 until 3) {
            val f = (wave + i / 3f) % 1f
            drawCircle(accent.copy(alpha = 0.45f * (1f - f)), max * f, c, style = Stroke(3f))
        }
        drawArc(
            Brush.radialGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent), c, max),
            sweep - 40f, 40f, useCenter = true,
            topLeft = Offset(c.x - max, c.y - max), size = Size(max * 2, max * 2),
        )
        phone(c, size.minDimension * 0.24f, accent)
        if (blip > 0f) {
            val a = Math.toRadians(-40.0)
            val p = Offset(c.x + max * 0.7f * cos(a).toFloat(), c.y + max * 0.7f * sin(a).toFloat())
            drawCircle(accent.copy(alpha = 0.25f * blip * blink), size.minDimension * 0.07f, p)
            drawCircle(accent.copy(alpha = blip), size.minDimension * 0.025f, p)
        }
    }
}

/**
 * Docking: the watch flies along a curve to the phone's port ([progress] 0→1),
 * a capture beam links them near the end, then a ring flashes and keeps pulsing.
 */
@Composable
internal fun DockingScene(progress: Float, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "dock")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1_600, easing = LinearEasing)), label = "pulse")
    val accent = CrewColors.teal
    Canvas(modifier) {
        val c = center + Offset(0f, size.height * 0.12f)
        val phoneH = size.minDimension * 0.34f
        val port = Offset(c.x, c.y - phoneH / 2 - size.minDimension * 0.14f)
        val start = Offset(size.width * 0.92f, size.height * 0.05f)
        val control = Offset(size.width * 0.05f, size.height * 0.02f)
        fun bezier(f: Float) = Offset(
            (1 - f) * (1 - f) * start.x + 2 * (1 - f) * f * control.x + f * f * port.x,
            (1 - f) * (1 - f) * start.y + 2 * (1 - f) * f * control.y + f * f * port.y,
        )

        // Planned trajectory (dotted) and the part already flown.
        val steps = 40
        for (i in 0..steps) {
            val f = i / steps.toFloat()
            val flown = f <= progress
            drawCircle(accent.copy(alpha = if (flown) 0.55f else 0.15f), if (flown) 3f else 2f, bezier(f))
        }

        phone(c, phoneH, accent)
        drawCircle(accent.copy(alpha = 0.35f), size.minDimension * 0.1f, port, style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))

        val pos = bezier(progress)
        // Capture beam between port and watch.
        if (progress in 0.55f..0.98f) {
            val beam = ((progress - 0.55f) / 0.2f).coerceIn(0f, 1f)
            drawLine(accent.copy(alpha = 0.5f * beam), c - Offset(0f, phoneH / 2), pos, strokeWidth = size.minDimension * 0.05f * beam, cap = StrokeCap.Round)
        }
        if (progress >= 1f) {
            drawCircle(accent.copy(alpha = 0.5f * (1f - pulse)), size.minDimension * (0.1f + 0.2f * pulse), port, style = Stroke(4f))
        }
        watch(pos, size.minDimension * (0.06f + 0.04f * progress))
    }
}

/**
 * Simulated ECG waveform (P, QRS, T as Gaussian bumps) for one beat; [phase] is 0..1 within
 * the beat. Visual only: it is never used as a measurement.
 */
internal fun ecgSample(phase: Float): Float {
    fun bump(mu: Float, amp: Float, w: Float): Float { val d = phase - mu; return amp * exp(-(d * d) / (2 * w * w)) }
    return bump(0.18f, 0.12f, 0.025f) + bump(0.36f, -0.12f, 0.009f) + bump(0.39f, 1f, 0.011f) +
        bump(0.42f, -0.25f, 0.01f) + bump(0.62f, 0.3f, 0.04f)
}

/** Milliseconds since first composition, updated every frame. Read it in draw/layer lambdas only. */
@Composable
internal fun rememberFrameClock(): State<Long> {
    val now = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { now.longValue = it - start }
    }
    return now
}

/** 1 right at the R peak, decaying to 0 before the next beat; drives the pulsing heart. */
internal fun beatKick(nowMs: Long, bpm: Double): Float {
    val beat = ((nowMs / 1000.0 * bpm / 60.0) % 1.0).toFloat()
    return exp(-((beat - 0.39f + 1f) % 1f) * 10f)
}

/** Scrolling ECG trace at [bpm]; the last 3 seconds are visible. */
@Composable
internal fun EcgTrace(bpm: Double, clock: State<Long>, modifier: Modifier = Modifier) {
    val beatsPerSec = bpm / 60.0
    val accent = CrewColors.teal
    Canvas(modifier) {
        val nowMs = clock.value
        val window = 3.0
        val mid = size.height * 0.62f
        val amp = size.height * 0.5f
        for (i in 1..5) drawLine(accent.copy(alpha = 0.06f), Offset(size.width * i / 6f, 0f), Offset(size.width * i / 6f, size.height))
        val path = Path()
        var x = 0f
        var last = Offset.Zero
        while (x <= size.width) {
            val tSec = nowMs / 1000.0 - (size.width - x) / size.width * window
            val ph = (((tSec * beatsPerSec) % 1.0 + 1.0) % 1.0).toFloat()
            last = Offset(x, mid - ecgSample(ph) * amp)
            if (x == 0f) path.moveTo(last.x, last.y) else path.lineTo(last.x, last.y)
            x += 3f
        }
        drawPath(path, Brush.horizontalGradient(listOf(Color.Transparent, accent), 0f, size.width * 0.5f), style = Stroke(4f, cap = StrokeCap.Round))
        drawCircle(accent.copy(alpha = 0.3f), 14f, last)
        drawCircle(accent, 6f, last)
    }
}
