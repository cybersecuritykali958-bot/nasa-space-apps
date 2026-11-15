package space.crewpulse.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.data.Profile
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.ListRow
import space.crewpulse.ui.components.TabHeader
import space.crewpulse.data.CrewMember
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.Icon
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.i18n.environmentName
import space.crewpulse.ui.login.roleLabel

/** Crew Member Profile tab: who is signed in, devices, data source, mission environment, sign-out. */
@Composable
fun SettingsScreen(
    state: HomeUiState,
    member: CrewMember?,
    onProfile: (Profile) -> Unit,
    onEnvironment: (String) -> Unit,
    watchConnected: Boolean,
    onWatchConnect: () -> Unit,
    onWatchDisconnect: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TabHeader(stringResource(R.string.title_profile))
        if (member != null) {
            CrewCard {
                ListRow(
                    title = member.name,
                    subtitle = "${roleLabel(member.role)} · ${stringResource(R.string.badge_id, member.badgeId)}",
                    icon = Icons.Outlined.Badge,
                )
            }
        }
        Section(stringResource(R.string.watch_setting)) {
            Text(
                if (watchConnected) {
                    stringResource(R.string.watch_status_on, "${stringResource(R.string.watch_device_name)} · ${stringResource(R.string.watch_simulated)}")
                } else {
                    stringResource(R.string.watch_status_off)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (watchConnected) {
                OutlinedButton(onClick = onWatchDisconnect) { Text(stringResource(R.string.watch_disconnect)) }
            } else {
                Button(onClick = onWatchConnect) { Text(stringResource(R.string.watch_connect)) }
            }
        }
        Section(stringResource(R.string.data_source_setting)) {
            Choice(stringResource(R.string.profile_demo), stringResource(R.string.profile_demo_hint), state.profile == Profile.DEMO) { onProfile(Profile.DEMO) }
            Choice(stringResource(R.string.profile_me), stringResource(R.string.profile_me_hint), state.profile == Profile.ME) { onProfile(Profile.ME) }
        }
        Section(stringResource(R.string.environment_setting)) {
            state.environments.forEach { env ->
                Choice(
                    environmentName(env.id, env.name),
                    stringResource(R.string.env_hint, env.gravityG, env.radiationMsvPerDay, env.commDelayMinOneWay),
                    env.id == state.environment?.id,
                ) { onEnvironment(env.id) }
            }
        }
        Section(stringResource(R.string.about_title)) {
            Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Text(stringResource(R.string.sign_out), Modifier.padding(start = 8.dp))
        }
        Text("", Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    CrewCard {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun Choice(title: String, hint: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
