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
    val attendanceActionMessage: String? = null
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
    val physicalAttendance: PostManagementPhysicalAttendance? = null,
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
    val markedAbsent: Boolean = false,
    val checkedInAt: String? = null,
    val verifiedMinutes: Int = 0
)
