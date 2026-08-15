package com.example.volunteerlink.organisation.create

import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostErrors
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
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

            helpNeeded = if (draft.helpNeeded.isEmpty()) {
                "Add at least one Help Needed item."
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
}
