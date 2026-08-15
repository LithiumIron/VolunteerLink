package com.example.volunteerlink.organisation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.BuildConfig
import com.example.volunteerlink.data.location.GeoapifyLocationService
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.create.CreatePostStepOneActions
import com.example.volunteerlink.organisation.create.model.CreatePostDateRules
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostStepOneErrors
import com.example.volunteerlink.organisation.create.model.CreatePostTimeRules
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
class CreatePostViewModel : ViewModel(), CreatePostStepOneActions {

    private val locationService = GeoapifyLocationService()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState = _uiState.asStateFlow()

    private var locationSearchJob: Job? = null
    private var locationBiasLatitude: Double? = null
    private var locationBiasLongitude: Double? = null

    // ---------------------------------------------------------------------
    // Shared post information
    // ---------------------------------------------------------------------

    override fun updatePostType(type: VolunteerPostType) {
        updateDraft { it.copy(postType = type) }
    }

    override fun updateCategory(category: VolunteerPostCategory) {
        updateDraft { it.copy(category = category) }
    }

    override fun updateTitle(title: String) {
        updateDraft { it.copy(title = title.take(120)) }
    }

    override fun updateDescription(description: String) {
        updateDraft { it.copy(description = description.take(2000)) }
    }

    override fun updateThumbnailUri(uri: String?) {
        updateDraft { it.copy(thumbnailUri = uri) }
    }

    // ---------------------------------------------------------------------
    // Help Needed
    // ---------------------------------------------------------------------

    override fun updateHelpNeededInput(text: String) {
        _uiState.update { current ->
            current.copy(
                helpNeededInput = text.take(100),
                isStepOneReady = false
            )
        }
    }

    override fun addHelpNeeded() {
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

    override fun removeHelpNeeded(item: String) {
        updateDraft { draft ->
            draft.copy(helpNeeded = draft.helpNeeded - item)
        }
    }

    // ---------------------------------------------------------------------
    // Physical event
    // ---------------------------------------------------------------------

    override fun updateIsMultiDay(isMultiDay: Boolean) {
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
                    else -> CreatePostDateRules.nextDayMillis(startDate)
                }
            }

            draft.copy(
                isMultiDayPhysicalEvent = isMultiDay,
                physicalEndDateMillis = endDate
            )
        }
    }

    override fun updatePhysicalStartDate(dateMillis: Long) {
        updateDraft { draft ->
            val normalizedDate = CreatePostDateRules.startOfDayMillis(dateMillis)

            val endDate = if (!draft.isMultiDayPhysicalEvent) {
                normalizedDate
            } else {
                draft.physicalEndDateMillis?.takeIf {
                    it > normalizedDate
                } ?: CreatePostDateRules.nextDayMillis(normalizedDate)
            }

            draft.copy(
                physicalStartDateMillis = normalizedDate,
                physicalEndDateMillis = endDate
            )
        }
    }

    override fun updatePhysicalEndDate(dateMillis: Long) {
        updateDraft { draft ->
            draft.copy(
                physicalEndDateMillis =
                    CreatePostDateRules.startOfDayMillis(dateMillis)
            )
        }
    }

    override fun updatePhysicalStartTime(hour: Int, minute: Int) {
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
    override fun updatePhysicalEndTime(hour: Int, minute: Int): String? {
        val endMinutes = hour * 60 + minute
        val error = CreatePostTimeRules.endTimeError(
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

    override fun clearPhysicalTimeError() {
        _uiState.update { it.copy(physicalTimeError = null) }
    }

    override fun updateMeetingPoint(text: String) {
        updateDraft { it.copy(meetingPoint = text.take(250)) }
    }

    override fun updatePhysicalVolunteerCapacity(text: String) {
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

    override fun onLocationQueryChanged(query: String) {
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

    override fun onLocationSelected(location: LocationSuggestion) {
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

    override fun clearLocation() {
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

    override fun updateRemoteStartDate(dateMillis: Long) {
        updateDraft { draft ->
            val normalizedDate = CreatePostDateRules.startOfDayMillis(dateMillis)

            draft.copy(
                remoteStartDateMillis = normalizedDate,
                remoteDueDateMillis = draft.remoteDueDateMillis
                    ?.takeIf { it > normalizedDate }
            )
        }
    }

    override fun updateRemoteDueDate(dateMillis: Long) {
        updateDraft { draft ->
            draft.copy(
                remoteDueDateMillis = CreatePostDateRules.startOfDayMillis(dateMillis)
            )
        }
    }

    override fun updateRemoteVolunteerCapacity(text: String) {
        updateDraft { draft ->
            draft.copy(
                remoteVolunteerCapacity = parsePositiveNumber(text)
            )
        }
    }

    override fun updateRemoteSubmissionMode(mode: RemoteSubmissionMode) {
        updateDraft { draft ->
            draft.copy(remoteSubmissionMode = mode)
        }
    }

    override fun updateSharedDeliverable(text: String) {
        updateDraft { draft ->
            draft.copy(sharedDeliverable = text.take(500))
        }
    }

    // ---------------------------------------------------------------------
    // Hybrid capacities
    // ---------------------------------------------------------------------

    override fun updateHybridPhysicalVolunteerCapacity(text: String) {
        updateDraft { draft ->
            draft.copy(
                hybridPhysicalVolunteerCapacity = parsePositiveNumber(text)
            )
        }
    }

    override fun updateHybridRemoteVolunteerCapacity(text: String) {
        updateDraft { draft ->
            draft.copy(
                hybridRemoteVolunteerCapacity = parsePositiveNumber(text)
            )
        }
    }

    // ---------------------------------------------------------------------
    // Step validation / editor lifecycle
    // ---------------------------------------------------------------------

    override fun continueFromStepOne(): Boolean {
        val errors = validateStepOne(_uiState.value.draft)
        val ready = !errors.hasErrors()

        _uiState.update { current ->
            current.copy(
                errors = errors,
                showValidationErrors = true,
                isStepOneReady = ready
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
                    validateStepOne(newDraft)
                } else {
                    current.errors
                },
                isStepOneReady = false
            )
        }
    }

    private fun validateStepOne(draft: CreatePostDraft): CreatePostStepOneErrors {
        val needsPhysical = draft.postType == VolunteerPostType.PHYSICAL ||
                draft.postType == VolunteerPostType.HYBRID

        val needsRemote = draft.postType == VolunteerPostType.REMOTE ||
                draft.postType == VolunteerPostType.HYBRID

        val physicalStartDateError = if (needsPhysical) {
            when {
                draft.physicalStartDateMillis == null -> "Select a start date."
                !CreatePostDateRules.isValidStartDate(draft.physicalStartDateMillis) ->
                    "Start date must be at least 7 days from today."
                else -> null
            }
        } else null

        val physicalEndDateError = if (
            needsPhysical && draft.isMultiDayPhysicalEvent
        ) {
            when {
                draft.physicalEndDateMillis == null -> "Select an end date."
                !CreatePostDateRules.isValidPhysicalEndDate(
                    draft.physicalStartDateMillis,
                    draft.physicalEndDateMillis,
                    isMultiDay = true
                ) -> "End date must be after the start date."
                else -> null
            }
        } else null

        val physicalTimeError = if (needsPhysical) {
            when {
                draft.physicalStartTimeMinutes == null -> "Select a start time."
                draft.physicalEndTimeMinutes == null -> "Select an end time."
                draft.physicalEndTimeMinutes <= draft.physicalStartTimeMinutes ->
                    "End time must be later than the start time."
                else -> null
            }
        } else null

        val remoteStartDateError = if (needsRemote) {
            when {
                draft.remoteStartDateMillis == null -> "Select a start date."
                !CreatePostDateRules.isValidStartDate(draft.remoteStartDateMillis) ->
                    "Start date must be at least 7 days from today."
                else -> null
            }
        } else null

        val remoteDueDateError = if (needsRemote) {
            when {
                draft.remoteDueDateMillis == null -> "Select a due date."
                !CreatePostDateRules.isValidRemoteDueDate(
                    draft.remoteStartDateMillis,
                    draft.remoteDueDateMillis
                ) -> "Due date must be after the start date."
                else -> null
            }
        } else null

        return CreatePostStepOneErrors(
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
            physicalLocation = if (needsPhysical && draft.physicalLocation == null) {
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
            remoteSubmissionMode = if (needsRemote && draft.remoteSubmissionMode == null) {
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

    private fun parsePositiveNumber(text: String): Int? {
        val digitsOnly = text.filter { it.isDigit() }.take(4)
        if (digitsOnly.isBlank()) return null

        return digitsOnly.toIntOrNull()?.takeIf { it > 0 }
    }
}
