package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.data.location.LocationSuggestion
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
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Supabase implementation used by the Create Post wizard.
 *
 * New-post Save Draft / Publish remain client-side multi-table operations for this
 * university project. Existing-post Edit uses one PostgreSQL RPC transaction because
 * application/activity history makes partial multi-table updates unsafe.
 */
class SupabaseCreatePostRepository : CreatePostRepository {

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

    override suspend fun publishPost(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): SavedPostResult {
        return savePost(
            draft = draft,
            roleCatalogue = roleCatalogue,
            thumbnail = thumbnail,
            publishAfterSave = true
        )
    }

    private suspend fun savePost(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?,
        publishAfterSave: Boolean
    ): SavedPostResult {
        val postType = draft.postType
            ?: error("Choose a post type before saving.")
        val category = draft.category
            ?: error("Choose a category before saving.")
        val saveTimestamp = currentUtcTimestamp()
        val posts = supabase.from("volunteer_posts")
        val bucket = supabase.storage.from(THUMBNAIL_BUCKET)

        // The parent starts as DRAFT for both actions. Save Draft keeps this
        // state; Publish switches it only after every child/thumbnail succeeds.
        val parentRow = buildJsonObject {
            put("organisation_id", TEST_ORGANISATION_ID)
            put("title", draft.title.trim())
            put("description", draft.description.trim())
            put("mode", postType.databaseValue)
            put("status", "DRAFT")
            put("created_at", saveTimestamp)
            put("updated_at", saveTimestamp)
            put("category", category.databaseValue)
        }

        val insertedPost = posts
            .insert(parentRow) {
                select()
            }
            .decodeSingle<JsonObject>()

        val postId = insertedPost.requiredText("post_id")
        var uploadedThumbnailPath: String? = null

        try {
            // post_roles now stores only role-level columns. Skills,
            // responsibilities and screening questions live in normalized
            // child tables in v1_erd_test.
            val roleRows = draft.selectedRoles.map { selectedRole ->
                buildJsonObject {
                    put("post_id", postId)
                    put("role_template_id", selectedRole.roleTemplateId)
                    put("capacity", selectedRole.capacity)
                    put(
                        "application_method",
                        selectedRole.applicationMethod?.databaseValue
                            ?: error("One selected role has no application method.")
                    )

                    selectedRole.roleNotes.nullIfBlank()?.let { roleNotes ->
                        put("role_notes", roleNotes)
                    }
                    selectedRole.individualSubmissionRequirement
                        .nullIfBlank()
                        ?.let { requirement ->
                            put("individual_submission_requirement", requirement)
                        }
                }
            }

            if (roleRows.isNotEmpty()) {
                supabase.from("post_roles").insert(roleRows)
            }

            insertPostRoleDetails(
                postId = postId,
                draft = draft
            )

            if (
                postType == VolunteerPostType.PHYSICAL ||
                postType == VolunteerPostType.HYBRID
            ) {
                supabase.from("physical_details").insert(
                    buildPhysicalDetailsRow(
                        postId = postId,
                        draft = draft
                    )
                )
            }

            if (
                postType == VolunteerPostType.REMOTE ||
                postType == VolunteerPostType.HYBRID
            ) {
                supabase.from("remote_details").insert(
                    buildRemoteDetailsRow(
                        postId = postId,
                        draft = draft
                    )
                )
            }

            // schedule_items no longer contains a JSON array of ROLE IDs.
            // Insert each schedule item, receive its SCH... ID, then insert
            // its role relationships into schedule_item_roles.
            for (item in draft.scheduleItems) {
                val scheduleData = buildSchedulePublishData(
                    postId = postId,
                    draft = draft,
                    item = item,
                    roleCatalogue = roleCatalogue
                )

                val insertedSchedule = supabase
                    .from("schedule_items")
                    .insert(scheduleData.row) {
                        select()
                    }
                    .decodeSingle<JsonObject>()

                val scheduleItemId = insertedSchedule
                    .requiredText("schedule_item_id")

                val targetRows = scheduleData.targetRoleTemplateIds.map { roleId ->
                    buildJsonObject {
                        put("schedule_item_id", scheduleItemId)
                        put("post_id", postId)
                        put("role_template_id", roleId)
                    }
                }

                if (targetRows.isNotEmpty()) {
                    supabase.from("schedule_item_roles").insert(targetRows)
                }
            }

            if (thumbnail != null) {
                val safeExtension = thumbnail.fileExtension
                    .lowercase()
                    .filter { it.isLetterOrDigit() }
                    .ifBlank { "jpg" }

                val storagePath =
                    "$TEST_STORAGE_PREFIX/$TEST_ORGANISATION_ID/$postId/" +
                            "${UUID.randomUUID()}.$safeExtension"

                bucket.upload(
                    path = storagePath,
                    data = thumbnail.bytes
                ) {
                    upsert = false
                    contentType = ContentType.parse(thumbnail.mimeType)
                }

                uploadedThumbnailPath = storagePath

                posts.update(
                    {
                        set("thumbnail_path", storagePath)
                    }
                ) {
                    filter {
                        eq("post_id", postId)
                    }
                }
            }

            // Publishing is deliberately the final database action.
            // Save Draft stops before this update, so status stays DRAFT and
            // published_at stays SQL NULL.
            if (publishAfterSave) {
                posts.update(
                    {
                        set("status", "PUBLISHED")
                        set("published_at", saveTimestamp)
                        set("updated_at", saveTimestamp)
                    }
                ) {
                    filter {
                        eq("post_id", postId)
                    }
                }
            }

            return SavedPostResult(
                postId = postId,
                thumbnailPath = uploadedThumbnailPath
            )
        } catch (e: Exception) {
            cleanupFailedSave(
                postId = postId,
                thumbnailPath = uploadedThumbnailPath
            )
            throw e
        }
    }

    override suspend fun loadExistingPostForEdit(postId: String): ExistingPostEditData {
        val postRow = supabase.from("volunteer_posts")
            .select(columns = Columns.raw(
                "post_id,organisation_id,title,description,mode,status,category,thumbnail_path,updated_at"
            )) {
                filter {
                    eq("post_id", postId)
                    eq("organisation_id", TEST_ORGANISATION_ID)
                }
            }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?: error("Volunteer post $postId does not belong to this organisation.")

        val physicalRow = supabase.from("physical_details")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
            .firstOrNull()
        val remoteRow = supabase.from("remote_details")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
            .firstOrNull()
        val roleRows = supabase.from("post_roles")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        val skillRows = supabase.from("post_role_skills")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        val responsibilityRows = supabase.from("post_role_responsibilities")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        val questionRows = supabase.from("post_role_screening_questions")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        val scheduleRows = supabase.from("schedule_items")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        val scheduleRoleRows = supabase.from("schedule_item_roles")
            .select { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        val participationRows = supabase.from("role_participations")
            .select(columns = Columns.raw(
                "role_template_id,application_status,completion_status,joined_at"
            )) { filter { eq("post_id", postId) } }
            .decodeList<JsonObject>()
        val screeningAnswerRows = supabase.from("participation_screening_answers")
            .select(columns = Columns.raw("role_template_id")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()
        val attendanceRows = supabase.from("attendance_records")
            .select(columns = Columns.raw("event_date,role_template_id")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()
        val submissionRows = supabase.from("remote_submissions")
            .select(columns = Columns.raw("role_template_id,submission_type")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()
        val evaluationRows = supabase.from("volunteer_evaluations")
            .select(columns = Columns.raw("role_template_id")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()
        val skillExperienceRows = supabase.from("volunteer_skill_experiences")
            .select(columns = Columns.raw("role_template_id")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()
        val certificateRows = supabase.from("volunteer_certificates")
            .select(columns = Columns.raw("role_template_id")) {
                filter { eq("post_id", postId) }
            }.decodeList<JsonObject>()

        val roleTemplateIds = roleRows.map { it.requiredText("role_template_id") }.distinct()
        val templateRows = if (roleTemplateIds.isEmpty()) emptyList() else {
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

    override suspend fun updateExistingPost(
        latest: ExistingPostEditData,
        editedDraft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): SavedPostResult {
        val postId = latest.postId
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
                val storagePath = "$TEST_STORAGE_PREFIX/$TEST_ORGANISATION_ID/$postId/" +
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
            supabase.postgrest.rpc(
                function = "update_volunteer_post_editor",
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
     * Inserts the normalized Step 3 child tables for every selected role.
     *
     * post_role_skills:
     * - one row = one practised skill
     * - required_experience NULL = practised only
     * - required_experience 1..5 = practised and required
     */
    private suspend fun insertPostRoleDetails(
        postId: String,
        draft: CreatePostDraft
    ) {
        val skillRows = draft.selectedRoles.flatMap { selectedRole ->
            selectedRole.practisedSkillIds
                .distinct()
                .map { skillId ->
                    buildJsonObject {
                        put("post_id", postId)
                        put("role_template_id", selectedRole.roleTemplateId)
                        put("skill_id", skillId)

                        selectedRole.requiredSkillExperience[skillId]
                            ?.let { requiredExperience ->
                                put("required_experience", requiredExperience)
                            }
                    }
                }
        }

        if (skillRows.isNotEmpty()) {
            supabase.from("post_role_skills").insert(skillRows)
        }

        val responsibilityRows = draft.selectedRoles.flatMap { selectedRole ->
            selectedRole.responsibilities
                .mapNotNull { responsibility -> responsibility.nullIfBlank() }
                .mapIndexed { index, responsibility ->
                    buildJsonObject {
                        put("post_id", postId)
                        put("role_template_id", selectedRole.roleTemplateId)
                        put("responsibility_no", index + 1)
                        put("responsibility_text", responsibility)
                    }
                }
        }

        if (responsibilityRows.isNotEmpty()) {
            supabase.from("post_role_responsibilities")
                .insert(responsibilityRows)
        }

        val screeningQuestionRows = draft.selectedRoles.flatMap { selectedRole ->
            selectedRole.screeningQuestions
                .mapNotNull { question -> question.nullIfBlank() }
                .mapIndexed { index, question ->
                    buildJsonObject {
                        put("post_id", postId)
                        put("role_template_id", selectedRole.roleTemplateId)
                        put("question_no", index + 1)
                        put("question_text", question)
                    }
                }
        }

        if (screeningQuestionRows.isNotEmpty()) {
            supabase.from("post_role_screening_questions")
                .insert(screeningQuestionRows)
        }
    }

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
    private suspend fun cleanupFailedSave(
        postId: String,
        thumbnailPath: String?
    ) {
        runCatching {
            supabase.from("schedule_item_roles").delete {
                filter { eq("post_id", postId) }
            }
        }
        runCatching {
            supabase.from("schedule_items").delete {
                filter { eq("post_id", postId) }
            }
        }
        runCatching {
            supabase.from("remote_details").delete {
                filter { eq("post_id", postId) }
            }
        }
        runCatching {
            supabase.from("physical_details").delete {
                filter { eq("post_id", postId) }
            }
        }
        runCatching {
            supabase.from("post_role_screening_questions").delete {
                filter { eq("post_id", postId) }
            }
        }
        runCatching {
            supabase.from("post_role_responsibilities").delete {
                filter { eq("post_id", postId) }
            }
        }
        runCatching {
            supabase.from("post_role_skills").delete {
                filter { eq("post_id", postId) }
            }
        }
        runCatching {
            supabase.from("post_roles").delete {
                filter { eq("post_id", postId) }
            }
        }

        if (thumbnailPath != null) {
            runCatching {
                supabase.storage
                    .from(THUMBNAIL_BUCKET)
                    .delete(thumbnailPath)
            }
        }

        runCatching {
            supabase.from("volunteer_posts").delete {
                filter { eq("post_id", postId) }
            }
        }
    }

    private fun sqlDate(dateMillis: Long): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date(dateMillis))
    }

    private fun sqlTime(minutesAfterMidnight: Int): String {
        require(minutesAfterMidnight in 0..1439) {
            "Time must be between 00:00 and 23:59."
        }

        val hour = minutesAfterMidnight / 60
        val minute = minutesAfterMidnight % 60
        return String.format(Locale.US, "%02d:%02d:00", hour, minute)
    }

    private fun currentUtcTimestamp(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    private fun String?.nullIfBlank(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

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

    private fun JsonObject.requiredText(key: String): String {
        return optionalText(key)
            ?: error("Missing '$key' in Create Post catalogue data.")
    }

    private fun JsonObject.optionalText(key: String): String? {
        return this[key]
            ?.jsonPrimitive
            ?.contentOrNull
    }

    private fun JsonObject.optionalBoolean(key: String): Boolean? {
        return optionalText(key)?.toBooleanStrictOrNull()
    }

    private fun JsonObject.optionalInt(key: String): Int? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        return runCatching { element.jsonPrimitive.intOrNull }.getOrNull()
    }

    private fun JsonObject.requiredInt(key: String): Int {
        return optionalInt(key) ?: error("Missing required integer '$key'.")
    }

    private fun JsonObject.optionalDouble(key: String): Double? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        return runCatching { element.jsonPrimitive.doubleOrNull }.getOrNull()
    }

    private fun JsonObject.requiredDouble(key: String): Double {
        return optionalDouble(key) ?: error("Missing required number '$key'.")
    }

    private fun parseSqlDate(value: String): Long {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }.parse(value)?.time ?: error("Invalid database date: $value")
    }

    private fun parseSqlTime(value: String): Int {
        val parts = value.take(5).split(":")
        if (parts.size != 2) error("Invalid database time: $value")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private data class SchedulePublishData(
        val row: JsonObject,
        val targetRoleTemplateIds: List<String>
    )

    private companion object {
        const val TEST_ORGANISATION_ID = "ORG0001"
        const val THUMBNAIL_BUCKET = "post-thumbnails"
        const val TEST_STORAGE_PREFIX = "v1_erd_test"
    }
}
