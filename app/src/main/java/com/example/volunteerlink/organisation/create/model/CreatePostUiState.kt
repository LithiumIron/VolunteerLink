package com.example.volunteerlink.organisation.create.model

import com.example.volunteerlink.data.location.LocationSuggestion

/** Field-level validation messages for Create Post Step 1. */
data class CreatePostErrors(
    val postType: String? = null,
    val category: String? = null,
    val title: String? = null,
    val description: String? = null,
    val physicalStartDate: String? = null,
    val physicalEndDate: String? = null,
    val physicalTime: String? = null,
    val physicalLocation: String? = null,
    val physicalCapacity: String? = null,
    val remoteStartDate: String? = null,
    val remoteDueDate: String? = null,
    val remoteCapacity: String? = null,
    val remoteSubmissionMode: String? = null,
    val sharedDeliverable: String? = null,
    val hybridPhysicalCapacity: String? = null,
    val hybridRemoteCapacity: String? = null
) {
    fun hasErrors(): Boolean {
        return listOf(
            postType,
            category,
            title,
            description,
            physicalStartDate,
            physicalEndDate,
            physicalTime,
            physicalLocation,
            physicalCapacity,
            remoteStartDate,
            remoteDueDate,
            remoteCapacity,
            remoteSubmissionMode,
            sharedDeliverable,
            hybridPhysicalCapacity,
            hybridRemoteCapacity
        ).any { it != null }
    }
}

/** Step 2 capacity-allocation messages. */
data class RoleSelectionErrors(
    val general: String? = null,
    val physical: String? = null,
    val remote: String? = null
) {
    fun hasErrors(): Boolean {
        return general != null || physical != null || remote != null
    }
}

/**
 * Everything the Create Post UI currently needs to display.
 * MutableStateFlow stays private inside CreatePostViewModel.
 */
data class CreatePostUiState(
    val draft: CreatePostDraft = CreatePostDraft(),
    val locationSuggestions: List<LocationSuggestion> = emptyList(),
    val isLocationSearching: Boolean = false,
    val locationSearchError: String? = null,
    val physicalTimeError: String? = null,
    val errors: CreatePostErrors = CreatePostErrors(),
    val showValidationErrors: Boolean = false,
    val isStepOneReady: Boolean = false,

    // Current Create Post page.
    val currentStep: Int = 1,

    // Step 2 role catalogue / role selection state.
    val roleCatalogue: List<CreateRoleTemplate> = emptyList(),
    val isRoleCatalogueLoading: Boolean = false,
    val roleCatalogueError: String? = null,
    val roleSearchQuery: String = "",
    val roleModeFilter: VolunteerRoleMode? = null,
    val roleSelectionErrors: RoleSelectionErrors = RoleSelectionErrors(),
    val showRoleSelectionErrors: Boolean = false,
    val isStepTwoReady: Boolean = false,

    // Step 3 role-settings state.
    val editingRoleTemplateId: String? = null,
    val roleSettingsError: String? = null,
    val isStepThreeReady: Boolean = false,

    // Step 4 schedule state. The editor uses a temporary buffer so pressing
    // Add never creates an incomplete saved schedule item.
    val activeScheduleSection: ScheduleType? = null,
    val selectedPhysicalScheduleDateMillis: Long? = null,
    val editingScheduleItemId: String? = null,
    val scheduleEditorDraft: ScheduleItemDraft? = null,
    val isScheduleEditorOpen: Boolean = false,
    val trainingLocationSuggestions: List<LocationSuggestion> = emptyList(),
    val isTrainingLocationSearching: Boolean = false,
    val trainingLocationSearchError: String? = null,
    val scheduleError: String? = null,
    val showScheduleErrors: Boolean = false,
    val isStepFourReady: Boolean = false,

    // Post type switching state.
    val pendingPostType: VolunteerPostType? = null,
    val isPostTypeCommitted: Boolean = false
) {
    /** Hide field errors until the organiser first presses Continue. */
    val visibleErrors: CreatePostErrors
        get() = if (showValidationErrors) errors else CreatePostErrors()

    val visibleRoleSelectionErrors: RoleSelectionErrors
        get() = if (showRoleSelectionErrors) {
            roleSelectionErrors
        } else {
            RoleSelectionErrors()
        }

    fun hasUnsavedInput(): Boolean {
        return draft.hasMeaningfulContent()
    }
}


