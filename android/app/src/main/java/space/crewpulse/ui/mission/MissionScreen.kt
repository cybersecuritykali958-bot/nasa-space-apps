package space.crewpulse.ui.mission

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.domain.GameState
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.i18n.badgeLabel
import space.crewpulse.ui.i18n.levelTitle
import space.crewpulse.ui.theme.StatusColors

/** Qualification level, compliance points and monitoring continuity in one card. */
@Composable
fun GameSummary(game: GameState) {
    CrewCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelRing(game.level, (game.points % 250) / 250f)
            Column(Modifier.padding(start = 20.dp)) {
                Text(
                    stringResource(R.string.qualification_level, game.level),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(levelTitle(game.levelTitle), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.xp, game.points, game.pointsToNextLevel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, tint = StatusColors.yellow, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.streak_days, game.currentStreakDays),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

/** Earned commendations as a row of round icons. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Commendations(game: GameState) {
    if (game.badges.isEmpty()) return
    Text(stringResource(R.string.badges), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        game.badges.forEach { b ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(badgeIcon(b), null, tint = MaterialTheme.colorScheme.primary) }
                }
                Text(badgeLabel(b), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun badgeIcon(name: String): ImageVector = when (name) {
    "7-day streak" -> Icons.Outlined.LocalFireDepartment
    "30-day streak" -> Icons.Outlined.RocketLaunch
    "100-day streak" -> Icons.Outlined.EmojiEvents
    "Full check-in" -> Icons.Outlined.MonitorHeart
    "Reaction tester" -> Icons.Outlined.Psychology
    else -> Icons.Outlined.Shield
}

@Composable
private fun LevelRing(level: Int, fraction: Float) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
        Canvas(Modifier.size(88.dp)) {
            val stroke = Stroke(width = 10f, cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = stroke)
            drawArc(accent, -90f, 360f * fraction, false, style = stroke)
        }
        Text(stringResource(R.string.level_short, level), style = MaterialTheme.typography.titleMedium)
    }
}
