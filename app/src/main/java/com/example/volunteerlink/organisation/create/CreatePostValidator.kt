package com.example.volunteerlink.organisation.create

import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostErrors
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.RoleSelectionErrors
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import java.util.Calendar

/**
 * Validation and date/time rules for the Create Post flow.
 *
 * Keeping these rules outside the ViewModel prevents the ViewModel from
 * becoming a giant class as Steps 2-5 are added later.
 */
object CreatePostValidator {

    const val MINIMUM_LEAD_DAYS = 7

    fun startOfDayMillis(
        timeMillis: Long = System.currentTimeMillis()
    ): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun minimumStartDateMillis(
        todayMillis: Long = System.currentTimeMillis()
    ): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(todayMillis)
            add(Calendar.DAY_OF_YEAR, MINIMUM_LEAD_DAYS)
        }.timeInMillis
    }

    fun nextDayMillis(dateMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(dateMillis)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    fun endTimeError(
        startTimeMinutes: Int?,
        endTimeMinutes: Int
    ): String? {
        return when {
            startTimeMinutes == null -> "Select the start time first."
            endTimeMinutes <= startTimeMinutes ->
                "End time must be later than the start time."
            else -> null
        }
    }

    fun validateStepOne(
        draft: CreatePostDraft
    ): CreatePostErrors {
        val needsPhysical =
            draft.postType == VolunteerPostType.PHYSICAL ||
                    draft.postType == VolunteerPostType.HYBRID

        val needsRemote =
            draft.postType == VolunteerPostType.REMOTE ||
                    draft.postType == VolunteerPostType.HYBRID

        val physicalStartDateError = if (needsPhysical) {
            when {
                draft.physicalStartDateMillis == null ->
                    "Select a start date."

                startOfDayMillis(draft.physicalStartDateMillis) <
                        minimumStartDateMillis() ->
                    "Start date must be at least 7 days from today."

                else -> null
            }
        } else {
            null
        }

        val physicalEndDateError = if (
            needsPhysical && draft.isMultiDayPhysicalEvent
        ) {
            when {
                draft.physicalEndDateMillis == null ->
                    "Select an end date."

                draft.physicalStartDateMillis == null ->
                    "Select a start date first."

                startOfDayMillis(draft.physicalEndDateMillis) <=
                        startOfDayMillis(draft.physicalStartDateMillis) ->
                    "End date must be after the start date."

                else -> null
            }
        } else {
            null
        }

        val physicalTimeError = if (needsPhysical) {
            when {
                draft.physicalStartTimeMinutes == null ->
                    "Select a start time."

                draft.physicalEndTimeMinutes == null ->
                    "Select an end time."

                draft.physicalEndTimeMinutes <= draft.physicalStartTimeMinutes ->
                    "End time must be later than the start time."

                else -> null
            }
        } else {
            null
        }

        val remoteStartDateError = if (needsRemote) {
            when {
                draft.remoteStartDateMillis == null ->
                    "Select a start date."

                startOfDayMillis(draft.remoteStartDateMillis) <
                        minimumStartDateMillis() ->
                    "Start date must be at least 7 days from today."

                else -> null
            }
        } else {
            null
        }

        val remoteDueDateError = if (needsRemote) {
            when {
                draft.remoteDueDateMillis == null ->
                    "Select a due date."

                draft.remoteStartDateMillis == null ->
                    "Select a start date first."

                startOfDayMillis(draft.remoteDueDateMillis) <=
                        startOfDayMillis(draft.remoteStartDateMillis) ->
                    "Due date must be after the start date."

                else -> null
            }
        } else {
            null
        }

        return CreatePostErrors(
            postType = if (draft.postType == null) {
                "Select a post type."
            } else null,

            category = if (draft.category == null) {
                "Select a category."
            } else null,

            title = if (draft.title.isBlank()) {
                "Enter a post title."
            } else null,

            description = if (draft.description.isBlank()) {
                "Enter a description."
            } else null,

            physicalStartDate = physicalStartDateError,
            physicalEndDate = physicalEndDateError,
            physicalTime = physicalTimeError,

            physicalLocation = if (
                needsPhysical && draft.physicalLocation == null
            ) {
                "Select a location from the suggestions."
            } else null,

            physicalCapacity = if (
                draft.postType == VolunteerPostType.PHYSICAL &&
                (draft.physicalVolunteerCapacity ?: 0) <= 0
            ) {
                "Enter the number of volunteers needed."
            } else null,

            remoteStartDate = remoteStartDateError,
            remoteDueDate = remoteDueDateError,

            remoteCapacity = if (
                draft.postType == VolunteerPostType.REMOTE &&
                (draft.remoteVolunteerCapacity ?: 0) <= 0
            ) {
                "Enter the number of volunteers needed."
            } else null,

            remoteSubmissionMode = if (
                needsRemote && draft.remoteSubmissionMode == null
            ) {
                "Choose a submission setup."
            } else null,

            sharedDeliverable = if (
                needsRemote &&
                draft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM &&
                draft.sharedDeliverable.isBlank()
            ) {
                "Describe the shared team deliverable."
            } else null,

            hybridPhysicalCapacity = if (
                draft.postType == VolunteerPostType.HYBRID &&
                (draft.hybridPhysicalVolunteerCapacity ?: 0) <= 0
            ) {
                "Enter the Physical volunteer requirement."
            } else null,

            hybridRemoteCapacity = if (
                draft.postType == VolunteerPostType.HYBRID &&
                (draft.hybridRemoteVolunteerCapacity ?: 0) <= 0
            ) {
                "Enter the Remote volunteer requirement."
            } else null
        )
    }

    /**
     * Step 2 is complete only when the selected role capacities exactly match
     * the volunteer requirement entered in Step 1.
     *
     * Hybrid posts are checked separately so Physical and Remote capacities
     * cannot accidentally be mixed together.
     */
    fun validateStepTwo(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): RoleSelectionErrors {
        if (roleCatalogue.isEmpty()) {
            return RoleSelectionErrors(
                general = "Role catalogue is not available yet."
            )
        }

        val templatesById = roleCatalogue.associateBy { it.roleTemplateId }

        if (draft.selectedRoles.any { it.capacity <= 0 }) {
            return RoleSelectionErrors(
                general = "Each selected role needs at least 1 volunteer."
            )
        }

        if (draft.selectedRoles.any { it.roleTemplateId !in templatesById }) {
            return RoleSelectionErrors(
                general = "One selected role is no longer available. Remove it and select another role."
            )
        }

        val physicalSelections = draft.selectedRoles.filter { selected ->
            templatesById[selected.roleTemplateId]?.roleMode ==
                    VolunteerRoleMode.PHYSICAL
        }

        val remoteSelections = draft.selectedRoles.filter { selected ->
            templatesById[selected.roleTemplateId]?.roleMode ==
                    VolunteerRoleMode.REMOTE
        }

        fun capacityError(
            label: String,
            required: Int?,
            assigned: Int,
            selectedCount: Int
        ): String? {
            if (required == null || required <= 0) {
                return "Return to Step 1 and set the $label volunteer requirement."
            }

            if (selectedCount == 0) {
                return "Select at least one $label role."
            }

            return if (assigned != required) {
                "Assign exactly $required $label volunteers. Currently assigned $assigned."
            } else {
                null
            }
        }

        val physicalAssigned = physicalSelections.sumOf { it.capacity }
        val remoteAssigned = remoteSelections.sumOf { it.capacity }

        return when (draft.postType) {
            VolunteerPostType.PHYSICAL -> RoleSelectionErrors(
                general = if (remoteSelections.isNotEmpty()) {
                    "Remote roles cannot be added to a Physical post."
                } else {
                    null
                },
                physical = capacityError(
                    label = "Physical",
                    required = draft.requiredPhysicalVolunteerTotal,
                    assigned = physicalAssigned,
                    selectedCount = physicalSelections.size
                )
            )

            VolunteerPostType.REMOTE -> RoleSelectionErrors(
                general = if (physicalSelections.isNotEmpty()) {
                    "Physical roles cannot be added to a Remote post."
                } else {
                    null
                },
                remote = capacityError(
                    label = "Remote",
                    required = draft.requiredRemoteVolunteerTotal,
                    assigned = remoteAssigned,
                    selectedCount = remoteSelections.size
                )
            )

            VolunteerPostType.HYBRID -> RoleSelectionErrors(
                physical = capacityError(
                    label = "Physical",
                    required = draft.requiredPhysicalVolunteerTotal,
                    assigned = physicalAssigned,
                    selectedCount = physicalSelections.size
                ),
                remote = capacityError(
                    label = "Remote",
                    required = draft.requiredRemoteVolunteerTotal,
                    assigned = remoteAssigned,
                    selectedCount = remoteSelections.size
                )
            )

            null -> RoleSelectionErrors(
                general = "Return to Step 1 and select a post type."
            )
        }
    }

}
