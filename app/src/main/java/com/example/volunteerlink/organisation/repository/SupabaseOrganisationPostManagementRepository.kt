package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.auth.OrganisationSession
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceDay
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceRecord
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceSnapshot
import com.example.volunteerlink.organisation.manage.model.PostManagementEvaluation
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalDetails
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingReviewDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteDetails
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteCompletionDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmission
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmissionDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRole
import com.example.volunteerlink.organisation.manage.model.PostManagementScheduleItem
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.Locale

/** Supabase reader for the Organisation Post Management detail screen. */
class SupabaseOrganisationPostManagementRepository : OrganisationPostManagementRepository {

    companion object {
        private const val REMOTE_SUBMISSION_BUCKET = "remote-submissions"
    }

    override suspend fun loadPost(postId: String): PostManagementPost {
        val organisationId = OrganisationSession.requireOrganisationId()
        val postRow = supabase
            .from("volunteer_posts")
            .select(
                columns = Columns.raw(
                    "post_id,organisation_id,title,description,mode,status,category"
                )
            ) {
                filter {
                    eq("post_id", postId)
                    eq("organisation_id", organisationId)
                }
            }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?: error("Volunteer post $postId does not belong to this organisation.")

        val physicalRow = supabase
            .from("physical_details")
            .select(
                columns = Columns.raw(
                    "start_date,end_date,start_time,end_time,location_name," +
                            "location_address,meeting_point,volunteer_capacity,time_zone"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()
            .firstOrNull()

        val remoteRow = supabase
            .from("remote_details")
            .select(
                columns = Columns.raw(
                    "start_date,end_date,new_end_date,volunteer_capacity,submission_mode," +
                            "shared_deliverable,responsible_role_template_id"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()
            .firstOrNull()

        val scheduleRows = supabase
            .from("schedule_items")
            .select(
                columns = Columns.raw(
                    "schedule_item_id,schedule_type,schedule_date,title,start_time,end_time," +
                            "location,notes"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        val scheduleRoleRows = supabase
            .from("schedule_item_roles")
            .select(
                columns = Columns.raw("schedule_item_id,role_template_id")
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        val scheduleRoleIdsBySchedule = scheduleRoleRows
            .groupBy { it.requiredText("schedule_item_id") }
            .mapValues { (_, rows) ->
                rows.map { it.requiredText("role_template_id") }.distinct()
            }

        val roleRows = supabase
            .from("post_roles")
            .select(
                columns = Columns.raw(
                    "role_template_id,capacity,application_method,role_notes," +
                            "individual_submission_requirement"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        val roleTemplateIds = roleRows
            .map { it.requiredText("role_template_id") }
            .distinct()

        val roleTemplateRows = if (roleTemplateIds.isEmpty()) {
            emptyList()
        } else {
            supabase
                .from("role_templates")
                .select(
                    columns = Columns.raw(
                        "role_template_id,role_name,role_mode,default_level"
                    )
                ) {
                    filter { isIn("role_template_id", roleTemplateIds) }
                }
                .decodeList<JsonObject>()
        }

        val roleTemplatesById = roleTemplateRows.associateBy {
            it.requiredText("role_template_id")
        }

        val roles = roleRows.mapNotNull { roleRow ->
            val roleTemplateId = roleRow.requiredText("role_template_id")
            val template = roleTemplatesById[roleTemplateId]
                ?: return@mapNotNull null

            PostManagementRole(
                roleTemplateId = roleTemplateId,
                roleName = template.requiredText("role_name"),
                roleMode = template.requiredText("role_mode"),
                defaultLevel = template.requiredText("default_level"),
                capacity = roleRow.requiredInt("capacity"),
                applicationMethod = roleRow.requiredText("application_method"),
                roleNotes = roleRow.optionalText("role_notes"),
                individualSubmissionRequirement = roleRow.optionalText(
                    "individual_submission_requirement"
                )
            )
        }.sortedBy { it.roleName }

        val participationRows = supabase
            .from("role_participations")
            .select(
                columns = Columns.raw(
                    "role_template_id,user_id,application_status,completion_status," +
                            "joined_at,completed_at,created_at,decision_note,is_shortlisted"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        val attendanceDayRows = supabase
            .from("attendance_days")
            .select(
                columns = Columns.raw(
                    "event_date,pin_code,expected_minutes,generated_at,is_active"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        val attendanceRecordRows = supabase
            .from("attendance_records")
            .select(
                columns = Columns.raw(
                    "event_date,role_template_id,user_id,attendance_status,checked_in_at,verified_minutes"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        val remoteSubmissionRows = supabase
            .from("remote_submissions")
            .select(
                columns = Columns.raw(
                    "submission_id,role_template_id,user_id,submission_type,file_path," +
                            "submission_url,status,feedback,submitted_at,reviewed_at,updated_at"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        val evaluationRows = supabase
            .from("volunteer_evaluations")
            .select(
                columns = Columns.raw(
                    "role_template_id,user_id,organisation_id,feedback,completion_reason,created_at,verified_minutes"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        val participantUserIds = participationRows
            .map { it.requiredText("user_id") }
            .distinct()

        val profileRows = if (participantUserIds.isEmpty()) {
            emptyList()
        } else {
            supabase
                .from("user_profiles")
                .select(
                    columns = Columns.raw(
                        "user_id,full_name,city,bio,avatar_path,account_type"
                    )
                ) {
                    filter {
                        isIn("user_id", participantUserIds)
                        eq("account_type", "VOLUNTEER")
                    }
                }
                .decodeList<JsonObject>()
        }

        val profilesById = profileRows.associateBy { it.requiredText("user_id") }
        val rolesById = roles.associateBy { it.roleTemplateId }

        val people = participationRows.mapNotNull { participationRow ->
            val userId = participationRow.requiredText("user_id")
            val roleTemplateId = participationRow.requiredText("role_template_id")
            val profile = profilesById[userId]
            val role = rolesById[roleTemplateId] ?: return@mapNotNull null

            PostManagementPerson(
                userId = userId,
                // Keep the participation visible even if a profile-select RLS
                // policy has not been added yet. The readable name replaces
                // the ID automatically as soon as user_profiles is readable.
                fullName = profile?.optionalText("full_name")?.takeIf { it.isNotBlank() } ?: "Volunteer",
                city = profile?.optionalText("city"),
                bio = profile?.optionalText("bio"),
                avatarPath = profile?.optionalText("avatar_path"),
                roleTemplateId = roleTemplateId,
                roleName = role.roleName,
                roleMode = role.roleMode,
                defaultLevel = role.defaultLevel,
                applicationStatus = participationRow.requiredText("application_status"),
                completionStatus = participationRow.requiredText("completion_status"),
                joinedAt = participationRow.optionalText("joined_at"),
                completedAt = participationRow.optionalText("completed_at"),
                appliedAt = participationRow.optionalText("created_at"),
                decisionNote = participationRow.optionalText("decision_note"),
                isShortlisted = participationRow.optionalBoolean("is_shortlisted") ?: false
            )
        }.sortedWith(
            compareBy<PostManagementPerson> { it.roleName }
                .thenBy { it.fullName }
        )

        return PostManagementPost(
            postId = postRow.requiredText("post_id"),
            title = postRow.requiredText("title"),
            description = postRow.requiredText("description"),
            mode = postRow.requiredText("mode"),
            databaseStatus = postRow.requiredText("status"),
            category = postRow.optionalText("category"),
            physical = physicalRow?.let {
                PostManagementPhysicalDetails(
                    startDate = it.requiredText("start_date"),
                    endDate = it.requiredText("end_date"),
                    startTime = it.requiredText("start_time"),
                    endTime = it.requiredText("end_time"),
                    locationName = it.requiredText("location_name"),
                    locationAddress = it.optionalText("location_address"),
                    meetingPoint = it.optionalText("meeting_point"),
                    volunteerCapacity = it.requiredInt("volunteer_capacity"),
                    timeZone = it.optionalText("time_zone")
                )
            },
            remote = remoteRow?.let {
                PostManagementRemoteDetails(
                    startDate = it.requiredText("start_date"),
                    endDate = it.requiredText("end_date"),
                    newEndDate = it.optionalText("new_end_date"),
                    volunteerCapacity = it.requiredInt("volunteer_capacity"),
                    submissionMode = it.requiredText("submission_mode"),
                    sharedDeliverable = it.optionalText("shared_deliverable"),
                    responsibleRoleTemplateId = it.optionalText(
                        "responsible_role_template_id"
                    )
                )
            },
            schedules = scheduleRows.map {
                PostManagementScheduleItem(
                    scheduleItemId = it.requiredText("schedule_item_id"),
                    scheduleType = it.requiredText("schedule_type"),
                    scheduleDate = it.requiredText("schedule_date"),
                    title = it.requiredText("title"),
                    startTime = it.optionalText("start_time"),
                    endTime = it.optionalText("end_time"),
                    location = it.optionalText("location"),
                    notes = it.optionalText("notes"),
                    roleTemplateIds = scheduleRoleIdsBySchedule[
                        it.requiredText("schedule_item_id")
                    ].orEmpty()
                )
            }.sortedWith(
                compareBy<PostManagementScheduleItem> { it.scheduleDate }
                    .thenBy { it.startTime.orEmpty() }
            ),
            roles = roles,
            people = people,
            attendanceDays = attendanceDayRows.map { row ->
                PostManagementAttendanceDay(
                    eventDate = row.requiredText("event_date"),
                    pinCode = row.requiredText("pin_code"),
                    expectedMinutes = row.requiredInt("expected_minutes"),
                    generatedAt = row.optionalText("generated_at"),
                    isActive = row.optionalBoolean("is_active") ?: false
                )
            }.sortedBy { it.eventDate },
            attendanceRecords = attendanceRecordRows.map { row ->
                PostManagementAttendanceRecord(
                    eventDate = row.requiredText("event_date"),
                    roleTemplateId = row.requiredText("role_template_id"),
                    userId = row.requiredText("user_id"),
                    attendanceStatus = row.optionalText("attendance_status") ?: "PRESENT",
                    checkedInAt = row.optionalText("checked_in_at"),
                    verifiedMinutes = row.requiredInt("verified_minutes")
                )
            }.sortedWith(
                compareBy<PostManagementAttendanceRecord> { it.eventDate }
                    .thenBy { it.roleTemplateId }
                    .thenBy { it.userId }
            ),
            remoteSubmissions = remoteSubmissionRows.map { row ->
                PostManagementRemoteSubmission(
                    submissionId = row.requiredText("submission_id"),
                    roleTemplateId = row.optionalText("role_template_id"),
                    userId = row.optionalText("user_id"),
                    submissionType = row.requiredText("submission_type"),
                    filePath = row.optionalText("file_path"),
                    submissionUrl = row.optionalText("submission_url"),
                    status = row.requiredText("status"),
                    feedback = row.optionalText("feedback"),
                    submittedAt = row.optionalText("submitted_at"),
                    reviewedAt = row.optionalText("reviewed_at"),
                    updatedAt = row.optionalText("updated_at")
                )
            }.sortedBy { it.submissionId },
            evaluations = evaluationRows.map { row ->
                PostManagementEvaluation(
                    roleTemplateId = row.requiredText("role_template_id"),
                    userId = row.requiredText("user_id"),
                    organisationId = row.requiredText("organisation_id"),
                    feedback = row.optionalText("feedback"),
                    completionReason = row.optionalText("completion_reason"),
                    createdAt = row.optionalText("created_at"),
                    verifiedMinutes = row.optionalInt("verified_minutes")
                )
            }.sortedBy { it.userId }
        )
    }


    override suspend fun downloadRemoteSubmission(
        postId: String,
        filePath: String
    ): ByteArray {
        requireOwnedPost(postId)

        val normalizedPath = filePath.trim().removePrefix("/")
        require(normalizedPath.isNotBlank()) {
            "This submission does not contain a file path."
        }
        require(normalizedPath.startsWith("$postId/")) {
            "This submission file does not belong to $postId."
        }

        return supabase.storage
            .from(REMOTE_SUBMISSION_BUCKET)
            .downloadAuthenticated(normalizedPath)
    }

    override suspend fun reviewRemoteSubmission(
        postId: String,
        submissionId: String,
        action: String,
        feedback: String?
    ) {
        requireOwnedPost(postId)

        val normalizedAction = action.trim().uppercase(Locale.US)
        require(normalizedAction == "ACCEPT" || normalizedAction == "REQUEST_REVISION") {
            "Unsupported Remote submission review action."
        }

        val revisionFeedback = feedback?.trim().orEmpty()
        if (normalizedAction == "REQUEST_REVISION") {
            require(revisionFeedback.isNotBlank()) {
                "Revision feedback is required."
            }
        }

        supabase.postgrest.rpc(
            function = "organisation_review_remote_submission_authenticated",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_submission_id", submissionId)
                put("p_action", normalizedAction)
                if (revisionFeedback.isBlank()) {
                    put("p_feedback", JsonNull)
                } else {
                    put("p_feedback", revisionFeedback)
                }
            }
        )
    }

    override suspend fun saveRemoteSubmissionReviewStage(
        postId: String,
        decisions: List<PostManagementRemoteSubmissionDecision>,
        newEndDate: String?
    ) {
        requireOwnedPost(postId)

        supabase.postgrest.rpc(
            function = "organisation_save_remote_submission_review_stage",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_decisions", buildJsonArray {
                    decisions.forEach { decision ->
                        add(buildJsonObject {
                            put("submission_id", decision.submissionId)
                            put("decision", decision.decision.name)
                            val feedback = decision.feedback
                            if (feedback.isNullOrBlank()) {
                                put("feedback", JsonNull)
                            } else {
                                put("feedback", feedback.trim())
                            }
                        })
                    }
                })
                if (newEndDate.isNullOrBlank()) {
                    put("p_new_end_date", JsonNull)
                } else {
                    put("p_new_end_date", newEndDate)
                }
            }
        )
    }

    override suspend fun finalizeRemoteReviewBatch(
        postId: String,
        decisions: List<PostManagementRemoteCompletionDecision>,
        feedbackByParticipation: Map<String, String>
    ) {
        requireOwnedPost(postId)

        supabase.postgrest.rpc(
            function = "organisation_finalize_remote_review_batch",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_decisions", buildJsonArray {
                    decisions.forEach { decision ->
                        add(buildJsonObject {
                            put("role_template_id", decision.roleTemplateId)
                            put("user_id", decision.userId)
                            put("decision", decision.decision.name)
                            val reason = decision.reason
                            if (reason.isNullOrBlank()) {
                                put("reason", JsonNull)
                            } else {
                                put("reason", reason.trim())
                            }
                        })
                    }
                })
                put("p_feedback", buildJsonArray {
                    feedbackByParticipation.forEach { (participationKey, feedback) ->
                        val splitAt = participationKey.indexOf("::")
                        if (splitAt > 0 && feedback.isNotBlank()) {
                            add(buildJsonObject {
                                put("role_template_id", participationKey.substring(0, splitAt))
                                put("user_id", participationKey.substring(splitAt + 2))
                                put("feedback", feedback.trim())
                            })
                        }
                    }
                })
            }
        )
    }

    override suspend fun loadPhysicalAttendance(
        postId: String
    ): PostManagementAttendanceSnapshot {
        requireOwnedPost(postId)

        val attendanceDayRows = supabase
            .from("attendance_days")
            .select(
                columns = Columns.raw(
                    "event_date,pin_code,expected_minutes,generated_at,is_active"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        val attendanceRecordRows = supabase
            .from("attendance_records")
            .select(
                columns = Columns.raw(
                    "event_date,role_template_id,user_id,attendance_status,checked_in_at,verified_minutes"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        return PostManagementAttendanceSnapshot(
            attendanceDays = attendanceDayRows.map { row ->
                PostManagementAttendanceDay(
                    eventDate = row.requiredText("event_date"),
                    pinCode = row.requiredText("pin_code"),
                    expectedMinutes = row.requiredInt("expected_minutes"),
                    generatedAt = row.optionalText("generated_at"),
                    isActive = row.optionalBoolean("is_active") ?: false
                )
            }.sortedBy { it.eventDate },
            attendanceRecords = attendanceRecordRows.map { row ->
                PostManagementAttendanceRecord(
                    eventDate = row.requiredText("event_date"),
                    roleTemplateId = row.requiredText("role_template_id"),
                    userId = row.requiredText("user_id"),
                    attendanceStatus = row.optionalText("attendance_status") ?: "PRESENT",
                    checkedInAt = row.optionalText("checked_in_at"),
                    verifiedMinutes = row.requiredInt("verified_minutes")
                )
            }.sortedWith(
                compareBy<PostManagementAttendanceRecord> { it.eventDate }
                    .thenBy { it.roleTemplateId }
                    .thenBy { it.userId }
            )
        )
    }


    override suspend fun setApplicantShortlisted(
        postId: String,
        roleTemplateId: String,
        userId: String,
        isShortlisted: Boolean
    ) {
        requireOwnedPost(postId)

        supabase.postgrest.rpc(
            function = "organisation_set_applicant_shortlisted",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_role_template_id", roleTemplateId)
                put("p_user_id", userId)
                put("p_is_shortlisted", isShortlisted)
            }
        )
    }

    override suspend fun startPhysicalAttendance(postId: String) {
        requireOwnedPost(postId)

        supabase.postgrest.rpc(
            function = "organisation_start_physical_attendance",
            parameters = buildJsonObject {
                put("p_post_id", postId)
            }
        )
    }

    override suspend fun markVolunteerPresent(
        postId: String,
        eventDate: String,
        roleTemplateId: String,
        userId: String
    ) {
        requireOwnedPost(postId)

        supabase.postgrest.rpc(
            function = "organisation_mark_physical_present",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_event_date", eventDate)
                put("p_role_template_id", roleTemplateId)
                put("p_user_id", userId)
            }
        )
    }

    override suspend fun markVolunteerAbsent(
        postId: String,
        eventDate: String,
        roleTemplateId: String,
        userId: String
    ) {
        requireOwnedPost(postId)

        supabase.postgrest.rpc(
            function = "organisation_mark_physical_absent",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_event_date", eventDate)
                put("p_role_template_id", roleTemplateId)
                put("p_user_id", userId)
            }
        )
    }

    override suspend fun preparePhysicalReview(postId: String) {
        requireOwnedPost(postId)
        supabase.postgrest.rpc(
            function = "organisation_prepare_physical_review",
            parameters = buildJsonObject { put("p_post_id", postId) }
        )
    }

    override suspend fun reportPhysicalReviewIssue(
        postId: String,
        roleTemplateId: String,
        userId: String,
        reason: String
    ) {
        requireOwnedPost(postId)
        supabase.postgrest.rpc(
            function = "organisation_report_physical_review_issue",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_role_template_id", roleTemplateId)
                put("p_user_id", userId)
                put("p_reason", reason)
            }
        )
    }

    override suspend fun completeAllReadyPhysical(postId: String) {
        requireOwnedPost(postId)
        supabase.postgrest.rpc(
            function = "organisation_complete_all_ready_physical",
            parameters = buildJsonObject { put("p_post_id", postId) }
        )
    }

    override suspend fun finalizePhysicalVolunteer(
        postId: String,
        roleTemplateId: String,
        userId: String,
        decision: String,
        note: String?
    ) {
        requireOwnedPost(postId)
        supabase.postgrest.rpc(
            function = "organisation_finalize_physical_volunteer",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_role_template_id", roleTemplateId)
                put("p_user_id", userId)
                put("p_decision", decision)
                if (note == null) put("p_note", JsonNull) else put("p_note", note)
            }
        )
    }

    override suspend fun savePhysicalFeedback(
        postId: String,
        userIds: List<String>,
        feedback: String,
        replaceExisting: Boolean
    ) {
        requireOwnedPost(postId)
        supabase.postgrest.rpc(
            function = "organisation_save_physical_feedback",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_user_ids", buildJsonArray { userIds.forEach { add(it) } })
                put("p_feedback", feedback)
                put("p_replace_existing", replaceExisting)
            }
        )
    }

    override suspend fun finalizePhysicalReviewPost(postId: String) {
        requireOwnedPost(postId)
        supabase.postgrest.rpc(
            function = "organisation_finalize_physical_review_post",
            parameters = buildJsonObject { put("p_post_id", postId) }
        )
    }

    override suspend fun finalizePhysicalReviewBatch(
        postId: String,
        decisions: List<PostManagementPendingReviewDecision>,
        feedbackByUserId: Map<String, String>
    ) {
        requireOwnedPost(postId)

        supabase.postgrest.rpc(
            function = "organisation_finalize_physical_review_batch",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_decisions", buildJsonArray {
                    decisions.forEach { decision ->
                        add(buildJsonObject {
                            put("role_template_id", decision.roleTemplateId)
                            put("user_id", decision.userId)
                            put("decision", decision.decision.name)
                            val reason = decision.reason
                            if (reason == null) {
                                put("note", JsonNull)
                                put("reason", JsonNull)
                            } else {
                                // Current SQL uses `note`. Keep `reason` as the same transport
                                // value too so an older deployed batch RPC cannot silently lose it.
                                put("note", reason)
                                put("reason", reason)
                            }
                        })
                    }
                })
                put("p_feedback", buildJsonArray {
                    feedbackByUserId.forEach { (userId, feedback) ->
                        add(buildJsonObject {
                            put("user_id", userId)
                            put("feedback", feedback)
                        })
                    }
                })
            }
        )
    }

    private suspend fun requireOwnedPost(postId: String) {
        val organisationId = OrganisationSession.requireOrganisationId()
        val ownsPost = supabase
            .from("volunteer_posts")
            .select(columns = Columns.raw("post_id")) {
                filter {
                    eq("post_id", postId)
                    eq("organisation_id", organisationId)
                }
            }
            .decodeList<JsonObject>()
            .isNotEmpty()

        require(ownsPost) {
            "Volunteer post $postId does not belong to this organisation."
        }
    }


    private fun JsonObject.requiredText(key: String): String {
        return optionalText(key)
            ?: error("Missing required Supabase field: $key")
    }

    private fun JsonObject.optionalText(key: String): String? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        return runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()
    }

    private fun JsonObject.optionalBoolean(key: String): Boolean? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        return runCatching { element.jsonPrimitive.booleanOrNull }.getOrNull()
    }

    private fun JsonObject.optionalInt(key: String): Int? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        return runCatching { element.jsonPrimitive.intOrNull }.getOrNull()
    }

    private fun JsonObject.requiredInt(key: String): Int {
        val value = this[key]
            ?.takeUnless { it is JsonNull }
            ?.jsonPrimitive
            ?.intOrNull
        return value ?: error("Missing required Supabase integer field: $key")
    }
}
