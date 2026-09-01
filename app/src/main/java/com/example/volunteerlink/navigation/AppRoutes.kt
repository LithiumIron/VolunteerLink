package com.example.volunteerlink.navigation

/**
 * Routes for the highest level of the app.
 *
 * The volunteer route uses Supabase Auth. The organisation branch remains
 * owned by its module and can adopt the same root routing contract later.
 */
object AppRoutes {
    const val USER_TYPE_SELECTION = "user_type_selection"
    const val VOLUNTEER_LOGIN = "volunteer_login"
    const val VOLUNTEER = "volunteer"

    const val ORGANISATION_LOGIN = "organisation_login"
    const val ORGANISATION_SIGNUP = "organisation_signup"
    const val ORGANISATION = "organisation"
}
