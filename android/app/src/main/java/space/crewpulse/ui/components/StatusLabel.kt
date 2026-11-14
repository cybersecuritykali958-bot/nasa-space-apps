package space.crewpulse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.crewpulse.data.model.Status
import space.crewpulse.ui.theme.StatusColors

/** Status as text + color (accessible, not color-only). */
@Composable
fun StatusLabel(status: Status, modifier: Modifier = Modifier) {
    val label = when (status) {
        Status.GREEN -> "Within range"
        Status.YELLOW -> "Watch"
        Status.RED -> "Action needed"
        Status.UNKNOWN -> "No data"
    }
    val color = StatusColors.of(status)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(status, 14.dp)
            Text(
                label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}