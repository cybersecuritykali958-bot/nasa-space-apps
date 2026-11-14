package space.crewpulse.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import space.crewpulse.ui.components.SpaceArt.planet
import space.crewpulse.ui.components.SpaceArt.ship

/**
 * Loading animation: the Sun with Earth and Mars on their orbits. Mars goes around about
 * 1.88 times slower than Earth, like the real orbital periods; a small ship flies between them.
 */
@Composable
fun OrbitLoader(text: String, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "orbits")
    val earthAngle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(4_000, easing = LinearEasing)), label = "earth")
    val marsAngle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(7_520, easing = LinearEasing)), label = "mars")
    val transfer by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3_000, easing = LinearEasing), RepeatMode.Restart), label = "ship")
    val flame by t.animateFloat(0f, 1f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "flame")
    val orbitColor = MaterialTheme.colorScheme.outline
    val body = MaterialTheme.colorScheme.onSurface

    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(220.dp)) {
            val c = center
            val rEarth = size.minDimension * 0.24f
            val rMars = size.minDimension * 0.42f
            drawCircle(orbitColor, rEarth, c, style = Stroke(2f))
            drawCircle(orbitColor, rMars, c, style = Stroke(2f))
            drawCircle(Brush.radialGradient(listOf(SpaceArt.sun, SpaceArt.sun.copy(alpha = 0f)), c, size.minDimension * 0.16f), size.minDimension * 0.16f, c)
            drawCircle(SpaceArt.sun, size.minDimension * 0.07f, c)

            fun at(r: Float, deg: Float) = Offset(c.x + r * cos(Math.toRadians(deg.toDouble())).toFloat(), c.y + r * sin(Math.toRadians(deg.toDouble())).toFloat())
            val e = at(rEarth, earthAngle)
            val m = at(rMars, marsAngle)
            planet(e, size.minDimension * 0.05f, SpaceArt.earthLight, SpaceArt.earthDark, earthAngle * 3, SpaceArt.land)
            planet(m, size.minDimension * 0.04f, SpaceArt.marsLight, SpaceArt.marsDark, marsAngle * 2)

            val s = Offset(e.x + (m.x - e.x) * transfer, e.y + (m.y - e.y) * transfer)
            val ang = Math.toDegrees(kotlin.math.atan2((m.y - e.y).toDouble(), (m.x - e.x).toDouble())).toFloat()
            ship(s, size.minDimension * 0.03f, ang, flame, body)
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

