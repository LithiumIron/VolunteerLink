package com.example.volunteerlink.navigation

object VolunteerOpportunityNavigationRoutes {
    const val VOLUNTEER_FAVOURITES_ROUTE = "volunteer_favourites"

    // Main bottom navigation destinations
    const val VOLUNTEER_HOME_ROUTE =
        "volunteer_home"

    const val VOLUNTEER_CHAT_ROUTE =
        "volunteer_chat"

    const val VOLUNTEER_SKILL_PATH_ROUTE =
        "volunteer_skill_path"

    const val VOLUNTEER_MAP_ROUTE =
        "volunteer_map"

    const val VOLUNTEER_PROFILE_ROUTE =
        "volunteer_profile"

    const val VOLUNTEER_EDIT_PROFILE_ROUTE =
        "volunteer_edit_profile"

    const val VOLUNTEER_CERTIFICATES_ROUTE=
        "volunteer_all_certificate"
    const val VOLUNTEER_SETTINGS_ROUTE=
        "volunteer_settings"


    // Volunteer Opportunity destinations
    const val VOLUNTEER_SEARCH_ROUTE =
        "volunteer_search"

    const val VOLUNTEER_MY_APPLICATIONS_ROUTE =
        "volunteer_my_applications"

    const val VOLUNTEER_NOTIFICATIONS_ROUTE =
        "volunteer_notifications"


    // Shared navigation arguments
    const val VOLUNTEER_EVENT_ID_ARGUMENT =
        "volunteerEventId"

    const val VOLUNTEER_ROLE_ID_ARGUMENT =
        "volunteerRoleId"

    const val VOLUNTEER_APPLICATION_ID_ARGUMENT =
        "volunteerApplicationId"

    const val VOLUNTEER_SKILL_PATH_ID_ARGUMENT =
        "volunteerSkillPathId"


    // Opportunity Details
    const val VOLUNTEER_OPPORTUNITY_DETAILS_ROUTE =
        "volunteer_opportunity_details/" +
                "{$VOLUNTEER_EVENT_ID_ARGUMENT}?recommendedRoleId={recommendedRoleId}&source={source}"

    fun createVolunteerOpportunityDetailsRoute(
        volunteerEventId: Int,
        recommendedRoleId: Int = -1,
        source: String = ""
    ): String {
        val base = "volunteer_opportunity_details/$volunteerEventId"
        return if (recommendedRoleId == -1) base
        else "$base?recommendedRoleId=$recommendedRoleId&source=$source"
    }


    // Role Details
    const val VOLUNTEER_ROLE_DETAILS_ROUTE =
        "volunteer_role_details/" +
                "{$VOLUNTEER_EVENT_ID_ARGUMENT}/" +
                "{$VOLUNTEER_ROLE_ID_ARGUMENT}"

    fun createVolunteerRoleDetailsRoute(
        volunteerEventId: Int,
        volunteerRoleId: Int
    ): String {
        return "volunteer_role_details/" +
                "$volunteerEventId/" +
                volunteerRoleId
    }


    // Role Application
    const val VOLUNTEER_ROLE_APPLICATION_ROUTE =
        "volunteer_role_application/" +
                "{$VOLUNTEER_EVENT_ID_ARGUMENT}/" +
                "{$VOLUNTEER_ROLE_ID_ARGUMENT}"

    fun createVolunteerRoleApplicationRoute(
        volunteerEventId: Int,
        volunteerRoleId: Int
    ): String {
        return "volunteer_role_application/" +
                "$volunteerEventId/" +
                volunteerRoleId
    }


    // Application Details
    const val VOLUNTEER_APPLICATION_DETAILS_ROUTE =
        "volunteer_application_details/" +
                "{$VOLUNTEER_APPLICATION_ID_ARGUMENT}"

    fun createVolunteerApplicationDetailsRoute(
        volunteerApplicationId: Int
    ): String {
        return "volunteer_application_details/" +
                volunteerApplicationId
    }


    // Certificate issued for a completed application
    const val VOLUNTEER_CERTIFICATE_ROUTE =
        "volunteer_certificate/" +
                "{$VOLUNTEER_APPLICATION_ID_ARGUMENT}"

    fun createVolunteerCertificateRoute(
        volunteerApplicationId: Int
    ): String {
        return "volunteer_certificate/" +
                volunteerApplicationId
    }


    // Skill Path Details
    const val VOLUNTEER_SKILL_PATH_DETAILS_ROUTE =
        "volunteer_skill_path_details/" +
                "{$VOLUNTEER_SKILL_PATH_ID_ARGUMENT}"

    fun createVolunteerSkillPathDetailsRoute(
        volunteerSkillPathId: String
    ): String {
        return "volunteer_skill_path_details/" +
                volunteerSkillPathId
    }
}
