package com.example.volunteerlink.organisation.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements the Organisation Post Management contract against Supabase PostgREST, RPCs and Storage.
//
// Read operations reconstruct one management view from normalized post, role, schedule, participation, attendance
// and submission records.
//
// Business mutations are intentionally routed through authenticated PostgreSQL RPCs because applicant decisions,
// attendance and completion touch multiple related rows and must be ownership-checked and transactionally
// consistent.
//
// Remote submission files are downloaded from Supabase Storage only after the repository has established the
// post/submission context used by the Organisation screen.
//
// AppClock-aligned timestamps are supplied/used where the project treats business time as testable application
// time.
//
// After a write succeeds the ViewModel reloads server state; this repository does not make local cached data
// authoritative.
//
// Architectural layer: Data/repository layer.
// ============================================================================


import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.auth.OrganisationSession
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceDay
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceRecord
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceSnapshot
import com.example.volunteerlink.organisation.manage.model.PostManagementEvaluation
import com.example.volunteerlink.organisation.manage.model.PostManagementImpactWeavePartner
import com.example.volunteerlink.organisation.manage.model.PostManagementImpactWeaveContribution
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalDetails
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingReviewDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteDetails
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteMissingDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmission
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmissionDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRole
import com.example.volunteerlink.organisation.manage.model.PostManagementScheduleItem
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Serializable
/**
 * Holds the values represented by post management event contact row as one strongly typed model.
 * It keeps backend-facing work behind the Manage Post repository boundary.
 */
/**
 * DETAILED DECLARATION — PostManagementEventContactRow
 *
 * Domain/UI type for Post Management Event Contact Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PostManagementEventContactRow(
    @SerialName("user_id") val userId: String,
    @SerialName("shared_phone") val sharedPhone: String = "",
    @SerialName("phone_contact_until_label") val phoneContactUntilLabel: String? = null
)

@Serializable
/**
 * Holds the values represented by impact weave post contribution row as one strongly typed model.
 * It keeps backend-facing work behind the Manage Post repository boundary.
 */
/**
 * DETAILED DECLARATION — ImpactWeavePostContributionRow
 *
 * Domain/UI type for Impact Weave Post Contribution Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class ImpactWeavePostContributionRow(
    @SerialName("impact_weave_draft_id") val impactWeaveDraftId: String,
    @SerialName("partner_organisation_id") val partnerOrganisationId: String,
    @SerialName("partner_organisation_name") val partnerOrganisationName: String,
    @SerialName("support_type") val supportType: String,
    @SerialName("need_resource_name") val needResourceName: String,
    @SerialName("provider_resource_name") val providerResourceName: String? = null,
    @SerialName("quantity_provided") val quantityProvided: Int? = null,
    @SerialName("capacity_provided") val capacityProvided: Int? = null
)

/** Supabase reader for the Organisation Post Management detail screen. */
/**
 * DETAILED DECLARATION — SupabaseOrganisationPostManagementRepository
 *
 * Data-access implementation/contract for Supabase Organisation Post Management Repository, isolating backend
 * details from the screen and ViewModel layers.
 *
 * Protected server state still relies on authenticated Supabase authorization and database rules rather than
 * trusting client-side checks alone.
 *
 * This implementation translates VolunteerLink models to PostgREST/RPC/Storage operations and maps backend
 * responses back into domain models.
 */
class SupabaseOrganisationPostManagementRepository : OrganisationPostManagementRepository {

    /**
     * Publishes the current Volunteer Post data after the required Manage Post checks pass.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — publishSavedDraft
     *
     * Performs the repository/data-layer operation for publish saved draft.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_publish_saved_draft`: Publishes an existing saved DRAFT after server-side
     * ownership, verification and timing checks; the database performs the authoritative status/timestamp
     * transition.
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     */
    override suspend fun publishSavedDraft(postId: String, appNowMillis: Long) {
        val organisation = OrganisationSession.requireContext()
        require(organisation.isVerified) {
            "Your organisation must be verified before publishing volunteer posts."
        }

        val appTimestamp = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            Locale.US
        ).format(Date(appNowMillis))
        val appToday = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(Date(appNowMillis))

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_publish_saved_draft
        // Publishes an existing saved DRAFT after server-side ownership, verification and timing checks; the
        // database performs the authoritative status/timestamp transition.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_publish_saved_draft",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_app_now", appTimestamp)
                put("p_app_today", appToday)
            }
        )
    }

    companion object {
        private const val REMOTE_SUBMISSION_BUCKET = "remote-submissions"
    }

    /**
     * Loads the post needed by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadPost
     *
     * Builds the complete Post Management domain object from normalized Supabase records needed by Overview,
     * People and Review.
     *
     * It combines parent post data with Physical/Remote details, roles, schedules, participations, screening
     * answers, attendance/submissions and Impact Weave partner support while preserving each table as the
     * backend source of truth.
     *
     * The result is screen-oriented but read-only; mutations use dedicated repository methods/RPCs instead of
     * modifying this assembled object and writing it back wholesale.
     *
     * Supabase RPC `organisation_resolve_application_lifecycle`: Reconciles pending application rows against
     * current post/role timing and capacity rules before Organisation counts or actions are shown.
     *
     * Supabase RPC `organisation_get_post_impact_weave_partners`: Returns accepted Impact Weave partner
     * organisations and the support linked to this post for the owner Manage Post view.
     *
     * Supabase RPC `organisation_list_post_volunteer_event_contacts`: Returns opportunity-scoped volunteer
     * contact availability that the organisation is currently permitted to use.
     *
     * Reads/maps Supabase table data from `volunteer_posts` (the parent Volunteer Post record, including owner,
     * mode, lifecycle status, category and publication metadata); `physical_details` (Physical-side
     * date/time/location/capacity data for a post); `remote_details` (Remote-side project dates, capacity and
     * submission configuration); `schedule_items` (Physical activities and Remote milestones belonging to the
     * post schedule); `schedule_item_roles` (many-to-many links between schedule items and the roles affected
     * by them).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    override suspend fun loadPost(postId: String): PostManagementPost {
        val organisationContext = OrganisationSession.requireContext()
        val organisationId = organisationContext.organisationId

        // Keep pending applicants truthful before building the screen snapshot.
        // Role-full and role-started applications are persisted as DECLINED, not merely hidden.
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_resolve_application_lifecycle
        // Reconciles pending application rows against current post/role timing and capacity rules before
        // Organisation counts or actions are shown.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_resolve_application_lifecycle",
            parameters = buildJsonObject { put("p_post_id", postId) }
        )

        val postRow = supabase
            // SUPABASE TABLE: volunteer_posts — the parent Volunteer Post record, including owner, mode, lifecycle status, category and publication metadata.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
            .from("volunteer_posts")
            .select(
                columns = Columns.raw(
                    "post_id,organisation_id,title,description,mode,status,category,impact_weave_draft_id"
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

        val impactWeaveDraftId = postRow.optionalText("impact_weave_draft_id")
        val impactWeavePartners = if (impactWeaveDraftId.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                // ------------------------------------------------------------------------
                // SUPABASE RPC: organisation_get_post_impact_weave_partners
                // Returns accepted Impact Weave partner organisations and the support linked to this post for
                // the owner Manage Post view.
                // The client sends parameters and waits for the database result; ownership, lifecycle and
                // multi-row consistency checks belong on the server for this operation.
                // ------------------------------------------------------------------------
                supabase.postgrest.rpc(
                    function = "organisation_get_post_impact_weave_partners",
                    parameters = buildJsonObject { put("p_post_id", postId) }
                ).decodeList<ImpactWeavePostContributionRow>()
                    .groupBy { it.partnerOrganisationId }
                    .map { (partnerId, rows) ->
                        PostManagementImpactWeavePartner(
                            organisationId = partnerId,
                            organisationName = rows.first().partnerOrganisationName,
                            contributions = rows.map { row ->
                                PostManagementImpactWeaveContribution(
                                    supportType = row.supportType,
                                    needResourceName = row.needResourceName,
                                    providerResourceName = row.providerResourceName,
                                    quantityProvided = row.quantityProvided,
                                    capacityProvided = row.capacityProvided
                                )
                            }
                        )
                    }
                    .sortedBy { it.organisationName.lowercase(Locale.ROOT) }
            }.getOrDefault(emptyList())
        }

        val physicalRow = supabase
            // SUPABASE TABLE: physical_details — Physical-side date/time/location/capacity data for a post.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
            // SUPABASE TABLE: remote_details — Remote-side project dates, capacity and submission configuration.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
            // SUPABASE TABLE: schedule_items — Physical activities and Remote milestones belonging to the post schedule.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
            // SUPABASE TABLE: schedule_item_roles — many-to-many links between schedule items and the roles affected by them.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
            // SUPABASE TABLE: post_roles — the selected role instances for a post, including capacity and application method.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
                // SUPABASE TABLE: role_templates — the fixed role catalogue used when an organisation chooses volunteer roles.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
            // SUPABASE TABLE: role_participations — volunteer application/join/completion state for one post role.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

        val screeningAnswerRows = supabase
            // SUPABASE TABLE: role_participation_screening_answers — the volunteer's normalized answers to the role screening questions.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
            .from("role_participation_screening_answers")
            .select(
                columns = Columns.raw(
                    "role_template_id,user_id,question_no,question_text,answer_text"
                )
            ) {
                filter { eq("post_id", postId) }
            }
            .decodeList<JsonObject>()

        val screeningByParticipation = screeningAnswerRows
            .groupBy { row ->
                row.requiredText("role_template_id") to row.requiredText("user_id")
            }
            .mapValues { (_, rows) ->
                rows.sortedBy { row -> row.requiredInt("question_no") }
            }

        val attendanceDayRows = supabase
            // SUPABASE TABLE: attendance_days — server-prepared Physical attendance day metadata.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
            // SUPABASE TABLE: attendance_records — per-volunteer Physical attendance/verified-time records.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
            // SUPABASE TABLE: remote_submissions — Remote deliverable submission status, file/url and review metadata.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
            // SUPABASE TABLE: volunteer_evaluations — organisation evaluation/feedback results used after participation.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
                // SUPABASE TABLE: user_profiles — account-level profile identity such as volunteer/organisation user id, name and public profile fields.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

        // Phone numbers are never read directly from user_profiles here.
        // The SECURITY DEFINER RPC only returns a number when this organisation
        // owns the post, the volunteer is accepted and active, and that volunteer
        // explicitly enabled phone sharing for this participation.
        val eventContactsByUserId = runCatching {
            // ------------------------------------------------------------------------
            // SUPABASE RPC: organisation_list_post_volunteer_event_contacts
            // Returns opportunity-scoped volunteer contact availability that the organisation is currently
            // permitted to use.
            // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
            // consistency checks belong on the server for this operation.
            // ------------------------------------------------------------------------
            supabase.postgrest.rpc(
                function = "organisation_list_post_volunteer_event_contacts",
                parameters = buildJsonObject { put("p_post_id", postId) }
            ).decodeList<PostManagementEventContactRow>()
                .associateBy { it.userId }
        }.getOrElse {
            it.printStackTrace()
            emptyMap()
        }

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
                eventSharedPhone = eventContactsByUserId[userId]?.sharedPhone?.takeIf { it.isNotBlank() },
                eventPhoneContactUntilLabel = eventContactsByUserId[userId]?.phoneContactUntilLabel,
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
                isShortlisted = participationRow.optionalBoolean("is_shortlisted") ?: false,
                screeningQuestions = screeningByParticipation[roleTemplateId to userId]
                    .orEmpty()
                    .map { row -> row.requiredText("question_text") },
                screeningAnswers = screeningByParticipation[roleTemplateId to userId]
                    .orEmpty()
                    .map { row -> row.requiredText("answer_text") }
            )
        }.sortedWith(
            compareBy<PostManagementPerson> { it.roleName }
                .thenBy { it.fullName }
        )

        return PostManagementPost(
            postId = postRow.requiredText("post_id"),
            organisationName = organisationContext.organisationName,
            title = postRow.requiredText("title"),
            description = postRow.requiredText("description"),
            mode = postRow.requiredText("mode"),
            databaseStatus = postRow.requiredText("status"),
            category = postRow.optionalText("category"),
            impactWeaveDraftId = impactWeaveDraftId,
            impactWeavePartners = impactWeavePartners,
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


    /**
     * Derives the download remote submission value used by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — downloadRemoteSubmission
     *
     * Performs the repository/data-layer operation for download remote submission.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Uses Supabase Storage for binary/file content while database rows keep only the controlled storage path
     * and metadata.
     */
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

    /**
     * Derives the review remote submission value used by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — reviewRemoteSubmission
     *
     * Performs the repository/data-layer operation for review remote submission.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_review_remote_submission_authenticated`: Reviews a Remote submission through
     * an authenticated ownership-checked server path and records the resulting submission status/feedback.
     */
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

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_review_remote_submission_authenticated
        // Reviews a Remote submission through an authenticated ownership-checked server path and records the
        // resulting submission status/feedback.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
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

    /**
     * Saves the remote submission review stage for the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — saveRemoteSubmissionReviewStage
     *
     * Performs the repository/data-layer operation for save remote submission review stage.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_save_remote_submission_review_stage`: Saves the Organisation's staged Remote
     * submission review decision before the final batch is committed.
     */
    override suspend fun saveRemoteSubmissionReviewStage(
        postId: String,
        decisions: List<PostManagementRemoteSubmissionDecision>,
        missingDecisions: List<PostManagementRemoteMissingDecision>,
        newEndDate: String?
    ) {
        requireOwnedPost(postId)

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_save_remote_submission_review_stage
        // Saves the Organisation's staged Remote submission review decision before the final batch is
        // committed.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
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
                put("p_missing_actions", buildJsonArray {
                    missingDecisions.forEach { decision ->
                        add(buildJsonObject {
                            put("action", decision.action.name)
                            val roleTemplateId = decision.roleTemplateId
                                ?.takeIf { it.isNotBlank() }
                            if (roleTemplateId == null) {
                                put("role_template_id", JsonNull)
                            } else {
                                put("role_template_id", roleTemplateId)
                            }

                            val userId = decision.userId?.takeIf { it.isNotBlank() }
                            if (userId == null) {
                                put("user_id", JsonNull)
                            } else {
                                put("user_id", userId)
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

    /**
     * Finalises the remote review batch for the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — finalizeRemoteReviewBatch
     *
     * Performs the repository/data-layer operation for finalize remote review batch.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_finalize_remote_review_batch`: Commits the Remote review batch so
     * submission/participation outcomes are finalised consistently by the database.
     */
    override suspend fun finalizeRemoteReviewBatch(
        postId: String,
        feedbackByParticipation: Map<String, String>
    ) {
        requireOwnedPost(postId)

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_finalize_remote_review_batch
        // Commits the Remote review batch so submission/participation outcomes are finalised consistently by
        // the database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_finalize_remote_review_batch",
            parameters = buildJsonObject {
                put("p_post_id", postId)
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

    /**
     * Loads the physical attendance needed by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadPhysicalAttendance
     *
     * Performs the repository/data-layer operation for load physical attendance.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `attendance_days` (server-prepared Physical attendance day metadata);
     * `attendance_records` (per-volunteer Physical attendance/verified-time records).
     */
    override suspend fun loadPhysicalAttendance(
        postId: String
    ): PostManagementAttendanceSnapshot {
        requireOwnedPost(postId)

        val attendanceDayRows = supabase
            // SUPABASE TABLE: attendance_days — server-prepared Physical attendance day metadata.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
            // SUPABASE TABLE: attendance_records — per-volunteer Physical attendance/verified-time records.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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


    /**
     * Sets the applicant shortlisted used by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — setApplicantShortlisted
     *
     * Performs the repository/data-layer operation for set applicant shortlisted.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_set_applicant_shortlisted`: Persists the organisation's shortlist marker for a
     * still-reviewable applicant.
     */
    override suspend fun setApplicantShortlisted(
        postId: String,
        roleTemplateId: String,
        userId: String,
        isShortlisted: Boolean
    ) {
        requireOwnedPost(postId)

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_set_applicant_shortlisted
        // Persists the organisation's shortlist marker for a still-reviewable applicant.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
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

    /**
     * Derives the review applicant value used by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — reviewApplicant
     *
     * Calls organisation_review_role_applicant with post id, role id, volunteer id, decision and the required
     * manual decline reason when applicable.
     *
     * The database checks that the post belongs to the signed-in organisation, the role still uses
     * REVIEW_APPLICANTS, the application is still PENDING and an ACCEPT does not exceed role capacity.
     *
     * The returned status is used as confirmation; the client does not directly UPDATE role_participations.
     *
     * Supabase RPC `organisation_review_role_applicant`: Accepts or declines one pending REVIEW_APPLICANTS
     * application after checking ownership, role method, capacity and application lifecycle; decline carries
     * the organisation reason.
     */
    override suspend fun reviewApplicant(
        postId: String,
        roleTemplateId: String,
        userId: String,
        decision: String,
        decisionNote: String?
    ) {
        requireOwnedPost(postId)
        val normalizedDecision = decision.trim().uppercase(Locale.US)
        require(normalizedDecision in setOf("ACCEPT", "DECLINE")) {
            "Applicant decision must be ACCEPT or DECLINE."
        }

        val normalizedDecisionNote = decisionNote?.trim().orEmpty()
        if (normalizedDecision == "DECLINE") {
            require(normalizedDecisionNote.isNotBlank()) {
                "Enter a reason before declining this application."
            }
        }

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_review_role_applicant
        // Accepts or declines one pending REVIEW_APPLICANTS application after checking ownership, role method,
        // capacity and application lifecycle; decline carries the organisation reason.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_review_role_applicant",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_role_template_id", roleTemplateId)
                put("p_user_id", userId)
                put("p_decision", normalizedDecision)
                put(
                    "p_decision_note",
                    if (normalizedDecision == "DECLINE") normalizedDecisionNote else ""
                )
            }
        )
    }

    /**
     * Starts the physical attendance for the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — startPhysicalAttendance
     *
     * Performs the repository/data-layer operation for start physical attendance.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_start_physical_attendance`: Initialises/opens the Physical attendance workflow
     * for an owned post using server-validated event dates and participants.
     */
    override suspend fun startPhysicalAttendance(postId: String) {
        requireOwnedPost(postId)

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_start_physical_attendance
        // Initialises/opens the Physical attendance workflow for an owned post using server-validated event
        // dates and participants.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_start_physical_attendance",
            parameters = buildJsonObject {
                put("p_post_id", postId)
            }
        )
    }

    /**
     * Marks the volunteer present with its new state in the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — markVolunteerPresent
     *
     * Performs the repository/data-layer operation for mark volunteer present.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_mark_physical_present`: Records one volunteer as present for the selected
     * Physical attendance day using the server-side attendance rules.
     */
    override suspend fun markVolunteerPresent(
        postId: String,
        eventDate: String,
        roleTemplateId: String,
        userId: String
    ) {
        requireOwnedPost(postId)

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_mark_physical_present
        // Records one volunteer as present for the selected Physical attendance day using the server-side
        // attendance rules.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
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

    /**
     * Marks the volunteer absent with its new state in the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — markVolunteerAbsent
     *
     * Performs the repository/data-layer operation for mark volunteer absent.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_mark_physical_absent`: Records one volunteer as absent for the selected
     * Physical attendance day using the server-side attendance rules.
     */
    override suspend fun markVolunteerAbsent(
        postId: String,
        eventDate: String,
        roleTemplateId: String,
        userId: String
    ) {
        requireOwnedPost(postId)

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_mark_physical_absent
        // Records one volunteer as absent for the selected Physical attendance day using the server-side
        // attendance rules.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
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

    /**
     * Prepares the physical review for the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — preparePhysicalReview
     *
     * Performs the repository/data-layer operation for prepare physical review.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_prepare_physical_review`: Builds the Physical completion/review state after
     * attendance so the organisation reviews consistent server-derived eligibility.
     */
    override suspend fun preparePhysicalReview(postId: String) {
        requireOwnedPost(postId)
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_prepare_physical_review
        // Builds the Physical completion/review state after attendance so the organisation reviews consistent
        // server-derived eligibility.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_prepare_physical_review",
            parameters = buildJsonObject { put("p_post_id", postId) }
        )
    }

    /**
     * Derives the report physical review issue value used by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — reportPhysicalReviewIssue
     *
     * Performs the repository/data-layer operation for report physical review issue.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_report_physical_review_issue`: Records a Physical review issue that prevents
     * automatic finalisation and preserves the reason for follow-up.
     */
    override suspend fun reportPhysicalReviewIssue(
        postId: String,
        roleTemplateId: String,
        userId: String,
        reason: String
    ) {
        requireOwnedPost(postId)
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_report_physical_review_issue
        // Records a Physical review issue that prevents automatic finalisation and preserves the reason for
        // follow-up.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
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

    /**
     * Confirms the all ready physical in the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — completeAllReadyPhysical
     *
     * Performs the repository/data-layer operation for complete all ready physical.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_complete_all_ready_physical`: Finalises all Physical volunteers that currently
     * satisfy the database readiness rules in one server-controlled operation.
     */
    override suspend fun completeAllReadyPhysical(postId: String) {
        requireOwnedPost(postId)
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_complete_all_ready_physical
        // Finalises all Physical volunteers that currently satisfy the database readiness rules in one server-
        // controlled operation.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_complete_all_ready_physical",
            parameters = buildJsonObject { put("p_post_id", postId) }
        )
    }

    /**
     * Finalises the physical volunteer for the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — finalizePhysicalVolunteer
     *
     * Performs the repository/data-layer operation for finalize physical volunteer.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_finalize_physical_volunteer`: Finalises one Physical volunteer's
     * participation/completion result with ownership and lifecycle checks.
     */
    override suspend fun finalizePhysicalVolunteer(
        postId: String,
        roleTemplateId: String,
        userId: String,
        decision: String,
        note: String?
    ) {
        requireOwnedPost(postId)
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_finalize_physical_volunteer
        // Finalises one Physical volunteer's participation/completion result with ownership and lifecycle
        // checks.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
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

    /**
     * Saves the physical feedback for the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — savePhysicalFeedback
     *
     * Performs the repository/data-layer operation for save physical feedback.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_save_physical_feedback`: Persists Organisation feedback/evaluation data for a
     * Physical participation without letting the UI update evaluation tables directly.
     */
    override suspend fun savePhysicalFeedback(
        postId: String,
        userIds: List<String>,
        feedback: String,
        replaceExisting: Boolean
    ) {
        requireOwnedPost(postId)
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_save_physical_feedback
        // Persists Organisation feedback/evaluation data for a Physical participation without letting the UI
        // update evaluation tables directly.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
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

    /**
     * Finalises the physical review post for the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — finalizePhysicalReviewPost
     *
     * Performs the repository/data-layer operation for finalize physical review post.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_finalize_physical_review_post`: Finalises the post-level Physical review state
     * after required participant decisions are complete.
     */
    override suspend fun finalizePhysicalReviewPost(postId: String) {
        requireOwnedPost(postId)
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_finalize_physical_review_post
        // Finalises the post-level Physical review state after required participant decisions are complete.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_finalize_physical_review_post",
            parameters = buildJsonObject { put("p_post_id", postId) }
        )
    }

    /**
     * Finalises the physical review batch for the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — finalizePhysicalReviewBatch
     *
     * Performs the repository/data-layer operation for finalize physical review batch.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_finalize_physical_review_batch`: Completes a Physical review batch atomically
     * so the final post/participant states remain consistent.
     */
    override suspend fun finalizePhysicalReviewBatch(
        postId: String,
        decisions: List<PostManagementPendingReviewDecision>,
        feedbackByUserId: Map<String, String>
    ) {
        requireOwnedPost(postId)

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_finalize_physical_review_batch
        // Completes a Physical review batch atomically so the final post/participant states remain consistent.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
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
                                // value too so every batch review save keeps the selected decision consistently.
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

    /**
     * Derives the require owned post value used by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — requireOwnedPost
     *
     * Performs the repository/data-layer operation for require owned post.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `volunteer_posts` (the parent Volunteer Post record, including owner,
     * mode, lifecycle status, category and publication metadata).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     */
    private suspend fun requireOwnedPost(postId: String) {
        val organisationId = OrganisationSession.requireOrganisationId()
        val ownsPost = supabase
            // SUPABASE TABLE: volunteer_posts — the parent Volunteer Post record, including owner, mode, lifecycle status, category and publication metadata.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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


    /**
     * Derives the json object value used by the organisation Manage Post flow.
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
            ?: error("Missing required Supabase field: $key")
    }

    /**
     * Derives the json object value used by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — optionalText
     *
     * Performs the repository/data-layer operation for optional text.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private fun JsonObject.optionalText(key: String): String? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        return runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()
    }

    /**
     * Derives the json object value used by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — optionalBoolean
     *
     * Performs the repository/data-layer operation for optional boolean.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private fun JsonObject.optionalBoolean(key: String): Boolean? {
        val element: JsonElement = this[key] ?: return null
        if (element is JsonNull) return null
        return runCatching { element.jsonPrimitive.booleanOrNull }.getOrNull()
    }

    /**
     * Derives the json object value used by the organisation Manage Post flow.
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
     * Derives the json object value used by the organisation Manage Post flow.
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
        val value = this[key]
            ?.takeUnless { it is JsonNull }
            ?.jsonPrimitive
            ?.intOrNull
        return value ?: error("Missing required Supabase integer field: $key")
    }
}
