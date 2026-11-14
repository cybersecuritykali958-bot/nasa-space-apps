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
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.i18n.LocaleHelper
import space.crewpulse.ui.i18n.environmentName

/** Language, data source and mission environment. */
@Composable
fun SettingsScreen(
    state: HomeUiState,
    currentLanguage: String,
    onLanguage: (String) -> Unit,
    onProfile: (Profile) -> Unit,
    onEnvironment: (String) -> Unit,
    watchConnected: Boolean,
    onWatchConnect: () -> Unit,
    onWatchDisconnect: () -> Unit,
    onBack: () -> Unit,
) {
    CrewScaffold(title = stringResource(R.string.settings_title), onBack = onBack) { pad ->
        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section(stringResource(R.string.language)) {
                LocaleHelper.LANGUAGES.forEach { lang ->
                    Choice(lang.nativeName, null, lang.code == currentLanguage) {
                        if (lang.code != currentLanguage) onLanguage(lang.code)
                    }
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
            Text("", Modifier.padding(bottom = 8.dp))
        }
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
