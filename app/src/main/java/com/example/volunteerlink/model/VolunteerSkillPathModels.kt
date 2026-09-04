
package com.example.volunteerlink.model

import kotlinx.serialization.Serializable

@Serializable
// Purpose: Handles volunteer skill path as one reusable step in the Volunteer flow.
// Usage: Read by repositories, ViewModels and Compose screens as shared Volunteer state.
// Result: The caller receives a stable value or action for the next Volunteer-flow step.
data class VolunteerSkillPath(
    val skillPathId: String,
    val name: String,
    val description: String?,
    val pathMode: String,
    val progressionType: String,
    val levels: List<VolunteerSkillPathLevel>,
    val skills: List<VolunteerSkill>,
    val currentLevel: Int,
    val verifiedAssignments: Int,
    val verifiedMinutes: Int?
) {
    val hasVerifiedEvidence: Boolean
        get() = verifiedAssignments > 0 ||
                (verifiedMinutes ?: 0) > 0

    val nextLevel: VolunteerSkillPathLevel?
        get() = levels
            .sortedBy { skillPathLevel ->
                skillPathLevel.levelNumber
            }
            .firstOrNull { skillPathLevel ->
                skillPathLevel.levelNumber >
                        currentLevel
            }

    val progressFraction: Float
        get() {
            val targetLevel = nextLevel
                ?: return 1f

            val assignmentProgress =
                if (
                    targetLevel.requiredAssignments == 0
                ) {
                    1f
                } else {
                    verifiedAssignments.toFloat() /
                            targetLevel.requiredAssignments
                                .toFloat()
                }

            val minuteProgress =
                targetLevel.requiredMinutes
                    ?.let { requiredMinutes ->
                        if (requiredMinutes == 0) {
                            1f
                        } else {
                            (verifiedMinutes ?: 0).toFloat() /
                                    requiredMinutes.toFloat()
                        }
                    }

            return if (minuteProgress == null) {
                assignmentProgress.coerceIn(0f, 1f)
            } else {
                minOf(
                    assignmentProgress,
                    minuteProgress
                ).coerceIn(0f, 1f)
            }
        }

    // Purpose: Handles level is reached as one reusable step in the Volunteer flow.
    // Usage: Read by repositories, ViewModels and Compose screens as shared Volunteer state.
    // Result: The caller receives a stable value or action for the next Volunteer-flow step.
    fun levelIsReached(
        skillPathLevel: VolunteerSkillPathLevel
    ): Boolean {
        val assignmentsReached =
            verifiedAssignments >=
                    skillPathLevel.requiredAssignments

        val minutesReached =
            skillPathLevel.requiredMinutes
                ?.let { requiredMinutes ->
                    (verifiedMinutes ?: 0) >= requiredMinutes
                }
                ?: true

        return assignmentsReached && minutesReached
    }
}

@Serializable
// Purpose: Handles volunteer skill path level as one reusable step in the Volunteer flow.
// Usage: Read by repositories, ViewModels and Compose screens as shared Volunteer state.
// Result: The caller receives a stable value or action for the next Volunteer-flow step.
data class VolunteerSkillPathLevel(
    val pathLevelId: String,
    val levelNumber: Int,
    val levelName: String,
    val requiredAssignments: Int,
    val requiredMinutes: Int?
)

@Serializable
// Purpose: Handles volunteer skill as one reusable step in the Volunteer flow.
// Usage: Read by repositories, ViewModels and Compose screens as shared Volunteer state.
// Result: The caller receives a stable value or action for the next Volunteer-flow step.
data class VolunteerSkill(
    val skillId: String,
    val name: String,
    val description: String?
)

