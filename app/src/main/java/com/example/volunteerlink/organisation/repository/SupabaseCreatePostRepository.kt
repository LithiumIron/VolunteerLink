package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreateRoleSkill
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.TrainingLocationMode
import com.example.volunteerlink.organisation.create.model.TrainingMode
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
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
 * Publishing is still a client-side multi-table operation for this university
 * project. The parent row stays DRAFT only while this one publish call is
 * running, then switches to PUBLISHED after all child rows and the optional
 * thumbnail succeed. There is no user-facing Save Draft feature here.
 */
class SupabaseCreatePostRepository : CreatePostRepository {

    override suspend fun publishPost(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): PublishedPostResult {
        val postType = draft.postType
            ?: error("Choose a post type before publishing.")
        val category = draft.category
            ?: error("Choose a category before publishing.")
        val publishTimestamp = currentUtcTimestamp()
        val posts = supabase.from("volunteer_posts")
        val bucket = supabase.storage.from(THUMBNAIL_BUCKET)

        // Keep the parent as DRAFT only while this publish call is inserting
        // its normalized child rows. The user still has no Save Draft feature.
        val parentRow = buildJsonObject {
            put("organisation_id", TEST_ORGANISATION_ID)
            put("title", draft.title.trim())
            put("description", draft.description.trim())
            put("mode", postType.databaseValue)
            put("status", "DRAFT")
            put("created_at", publishTimestamp)
            put("updated_at", publishTimestamp)
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

            // Publication is deliberately the final database action.
            posts.update(
                {
                    set("status", "PUBLISHED")
                    set("published_at", publishTimestamp)
                    set("updated_at", publishTimestamp)
                }
            ) {
                filter {
                    eq("post_id", postId)
                }
            }

            return PublishedPostResult(
                postId = postId,
                thumbnailPath = uploadedThumbnailPath
            )
        } catch (e: Exception) {
            cleanupFailedPublish(
                postId = postId,
                thumbnailPath = uploadedThumbnailPath
            )
            throw e
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
            ?: error("Choose a Physical event location before publishing.")
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
            ?: error("Choose a Remote submission mode before publishing.")
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
                        ?: error("Choose the responsible Remote role before publishing.")
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

            if (item.scheduleType == ScheduleType.TRAINING) {
                val trainingMode = item.trainingMode
                    ?: error("A Training item is missing its format.")
                put("training_mode", trainingMode.databaseValue)

                item.trainingTimeZoneId.nullIfBlank()?.let { timeZone ->
                    put("training_time_zone", timeZone)
                }
                item.allowApplicationsAfterStart?.let { allow ->
                    put("allow_applications_after_start", allow)
                }

                when (trainingMode) {
                    TrainingMode.ONLINE -> {
                        item.onlinePlatform.nullIfBlank()?.let { platform ->
                            put("online_platform", platform)
                        }
                        item.meetingLink.nullIfBlank()?.let { link ->
                            put("meeting_link", link)
                        }
                    }

                    TrainingMode.ONSITE -> {
                        val locationMode = item.trainingLocationMode
                            ?: error("An on-site Training item is missing its location choice.")
                        put("training_location_mode", locationMode.name)

                        if (locationMode == TrainingLocationMode.CUSTOM) {
                            val location = item.trainingLocation
                                ?: error("Choose the custom Training location before publishing.")

                            location.placeId.nullIfBlank()?.let { placeId ->
                                put("training_location_place_id", placeId)
                            }
                            location.displayName.nullIfBlank()?.let { name ->
                                put("training_location_name", name)
                            }
                            location.address.nullIfBlank()?.let { address ->
                                put("training_location_address", address)
                            }
                            location.city.nullIfBlank()?.let { city ->
                                put("training_location_city", city)
                            }
                            location.state.nullIfBlank()?.let { state ->
                                put("training_location_state_region", state)
                            }
                            location.country.nullIfBlank()?.let { country ->
                                put("training_location_country", country)
                            }
                            put("training_location_latitude", location.latitude)
                            put("training_location_longitude", location.longitude)
                        }
                    }
                }
            }
        }

        return SchedulePublishData(
            row = row,
            targetRoleTemplateIds = targetRoleIds
        )
    }

    /**
     * Best-effort cleanup for a publish that fails before the final PUBLISHED
     * update. Explicit child cleanup is kept even where a test FK currently
     * cascades, so the temporary publish test is easy to understand and debug.
     */
    private suspend fun cleanupFailedPublish(
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
