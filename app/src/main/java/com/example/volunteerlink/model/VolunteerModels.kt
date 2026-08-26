package com.example.volunteerlink.model

import kotlinx.serialization.Serializable

@Serializable
enum class VolunteerRoleApplicationFlow {
    DIRECT_SUBMISSION,
    ADDITIONAL_FORM
}

// Mirrors post_roles.application_method created by Organisation Create.
@Serializable
enum class VolunteerRoleApplicationMethod {
    INSTANT_JOIN,
    REVIEW_APPLICANTS
}

@Serializable
data class VolunteerRoleScheduleItem(
    val scheduleTime: String,
    val scheduleActivity: String
)

@Serializable
data class VolunteerOpportunityRole(
    val roleId: Int,
    // Stable ROLE... catalogue ID written by Organisation Create.
    val roleTemplateId: String = "",
    val roleTitle: String,
    val roleLevel: String,
    val roleVacancies: Int,
    val rolePrimarySkillPath: String,
    val roleSkillsPractised: List<String>,
    val roleExperienceRequirement: String,
    val roleExtraApplicationQuestions: List<String> = emptyList(),
    val roleSpecificAssignment: String = "",
    val roleTrainingDetails: String? = null,
    val roleResponsibilities: List<String> = emptyList(),
    val roleScheduleItems: List<VolunteerRoleScheduleItem> = emptyList(),
    val roleMinimumSkillPathLevel: Int = 1,
    val roleApplicationFlow: VolunteerRoleApplicationFlow =
        VolunteerRoleApplicationFlow.DIRECT_SUBMISSION,
    val roleApplicationMethod: VolunteerRoleApplicationMethod =
        VolunteerRoleApplicationMethod.REVIEW_APPLICANTS,
    // Composite/normalized role identity used by application RPC calls.
    val roleDatabaseId: String = ""
)

@Serializable
enum class VolunteerOpportunityCategory {
    SPORTS,
    COMMUNITY,
    EDUCATION,
    ENVIRONMENT,
    HEALTH,
    ANIMALS,
    ARTS
}

@Serializable
data class VolunteerOpportunityEvent(
    val eventId: Int,
    val eventTitle: String,
    val eventOrganisationName: String,
    val eventIsVerifiedOrganisation: Boolean,
    val eventOpportunityType: String,
    val eventCategory: VolunteerOpportunityCategory,
    val eventLocation: String,
    val eventDistanceKm: Double?,
    val eventDate: String,
    val eventTime: String,
    val eventAvailableSpots: Int,
    val eventApplicationCount: Int,
    val eventDescription: String,
    val eventIsLongTerm: Boolean = false,
    val eventVolunteerRoles: List<VolunteerOpportunityRole>,
    val eventIsGovernmentApproved: Boolean = false,
    val eventFullAddress: String = "",
    val eventCauseName: String = "",
    val eventContactEmail: String = "",
    val eventContactPhone: String = "",
    val eventShareLink: String = "",
    // Coordinates written by Organisation Create's Geoapify selection.
    val eventLatitude: Double? = null,
    val eventLongitude: Double? = null,
    val eventDatabaseId: String = "",
    val eventStatus: String = "PUBLISHED"
)

@Serializable
enum class VolunteerApplicationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED
}

@Serializable
data class VolunteerOpportunityApplication(
    val applicationId: Int,
    val applicationEventId: Int,
    val applicationEventTitle: String,
    val applicationOrganisationName: String,
    val applicationRoleTitle: String,
    val applicationSubmittedDate: String,
    val applicationStatus: VolunteerApplicationStatus,
    val applicationRoleId: Int? = null,
    val applicationStatusMessage: String = "",
    val applicationRejectionReason: String? = null,
    val applicationVerifiedHours: Int? = null,
    val applicationVerifiedMinutes: Int? = null,
    val applicationCertificateId: String? = null,
    val applicationCompletedDate: String? = null,
    val applicationOrganisationFeedback: String? = null,
    val applicationVolunteerName: String = "VolunteerLink Volunteer",
    val applicationEventDate: String? = null,
    val applicationEventTime: String? = null,
    val applicationEventLocation: String? = null,
    val applicationPrimarySkillPath: String? = null,
    val applicationPractisedSkills: List<String> = emptyList(),
    val applicationDatabaseId: String = ""
)

@Serializable
data class VolunteerEvent(
    val id: String,
    val title: String,
    val organisation: String,
    val distanceKm: Double,
    val date: String,
    val spotsLeft: Int,
    val mapX: Float,
    val mapY: Float,
    val description: String =
        "Join us and make a difference in your community. " +
            "Full details and meeting point will be shared " +
            "in the event chat room."
)
