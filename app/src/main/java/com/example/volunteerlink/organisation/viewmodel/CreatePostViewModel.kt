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
import com.example.volunteerlink.organisation.create.model.RoleApplicationMethod
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
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
            VolunteerPostType.HYBRID -> VolunteerRoleMode.PHYSICAL
            null -> null
        }

        _uiState.update { current ->
            val refreshedRoleErrors = if (current.roleCatalogue.isNotEmpty()) {
                CreatePostValidator.validateStepTwo(
                    draft = current.draft,
                    roleCatalogue = current.roleCatalogue
                )
            } else {
                current.roleSelectionErrors
            }

            current.copy(
                currentStep = 2,
                roleModeFilter = defaultFilter,
                roleSearchQuery = "",
                roleSelectionErrors = refreshedRoleErrors,
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

        val acceptedMode = when (postType) {
            VolunteerPostType.PHYSICAL -> {
                if (mode != VolunteerRoleMode.PHYSICAL) return
                VolunteerRoleMode.PHYSICAL
            }

            VolunteerPostType.REMOTE -> {
                if (mode != VolunteerRoleMode.REMOTE) return
                VolunteerRoleMode.REMOTE
            }

            VolunteerPostType.HYBRID -> mode ?: return
            null -> return
        }

        _uiState.update { current ->
            current.copy(
                roleModeFilter = acceptedMode,
                // Match the prototype: switching Physical/Remote starts with
                // a clean search instead of carrying an unrelated query over.
                roleSearchQuery = if (current.roleModeFilter != acceptedMode) {
                    ""
                } else {
                    current.roleSearchQuery
                }
            )
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
                },
                sharedSubmissionResponsibleRoleTemplateId =
                    if (
                        draft.sharedSubmissionResponsibleRoleTemplateId ==
                        roleTemplateId
                    ) {
                        null
                    } else {
                        draft.sharedSubmissionResponsibleRoleTemplateId
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
        val requestedCapacity = text
            .filter { it.isDigit() }
            .take(4)
            .toIntOrNull()
            ?: return

        val current = _uiState.value
        val template = current.roleCatalogue.firstOrNull { role ->
            role.roleTemplateId == roleTemplateId
        } ?: return

        val requiredTotal = requiredCapacityForMode(
            draft = current.draft,
            mode = template.roleMode
        ) ?: return

        val templatesById = current.roleCatalogue.associateBy { role ->
            role.roleTemplateId
        }

        val assignedByOtherRoles = current.draft.selectedRoles
            .filter { selected ->
                selected.roleTemplateId != roleTemplateId &&
                        templatesById[selected.roleTemplateId]?.roleMode ==
                        template.roleMode
            }
            .sumOf { it.capacity }

        // The typed value follows the same rule as the + button: one role
        // cannot use more positions than are still available for its mode.
        val maximumForThisRole =
            (requiredTotal - assignedByOtherRoles).coerceAtLeast(1)

        val acceptedCapacity = requestedCapacity.coerceIn(
            minimumValue = 1,
            maximumValue = maximumForThisRole
        )

        updateStepTwoDraft { draft ->
            draft.copy(
                selectedRoles = draft.selectedRoles.map { selected ->
                    if (selected.roleTemplateId == roleTemplateId) {
                        selected.copy(capacity = acceptedCapacity)
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

        if (ready) {
            openStepThree()
        }

        return ready
    }

    // ---------------------------------------------------------------------
    // Step 3: role settings
    // ---------------------------------------------------------------------

    /**
     * Opens Step 3 and prepares each selected role from the Supabase catalogue.
     * Recommended skills become the starting selection only the first time the
     * role reaches Step 3; existing organiser edits are preserved.
     */
    fun openStepThree() {
        val current = _uiState.value
        val stepTwoErrors = CreatePostValidator.validateStepTwo(
            draft = current.draft,
            roleCatalogue = current.roleCatalogue
        )

        if (stepTwoErrors.hasErrors()) {
            _uiState.update { state ->
                state.copy(
                    roleSelectionErrors = stepTwoErrors,
                    showRoleSelectionErrors = true,
                    isStepTwoReady = false
                )
            }
            return
        }

        val templatesById = current.roleCatalogue.associateBy { template ->
            template.roleTemplateId
        }

        val preparedRoles = current.draft.selectedRoles.map { selectedRole ->
            val template = templatesById[selectedRole.roleTemplateId]
                ?: return@map selectedRole

            val allowedSkillIds = template.skillsPractised
                .map { skill -> skill.skillId }
            val allowedSkillSet = allowedSkillIds.toSet()

            val existingSkills = selectedRole.practisedSkillIds
                .filter { skillId -> skillId in allowedSkillSet }
                .distinct()

            val recommendedIds = template.recommendedSkills
                .map { skill -> skill.skillId }
                .filter { skillId -> skillId in allowedSkillSet }
                .distinct()

            val startingSkills = if (existingSkills.isNotEmpty()) {
                existingSkills.take(4)
            } else {
                (recommendedIds + allowedSkillIds)
                    .distinct()
                    .take(2)
            }

            val recommendedMethod =
                CreatePostValidator.recommendedApplicationMethodForLevel(
                    template.defaultLevel
                )
            val selectedMethod =
                selectedRole.applicationMethod ?: recommendedMethod

            val cleanedRequirements = if (
                template.defaultLevel == VolunteerRoleLevel.BEGINNER
            ) {
                emptyMap()
            } else {
                selectedRole.requiredSkillExperience
                    .filter { (skillId, minimum) ->
                        skillId in startingSkills && minimum in 1..5
                    }
            }

            val prepared = selectedRole.copy(
                practisedSkillIds = startingSkills,
                requiredSkillExperience = cleanedRequirements,
                responsibilities = if (selectedRole.responsibilities.isEmpty()) {
                    listOf("")
                } else {
                    selectedRole.responsibilities
                },
                applicationMethod = selectedMethod,
                screeningQuestions = if (
                    selectedMethod == RoleApplicationMethod.INSTANT_JOIN
                ) {
                    emptyList()
                } else {
                    selectedRole.screeningQuestions.take(3)
                }
            )

            prepared.copy(
                isConfigured = selectedRole.isConfigured &&
                        CreatePostValidator.validateRoleConfiguration(
                            draft = current.draft,
                            selectedRole = prepared,
                            template = template
                        ).isEmpty()
            )
        }

        var preparedDraft = current.draft.copy(
            selectedRoles = preparedRoles
        )

        if (preparedDraft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM) {
            val remoteRoleIds = preparedRoles.mapNotNull { selectedRole ->
                val template = templatesById[selectedRole.roleTemplateId]
                if (template?.roleMode == VolunteerRoleMode.REMOTE) {
                    selectedRole.roleTemplateId
                } else {
                    null
                }
            }

            val currentResponsible =
                preparedDraft.sharedSubmissionResponsibleRoleTemplateId

            val resolvedResponsible = when {
                currentResponsible != null && currentResponsible in remoteRoleIds ->
                    currentResponsible

                remoteRoleIds.size == 1 -> remoteRoleIds.first()
                else -> null
            }

            preparedDraft = preparedDraft.copy(
                sharedSubmissionResponsibleRoleTemplateId = resolvedResponsible
            )
        }

        val ready = CreatePostValidator.validateStepThree(
            draft = preparedDraft,
            roleCatalogue = current.roleCatalogue
        ) == null

        _uiState.update { state ->
            state.copy(
                draft = preparedDraft,
                currentStep = 3,
                editingRoleTemplateId = null,
                roleSettingsError = null,
                isStepThreeReady = ready,
                showRoleSelectionErrors = false
            )
        }
    }

    /** Opens one selected role in the Step 3 editor. */
    fun openRoleEditor(roleTemplateId: String) {
        val current = _uiState.value
        if (current.draft.selectedRoles.none {
                it.roleTemplateId == roleTemplateId
            }) {
            return
        }

        _uiState.update { state ->
            state.copy(
                editingRoleTemplateId = roleTemplateId,
                roleSettingsError = null
            )
        }
    }

    /** Returns from one role editor to the Step 3 overview. */
    fun closeRoleEditor() {
        _uiState.update { state ->
            state.copy(
                editingRoleTemplateId = null,
                roleSettingsError = null
            )
        }
    }

    /** System/UI Back: role editor -> overview, overview -> Step 2. */
    fun backFromStepThree() {
        if (_uiState.value.editingRoleTemplateId != null) {
            closeRoleEditor()
        } else {
            _uiState.update { state ->
                state.copy(
                    currentStep = 2,
                    roleSettingsError = null,
                    isStepThreeReady = false
                )
            }
        }
    }

    fun togglePractisedSkill(
        roleTemplateId: String,
        skillId: String
    ) {
        val current = _uiState.value
        val template = current.roleCatalogue.firstOrNull {
            it.roleTemplateId == roleTemplateId
        } ?: return

        if (template.skillsPractised.none { it.skillId == skillId }) return

        val selectedRole = current.draft.selectedRoles.firstOrNull {
            it.roleTemplateId == roleTemplateId
        } ?: return

        val currentlySelected = skillId in selectedRole.practisedSkillIds

        if (!currentlySelected && selectedRole.practisedSkillIds.size >= 4) {
            setRoleSettingsError(
                "Maximum 4 practised skills. Remove another skill first."
            )
            return
        }

        if (
            currentlySelected &&
            skillId in selectedRole.requiredSkillExperience
        ) {
            setRoleSettingsError(
                "This skill is Required. Remove the requirement before unticking it."
            )
            return
        }

        if (currentlySelected && selectedRole.practisedSkillIds.size <= 2) {
            setRoleSettingsError(
                "Keep at least 2 practised skills for this role."
            )
            return
        }

        updateRoleConfiguration(roleTemplateId) { role ->
            val updatedIds = if (currentlySelected) {
                role.practisedSkillIds - skillId
            } else {
                val selectedSet = (role.practisedSkillIds + skillId).toSet()
                template.skillsPractised
                    .map { skill -> skill.skillId }
                    .filter { availableId -> availableId in selectedSet }
            }

            role.copy(
                practisedSkillIds = updatedIds,
                requiredSkillExperience = role.requiredSkillExperience
                    .filterKeys { requiredId -> requiredId in updatedIds }
            )
        }
    }

    fun toggleRequiredSkill(
        roleTemplateId: String,
        skillId: String
    ) {
        val current = _uiState.value
        val template = current.roleCatalogue.firstOrNull {
            it.roleTemplateId == roleTemplateId
        } ?: return

        if (template.defaultLevel == VolunteerRoleLevel.BEGINNER) return
        if (template.skillsPractised.none { it.skillId == skillId }) return

        val selectedRole = current.draft.selectedRoles.firstOrNull {
            it.roleTemplateId == roleTemplateId
        } ?: return

        val isRequired = skillId in selectedRole.requiredSkillExperience

        if (
            !isRequired &&
            skillId !in selectedRole.practisedSkillIds &&
            selectedRole.practisedSkillIds.size >= 4
        ) {
            setRoleSettingsError(
                "Maximum 4 practised skills. Remove another skill before making this one Required."
            )
            return
        }

        updateRoleConfiguration(roleTemplateId) { role ->
            if (isRequired) {
                role.copy(
                    requiredSkillExperience =
                        role.requiredSkillExperience - skillId
                )
            } else {
                val selectedSet =
                    (role.practisedSkillIds + skillId).toSet()
                val orderedSkills = template.skillsPractised
                    .map { skill -> skill.skillId }
                    .filter { availableId -> availableId in selectedSet }
                    .take(4)

                role.copy(
                    practisedSkillIds = orderedSkills,
                    requiredSkillExperience =
                        role.requiredSkillExperience + (skillId to 1)
                )
            }
        }
    }

    fun increaseRequiredSkillExperience(
        roleTemplateId: String,
        skillId: String
    ) {
        changeRequiredSkillExperience(
            roleTemplateId = roleTemplateId,
            skillId = skillId,
            change = 1
        )
    }

    fun decreaseRequiredSkillExperience(
        roleTemplateId: String,
        skillId: String
    ) {
        changeRequiredSkillExperience(
            roleTemplateId = roleTemplateId,
            skillId = skillId,
            change = -1
        )
    }

    private fun changeRequiredSkillExperience(
        roleTemplateId: String,
        skillId: String,
        change: Int
    ) {
        updateRoleConfiguration(roleTemplateId) { role ->
            val current = role.requiredSkillExperience[skillId]
                ?: return@updateRoleConfiguration role

            role.copy(
                requiredSkillExperience =
                    role.requiredSkillExperience +
                            (skillId to (current + change).coerceIn(1, 5))
            )
        }
    }

    fun addResponsibility(roleTemplateId: String) {
        updateRoleConfiguration(roleTemplateId) { role ->
            if (role.responsibilities.lastOrNull()?.isBlank() == true) {
                role
            } else {
                role.copy(
                    responsibilities = role.responsibilities + ""
                )
            }
        }
    }

    fun updateResponsibility(
        roleTemplateId: String,
        index: Int,
        text: String
    ) {
        updateRoleConfiguration(roleTemplateId) { role ->
            if (index !in role.responsibilities.indices) {
                return@updateRoleConfiguration role
            }

            role.copy(
                responsibilities = role.responsibilities.mapIndexed { itemIndex, item ->
                    if (itemIndex == index) text.take(160) else item
                }
            )
        }
    }

    fun removeResponsibility(
        roleTemplateId: String,
        index: Int
    ) {
        updateRoleConfiguration(roleTemplateId) { role ->
            if (index !in role.responsibilities.indices) {
                return@updateRoleConfiguration role
            }

            val updated = role.responsibilities.filterIndexed { itemIndex, _ ->
                itemIndex != index
            }

            role.copy(
                responsibilities = if (updated.isEmpty()) listOf("") else updated
            )
        }
    }

    /**
     * Changes the application method for one role.
     *
     * The role level only controls the initial recommendation. Organisations
     * can choose either method for Beginner, Intermediate or Advanced roles.
     */
    fun updateRoleApplicationMethod(
        roleTemplateId: String,
        method: RoleApplicationMethod
    ) {
        updateRoleConfiguration(roleTemplateId) { role ->
            role.copy(
                applicationMethod = method,
                screeningQuestions = if (
                    method == RoleApplicationMethod.INSTANT_JOIN
                ) {
                    emptyList()
                } else {
                    role.screeningQuestions.take(3)
                }
            )
        }
    }

    fun addScreeningQuestion(roleTemplateId: String) {
        updateRoleConfiguration(roleTemplateId) { role ->
            if (
                role.applicationMethod != RoleApplicationMethod.REVIEW_APPLICANTS ||
                role.screeningQuestions.size >= 3 ||
                role.screeningQuestions.lastOrNull()?.isBlank() == true
            ) {
                role
            } else {
                role.copy(
                    screeningQuestions = role.screeningQuestions + ""
                )
            }
        }
    }

    fun updateScreeningQuestion(
        roleTemplateId: String,
        index: Int,
        text: String
    ) {
        updateRoleConfiguration(roleTemplateId) { role ->
            if (index !in role.screeningQuestions.indices) {
                return@updateRoleConfiguration role
            }

            role.copy(
                screeningQuestions = role.screeningQuestions.mapIndexed { itemIndex, item ->
                    if (itemIndex == index) text.take(180) else item
                }
            )
        }
    }

    fun removeScreeningQuestion(
        roleTemplateId: String,
        index: Int
    ) {
        updateRoleConfiguration(roleTemplateId) { role ->
            role.copy(
                screeningQuestions = role.screeningQuestions
                    .filterIndexed { itemIndex, _ -> itemIndex != index }
            )
        }
    }

    fun updateRoleNotes(
        roleTemplateId: String,
        text: String
    ) {
        updateRoleConfiguration(roleTemplateId) { role ->
            role.copy(roleNotes = text.take(400))
        }
    }

    fun updateIndividualSubmissionRequirement(
        roleTemplateId: String,
        text: String
    ) {
        updateRoleConfiguration(roleTemplateId) { role ->
            role.copy(
                individualSubmissionRequirement = text.take(500)
            )
        }
    }

    fun updateSharedSubmissionResponsibleRole(
        roleTemplateId: String
    ) {
        val current = _uiState.value
        if (current.draft.remoteSubmissionMode != RemoteSubmissionMode.SHARED_TEAM) {
            return
        }

        val isSelectedRemoteRole = current.draft.selectedRoles.any { selectedRole ->
            selectedRole.roleTemplateId == roleTemplateId &&
                    current.roleCatalogue.firstOrNull {
                        it.roleTemplateId == roleTemplateId
                    }?.roleMode == VolunteerRoleMode.REMOTE
        }

        if (!isSelectedRemoteRole) return

        val newDraft = current.draft.copy(
            sharedSubmissionResponsibleRoleTemplateId = roleTemplateId
        )

        _uiState.update { state ->
            state.copy(
                draft = newDraft,
                roleSettingsError = null,
                isStepThreeReady = CreatePostValidator.validateStepThree(
                    draft = newDraft,
                    roleCatalogue = state.roleCatalogue
                ) == null
            )
        }
    }

    /** True when the current Step 3 values for this role can be saved. */
    fun canSaveRoleConfiguration(roleTemplateId: String): Boolean {
        val current = _uiState.value
        val template = current.roleCatalogue.firstOrNull {
            it.roleTemplateId == roleTemplateId
        } ?: return false
        val selectedRole = current.draft.selectedRoles.firstOrNull {
            it.roleTemplateId == roleTemplateId
        } ?: return false

        return CreatePostValidator.validateRoleConfiguration(
            draft = current.draft,
            selectedRole = selectedRole,
            template = template
        ).isEmpty()
    }

    /** Cleans, validates and marks one role Ready. */
    fun saveRoleConfiguration(roleTemplateId: String): Boolean {
        val current = _uiState.value
        val template = current.roleCatalogue.firstOrNull {
            it.roleTemplateId == roleTemplateId
        } ?: return false

        val selectedRole = current.draft.selectedRoles.firstOrNull {
            it.roleTemplateId == roleTemplateId
        } ?: return false

        val selectedMethod = selectedRole.applicationMethod

        val cleanedRole = selectedRole.copy(
            responsibilities = selectedRole.responsibilities
                .map { responsibility -> responsibility.trim() }
                .filter { responsibility -> responsibility.isNotEmpty() },
            applicationMethod = selectedMethod,
            screeningQuestions = if (
                selectedMethod == RoleApplicationMethod.REVIEW_APPLICANTS
            ) {
                selectedRole.screeningQuestions
                    .map { question -> question.trim() }
                    .filter { question -> question.isNotEmpty() }
                    .take(3)
            } else {
                emptyList()
            },
            roleNotes = selectedRole.roleNotes.trim(),
            individualSubmissionRequirement =
                selectedRole.individualSubmissionRequirement.trim(),
            isConfigured = false
        )

        val problems = CreatePostValidator.validateRoleConfiguration(
            draft = current.draft,
            selectedRole = cleanedRole,
            template = template
        )

        if (problems.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    roleSettingsError = problems.first(),
                    isStepThreeReady = false
                )
            }
            return false
        }

        val savedRole = cleanedRole.copy(isConfigured = true)
        val newDraft = current.draft.copy(
            selectedRoles = current.draft.selectedRoles.map { role ->
                if (role.roleTemplateId == roleTemplateId) savedRole else role
            }
        )

        val ready = CreatePostValidator.validateStepThree(
            draft = newDraft,
            roleCatalogue = current.roleCatalogue
        ) == null

        _uiState.update { state ->
            state.copy(
                draft = newDraft,
                roleSettingsError = null,
                isStepThreeReady = ready
            )
        }

        return true
    }

    /** Saves the current role and opens the next role that still needs review. */
    fun saveAndOpenNextRole(roleTemplateId: String): Boolean {
        if (!saveRoleConfiguration(roleTemplateId)) return false

        val stateAfterSave = _uiState.value
        val nextRoleId = stateAfterSave.draft.selectedRoles
            .firstOrNull { selectedRole ->
                selectedRole.roleTemplateId != roleTemplateId &&
                        !selectedRole.isConfigured
            }
            ?.roleTemplateId

        _uiState.update { state ->
            state.copy(
                editingRoleTemplateId = nextRoleId,
                roleSettingsError = null
            )
        }

        return true
    }

    /** Validates Step 3 before the Schedule step is opened. */
    fun continueFromStepThree(): Boolean {
        val current = _uiState.value
        val error = CreatePostValidator.validateStepThree(
            draft = current.draft,
            roleCatalogue = current.roleCatalogue
        )
        val ready = error == null

        _uiState.update { state ->
            state.copy(
                roleSettingsError = error,
                isStepThreeReady = ready,
                currentStep = if (ready) 4 else state.currentStep,
                editingRoleTemplateId = if (ready) null else state.editingRoleTemplateId
            )
        }

        return ready
    }

    fun backToStepThree() {
        _uiState.update { state ->
            state.copy(
                currentStep = 3,
                editingRoleTemplateId = null,
                roleSettingsError = null
            )
        }
    }

    private fun updateRoleConfiguration(
        roleTemplateId: String,
        transform: (SelectedRoleDraft) -> SelectedRoleDraft
    ) {
        _uiState.update { current ->
            val updatedRoles = current.draft.selectedRoles.map { selectedRole ->
                if (selectedRole.roleTemplateId == roleTemplateId) {
                    val transformed = transform(selectedRole)
                    if (transformed == selectedRole) {
                        selectedRole
                    } else {
                        transformed.copy(isConfigured = false)
                    }
                } else {
                    selectedRole
                }
            }

            val newDraft = current.draft.copy(selectedRoles = updatedRoles)

            current.copy(
                draft = newDraft,
                roleSettingsError = null,
                isStepThreeReady = false
            )
        }
    }

    private fun setRoleSettingsError(message: String) {
        _uiState.update { state ->
            state.copy(
                roleSettingsError = message,
                isStepThreeReady = false
            )
        }
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
