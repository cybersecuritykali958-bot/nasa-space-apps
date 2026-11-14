package space.crewpulse.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small persisted settings. SharedPreferences is enough here, no extra dependency needed. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("crewpulse_settings", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(
        prefs.getString(KEY_PROFILE, null)?.let { runCatching { Profile.valueOf(it) }.getOrNull() } ?: Profile.DEMO,
    )
    val profile: StateFlow<Profile> = _profile

    /** Mission environment id from environments.json (ISS, Moon, Mars...). */
    private val _environmentId = MutableStateFlow(prefs.getString(KEY_ENVIRONMENT, null) ?: DEFAULT_ENVIRONMENT)
    val environmentId: StateFlow<String> = _environmentId

    fun setEnvironment(id: String) {
        prefs.edit().putString(KEY_ENVIRONMENT, id).apply()
        _environmentId.value = id
    }

    fun setProfile(profile: Profile) {
        prefs.edit().putString(KEY_PROFILE, profile.name).apply()
        _profile.value = profile
    }

    private companion object {
        const val KEY_PROFILE = "profile"
        const val KEY_ENVIRONMENT = "environment"
        const val DEFAULT_ENVIRONMENT = "mars_transit"
    }
}
