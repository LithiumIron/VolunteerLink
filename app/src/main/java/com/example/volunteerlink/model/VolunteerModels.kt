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

// Volunteer Event Details for Map purposes
data class VolunteerEvent(
    val id: String,
    val title: String,
    val organisation: String,
    val distanceKm: Double,
    val date: String,
    val spotsLeft: Int,
    val mapX: Float,   // 0f..1f relative position on the map canvas
    val mapY: Float,
    val description: String = "Join us and make a difference in your community. Full details and meeting point will be shared in the event chat room."
)