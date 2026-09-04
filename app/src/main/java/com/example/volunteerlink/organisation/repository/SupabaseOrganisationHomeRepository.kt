package com.example.volunteerlink.organisation.repository

// FILE OVERVIEW:
/*
 * SupabaseOrganisationHomeRepository defines or implements data access used by the organisation Home dashboard flow.
 * Repository code keeps Supabase/RPC/storage details away from the composables and ViewModels
 * so UI code can work with application models instead of backend-specific responses.
 */


import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.home.model.OrganisationHomeParticipation
import com.example.volunteerlink.organisation.home.model.OrganisationHomePost
import com.example.volunteerlink.organisation.home.model.OrganisationHomeRole
import com.example.volunteerlink.organisation.home.model.OrganisationHomeSchedule
import com.example.volunteerlink.organisation.home.model.OrganisationHomeSnapshot
import com.example.volunteerlink.organisation.home.model.OrganisationImpactWeaveAttention
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
/**
 * Holds the values represented by partner post summary row as one strongly typed model.
 * It keeps backend-facing work behind the Home dashboard repository boundary.
 */
data class PartnerPostSummaryRow(
    @SerialName("post_id") val postId: String,
    @SerialName("owner_organisation_name") val ownerOrganisationName: String,
    val title: String,
    val description: String,
    val mode: String,
    val status: String,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("contribution_summary") val contributionSummary: String = "",
    @SerialName("is_owner") val isOwner: Boolean = false
)

@Serializable
/**
 * Holds the values represented by impact weave attention row as one strongly typed model.
 * It keeps backend-facing work behind the Home dashboard repository boundary.
 */
private data class ImpactWeaveAttentionRow(
    @SerialName("draft_id") val draftId: String,
    val title: String,
    val status: String,
    @SerialName("attention_type") val attentionType: String,
    val severity: String,
    val message: String,
    @SerialName("planning_deadline") val planningDeadline: String? = null,
    @SerialName("days_remaining") val daysRemaining: Int? = null
)

/**
 * Supabase implementation for Organisation Home.
 *
 * One post query embeds the normalized one-to-one Physical/Remote details and
 * one-to-many schedule rows. This avoids placing relational IDs in JSONB and
 * avoids an N+1 query for every post.
 */
class SupabaseOrganisationHomeRepository : OrganisationHomeRepository {

    /**
     * Loads the partner posts needed by the organisation Home dashboard flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    override suspend fun loadPartnerPosts(): List<PartnerPostSummary> {
        val response = supabase.postgrest.rpc("organisation_list_partner_posts")
        return Json { ignoreUnknownKeys = true }
            .decodeFromString<List<PartnerPostSummaryRow>>(response.data)
            .map { row ->
                PartnerPostSummary(
                    postId = row.postId,
                    ownerOrganisationName = row.ownerOrganisationName,
                    title = row.title,
                    description = row.description,
                    mode = row.mode,
                    status = row.status,
                    startDate = row.startDate,
                    endDate = row.endDate,
                    locationName = row.locationName,
                    contributionSummary = row.contributionSummary,
                    isOwner = row.isOwner
                )
            }
    }

    /**
     * Loads the home snapshot needed by the organisation Home dashboard flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    override suspend fun loadHomeSnapshot(
        organisationId: String
    ): OrganisationHomeSnapshot {
        // Persist role-start/full-capacity application outcomes before Home counts them.
        supabase.postgrest.rpc(
            function = "organisation_resolve_application_lifecycle"
        )

        val organisationRow = supabase
            .from("organisations")
            .select(columns = Columns.raw("organisation_id,organisation_name")) {
                filter {
                    eq("organisation_id", organisationId)
                }
            }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?: error("Organisation $organisationId was not found.")

        val postRows = supabase
            .from("volunteer_posts")
            .select(
                columns = Columns.raw(
                    """
                    post_id,
                    title,
                    mode,
                    status,
                    category,
                    physical_details (
                        start_date,
                        end_date,
                        start_time,
                        location_name
                    ),
                    remote_details (
                        start_date,
                        end_date,
                        new_end_date
                    ),
                    schedule_items (
                        schedule_item_id,
                        schedule_type,
                        schedule_date,
                        title,
                        start_time
                    )
                    """.trimIndent()
                )
            ) {
                filter {
                    eq("organisation_id", organisationId)
                }
            }
            .decodeList<JsonObject>()

        val posts = postRows.map { row ->
            val physical = row.firstRelatedObject("physical_details")
            val remote = row.firstRelatedObject("remote_details")
            val schedules = row.relatedObjects("schedule_items")
                .map { scheduleRow ->
                    OrganisationHomeSchedule(
                        scheduleItemId = scheduleRow.requiredText("schedule_item_id"),
                        scheduleType = scheduleRow.requiredText("schedule_type"),
                        scheduleDate = scheduleRow.requiredText("schedule_date"),
                        title = scheduleRow.requiredText("title"),
                        startTime = scheduleRow.optionalText("start_time")
                    )
                }
                .sortedWith(
                    compareBy<OrganisationHomeSchedule> { it.scheduleDate }
                        .thenBy { it.startTime.orEmpty() }
                )

            OrganisationHomePost(
                postId = row.requiredText("post_id"),
                title = row.requiredText("title"),
                mode = row.requiredText("mode"),
                status = row.requiredText("status"),
                category = row.optionalText("category"),
                physicalStartDate = physical?.optionalText("start_date"),
                physicalEndDate = physical?.optionalText("end_date"),
                physicalStartTime = physical?.optionalText("start_time"),
                physicalLocationName = physical?.optionalText("location_name"),
                remoteStartDate = remote?.optionalText("start_date"),
                remoteEndDate = remote?.optionalText("end_date"),
                remoteNewEndDate = remote?.optionalText("new_end_date"),
                schedules = schedules,
                roles = emptyList()
            )
        }

        // Fetch application attention data separately instead of relying on a
        // deep PostgREST embed. role_participations belongs to a post role by
        // the pair (post_id, role_template_id), so joining by that pair here is
        // explicit and stable even when PostgREST relationship inference is
        // ambiguous.
        val postIds = posts.map { it.postId }

        val roleRows: List<JsonObject> = if (postIds.isEmpty()) {
            emptyList()
        } else {
            supabase
                .from("post_roles")
                .select(
                    columns = Columns.raw(
                        "post_id,role_template_id,application_method"
                    )
                ) {
                    filter {
                        isIn("post_id", postIds)
                        eq("application_method", "REVIEW_APPLICANTS")
                    }
                }
                .decodeList<JsonObject>()
        }

        val participationRows: List<JsonObject> = if (postIds.isEmpty()) {
            emptyList()
        } else {
            supabase
                .from("role_participations")
                .select(
                    columns = Columns.raw(
                        "post_id,role_template_id,user_id,application_status"
                    )
                ) {
                    filter {
                        isIn("post_id", postIds)
                        eq("application_status", "PENDING")
                    }
                }
                .decodeList<JsonObject>()
        }

        val roleTemplateIds = roleRows
            .map { it.requiredText("role_template_id") }
            .distinct()

        // role_mode is essential for Hybrid posts: a Physical role closes from
        // the Physical timeline, while a Remote role closes from the Remote one.
        val roleTemplateRows: List<JsonObject> = if (roleTemplateIds.isEmpty()) {
            emptyList()
        } else {
            supabase
                .from("role_templates")
                .select(
                    columns = Columns.raw(
                        "role_template_id,role_name,role_mode"
                    )
                ) {
                    filter {
                        isIn("role_template_id", roleTemplateIds)
                    }
                }
                .decodeList<JsonObject>()
        }

        val roleTemplatesById = roleTemplateRows.associateBy { row ->
            row.requiredText("role_template_id")
        }

        val participationsByRole = participationRows.groupBy { participationRow ->
            participationRow.requiredText("post_id") to
                    participationRow.requiredText("role_template_id")
        }

        val rolesByPost = roleRows
            .mapNotNull { roleRow ->
                val postId = roleRow.requiredText("post_id")
                val roleTemplateId = roleRow.requiredText("role_template_id")
                val template = roleTemplatesById[roleTemplateId]
                    ?: return@mapNotNull null

                postId to OrganisationHomeRole(
                    roleTemplateId = roleTemplateId,
                    roleName = template.requiredText("role_name"),
                    roleMode = template.requiredText("role_mode"),
                    applicationMethod = roleRow.requiredText("application_method"),
                    participations = participationsByRole[
                        postId to roleTemplateId
                    ].orEmpty().map { participationRow ->
                        OrganisationHomeParticipation(
                            userId = participationRow.requiredText("user_id"),
                            applicationStatus = participationRow.requiredText(
                                "application_status"
                            )
                        )
                    }
                )
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )

        val postsWithApplications = posts.map { post ->
            post.copy(roles = rolesByPost[post.postId].orEmpty())
        }

        val impactWeaveAttention = runCatching {
            supabase.postgrest
                .rpc("organisation_list_impact_weave_attention")
                .decodeList<ImpactWeaveAttentionRow>()
                .map { row ->
                    OrganisationImpactWeaveAttention(
                        draftId = row.draftId,
                        title = row.title,
                        status = row.status,
                        attentionType = row.attentionType,
                        severity = row.severity,
                        message = row.message,
                        planningDeadline = row.planningDeadline,
                        daysRemaining = row.daysRemaining
                    )
                }
        }.getOrDefault(emptyList())

        return OrganisationHomeSnapshot(
            organisationId = organisationId,
            organisationName = organisationRow.requiredText("organisation_name"),
            posts = postsWithApplications,
            impactWeaveAttention = impactWeaveAttention
        )
    }

    /**
     * PostgREST may represent a one-to-one embed as an object and some schema
     * configurations may still return a one-item array. Accept both shapes so
     * this repository remains stable if the relationship representation changes.
     */
    private fun JsonObject.firstRelatedObject(key: String): JsonObject? {
        return when (val element = this[key]) {
            is JsonObject -> element
            is JsonArray -> element.firstOrNull() as? JsonObject
            else -> null
        }
    }

    /**
     * Derives the json object value used by the organisation Home dashboard flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    private fun JsonObject.relatedObjects(key: String): List<JsonObject> {
        return when (val element = this[key]) {
            is JsonArray -> element.mapNotNull { it as? JsonObject }
            is JsonObject -> listOf(element)
            else -> emptyList()
        }
    }

    /**
     * Derives the json object value used by the organisation Home dashboard flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    private fun JsonObject.requiredText(key: String): String {
        return optionalText(key)
            ?: error("Missing required Supabase field: $key")
    }

    /**
     * Derives the json object value used by the organisation Home dashboard flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    private fun JsonObject.optionalText(key: String): String? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        return runCatching {
            element.jsonPrimitive.contentOrNull
        }.getOrNull()
    }
}
