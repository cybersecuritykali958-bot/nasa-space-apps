package space.crewpulse

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        enableEdgeToEdge()
        val container = (application as CrewPulseApp).container
        setContent {
            CrewPulseTheme {
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
