package space.crewpulse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class DemoDayPreset(val day: Int?, val label: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DemoDaySelector(
    selectedDay: Int?,
    presets: List<DemoDayPreset>,
    onSelect: (Int?) -> Unit,
) {
    CrewCard {
        Text("Demo mission day", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { preset ->
                FilterChip(
                    selected = selectedDay == preset.day,
                    onClick = { onSelect(preset.day) },
                    label = { Text(preset.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    ),
                )
            }
        }
    }
}