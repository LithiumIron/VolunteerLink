package com.example.volunteerlink.data

import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import com.example.volunteerlink.model.VolunteerOpportunityCategory
import com.example.volunteerlink.model.VolunteerOpportunityCategory

object VolunteerOpportunitySampleData {

    val volunteerOpportunityEvents = listOf(

        VolunteerOpportunityEvent(
            eventId = 1,
            eventTitle = "Charity Fun Run 2026",
            eventOrganisationName = "Green Earth Society",
            eventIsVerifiedOrganisation = true,
            eventCategory =
                VolunteerOpportunityCategory.SPORTS,
            eventOpportunityType = "Physical",
            eventCategory = VolunteerOpportunityCategory.SPORTS,
            eventLocation = "Kuala Lumpur",
            eventDistanceKm = 2.3,
            eventDate = "15 Aug 2026",
            eventTime = "7:00 AM - 12:00 PM",
            eventAvailableSpots = 18,
            eventApplicationCount = 86,
            eventDescription =
                "Support participants and organisers during a community charity run.",
            eventIsLongTerm = false,
            eventVolunteerRoles = listOf(

                VolunteerOpportunityRole(
                    roleId = 101,
                    roleTitle = "Communication Assistant",
                    roleLevel = "Beginner",
                    roleVacancies = 5,
                    rolePrimarySkillPath =
                        "Communication & Guest Services",
                    roleSkillsPractised = listOf(
                        "Active Listening",
                        "Customer Service",
                        "Attendee Guidance"
                    ),
                    roleExperienceRequirement =
                        "No experience required"
                ),

                VolunteerOpportunityRole(
                    roleId = 102,
                    roleTitle = "Logistics Assistant",
                    roleLevel = "Intermediate",
                    roleVacancies = 6,
                    rolePrimarySkillPath =
                        "Operations, Logistics & Safety",
                    roleSkillsPractised = listOf(
                        "Logistics Coordination",
                        "Inventory Handling",
                        "Team Collaboration"
                    ),
                    roleExperienceRequirement =
                        "Previous volunteering experience recommended",
                    roleExtraApplicationQuestions = listOf(
                        "Describe any relevant volunteering experience."
                    )
                )
            )
        ),

        VolunteerOpportunityEvent(
            eventId = 2,
            eventTitle = "Food Bank Distribution",
            eventOrganisationName = "Community Food Support",
            eventIsVerifiedOrganisation = true,
            eventCategory =
                VolunteerOpportunityCategory.COMMUNITY,
            eventOpportunityType = "Physical",
            eventCategory = VolunteerOpportunityCategory.COMMUNITY,
            eventLocation = "Butterworth",
            eventDistanceKm = 5.1,
            eventDate = "22 Aug 2026",
            eventTime = "9:00 AM - 2:00 PM",
            eventAvailableSpots = 12,
            eventApplicationCount = 124,
            eventDescription =
                "Help prepare and distribute essential food supplies to the community.",
            eventIsLongTerm = false,
            eventVolunteerRoles = listOf(

                VolunteerOpportunityRole(
                    roleId = 201,
                    roleTitle = "Distribution Assistant",
                    roleLevel = "Beginner",
                    roleVacancies = 12,
                    rolePrimarySkillPath =
                        "Operations, Logistics & Safety",
                    roleSkillsPractised = listOf(
                        "Supply Distribution",
                        "Inventory Handling",
                        "Team Collaboration"
                    ),
                    roleExperienceRequirement =
                        "No experience required"
                )
            )
        ),

        VolunteerOpportunityEvent(
            eventId = 3,
            eventTitle = "Senior Digital Literacy Project",
            eventOrganisationName = "DigitalCare Foundation",
            eventIsVerifiedOrganisation = true,
            eventCategory =
                VolunteerOpportunityCategory.EDUCATION,
            eventOpportunityType = "Remote",
            eventCategory = VolunteerOpportunityCategory.EDUCATION,
            eventLocation = "Online",
            eventDistanceKm = null,
            eventDate = "25 Aug 2026",
            eventTime = "Flexible",
            eventAvailableSpots = 8,
            eventApplicationCount = 42,
            eventDescription =
                "Support the preparation of digital learning materials for senior citizens.",
            eventIsLongTerm = true,
            eventVolunteerRoles = listOf(

                VolunteerOpportunityRole(
                    roleId = 301,
                    roleTitle = "Online Content Assistant",
                    roleLevel = "Beginner",
                    roleVacancies = 8,
                    rolePrimarySkillPath =
                        "Writing, Content & Digital Campaigns",
                    roleSkillsPractised = listOf(
                        "Content Writing",
                        "Editing & Proofreading",
                        "Digital Communication"
                    ),
                    roleExperienceRequirement =
                        "Basic writing skills"
                )
            )
        )
    )

    val volunteerApplications = listOf(

        VolunteerOpportunityApplication(
            applicationId = 1,
            applicationEventId = 1,
            applicationEventTitle = "Charity Fun Run 2026",
            applicationOrganisationName = "Green Earth Society",
            applicationRoleTitle = "Communication Assistant",
            applicationSubmittedDate = "2 days ago",
            applicationStatus =
                VolunteerApplicationStatus.PENDING
        ),

        VolunteerOpportunityApplication(
            applicationId = 2,
            applicationEventId = 2,
            applicationEventTitle = "Food Bank Distribution",
            applicationOrganisationName = "Community Food Support",
            applicationRoleTitle = "Distribution Assistant",
            applicationSubmittedDate = "1 week ago",
            applicationStatus =
                VolunteerApplicationStatus.ACCEPTED
        )
    )
}