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
// Purpose: Handles volunteer role schedule item as one reusable step in the Volunteer flow.
// Usage: Read by repositories, ViewModels and Compose screens as shared Volunteer state.
// Result: The caller receives a stable value or action for the next Volunteer-flow step.
data class VolunteerRoleScheduleItem(
    val scheduleDate: String = "",
    val scheduleTime: String,
    val scheduleActivity: String,
    val rawDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val scheduleType: String = "",
    val location: String = "",
    val notes: String = "",
    val assignedToRole: Boolean = false
)

@Serializable
// Purpose: Handles volunteer opportunity role as one reusable step in the Volunteer flow.
// Usage: Read by repositories, ViewModels and Compose screens as shared Volunteer state.
// Result: The caller receives a stable value or action for the next Volunteer-flow step.
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
    val roleResponsibilities: List<String> = emptyList(),
    val roleScheduleItems: List<VolunteerRoleScheduleItem> = emptyList(),
    val roleMinimumSkillPathLevel: Int = 1,
    val roleApplicationFlow: VolunteerRoleApplicationFlow =
        VolunteerRoleApplicationFlow.DIRECT_SUBMISSION,
    val roleApplicationMethod: VolunteerRoleApplicationMethod =
        VolunteerRoleApplicationMethod.REVIEW_APPLICANTS,
    val roleMode: String = "",
    val roleSubmissionRequirement: String = "",
    val roleSubmissionInstruction: String = "",
    // Composite/normalized role identity used by application RPC calls.
    val roleDatabaseId: String = ""
)

@Serializable
// Purpose: Handles volunteer opportunity partnership contribution as one reusable step in the Volunteer flow.
// Usage: Read by repositories, ViewModels and Compose screens as shared Volunteer state.
// Result: The caller receives a stable value or action for the next Volunteer-flow step.
data class VolunteerOpportunityPartnershipContribution(
    val supportType: String = "",
    val needResourceName: String = "",
    val providerResourceName: String? = null,
    val quantityProvided: Int? = null,
    val capacityProvided: Int? = null
)

@Serializable
// Purpose: Handles volunteer opportunity…48 tokens truncated…r action for the next Volunteer-flow step.
data class VolunteerOpportunityPartner(
    val organisationId: String = "",
    val organisationName: String = "",
    val contributions: List<VolunteerOpportunityPartnershipContribution> = emptyList()
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
// Purpose: Handles volunteer opportunity event as one reusable step in the Volunteer flow.
// Usage: Read by repositories, ViewModels and Compose screens as shared Volunteer state.
// Result: The caller receives a stable value or action for the next Volunteer-flow step.
data class VolunteerOpportunityEvent(
    val eventId: Int,
    val eventTitle: String,
    val eventOrganisationName: String,
    val eventOrganisationId: String = "",
    val eventIsVerifiedOrganisation: Boolean,
    val eventOpportunityType: String,
    val eventCategory: VolunteerOpportunityCategory,
    val eventLocation: String,
    val eventDistanceKm: Double?,
    val eventDate: String,
    val eventEndDate: String = "",
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
    val eventThumbnailPath: String? = null,
    val eventIsSaved: Boolean = false,
    val eventDatabaseId: String = "",
    val eventStatus: String = "PUBLISHED",
    // Raw ISO dates are kept for role-specific application rules.
    // Hybrid PHYSICAL roles use eventPhysicalStartDate; REMOTE roles use eventRemoteStartDate.
    val eventPhysicalStartDate: String = "",
    val eventRemoteStartDate: String = "",
    val eventPhysicalEndDate: String = "",
    val eventPhysicalStartTime: String = "",
    val eventPhysicalEndTime: String = "",
    val eventTimeZone: String = "Asia/Kuala_Lumpur",
    val eventRemoteEndDate: String = "",
    val eventRemoteOriginalEndDate: String = "",
    val eventMeetingPoint: String = "",
    val eventIsPartnershipPost: Boolean = false,
    val eventPartnershipPartners: List<VolunteerOpportunityPartner> = emptyList(),
    // Retained for older cached payload compatibility. New application checks are role-specific.
    val eventApplicationStartDate: String = ""
)

@Serializable
enum class VolunteerApplicationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    NOT_COMPLETED,
    CANCELLED
}

@Serializable
// Purpose: Handles volunteer opportunity application as one reusable step in the Volunteer flow.
// Usage: Read by repositories, ViewModels and Compose screens as shared Volunteer state.
// Result: The caller receives a stable value or action for the next Volunteer-flow step.
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
    val applicationCompletionReason: String? = null,
    val applicationScreeningQuestions: List<String> = emptyList(),
    val applicationScreeningAnswers: List<String> = emptyList(),
    val applicationVolunteerName: String = "VolunteerLink Volunteer",
    val applicationEventDate: String? = null,
    val applicationEventTime: String? = null,
    val applicationEventLocation: String? = null,
    val applicationPrimarySkillPath: String? = null,
    val applicationPractisedSkills: List<String> = emptyList(),
    val applicationRoleMode: String = "",
    val applicationDatabaseId: String = "",
    val applicationCreatedAtRaw: String = ""
)

@Serializable
// Purpose: Handles volunteer event as one reusable step in the Volunteer flow.
// Usage: Read by repositories, ViewModels and Compose screens as shared Volunteer state.
// Result: The caller receives a stable value or action for the next Volunteer-flow step.
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
