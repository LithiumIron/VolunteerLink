package com.example.volunteerlink.data

import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import kotlin.math.roundToInt

enum class VolunteerHomeFeedFilter(
    val displayName: String
) {
    FOR_YOU("For You"),
    ALL("All"),
    PHYSICAL("Physical"),
    NEAR_ME("Near Me"),
    REMOTE("Remote"),
    LONG_TERM("Long Term")
}

enum class VolunteerMatchFactorStatus {
    STRENGTH,
    OPPORTUNITY,
    ATTENTION
}

data class VolunteerMatchFactor(
    val title: String,
    val explanation: String,
    val earnedPoints: Int,
    val maximumPoints: Int,
    val status: VolunteerMatchFactorStatus
)

data class VolunteerHomeRecommendation(
    val event: VolunteerOpportunityEvent,
    val score: Int,
    val matchLabel: String,
    val bestRoleId: Int,
    val bestRoleTitle: String,
    val reason: String,
    val factors: List<VolunteerMatchFactor>
)

/**
 * Type-safe feed filtering used by Home. FOR_YOU is intentionally handled by
 * the recommendation engine; every other option returns each matching event
 * once and keeps the repository order.
 */
object VolunteerHomeFeedEngine {

    fun filter(
        events: List<VolunteerOpportunityEvent>,
        filter: VolunteerHomeFeedFilter
    ): List<VolunteerOpportunityEvent> =
        events
            .asSequence()
            .filter { event ->
                event.eventStatus.equals(
                    other = "PUBLISHED",
                    ignoreCase = true
                )
            }
            .distinctBy { event ->
                event.eventDatabaseId.ifBlank {
                    event.eventId.toString()
                }
            }
            .filter { event ->
                when (filter) {
                    VolunteerHomeFeedFilter.FOR_YOU,
                    VolunteerHomeFeedFilter.ALL -> true

                    VolunteerHomeFeedFilter.PHYSICAL ->
                        event.eventOpportunityType.equals(
                            other = "Physical",
                            ignoreCase = true
                        )

                    VolunteerHomeFeedFilter.NEAR_ME ->
                        event.eventDistanceKm?.let { distanceKm ->
                            distanceKm <= 10.0
                        } == true

                    VolunteerHomeFeedFilter.REMOTE ->
                        event.eventOpportunityType.equals(
                            other = "Remote",
                            ignoreCase = true
                        )

                    VolunteerHomeFeedFilter.LONG_TERM ->
                        event.eventIsLongTerm
                }
            }
            .toList()
}

/**
 * Explainable, deterministic opportunity matching.
 *
 * Every role is scored independently. The event receives the score of its
 * strongest role, preventing unrelated roles from being combined into a
 * misleading match. The six factors total 100 points:
 *
 * Skill Path 30, verified skills 25, eligibility 20, growth 10,
 * practical access 10 and organisation trust 5.
 */
object VolunteerHomeRecommendationEngine {

    fun recommend(
        volunteerOpportunityEvents: List<VolunteerOpportunityEvent>,
        volunteerApplications: List<VolunteerOpportunityApplication>,
        currentSkillPathLevels: Map<String, Int> = emptyMap()
    ): List<VolunteerHomeRecommendation> {
        val completedApplications =
            volunteerApplications.filter { application ->
                application.applicationStatus ==
                    VolunteerApplicationStatus.COMPLETED
            }

        val experiencedPaths =
            buildSet {
                completedApplications
                    .mapNotNull { application ->
                        application.applicationPrimarySkillPath
                    }
                    .filter { pathName ->
                        pathName.isNotBlank()
                    }
                    .forEach { pathName ->
                        add(pathName)
                    }

                currentSkillPathLevels
                    .filterValues { currentLevel ->
                        currentLevel > 1
                    }
                    .keys
                    .forEach { pathName ->
                        add(pathName)
                    }
            }

        val verifiedSkills =
            completedApplications
                .flatMap { application ->
                    application.applicationPractisedSkills
                }
                .filter { skillName ->
                    skillName.isNotBlank()
                }
                .toSet()

        val appliedEventIds =
            volunteerApplications
                .map { application ->
                    application.applicationEventId
                }
                .toSet()

        return VolunteerHomeFeedEngine
            .filter(
                events = volunteerOpportunityEvents,
                filter = VolunteerHomeFeedFilter.ALL
            )
            .filterNot { event ->
                event.eventId in appliedEventIds
            }
            .filter { event ->
                event.eventAvailableSpots > 0 &&
                    event.eventVolunteerRoles.isNotEmpty()
            }
            .map { event ->
                createRecommendation(
                    event = event,
                    experiencedPaths = experiencedPaths,
                    verifiedSkills = verifiedSkills,
                    currentSkillPathLevels =
                        currentSkillPathLevels
                )
            }
            .sortedWith(
                compareByDescending<VolunteerHomeRecommendation> {
                    recommendation ->
                    recommendation.score
                }.thenByDescending { recommendation ->
                    recommendation.event.eventAvailableSpots
                }.thenBy { recommendation ->
                    recommendation.event.eventDistanceKm
                        ?: Double.MAX_VALUE
                }
            )
    }

    private fun createRecommendation(
        event: VolunteerOpportunityEvent,
        experiencedPaths: Set<String>,
        verifiedSkills: Set<String>,
        currentSkillPathLevels: Map<String, Int>
    ): VolunteerHomeRecommendation {
        val bestRoleMatch =
            event.eventVolunteerRoles
                .map { role ->
                    scoreRole(
                        event = event,
                        role = role,
                        experiencedPaths = experiencedPaths,
                        verifiedSkills = verifiedSkills,
                        currentSkillPathLevels =
                            currentSkillPathLevels
                    )
                }
                .maxWithOrNull(
                    compareBy<RoleMatch> { roleMatch ->
                        roleMatch.score
                    }.thenBy { roleMatch ->
                        roleMatch.role.roleVacancies
                    }
                ) ?: error(
                    "A recommendation requires at least one role."
                )

        val strongestFactor =
            bestRoleMatch.factors
                .filter { factor ->
                    factor.status ==
                        VolunteerMatchFactorStatus.STRENGTH
                }
                .maxByOrNull { factor ->
                    factor.earnedPoints
                }
                ?: bestRoleMatch.factors.maxBy { factor ->
                    factor.earnedPoints
                }

        return VolunteerHomeRecommendation(
            event = event,
            score = bestRoleMatch.score,
            matchLabel = matchLabel(bestRoleMatch.score),
            bestRoleId = bestRoleMatch.role.roleId,
            bestRoleTitle = bestRoleMatch.role.roleTitle,
            reason = strongestFactor.explanation,
            factors = bestRoleMatch.factors
        )
    }

    private fun scoreRole(
        event: VolunteerOpportunityEvent,
        role: VolunteerOpportunityRole,
        experiencedPaths: Set<String>,
        verifiedSkills: Set<String>,
        currentSkillPathLevels: Map<String, Int>
    ): RoleMatch {
        val volunteerLevel =
            currentSkillPathLevels[
                role.rolePrimarySkillPath
            ] ?: 1

        val pathIsExperienced =
            role.rolePrimarySkillPath in experiencedPaths

        val pathPoints = when {
            pathIsExperienced -> 30
            role.roleMinimumSkillPathLevel <= 1 -> 18
            else -> 8
        }

        val roleSkills =
            role.roleSkillsPractised
                .filter { skillName ->
                    skillName.isNotBlank()
                }
                .distinct()

        val matchingSkills =
            roleSkills.filter { skillName ->
                skillName in verifiedSkills
            }

        val skillPoints =
            if (roleSkills.isEmpty()) {
                0
            } else {
                (
                    matchingSkills.size.toDouble() /
                        roleSkills.size.toDouble() * 25.0
                    ).roundToInt()
            }

        val volunteerIsEligible =
            volunteerLevel >=
                role.roleMinimumSkillPathLevel

        val eligibilityPoints =
            if (volunteerIsEligible) 20 else 0

        val newSkills =
            roleSkills.filterNot { skillName ->
                skillName in verifiedSkills
            }

        val growthPoints = when {
            newSkills.size >= 2 -> 10
            newSkills.size == 1 -> 6
            else -> 2
        }

        val accessPoints = when {
            event.eventOpportunityType.equals(
                other = "Remote",
                ignoreCase = true
            ) -> 10

            event.eventDistanceKm == null -> 5
            event.eventDistanceKm <= 5.0 -> 10
            event.eventDistanceKm <= 10.0 -> 7
            event.eventDistanceKm <= 25.0 -> 4
            else -> 1
        }

        val trustPoints =
            if (event.eventIsVerifiedOrganisation) 5 else 0

        val factors = listOf(
            VolunteerMatchFactor(
                title = "Skill Path fit",
                explanation =
                    if (pathIsExperienced) {
                        "Continues your verified ${role.rolePrimarySkillPath} evidence."
                    } else {
                        "Opens ${role.rolePrimarySkillPath} as a new growth path."
                    },
                earnedPoints = pathPoints,
                maximumPoints = 30,
                status =
                    if (pathIsExperienced) {
                        VolunteerMatchFactorStatus.STRENGTH
                    } else {
                        VolunteerMatchFactorStatus.OPPORTUNITY
                    }
            ),
            VolunteerMatchFactor(
                title = "Verified skills",
                explanation =
                    if (matchingSkills.isEmpty()) {
                        "No verified skill overlap yet; this role can create new evidence."
                    } else {
                        "Matches ${matchingSkills.take(3).joinToString(", ")}."
                    },
                earnedPoints = skillPoints,
                maximumPoints = 25,
                status =
                    if (matchingSkills.isEmpty()) {
                        VolunteerMatchFactorStatus.OPPORTUNITY
                    } else {
                        VolunteerMatchFactorStatus.STRENGTH
                    }
            ),
            VolunteerMatchFactor(
                title = "Level eligibility",
                explanation =
                    if (volunteerIsEligible) {
                        "Your Level $volunteerLevel meets the Level ${role.roleMinimumSkillPathLevel} requirement."
                    } else {
                        "Requires Level ${role.roleMinimumSkillPathLevel}; your current verified level is $volunteerLevel."
                    },
                earnedPoints = eligibilityPoints,
                maximumPoints = 20,
                status =
                    if (volunteerIsEligible) {
                        VolunteerMatchFactorStatus.STRENGTH
                    } else {
                        VolunteerMatchFactorStatus.ATTENTION
                    }
            ),
            VolunteerMatchFactor(
                title = "Growth potential",
                explanation =
                    if (newSkills.isEmpty()) {
                        "Reinforces skills you have already verified."
                    } else {
                        "Can build ${newSkills.take(3).joinToString(", ")}."
                    },
                earnedPoints = growthPoints,
                maximumPoints = 10,
                status = VolunteerMatchFactorStatus.OPPORTUNITY
            ),
            VolunteerMatchFactor(
                title = "Practical access",
                explanation = when {
                    event.eventOpportunityType.equals(
                        other = "Remote",
                        ignoreCase = true
                    ) -> "Remote participation removes travel distance."

                    event.eventDistanceKm != null ->
                        "${event.eventDistanceKm} km from your current search area."

                    else ->
                        "Travel distance is not available yet."
                },
                earnedPoints = accessPoints,
                maximumPoints = 10,
                status =
                    if (accessPoints >= 7) {
                        VolunteerMatchFactorStatus.STRENGTH
                    } else {
                        VolunteerMatchFactorStatus.ATTENTION
                    }
            ),
            VolunteerMatchFactor(
                title = "Organisation trust",
                explanation =
                    if (event.eventIsVerifiedOrganisation) {
                        "Published by a verified organisation."
                    } else {
                        "Organisation verification is not shown for this opportunity."
                    },
                earnedPoints = trustPoints,
                maximumPoints = 5,
                status =
                    if (event.eventIsVerifiedOrganisation) {
                        VolunteerMatchFactorStatus.STRENGTH
                    } else {
                        VolunteerMatchFactorStatus.ATTENTION
                    }
            )
        )

        return RoleMatch(
            role = role,
            score = factors
                .sumOf { factor ->
                    factor.earnedPoints
                }
                .coerceIn(0, 100),
            factors = factors
        )
    }

    private fun matchLabel(score: Int): String =
        when {
            score >= 90 -> "Excellent match"
            score >= 75 -> "Strong match"
            score >= 60 -> "Good match"
            else -> "Growth match"
        }

    private data class RoleMatch(
        val role: VolunteerOpportunityRole,
        val score: Int,
        val factors: List<VolunteerMatchFactor>
    )
}
