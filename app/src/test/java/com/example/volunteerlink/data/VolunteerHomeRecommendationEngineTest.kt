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
    fun applicationWindowClosesAtStartDateIncludingRemote() {
        val opportunity = event(1, "Communication", emptyList())
            .copy(eventApplicationStartDate = "2026-09-12")
        val before = java.time.Instant.parse("2026-09-11T23:59:59Z").toEpochMilli()
        val start = java.time.Instant.parse("2026-09-12T00:00:00Z").toEpochMilli()
        assertTrue(VolunteerApplicationWindow.canApply(opportunity, before))
        assertFalse(VolunteerApplicationWindow.canApply(opportunity, start))
        assertFalse(VolunteerApplicationWindow.canApply(
            opportunity.copy(eventOpportunityType = "Remote"), start))
        assertFalse(VolunteerApplicationWindow.canApply(
            opportunity.copy(eventStatus = "COMPLETED"), before))
    }

    @Test
    fun missingOrMalformedCachedDatesFailClosed() {
        val opportunity = event(1, "Communication", emptyList())
        val now = java.time.Instant.parse("2026-09-12T00:00:00Z").toEpochMilli()
        assertFalse(VolunteerApplicationWindow.canApply(opportunity.copy(eventApplicationStartDate = ""), now))
        assertFalse(VolunteerApplicationWindow.canApply(opportunity.copy(eventApplicationStartDate = "invalid"), now))
    }

    @Test
    fun expiredPublishedEventsAreNotRecommended() {
        val expired = event(1, "Communication", emptyList())
            .copy(eventApplicationStartDate = "2000-01-01")
        assertTrue(VolunteerHomeRecommendationEngine.recommend(listOf(expired), emptyList()).isEmpty())
    }

    @Test
    fun fullRolesAreNotRecommended() {
        val available = event(1, "Communication", emptyList())
        val full = available.copy(eventVolunteerRoles = available.eventVolunteerRoles.map { it.copy(roleVacancies = 0) })
        assertTrue(VolunteerHomeRecommendationEngine.recommend(listOf(full), emptyList()).isEmpty())
    }

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
            recommendations.any { recommendation ->
                recommendation.event.eventId == 1
            }
        )
        assertTrue(
            recommendations.any { recommendation ->
                recommendation.event.eventId == 2
            }
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
                    listOf(
                        unrelatedNearbyEvent,
                        matchingEvent
                    ),
                volunteerApplications =
                    listOf(
                        completedApplication(eventId = 99)
                    )
            )

        assertEquals(
            2,
            recommendations.first().event.eventId
        )
        assertTrue(
            recommendations.first().reason.contains(
                "Communication & Guest Services"
            )
        )
    }

    @Test
    fun allFilterReturnsEveryPublishedEventExactlyOnce() {
        val first = event(
            id = 1,
            path = "Path A",
            skills = listOf("Skill A")
        )
        val second = event(
            id = 2,
            path = "Path B",
            skills = listOf("Skill B")
        )
        val draft = event(
            id = 3,
            path = "Path C",
            skills = listOf("Skill C"),
            status = "DRAFT"
        )

        val filtered =
            VolunteerHomeFeedEngine.filter(
                events = listOf(
                    first,
                    second,
                    first,
                    draft
                ),
                filter = VolunteerHomeFeedFilter.ALL
            )

        assertEquals(listOf(1, 2), filtered.map {
            opportunity -> opportunity.eventId
        })
    }

    @Test
    fun matchScoreAlwaysStaysWithinPercentageRange() {
        val recommendations =
            VolunteerHomeRecommendationEngine.recommend(
                volunteerOpportunityEvents =
                    listOf(
                        event(
                            id = 5,
                            path =
                                "Communication & Guest Services",
                            skills = listOf(
                                "Active Listening",
                                "Customer Service"
                            )
                        )
                    ),
                volunteerApplications =
                    listOf(
                        completedApplication(eventId = 99)
                    ),
                currentSkillPathLevels =
                    mapOf(
                        "Communication & Guest Services" to 3
                    )
            )

        assertTrue(
            recommendations.all { recommendation ->
                recommendation.score in 0..100
            }
        )
        assertEquals(
            100,
            recommendations.first().factors.sumOf {
                factor -> factor.maximumPoints
            }
        )
    }

    @Test
    fun eligibleRoleRanksAboveOtherwiseEqualIneligibleRole() {
        val eligibleEvent = event(
            id = 6,
            path = "Communication & Guest Services",
            skills = listOf("Active Listening"),
            minimumLevel = 1
        )
        val ineligibleEvent = event(
            id = 7,
            path = "Communication & Guest Services",
            skills = listOf("Active Listening"),
            minimumLevel = 3
        )

        val recommendations =
            VolunteerHomeRecommendationEngine.recommend(
                volunteerOpportunityEvents =
                    listOf(ineligibleEvent, eligibleEvent),
                volunteerApplications =
                    listOf(
                        completedApplication(eventId = 99)
                    ),
                currentSkillPathLevels =
                    mapOf(
                        "Communication & Guest Services" to 1
                    )
            )

        assertEquals(
            6,
            recommendations.first().event.eventId
        )
        assertTrue(
            recommendations
                .first { recommendation ->
                    recommendation.event.eventId == 7
                }
                .factors
                .any { factor ->
                    factor.title == "Level eligibility" &&
                        factor.status ==
                            VolunteerMatchFactorStatus.ATTENTION
                }
        )
    }

    @Test
    fun eventUsesOneBestRoleInsteadOfCombiningEveryRole() {
        val event = event(
            id = 8,
            path = "Unrelated Path",
            skills = listOf("Unrelated Skill")
        ).copy(
            eventVolunteerRoles = listOf(
                role(
                    id = 81,
                    title = "Advanced Photographer",
                    path = "Creative Media",
                    skills = listOf("Photography"),
                    minimumLevel = 3
                ),
                role(
                    id = 82,
                    title = "Guest Guide",
                    path =
                        "Communication & Guest Services",
                    skills = listOf("Active Listening"),
                    minimumLevel = 1
                )
            )
        )

        val recommendation =
            VolunteerHomeRecommendationEngine.recommend(
                volunteerOpportunityEvents = listOf(event),
                volunteerApplications =
                    listOf(
                        completedApplication(eventId = 99)
                    ),
                currentSkillPathLevels =
                    mapOf(
                        "Communication & Guest Services" to 1
                    )
            ).single()

        assertEquals(82, recommendation.bestRoleId)
        assertEquals(
            "Guest Guide",
            recommendation.bestRoleTitle
        )
    }

    private fun completedApplication(
        eventId: Int
    ): VolunteerOpportunityApplication =
        VolunteerOpportunityApplication(
            applicationId = eventId,
            applicationEventId = eventId,
            applicationEventTitle = "Completed Event",
            applicationOrganisationName =
                "Verified Organisation",
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
        distanceKm: Double = 3.0,
        minimumLevel: Int = 1,
        status: String = "PUBLISHED"
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
                    role(
                        id = id,
                        title = "Volunteer Role $id",
                        path = path,
                        skills = skills,
                        minimumLevel = minimumLevel
                    )
                ),
            eventDatabaseId = "POST$id",
            eventStatus = status,
            eventApplicationStartDate = "2099-09-20"
        )

    private fun role(
        id: Int,
        title: String,
        path: String,
        skills: List<String>,
        minimumLevel: Int
    ): VolunteerOpportunityRole =
        VolunteerOpportunityRole(
            roleId = id,
            roleTemplateId = "ROLE$id",
            roleTitle = title,
            roleLevel =
                when (minimumLevel) {
                    1 -> "Beginner"
                    2 -> "Intermediate"
                    else -> "Advanced"
                },
            roleVacancies = 5,
            rolePrimarySkillPath = path,
            roleSkillsPractised = skills,
            roleExperienceRequirement =
                "Role-specific experience requirements apply.",
            roleMinimumSkillPathLevel = minimumLevel
        )
}
