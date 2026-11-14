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
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import space.crewpulse.AppContainer
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: CheckInViewModel = viewModel(factory = CheckInViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val view = LocalView.current

    LaunchedEffect(state.savedMessage) {
        val msg = state.savedMessage ?: return@LaunchedEffect
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        snackbar.showSnackbar(msg)
        vm.consumeSavedMessage()
        onBack()
    }

    CrewScaffold(
        title = "Daily check-in",
        subtitle = "Glove-friendly controls",
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { paddingModifier ->
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.loadError != null -> Text(
                "Could not load form: ${state.loadError}",
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
                    "Enter vitals from your cuff, glucometer, and self-report.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BloodPressureCard(state.bloodPressure, vm::updateSystolic, vm::updateDiastolic, vm::toggleSystolicHint, vm::toggleDiastolicHint)
                state.fields.forEachIndexed { index, field ->
                    if (field.indicator.id == "mood") {
                        MoodCard(field, vm::setMood) { vm.toggleFieldHint(index) }
                    } else {
                        MetricCard(field, { vm.updateField(index, it) }) { vm.toggleFieldHint(index) }
                    }
                }
                Button(
                    onClick = vm::submit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Save check-in", style = MaterialTheme.typography.titleMedium)
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
            Text("Blood pressure", style = MaterialTheme.typography.titleMedium)
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
private fun MoodCard(field: CheckInField, onSelect: (Int) -> Unit, onToggleHint: () -> Unit) {
    val selected = field.valueText.toIntOrNull()
    CrewCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Mood (1 = very low, 5 = very good)", style = MaterialTheme.typography.titleMedium)
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
            HintBlock(field.hintExpanded, field.indicator.howToMeasure, onToggleHint)
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
            label = { Text("${field.indicator.name} (${field.indicator.unit})") },
            isError = field.error != null,
            supportingText = field.error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
        )
        HintBlock(field.hintExpanded, field.indicator.howToMeasure, onToggleHint)
    }
}

@Composable
private fun HintBlock(expanded: Boolean, howToMeasure: String, onToggle: () -> Unit) {
    TextButton(onClick = onToggle) {
        Text(if (expanded) "Hide how to measure" else "How to measure", color = MaterialTheme.colorScheme.secondary)
    }
    if (expanded) Text(howToMeasure, style = MaterialTheme.typography.bodyMedium)
}