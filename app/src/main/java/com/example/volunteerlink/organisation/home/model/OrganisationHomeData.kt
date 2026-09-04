package com.example.volunteerlink.organisation.home.model

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines immutable Home dashboard data/state associated with Organisation Home Data.
//
// The repository/ViewModel fills these models from authenticated Supabase reads and derived timing/application
// information.
//
// Compose consumes these models directly, which keeps raw JSON/table rows out of the Home screen and makes
// loading/error/cached states explicit.
//
// Architectural layer: Domain/UI model layer.
// ============================================================================


import kotlinx.serialization.Serializable

/**
 * Database data needed by the Organisation Home feature.
 *
 * These models deliberately mirror only the normalized columns Home needs.
 * They are not UI models and they do not calculate date-dependent states.
 */
@Serializable
/**
 * DETAILED DECLARATION — OrganisationHomeSnapshot
 *
 * Domain/UI type for Organisation Home Snapshot used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationHomeSnapshot(
    val organisationId: String,
    val organisationName: String,
    val profileImageUrl: String? = null,
    val posts: List<OrganisationHomePost>,
    val impactWeaveAttention: List<OrganisationImpactWeaveAttention> = emptyList()
)

@Serializable
/**
 * Holds the values represented by organisation home post as one strongly typed model.
 * It keeps related Home dashboard values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — OrganisationHomePost
 *
 * Domain/UI type for Organisation Home Post used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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
/**
 * Holds the values represented by organisation home schedule as one strongly typed model.
 * It keeps related Home dashboard values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — OrganisationHomeSchedule
 *
 * Domain/UI type for Organisation Home Schedule used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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
/**
 * DETAILED DECLARATION — OrganisationHomeRole
 *
 * Domain/UI type for Organisation Home Role used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationHomeRole(
    val roleTemplateId: String,
    val roleName: String,
    val roleMode: String,
    val applicationMethod: String,
    val participations: List<OrganisationHomeParticipation> = emptyList()
)

@Serializable
/**
 * Holds the values represented by organisation home participation as one strongly typed model.
 * It keeps related Home dashboard values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — OrganisationHomeParticipation
 *
 * Domain/UI type for Organisation Home Participation used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationHomeParticipation(
    val userId: String,
    val applicationStatus: String
)


@Serializable
/**
 * Holds the values represented by organisation impact weave attention as one strongly typed model.
 * It keeps related Home dashboard values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — OrganisationImpactWeaveAttention
 *
 * Domain/UI type for Organisation Impact Weave Attention used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationImpactWeaveAttention(
    val draftId: String,
    val title: String,
    val status: String,
    val attentionType: String,
    val severity: String,
    val message: String,
    val planningDeadline: String? = null,
    val daysRemaining: Int? = null
)
