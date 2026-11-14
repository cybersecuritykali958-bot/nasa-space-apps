package space.crewpulse.ui.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** In-app language switch without extra libraries: wraps the activity context with the chosen locale. */
object LocaleHelper {

    data class Language(val code: String, val nativeName: String)

    val LANGUAGES = listOf(
        Language("en", "English"),
        Language("uk", "Українська"),
        Language("ja", "日本語"),
        Language("de", "Deutsch"),
    )

    fun wrap(base: Context, code: String): Context {
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
