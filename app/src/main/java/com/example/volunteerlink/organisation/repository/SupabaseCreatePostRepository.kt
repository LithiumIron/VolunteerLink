package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.create.model.CreateRoleSkill
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Supabase implementation used by the Create Post wizard.
 *
 * The fixed catalogue is small, so Step 2 loads role templates, skill paths
 * and skills once and joins them locally before showing the role cards.
 */
class SupabaseCreatePostRepository : CreatePostRepository {

    override suspend fun loadRoleCatalogue(): List<CreateRoleTemplate> {
        val pathRows = supabase
            .from("skill_paths")
            .select()
            .decodeList<JsonObject>()

        val skillRows = supabase
            .from("skills")
            .select()
            .decodeList<JsonObject>()

        val roleRows = supabase
            .from("role_templates")
            .select()
            .decodeList<JsonObject>()

        val pathNamesById = pathRows.associate { row ->
            row.requiredText("skill_path_id") to row.requiredText("name")
        }

        val skillsById = skillRows.associate { row ->
            val skill = CreateRoleSkill(
                skillId = row.requiredText("skill_id"),
                name = row.requiredText("name")
            )
            skill.skillId to skill
        }

        return roleRows
            .map { row ->
                val skillPathId = row.requiredText("skill_path_id")
                val practisedSkillIds = row.idList("skills_practised")
                val recommendedSkillIds = row.idList("recommended_skills")

                CreateRoleTemplate(
                    roleTemplateId = row.requiredText("role_template_id"),
                    roleName = row.requiredText("role_name"),
                    roleArea = row.requiredText("role_area"),
                    roleMode = VolunteerRoleMode.valueOf(
                        row.requiredText("role_mode")
                    ),
                    skillPathId = skillPathId,
                    skillPathName = pathNamesById[skillPathId]
                        ?: error("Missing Skill Path: $skillPathId"),
                    description = row.optionalText("description").orEmpty(),
                    skillsPractised = practisedSkillIds.map { skillId ->
                        skillsById[skillId]
                            ?: error("Missing Skill: $skillId")
                    },
                    recommendedSkills = recommendedSkillIds.map { skillId ->
                        skillsById[skillId]
                            ?: error("Missing Skill: $skillId")
                    },
                    defaultLevel = VolunteerRoleLevel.valueOf(
                        row.requiredText("default_level")
                    )
                )
            }
            .sortedBy { it.roleTemplateId }
    }

    private fun JsonObject.requiredText(key: String): String {
        return optionalText(key)
            ?: error("Missing '$key' in Create Post catalogue data.")
    }

    private fun JsonObject.optionalText(key: String): String? {
        return this[key]
            ?.jsonPrimitive
            ?.contentOrNull
    }

    private fun JsonObject.idList(key: String): List<String> {
        val array = this[key] as? JsonArray ?: return emptyList()

        return array.mapNotNull { element ->
            element.jsonPrimitive.contentOrNull
        }
    }
}
