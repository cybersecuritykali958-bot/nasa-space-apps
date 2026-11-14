package space.crewpulse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlaceholderScreen(title: String, owner: String, todo: List<String>, onBack: () -> Unit) {
    CrewScaffold(title = title, subtitle = "Coming soon", onBack = onBack) { paddingModifier ->
        Column(
            Modifier
                .fillMaxSize()
                .then(paddingModifier)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CrewCard {
                Text("Owner: $owner", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            CrewCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backlog", style = MaterialTheme.typography.titleSmall)
                    todo.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
                }
            }
        }
    }
}