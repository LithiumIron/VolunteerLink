package com.example.volunteerlink.organisation.navigation

/** Main destinations for the Organisation side of VolunteerLink. */
object OrganisationNavigationRoutes {
    const val HOME = "organisation_home"
    const val MANAGE = "organisation_manage"
    const val MANAGE_POSTS = "organisation_manage_posts"
    const val MANAGE_POST_DETAIL = "organisation_manage_post/{postId}"
    const val MANAGE_IMPACT_WEAVE = "organisation_manage_impact_weave"
    const val MANAGE_PROMOTIONS = "organisation_manage_promotions"
    const val CREATE = "organisation_create"
    const val CHATS = "organisation_chats"
    const val PROFILE = "organisation_profile"

    fun managePostDetail(postId: String): String =
        "organisation_manage_post/$postId"
}
