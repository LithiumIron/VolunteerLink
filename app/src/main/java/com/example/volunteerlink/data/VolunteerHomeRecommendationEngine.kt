package com.example.volunteerlink.data

import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityEvent

data class VolunteerHomeRecommendation(
    val event: VolunteerOpportunityEvent,
    val score: Int,
    val reason: String
)

/**
 * Produces explainable Home recommendations from the volunteer's verified
 * history. Keeping this out of Compose makes the ranking deterministic and
 * independently testable.
 */
object VolunteerHomeRecommendationEngine {

    fun recommend(
        volunteerOpportunityEvents: List<VolunteerOpportunityEvent>,
        volunteerApplications: List<VolunteerOpportunityApplication>
    ): List<VolunteerHomeRecommendation> {
        val completedApplications =
            volunteerApplications.filter { application ->
                application.applicationStatus ==
                    VolunteerApplicationStatus.COMPLETED
            }
        val experiencedPaths =
            completedApplications
                .mapNotNull { it.applicationPrimarySkillPath }
                .toSet()
        val practisedSkills =
            completedApplications
                .flatMap { it.applicationPractisedSkills }
                .toSet()
        val appliedEventIds =
            volunteerApplications
                .map { it.applicationEventId }
                .toSet()

        return volunteerOpportunityEvents
            .filterNot { event ->
                event.eventId in appliedEventIds
            }
            .map { event ->
                createRecommendation(
                    event = event,
                    experiencedPaths = experiencedPaths,
                    practisedSkills = practisedSkills
                )
            }
            .sortedWith(
                compareByDescending<VolunteerHomeRecommendation> {
                    it.score
                }.thenBy { recommendation ->
                    recommendation.event.eventDistanceKm
                        ?: Double.MAX_VALUE
                }
            )
    }

    private fun createRecommendation(
        event: VolunteerOpportunityEvent,
        experiencedPaths: Set<String>,
        practisedSkills: Set<String>
    ): VolunteerHomeRecommendation {
        val matchingPaths =
            event.eventVolunteerRoles
                .map { it.rolePrimarySkillPath }
                .filter { it in experiencedPaths }
                .distinct()
        val matchingSkills =
            event.eventVolunteerRoles
                .flatMap { it.roleSkillsPractised }
                .filter { it in practisedSkills }
                .distinct()
        val beginnerFriendly =
            event.eventVolunteerRoles.any { role ->
                role.roleMinimumSkillPathLevel <= 1
            }
        val isNearby =
            event.eventDistanceKm?.let { it <= 10.0 } == true

        val score =
            matchingPaths.size * 45 +
                matchingSkills.size.coerceAtMost(3) * 9 +
                (if (event.eventIsVerifiedOrganisation) 8 else 0) +
                (if (isNearby) 7 else 0) +
                (if (beginnerFriendly) 5 else 0) +
                event.eventApplicationCount.coerceAtMost(10)

        val reason = when {
            matchingPaths.isNotEmpty() &&
                matchingSkills.isNotEmpty() ->
                "Builds ${matchingPaths.first()} and " +
                    matchingSkills.take(2).joinToString(" + ")

            matchingPaths.isNotEmpty() ->
                "Continues your ${matchingPaths.first()} growth"

            matchingSkills.isNotEmpty() ->
                "Uses ${matchingSkills.take(2).joinToString(" + ")}"

            isNearby && event.eventIsVerifiedOrganisation ->
                "Nearby opportunity from a verified organisation"

            beginnerFriendly ->
                "Beginner-friendly way to start a new Skill Path"

            event.eventOpportunityType == "Remote" ->
                "Flexible remote role with verified skill evidence"

            else ->
                "Open role with available volunteer places"
        }

        return VolunteerHomeRecommendation(
            event = event,
            score = score,
            reason = reason
        )
    }
}
