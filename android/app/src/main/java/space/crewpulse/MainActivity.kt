package space.crewpulse

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
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

    private val nfc: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(this) }

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

    // While the app is in front, any NFC badge held to the phone is passed to sign-in.
    override fun onResume() {
        super.onResume()
        val badges = (application as CrewPulseApp).container.badges
        nfc?.enableReaderMode(
            this,
            { tag -> badges.onScan(badgeId(tag)) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
            null,
        )
    }

    override fun onPause() {
        super.onPause()
        nfc?.disableReaderMode(this)
    }

    /** Badge id: first NDEF text record if present (e.g. "CP-0001"), otherwise the tag UID. */
    private fun badgeId(tag: Tag): String {
        val text = runCatching {
            Ndef.get(tag)?.cachedNdefMessage?.records?.firstOrNull { it.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN }?.payload?.let { p ->
                val langLength = p[0].toInt() and 0x3F
                String(p, 1 + langLength, p.size - 1 - langLength, Charsets.UTF_8)
            }
        }.getOrNull()
        return text ?: "UID:" + tag.id.joinToString("") { "%02X".format(it) }
    }
}
