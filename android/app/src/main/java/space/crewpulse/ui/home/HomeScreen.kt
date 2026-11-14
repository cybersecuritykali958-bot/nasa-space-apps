package space.crewpulse.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.data.Profile
import space.crewpulse.data.model.Status
import space.crewpulse.domain.SpaceWeatherLevel
import space.crewpulse.domain.StatusEngine
import space.crewpulse.ui.components.BodyFigure
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.StatusPill
import space.crewpulse.ui.i18n.alertText
import space.crewpulse.ui.i18n.environmentName
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.i18n.responderLabel
import space.crewpulse.ui.i18n.levelTitle
import space.crewpulse.ui.i18n.triageReason
import space.crewpulse.ui.navigation.Routes
import space.crewpulse.ui.theme.StatusColors

/** Home: a short overview. Details live in the tabs (Health, Readiness, Twin, Mission). */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onIndicator: (String) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
            }
        }
        when {
            state.loading -> Text(stringResource(R.string.loading))
            state.error != null -> Text(stringResource(R.string.data_error, state.error), color = MaterialTheme.colorScheme.error)
            else -> Overview(state, onIndicator, onNavigate)
        }
    }
}

@Composable
private fun Overview(state: HomeUiState, onIndicator: (String) -> Unit, onNavigate: (String) -> Unit) {
    // Mission banner: who, where, how far from Earth.
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier.clickable { onNavigate(Routes.SETTINGS) },
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.home_banner, environmentName(state.environment?.id, state.environmentName), state.missionDay, "%.0f".format(state.commDelayMin ?: 0.0)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(if (state.profile == Profile.DEMO) R.string.profile_demo else R.string.profile_me),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // Body figure and overall status.
    val overall = StatusEngine.worst(state.systems.map { it.status })
    val attention = state.systems.count { it.status == Status.YELLOW || it.status == Status.RED }
    Row(verticalAlignment = Alignment.CenterVertically) {
        BodyFigure(
            statuses = state.systems.associate { it.systemId to it.status },
            modifier = Modifier.weight(0.45f).clickable { onNavigate(Routes.HEALTH) },
        )
        Column(Modifier.weight(0.55f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.overall_status), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusPill(overall)
            Text(
                if (attention == 0) stringResource(R.string.all_nominal) else stringResource(R.string.systems_attention, attention),
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedButton(onClick = { onNavigate(Routes.HEALTH) }) { Text(stringResource(R.string.open_health)) }
        }
    }

    // What to do now.
    val t = state.report.triage
    CrewCard(
        accent = StatusColors.of(t.status).takeIf { t.status != Status.GREEN },
        modifier = Modifier.clickable { onNavigate(Routes.READINESS) },
    ) {
        Text(stringResource(R.string.what_to_do), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(responderLabel(t.responder), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
        Text(triageReason(state.report), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // The single most important warning.
    state.report.spaceWeather?.takeIf { it.level == SpaceWeatherLevel.WARNING }?.let {
        Banner(stringResource(R.string.sw_warning), StatusColors.red) { onNavigate(Routes.READINESS) }
    } ?: state.report.alerts.firstOrNull()?.let { alert ->
        val ind = state.evaluation(alert.indicatorId)?.indicator
        Banner(alertText(alert, ind?.let { indicatorName(it) } ?: alert.indicatorId), StatusColors.yellow) { onIndicator(alert.indicatorId) }
    }

    // Main actions.
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { onNavigate(Routes.CHECKIN) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
            Icon(Icons.Filled.CheckCircle, null)
            Text(stringResource(R.string.qa_checkin), Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = { onNavigate(Routes.PVT) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
            Icon(Icons.Filled.PlayArrow, null)
            Text(stringResource(R.string.qa_pvt), Modifier.padding(start = 8.dp))
        }
    }

    // Today's mission in one line.
    state.report.plan?.let { plan ->
        CrewCard(modifier = Modifier.clickable { onNavigate(Routes.MISSION) }) {
            Text(stringResource(R.string.todays_mission), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                if (plan.next24h.isEmpty()) stringResource(R.string.all_done)
                else stringResource(R.string.today_short, plan.next24h.size, plan.minutesNext24h),
                style = MaterialTheme.typography.bodyLarge,
            )
            state.report.game?.let {
                Text(stringResource(R.string.streak_short, it.currentStreakDays, levelTitle(it.levelTitle)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    Text(
        stringResource(R.string.demo_data_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

@Composable
private fun Banner(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = color)
            Text(text, modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}
