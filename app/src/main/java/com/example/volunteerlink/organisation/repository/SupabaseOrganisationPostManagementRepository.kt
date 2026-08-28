package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceDay
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceRecord
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceSnapshot
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalDetails
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteDetails
import com.example.volunteerlink.organisation.manage.model.PostManagementRole
import com.example.volunteerlink.organisation.manage.model.PostManagementScheduleItem
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Supabase reader for the Organisation Post Management detail screen. */
class SupabaseOrganisationPostManagementRepository : OrganisationPostManagementRepository {

    override suspend fun loadPost(postId: String): PostManagementPost {
        val postRow = supabase
            .from("volunteer_posts")
            .select(
                columns = Columns.raw(
                    "post_id,title,description,mode,status,category"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?: error("Volunteer post $postId was not found.")

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
                    "start_date,end_date,volunteer_capacity,submission_mode," +
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
                            "joined_at,created_at,decision_note,is_shortlisted"
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
                fullName = profile?.optionalText("full_name") ?: userId,
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
            )
        )
    }


    override suspend fun loadPhysicalAttendance(
        postId: String
    ): PostManagementAttendanceSnapshot {
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
        supabase
            .from("role_participations")
            .update(
                {
                    set("is_shortlisted", isShortlisted)
                }
            ) {
                filter {
                    eq("post_id", postId)
                    eq("role_template_id", roleTemplateId)
                    eq("user_id", userId)
                    eq("application_status", "PENDING")
                }
            }
    }

    override suspend fun startPhysicalAttendance(postId: String) {
        supabase.postgrest.rpc(
            function = "start_physical_attendance",
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

    private fun JsonObject.requiredInt(key: String): Int {
        val value = this[key]
            ?.takeUnless { it is JsonNull }
            ?.jsonPrimitive
            ?.intOrNull
        return value ?: error("Missing required Supabase integer field: $key")
    }
}
