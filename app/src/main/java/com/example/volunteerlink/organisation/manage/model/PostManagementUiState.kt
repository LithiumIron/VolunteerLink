package com.example.volunteerlink.organisation.manage.model

import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.data.post.RoleApplicationCutoffReason
import com.example.volunteerlink.data.post.RoleApplicationWindowState

/** State for one Volunteer Post opened from Organisation > Manage > Volunteer Posts. */
data class OrganisationPostManagementUiState(
    val isLoading: Boolean = true,
    val post: PostManagementPost? = null,
    val errorMessage: String? = null
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
