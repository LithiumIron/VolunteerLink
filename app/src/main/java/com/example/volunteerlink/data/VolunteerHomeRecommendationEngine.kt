
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
        filter: VolunteerHomeFeedFilter,
        applications: List<VolunteerOpportunityApplication> = emptyList(),
        nowMillis: Long = com.example.volunteerlink.data.time.AppClock.nowMillis()
    ): List<VolunteerOpportunityEvent> =
        events
            .asSequence()
            .filter { event ->
                event.eventVolunteerRoles.any { role ->
                    VolunteerDiscoveryEligibility.canRecommendRole(event, role, applications, nowMillis)
                }
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
                        event.eventVolunteerRoles.any { role ->
                            role.roleMode.ifBlank { event.eventOpportunityType }.equals("Physical", true) &&
                                VolunteerDiscoveryEligibility.canRecommendRole(event, role, applications, nowMillis)
                        }

                    VolunteerHomeFeedFilter.NEAR_ME ->
                        event.eventDistanceKm?.let { distanceKm ->
                            distanceKm <= 10.0
                        } == true

                    VolunteerHomeFeedFilter.REMOTE ->
                        event.eventVolunteerRoles.any { role ->
                            role.roleMode.ifBlank { event.eventOpportunityType }.equals("Remote", true) &&
                                VolunteerDiscoveryEligibility.canRecommendRole(event, role, applications, nowMillis)
                        }

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
        currentSkillPathLevels: Map<String, Int> = emptyMap(),
        nowMillis: Long = com.example.volunteerlink.data.time.AppClock.nowMillis()
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
                        add(pathName.matchKey())
                    }

                currentSkillPathLevels
                    .filterValues { currentLevel ->
                        currentLevel > 1
                    }
                    .keys
                    .forEach { pathName ->
                        add(pathName.matchKey())
                    }
            }

        val interestPaths =
            volunteerApplications
                .filter { application ->
                    application.applicationStatus !=
                        VolunteerApplicationStatus.CANCELLED
                }
                .mapNotNull { application ->
                    application.applicationPrimarySkillPath
                }
                .filter { pathName -> pathName.isNotBlank() }
                .map(String::matchKey)
                .toSet()

        val verifiedSkills =
            completedApplications
                .flatMap { application ->
                    application.applicationPractisedSkills
                }
                .filter { skillName ->
                    skillName.isNotBlank()
                }
                .map(String::matchKey)
                .toSet()

        return VolunteerHomeFeedEngine
            .filter(
                events = volunteerOpportunityEvents,
                filter = VolunteerHomeFeedFilter.ALL,
                applications = volunteerApplications,
                nowMillis = nowMillis
            )
            .filter { event ->
                event.eventAvailableSpots > 0 &&
                    event.eventVolunteerRoles.any { it.roleVacancies > 0 }
            }
            .mapNotNull { event ->
                createRecommendation(
                    event = event,
                    applications = volunteerApplications,
                    nowMillis = nowMillis,
                    experiencedPaths = experiencedPaths,
                    interestPaths = interestPaths,
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
        applications: List<VolunteerOpportunityApplication>,
        nowMillis: Long,
        experiencedPaths: Set<String>,
        interestPaths: Set<String>,
        verifiedSkills: Set<String>,
        currentSkillPathLevels: Map<String, Int>
    ): VolunteerHomeRecommendation? {
        val bestRoleMatch =
            event.eventVolunteerRoles
                .filter { role ->
                    VolunteerDiscoveryEligibility.canRecommendRole(event, role, applications, nowMillis) &&
                        role.roleMinimumSkillPathLevel <= (currentSkillPathLevels.entries
                            .firstOrNull { it.key.matchKey() == role.rolePrimarySkillPath.matchKey() }
                            ?.value ?: 1)
                }
                .map { role ->
                    scoreRole(
                        event = event,
                        role = role,
                        experiencedPaths = experiencedPaths,
                        interestPaths = interestPaths,
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
                ) ?: return null

        return VolunteerHomeRecommendation(
            event = event,
            score = bestRoleMatch.score,
            matchLabel = matchLabel(bestRoleMatch.score),
            bestRoleId = bestRoleMatch.role.roleId,
            bestRoleTitle = bestRoleMatch.role.roleTitle,
            reason = recommendationReason(
                role = bestRoleMatch.role,
                experiencedPaths = experiencedPaths,
                interestPaths = interestPaths,
                verifiedSkills = verifiedSkills
            ),
            factors = bestRoleMatch.factors
        )
    }

    private fun scoreRole(
        event: VolunteerOpportunityEvent,
        role: VolunteerOpportunityRole,
        experiencedPaths: Set<String>,
        interestPaths: Set<String>,
        verifiedSkills: Set<String>,
        currentSkillPathLevels: Map<String, Int>
    ): RoleMatch {
        val volunteerLevel =
            currentSkillPathLevels.entries
                .firstOrNull { (pathName, _) ->
                    pathName.matchKey() ==
                        role.rolePrimarySkillPath.matchKey()
                }
                ?.value
                ?: 1

        val pathIsExperienced =
            role.rolePrimarySkillPath.matchKey() in experiencedPaths

        val pathWasPreviouslySelected =
            role.rolePrimarySkillPath.matchKey() in interestPaths

        val pathPoints = when {
            pathIsExperienced -> 30
            pathWasPreviouslySelected -> 24
            role.roleMinimumSkillPathLevel <= 1 -> 16
            else -> 6
        }

        val roleSkills =
            role.roleSkillsPractised
                .filter { skillName ->
                    skillName.isNotBlank()
                }
                .distinct()

        val matchingSkills =
            roleSkills.filter { skillName ->
                skillName.matchKey() in verifiedSkills
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
                skillName.matchKey() in verifiedSkills
            }

        val growthPoints =
            (newSkills.size * 3 + 1).coerceIn(2, 10)

        val travelAccessPoints = when {
            event.eventOpportunityType.equals(
                other = "Remote",
                ignoreCase = true
            ) -> 6

            event.eventDistanceKm == null -> 2
            event.eventDistanceKm <= 5.0 -> 6
            event.eventDistanceKm <= 10.0 -> 4
            event.eventDistanceKm <= 25.0 -> 2
            else -> 0
        }

        val availabilityPoints =
            role.roleVacancies.coerceIn(0, 4)

        val accessPoints =
            travelAccessPoints + availabilityPoints

        val trustPoints =
            if (event.eventIsVerifiedOrganisation) 5 else 0

        val factors = listOf(
            VolunteerMatchFactor(
                title = "Skill Path fit",
                explanation =
                    when {
                        pathIsExperienced ->
                            "Builds on organisation-verified experience in ${role.rolePrimarySkillPath}."

                        pathWasPreviouslySelected ->
                            "You previously selected a role in ${role.rolePrimarySkillPath}; this continues that interest, but it is not verified experience yet."

                        else ->
                            "A beginner-accessible way to explore ${role.rolePrimarySkillPath}; this is a growth suggestion, not proof of prior experience."
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
                    ) ->
                        "Remote role: no travel distance is required. ${role.roleVacancies} ${if (role.roleVacancies == 1) "vacancy" else "vacancies"} currently available."

                    event.eventDistanceKm != null ->
                        "Travel distance from your current device location: ${event.eventDistanceKm} km. ${role.roleVacancies} ${if (role.roleVacancies == 1) "vacancy" else "vacancies"} currently available."

                    else ->
                        "Travel distance cannot be calculated until location access is available. ${role.roleVacancies} ${if (role.roleVacancies == 1) "vacancy" else "vacancies"} currently available."
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

    private fun recommendationReason(
        role: VolunteerOpportunityRole,
        experiencedPaths: Set<String>,
        interestPaths: Set<String>,
        verifiedSkills: Set<String>
    ): String {
        val matchingSkills =
            role.roleSkillsPractised.filter { skillName ->
                skillName.matchKey() in verifiedSkills
            }

        return when {
            matchingSkills.isNotEmpty() ->
                "Builds on your verified ${matchingSkills.take(2).joinToString(" and ")} evidence in ${role.rolePrimarySkillPath}."

            role.rolePrimarySkillPath.matchKey() in experiencedPaths ->
                "Continues your verified ${role.rolePrimarySkillPath} experience."

            role.rolePrimarySkillPath.matchKey() in interestPaths ->
                "Continues an area you previously selected; completion is still required before it becomes verified experience."

            else ->
                "A beginner-accessible opportunity to explore ${role.rolePrimarySkillPath} and earn your first verified evidence."
        }
    }

    private data class RoleMatch(
        val role: VolunteerOpportunityRole,
        val score: Int,
        val factors: List<VolunteerMatchFactor>
    )
}

private fun String.matchKey(): String =
    trim()
        .lowercase()
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
