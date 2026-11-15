package space.crewpulse.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.crewpulse.R
import space.crewpulse.ui.i18n.LocaleHelper

/** Current app language and how to change it; provided once for all tabs. */
data class LanguageControl(val current: String, val onChange: (String) -> Unit)

val LocalLanguageControl = staticCompositionLocalOf<LanguageControl?> { null }

/** Tab title on the left, language menu on the right. */
@Composable
fun TabHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).padding(top = 4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LanguageMenu()
    }
}

/** Globe button with the current language code; the list opens as a dropdown. */
@Composable
fun LanguageMenu() {
    val control = LocalLanguageControl.current ?: return
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Outlined.Language, contentDescription = stringResource(R.string.language), modifier = Modifier.size(20.dp))
            Text(control.current.uppercase(), Modifier.padding(start = 6.dp))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LocaleHelper.LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.nativeName) },
                    leadingIcon = {
                        if (lang.code == control.current) Icon(Icons.Filled.Check, contentDescription = null)
                        else Spacer(Modifier.size(24.dp))
                    },
                    onClick = {
                        open = false
                        if (lang.code != control.current) control.onChange(lang.code)
                    },
                )
            }
        }
    }
}
