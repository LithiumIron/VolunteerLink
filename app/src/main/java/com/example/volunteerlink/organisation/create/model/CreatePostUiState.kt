package com.example.volunteerlink.organisation.create.model

import com.example.volunteerlink.data.location.LocationSuggestion

data class CreatePostUiState(
    val locationQuery: String = "",
    val locationSuggestions: List<LocationSuggestion> = emptyList(),
    val selectedLocation: LocationSuggestion? = null,
    val isLocationSearching: Boolean = false,
    val locationError: String? = null
)