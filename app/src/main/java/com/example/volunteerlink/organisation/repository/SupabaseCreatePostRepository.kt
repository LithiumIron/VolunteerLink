package com.example.volunteerlink.organisation.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements all Supabase-facing persistence required by the Create/Edit Post wizard.
//
// New posts are sent to ownership-checked PostgreSQL RPCs as structured JSON payloads so creation of
// volunteer_posts and its normalized child records can be coordinated by the database.
//
// Thumbnail bytes are uploaded to Supabase Storage separately from the database transaction; the repository
// therefore tracks uploaded paths and performs best-effort cleanup if a later save step fails.
//
// Existing-post editing hydrates the current normalized tables, maps them back into CreatePostDraft, then submits
// one edit payload through the atomic editor RPC instead of deleting/recreating a published post.
//
// The repository always resolves the authenticated OrganisationSession and verification state before write
// operations; server-side auth.uid(), ownership checks and RLS/RPC rules remain the final security boundary.
//
// All PostgREST access uses the shared Supabase client whose default schema is v1_erd_test.
//
// Architectural layer: Data/repository layer.
// ============================================================================


import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.auth.OrganisationSession
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.create.PostEditParticipationInput
import com.example.volunteerlink.organisation.create.PostEditPolicyInput
import com.example.volunteerlink.organisation.create.PostEditRoleInput
import com.example.volunteerlink.organisation.create.PostEditScheduleInput
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreateRoleSkill
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.RoleApplicationMethod
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Supabase implementation used by the Create Post wizard.
 *
 * New-post creation and existing-post editing both use ownership-checked PostgreSQL
 * RPCs. Storage uploads remain separate because Supabase Storage is outside the
 * database transaction, so failed uploads/DB saves are cleaned up best-effort.
 */
/**
 * DETAILED DECLARATION — SupabaseCreatePostRepository
 *
 * Data-access implementation/contract for Supabase Create Post Repository, isolating backend details from the
 * screen and ViewModel layers.
 *
 * Protected server state still relies on authenticated Supabase authorization and database rules rather than
 * trusting client-side checks alone.
 *
 * This implementation translates VolunteerLink models to PostgREST/RPC/Storage operations and maps backend
 * responses back into domain models.
 */
class SupabaseCreatePostRepository : CreatePostRepository {

    /**
     * Saves the draft for the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — saveDraft
     *
     * Persists the current validated Create Post draft as a real Supabase Volunteer Post with database status
     * DRAFT.
     *
     * This is different from the device autosave: Save Draft creates a server record that can appear in Manage,
     * while device autosave only protects unfinished typing on this phone.
     *
     * The method reuses the same normalized save pipeline as publishing but passes publishAfterSave = false so
     * creation/storage/error-cleanup behaviour remains consistent.
     */
    override suspend fun saveDraft(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): SavedPostResult {
        return savePost(
            draft = draft,
            roleCatalogue = roleCatalogue,
            thumbnail = thumbnail,
            publishAfterSave = false
        )
    }

    /**
     * Publishes the current Volunteer Post data after the required Create/Edit Post checks pass.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — publishPost
     *
     * Persists the current validated Create Post draft and completes the database transition to PUBLISHED.
     *
     * When the wizard was opened from Impact Weave, the successful post id is also passed into the conversion
     * workflow so accepted partnership support is linked to the real Volunteer Post.
     *
     * The common save pipeline handles normalized payload creation, optional thumbnail upload and failure
     * cleanup before this method returns success to the ViewModel.
     */
    override suspend fun publishPost(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?,
        impactWeaveDraftId: String?
    ): SavedPostResult {
        return savePost(
            draft = draft,
            roleCatalogue = roleCatalogue,
            thumbnail = thumbnail,
            publishAfterSave = true,
            impactWeaveDraftId = impactWeaveDraftId
        )
    }

    /**
     * Saves the post for the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — savePost
     *
     * Performs the repository/data-layer operation for save post.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_create_volunteer_post`: Creates the database-owned post identity and
     * normalized initial records from the validated Create Post payload; ownership is derived from the
     * authenticated organisation.
     *
     * Supabase RPC `organisation_finish_created_post`: Finalises a newly created post after optional thumbnail
     * upload by attaching the storage path and deciding whether the database record remains DRAFT or becomes
     * PUBLISHED.
     *
     * Supabase RPC `organisation_prepare_failed_impact_weave_post_cleanup`: Prepares an Impact Weave-linked
     * create attempt for safe cleanup when the Volunteer Post could not be fully completed.
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Uses Supabase Storage for binary/file content while database rows keep only the controlled storage path
     * and metadata.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private suspend fun savePost(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?,
        publishAfterSave: Boolean,
        impactWeaveDraftId: String? = null
    ): SavedPostResult {
        val organisation = OrganisationSession.requireContext()
        require(organisation.isVerified) {
            "Your organisation must be verified before creating volunteer posts."
        }

        val payload = buildNewPostPayload(
            draft = draft,
            roleCatalogue = roleCatalogue
        )

        var createdPostId: String? = null
        var uploadedThumbnailPath: String? = null

        try {
            // RPCs that return JSONB come back as a JSON object, not a PostgREST row array.
            // Parse the raw RPC response directly instead of using decodeSingle(), which
            // expects an array-shaped SELECT response.
            // ------------------------------------------------------------------------
            // SUPABASE RPC: organisation_create_volunteer_post
            // Creates the database-owned post identity and normalized initial records from the validated Create
            // Post payload; ownership is derived from the authenticated organisation.
            // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
            // consistency checks belong on the server for this operation.
            // ------------------------------------------------------------------------
            val createResponse = supabase.postgrest.rpc(
                function = "organisation_create_volunteer_post",
                parameters = buildJsonObject {
                    put("p_payload", payload)
                }
            )
            val created = Json.parseToJsonElement(createResponse.data).jsonObject
            val postId = created.requiredText("post_id")
            createdPostId = postId

            if (thumbnail != null) {
                val safeExtension = thumbnail.fileExtension
                    .lowercase()
                    .filter { it.isLetterOrDigit() }
                    .ifBlank { "jpg" }

                val storagePath =
                    "$STORAGE_PREFIX/${organisation.organisationId}/$postId/" +
                            "${UUID.randomUUID()}.$safeExtension"

                supabase.storage.from(THUMBNAIL_BUCKET).upload(
                    path = storagePath,
                    data = thumbnail.bytes
                ) {
                    upsert = false
                    contentType = ContentType.parse(thumbnail.mimeType)
                }

                uploadedThumbnailPath = storagePath
            }

            // ------------------------------------------------------------------------
            // SUPABASE RPC: organisation_finish_created_post
            // Finalises a newly created post after optional thumbnail upload by attaching the storage path and
            // deciding whether the database record remains DRAFT or becomes PUBLISHED.
            // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
            // consistency checks belong on the server for this operation.
            // ------------------------------------------------------------------------
            supabase.postgrest.rpc(
                function = "organisation_finish_created_post",
                parameters = buildJsonObject {
                    put("p_post_id", postId)
                    put("p_publish", publishAfterSave)
                    if (uploadedThumbnailPath == null) {
                        put("p_thumbnail_path", JsonNull)
                    } else {
                        put("p_thumbnail_path", uploadedThumbnailPath)
                    }
                }
            )

            if (!impactWeaveDraftId.isNullOrBlank()) {
                SupabaseImpactWeaveRepository().completeConversion(
                    draftId = impactWeaveDraftId,
                    postId = postId
                )
            }

            return SavedPostResult(
                postId = postId,
                thumbnailPath = uploadedThumbnailPath
            )
        } catch (exception: Exception) {
            uploadedThumbnailPath?.let { path ->
                runCatching {
                    supabase.storage.from(THUMBNAIL_BUCKET).delete(path)
                }
            }

            createdPostId?.let { postId ->
                if (!impactWeaveDraftId.isNullOrBlank()) {
                    runCatching {
                        // ------------------------------------------------------------------------
                        // SUPABASE RPC: organisation_prepare_failed_impact_weave_post_cleanup
                        // Prepares an Impact Weave-linked create attempt for safe cleanup when the Volunteer
                        // Post could not be fully completed.
                        // The client sends parameters and waits for the database result; ownership, lifecycle
                        // and multi-row consistency checks belong on the server for this operation.
                        // ------------------------------------------------------------------------
                        supabase.postgrest.rpc(
                            function = "organisation_prepare_failed_impact_weave_post_cleanup",
                            parameters = buildJsonObject {
                                put("p_draft_id", impactWeaveDraftId)
                                put("p_post_id", postId)
                            }
                        )
                    }
                }
                runCatching {
                    // ------------------------------------------------------------------------
                    // SUPABASE RPC: organisation_delete_created_draft
                    // Removes a newly-created unfinished database draft during failure cleanup so a partially
                    // completed client workflow does not leave an orphan post.
                    // The client sends parameters and waits for the database result; ownership, lifecycle and
                    // multi-row consistency checks belong on the server for this operation.
                    // ------------------------------------------------------------------------
                    supabase.postgrest.rpc(
                        function = "organisation_delete_created_draft",
                        parameters = buildJsonObject { put("p_post_id", postId) }
                    )
                }
            }
            throw exception
        }
    }

    /**
     * Builds the new post payload used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — buildNewPostPayload
     *
     * Converts CreatePostDraft plus the fixed role catalogue into the JSON structure expected by the new-post
     * database RPC.
     *
     * The payload contains parent post fields and the normalized child information required for Physical/Remote
     * details, roles, role skills/responsibilities/questions and schedule items.
     *
     * Only values that belong to the selected PHYSICAL/REMOTE/HYBRID mode are included, preventing stale hidden
     * fields from another mode from being persisted.
     */
    private fun buildNewPostPayload(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): JsonObject {
        val postType = draft.postType
            ?: error("Choose a post type before saving.")
        val category = draft.category
            ?: error("Choose a category before saving.")

        val needsPhysical = postType == VolunteerPostType.PHYSICAL ||
                postType == VolunteerPostType.HYBRID
        val needsRemote = postType == VolunteerPostType.REMOTE ||
                postType == VolunteerPostType.HYBRID

        return buildJsonObject {
            put("title", draft.title.trim())
            put("description", draft.description.trim())
            put("mode", postType.databaseValue)
            put("category", category.databaseValue)

            put(
                "physical",
                if (needsPhysical) {
                    JsonObject(
                        buildPhysicalDetailsRow("NEW", draft)
                            .filterKeys { it != "post_id" }
                    )
                } else JsonNull
            )

            put(
                "remote",
                if (needsRemote) {
                    JsonObject(
                        buildRemoteDetailsRow("NEW", draft)
                            .filterKeys { it != "post_id" }
                    )
                } else JsonNull
            )

            put("roles", buildJsonArray {
                draft.selectedRoles.forEach { role ->
                    add(buildJsonObject {
                        put("role_template_id", role.roleTemplateId)
                        put("capacity", role.capacity)
                        put(
                            "application_method",
                            role.applicationMethod?.databaseValue
                                ?: error("Choose an application method for ${role.roleTemplateId}.")
                        )
                        put(
                            "role_notes",
                            role.roleNotes.nullIfBlank()?.let(::JsonPrimitive) ?: JsonNull
                        )
                        put(
                            "individual_submission_requirement",
                            role.individualSubmissionRequirement.nullIfBlank()
                                ?.let(::JsonPrimitive) ?: JsonNull
                        )

                        put("skills", buildJsonArray {
                            role.practisedSkillIds.distinct().forEach { skillId ->
                                add(buildJsonObject {
                                    put("skill_id", skillId)
                                    role.requiredSkillExperience[skillId]?.let { experience ->
                                        put("required_experience", experience)
                                    } ?: put("required_experience", JsonNull)
                                })
                            }
                        })

                        put("responsibilities", buildJsonArray {
                            role.responsibilities
                                .mapNotNull { it.nullIfBlank() }
                                .forEach { add(JsonPrimitive(it)) }
                        })

                        put("screening_questions", buildJsonArray {
                            role.screeningQuestions
                                .mapNotNull { it.nullIfBlank() }
                                .forEach { add(JsonPrimitive(it)) }
                        })
                    })
                }
            })

            put("schedules", buildJsonArray {
                draft.scheduleItems.forEach { item ->
                    val data = buildSchedulePublishData(
                        postId = "NEW",
                        draft = draft,
                        item = item,
                        roleCatalogue = roleCatalogue
                    )
                    add(buildJsonObject {
                        data.row
                            .filterKeys { it != "post_id" }
                            .forEach { (key, value) -> put(key, value) }
                        put("target_role_template_ids", buildJsonArray {
                            data.targetRoleTemplateIds.forEach { add(JsonPrimitive(it)) }
                        })
                    })
                }
            })
        }
    }

    /**
     * Loads the existing post for edit needed by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadExistingPostForEdit
     *
     * Reconstructs the editor state for an existing owned post by reading the normalized post, Physical/Remote,
     * role, screening, schedule and participation-related tables required by edit policy.
     *
     * The method maps stored database ids/values back to CreatePostDraft and edit-policy input instead of
     * making the edit UI understand individual PostgREST rows.
     *
     * Participation/submission/history reads are used to decide which fields are safe to edit; they are not
     * copied into the editable draft as organisation-controlled values.
     *
     * Reads/maps Supabase table data from `volunteer_posts` (the parent Volunteer Post record, including owner,
     * mode, lifecycle status, category and publication metadata); `physical_details` (Physical-side
     * date/time/location/capacity data for a post); `remote_details` (Remote-side project dates, capacity and
     * submission configuration); `post_roles` (the selected role instances for a post, including capacity and
     * application method); `post_role_skills` (required/practised skill settings attached to a selected post
     * role).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     */
    override suspend fun loadExistingPostForEdit(postId: String): ExistingPostEditData {
        val organisationId = OrganisationSession.requireOrganisationId()
        // SUPABASE TABLE: volunteer_posts — the parent Volunteer Post record, including owner, mode, lifecycle status, category and publication metadata.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val postRow = supabase.from("volunteer_posts")
            .select(columns = Columns.raw(
                "post_id,organisation_id,title,description,mode,status,category,thumbnail_path,updated_at"
            )) {
                filter {
                    eq("post_id", postId)
                    eq("organisation_id", organisationId)
                }
            }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?: error("Volunteer post $postId does not belong to this organisation.")

        // SUPABASE TABLE: physical_details — Physical-side date/time/location/capacity data for a post.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val physicalRow = supabase.from("physical_details")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
            .firstOrNull()
        // SUPABASE TABLE: remote_details — Remote-side project dates, capacity and submission configuration.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val remoteRow = supabase.from("remote_details")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
            .firstOrNull()
        // SUPABASE TABLE: post_roles — the selected role instances for a post, including capacity and application method.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val roleRows = supabase.from("post_roles")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        // SUPABASE TABLE: post_role_skills — required/practised skill settings attached to a selected post role.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val skillRows = supabase.from("post_role_skills")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        // SUPABASE TABLE: post_role_responsibilities — organisation-defined responsibility text for each selected role.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val responsibilityRows = supabase.from("post_role_responsibilities")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        // SUPABASE TABLE: post_role_screening_questions — screening questions configured for REVIEW_APPLICANTS roles.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val questionRows = supabase.from("post_role_screening_questions")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        // SUPABASE TABLE: schedule_items — Physical activities and Remote milestones belonging to the post schedule.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val scheduleRows = supabase.from("schedule_items")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        // SUPABASE TABLE: schedule_item_roles — many-to-many links between schedule items and the roles affected by them.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val scheduleRoleRows = supabase.from("schedule_item_roles")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        // SUPABASE TABLE: role_participations — volunteer application/join/completion state for one post role.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val participationRows = supabase.from("role_participations")
            .select(columns = Columns.raw(
                "role_template_id,application_status,completion_status,joined_at"
            )) { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        // SUPABASE TABLE: role_participation_screening_answers — the volunteer's normalized answers to the role screening questions.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val screeningAnswerRows = supabase.from("role_participation_screening_answers")
            .select(columns = Columns.raw("role_template_id")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()
        // SUPABASE TABLE: attendance_records — per-volunteer Physical attendance/verified-time records.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val attendanceRows = supabase.from("attendance_records")
            .select(columns = Columns.raw("event_date,role_template_id")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()
        // SUPABASE TABLE: remote_submissions — Remote deliverable submission status, file/url and review metadata.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val submissionRows = supabase.from("remote_submissions")
            .select(columns = Columns.raw("role_template_id,submission_type")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()
        // SUPABASE TABLE: volunteer_evaluations — organisation evaluation/feedback results used after participation.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val evaluationRows = supabase.from("volunteer_evaluations")
            .select(columns = Columns.raw("role_template_id")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()
        // SUPABASE TABLE: volunteer_skill_experiences — verified skill-path evidence accumulated by volunteers.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val skillExperienceRows = supabase.from("volunteer_skill_experiences")
            .select(columns = Columns.raw("role_template_id")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()
        // SUPABASE TABLE: volunteer_certificates — certificate records made visible only through appropriate profile/certificate access rules.
        // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
        val certificateRows = supabase.from("volunteer_certificates")
            .select(columns = Columns.raw("role_template_id")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()

        val roleTemplateIds = roleRows.map { it.requiredText("role_template_id") }.distinct()
        val templateRows = if (roleTemplateIds.isEmpty()) emptyList() else {
            // SUPABASE TABLE: role_templates — the fixed role catalogue used when an organisation chooses volunteer roles.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
            supabase.from("role_templates")
                .select(columns = Columns.raw("role_template_id,role_mode")) {
                    filter { isIn("role_template_id", roleTemplateIds) }
                }.decodeList<JsonObject>()
        }
        val roleModes = templateRows.associate {
            it.requiredText("role_template_id") to VolunteerRoleMode.valueOf(
                it.requiredText("role_mode")
            )
        }

        val skillsByRole = skillRows.groupBy { it.requiredText("role_template_id") }
        val responsibilitiesByRole = responsibilityRows
            .groupBy { it.requiredText("role_template_id") }
        val questionsByRole = questionRows.groupBy { it.requiredText("role_template_id") }

        val selectedRoles = roleRows.map { row ->
            val roleId = row.requiredText("role_template_id")
            val roleSkills = skillsByRole[roleId].orEmpty()
            SelectedRoleDraft(
                roleTemplateId = roleId,
                capacity = row.requiredInt("capacity"),
                practisedSkillIds = roleSkills
                    .map { it.requiredText("skill_id") }
                    .distinct(),
                requiredSkillExperience = roleSkills.mapNotNull { skillRow ->
                    skillRow.optionalInt("required_experience")?.let { experience ->
                        skillRow.requiredText("skill_id") to experience
                    }
                }.toMap(),
                responsibilities = responsibilitiesByRole[roleId].orEmpty()
                    .sortedBy { it.requiredInt("responsibility_no") }
                    .map { it.requiredText("responsibility_text") },
                applicationMethod = RoleApplicationMethod.valueOf(
                    row.requiredText("application_method")
                ),
                screeningQuestions = questionsByRole[roleId].orEmpty()
                    .sortedBy { it.requiredInt("question_no") }
                    .map { it.requiredText("question_text") },
                roleNotes = row.optionalText("role_notes").orEmpty(),
                individualSubmissionRequirement = row
                    .optionalText("individual_submission_requirement").orEmpty(),
                isConfigured = true
            )
        }

        val scheduleTargets = scheduleRoleRows.groupBy {
            it.requiredText("schedule_item_id")
        }
        val schedules = scheduleRows.map { row ->
            val scheduleId = row.requiredText("schedule_item_id")
            val relations = scheduleTargets[scheduleId].orEmpty()
            val targetRoleIds = relations.map { it.requiredText("role_template_id") }
            val scheduleType = ScheduleType.valueOf(row.requiredText("schedule_type"))

            ScheduleItemDraft(
                draftId = scheduleId,
                scheduleType = scheduleType,
                scheduleDateMillis = parseSqlDate(row.requiredText("schedule_date")),
                title = row.requiredText("title"),
                startTimeMinutes = row.optionalText("start_time")?.let(::parseSqlTime),
                endTimeMinutes = row.optionalText("end_time")?.let(::parseSqlTime),
                location = row.optionalText("location").orEmpty(),
                appliesToAllRoles = false,
                targetRoleTemplateIds = targetRoleIds,
                notes = row.optionalText("notes").orEmpty()
            )
        }

        val postType = VolunteerPostType.valueOf(postRow.requiredText("mode"))
        val physicalLocation = physicalRow?.let { row ->
            LocationSuggestion(
                placeId = "existing:$postId",
                name = row.requiredText("location_name"),
                address = row.optionalText("location_address").orEmpty(),
                city = null,
                state = row.optionalText("state_region"),
                country = row.optionalText("country"),
                latitude = row.optionalDouble("latitude") ?: 0.0,
                longitude = row.optionalDouble("longitude") ?: 0.0
            )
        }

        val draft = CreatePostDraft(
            postType = postType,
            category = postRow.optionalText("category")?.let {
                com.example.volunteerlink.organisation.create.model.VolunteerPostCategory.valueOf(it)
            },
            title = postRow.requiredText("title"),
            description = postRow.requiredText("description"),
            thumbnailUri = null,
            isMultiDayPhysicalEvent = physicalRow?.let {
                it.requiredText("start_date") != it.requiredText("end_date")
            } ?: false,
            physicalStartDateMillis = physicalRow?.requiredText("start_date")?.let(::parseSqlDate),
            physicalEndDateMillis = physicalRow?.requiredText("end_date")?.let(::parseSqlDate),
            physicalStartTimeMinutes = physicalRow?.requiredText("start_time")?.let(::parseSqlTime),
            physicalEndTimeMinutes = physicalRow?.requiredText("end_time")?.let(::parseSqlTime),
            physicalLocationQuery = physicalLocation?.displayName.orEmpty(),
            physicalLocation = physicalLocation,
            meetingPoint = physicalRow?.optionalText("meeting_point").orEmpty(),
            physicalVolunteerCapacity = if (postType == VolunteerPostType.PHYSICAL) {
                physicalRow?.requiredInt("volunteer_capacity")
            } else null,
            physicalTimeZoneId = physicalRow?.optionalText("time_zone"),
            remoteStartDateMillis = remoteRow?.requiredText("start_date")?.let(::parseSqlDate),
            remoteDueDateMillis = remoteRow?.requiredText("end_date")?.let(::parseSqlDate),
            remoteVolunteerCapacity = if (postType == VolunteerPostType.REMOTE) {
                remoteRow?.requiredInt("volunteer_capacity")
            } else null,
            remoteSubmissionMode = remoteRow?.requiredText("submission_mode")
                ?.let(RemoteSubmissionMode::valueOf),
            sharedDeliverable = remoteRow?.optionalText("shared_deliverable").orEmpty(),
            hybridPhysicalVolunteerCapacity = if (postType == VolunteerPostType.HYBRID) {
                physicalRow?.requiredInt("volunteer_capacity")
            } else null,
            hybridRemoteVolunteerCapacity = if (postType == VolunteerPostType.HYBRID) {
                remoteRow?.requiredInt("volunteer_capacity")
            } else null,
            selectedRoles = selectedRoles,
            sharedSubmissionResponsibleRoleTemplateId = remoteRow
                ?.optionalText("responsible_role_template_id"),
            scheduleItems = schedules
        )

        val submissionRoleIds = submissionRows.mapNotNull {
            it.optionalText("role_template_id")
        }.toSet()
        // A valid SHARED_TEAM post writes SHARED submissions. The mode fallback
        // is deliberately conservative for older/inconsistent rows so Manage Edit
        // never offers a global deadline edit after shared work already exists.
        val hasSharedRemoteSubmission = submissionRows.any { row ->
            row.optionalText("submission_type").equals("SHARED", ignoreCase = true)
        } || (
            draft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM &&
                submissionRows.isNotEmpty()
            )
        val completedHistoryRoleIds = buildSet {
            addAll(attendanceRows.map { it.requiredText("role_template_id") })
            addAll(submissionRoleIds)
            addAll(evaluationRows.map { it.requiredText("role_template_id") })
            addAll(skillExperienceRows.map { it.requiredText("role_template_id") })
            addAll(certificateRows.map { it.requiredText("role_template_id") })
            addAll(participationRows.filter {
                !it.requiredText("completion_status").equals("IN_PROGRESS", true)
            }.map { it.requiredText("role_template_id") })
        }

        return ExistingPostEditData(
            postId = postId,
            databaseStatus = postRow.requiredText("status"),
            originalUpdatedAt = postRow.requiredText("updated_at"),
            existingThumbnailPath = postRow.optionalText("thumbnail_path"),
            draft = draft,
            policyInput = PostEditPolicyInput(
                postStatus = postRow.requiredText("status"),
                physicalStartDateMillis = draft.physicalStartDateMillis,
                physicalEndDateMillis = draft.physicalEndDateMillis,
                remoteStartDateMillis = draft.remoteStartDateMillis,
                remoteEndDateMillis = draft.remoteDueDateMillis,
                roles = selectedRoles.map { selected ->
                    PostEditRoleInput(
                        roleTemplateId = selected.roleTemplateId,
                        roleMode = roleModes[selected.roleTemplateId]
                            ?: error("Missing role mode for ${selected.roleTemplateId}."),
                        hasConfiguredScreeningQuestions = selected.screeningQuestions.isNotEmpty()
                    )
                },
                participations = participationRows.map { row ->
                    PostEditParticipationInput(
                        roleTemplateId = row.requiredText("role_template_id"),
                        applicationStatus = row.requiredText("application_status"),
                        joinedAt = row.optionalText("joined_at")
                    )
                },
                schedules = schedules.map { item ->
                    PostEditScheduleInput(
                        scheduleItemId = item.draftId,
                        scheduleType = item.scheduleType,
                        scheduleDateMillis = item.scheduleDateMillis
                            ?: error("Existing schedule has no date.")
                    )
                },
                attendanceDatesMillis = attendanceRows.map {
                    parseSqlDate(it.requiredText("event_date"))
                },
                remoteSubmissionRoleIds = submissionRoleIds,
                hasAnyRemoteSubmission = submissionRows.isNotEmpty(),
                hasSharedRemoteSubmission = hasSharedRemoteSubmission,
                completedHistoryRoleIds = completedHistoryRoleIds,
                screeningAnswerRoleIds = screeningAnswerRows
                    .map { it.requiredText("role_template_id") }
                    .toSet()
            )
        )
    }

    /**
     * Updates the existing post used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — updateExistingPost
     *
     * Sends the edited existing-post payload to the atomic organisation_update_volunteer_post_editor RPC.
     *
     * The RPC is used instead of a sequence of client-side UPDATE/DELETE/INSERT calls because an existing post
     * can already have applications, attendance and submissions that must remain relationally consistent.
     *
     * Thumbnail replacement is coordinated separately with Supabase Storage, and the database path is changed
     * only after the upload/save sequence succeeds.
     *
     * Supabase RPC `organisation_update_volunteer_post_editor`: Applies Existing Post Edit as one ownership-
     * checked database transaction so related post/role/schedule changes cannot partially succeed.
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Uses Supabase Storage for binary/file content while database rows keep only the controlled storage path
     * and metadata.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    override suspend fun updateExistingPost(
        latest: ExistingPostEditData,
        editedDraft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): SavedPostResult {
        val postId = latest.postId
        val organisation = OrganisationSession.requireContext()
        val postType = editedDraft.postType ?: error("Post Type is missing.")
        editedDraft.category ?: error("Category is missing.")

        var thumbnailPath = latest.existingThumbnailPath
        var newlyUploadedThumbnailPath: String? = null

        try {
            // Storage is outside PostgreSQL, so upload the new object first and
            // remove it again if the atomic database RPC rejects/rolls back.
            if (thumbnail != null) {
                val safeExtension = thumbnail.fileExtension.lowercase()
                    .filter { it.isLetterOrDigit() }
                    .ifBlank { "jpg" }
                val storagePath = "$STORAGE_PREFIX/${organisation.organisationId}/$postId/" +
                    "${UUID.randomUUID()}.$safeExtension"

                supabase.storage.from(THUMBNAIL_BUCKET).upload(
                    path = storagePath,
                    data = thumbnail.bytes
                ) {
                    upsert = false
                    contentType = ContentType.parse(thumbnail.mimeType)
                }

                newlyUploadedThumbnailPath = storagePath
                thumbnailPath = storagePath
            }

            val payload = buildExistingEditPayload(
                latest = latest,
                editedDraft = editedDraft,
                roleCatalogue = roleCatalogue,
                thumbnailPath = thumbnailPath,
                postType = postType
            )

            // One PostgreSQL function call = one transaction for parent, side
            // details, roles, child role data and schedules. The function locks
            // volunteer_posts first and verifies originalUpdatedAt before any
            // child row is touched.
            // ------------------------------------------------------------------------
            // SUPABASE RPC: organisation_update_volunteer_post_editor
            // Applies Existing Post Edit as one ownership-checked database transaction so related
            // post/role/schedule changes cannot partially succeed.
            // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
            // consistency checks belong on the server for this operation.
            // ------------------------------------------------------------------------
            supabase.postgrest.rpc(
                function = "organisation_update_volunteer_post_editor",
                parameters = buildJsonObject {
                    put("p_payload", payload)
                }
            )

            // Only after the DB transaction succeeds may the previous image be
            // cleaned up. Failure to delete the old object does not corrupt DB
            // state, so this remains deliberately best-effort.
            if (
                newlyUploadedThumbnailPath != null &&
                latest.existingThumbnailPath != null &&
                latest.existingThumbnailPath != newlyUploadedThumbnailPath
            ) {
                runCatching {
                    supabase.storage.from(THUMBNAIL_BUCKET)
                        .delete(latest.existingThumbnailPath)
                }
            }

            return SavedPostResult(
                postId = postId,
                thumbnailPath = thumbnailPath
            )
        } catch (exception: Exception) {
            newlyUploadedThumbnailPath?.let { uploadedPath ->
                runCatching {
                    supabase.storage.from(THUMBNAIL_BUCKET).delete(uploadedPath)
                }
            }
            throw exception
        }
    }

    /**
     * Builds the existing edit payload used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — buildExistingEditPayload
     *
     * Performs the repository/data-layer operation for build existing edit payload.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun buildExistingEditPayload(
        latest: ExistingPostEditData,
        editedDraft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnailPath: String?,
        postType: VolunteerPostType
    ): JsonObject {
        val postId = latest.postId
        val category = editedDraft.category ?: error("Category is missing.")
        val originalRoles = latest.draft.selectedRoles.associateBy { it.roleTemplateId }
        val originalSchedules = latest.draft.scheduleItems.associateBy { it.draftId }

        val needsPhysical = postType == VolunteerPostType.PHYSICAL ||
            postType == VolunteerPostType.HYBRID
        val needsRemote = postType == VolunteerPostType.REMOTE ||
            postType == VolunteerPostType.HYBRID

        return buildJsonObject {
            put("post_id", postId)
            put("expected_updated_at", latest.originalUpdatedAt)
            put("title", editedDraft.title.trim())
            put("description", editedDraft.description.trim())
            put("category", category.databaseValue)
            put("mode", postType.databaseValue)
            put("thumbnail_path", thumbnailPath?.let(::JsonPrimitive) ?: JsonNull)

            put(
                "physical",
                if (needsPhysical) buildPhysicalDetailsRow(postId, editedDraft) else JsonNull
            )
            put(
                "remote",
                if (needsRemote) buildRemoteDetailsRow(postId, editedDraft) else JsonNull
            )

            put("roles", buildJsonArray {
                editedDraft.selectedRoles.forEach { role ->
                    val original = originalRoles[role.roleTemplateId]
                    val skillsChanged = original == null ||
                        original.practisedSkillIds != role.practisedSkillIds ||
                        original.requiredSkillExperience != role.requiredSkillExperience
                    val responsibilitiesChanged = original == null ||
                        original.responsibilities != role.responsibilities
                    val questionsChanged = original == null ||
                        original.screeningQuestions != role.screeningQuestions

                    add(buildJsonObject {
                        put("role_template_id", role.roleTemplateId)
                        put("capacity", role.capacity)
                        put(
                            "application_method",
                            role.applicationMethod?.databaseValue
                                ?: error("Choose an application method for ${role.roleTemplateId}.")
                        )
                        put(
                            "role_notes",
                            role.roleNotes.nullIfBlank()?.let(::JsonPrimitive) ?: JsonNull
                        )
                        put(
                            "individual_submission_requirement",
                            role.individualSubmissionRequirement.nullIfBlank()
                                ?.let(::JsonPrimitive) ?: JsonNull
                        )
                        put("replace_skills", skillsChanged)
                        put("replace_responsibilities", responsibilitiesChanged)
                        put("replace_questions", questionsChanged)

                        put("skills", buildJsonArray {
                            role.practisedSkillIds.distinct().forEach { skillId ->
                                add(buildJsonObject {
                                    put("skill_id", skillId)
                                    role.requiredSkillExperience[skillId]?.let { experience ->
                                        put("required_experience", experience)
                                    } ?: put("required_experience", JsonNull)
                                })
                            }
                        })

                        put("responsibilities", buildJsonArray {
                            role.responsibilities
                                .mapNotNull { it.nullIfBlank() }
                                .forEach { add(JsonPrimitive(it)) }
                        })

                        put("screening_questions", buildJsonArray {
                            role.screeningQuestions
                                .mapNotNull { it.nullIfBlank() }
                                .forEach { add(JsonPrimitive(it)) }
                        })
                    })
                }
            })

            put("schedules", buildJsonArray {
                editedDraft.scheduleItems.forEach { item ->
                    val original = originalSchedules[item.draftId]
                    val data = buildSchedulePublishData(
                        postId = postId,
                        draft = editedDraft,
                        item = item,
                        roleCatalogue = roleCatalogue
                    )

                    add(buildJsonObject {
                        data.row.forEach { (key, value) -> put(key, value) }
                        put(
                            "schedule_item_id",
                            if (original != null) JsonPrimitive(item.draftId) else JsonNull
                        )
                        put("changed", original == null || original != item)
                        put("target_role_template_ids", buildJsonArray {
                            data.targetRoleTemplateIds.forEach { add(JsonPrimitive(it)) }
                        })
                    })
                }
            })
        }
    }

    /**
     * Loads the role catalogue needed by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadRoleCatalogue
     *
     * Performs the repository/data-layer operation for load role catalogue.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    override suspend fun loadRoleCatalogue(): List<CreateRoleTemplate> {
        // Step 2 now depends on four normalized catalogue tables.
        // Keep the table name in any thrown error so test builds tell us
        // exactly which Supabase read failed instead of showing only a
        // generic "connection" message.
        val pathRows = loadCatalogueTable("skill_paths")
        val skillRows = loadCatalogueTable("skills")
        val roleRows = loadCatalogueTable("role_templates")
        val roleSkillRows = loadCatalogueTable("role_template_skills")

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

        val skillLinksByRoleId = roleSkillRows.groupBy { row ->
            row.requiredText("role_template_id")
        }

        return roleRows
            .map { row ->
                val roleTemplateId = row.requiredText("role_template_id")
                val skillPathId = row.requiredText("skill_path_id")
                val skillLinks = skillLinksByRoleId[roleTemplateId].orEmpty()

                val practisedSkills = skillLinks
                    .map { link ->
                        val skillId = link.requiredText("skill_id")
                        skillsById[skillId]
                            ?: error("Missing Skill: $skillId")
                    }
                    .distinctBy { skill -> skill.skillId }
                    .sortedBy { skill -> skill.skillId }

                val recommendedSkills = skillLinks
                    .filter { link ->
                        link.optionalBoolean("is_recommended") == true
                    }
                    .map { link ->
                        val skillId = link.requiredText("skill_id")
                        skillsById[skillId]
                            ?: error("Missing Skill: $skillId")
                    }
                    .distinctBy { skill -> skill.skillId }
                    .sortedBy { skill -> skill.skillId }

                CreateRoleTemplate(
                    roleTemplateId = roleTemplateId,
                    roleName = row.requiredText("role_name"),
                    roleArea = row.requiredText("role_area"),
                    roleMode = VolunteerRoleMode.valueOf(
                        row.requiredText("role_mode")
                    ),
                    skillPathId = skillPathId,
                    skillPathName = pathNamesById[skillPathId]
                        ?: error("Missing Skill Path: $skillPathId"),
                    description = row.optionalText("description").orEmpty(),
                    skillsPractised = practisedSkills,
                    recommendedSkills = recommendedSkills,
                    defaultLevel = VolunteerRoleLevel.valueOf(
                        row.requiredText("default_level")
                    )
                )
            }
            .sortedBy { it.roleTemplateId }
    }

    /**
     * Renders the build physical details row row used in the organisation Create/Edit Post flow.
     * It receives state and callbacks from its caller so presentation code stays separate from database operations.
     */
    /**
     * DETAILED BEHAVIOUR — buildPhysicalDetailsRow
     *
     * Performs the repository/data-layer operation for build physical details row.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun buildPhysicalDetailsRow(
        postId: String,
        draft: CreatePostDraft
    ): JsonObject {
        val location = draft.physicalLocation
            ?: error("Choose a Physical event location before saving.")
        val capacity = draft.requiredPhysicalVolunteerTotal
            ?: error("Physical volunteer capacity is missing.")

        return buildJsonObject {
            put("post_id", postId)
            put(
                "start_date",
                sqlDate(
                    draft.physicalStartDateMillis
                        ?: error("Physical start date is missing.")
                )
            )
            put(
                "end_date",
                sqlDate(
                    draft.physicalEndDateMillis
                        ?: error("Physical end date is missing.")
                )
            )
            put(
                "start_time",
                sqlTime(
                    draft.physicalStartTimeMinutes
                        ?: error("Physical start time is missing.")
                )
            )
            put(
                "end_time",
                sqlTime(
                    draft.physicalEndTimeMinutes
                        ?: error("Physical end time is missing.")
                )
            )
            put("location_name", location.displayName)
            location.address.nullIfBlank()?.let { address ->
                put("location_address", address)
            }
            location.state.nullIfBlank()?.let { state ->
                put("state_region", state)
            }
            location.country.nullIfBlank()?.let { country ->
                put("country", country)
            }
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            draft.meetingPoint.nullIfBlank()?.let { meetingPoint ->
                put("meeting_point", meetingPoint)
            }
            put("volunteer_capacity", capacity)
            draft.physicalTimeZoneId.nullIfBlank()?.let { timeZone ->
                put("time_zone", timeZone)
            }
        }
    }

    /**
     * Renders the build remote details row row used in the organisation Create/Edit Post flow.
     * It receives state and callbacks from its caller so presentation code stays separate from database operations.
     */
    /**
     * DETAILED BEHAVIOUR — buildRemoteDetailsRow
     *
     * Performs the repository/data-layer operation for build remote details row.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun buildRemoteDetailsRow(
        postId: String,
        draft: CreatePostDraft
    ): JsonObject {
        val mode = draft.remoteSubmissionMode
            ?: error("Choose a Remote submission mode before saving.")
        val capacity = draft.requiredRemoteVolunteerTotal
            ?: error("Remote volunteer capacity is missing.")

        return buildJsonObject {
            put("post_id", postId)
            put(
                "start_date",
                sqlDate(
                    draft.remoteStartDateMillis
                        ?: error("Remote start date is missing.")
                )
            )
            put(
                "end_date",
                sqlDate(
                    draft.remoteDueDateMillis
                        ?: error("Remote due date is missing.")
                )
            )
            put("volunteer_capacity", capacity)
            put("submission_mode", mode.databaseValue)

            if (mode == RemoteSubmissionMode.SHARED_TEAM) {
                draft.sharedDeliverable.nullIfBlank()?.let { deliverable ->
                    put("shared_deliverable", deliverable)
                }
                put(
                    "responsible_role_template_id",
                    draft.sharedSubmissionResponsibleRoleTemplateId
                        ?: error("Choose the responsible Remote role before saving.")
                )
            }
        }
    }

    /**
     * Builds the schedule publish data used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — buildSchedulePublishData
     *
     * Performs the repository/data-layer operation for build schedule publish data.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Runs the shared CreatePostValidator so navigation/save behaviour uses the same validation rules as the
     * rest of the wizard.
     */
    private fun buildSchedulePublishData(
        postId: String,
        draft: CreatePostDraft,
        item: ScheduleItemDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): SchedulePublishData {
        val applicableRoleIds = CreatePostValidator.applicableScheduleRoleIds(
            draft = draft,
            scheduleType = item.scheduleType,
            roleCatalogue = roleCatalogue
        )

        val targetRoleIds = if (item.appliesToAllRoles) {
            applicableRoleIds
        } else {
            item.targetRoleTemplateIds
                .filter { roleId -> roleId in applicableRoleIds }
                .distinct()
        }

        if (targetRoleIds.isEmpty()) {
            error("A schedule item has no valid target roles.")
        }

        val row = buildJsonObject {
            put("post_id", postId)
            put("schedule_type", item.scheduleType.databaseValue)
            put(
                "schedule_date",
                sqlDate(
                    item.scheduleDateMillis
                        ?: error("A schedule item is missing its date.")
                )
            )
            put("title", item.title.trim())

            item.startTimeMinutes?.let { minutes ->
                put("start_time", sqlTime(minutes))
            }
            item.endTimeMinutes?.let { minutes ->
                put("end_time", sqlTime(minutes))
            }

            if (item.scheduleType == ScheduleType.PHYSICAL) {
                item.location.nullIfBlank()?.let { location ->
                    put("location", location)
                }
            }

            item.notes.nullIfBlank()?.let { notes ->
                put("notes", notes)
            }


        }

        return SchedulePublishData(
            row = row,
            targetRoleTemplateIds = targetRoleIds
        )
    }

    /**
     * Best-effort cleanup when Save Draft or Publish fails midway.
     * Explicit child cleanup is kept even where a test FK currently cascades,
     * so this multi-table client-side save stays understandable and debuggable.
     */
    /**
     * DETAILED BEHAVIOUR — sqlDate
     *
     * Performs the repository/data-layer operation for sql date.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun sqlDate(dateMillis: Long): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date(dateMillis))
    }

    /**
     * Returns the sql time value required by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — sqlTime
     *
     * Performs the repository/data-layer operation for sql time.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun sqlTime(minutesAfterMidnight: Int): String {
        require(minutesAfterMidnight in 0..1439) {
            "Time must be between 00:00 and 23:59."
        }

        val hour = minutesAfterMidnight / 60
        val minute = minutesAfterMidnight % 60
        return String.format(Locale.US, "%02d:%02d:00", hour, minute)
    }

    /**
     * Derives the string value used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — nullIfBlank
     *
     * Performs the repository/data-layer operation for null if blank.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun String?.nullIfBlank(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Loads the catalogue table needed by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadCatalogueTable
     *
     * Performs the repository/data-layer operation for load catalogue table.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private suspend fun loadCatalogueTable(
        tableName: String
    ): List<JsonObject> {
        return try {
            supabase
                .from(tableName)
                .select()
                .decodeList<JsonObject>()
        } catch (exception: Exception) {
            throw IllegalStateException(
                "Failed to load $tableName: " +
                        (exception.message ?: "Unknown Supabase error."),
                exception
            )
        }
    }

    /**
     * Derives the json object value used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — requiredText
     *
     * Performs the repository/data-layer operation for required text.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun JsonObject.requiredText(key: String): String {
        return optionalText(key)
            ?: error("Missing '$key' in Create Post catalogue data.")
    }

    /**
     * Derives the json object value used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — optionalText
     *
     * Performs the repository/data-layer operation for optional text.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun JsonObject.optionalText(key: String): String? {
        return this[key]
            ?.jsonPrimitive
            ?.contentOrNull
    }

    /**
     * Derives the json object value used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — optionalBoolean
     *
     * Performs the repository/data-layer operation for optional boolean.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun JsonObject.optionalBoolean(key: String): Boolean? {
        return optionalText(key)?.toBooleanStrictOrNull()
    }

    /**
     * Derives the json object value used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — optionalInt
     *
     * Performs the repository/data-layer operation for optional int.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private fun JsonObject.optionalInt(key: String): Int? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        return runCatching { element.jsonPrimitive.intOrNull }.getOrNull()
    }

    /**
     * Derives the json object value used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — requiredInt
     *
     * Performs the repository/data-layer operation for required int.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun JsonObject.requiredInt(key: String): Int {
        return optionalInt(key) ?: error("Missing required integer '$key'.")
    }

    /**
     * Derives the json object value used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — optionalDouble
     *
     * Performs the repository/data-layer operation for optional double.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private fun JsonObject.optionalDouble(key: String): Double? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        return runCatching { element.jsonPrimitive.doubleOrNull }.getOrNull()
    }

    /**
     * Derives the json object value used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — requiredDouble
     *
     * Performs the repository/data-layer operation for required double.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun JsonObject.requiredDouble(key: String): Double {
        return optionalDouble(key) ?: error("Missing required number '$key'.")
    }

    /**
     * Parses the sql date used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — parseSqlDate
     *
     * Performs the repository/data-layer operation for parse sql date.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun parseSqlDate(value: String): Long {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }.parse(value)?.time ?: error("Invalid database date: $value")
    }

    /**
     * Parses the sql time used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — parseSqlTime
     *
     * Performs the repository/data-layer operation for parse sql time.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun parseSqlTime(value: String): Int {
        val parts = value.take(5).split(":")
        if (parts.size != 2) error("Invalid database time: $value")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    /**
     * Holds the values represented by schedule publish data as one strongly typed model.
     * It keeps backend-facing work behind the Create/Edit Post repository boundary.
     */
    /**
     * DETAILED DECLARATION — SchedulePublishData
     *
     * Domain/UI type for Schedule Publish Data used by the Organisation module.
     *
     * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-
     * typed maps.
     */
    private data class SchedulePublishData(
        val row: JsonObject,
        val targetRoleTemplateIds: List<String>
    )

    private companion object {
        const val THUMBNAIL_BUCKET = "post-thumbnails"
        const val STORAGE_PREFIX = "v1_erd_test"
    }
}
