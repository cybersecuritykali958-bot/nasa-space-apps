package space.crewpulse.data

import android.content.Context

class OnboardingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasSeenOnboarding(): Boolean = prefs.getBoolean(KEY_SEEN, false)

    fun markOnboardingSeen() {
        prefs.edit().putBoolean(KEY_SEEN, true).apply()
    }

    private companion object {
        const val PREFS = "crewpulse_ui"
        const val KEY_SEEN = "onboarding_seen"
    }
}