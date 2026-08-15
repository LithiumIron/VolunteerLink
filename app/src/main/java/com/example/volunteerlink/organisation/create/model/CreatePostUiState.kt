package com.example.volunteerlink.organisation.create.model

import com.example.volunteerlink.data.location.LocationSuggestion

/**
 * Single read-only state collected by OrganisationCreateScreen.
 * MutableStateFlow stays private inside CreatePostViewModel.
 */
data class CreatePostUiState(
    val draft: CreatePostDraft = CreatePostDraft(),
    val helpNeededInput: String = "",
    val locationSuggestions: List<LocationSuggestion> = emptyList(),
    val isLocationSearching: Boolean = false,
    val locationSearchError: String? = null,
    val physicalTimeError: String? = null,
    val errors: CreatePostStepOneErrors = CreatePostStepOneErrors(),
    val showValidationErrors: Boolean = false,
    val isStepOneReady: Boolean = false
) {
    fun hasUnsavedInput(): Boolean {
        return draft.hasMeaningfulContent() || helpNeededInput.isNotBlank()
    }
}
