package space.crewpulse.ui.who5

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.domain.Who5
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.theme.StatusColors

private val ITEMS = listOf(R.string.who5_q1, R.string.who5_q2, R.string.who5_q3, R.string.who5_q4, R.string.who5_q5)

/** Answer labels from 5 ("all of the time") down to 0 ("at no time"), as on the WHO form. */
private val ANSWERS = listOf(
    5 to R.string.who5_a5, 4 to R.string.who5_a4, 3 to R.string.who5_a3,
    2 to R.string.who5_a2, 1 to R.string.who5_a1, 0 to R.string.who5_a0,
)

/** WHO-5 Well-Being Index, every two weeks. Saved as the "who5" reading in percent (0-100). */
@Composable
fun Who5Screen(onSave: (Double) -> Boolean, onBack: () -> Unit) {
    val answers = remember { mutableStateListOf<Int?>(null, null, null, null, null) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val score = Who5.percent(answers)

    CrewScaffold(title = stringResource(R.string.who5_title), onBack = onBack) { pad ->
        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.who5_intro), style = MaterialTheme.typography.bodyLarge)
            ITEMS.indices.forEach { i ->
                CrewCard { Who5Item(i, answers[i]) { answers[i] = it; saved = false } }
            }

            CrewCard {
                if (score == null) {
                    Text(stringResource(R.string.who5_answer_all), style = MaterialTheme.typography.bodyLarge)
                } else {
                    val low = score < Who5.CUT_OFF_PCT
                    Text(
                        stringResource(R.string.who5_score, score.toInt()),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (low) StatusColors.yellow else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(if (low) R.string.who5_low else R.string.who5_ok),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Button(
                        onClick = { saved = onSave(score) },
                        enabled = !saved,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    ) { Text(stringResource(if (saved) R.string.who5_saved else R.string.who5_save)) }
                }
            }

            Text(
                stringResource(R.string.who5_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

/** One WHO-5 statement with its six answers; shared by this screen and the daily check-in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Who5Item(index: Int, selected: Int?, onAnswer: (Int) -> Unit) {
    Text("${index + 1}. ${stringResource(ITEMS[index])}", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
        ANSWERS.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onAnswer(value) },
                label = { Text("$value · ${stringResource(label)}") },
            )
        }
    }
}

/** Score line under the questions: percentage, and whether it is below the cut-off. */
@Composable
fun Who5ScoreText(score: Double?) {
    if (score == null) {
        Text(stringResource(R.string.who5_answer_all), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val low = score < Who5.CUT_OFF_PCT
    Text(
        stringResource(R.string.who5_score, score.toInt()),
        style = MaterialTheme.typography.titleMedium,
        color = if (low) StatusColors.yellow else MaterialTheme.colorScheme.primary,
    )
    Text(stringResource(if (low) R.string.who5_low else R.string.who5_ok), style = MaterialTheme.typography.bodyMedium)
}
