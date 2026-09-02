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
    fun hybridApplicationWindowUsesEachRolesOwnStartDate() {
        val physicalRole = role(
            id = 101,
            title = "Physical Helper",
            path = "Path",
            skills = emptyList(),
            minimumLevel = 1
        ).copy(roleMode = "PHYSICAL")
        val remoteRole = role(
            id = 102,
            title = "Remote Helper",
            path = "Path",
            skills = emptyList(),
            minimumLevel = 1
        ).copy(roleMode = "REMOTE")

        val opportunity = event(10, "Path", emptyList()).copy(
            eventOpportunityType = "Hybrid",
            eventVolunteerRoles = listOf(physicalRole, remoteRole),
            eventPhysicalStartDate = "2026-09-10",
            eventRemoteStartDate = "2026-09-15",
            eventApplicationStartDate = "2026-09-10"
        )

        val september12 =
            java.time.Instant.parse("2026-09-12T00:00:00Z").toEpochMilli()

        assertFalse(
            VolunteerApplicationWindow.canApply(
                opportunity,
                physicalRole,
                september12
            )
        )
        assertTrue(
            VolunteerApplicationWindow.canApply(
                opportunity,
                remoteRole,
                september12
            )
        )
        assertTrue(VolunteerApplicationWindow.canApply(opportunity, september12))
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
    fun rolesAboveCurrentSkillLevelAreNotRecommended() {
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
        assertFalse(recommendations.any { it.event.eventId == 7 })
    }

    @Test
    fun hybridRecommendationDoesNotChooseClosedPhysicalRole() {
        val physical = role(101, "Physical", "Path", emptyList(), 1).copy(roleMode = "PHYSICAL")
        val remote = physical.copy(roleId = 102, roleMode = "REMOTE", roleTitle = "Remote")
        val hybrid = event(10, "Path", emptyList()).copy(
            eventOpportunityType = "Hybrid",
            eventVolunteerRoles = listOf(physical, remote),
            eventPhysicalStartDate = "2026-09-10",
            eventRemoteStartDate = "2026-09-15"
        )
        val now = java.time.Instant.parse("2026-09-12T12:00:00Z").toEpochMilli()
        val result = VolunteerHomeRecommendationEngine.recommend(
            listOf(hybrid), emptyList(), nowMillis = now
        )
        assertEquals(102, result.single().bestRoleId)
        assertEquals(2, result.single().event.eventVolunteerRoles.size)
        assertTrue(VolunteerHomeRecommendationEngine.recommend(
            listOf(hybrid.copy(eventVolunteerRoles = listOf(physical, remote.copy(roleVacancies = 0)))),
            emptyList(), nowMillis = now
        ).isEmpty())
    }

    @Test
    fun hybridMissingPhaseDateNeverFallsBackToGeneralDate() {
        val remote = role(102, "Remote", "Path", emptyList(), 1).copy(roleMode = "REMOTE")
        val hybrid = event(10, "Path", emptyList()).copy(
            eventOpportunityType = "Hybrid", eventVolunteerRoles = listOf(remote),
            eventApplicationStartDate = "2099-09-20", eventRemoteStartDate = ""
        )
        val now = java.time.Instant.parse("2026-09-12T12:00:00Z").toEpochMilli()
        assertFalse(VolunteerApplicationWindow.canApply(hybrid, remote, now))
        assertTrue(VolunteerApplicationWindow.reason(hybrid, remote).contains("no valid start date"))
        assertTrue(VolunteerApplicationWindow.reason(
            hybrid.copy(eventRemoteStartDate = "2026-02-30"), remote
        ).contains("no valid start date"))
    }

    @Test
    fun homeHidesActiveParticipationButAllowsCancelledPostAgain() {
        val opportunity = event(1, "Path", emptyList())
        for (status in listOf(VolunteerApplicationStatus.PENDING, VolunteerApplicationStatus.ACCEPTED)) {
            val application = completedApplication(1).copy(applicationStatus = status)
            assertTrue(VolunteerHomeFeedEngine.filter(
                listOf(opportunity), VolunteerHomeFeedFilter.ALL, listOf(application)
            ).isEmpty())
        }
        val cancelled = completedApplication(1).copy(applicationStatus = VolunteerApplicationStatus.CANCELLED)
        assertEquals(1, VolunteerHomeFeedEngine.filter(
            listOf(opportunity), VolunteerHomeFeedFilter.ALL, listOf(cancelled)
        ).size)
    }

    @Test
    fun rejectionBlocksOnlyThatRoleForDiscovery() {
        val opportunity = event(1, "Path", emptyList())
        val rejected = completedApplication(1).copy(
            applicationStatus = VolunteerApplicationStatus.REJECTED, applicationRoleId = 1
        )
        assertTrue(VolunteerHomeRecommendationEngine.recommend(listOf(opportunity), listOf(rejected)).isEmpty())
        val alternative = opportunity.copy(eventVolunteerRoles = opportunity.eventVolunteerRoles +
            opportunity.eventVolunteerRoles.single().copy(roleId = 2))
        assertEquals(2, VolunteerHomeRecommendationEngine.recommend(listOf(alternative), listOf(rejected)).single().bestRoleId)
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
