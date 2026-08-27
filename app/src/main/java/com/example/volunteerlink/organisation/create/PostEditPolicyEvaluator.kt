package com.example.volunteerlink.organisation.create

import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import java.util.Calendar

/**
 * Central Edit Post rules.
 *
 * The UI consumes this policy, but the same rules are checked again before
 * Save Changes so a stale screen cannot overwrite newer volunteer activity.
 */
data class PostEditPolicy(
    val postStatus: String,
    val isReadOnly: Boolean,
    val readOnlyReason: String? = null,
    val canEditSharedPostInfo: Boolean = true,
    val canEditPhysicalCore: Boolean = true,
    val canEditPhysicalMeetingPoint: Boolean = true,
    val canEditPhysicalCapacity: Boolean = true,
    val canEditRemoteStart: Boolean = true,
    val canEditRemoteDueDate: Boolean = true,
    val minimumRemoteDueDateMillis: Long? = null,
    val remoteDueDateLockedReason: String? = null,
    val canEditRemoteCapacity: Boolean = true,
    val canEditRemoteSubmissionSetup: Boolean = true,
    val canAddPhysicalRole: Boolean = true,
    val canAddRemoteRole: Boolean = true,
    val canAddPhysicalSchedule: Boolean = true,
    val canAddRemoteSchedule: Boolean = true,
    val rolePolicies: Map<String, RoleEditPolicy> = emptyMap(),
    val schedulePolicies: Map<String, ScheduleEditPolicy> = emptyMap()
)

data class RoleEditPolicy(
    val roleTemplateId: String,
    val roleMode: VolunteerRoleMode,
    val hasApplicationHistory: Boolean,
    val hasPendingApplications: Boolean,
    val hasAcceptedOrJoinedHistory: Boolean,
    val hasScreeningAnswerHistory: Boolean,
    val acceptedCount: Int,
    val canRemove: Boolean,
    val canChangeCapacity: Boolean,
    val minimumCapacity: Int,
    val canChangeApplicationMethod: Boolean,
    val canChangeSkills: Boolean,
    val canChangeResponsibilities: Boolean,
    val canChangeScreeningQuestions: Boolean,
    val canChangeIndividualDeliverable: Boolean,
    val canChangeRoleNotes: Boolean,
    val selectionLockedReason: String? = null,
    val settingsLockedReason: String? = null
)

data class ScheduleEditPolicy(
    val scheduleItemId: String,
    val canEdit: Boolean,
    val canRemove: Boolean,
    val reason: String? = null
)

/** Raw dependency facts loaded from normalized Supabase tables. */
data class PostEditPolicyInput(
    val postStatus: String,
    val physicalStartDateMillis: Long? = null,
    val physicalEndDateMillis: Long? = null,
    val remoteStartDateMillis: Long? = null,
    val remoteEndDateMillis: Long? = null,
    val roles: List<PostEditRoleInput> = emptyList(),
    val participations: List<PostEditParticipationInput> = emptyList(),
    val schedules: List<PostEditScheduleInput> = emptyList(),
    val attendanceDatesMillis: List<Long> = emptyList(),
    val remoteSubmissionRoleIds: Set<String> = emptySet(),
    val hasAnyRemoteSubmission: Boolean = false,
    val hasSharedRemoteSubmission: Boolean = false,
    val completedHistoryRoleIds: Set<String> = emptySet(),
    val screeningAnswerRoleIds: Set<String> = emptySet()
)

data class PostEditRoleInput(
    val roleTemplateId: String,
    val roleMode: VolunteerRoleMode,
    val hasConfiguredScreeningQuestions: Boolean = false
)

data class PostEditParticipationInput(
    val roleTemplateId: String,
    val applicationStatus: String,
    val joinedAt: String? = null
)

data class PostEditScheduleInput(
    val scheduleItemId: String,
    val scheduleType: ScheduleType,
    val scheduleDateMillis: Long
)

object PostEditPolicyEvaluator {

    fun evaluate(
        input: PostEditPolicyInput,
        nowMillis: Long = AppClock.nowMillis()
    ): PostEditPolicy {
        val status = input.postStatus.uppercase()
        val nowDay = startOfDay(nowMillis)
        val draft = status == "DRAFT"
        val completedOrCancelled = status == "COMPLETED" || status == "CANCELLED"
        val closed = status == "CLOSED"

        if (completedOrCancelled) {
            val reason = if (status == "COMPLETED") {
                "Completed posts are historical records and cannot be edited."
            } else {
                "Cancelled posts are kept as historical records and cannot be edited."
            }
            return readOnlyPolicy(input, reason)
        }

        val physicalUpcoming = input.physicalStartDateMillis?.let {
            nowDay < startOfDay(it)
        } ?: false
        val remoteUpcoming = input.remoteStartDateMillis?.let {
            nowDay < startOfDay(it)
        } ?: false

        val anyStarted = listOfNotNull(
            input.physicalStartDateMillis,
            input.remoteStartDateMillis
        ).any { nowDay >= startOfDay(it) }

        val participationByRole = input.participations.groupBy { it.roleTemplateId }
        val roleById = input.roles.associateBy { it.roleTemplateId }

        fun sideHasActivePeople(mode: VolunteerRoleMode): Boolean {
            val roleIds = input.roles
                .filter { it.roleMode == mode }
                .map { it.roleTemplateId }
                .toSet()

            return input.participations.any { participation ->
                participation.roleTemplateId in roleIds &&
                    participation.applicationStatus.uppercase() in setOf("PENDING", "ACCEPTED")
            }
        }

        fun sideHasParticipationDependency(mode: VolunteerRoleMode): Boolean {
            val roleIds = input.roles
                .filter { it.roleMode == mode }
                .map { it.roleTemplateId }
                .toSet()

            return input.participations.any { participation ->
                participation.roleTemplateId in roleIds && (
                    participation.applicationStatus.uppercase() in setOf("PENDING", "ACCEPTED") ||
                        !participation.joinedAt.isNullOrBlank()
                    )
            }
        }

        val physicalActivePeople = sideHasActivePeople(VolunteerRoleMode.PHYSICAL)
        val remoteActivePeople = sideHasActivePeople(VolunteerRoleMode.REMOTE)
        val remoteParticipationDependency = sideHasParticipationDependency(VolunteerRoleMode.REMOTE)
        val remoteHasSubmissions = input.hasAnyRemoteSubmission
        val remoteHasSharedSubmission = input.hasSharedRemoteSubmission

        val hasCommittedRemoteVolunteer = input.participations.any { participation ->
            val role = roleById[participation.roleTemplateId]
            role?.roleMode == VolunteerRoleMode.REMOTE &&
                (participation.applicationStatus.equals("ACCEPTED", ignoreCase = true) ||
                    !participation.joinedAt.isNullOrBlank())
        }
        val remoteDueDateStillOpen = input.remoteEndDateMillis?.let {
            nowDay <= startOfDay(it)
        } == true
        val remoteDueDateMinimum = if (
            !draft && (hasCommittedRemoteVolunteer || (remoteHasSubmissions && !remoteHasSharedSubmission))
        ) {
            input.remoteEndDateMillis
        } else {
            null
        }
        val remoteDueDateLockedReason = when {
            draft -> null
            remoteHasSharedSubmission ->
                "A Shared Team deliverable has already been submitted. The post due date is locked; use submission review if the team needs a revision deadline."
            !remoteDueDateStillOpen ->
                "The Remote project due date has already passed."
            else -> null
        }

        val rolePolicies = input.roles.associate { role ->
            val history = participationByRole[role.roleTemplateId].orEmpty()
            val hasHistory = history.isNotEmpty()
            val hasPendingApplications = history.any {
                it.applicationStatus.equals("PENDING", ignoreCase = true)
            }
            val acceptedCount = history.count {
                it.applicationStatus.equals("ACCEPTED", ignoreCase = true)
            }
            val hasAcceptedOrJoinedHistory = history.any {
                it.applicationStatus.equals("ACCEPTED", ignoreCase = true) ||
                    !it.joinedAt.isNullOrBlank()
            }
            val hasRecruitmentDependency =
                hasPendingApplications || hasAcceptedOrJoinedHistory
            // Review-applicant questions are mandatory in the volunteer apply flow.
            // If RLS hides the answer rows from the organisation client, an existing
            // participation plus configured questions is still enough to protect
            // the method/questions from being rewritten.
            val hasScreeningAnswerHistory =
                role.roleTemplateId in input.screeningAnswerRoleIds ||
                    (hasHistory && role.hasConfiguredScreeningQuestions)
            val hasActivityHistory = role.roleTemplateId in input.completedHistoryRoleIds ||
                role.roleTemplateId in input.remoteSubmissionRoleIds

            val sideStart = when (role.roleMode) {
                VolunteerRoleMode.PHYSICAL -> input.physicalStartDateMillis
                VolunteerRoleMode.REMOTE -> input.remoteStartDateMillis
            }

            val applicationCutoff = sideStart
            val applicationOpen = draft || (
                !closed && applicationCutoff != null && nowDay < startOfDay(applicationCutoff)
            )
            val sideUpcoming = when (role.roleMode) {
                VolunteerRoleMode.PHYSICAL -> physicalUpcoming
                VolunteerRoleMode.REMOTE -> remoteUpcoming
            }

            val baseStructuralEdit = draft || (!closed && sideUpcoming)

            // Any participation row is still a historical record, so the role itself
            // cannot be deleted. Past-only DECLINED/CANCELLED applications no longer
            // freeze every setting, though: only active/accepted dependencies do.
            val canRemove = applicationOpen && !hasHistory
            val canChangeRecruitmentStructure = applicationOpen && !hasRecruitmentDependency
            val canChangeApplicationMethod =
                canChangeRecruitmentStructure && !hasScreeningAnswerHistory
            val canChangeScreeningQuestions =
                canChangeRecruitmentStructure && !hasScreeningAnswerHistory
            val canChangeCommitment =
                baseStructuralEdit &&
                    !hasPendingApplications &&
                    !hasAcceptedOrJoinedHistory &&
                    !hasActivityHistory
            val canChangeCapacity = draft || (!closed && applicationOpen)
            val canChangeNotes = draft || sideUpcoming

            val selectionReason = when {
                closed -> "Recruitment is closed for this post."
                !sideUpcoming && !draft ->
                    "This role's volunteering phase has already started."
                !applicationOpen && !draft ->
                    "Applications are already closed for this role."
                hasAcceptedOrJoinedHistory ->
                    "This role must stay. Capacity can change while applications are open, but never below $acceptedCount accepted volunteer(s)."
                hasPendingApplications ->
                    "This role must stay while applications are active. Capacity can still change while applications are open."
                hasHistory ->
                    "This role must stay to preserve application history. Other available settings can still be edited."
                else -> null
            }

            val settingsReason = when {
                closed ->
                    "Recruitment and commitment settings are locked because recruitment is closed."
                !sideUpcoming && !draft ->
                    "This role's volunteering phase has already started, so historical role settings are locked."
                hasAcceptedOrJoinedHistory ->
                    "Recruitment, responsibilities and deliverables are locked. Operational notes remain editable until the phase starts."
                hasPendingApplications ->
                    "Recruitment, responsibilities and deliverables are locked until current applications are resolved."
                hasActivityHistory ->
                    "Responsibilities and deliverables are locked to preserve volunteer activity history."
                !applicationOpen && !draft ->
                    "Recruitment settings are locked because applications have closed."
                hasScreeningAnswerHistory ->
                    "Application method and screening questions are locked to preserve previous answers."
                else -> null
            }

            role.roleTemplateId to RoleEditPolicy(
                roleTemplateId = role.roleTemplateId,
                roleMode = role.roleMode,
                hasApplicationHistory = hasHistory,
                hasPendingApplications = hasPendingApplications,
                hasAcceptedOrJoinedHistory = hasAcceptedOrJoinedHistory,
                hasScreeningAnswerHistory = hasScreeningAnswerHistory,
                acceptedCount = acceptedCount,
                canRemove = canRemove,
                canChangeCapacity = canChangeCapacity,
                minimumCapacity = acceptedCount.coerceAtLeast(1),
                canChangeApplicationMethod = canChangeApplicationMethod,
                canChangeSkills = canChangeRecruitmentStructure,
                canChangeResponsibilities = canChangeCommitment,
                canChangeScreeningQuestions = canChangeScreeningQuestions,
                canChangeIndividualDeliverable = canChangeCommitment,
                canChangeRoleNotes = canChangeNotes,
                selectionLockedReason = selectionReason,
                settingsLockedReason = settingsReason
            )
        }

        val attendanceDates = input.attendanceDatesMillis.map(::startOfDay).toSet()
        val schedulePolicies = input.schedules.associate { schedule ->
            val scheduleDay = startOfDay(schedule.scheduleDateMillis)
            val isPast = scheduleDay < nowDay
            val hasAttendanceOnDay = schedule.scheduleType == ScheduleType.PHYSICAL &&
                scheduleDay in attendanceDates
            val editable = draft || (!isPast && !hasAttendanceOnDay)

            val reason = when {
                hasAttendanceOnDay -> "Attendance already exists for this date."
                isPast -> "Past schedule items are historical records."
                else -> null
            }

            schedule.scheduleItemId to ScheduleEditPolicy(
                scheduleItemId = schedule.scheduleItemId,
                canEdit = editable,
                canRemove = editable,
                reason = reason
            )
        }

        // Step 1 stores the side-wide capacity, while Step 2 distributes that
        // total across roles. Once every role on a side has closed applications,
        // changing only the Step 1 total would create a number that the organiser
        // can no longer reconcile in Step 2. Keep the side capacity editable only
        // while at least one role capacity on that side is still legitimately open.
        val canAdjustPhysicalRoleCapacity = rolePolicies.values.any { policy ->
            policy.roleMode == VolunteerRoleMode.PHYSICAL && policy.canChangeCapacity
        }
        val canAdjustRemoteRoleCapacity = rolePolicies.values.any { policy ->
            policy.roleMode == VolunteerRoleMode.REMOTE && policy.canChangeCapacity
        }

        return PostEditPolicy(
            postStatus = status,
            isReadOnly = false,
            canEditSharedPostInfo = draft || (!closed && !anyStarted),
            canEditPhysicalCore = draft || (!closed && physicalUpcoming && !physicalActivePeople),
            canEditPhysicalMeetingPoint = draft || physicalUpcoming,
            canEditPhysicalCapacity = draft || (
                !closed && physicalUpcoming && canAdjustPhysicalRoleCapacity
            ),
            canEditRemoteStart = draft || (!closed && remoteUpcoming && !remoteActivePeople),
            // Global Remote deadline rules:
            // - no committed volunteer: move normally while the project is still open;
            // - accepted/joined volunteer or Individual submission: extension only;
            // - Shared Team submission: lock the post-wide due date and use the
            //   submission's revision deadline instead.
            canEditRemoteDueDate = draft || (!remoteHasSharedSubmission && remoteDueDateStillOpen),
            minimumRemoteDueDateMillis = remoteDueDateMinimum,
            remoteDueDateLockedReason = remoteDueDateLockedReason,
            canEditRemoteCapacity = draft || (
                !closed && remoteUpcoming && canAdjustRemoteRoleCapacity
            ),
            canEditRemoteSubmissionSetup = draft || (!closed && remoteUpcoming && !remoteParticipationDependency && !remoteHasSubmissions),
            canAddPhysicalRole = draft || (!closed && physicalUpcoming),
            canAddRemoteRole = draft || (!closed && remoteUpcoming),
            canAddPhysicalSchedule = draft || physicalUpcoming,
            canAddRemoteSchedule = draft || input.remoteEndDateMillis?.let { nowDay <= startOfDay(it) } == true,
            rolePolicies = rolePolicies,
            schedulePolicies = schedulePolicies
        )
    }

    /**
     * Final safety check against the latest database snapshot.
     * Locked values must remain byte-for-byte equivalent from the editor's
     * perspective; otherwise Save Changes is stopped before any write occurs.
     */
    fun validateChanges(
        original: CreatePostDraft,
        edited: CreatePostDraft,
        policy: PostEditPolicy
    ): String? {
        if (policy.isReadOnly && original != edited) {
            return policy.readOnlyReason ?: "This post is read-only."
        }

        if (original.postType != edited.postType && policy.postStatus != "DRAFT") {
            return "Post Type cannot be changed after the post has been published."
        }

        if (!policy.canEditSharedPostInfo && (
                original.category != edited.category ||
                    original.title != edited.title ||
                    original.description != edited.description ||
                    original.thumbnailUri != edited.thumbnailUri
            )) {
            return "Post information is locked because this opportunity has already started."
        }

        if (!policy.canEditPhysicalCore && physicalCoreChanged(original, edited)) {
            return "Physical date, time and location are locked because volunteers already depend on them or the activity has started."
        }
        if (!policy.canEditPhysicalMeetingPoint && original.meetingPoint != edited.meetingPoint) {
            return "The Physical meeting point can no longer be changed."
        }
        if (!policy.canEditPhysicalCapacity && physicalCapacityChanged(original, edited)) {
            return "Physical capacity can no longer be changed."
        }
        if (!policy.canEditRemoteStart && original.remoteStartDateMillis != edited.remoteStartDateMillis) {
            return "The Remote start date can no longer be changed."
        }
        if (!policy.canEditRemoteDueDate && original.remoteDueDateMillis != edited.remoteDueDateMillis) {
            return policy.remoteDueDateLockedReason
                ?: "The Remote due date can no longer be changed."
        }
        policy.minimumRemoteDueDateMillis?.let { minimum ->
            if (edited.remoteDueDateMillis != null && edited.remoteDueDateMillis < minimum) {
                return "The Remote due date can be extended, but it cannot be shortened after volunteers have joined or Individual work has been submitted."
            }
        }
        if (!policy.canEditRemoteCapacity && remoteCapacityChanged(original, edited)) {
            return "Remote capacity can no longer be changed."
        }
        if (!policy.canEditRemoteSubmissionSetup && (
                original.remoteSubmissionMode != edited.remoteSubmissionMode ||
                    original.sharedDeliverable != edited.sharedDeliverable ||
                    original.sharedSubmissionResponsibleRoleTemplateId != edited.sharedSubmissionResponsibleRoleTemplateId
            )) {
            return "Remote submission setup is locked because active Remote applicants, joined volunteers, or submitted work depend on it."
        }

        val originalRoles = original.selectedRoles.associateBy { it.roleTemplateId }
        val editedRoles = edited.selectedRoles.associateBy { it.roleTemplateId }

        for ((roleId, originalRole) in originalRoles) {
            val rolePolicy = policy.rolePolicies[roleId] ?: continue
            val editedRole = editedRoles[roleId]
            if (editedRole == null) {
                if (!rolePolicy.canRemove) {
                    return "${roleId} cannot be removed because past or current application/volunteer records depend on it."
                }
                continue
            }

            if (!rolePolicy.canChangeCapacity && originalRole.capacity != editedRole.capacity) {
                return "Capacity for ${roleId} is locked because applications are closed."
            }
            if (editedRole.capacity < rolePolicy.minimumCapacity) {
                return "Capacity for ${roleId} cannot be lower than ${rolePolicy.minimumCapacity} accepted volunteer(s)."
            }
            if (!rolePolicy.canChangeApplicationMethod && originalRole.applicationMethod != editedRole.applicationMethod) {
                return "Application method for ${roleId} is locked because active applicants, joined volunteers, or historical screening answers depend on it."
            }
            if (!rolePolicy.canChangeSkills && (
                    originalRole.practisedSkillIds != editedRole.practisedSkillIds ||
                        originalRole.requiredSkillExperience != editedRole.requiredSkillExperience
                )) {
                return "Skills for ${roleId} are locked because active applicants or joined volunteers depend on them."
            }
            if (!rolePolicy.canChangeResponsibilities && originalRole.responsibilities != editedRole.responsibilities) {
                return "Responsibilities for ${roleId} are locked because an active applicant, joined volunteer, or activity history depends on them."
            }
            if (!rolePolicy.canChangeScreeningQuestions && originalRole.screeningQuestions != editedRole.screeningQuestions) {
                return "Screening questions for ${roleId} are locked because active applicants or historical answers depend on them."
            }
            if (!rolePolicy.canChangeIndividualDeliverable &&
                originalRole.individualSubmissionRequirement != editedRole.individualSubmissionRequirement
            ) {
                return "The Remote deliverable for ${roleId} is locked because an active applicant, joined volunteer, or submitted work depends on it."
            }
            if (!rolePolicy.canChangeRoleNotes && originalRole.roleNotes != editedRole.roleNotes) {
                return "Role notes for ${roleId} can no longer be changed because its volunteering phase has started."
            }
        }

        val originalSchedules = original.scheduleItems.associateBy { it.draftId }
        val editedSchedules = edited.scheduleItems.associateBy { it.draftId }
        for ((scheduleId, originalItem) in originalSchedules) {
            val schedulePolicy = policy.schedulePolicies[scheduleId] ?: continue
            val editedItem = editedSchedules[scheduleId]
            if (editedItem == null) {
                if (!schedulePolicy.canRemove) {
                    return schedulePolicy.reason ?: "This schedule item can no longer be removed."
                }
            } else if (!schedulePolicy.canEdit && originalItem != editedItem) {
                return schedulePolicy.reason ?: "This schedule item can no longer be changed."
            }
        }

        return null
    }

    private fun readOnlyPolicy(
        input: PostEditPolicyInput,
        reason: String
    ): PostEditPolicy {
        return PostEditPolicy(
            postStatus = input.postStatus.uppercase(),
            isReadOnly = true,
            readOnlyReason = reason,
            canEditSharedPostInfo = false,
            canEditPhysicalCore = false,
            canEditPhysicalMeetingPoint = false,
            canEditPhysicalCapacity = false,
            canEditRemoteStart = false,
            canEditRemoteDueDate = false,
            remoteDueDateLockedReason = reason,
            canEditRemoteCapacity = false,
            canEditRemoteSubmissionSetup = false,
            canAddPhysicalRole = false,
            canAddRemoteRole = false,
            canAddPhysicalSchedule = false,
            canAddRemoteSchedule = false,
            rolePolicies = input.roles.associate { role ->
                role.roleTemplateId to RoleEditPolicy(
                    roleTemplateId = role.roleTemplateId,
                    roleMode = role.roleMode,
                    hasApplicationHistory = input.participations.any {
                        it.roleTemplateId == role.roleTemplateId
                    },
                    hasPendingApplications = input.participations.any {
                        it.roleTemplateId == role.roleTemplateId &&
                            it.applicationStatus.equals("PENDING", ignoreCase = true)
                    },
                    hasAcceptedOrJoinedHistory = input.participations.any {
                        it.roleTemplateId == role.roleTemplateId && (
                            it.applicationStatus.equals("ACCEPTED", ignoreCase = true) ||
                                !it.joinedAt.isNullOrBlank()
                            )
                    },
                    hasScreeningAnswerHistory =
                        role.roleTemplateId in input.screeningAnswerRoleIds,
                    acceptedCount = input.participations.count {
                        it.roleTemplateId == role.roleTemplateId &&
                            it.applicationStatus.equals("ACCEPTED", ignoreCase = true)
                    },
                    canRemove = false,
                    canChangeCapacity = false,
                    minimumCapacity = 1,
                    canChangeApplicationMethod = false,
                    canChangeSkills = false,
                    canChangeResponsibilities = false,
                    canChangeScreeningQuestions = false,
                    canChangeIndividualDeliverable = false,
                    canChangeRoleNotes = false,
                    selectionLockedReason = reason,
                    settingsLockedReason = reason
                )
            },
            schedulePolicies = input.schedules.associate { schedule ->
                schedule.scheduleItemId to ScheduleEditPolicy(
                    scheduleItemId = schedule.scheduleItemId,
                    canEdit = false,
                    canRemove = false,
                    reason = reason
                )
            }
        )
    }

    private fun physicalCoreChanged(a: CreatePostDraft, b: CreatePostDraft): Boolean {
        return a.isMultiDayPhysicalEvent != b.isMultiDayPhysicalEvent ||
            a.physicalStartDateMillis != b.physicalStartDateMillis ||
            a.physicalEndDateMillis != b.physicalEndDateMillis ||
            a.physicalStartTimeMinutes != b.physicalStartTimeMinutes ||
            a.physicalEndTimeMinutes != b.physicalEndTimeMinutes ||
            a.physicalLocationQuery != b.physicalLocationQuery ||
            a.physicalLocation != b.physicalLocation
    }

    private fun physicalCapacityChanged(a: CreatePostDraft, b: CreatePostDraft): Boolean {
        return a.physicalVolunteerCapacity != b.physicalVolunteerCapacity ||
            a.hybridPhysicalVolunteerCapacity != b.hybridPhysicalVolunteerCapacity
    }

    private fun remoteCapacityChanged(a: CreatePostDraft, b: CreatePostDraft): Boolean {
        return a.remoteVolunteerCapacity != b.remoteVolunteerCapacity ||
            a.hybridRemoteVolunteerCapacity != b.hybridRemoteVolunteerCapacity
    }

    private fun startOfDay(value: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = value
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
