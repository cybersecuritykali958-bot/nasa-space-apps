package space.crewpulse.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import space.crewpulse.ui.components.SpaceArt.planet
import space.crewpulse.ui.components.SpaceArt.ship
import space.crewpulse.ui.components.SpaceArt.stars

/**
 * Animated mission view for Home: Earth on the left, the destination on the right and the ship
 * on the transfer arc at [progress] (0..1). For low Earth orbit the ship circles Earth instead.
 * Tapping toggles [details] text under the picture.
 */
@Composable
fun MissionTrajectory(
    environmentId: String?,
    progress: Float,
    caption: String,
    details: String,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember { mutableStateOf(false) }
    val t = rememberInfiniteTransition(label = "mission")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "spin")
    val twinkle by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4_000, easing = LinearEasing)), label = "twinkle")
    val flame by t.animateFloat(0f, 1f, infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "flame")
    val bob by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(2_200), RepeatMode.Reverse), label = "bob")
    val orbit by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9_000, easing = LinearEasing)), label = "orbit")
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline
    val body = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier.fillMaxWidth().clickable { showDetails = !showDetails },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            stars(40, twinkle)
            val h = size.height
            val w = size.width
            val earth = Offset(w * 0.14f, h * 0.62f)
            val p = progress.coerceIn(0f, 1f)

            if (environmentId == "iss") {
                // Low Earth orbit: a big Earth and the ship circling it.
                val c = Offset(w * 0.5f, h * 0.55f)
                val r = h * 0.28f
                planet(c, r, SpaceArt.earthLight, SpaceArt.earthDark, spin, SpaceArt.land)
                drawOval(track, Offset(c.x - r * 1.9f, c.y - r * 0.7f), androidx.compose.ui.geometry.Size(r * 3.8f, r * 1.4f), style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
                val a = Math.toRadians(orbit.toDouble())
                val sp = Offset(c.x + (kotlin.math.cos(a) * r * 1.9f).toFloat(), c.y + (kotlin.math.sin(a) * r * 0.7f).toFloat())
                val ang = Math.toDegrees(atan2(kotlin.math.cos(a) * r * 0.7, -kotlin.math.sin(a) * r * 1.9)).toFloat()
                ship(sp, h * 0.07f, ang, flame, body)
                return@Canvas
            }

            val destIsMoon = environmentId == "moon_surface"
            val dest = Offset(w * 0.86f, h * 0.42f)
            val ctrl = Offset(w * 0.5f, -h * 0.15f)
            val arc = Path().apply {
                moveTo(earth.x, earth.y)
                quadraticBezierTo(ctrl.x, ctrl.y, dest.x, dest.y)
            }
            drawPath(arc, track, style = Stroke(2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
            // Travelled part of the route.
            val done = Path().apply {
                moveTo(earth.x, earth.y)
                val steps = 40
                for (i in 1..steps) {
                    val s = p * i / steps
                    lineTo(quad(earth.x, ctrl.x, dest.x, s), quad(earth.y, ctrl.y, dest.y, s))
                }
            }
            drawPath(done, accent, style = Stroke(3f))

            planet(earth, h * 0.17f, SpaceArt.earthLight, SpaceArt.earthDark, spin, SpaceArt.land)
            if (destIsMoon) {
                planet(dest, h * 0.10f, SpaceArt.moonLight, SpaceArt.moonDark, spin * 0.3f)
            } else {
                planet(dest, h * 0.13f, SpaceArt.marsLight, SpaceArt.marsDark, spin * 0.9f, SpaceArt.marsDark)
            }

            // On a surface mission the crew is already at the destination.
            val atMars = environmentId == "mars_surface" || environmentId == "moon_surface"
            val s = if (atMars) 1f else p
            val pos = Offset(quad(earth.x, ctrl.x, dest.x, s), quad(earth.y, ctrl.y, dest.y, s) + bob * 2f)
            val dx = dquad(earth.x, ctrl.x, dest.x, s)
            val dy = dquad(earth.y, ctrl.y, dest.y, s)
            ship(if (atMars) dest + Offset(-h * 0.02f, -h * 0.2f) else pos, h * 0.07f, Math.toDegrees(atan2(dy, dx).toDouble()).toFloat(), flame, body)
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(caption, style = MaterialTheme.typography.titleSmall)
            if (showDetails) {
                Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun quad(a: Float, b: Float, c: Float, t: Float) = (1 - t) * (1 - t) * a + 2 * (1 - t) * t * b + t * t * c
private fun dquad(a: Float, b: Float, c: Float, t: Float) = 2 * (1 - t) * (b - a) + 2 * t * (c - b)
