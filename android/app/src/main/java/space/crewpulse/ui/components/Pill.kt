package space.crewpulse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import space.crewpulse.data.model.Status
import space.crewpulse.domain.ConfidenceLevel
import space.crewpulse.domain.Verdict
import space.crewpulse.ui.theme.StatusColors

/** Small colored badge with text, e.g. "Elevated", "GO", "CAUTION". */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.8f)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

fun statusWord(status: Status): String = when (status) {
    Status.GREEN -> "Normal"
    Status.YELLOW -> "Elevated"
    Status.RED -> "Action needed"
    Status.UNKNOWN -> "No data"
}

@Composable
fun StatusPill(status: Status, modifier: Modifier = Modifier) =
    Pill(statusWord(status), StatusColors.of(status), modifier)

@Composable
fun VerdictPill(verdict: Verdict, modifier: Modifier = Modifier) = when (verdict) {
    Verdict.GO -> Pill("GO", StatusColors.green, modifier)
    Verdict.CAUTION -> Pill("CAUTION", StatusColors.yellow, modifier)
    Verdict.NO_GO -> Pill("NO-GO", StatusColors.red, modifier)
}

@Composable
fun ConfidencePill(level: ConfidenceLevel, modifier: Modifier = Modifier) = when (level) {
    ConfidenceLevel.HIGH -> Pill("High", StatusColors.green, modifier)
    ConfidenceLevel.MEDIUM -> Pill("Medium", StatusColors.yellow, modifier)
    ConfidenceLevel.LOW -> Pill("Low", StatusColors.red, modifier)
}
