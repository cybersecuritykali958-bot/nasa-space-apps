package space.crewpulse.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.ui.graphics.vector.ImageVector

/** Outlined icons for body systems and indicators. */
object CrewIcons {
    fun system(id: String): ImageVector = when (id) {
        "cardiovascular" -> Icons.Outlined.FavoriteBorder
        "metabolic" -> Icons.Outlined.WaterDrop
        "immune" -> Icons.Outlined.Thermostat
        "behavioral" -> Icons.Outlined.Bedtime
        "musculoskeletal" -> Icons.Outlined.FitnessCenter
        "vision" -> Icons.Outlined.Visibility
        "radiation" -> Icons.Outlined.WbSunny
        else -> Icons.Outlined.Air
    }

    fun indicator(id: String): ImageVector = when (id) {
        "systolic_bp", "diastolic_bp", "heart_rate" -> Icons.Outlined.FavoriteBorder
        "glucose" -> Icons.Outlined.WaterDrop
        "body_temp" -> Icons.Outlined.Thermostat
        "sleep_hours" -> Icons.Outlined.Bedtime
        "mood" -> Icons.Outlined.Mood
        "pvt_mean_rt" -> Icons.Outlined.Psychology
        "cumulative_dose" -> Icons.Outlined.WbSunny
        else -> Icons.Outlined.Speed
    }
}
