package com.example.volunteerlink.navigation

/**
 * Routes for the highest level of the app.
 *
 * Authentication is not implemented yet, so the app starts with a temporary
 * choice between the Volunteer and Organisation sides.
 */
object AppRoutes {
    const val USER_TYPE_SELECTION = "user_type_selection"
    const val VOLUNTEER = "volunteer"
    const val ORGANISATION = "organisation"
}
