package space.crewpulse.ui.mission

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import space.crewpulse.R
import space.crewpulse.domain.TaskPriority
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.i18n.badgeLabel
import space.crewpulse.ui.i18n.levelTitle
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.theme.CrewColors
import space.crewpulse.ui.theme.StatusColors

/** Gamification: level, streak, today's measurement mission and badges. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionScreen(state: HomeUiState, onReminder: () -> Boolean, onBack: (() -> Unit)?) {
    val game = state.report.game
    val plan = state.report.plan
    var reminderResult by remember { mutableStateOf<Boolean?>(null) }

    CrewScaffold(title = stringResource(R.string.mission_title), onBack = onBack) { pad ->
        if (game == null || plan == null) {
            Text(stringResource(R.string.no_data), pad.padding(16.dp))
            return@CrewScaffold
        }
        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CrewCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Hexagon(Modifier.size(48.dp)) { Icon(Icons.Filled.Star, null, tint = CrewColors.teal) }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(levelTitle(game.levelTitle), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.level, game.level), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.xp, game.points, game.pointsToNextLevel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val per = (game.points % 250).toFloat() / 250f
                    ProgressRing(per)
                }
            }
            CrewCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Hexagon(Modifier.size(48.dp), StatusColors.yellow) { Text("🚀") }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(stringResource(R.string.days, game.currentStreakDays), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.current_streak), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            CrewCard {
                Text(stringResource(R.string.todays_mission), style = MaterialTheme.typography.titleSmall)
                if (plan.next24h.isEmpty()) {
                    Text(stringResource(R.string.all_done), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                plan.next24h.forEach { task ->
                    val name = state.evaluation(task.indicatorId)?.indicator?.let { indicatorName(it) } ?: task.indicatorName
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, modifier = Modifier.weight(1f))
                        if (task.minutes >= 1) {
                            Text("~%.0f ".format(task.minutes) + stringResource(R.string.min), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                        }
                        Icon(
                            Icons.Filled.CheckCircle, null,
                            tint = if (task.priority == TaskPriority.HIGH) StatusColors.red else if (task.priority == TaskPriority.NORMAL) StatusColors.yellow else MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Text(stringResource(R.string.minutes_saved, plan.minutesNext24h, plan.minutesSaved), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = { reminderResult = onReminder() }, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Notifications, null)
                    Text(stringResource(R.string.send_reminder), Modifier.padding(start = 8.dp))
                }
                reminderResult?.let {
                    Text(stringResource(if (it) R.string.reminder_sent else R.string.reminder_none), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(stringResource(R.string.badges), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                game.badges.forEach { b ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
                        Hexagon(Modifier.size(64.dp)) { Icon(Icons.Filled.Star, null, tint = CrewColors.teal) }
                        Text(badgeLabel(b), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                }
            }
            Text("", Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun ProgressRing(fraction: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
        val track = MaterialTheme.colorScheme.outline
        Canvas(Modifier.size(64.dp)) {
            val stroke = Stroke(width = 8f)
            drawArc(track, 0f, 360f, false, style = stroke)
            drawArc(CrewColors.teal, -90f, 360f * fraction, false, style = stroke)
        }
        Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Hexagon(modifier: Modifier, color: androidx.compose.ui.graphics.Color = CrewColors.teal, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val c = Offset(size.width / 2, size.height / 2)
            val path = Path().apply {
                for (i in 0 until 6) {
                    val a = Math.toRadians(60.0 * i - 30)
                    val p = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(path, color.copy(alpha = 0.15f))
            drawPath(path, color, style = Stroke(width = 4f))
        }
        content()
    }
}

