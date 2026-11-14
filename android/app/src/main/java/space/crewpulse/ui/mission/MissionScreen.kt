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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.domain.TaskPriority
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.ListRow
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.i18n.badgeLabel
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.i18n.levelTitle
import space.crewpulse.ui.theme.StatusColors

/** Gamification: level ring, streak, today's measurements and badges. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionScreen(state: HomeUiState, onReminder: () -> Boolean, onBack: (() -> Unit)?) {
    val game = state.report.game
    val plan = state.report.plan
    var reminderResult by remember { mutableStateOf<Boolean?>(null) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.mission_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
        if (game == null || plan == null) {
            Text(stringResource(R.string.no_data))
            return@Column
        }
        CrewCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelRing(game.level, (game.points % 250) / 250f)
                Column(Modifier.padding(start = 20.dp)) {
                    Text(levelTitle(game.levelTitle), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.xp, game.points, game.pointsToNextLevel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        CrewCard {
            ListRow(
                title = stringResource(R.string.streak_days, game.currentStreakDays),
                icon = Icons.Outlined.LocalFireDepartment,
                iconTint = StatusColors.yellow,
            )
        }
        CrewCard {
            Text(stringResource(R.string.todays_mission), style = MaterialTheme.typography.titleMedium)
            if (plan.next24h.isEmpty()) Text(stringResource(R.string.all_done), color = MaterialTheme.colorScheme.onSurfaceVariant)
            plan.next24h.forEach { task ->
                val name = state.evaluation(task.indicatorId)?.indicator?.let { indicatorName(it) } ?: task.indicatorName
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (task.priority == TaskPriority.LOW) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        null,
                        tint = if (task.priority == TaskPriority.LOW) StatusColors.green else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(name, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (task.minutes >= 1) Text("~%.0f ".format(task.minutes) + stringResource(R.string.min), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(stringResource(R.string.minutes_saved, plan.minutesNext24h, plan.minutesSaved), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
            OutlinedButton(onClick = { reminderResult = onReminder() }, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Outlined.NotificationsNone, null)
                Text(stringResource(R.string.send_reminder), Modifier.padding(start = 8.dp))
            }
            reminderResult?.let { Text(stringResource(if (it) R.string.reminder_sent else R.string.reminder_none), style = MaterialTheme.typography.bodyMedium) }
        }
        Text(stringResource(R.string.badges), style = MaterialTheme.typography.titleMedium)
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
        Text("", Modifier.padding(bottom = 8.dp))
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
