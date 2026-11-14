package space.crewpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import space.crewpulse.ui.theme.CrewGradients

@Composable
fun CrewBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(CrewGradients.cabinBackdrop),
    ) {
        content()
    }
}

object CrewGradients {
    val cabinBackdrop = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A1628),
            Color(0xFF050D1A),
            Color(0xFF0B1D3A),
        ),
    )
}