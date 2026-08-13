package com.example.volunteerlink.data

import com.example.volunteerlink.model.VolunteerSkill
import com.example.volunteerlink.model.VolunteerSkillPath
import com.example.volunteerlink.model.VolunteerSkillPathLevel
import com.example.volunteerlink.model.VolunteerSkillPathRole
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object VolunteerSkillPathRepository {

    private const val DEMO_VOLUNTEER_USER_ID =
        "USER001"

    private var cachedSkillPaths:
        List<VolunteerSkillPath>? = null

    suspend fun getSkillPaths(
        forceRefresh: Boolean = false,
        volunteerUserId: String =
            DEMO_VOLUNTEER_USER_ID
    ): List<VolunteerSkillPath> {
        if (!forceRefresh) {
            cachedSkillPaths?.let {
                    volunteerSkillPaths ->
                return volunteerSkillPaths
            }
        }

        val skillPathRows =
            supabase
                .from("skill_paths")
                .select()
                .decodeList<VolunteerSkillPathRow>()

        val levelRows =
            supabase
                .from("skill_path_levels")
                .select()
                .decodeList<VolunteerSkillPathLevelRow>()

        val skillRows =
            supabase
                .from("skills")
                .select()
                .decodeList<VolunteerSkillRow>()

        val roleRows =
            supabase
                .from("role_templates")
                .select()
                .decodeList<VolunteerSkillPathRoleRow>()

        val progressRows =
            supabase
                .from("volunteer_skill_path_progress")
                .select()
                .decodeList<VolunteerSkillPathProgressRow>()
                .filter { skillPathProgressRow ->
                    skillPathProgressRow.userId ==
                            volunteerUserId
                }

        val progressByPathId =
            progressRows.associateBy {
                    skillPathProgressRow ->
                skillPathProgressRow.skillPathId
            }

        val loadedSkillPaths =
            skillPathRows
                .sortedBy { skillPathRow ->
                    skillPathRow.skillPathId
                }
                .map { skillPathRow ->
                    val progress =
                        progressByPathId[
                            skillPathRow.skillPathId
                        ]

                    VolunteerSkillPath(
                        skillPathId =
                            skillPathRow.skillPathId,
                        name = skillPathRow.name,
                        description =
                            skillPathRow.description,
                        pathMode =
                            skillPathRow.pathMode,
                        progressionType =
                            skillPathRow.progressionType,
                        levels = levelRows
                            .filter { levelRow ->
                                levelRow.skillPathId ==
                                        skillPathRow
                                            .skillPathId
                            }
                            .sortedBy { levelRow ->
                                levelRow.levelNumber
                            }
                            .map { levelRow ->
                                VolunteerSkillPathLevel(
                                    pathLevelId =
                                        levelRow.pathLevelId,
                                    levelNumber =
                                        levelRow.levelNumber,
                                    levelName =
                                        levelRow.levelName,
                                    requiredAssignments =
                                        levelRow
                                            .requiredAssignments,
                                    requiredMinutes =
                                        levelRow
                                            .requiredMinutes
                                )
                            },
                        skills = skillRows
                            .filter { skillRow ->
                                skillRow.skillPathId ==
                                        skillPathRow
                                            .skillPathId
                            }
                            .sortedBy { skillRow ->
                                skillRow.skillId
                            }
                            .map { skillRow ->
                                VolunteerSkill(
                                    skillId =
                                        skillRow.skillId,
                                    name = skillRow.name,
                                    description =
                                        skillRow.description
                                )
                            },
                        relatedRoles = roleRows
                            .filter { roleRow ->
                                roleRow.skillPathId ==
                                        skillPathRow
                                            .skillPathId
                            }
                            .sortedBy { roleRow ->
                                roleRow.roleTemplateId
                            }
                            .map { roleRow ->
                                VolunteerSkillPathRole(
                                    roleTemplateId =
                                        roleRow.roleTemplateId,
                                    roleName = roleRow.roleName,
                                    roleArea = roleRow.roleArea,
                                    roleMode = roleRow.roleMode,
                                    description =
                                        roleRow.description
                                )
                            },
                        currentLevel =
                            progress?.currentLevel ?: 1,
                        verifiedAssignments =
                            progress
                                ?.verifiedAssignments
                                ?: 0,
                        verifiedMinutes =
                            progress?.verifiedMinutes
                    )
                }

        cachedSkillPaths = loadedSkillPaths

        return loadedSkillPaths
    }
}

@Serializable
private data class VolunteerSkillPathRow(
    @SerialName("skill_path_id")
    val skillPathId: String,
    val name: String,
    val description: String? = null,
    @SerialName("path_mode")
    val pathMode: String,
    @SerialName("progression_type")
    val progressionType: String
)

@Serializable
private data class VolunteerSkillPathLevelRow(
    @SerialName("path_level_id")
    val pathLevelId: String,
    @SerialName("skill_path_id")
    val skillPathId: String,
    @SerialName("level_number")
    val levelNumber: Int,
    @SerialName("level_name")
    val levelName: String,
    @SerialName("required_assignments")
    val requiredAssignments: Int,
    @SerialName("required_minutes")
    val requiredMinutes: Int? = null
)

@Serializable
private data class VolunteerSkillRow(
    @SerialName("skill_id")
    val skillId: String,
    @SerialName("skill_path_id")
    val skillPathId: String,
    val name: String,
    val description: String? = null
)

@Serializable
private data class VolunteerSkillPathRoleRow(
    @SerialName("role_template_id")
    val roleTemplateId: String,
    @SerialName("role_name")
    val roleName: String,
    @SerialName("role_area")
    val roleArea: String,
    @SerialName("role_mode")
    val roleMode: String,
    @SerialName("skill_path_id")
    val skillPathId: String,
    val description: String? = null,
    @SerialName("skills_practised")
    val skillsPractised: List<String> = emptyList(),
    @SerialName("recommended_skills")
    val recommendedSkills: List<String> = emptyList()
)

@Serializable
private data class VolunteerSkillPathProgressRow(
    @SerialName("path_progress_id")
    val pathProgressId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("skill_path_id")
    val skillPathId: String,
    @SerialName("current_level")
    val currentLevel: Int,
    @SerialName("verified_assignments")
    val verifiedAssignments: Int,
    @SerialName("verified_minutes")
    val verifiedMinutes: Int? = null,
    @SerialName("updated_at")
    val updatedAt: String
)
