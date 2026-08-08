package com.example.volunteerlink.model

// Volunteer role data model
data class VolunteerOpportunityRole(
    val roleId: Int,
    val roleTitle: String,
    val roleLevel: String,
    val roleVacancies: Int,
    val rolePrimarySkillPath: String,
    val roleSkillsPractised: List<String>,
    val roleExperienceRequirement: String,
    val roleExtraApplicationQuestions: List<String> = emptyList()
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

// Volunteer event data model
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
    val eventVolunteerRoles: List<VolunteerOpportunityRole>
)

// Volunteer application status
enum class VolunteerApplicationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED
}

// Volunteer application record
data class VolunteerOpportunityApplication(
    val applicationId: Int,
    val applicationEventId: Int,
    val applicationEventTitle: String,
    val applicationOrganisationName: String,
    val applicationRoleTitle: String,
    val applicationSubmittedDate: String,
    val applicationStatus: VolunteerApplicationStatus
)