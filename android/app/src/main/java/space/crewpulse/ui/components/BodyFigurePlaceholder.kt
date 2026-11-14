package space.crewpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import space.crewpulse.data.model.Status
import space.crewpulse.domain.BodySystems
import space.crewpulse.ui.home.SystemStatus
import space.crewpulse.ui.theme.StatusColors

/**
 * Stand-in for the Figma body SVG. Tap a system chip to jump to its first indicator below.
 * TODO(@harvio1): swap for VectorDrawable paths when design assets land.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BodyFigurePlaceholder(
    systems: List<SystemStatus>,
    onSystemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CrewCard(modifier = modifier, accent = MaterialTheme.colorScheme.outline) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Body systems overview", style = MaterialTheme.typography.titleSmall)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CREW", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Interactive figure coming from design",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                systems.forEach { system ->
                    SystemChip(
                        label = BodySystems.label(system.systemId),
                        status = system.status,
                        onClick = { onSystemClick(system.systemId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemChip(label: String, status: Status, onClick: () -> Unit) {
    val color = StatusColors.of(status)
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        RowWithDot(label, status)
    }
}

@Composable
private fun RowWithDot(label: String, status: Status) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(status, 12.dp)
        Text(
            label,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}