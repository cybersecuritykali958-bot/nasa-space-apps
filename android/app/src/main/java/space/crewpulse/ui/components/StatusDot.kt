package space.crewpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import space.crewpulse.data.model.Status
import space.crewpulse.ui.theme.StatusColors

@Composable
fun StatusDot(status: Status, size: Dp = 14.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(StatusColors.of(status)),
    )
}
