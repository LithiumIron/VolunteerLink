package com.example.volunteerlink.organisation.create.model

import com.example.volunteerlink.data.location.LocationSuggestion

/** Field-level validation messages for Create Post Step 1. */
data class CreatePostErrors(
    val postType: String? = null,
    val category: String? = null,
    val title: String? = null,
    val description: String? = null,
    val helpNeeded: String? = null,
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
            helpNeeded,
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

/**
 * Everything the Create Post UI currently needs to display.
 * MutableStateFlow stays private inside CreatePostViewModel.
 */
data class CreatePostUiState(
    val draft: CreatePostDraft = CreatePostDraft(),
    val helpNeededInput: String = "",
    val locationSuggestions: List<LocationSuggestion> = emptyList(),
    val isLocationSearching: Boolean = false,
    val locationSearchError: String? = null,
    val physicalTimeError: String? = null,
    val errors: CreatePostErrors = CreatePostErrors(),
    val showValidationErrors: Boolean = false,
    val isStepOneReady: Boolean = false,

    // Post type switching state.
    val pendingPostType: VolunteerPostType? = null,
    val isPostTypeCommitted: Boolean = false
) {
    /** Hide field errors until the organiser first presses Continue. */
    val visibleErrors: CreatePostErrors
        get() = if (showValidationErrors) errors else CreatePostErrors()

    fun hasUnsavedInput(): Boolean {
        return draft.hasMeaningfulContent() || helpNeededInput.isNotBlank()
    }
}


