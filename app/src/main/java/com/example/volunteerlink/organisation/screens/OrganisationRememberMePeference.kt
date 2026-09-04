package com.example.volunteerlink.organisation

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements the Organisation UI associated with Organisation Remember Me Peference.
//
// The composable layer is responsible for layout, interaction and displaying loading/error/validation state;
// business rules and persistence are delegated to ViewModels/repositories.
//
// This separation makes it clear during maintenance which code changes appearance versus which code changes real
// server data.
//
// Where the screen displays cached information, server-changing actions remain disabled or routed through a fresh
// authenticated repository operation.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import android.content.Context

/**
 * Stores only the Organisation sign-in screen's small "Remember me" preference.
 *
 * SharedPreferences is appropriate here because the value is one simple boolean,
 * not a relational record or a large object graph. The actual authenticated
 * Supabase session remains owned by the Supabase Auth SDK.
 *
 * SECURITY BOUNDARY:
 * - stored here: whether the user asked VolunteerLink to remember the sign-in choice;
 * - NOT stored here: password, OTP, access token, refresh token or organisation data.
 *
 * Larger Organisation data that needs offline fallback is handled separately by
 * OrganisationLocalStorage using account-scoped internal JSON files.
 */
/**
 * DETAILED DECLARATION — OrganisationRememberMePreferences
 *
 * Domain/UI type for Organisation Remember Me Preferences used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
class OrganisationRememberMePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(
        "organisation_remember_me",
        Context.MODE_PRIVATE
    )

    /** Saves the user's UI preference; it does not create or extend a login session. */
    /**
     * DETAILED BEHAVIOUR — setRememberMeEnabled
     *
     * Handles the Compose/UI responsibility for set remember me enabled.
     *
     * UI-only work stays here; business validation and Supabase persistence remain delegated to the
     * ViewModel/repository layers.
     */
    fun setRememberMeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, enabled).apply()
    }

    /** Returns false for a fresh install because remembering sign-in is opt-in. */
    /**
     * DETAILED BEHAVIOUR — isRememberMeEnabled
     *
     * Handles the Compose/UI responsibility for is remember me enabled.
     *
     * UI-only work stays here; business validation and Supabase persistence remain delegated to the
     * ViewModel/repository layers.
     */
    fun isRememberMeEnabled(): Boolean =
        prefs.getBoolean(KEY_REMEMBER_ME, false)

    companion object {
        private const val KEY_REMEMBER_ME = "remember_me_enabled"
    }
}
