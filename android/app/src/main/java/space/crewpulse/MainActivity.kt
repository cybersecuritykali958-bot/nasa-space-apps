package space.crewpulse

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import space.crewpulse.data.SettingsStore
import space.crewpulse.ui.i18n.LocaleHelper
import space.crewpulse.ui.navigation.AppNavHost
import space.crewpulse.ui.theme.CrewPulseTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Settings are read directly: the app container does not exist yet at this point.
        super.attachBaseContext(LocaleHelper.wrap(newBase, SettingsStore(newBase).language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark app: light status bar and navigation bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val container = (application as CrewPulseApp).container
        setContent {
            CrewPulseTheme {
                // Surface sets the default content color, so plain Text is light on the dark background.
                Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
                AppNavHost(
                    container = container,
                    onLanguageChange = { code ->
                        container.settings.setLanguage(code)
                        recreate()
                    },
                )
                }
            }
        }
    }
}
