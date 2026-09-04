
package com.example.volunteerlink.data

// Reads verified Skill Path progress and completion evidence for the volunteer.

import com.example.volunteerlink.model.VolunteerSkill
import com.example.volunteerlink.model.VolunteerSkillPath
import com.example.volunteerlink.model.VolunteerSkillPathLevel
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object VolunteerSkillPathRepository {

    suspend fun getSkillPaths():
        List<VolunteerSkillPath> {
        return try {
            loadSkillPathsFromCloud().also { skillPaths ->
                VolunteerDashboardDataSource.cacheSkillPaths(skillPaths)
            }
        } catch (exception: Exception) {
            VolunteerDashboardDataSource.readCachedSkillPaths()
                ?: throw exception
        }
    }

    private suspend fun loadSkillPathsFromCloud():
        List<VolunteerSkillPath> {
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

        // Progress is derived from verified, completed participation records.
        // Accepted or in-progress applications must never increase Skill Path
        // assignments or minutes.
        runCatching {
            supabase.postgrest.rpc(
                function = "refresh_my_skill_path_progress"
            )
        }.onFailure { exception ->
            exception.printStackTrace()
        }

        // Do not turn a schema/permission error into a fake zero-progress
        // result. Let getSkillPaths() use the last valid local cache instead.
        val progressRows =
            supabase
                .from("volunteer_skill_path_progress")
                .select()
                .decodeList<VolunteerSkillPathProgressRow>()

        val progressByPathId =
            progressRows.associateBy {
                    skillPathProgressRow ->
                skillPathProgressRow.skillPathId
            }

        return skillPathRows
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
                        currentLevel =
                            progress?.currentLevel ?: 1,
                        verifiedAssignments =
                            progress
                                ?.verifiedAssignments
                                ?: 0,
                        verifiedMinutes =
                            progress?.verifiedMinutes ?: 0
                    )
                }
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
) {
    val pathLevelId: String
        get() = "$skillPathId-L$levelNumber"
}

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
private data class VolunteerSkillPathProgressRow(
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

