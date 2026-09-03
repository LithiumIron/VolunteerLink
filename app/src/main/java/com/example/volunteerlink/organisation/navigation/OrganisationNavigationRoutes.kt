package com.example.volunteerlink.organisation.navigation

/** Main destinations for the Organisation side of VolunteerLink. */
object OrganisationNavigationRoutes {
    const val HOME = "organisation_home"
    const val MANAGE = "organisation_manage"
    const val MANAGE_POSTS = "organisation_manage_posts"
    const val MANAGE_POST_DETAIL = "organisation_manage_post/{postId}"
    const val MANAGE_POST_EDIT = "organisation_manage_post/{postId}/edit"
    const val MANAGE_APPLICANT_REVIEW =
        "organisation_manage_post/{postId}/applicant/{roleTemplateId}/{userId}"
    const val MANAGE_IMPACT_WEAVE = "organisation_manage_impact_weave"
    const val MANAGE_PROMOTIONS = "organisation_manage_promotions"
    const val CREATE = "organisation_create"
    const val CHATS = "organisation_chats"
    const val CHAT_ID_ARGUMENT = "chatId"

    const val CHAT_ROOM =
        "organisation_chat_room/{$CHAT_ID_ARGUMENT}"

    const val PARTNERSHIP_CHAT_ROOM =
        "organisation_partnership_chat_room/{$CHAT_ID_ARGUMENT}"

    const val GROUP_INFO =
        "organisation_group_info/{$CHAT_ID_ARGUMENT}"
    const val PROFILE = "organisation_profile"
    const val EDIT_PROFILE = "organisation_edit_profile"
    const val SETTINGS = "organisation_settings"

    fun managePostDetail(postId: String): String =
        "organisation_manage_post/$postId"

    fun managePostEdit(postId: String): String =
        "organisation_manage_post/$postId/edit"

    fun manageApplicantReview(
        postId: String,
        roleTemplateId: String,
        userId: String
    ): String =
        "organisation_manage_post/$postId/applicant/$roleTemplateId/$userId"

    fun chatRoom(chatId: String): String =
        "organisation_chat_room/$chatId"

    fun partnershipChatRoom(chatId: String): String =
        "organisation_partnership_chat_room/$chatId"

    fun groupInfo(chatId: String): String =
        "organisation_group_info/$chatId"
}