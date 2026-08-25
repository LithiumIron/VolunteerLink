package com.example.volunteerlink.organisation.home.model

/**
 * Database data needed by the Organisation Home feature.
 *
 * These models deliberately mirror only the normalized columns Home needs.
 * They are not UI models and they do not calculate date-dependent states.
 */
data class OrganisationHomeSnapshot(
    val organisationId: String,
    val organisationName: String,
    val posts: List<OrganisationHomePost>
)

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
    val schedules: List<OrganisationHomeSchedule> = emptyList(),
    val roles: List<OrganisationHomeRole> = emptyList()
)

data class OrganisationHomeSchedule(
    val scheduleItemId: String,
    val scheduleType: String,
    val scheduleDate: String,
    val title: String,
    val startTime: String? = null,
    val trainingMode: String? = null,
    val meetingLink: String? = null,
    val trainingLocationMode: String? = null,
    val trainingLocationName: String? = null
)

/**
 * Only the role/application information Home needs.
 * Detailed applicant profiles remain the responsibility of the application feature.
 */
data class OrganisationHomeRole(
    val roleTemplateId: String,
    val roleName: String,
    val roleMode: String,
    val applicationMethod: String,
    val participations: List<OrganisationHomeParticipation> = emptyList(),
    val applicationClosingSchedules: List<OrganisationHomeRoleClosingSchedule> = emptyList()
)

data class OrganisationHomeParticipation(
    val userId: String,
    val applicationStatus: String
)

/**
 * A schedule explicitly configured to close applications for one role when it starts.
 * The schedule row remains normalized in schedule_items + schedule_item_roles; Home only
 * carries the small piece of that relationship needed to derive whether review is open.
 */
data class OrganisationHomeRoleClosingSchedule(
    val scheduleItemId: String,
    val scheduleDate: String,
    val startTime: String? = null
)
