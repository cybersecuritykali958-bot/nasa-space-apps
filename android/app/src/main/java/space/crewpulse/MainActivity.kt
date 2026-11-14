package space.crewpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import space.crewpulse.ui.components.CrewBackground
import space.crewpulse.ui.navigation.AppNavHost
import space.crewpulse.ui.theme.CrewPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CrewPulseApp).container
        setContent {
            CrewPulseTheme {
                CrewBackground {
                    AppNavHost(container)
                }
            }
        }
    }
}
