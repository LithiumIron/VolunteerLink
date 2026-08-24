package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.organisation.manage.model.PostManagementPost

/** Loads the normalized data needed to manage one Volunteer Post. */
interface OrganisationPostManagementRepository {
    suspend fun loadPost(postId: String): PostManagementPost

    /** Shortlists or unshortlists one pending application for comparison. */
    suspend fun setApplicantShortlisted(
        postId: String,
        roleTemplateId: String,
        userId: String,
        isShortlisted: Boolean
    )
}
