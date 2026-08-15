package com.example.volunteerlink.organisation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.BuildConfig
import com.example.volunteerlink.data.location.GeoapifyLocationService
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared state holder for the Create Post wizard.
 *
 * The UI only reads uiState and calls these action functions. This follows the
 * same StateFlow pattern used in the practicals while keeping a larger form in
 * one structured CreatePostDraft object.
 */
class CreatePostViewModel : ViewModel() {

    private val locationService = GeoapifyLocationService()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState = _uiState.asStateFlow()

    private var locationSearchJob: Job? = null
    private var locationBiasLatitude: Double? = null
    private var locationBiasLongitude: Double? = null

    // ---------------------------------------------------------------------
    // Shared post information
    // ---------------------------------------------------------------------

    /**
     * Handles a tap on a post type card.
     *
     * Shared Step 1 information is never cleared here. If the current mode
     * already contains mode-specific input, the UI asks for confirmation first.
     */
    fun requestPostTypeChange(type: VolunteerPostType) {
        val currentState = _uiState.value
        val currentType = currentState.draft.postType

        if (currentType == type) return

        // After Continue succeeds, the selected mode is committed.
        if (currentState.isPostTypeCommitted) {
            // The mode is committed after Continue. Ignore further mode taps
            // until the organiser discards the draft.
            _uiState.update { current ->
                current.copy(pendingPostType = null)
            }
            return
        }

        // First selection does not need a confirmation dialog.
        if (currentType == null) {
            applyPostTypeChange(type)
            return
        }

        // Ask before hiding mode-specific information the organiser already entered.
        if (currentState.draft.hasModeSpecificInput(currentType)) {
            _uiState.update { current ->
                current.copy(
                    pendingPostType = type
                )
            }
            return
        }

        applyPostTypeChange(type)
    }

    /** Called by the confirmation dialog. */
    fun confirmPostTypeChange() {
        val pendingType = _uiState.value.pendingPostType ?: return
        applyPostTypeChange(pendingType)
    }

    /** Keeps the current mode and closes the confirmation dialog. */
    fun cancelPostTypeChange() {
        _uiState.update { current ->
            current.copy(pendingPostType = null)
        }
    }

    private fun applyPostTypeChange(type: VolunteerPostType) {
        _uiState.update { current ->
            val newDraft = current.draft.copy(postType = type)

            current.copy(
                draft = newDraft,
                pendingPostType = null,
                errors = if (current.showValidationErrors) {
                    CreatePostValidator.validateStepOne(newDraft)
                } else {
                    current.errors
                },
                isStepOneReady = false
            )
        }
    }

    fun updateCategory(category: VolunteerPostCategory) {
        updateDraft { it.copy(category = category) }
    }

    fun updateTitle(title: String) {
        updateDraft { it.copy(title = title.take(120)) }
    }

    fun updateDescription(description: String) {
        updateDraft { it.copy(description = description.take(2000)) }
    }

    fun updateThumbnailUri(uri: String?) {
        updateDraft { it.copy(thumbnailUri = uri) }
    }

    // ---------------------------------------------------------------------
    // Help Needed
    // ---------------------------------------------------------------------

    fun updateHelpNeededInput(text: String) {
        _uiState.update { current ->
            current.copy(
                helpNeededInput = text.take(100),
                isStepOneReady = false
            )
        }
    }

    fun addHelpNeeded() {
        val input = _uiState.value.helpNeededInput.trim()
        if (input.isBlank()) return

        val existing = _uiState.value.draft.helpNeeded
        if (existing.any { it.equals(input, ignoreCase = true) }) {
            _uiState.update { it.copy(helpNeededInput = "") }
            return
        }

        updateDraft { draft ->
            draft.copy(helpNeeded = draft.helpNeeded + input)
        }

        _uiState.update { current ->
            current.copy(helpNeededInput = "")
        }
    }

    fun removeHelpNeeded(item: String) {
        updateDraft { draft ->
            draft.copy(helpNeeded = draft.helpNeeded - item)
        }
    }

    // ---------------------------------------------------------------------
    // Physical event
    // ---------------------------------------------------------------------

    fun updateIsMultiDay(isMultiDay: Boolean) {
        updateDraft { draft ->
            val startDate = draft.physicalStartDateMillis

            val endDate = if (!isMultiDay) {
                startDate
            } else {
                when {
                    startDate == null -> draft.physicalEndDateMillis
                    draft.physicalEndDateMillis != null &&
                            draft.physicalEndDateMillis > startDate -> {
                        draft.physicalEndDateMillis
                    }
                    else -> CreatePostValidator.nextDayMillis(startDate)
                }
            }

            draft.copy(
                isMultiDayPhysicalEvent = isMultiDay,
                physicalEndDateMillis = endDate
            )
        }
    }

    fun updatePhysicalStartDate(dateMillis: Long) {
        updateDraft { draft ->
            val normalizedDate = CreatePostValidator.startOfDayMillis(dateMillis)

            val endDate = if (!draft.isMultiDayPhysicalEvent) {
                normalizedDate
            } else {
                draft.physicalEndDateMillis?.takeIf {
                    it > normalizedDate
                } ?: CreatePostValidator.nextDayMillis(normalizedDate)
            }

            draft.copy(
                physicalStartDateMillis = normalizedDate,
                physicalEndDateMillis = endDate
            )
        }
    }

    fun updatePhysicalEndDate(dateMillis: Long) {
        updateDraft { draft ->
            draft.copy(
                physicalEndDateMillis =
                    CreatePostValidator.startOfDayMillis(dateMillis)
            )
        }
    }

    fun updatePhysicalStartTime(hour: Int, minute: Int) {
        val startMinutes = hour * 60 + minute

        updateDraft { draft ->
            draft.copy(
                physicalStartTimeMinutes = startMinutes,
                physicalEndTimeMinutes = draft.physicalEndTimeMinutes
                    ?.takeIf { it > startMinutes }
            )
        }

        _uiState.update { it.copy(physicalTimeError = null) }
    }

    /**
     * Returns an error for the time dialog. Invalid end times are not saved.
     */
    fun updatePhysicalEndTime(hour: Int, minute: Int): String? {
        val endMinutes = hour * 60 + minute
        val error = CreatePostValidator.endTimeError(
            startTimeMinutes = _uiState.value.draft.physicalStartTimeMinutes,
            endTimeMinutes = endMinutes
        )

        if (error != null) {
            _uiState.update { current ->
                current.copy(
                    physicalTimeError = error,
                    errors = if (current.showValidationErrors) {
                        current.errors.copy(physicalTime = error)
                    } else {
                        current.errors
                    },
                    isStepOneReady = false
                )
            }
            return error
        }

        updateDraft { draft ->
            draft.copy(physicalEndTimeMinutes = endMinutes)
        }

        _uiState.update { it.copy(physicalTimeError = null) }
        return null
    }

    fun clearPhysicalTimeError() {
        _uiState.update { it.copy(physicalTimeError = null) }
    }

    fun updateMeetingPoint(text: String) {
        updateDraft { it.copy(meetingPoint = text.take(250)) }
    }

    fun updatePhysicalVolunteerCapacity(text: String) {
        updateDraft { draft ->
            draft.copy(
                physicalVolunteerCapacity = parsePositiveNumber(text)
            )
        }
    }

    // ---------------------------------------------------------------------
    // Geoapify location
    // ---------------------------------------------------------------------

    fun updateLocationSearchBias(latitude: Double, longitude: Double) {
        locationBiasLatitude = latitude
        locationBiasLongitude = longitude
    }

    fun onLocationQueryChanged(query: String) {
        locationSearchJob?.cancel()

        updateDraft { draft ->
            draft.copy(
                physicalLocationQuery = query,
                physicalLocation = null
            )
        }

        _uiState.update { current ->
            current.copy(
                locationSuggestions = emptyList(),
                isLocationSearching = false,
                locationSearchError = null
            )
        }

        val cleanQuery = query.trim()

        if (cleanQuery.length < 2) {
            return
        }

        locationSearchJob = viewModelScope.launch {
            delay(350)

            if (BuildConfig.GEOAPIFY_API_KEY.isBlank()) {
                _uiState.update {
                    it.copy(
                        isLocationSearching = false,
                        locationSearchError = "Geoapify API key is missing."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLocationSearching = true,
                    locationSearchError = null
                )
            }

            try {
                val results = locationService.searchLocations(
                    query = cleanQuery,
                    biasLatitude = locationBiasLatitude,
                    biasLongitude = locationBiasLongitude
                )

                if (
                    _uiState.value.draft.physicalLocationQuery.trim() == cleanQuery &&
                    _uiState.value.draft.physicalLocation == null
                ) {
                    _uiState.update {
                        it.copy(
                            locationSuggestions = results,
                            isLocationSearching = false,
                            locationSearchError = if (results.isEmpty()) {
                                "No matching location found."
                            } else {
                                null
                            }
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                if (_uiState.value.draft.physicalLocationQuery.trim() == cleanQuery) {
                    _uiState.update {
                        it.copy(
                            locationSuggestions = emptyList(),
                            isLocationSearching = false,
                            locationSearchError = "Unable to search locations."
                        )
                    }
                }
            }
        }
    }

    fun onLocationSelected(location: LocationSuggestion) {
        locationSearchJob?.cancel()

        updateDraft { draft ->
            draft.copy(
                physicalLocationQuery = location.address.ifBlank {
                    location.displayName
                },
                physicalLocation = location
            )
        }

        _uiState.update { current ->
            current.copy(
                locationSuggestions = emptyList(),
                isLocationSearching = false,
                locationSearchError = null
            )
        }
    }

    fun clearLocation() {
        locationSearchJob?.cancel()

        updateDraft { draft ->
            draft.copy(
                physicalLocationQuery = "",
                physicalLocation = null
            )
        }

        _uiState.update { current ->
            current.copy(
                locationSuggestions = emptyList(),
                isLocationSearching = false,
                locationSearchError = null
            )
        }
    }

    // ---------------------------------------------------------------------
    // Remote project
    // ---------------------------------------------------------------------

    fun updateRemoteStartDate(dateMillis: Long) {
        updateDraft { draft ->
            val normalizedDate = CreatePostValidator.startOfDayMillis(dateMillis)

            draft.copy(
                remoteStartDateMillis = normalizedDate,
                remoteDueDateMillis = draft.remoteDueDateMillis
                    ?.takeIf { it > normalizedDate }
            )
        }
    }

    fun updateRemoteDueDate(dateMillis: Long) {
        updateDraft { draft ->
            draft.copy(
                remoteDueDateMillis = CreatePostValidator.startOfDayMillis(dateMillis)
            )
        }
    }

    fun updateRemoteVolunteerCapacity(text: String) {
        updateDraft { draft ->
            draft.copy(
                remoteVolunteerCapacity = parsePositiveNumber(text)
            )
        }
    }

    fun updateRemoteSubmissionMode(mode: RemoteSubmissionMode) {
        updateDraft { draft ->
            draft.copy(remoteSubmissionMode = mode)
        }
    }

    fun updateSharedDeliverable(text: String) {
        updateDraft { draft ->
            draft.copy(sharedDeliverable = text.take(500))
        }
    }

    // ---------------------------------------------------------------------
    // Hybrid capacities
    // ---------------------------------------------------------------------

    fun updateHybridPhysicalVolunteerCapacity(text: String) {
        updateDraft { draft ->
            draft.copy(
                hybridPhysicalVolunteerCapacity = parsePositiveNumber(text)
            )
        }
    }

    fun updateHybridRemoteVolunteerCapacity(text: String) {
        updateDraft { draft ->
            draft.copy(
                hybridRemoteVolunteerCapacity = parsePositiveNumber(text)
            )
        }
    }

    // ---------------------------------------------------------------------
    // Step validation / editor lifecycle
    // ---------------------------------------------------------------------

    fun continueFromStepOne(): Boolean {
        val currentDraft = _uiState.value.draft
        val errors = CreatePostValidator.validateStepOne(currentDraft)
        val ready = !errors.hasErrors()

        _uiState.update { current ->
            current.copy(
                // Only clear unused temporary mode data after validation succeeds.
                draft = if (ready) {
                    current.draft.keepOnlySelectedModeData()
                } else {
                    current.draft
                },
                errors = errors,
                showValidationErrors = true,
                isStepOneReady = ready,
                pendingPostType = null,
                isPostTypeCommitted = ready
            )
        }

        return ready
    }

    fun hasUnsavedInput(): Boolean = _uiState.value.hasUnsavedInput()

    fun discardDraft() {
        locationSearchJob?.cancel()
        _uiState.value = CreatePostUiState()
    }

    private fun updateDraft(
        change: (CreatePostDraft) -> CreatePostDraft
    ) {
        _uiState.update { current ->
            val newDraft = change(current.draft)
            current.copy(
                draft = newDraft,
                errors = if (current.showValidationErrors) {
                    CreatePostValidator.validateStepOne(newDraft)
                } else {
                    current.errors
                },
                isStepOneReady = false
            )
        }
    }

    private fun parsePositiveNumber(text: String): Int? {
        val digitsOnly = text.filter { it.isDigit() }.take(4)
        if (digitsOnly.isBlank()) return null

        return digitsOnly.toIntOrNull()?.takeIf { it > 0 }
    }
}
