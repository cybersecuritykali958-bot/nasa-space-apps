package space.crewpulse.ui.earth

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import space.crewpulse.R
import space.crewpulse.data.CrewMember
import space.crewpulse.domain.EarthPacket
import space.crewpulse.domain.Responder
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.home.HomeUiState
import space.crewpulse.ui.theme.StatusColors

/**
 * Report for the flight surgeon: what will be sent, when Earth receives it and when an answer can come back.
 * The downlink itself is simulated; "Share" hands the same text to any real channel on the phone.
 */
@Composable
fun EarthReportScreen(state: HomeUiState, member: CrewMember?, onBack: () -> Unit) {
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }
    val packet = remember(state.report, member) {
        EarthPacket.build(state.report, member?.name, member?.badgeId, state.missionDay, state.environmentName, now)
    }
    val timing = EarthPacket.timing(state.report, state.commDelayMin)
    var sentAt by rememberSaveable { mutableLongStateOf(0L) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sentAt) {
        while (sentAt > 0) {
            clock = System.currentTimeMillis()
            delay(1_000)
        }
    }

    CrewScaffold(title = stringResource(R.string.earth_title), onBack = onBack) { pad ->
        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CrewCard {
                if (timing != null) {
                    Text(
                        if (timing.oneWayMin < 1.0) stringResource(R.string.earth_one_way_s, timing.oneWayMin * 60)
                        else stringResource(R.string.earth_one_way, timing.oneWayMin),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    timing.earliestReplyMin?.let {
                        Text(stringResource(R.string.earth_reply, it), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                    }
                } else {
                    Text(stringResource(R.string.earth_no_env), style = MaterialTheme.typography.bodyLarge)
                }
                if (state.report.triage.responder == Responder.ONBOARD_PROTOCOL) {
                    Text(
                        stringResource(R.string.earth_dont_wait),
                        style = MaterialTheme.typography.titleMedium,
                        color = StatusColors.red,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            if (sentAt > 0 && timing != null) {
                CrewCard {
                    val elapsedMin = (clock - sentAt) / 60_000.0
                    Countdown(stringResource(R.string.earth_arrives_in), stringResource(R.string.earth_delivered), timing.oneWayMin - elapsedMin)
                    timing.earliestReplyMin?.let {
                        Countdown(stringResource(R.string.earth_reply_in), stringResource(R.string.earth_reply_open), it - elapsedMin)
                    }
                    Text(
                        stringResource(R.string.earth_simulated),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { sentAt = System.currentTimeMillis(); clock = sentAt }, enabled = sentAt == 0L) {
                    Icon(Icons.AutoMirrored.Outlined.Send, null)
                    Text(stringResource(R.string.earth_queue), Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, packet)
                    context.startActivity(Intent.createChooser(send, null))
                }) {
                    Icon(Icons.Outlined.Share, null)
                    Text(stringResource(R.string.earth_share), Modifier.padding(start = 8.dp))
                }
            }

            Text(
                stringResource(R.string.earth_packet, packet.toByteArray().size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    packet,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Text("", Modifier.padding(bottom = 8.dp))
        }
    }
}

@Composable
private fun Countdown(pending: String, done: String, minutesLeft: Double) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (minutesLeft <= 0) {
            Icon(Icons.Outlined.CheckCircle, null, tint = StatusColors.green)
            Text(done, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
        } else {
            val total = (minutesLeft * 60).toLong()
            Text("$pending ${"%d:%02d".format(total / 60, total % 60)}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
