package space.crewpulse.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/** Small drawing helpers shared by the mission trajectory and the loading animation. */
internal object SpaceArt {
    val earthLight = Color(0xFF6FA8DC)
    val earthDark = Color(0xFF1F3A5A)
    val land = Color(0xFF7FC8A0)
    val marsLight = Color(0xFFD98C5F)
    val marsDark = Color(0xFF6E3322)
    val moonLight = Color(0xFFCFD3D8)
    val moonDark = Color(0xFF5E636A)
    val sun = Color(0xFFE6B566)

    /** A shaded planet; [spin] in degrees moves the surface marks to suggest rotation. */
    fun DrawScope.planet(center: Offset, radius: Float, light: Color, dark: Color, spin: Float, marks: Color? = null) {
        drawCircle(
            Brush.radialGradient(listOf(light, dark), center = center + Offset(-radius * 0.35f, -radius * 0.35f), radius = radius * 1.6f),
            radius, center,
        )
        if (marks != null) {
            val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, radius)) }
            clipPath(clip) {
                for (i in 0 until 3) {
                    val a = Math.toRadians((spin + i * 120).toDouble())
                    val x = center.x + (cos(a) * radius * 0.9).toFloat()
                    val y = center.y + (sin(i * 1.7) * radius * 0.4).toFloat()
                    if (cos(a) > -0.2) drawCircle(marks.copy(alpha = 0.55f), radius * 0.28f, Offset(x, y))
                }
            }
        }
        // Night side.
        drawCircle(
            Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)), center = center + Offset(-radius * 0.5f, -radius * 0.5f), radius = radius * 2f),
            radius, center,
        )
    }

    /** A tiny spacecraft pointing along [angleDeg], with a flickering engine glow. */
    fun DrawScope.ship(at: Offset, size: Float, angleDeg: Float, flame: Float, body: Color) {
        rotate(angleDeg, at) {
            val p = Path().apply {
                moveTo(at.x + size, at.y)
                lineTo(at.x - size * 0.6f, at.y - size * 0.45f)
                lineTo(at.x - size * 0.35f, at.y)
                lineTo(at.x - size * 0.6f, at.y + size * 0.45f)
                close()
            }
            drawCircle(sun.copy(alpha = 0.35f + 0.35f * flame), size * (0.35f + 0.2f * flame), Offset(at.x - size * 0.75f, at.y))
            drawPath(p, body)
        }
    }

    /** Deterministic star field (same stars every frame), twinkling with [phase]. */
    fun DrawScope.stars(count: Int, phase: Float, color: Color = Color(0xFFE3E5E8)) {
        for (i in 0 until count) {
            val x = ((i * 73 + 17) % 101) / 101f * size.width
            val y = ((i * 37 + 11) % 97) / 97f * size.height
            val tw = 0.25f + 0.5f * ((sin(phase * 6.283f + i) + 1f) / 2f)
            drawCircle(color.copy(alpha = tw), if (i % 5 == 0) 2.2f else 1.3f, Offset(x, y))
        }
    }
}
