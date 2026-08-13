package com.example.volunteerlink.data

import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityCategory
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolunteerHomeRecommendationEngineTest {

    @Test
    fun appliedEventIsNotRecommendedAgain() {
        val appliedEvent = event(
            id = 1,
            path = "Communication & Guest Services",
            skills = listOf("Active Listening")
        )
        val newEvent = event(
            id = 2,
            path = "Communication & Guest Services",
            skills = listOf("Active Listening")
        )

        val recommendations =
            VolunteerHomeRecommendationEngine.recommend(
                volunteerOpportunityEvents =
                    listOf(appliedEvent, newEvent),
                volunteerApplications =
                    listOf(
                        completedApplication(eventId = 1)
                    )
            )

        assertFalse(
            recommendations.any { it.event.eventId == 1 }
        )
        assertTrue(
            recommendations.any { it.event.eventId == 2 }
        )
    }

    @Test
    fun verifiedSkillPathMatchRanksAboveUnrelatedNearbyEvent() {
        val matchingEvent = event(
            id = 2,
            path = "Communication & Guest Services",
            skills = listOf("Active Listening"),
            distanceKm = 8.0
        )
        val unrelatedNearbyEvent = event(
            id = 3,
            path = "Creative Media & Event Promotion",
            skills = listOf("Photography"),
            distanceKm = 1.0
        )

        val recommendations =
            VolunteerHomeRecommendationEngine.recommend(
                volunteerOpportunityEvents =
                    listOf(unrelatedNearbyEvent, matchingEvent),
                volunteerApplications =
                    listOf(completedApplication(eventId = 99))
            )

        assertEquals(2, recommendations.first().event.eventId)
        assertTrue(
            recommendations.first().reason.contains(
                "Communication & Guest Services"
            )
        )
    }

    private fun completedApplication(
        eventId: Int
    ): VolunteerOpportunityApplication =
        VolunteerOpportunityApplication(
            applicationId = eventId,
            applicationEventId = eventId,
            applicationEventTitle = "Completed Event",
            applicationOrganisationName = "Verified Organisation",
            applicationRoleTitle = "Greeter",
            applicationSubmittedDate = "1 Aug 2026",
            applicationStatus =
                VolunteerApplicationStatus.COMPLETED,
            applicationPrimarySkillPath =
                "Communication & Guest Services",
            applicationPractisedSkills =
                listOf("Active Listening")
        )

    private fun event(
        id: Int,
        path: String,
        skills: List<String>,
        distanceKm: Double = 3.0
    ): VolunteerOpportunityEvent =
        VolunteerOpportunityEvent(
            eventId = id,
            eventTitle = "Event $id",
            eventOrganisationName = "Organisation $id",
            eventIsVerifiedOrganisation = true,
            eventOpportunityType = "Physical",
            eventCategory =
                VolunteerOpportunityCategory.COMMUNITY,
            eventLocation = "Butterworth",
            eventDistanceKm = distanceKm,
            eventDate = "20 Aug 2026",
            eventTime = "9:00 AM - 1:00 PM",
            eventAvailableSpots = 5,
            eventApplicationCount = 2,
            eventDescription = "Volunteer opportunity",
            eventVolunteerRoles =
                listOf(
                    VolunteerOpportunityRole(
                        roleId = id,
                        roleTemplateId = "ROLE$id",
                        roleTitle = "Volunteer Role $id",
                        roleLevel = "Beginner",
                        roleVacancies = 5,
                        rolePrimarySkillPath = path,
                        roleSkillsPractised = skills,
                        roleExperienceRequirement =
                            "No experience required"
                    )
                )
        )
}
