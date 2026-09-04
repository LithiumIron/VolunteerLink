package com.example.volunteerlink.screens

import android.content.Context

// Purpose: Handles volunteer remember me preferences as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
class VolunteerRememberMePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(
        "volunteer_remember_me",
        Context.MODE_PRIVATE
    )

    // Purpose: Handles set remember me enabled as one reusable step in the Volunteer flow.
    // Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
    // State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
    fun setRememberMeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, enabled).apply()
    }

    // Purpose: Handles is remember me enabled as one reusable step in the Volunteer flow.
    // Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
    // State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
    fun isRememberMeEnabled(): Boolean =
        prefs.getBoolean(KEY_REMEMBER_ME, false) // opt-in, defaults false

    companion object {
        private const val KEY_REMEMBER_ME = "remember_me_enabled"
    }
}