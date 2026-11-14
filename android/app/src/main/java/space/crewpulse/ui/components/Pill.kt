package space.crewpulse.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.data.model.Status
import space.crewpulse.domain.ConfidenceLevel
import space.crewpulse.domain.Verdict
import space.crewpulse.ui.theme.StatusColors

/** Filled status chip with dark text, e.g. "Elevated", "NO-GO" (Material 3 style). */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = color) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = StatusColors.onChip,
        )
    }
}

@Composable
fun statusWord(status: Status): String = stringResource(
    when (status) {
        Status.GREEN -> R.string.status_normal
        Status.YELLOW -> R.string.status_elevated
        Status.RED -> R.string.status_action
        Status.UNKNOWN -> R.string.no_data
    },
)

@Composable
fun StatusPill(status: Status, modifier: Modifier = Modifier) =
    Pill(statusWord(status), StatusColors.of(status), modifier)

@Composable
fun VerdictPill(verdict: Verdict, modifier: Modifier = Modifier) = when (verdict) {
    Verdict.GO -> Pill(stringResource(R.string.verdict_go), StatusColors.green, modifier)
    Verdict.CAUTION -> Pill(stringResource(R.string.verdict_caution), StatusColors.yellow, modifier)
    Verdict.NO_GO -> Pill(stringResource(R.string.verdict_nogo), StatusColors.red, modifier)
}

@Composable
fun ConfidencePill(level: ConfidenceLevel, modifier: Modifier = Modifier) = when (level) {
    ConfidenceLevel.HIGH -> Pill(stringResource(R.string.conf_high), StatusColors.green, modifier)
    ConfidenceLevel.MEDIUM -> Pill(stringResource(R.string.conf_medium), StatusColors.yellow, modifier)
    ConfidenceLevel.LOW -> Pill(stringResource(R.string.conf_low), StatusColors.red, modifier)
}
