package com.example.volunteerlink.organisation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.location.GeoapifyLocationService
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreatePostViewModel : ViewModel() {

    // Handles communication with the Geoapify API.
    private val locationService =
        GeoapifyLocationService()

    // Internal mutable state.
    private val _uiState =
        MutableStateFlow(CreatePostUiState())

    // Read-only state exposed to the UI.
    val uiState =
        _uiState.asStateFlow()

    // Keeps track of the current autocomplete request.
    private var locationSearchJob: Job? = null

    /**
     * Called whenever the organiser types inside
     * the location search field.
     */
    fun onLocationQueryChanged(
        query: String
    ) {
        // If the organiser edits the text after selecting
        // a location, that previous selection is no longer valid.
        _uiState.update {
            it.copy(
                locationQuery = query,
                selectedLocation = null,
                locationError = null
            )
        }

        // Cancel the previous pending search.
        locationSearchJob?.cancel()

        val trimmedQuery = query.trim()

        // Do not search very short text.
        if (trimmedQuery.length < 2) {
            _uiState.update {
                it.copy(
                    locationSuggestions = emptyList(),
                    isLocationSearching = false
                )
            }

            return
        }

        locationSearchJob =
            viewModelScope.launch {

                // Small delay so Geoapify is not called
                // after every single keystroke.
                delay(350)

                _uiState.update {
                    it.copy(
                        isLocationSearching = true,
                        locationError = null
                    )
                }

                try {
                    val results =
                        locationService.searchLocations(
                            query = trimmedQuery
                        )

                    _uiState.update {
                        it.copy(
                            locationSuggestions = results,
                            isLocationSearching = false,
                            locationError =
                                if (results.isEmpty()) {
                                    "No locations found."
                                } else {
                                    null
                                }
                        )
                    }

                } catch (e: Exception) {

                    _uiState.update {
                        it.copy(
                            locationSuggestions = emptyList(),
                            isLocationSearching = false,
                            locationError =
                                "Unable to search locations."
                        )
                    }
                }
            }
    }

    /**
     * Called when the organiser chooses one
     * of the Geoapify suggestions.
     */
    fun onLocationSelected(
        location: LocationSuggestion
    ) {
        _uiState.update {
            it.copy(
                locationQuery = location.displayName,
                selectedLocation = location,
                locationSuggestions = emptyList(),
                locationError = null
            )
        }
    }

    /**
     * Removes the selected location and
     * clears the location search field.
     */
    fun clearLocation() {
        locationSearchJob?.cancel()

        _uiState.update {
            it.copy(
                locationQuery = "",
                locationSuggestions = emptyList(),
                selectedLocation = null,
                isLocationSearching = false,
                locationError = null
            )
        }
    }
}