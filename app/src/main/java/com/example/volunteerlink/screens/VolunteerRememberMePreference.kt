package com.example.volunteerlink.screens

import android.content.Context

class VolunteerRememberMePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(
        "volunteer_remember_me",
        Context.MODE_PRIVATE
    )

    fun setRememberMeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, enabled).apply()
    }

    fun isRememberMeEnabled(): Boolean =
        prefs.getBoolean(KEY_REMEMBER_ME, false) // opt-in, defaults false

    companion object {
        private const val KEY_REMEMBER_ME = "remember_me_enabled"
    }
}