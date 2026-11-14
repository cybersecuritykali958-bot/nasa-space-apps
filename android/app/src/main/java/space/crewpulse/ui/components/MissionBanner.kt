package space.crewpulse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.crewpulse.ui.theme.CrewColors

@Composable
fun MissionBanner(
    astronaut: String,
    missionDay: Int,
    environmentName: String,
    commDelayMin: String?,
    modifier: Modifier = Modifier,
) {
    CrewCard(modifier = modifier, accent = CrewColors.missionAccent) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Mission status", style = MaterialTheme.typography.titleSmall, color = CrewColors.missionAccent)
            Text(astronaut, style = MaterialTheme.typography.titleLarge)
            Text(
                "Day $missionDay · $environmentName",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (commDelayMin != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrewColors.commDelayBadge.copy(alpha = 0.25f),
                ) {
                    Text(
                        "Earth one-way delay ~ $commDelayMin min",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Synthetic demo data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}