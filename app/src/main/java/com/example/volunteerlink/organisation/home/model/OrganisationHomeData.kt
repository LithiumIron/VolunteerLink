package com.example.volunteerlink.organisation.home.model

import kotlinx.serialization.Serializable

/**
 * Database data needed by the Organisation Home feature.
 *
 * These models deliberately mirror only the normalized columns Home needs.
 * They are not UI models and they do not calculate date-dependent states.
 */
@Serializable
data class OrganisationHomeSnapshot(
    val organisationId: String,
    val organisationName: String,
    val posts: List<OrganisationHomePost>
)

@Serializable
data class OrganisationHomePost(
    val postId: String,
    val title: String,
    val mode: String,
    val status: String,
    val category: String? = null,
    val physicalStartDate: String? = null,
    val physicalEndDate: String? = null,
    val physicalStartTime: String? = null,
    val physicalLocationName: String? = null,
    val remoteStartDate: String? = null,
    val remoteEndDate: String? = null,
    val remoteNewEndDate: String? = null,
    val schedules: List<OrganisationHomeSchedule> = emptyList(),
    val roles: List<OrganisationHomeRole> = emptyList()
) {
    val effectiveRemoteEndDate: String?
        get() = remoteNewEndDate?.takeIf { it.isNotBlank() } ?: remoteEndDate
}

@Serializable
data class OrganisationHomeSchedule(
    val scheduleItemId: String,
    val scheduleType: String,
    val scheduleDate: String,
    val title: String,
    val startTime: String? = null,
)

/**
 * Only the role/application information Home needs.
 * Detailed applicant profiles remain the responsibility of the application feature.
 */
@Serializable
data class OrganisationHomeRole(
    val roleTemplateId: String,
    val roleName: String,
    val roleMode: String,
    val applicationMethod: String,
    val participations: List<OrganisationHomeParticipation> = emptyList()
)

@Serializable
data class OrganisationHomeParticipation(
    val userId: String,
    val applicationStatus: String
)
