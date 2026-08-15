package com.example.volunteerlink.organisation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.BuildConfig
import com.example.volunteerlink.data.location.GeoapifyLocationService
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.organisation.repository.CreatePostRepository
import com.example.volunteerlink.organisation.repository.SupabaseCreatePostRepository
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
    private val createPostRepository: CreatePostRepository =
        SupabaseCreatePostRepository()

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
    // Step 2: select roles
    // ---------------------------------------------------------------------

    /**
     * Moves from the validated Step 1 form to role selection.
     * Catalogue data is loaded from Supabase only when it is not already kept
     * in this ViewModel.
     */
    fun openStepTwo() {
        val defaultFilter = when (_uiState.value.draft.postType) {
            VolunteerPostType.PHYSICAL -> VolunteerRoleMode.PHYSICAL
            VolunteerPostType.REMOTE -> VolunteerRoleMode.REMOTE
            VolunteerPostType.HYBRID -> null
            null -> null
        }

        _uiState.update { current ->
            current.copy(
                currentStep = 2,
                roleModeFilter = defaultFilter,
                showRoleSelectionErrors = false,
                isStepTwoReady = false
            )
        }

        if (
            _uiState.value.roleCatalogue.isEmpty() &&
            !_uiState.value.isRoleCatalogueLoading
        ) {
            loadRoleCatalogue()
        }
    }

    fun backToStepOne() {
        _uiState.update { current ->
            current.copy(
                currentStep = 1,
                showRoleSelectionErrors = false,
                isStepTwoReady = false
            )
        }
    }

    fun retryRoleCatalogue() {
        loadRoleCatalogue(forceReload = true)
    }

    private fun loadRoleCatalogue(forceReload: Boolean = false) {
        if (
            !forceReload &&
            (_uiState.value.roleCatalogue.isNotEmpty() ||
                    _uiState.value.isRoleCatalogueLoading)
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    isRoleCatalogueLoading = true,
                    roleCatalogueError = null
                )
            }

            try {
                val catalogue = createPostRepository.loadRoleCatalogue()

                _uiState.update { current ->
                    val selectionErrors = CreatePostValidator.validateStepTwo(
                        draft = current.draft,
                        roleCatalogue = catalogue
                    )

                    current.copy(
                        roleCatalogue = catalogue,
                        isRoleCatalogueLoading = false,
                        roleCatalogueError = null,
                        roleSelectionErrors = selectionErrors,
                        isStepTwoReady = false
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isRoleCatalogueLoading = false,
                        roleCatalogueError =
                            "Unable to load volunteer roles. Check your connection and try again.",
                        isStepTwoReady = false
                    )
                }
            }
        }
    }

    fun updateRoleSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                roleSearchQuery = query.take(80),
                isStepTwoReady = false
            )
        }
    }

    fun updateRoleModeFilter(mode: VolunteerRoleMode?) {
        val postType = _uiState.value.draft.postType

        // Physical/Remote posts always stay on their matching catalogue.
        if (
            postType == VolunteerPostType.PHYSICAL &&
            mode != VolunteerRoleMode.PHYSICAL
        ) {
            return
        }

        if (
            postType == VolunteerPostType.REMOTE &&
            mode != VolunteerRoleMode.REMOTE
        ) {
            return
        }

        _uiState.update { current ->
            current.copy(roleModeFilter = mode)
        }
    }

    fun addRole(roleTemplateId: String) {
        val current = _uiState.value
        if (current.draft.selectedRoles.any {
                it.roleTemplateId == roleTemplateId
            }) {
            return
        }

        val template = current.roleCatalogue.firstOrNull {
            it.roleTemplateId == roleTemplateId
        } ?: return

        if (!roleMatchesPostType(template.roleMode, current.draft.postType)) {
            return
        }

        val requiredTotal = requiredCapacityForMode(
            draft = current.draft,
            mode = template.roleMode
        ) ?: return

        val currentlyAssigned = assignedCapacityForMode(
            draft = current.draft,
            mode = template.roleMode,
            catalogue = current.roleCatalogue
        )

        if (currentlyAssigned >= requiredTotal) {
            _uiState.update { state ->
                state.copy(
                    roleSelectionErrors = CreatePostValidator.validateStepTwo(
                        draft = state.draft,
                        roleCatalogue = state.roleCatalogue
                    ),
                    showRoleSelectionErrors = true,
                    isStepTwoReady = false
                )
            }
            return
        }

        updateStepTwoDraft { draft ->
            draft.copy(
                selectedRoles = draft.selectedRoles +
                        SelectedRoleDraft(
                            roleTemplateId = roleTemplateId,
                            capacity = 1
                        )
            )
        }
    }

    fun removeRole(roleTemplateId: String) {
        updateStepTwoDraft { draft ->
            draft.copy(
                selectedRoles = draft.selectedRoles.filterNot {
                    it.roleTemplateId == roleTemplateId
                }
            )
        }
    }

    fun increaseRoleCapacity(roleTemplateId: String) {
        val current = _uiState.value
        val template = current.roleCatalogue.firstOrNull {
            it.roleTemplateId == roleTemplateId
        } ?: return

        val requiredTotal = requiredCapacityForMode(
            draft = current.draft,
            mode = template.roleMode
        ) ?: return

        val assigned = assignedCapacityForMode(
            draft = current.draft,
            mode = template.roleMode,
            catalogue = current.roleCatalogue
        )

        if (assigned >= requiredTotal) return

        updateStepTwoDraft { draft ->
            draft.copy(
                selectedRoles = draft.selectedRoles.map { selected ->
                    if (selected.roleTemplateId == roleTemplateId) {
                        selected.copy(capacity = selected.capacity + 1)
                    } else {
                        selected
                    }
                }
            )
        }
    }

    fun decreaseRoleCapacity(roleTemplateId: String) {
        updateStepTwoDraft { draft ->
            draft.copy(
                selectedRoles = draft.selectedRoles.map { selected ->
                    if (
                        selected.roleTemplateId == roleTemplateId &&
                        selected.capacity > 1
                    ) {
                        selected.copy(capacity = selected.capacity - 1)
                    } else {
                        selected
                    }
                }
            )
        }
    }

    fun updateRoleCapacity(
        roleTemplateId: String,
        text: String
    ) {
        val digitsOnly = text.filter { it.isDigit() }.take(4)
        val capacity = if (digitsOnly.isBlank()) {
            0
        } else {
            digitsOnly.toIntOrNull() ?: return
        }

        updateStepTwoDraft { draft ->
            draft.copy(
                selectedRoles = draft.selectedRoles.map { selected ->
                    if (selected.roleTemplateId == roleTemplateId) {
                        selected.copy(capacity = capacity)
                    } else {
                        selected
                    }
                }
            )
        }
    }

    fun continueFromStepTwo(): Boolean {
        val current = _uiState.value
        val errors = CreatePostValidator.validateStepTwo(
            draft = current.draft,
            roleCatalogue = current.roleCatalogue
        )
        val ready = !errors.hasErrors()

        _uiState.update { state ->
            state.copy(
                roleSelectionErrors = errors,
                showRoleSelectionErrors = true,
                isStepTwoReady = ready
            )
        }

        return ready
    }

    private fun updateStepTwoDraft(
        change: (CreatePostDraft) -> CreatePostDraft
    ) {
        _uiState.update { current ->
            val newDraft = change(current.draft)
            val errors = CreatePostValidator.validateStepTwo(
                draft = newDraft,
                roleCatalogue = current.roleCatalogue
            )

            current.copy(
                draft = newDraft,
                roleSelectionErrors = errors,
                isStepTwoReady = false
            )
        }
    }

    private fun roleMatchesPostType(
        roleMode: VolunteerRoleMode,
        postType: VolunteerPostType?
    ): Boolean {
        return when (postType) {
            VolunteerPostType.PHYSICAL ->
                roleMode == VolunteerRoleMode.PHYSICAL

            VolunteerPostType.REMOTE ->
                roleMode == VolunteerRoleMode.REMOTE

            VolunteerPostType.HYBRID -> true
            null -> false
        }
    }

    private fun requiredCapacityForMode(
        draft: CreatePostDraft,
        mode: VolunteerRoleMode
    ): Int? {
        return when (mode) {
            VolunteerRoleMode.PHYSICAL ->
                draft.requiredPhysicalVolunteerTotal

            VolunteerRoleMode.REMOTE ->
                draft.requiredRemoteVolunteerTotal
        }
    }

    private fun assignedCapacityForMode(
        draft: CreatePostDraft,
        mode: VolunteerRoleMode,
        catalogue: List<CreateRoleTemplate>
    ): Int {
        val templatesById = catalogue.associateBy { it.roleTemplateId }

        return draft.selectedRoles
            .filter { selected ->
                templatesById[selected.roleTemplateId]?.roleMode == mode
            }
            .sumOf { it.capacity }
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
                isStepOneReady = false,
                isStepTwoReady = false,
                showRoleSelectionErrors = false
            )
        }
    }

    private fun parsePositiveNumber(text: String): Int? {
        val digitsOnly = text.filter { it.isDigit() }.take(4)
        if (digitsOnly.isBlank()) return null

        return digitsOnly.toIntOrNull()?.takeIf { it > 0 }
    }
}
