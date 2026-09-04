package com.example.volunteerlink.organisation

import android.content.Context

class OrganisationRememberMePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(
        "organisation_remember_me",
        Context.MODE_PRIVATE
    )

    fun setRememberMeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, enabled).apply()
    }

    fun isRememberMeEnabled(): Boolean =
        prefs.getBoolean(KEY_REMEMBER_ME, false)

    companion object {
        private const val KEY_REMEMBER_ME = "remember_me_enabled"
    }
}