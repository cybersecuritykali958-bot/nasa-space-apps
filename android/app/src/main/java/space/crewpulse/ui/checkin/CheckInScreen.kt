package space.crewpulse.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import space.crewpulse.AppContainer
import space.crewpulse.R
import space.crewpulse.ui.i18n.indicatorHow
import space.crewpulse.ui.i18n.indicatorName
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.who5.Who5Item
import space.crewpulse.ui.who5.Who5ScoreText
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: CheckInViewModel = viewModel(factory = CheckInViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val savedText = stringResource(R.string.checkin_saved)

    LaunchedEffect(state.savedMessage) {
        if (state.savedMessage == null) return@LaunchedEffect
        snackbar.showSnackbar(savedText)
        vm.consumeSavedMessage()
        onBack()
    }

    CrewScaffold(
        title = stringResource(R.string.checkin_title),
        subtitle = stringResource(R.string.checkin_subtitle),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { paddingModifier ->
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.loadError != null -> Text(
                stringResource(R.string.data_error, state.loadError.orEmpty()),
                color = MaterialTheme.colorScheme.error,
                modifier = paddingModifier.padding(16.dp),
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .then(paddingModifier)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.checkin_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BloodPressureCard(state.bloodPressure, vm::updateSystolic, vm::updateDiastolic, vm::toggleSystolicHint, vm::toggleDiastolicHint)
                state.fields.forEachIndexed { index, field ->
                    if (field.indicator.id == "mood") {
                        MoodCard(field, vm::setMood, { vm.toggleFieldHint(index) }, state, vm::toggleWho5, vm::setWho5Answer)
                    } else {
                        MetricCard(field, { vm.updateField(index, it) }) { vm.toggleFieldHint(index) }
                    }
                }
                Button(
                    onClick = vm::submit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(stringResource(R.string.save_checkin), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun BloodPressureCard(
    state: BloodPressureState,
    onSystolic: (String) -> Unit,
    onDiastolic: (String) -> Unit,
    onToggleSystolicHint: () -> Unit,
    onToggleDiastolicHint: () -> Unit,
) {
    CrewCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.blood_pressure), style = MaterialTheme.typography.titleMedium)
            state.systolic?.let {
                MetricTextField(it, onSystolic, onToggleSystolicHint)
            }
            state.diastolic?.let {
                MetricTextField(it, onDiastolic, onToggleDiastolicHint)
            }
        }
    }
}

@Composable
private fun MetricCard(field: CheckInField, onValueChange: (String) -> Unit, onToggleHint: () -> Unit) {
    CrewCard { MetricTextField(field, onValueChange, onToggleHint) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodCard(
    field: CheckInField,
    onSelect: (Int) -> Unit,
    onToggleHint: () -> Unit,
    state: CheckInUiState,
    onToggleWho5: () -> Unit,
    onWho5Answer: (Int, Int) -> Unit,
) {
    val selected = field.valueText.toIntOrNull()
    CrewCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.mood_scale), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { score ->
                    FilterChip(
                        selected = selected == score,
                        onClick = { onSelect(score) },
                        label = { Text("$score", style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        ),
                    )
                }
            }
            field.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            HintBlock(field.hintExpanded, indicatorHow(field.indicator), onToggleHint)

            // The 1-5 rating stays (the engine's fatigue rules use it); the validated WHO-5 test unfolds below it.
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggleWho5).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.checkin_who5_open), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.checkin_who5_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (state.who5Expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }
            AnimatedVisibility(visible = state.who5Expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.who5_intro), style = MaterialTheme.typography.bodyMedium)
                    state.who5Answers.forEachIndexed { i, answer ->
                        Column { Who5Item(i, answer) { onWho5Answer(i, it) } }
                    }
                    Who5ScoreText(state.who5Score)
                    if (state.who5Incomplete) {
                        Text(stringResource(R.string.checkin_who5_incomplete), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(stringResource(R.string.who5_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MetricTextField(field: CheckInField, onValueChange: (String) -> Unit, onToggleHint: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = field.valueText,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            label = { Text("${indicatorName(field.indicator)} (${field.indicator.unit})") },
            isError = field.error != null,
            supportingText = field.error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
        )
        if (field.fromWatch) {
            Text(
                stringResource(R.string.watch_from_watch),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        HintBlock(field.hintExpanded, indicatorHow(field.indicator), onToggleHint)
    }
}

@Composable
private fun HintBlock(expanded: Boolean, howToMeasure: String, onToggle: () -> Unit) {
    TextButton(onClick = onToggle) {
        Text(stringResource(if (expanded) R.string.hide_how_to_measure else R.string.how_to_measure), color = MaterialTheme.colorScheme.secondary)
    }
    if (expanded) Text(howToMeasure, style = MaterialTheme.typography.bodyMedium)
}