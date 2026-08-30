package com.example.volunteerlink.organisation.manage.model

import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.data.post.RoleApplicationCutoffReason
import com.example.volunteerlink.data.post.RoleApplicationWindowState

/** State for one Volunteer Post opened from Organisation > Manage > Volunteer Posts. */
data class OrganisationPostManagementUiState(
    val isLoading: Boolean = true,
    val post: PostManagementPost? = null,
    val errorMessage: String? = null,
    val isStartingAttendance: Boolean = false,
    val isUpdatingAttendance: Boolean = false,
    val attendanceActionMessage: String? = null,
    val isUpdatingReview: Boolean = false,
    val reviewActionMessage: String? = null,
    /** True only after the final batch RPC has committed successfully. */
    val reviewFinalizeSucceeded: Boolean = false,
    val physicalReviewSession: PostManagementPhysicalReviewSession = PostManagementPhysicalReviewSession()
)

/**
 * One post-management snapshot. The repository loads normalized database rows;
 * the ViewModel only adds the time-dependent lifecycle values.
 */
data class PostManagementPost(
    val postId: String,
    val title: String,
    val description: String,
    val mode: String,
    val databaseStatus: String,
    val category: String? = null,
    val physical: PostManagementPhysicalDetails? = null,
    val remote: PostManagementRemoteDetails? = null,
    val schedules: List<PostManagementScheduleItem> = emptyList(),
    val roles: List<PostManagementRole> = emptyList(),
    val people: List<PostManagementPerson> = emptyList(),
    val attendanceDays: List<PostManagementAttendanceDay> = emptyList(),
    val attendanceRecords: List<PostManagementAttendanceRecord> = emptyList(),
    val evaluations: List<PostManagementEvaluation> = emptyList(),
    val physicalAttendance: PostManagementPhysicalAttendance? = null,
    val physicalReview: PostManagementPhysicalReview? = null,
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

data class PostManagementRemoteDetails(
    val startDate: String,
    val endDate: String,
    val volunteerCapacity: Int,
    val submissionMode: String,
    val sharedDeliverable: String? = null,
    val responsibleRoleTemplateId: String? = null
)

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


data class PostManagementPerson(
    val userId: String,
    val fullName: String,
    val city: String? = null,
    val bio: String? = null,
    val avatarPath: String? = null,
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
    val isShortlisted: Boolean = false
)

/** One attendance session opened by the organisation for one Physical event day. */
data class PostManagementAttendanceDay(
    val eventDate: String,
    val pinCode: String,
    val expectedMinutes: Int,
    val generatedAt: String? = null,
    val isActive: Boolean
)

/** One normalized volunteer check-in for one Physical role and event day. */
data class PostManagementAttendanceRecord(
    val eventDate: String,
    val roleTemplateId: String,
    val userId: String,
    val attendanceStatus: String,
    val checkedInAt: String? = null,
    val verifiedMinutes: Int
)

/** One final organisation review record for one volunteer participation. */
data class PostManagementEvaluation(
    val roleTemplateId: String,
    val userId: String,
    val organisationId: String,
    val feedback: String? = null,
    /** Required for a finalized NOT_COMPLETED Physical participation. */
    val completionReason: String? = null,
    val createdAt: String? = null,
    /** Final credited minutes. Physical NOT_COMPLETED is always 0. */
    val verifiedMinutes: Int? = null
)

/** Small attendance-only payload used by the active People-screen poll. */
data class PostManagementAttendanceSnapshot(
    val attendanceDays: List<PostManagementAttendanceDay> = emptyList(),
    val attendanceRecords: List<PostManagementAttendanceRecord> = emptyList()
)

/** Time-dependent Physical attendance state prepared for the Manage UI. */
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
data class PostManagementFeedbackGroup(
    val feedback: String,
    val userIds: List<String>,
    val recipientNames: List<String>
) {
    val recipientCount: Int
        get() = userIds.size
}

/** Physical close-out state. `ready` means full attendance/no flagged issue and is only a quick-completion candidate, not automatically completed. */
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
enum class PostManagementPhysicalReviewStage {
    ATTENDANCE,
    COMPLETION,
    FEEDBACK,
    FINISH
}

enum class PostManagementPendingDecisionType {
    COMPLETED,
    NOT_COMPLETED
}

enum class PostManagementPendingDecisionSource {
    FULL_ATTENDANCE,
    PARTIAL_ATTENDANCE,
    WORK_ISSUE
}

data class PostManagementPendingReviewDecision(
    val roleTemplateId: String,
    val userId: String,
    val decision: PostManagementPendingDecisionType,
    val reason: String? = null,
    val source: PostManagementPendingDecisionSource
)

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
