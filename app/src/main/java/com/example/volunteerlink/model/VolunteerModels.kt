package com.example.volunteerlink.model

// Determines whether joining a role needs an additional form.
enum class VolunteerRoleApplicationFlow {
    DIRECT_SUBMISSION,
    ADDITIONAL_FORM
}

// Mirrors post_roles.application_method in Supabase.
enum class VolunteerRoleApplicationMethod {
    INSTANT_JOIN,
    REVIEW_APPLICANTS
}

// One activity shown in the role schedule.
data class VolunteerRoleScheduleItem(
    val scheduleTime: String,
    val scheduleActivity: String
)

// Volunteer role data model.
data class VolunteerOpportunityRole(
    val roleId: Int,
    val roleTemplateId: String,
    val roleTitle: String,
    val roleLevel: String,
    val roleVacancies: Int,
    val rolePrimarySkillPath: String,
    val roleSkillsPractised: List<String>,
    val roleExperienceRequirement: String,
    val roleExtraApplicationQuestions: List<String> = emptyList(),

    // Detailed role information.
    val roleSpecificAssignment: String = "",
    val roleTrainingDetails: String? = null,
    val roleResponsibilities: List<String> = emptyList(),
    val roleScheduleItems:
    List<VolunteerRoleScheduleItem> = emptyList(),

    // Level 1 = Beginner, 2 = Intermediate, 3 = Advanced.
    val roleMinimumSkillPathLevel: Int = 1,

    // Both flows use the same Join Event action.
    val roleApplicationFlow:
    VolunteerRoleApplicationFlow =
        VolunteerRoleApplicationFlow.DIRECT_SUBMISSION,

    val roleApplicationMethod:
    VolunteerRoleApplicationMethod =
        VolunteerRoleApplicationMethod.REVIEW_APPLICANTS,

    // Stable Supabase primary key, for secure RPC calls.
    val roleDatabaseId: String = ""
)

enum class VolunteerOpportunityCategory {
    SPORTS,
    COMMUNITY,
    EDUCATION,
    ENVIRONMENT,
    HEALTH,
    ANIMALS,
    ARTS
}

// Volunteer event data model.
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

    // Extended event details.
    val eventIsGovernmentApproved: Boolean = false,
    val eventFullAddress: String = "",
    val eventCauseName: String = "",
    val eventContactEmail: String = "",
    val eventContactPhone: String = "",
    val eventShareLink: String = "",

    // Stable Supabase primary key. eventId remains the navigation-safe Int.
    val eventDatabaseId: String = "",
    val eventStatus: String = "PUBLISHED"
)

// Volunteer application status.
enum class VolunteerApplicationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED
}

// Volunteer application record.
data class VolunteerOpportunityApplication(
    val applicationId: Int,
    val applicationEventId: Int,
    val applicationEventTitle: String,
    val applicationOrganisationName: String,
    val applicationRoleTitle: String,
    val applicationSubmittedDate: String,
    val applicationStatus: VolunteerApplicationStatus,

    // Additional information for application details.
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

    // Stable Supabase primary key, used when cancelling an application.
    val applicationDatabaseId: String = ""
)

// Temporary model used by the Map teammate's prototype.
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
