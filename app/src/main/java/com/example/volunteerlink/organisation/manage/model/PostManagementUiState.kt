package com.example.volunteerlink.organisation.manage.model

import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.data.post.RoleApplicationCutoffReason
import com.example.volunteerlink.data.post.RoleApplicationWindowState
import kotlinx.serialization.Serializable

/** State for one Volunteer Post opened from Organisation > Manage > Volunteer Posts. */
@Serializable
data class OrganisationPostManagementUiState(
    val isLoading: Boolean = true,
    val post: PostManagementPost? = null,
    val isShowingCachedData: Boolean = false,
    val lastSyncedAtEpochMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isStartingAttendance: Boolean = false,
    val isUpdatingAttendance: Boolean = false,
    val attendanceActionMessage: String? = null,
    val isUpdatingReview: Boolean = false,
    val reviewActionMessage: String? = null,
    /** True only after the final batch RPC has committed successfully. */
    val reviewFinalizeSucceeded: Boolean = false,
    val physicalReviewSession: PostManagementPhysicalReviewSession = PostManagementPhysicalReviewSession(),
    val isUpdatingRemoteReview: Boolean = false,
    val remoteReviewActionMessage: String? = null,
    val remoteReviewFinalizeSucceeded: Boolean = false,
    val isUpdatingApplicant: Boolean = false,
    val applicantActionMessage: String? = null,
    val isLoadingPostGroup: Boolean = false,
    val isAddingPostGroupMembers: Boolean = false,
    val postGroupConversationId: String? = null,
    val postGroupEligibleCount: Int = 0,
    val postGroupActiveMemberCount: Int = 0,
    val postGroupMissingCount: Int = 0,
    val postGroupHasStarted: Boolean = false,
    val postGroupCanAdd: Boolean = false,
    val postGroupActionMessage: String? = null,
    val remoteReviewSession: PostManagementRemoteReviewSession = PostManagementRemoteReviewSession()
)

/**
 * One post-management snapshot. The repository loads normalized database rows;
 * the ViewModel only adds the time-dependent lifecycle values.
 */
@Serializable
data class PostManagementPost(
    val postId: String,
    val organisationName: String,
    val title: String,
    val description: String,
    val mode: String,
    val databaseStatus: String,
    val category: String? = null,
    val impactWeaveDraftId: String? = null,
    val impactWeavePartners: List<PostManagementImpactWeavePartner> = emptyList(),
    val physical: PostManagementPhysicalDetails? = null,
    val remote: PostManagementRemoteDetails? = null,
    val schedules: List<PostManagementScheduleItem> = emptyList(),
    val roles: List<PostManagementRole> = emptyList(),
    val people: List<PostManagementPerson> = emptyList(),
    val attendanceDays: List<PostManagementAttendanceDay> = emptyList(),
    val attendanceRecords: List<PostManagementAttendanceRecord> = emptyList(),
    val remoteSubmissions: List<PostManagementRemoteSubmission> = emptyList(),
    val evaluations: List<PostManagementEvaluation> = emptyList(),
    val physicalAttendance: PostManagementPhysicalAttendance? = null,
    val physicalReview: PostManagementPhysicalReview? = null,
    val remoteReview: PostManagementRemoteReview? = null,
    val timingState: PostTimingState? = null,
    val physicalTimingState: PostTimingState? = null,
    val remoteTimingState: PostTimingState? = null
) {
    val applicants: List<PostManagementPerson>
        get() = people.filter {
            it.applicationStatus.equals("PENDING", ignoreCase = true)
        }

    val volunteers: List<PostManagementPerson>
        get() = people.filter {
            it.applicationStatus.equals("ACCEPTED", ignoreCase = true)
        }
}

@Serializable
data class PostManagementImpactWeavePartner(
    val organisationId: String,
    val organisationName: String,
    val contributions: List<PostManagementImpactWeaveContribution> = emptyList()
)

@Serializable
data class PostManagementImpactWeaveContribution(
    val supportType: String,
    val needResourceName: String,
    val providerResourceName: String? = null,
    val quantityProvided: Int? = null,
    val capacityProvided: Int? = null
)

@Serializable
data class PostManagementPhysicalDetails(
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val endTime: String,
    val locationName: String,
    val locationAddress: String? = null,
    val meetingPoint: String? = null,
    val volunteerCapacity: Int,
    val timeZone: String? = null
)

@Serializable
data class PostManagementRemoteDetails(
    val startDate: String,
    val endDate: String,
    val newEndDate: String? = null,
    val volunteerCapacity: Int,
    val submissionMode: String,
    val sharedDeliverable: String? = null,
    val responsibleRoleTemplateId: String? = null
) {
    val effectiveEndDate: String
        get() = newEndDate?.takeIf { it.isNotBlank() } ?: endDate
}

/** One Remote deliverable submission. Shared submissions belong to the post; individual submissions belong to one volunteer. */
@Serializable
data class PostManagementRemoteSubmission(
    val submissionId: String,
    val roleTemplateId: String? = null,
    val userId: String? = null,
    val submissionType: String,
    val filePath: String? = null,
    val submissionUrl: String? = null,
    val status: String,
    val feedback: String? = null,
    val submittedAt: String? = null,
    val reviewedAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class PostManagementScheduleItem(
    val scheduleItemId: String,
    val scheduleType: String,
    val scheduleDate: String,
    val title: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val location: String? = null,
    val notes: String? = null,
    val roleTemplateIds: List<String> = emptyList()
)

@Serializable
data class PostManagementRole(
    val roleTemplateId: String,
    val roleName: String,
    val roleMode: String,
    val defaultLevel: String,
    val capacity: Int,
    val applicationMethod: String,
    val roleNotes: String? = null,
    val individualSubmissionRequirement: String? = null,
    val applicationWindowState: RoleApplicationWindowState = RoleApplicationWindowState.OPEN,
    val applicationCutoffDate: String? = null,
    val applicationCutoffReason: RoleApplicationCutoffReason? = null
) {
    val isApplicationOpen: Boolean
        get() = applicationWindowState == RoleApplicationWindowState.OPEN
}


@Serializable
data class PostManagementPerson(
    val userId: String,
    val fullName: String,
    val city: String? = null,
    val bio: String? = null,
    val avatarPath: String? = null,
    val eventSharedPhone: String? = null,
    val eventPhoneContactUntilLabel: String? = null,
    val roleTemplateId: String,
    val roleName: String,
    val roleMode: String,
    val defaultLevel: String,
    val applicationStatus: String,
    val completionStatus: String,
    val joinedAt: String? = null,
    val completedAt: String? = null,
    val appliedAt: String? = null,
    val decisionNote: String? = null,
    val isShortlisted: Boolean = false,
    val screeningQuestions: List<String> = emptyList(),
    val screeningAnswers: List<String> = emptyList()
)

/** One attendance session opened by the organisation for one Physical event day. */
@Serializable
data class PostManagementAttendanceDay(
    val eventDate: String,
    val pinCode: String,
    val expectedMinutes: Int,
    val generatedAt: String? = null,
    val isActive: Boolean
)

/** One normalized volunteer check-in for one Physical role and event day. */
@Serializable
data class PostManagementAttendanceRecord(
    val eventDate: String,
    val roleTemplateId: String,
    val userId: String,
    val attendanceStatus: String,
    val checkedInAt: String? = null,
    val verifiedMinutes: Int
)

/** One final organisation review record for one volunteer participation. */
@Serializable
data class PostManagementEvaluation(
    val roleTemplateId: String,
    val userId: String,
    val organisationId: String,
    val feedback: String? = null,
    /** Required for a finalized NOT_COMPLETED participation. */
    val completionReason: String? = null,
    val createdAt: String? = null,
    /** Final credited minutes. Remote evaluations keep this null. */
    val verifiedMinutes: Int? = null
)

/** Small attendance-only payload used by the active People-screen poll. */
@Serializable
data class PostManagementAttendanceSnapshot(
    val attendanceDays: List<PostManagementAttendanceDay> = emptyList(),
    val attendanceRecords: List<PostManagementAttendanceRecord> = emptyList()
)

/** Time-dependent Physical attendance state prepared for the Manage UI. */
@Serializable
data class PostManagementPhysicalAttendance(
    val todayDate: String,
    val todaySession: PostManagementAttendanceDay? = null,
    val eligiblePhysicalVolunteerCount: Int = 0,
    val checkedInTodayCount: Int = 0,
    val canStartAttendance: Boolean = false,
    val canCorrectAttendance: Boolean = false,
    val isLiveWindowOpen: Boolean = false,
    val attendanceWindowLabel: String = "",
    val startBlockedReason: String? = null,
    val availableDates: List<String> = emptyList(),
    val defaultSelectedDate: String? = null,
    val volunteerSummaries: List<PostManagementVolunteerAttendanceSummary> = emptyList()
) {
    fun summaryFor(person: PostManagementPerson): PostManagementVolunteerAttendanceSummary? {
        return volunteerSummaries.firstOrNull {
            it.userId == person.userId &&
                it.roleTemplateId == person.roleTemplateId
        }
    }
}

@Serializable
data class PostManagementVolunteerAttendanceSummary(
    val userId: String,
    val roleTemplateId: String,
    val attendedDays: Int,
    val expectedDays: Int,
    val verifiedMinutes: Int,
    val expectedToday: Boolean,
    val checkedInToday: Boolean,
    val dateStatuses: List<PostManagementVolunteerAttendanceDateStatus> = emptyList()
) {
    fun statusFor(eventDate: String): PostManagementVolunteerAttendanceDateStatus? {
        return dateStatuses.firstOrNull { it.eventDate == eventDate }
    }
}

@Serializable
data class PostManagementVolunteerAttendanceDateStatus(
    val eventDate: String,
    val expected: Boolean,
    val present: Boolean,
    /** True only when an explicit ABSENT row exists in attendance_records. */
    val markedAbsent: Boolean = false,
    /** Past expected date with no attendance row. Review treats this as Absent · 0h. */
    val inferredAbsent: Boolean = false,
    val checkedInAt: String? = null,
    val verifiedMinutes: Int = 0
)

/** One accepted Physical volunteer in the Physical close-out flow. Attendance determines verified time, not completion by itself. */
@Serializable
data class PostManagementPhysicalReviewEntry(
    val person: PostManagementPerson,
    val attendanceSummary: PostManagementVolunteerAttendanceSummary,
    val absentDays: Int,
    /** Missing past check-ins are inferred as Absent; they remain visible for optional correction. */
    val missingCheckInDays: Int,
    val hasPerformanceIssue: Boolean,
    val performanceIssueText: String? = null,
    val feedback: String? = null,
    val completionReason: String? = null,
    val verifiedMinutes: Int? = null
) {
    val isFinalized: Boolean
        get() = person.completionStatus.equals("COMPLETED", true) ||
            person.completionStatus.equals("NOT_COMPLETED", true)

    val hasFeedback: Boolean
        get() = !feedback.isNullOrBlank()
}

/** Feedback groups are reconstructed from equal feedback text; no group table is stored. */
@Serializable
data class PostManagementFeedbackGroup(
    val feedback: String,
    val userIds: List<String>,
    val recipientNames: List<String>
) {
    val recipientCount: Int
        get() = userIds.size
}

/** Physical close-out state. `ready` means full attendance/no flagged issue and is only a quick-completion candidate, not automatically completed. */
@Serializable
data class PostManagementPhysicalReview(
    val ready: List<PostManagementPhysicalReviewEntry> = emptyList(),
    val needsReview: List<PostManagementPhysicalReviewEntry> = emptyList(),
    val completed: List<PostManagementPhysicalReviewEntry> = emptyList(),
    val notCompleted: List<PostManagementPhysicalReviewEntry> = emptyList(),
    val feedbackGroups: List<PostManagementFeedbackGroup> = emptyList(),
    val completedWithoutFeedback: List<PostManagementPhysicalReviewEntry> = emptyList(),
    val canEdit: Boolean = true
) {
    val readyCount: Int get() = ready.size
    val needsReviewCount: Int get() = needsReview.size
    val completedCount: Int get() = completed.size
    val notCompletedCount: Int get() = notCompleted.size
    val unresolvedCount: Int get() = readyCount + needsReviewCount
}


/** Temporary organisation choices for an ended Physical event.
 *
 * These values deliberately live only in the ViewModel. They are not written to
 * Supabase until the organisation presses Finalize Event.
 */
@Serializable
enum class PostManagementPhysicalReviewStage {
    ATTENDANCE,
    COMPLETION,
    FEEDBACK,
    FINISH
}

@Serializable
enum class PostManagementPendingDecisionType {
    COMPLETED,
    NOT_COMPLETED
}

@Serializable
enum class PostManagementPendingDecisionSource {
    FULL_ATTENDANCE,
    PARTIAL_ATTENDANCE,
    WORK_ISSUE
}

@Serializable
data class PostManagementPendingReviewDecision(
    val roleTemplateId: String,
    val userId: String,
    val decision: PostManagementPendingDecisionType,
    val reason: String? = null,
    val source: PostManagementPendingDecisionSource
)

@Serializable
data class PostManagementPhysicalReviewSession(
    val stage: PostManagementPhysicalReviewStage = PostManagementPhysicalReviewStage.ATTENDANCE,
    val decisions: List<PostManagementPendingReviewDecision> = emptyList(),
    val feedbackByUserId: Map<String, String> = emptyMap(),
    /** True only while the organisation has an uncommitted review text draft open.
     * Saved attendance changes are already persisted and must never trigger a discard warning.
     */
    val touched: Boolean = false
) {
    val hasUnfinishedReview: Boolean
        get() = touched || decisions.isNotEmpty() || feedbackByUserId.isNotEmpty()

    fun decisionFor(roleTemplateId: String, userId: String): PostManagementPendingReviewDecision? {
        return decisions.firstOrNull {
            it.roleTemplateId == roleTemplateId && it.userId == userId
        }
    }
}


/** Remote close-out uses submitted work rather than Physical attendance. */
@Serializable
enum class PostManagementRemoteReviewStage {
    SUBMISSION,
    FEEDBACK,
    FINISH
}

@Serializable
enum class PostManagementRemoteSubmissionDecisionType {
    ACCEPT,
    REQUEST_REVISION,
    NOT_ACCEPT
}

@Serializable
enum class PostManagementRemoteMissingAction {
    GIVE_MORE_TIME,
    CONTINUE_WITHOUT_WORK
}

/**
 * One persisted action for Remote work that is still missing after the deadline.
 * Shared Team work is represented by null role/user because remote_details already
 * tells the database that the post uses SHARED_TEAM; submission mode is not duplicated.
 */
@Serializable
data class PostManagementRemoteMissingDecision(
    val roleTemplateId: String? = null,
    val userId: String? = null,
    val action: PostManagementRemoteMissingAction
)

@Serializable
data class PostManagementRemoteSubmissionDecision(
    val itemKey: String,
    val submissionId: String,
    val decision: PostManagementRemoteSubmissionDecisionType,
    val feedback: String? = null
)


/** One submission stream shown in Remote > Needs Review. */
@Serializable
data class PostManagementRemoteReviewItem(
    val itemKey: String,
    val submissionType: String,
    val roleTemplateId: String? = null,
    val userId: String? = null,
    val person: PostManagementPerson? = null,
    val roleName: String,
    val requirement: String? = null,
    val latestSubmission: PostManagementRemoteSubmission? = null,
    val submittedByName: String? = null,
    val isResubmission: Boolean = false
) {
    val isShared: Boolean
        get() = submissionType.equals("SHARED", ignoreCase = true)

    val currentStatus: String
        get() = latestSubmission?.status ?: "NOT_SUBMITTED"
}

/** Derived Remote review data. No review history is duplicated here. */
@Serializable
data class PostManagementRemoteReview(
    val todayDate: String,
    val currentDeadline: String,
    val submissionMode: String,
    val items: List<PostManagementRemoteReviewItem> = emptyList(),
    val participants: List<PostManagementPerson> = emptyList(),
    val canEdit: Boolean = true
) {
    fun itemFor(person: PostManagementPerson): PostManagementRemoteReviewItem? {
        return if (submissionMode.equals("SHARED_TEAM", ignoreCase = true)) {
            items.firstOrNull { it.isShared }
        } else {
            items.firstOrNull {
                !it.isShared &&
                    it.roleTemplateId == person.roleTemplateId &&
                    it.userId == person.userId
            }
        }
    }

}

/**
 * Temporary Remote review choices.
 *
 * Submission decisions are persisted when the Submission stage is saved. At that
 * same save, accepted work becomes COMPLETED and rejected/missing work becomes
 * NOT_COMPLETED. Only optional final feedback remains until Finish.
 */
@Serializable
data class PostManagementRemoteReviewSession(
    val stage: PostManagementRemoteReviewStage = PostManagementRemoteReviewStage.SUBMISSION,
    val submissionDecisions: List<PostManagementRemoteSubmissionDecision> = emptyList(),
    val missingActions: Map<String, PostManagementRemoteMissingAction> = emptyMap(),
    val newEndDate: String? = null,
    /** Key format is roleTemplateId::userId to avoid ambiguity if one user has multiple roles. */
    val feedbackByParticipation: Map<String, String> = emptyMap(),
    val touched: Boolean = false
) {
    val hasUnfinishedReview: Boolean
        get() = touched ||
            submissionDecisions.isNotEmpty() ||
            missingActions.isNotEmpty() ||
            !newEndDate.isNullOrBlank() ||
            feedbackByParticipation.isNotEmpty()

    fun submissionDecisionFor(itemKey: String): PostManagementRemoteSubmissionDecision? =
        submissionDecisions.firstOrNull { it.itemKey == itemKey }

    fun missingActionFor(itemKey: String): PostManagementRemoteMissingAction? =
        missingActions[itemKey]
}

fun remoteReviewParticipationKey(roleTemplateId: String, userId: String): String =
    "$roleTemplateId::$userId"
