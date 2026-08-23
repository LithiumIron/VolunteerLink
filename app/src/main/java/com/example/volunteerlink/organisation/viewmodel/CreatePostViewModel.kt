package com.example.volunteerlink.organisation.viewmodel

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
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
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.TrainingMode
import com.example.volunteerlink.organisation.create.model.TrainingLocationMode
import com.example.volunteerlink.organisation.create.model.RoleApplicationMethod
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.organisation.repository.CreatePostRepository
import com.example.volunteerlink.organisation.repository.PublishThumbnail
import com.example.volunteerlink.organisation.repository.SupabaseCreatePostRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

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
    private var trainingLocationSearchJob: Job? = null
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
        if (_uiState.value.reviewEditStep == 1) {
            finishReviewEditAfterStepOne()
            return
        }

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
            } catch (exception: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isRoleCatalogueLoading = false,
                        roleCatalogueError = buildString {
                            append("Unable to load volunteer roles.")
                            exception.message
                                ?.takeIf { message -> message.isNotBlank() }
                                ?.let { message ->
                                    append("\n")
                                    append(message)
                                }
                        },
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
            if (_uiState.value.reviewEditStep == 2) {
                finishReviewEditAfterStepTwo()
            } else {
                openStepThree()
            }
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

    /** System Back: role editor -> overview; Review edit -> Review; otherwise Step 2. */
    fun backFromStepThree() {
        val current = _uiState.value

        if (current.editingRoleTemplateId != null) {
            closeRoleEditor()
            return
        }

        if (current.reviewEditStep == 3) {
            returnToReviewFromEdit()
            return
        }

        _uiState.update { state ->
            state.copy(
                currentStep = 2,
                roleSettingsError = null,
                isStepThreeReady = false
            )
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

        if (!ready) {
            _uiState.update { state ->
                state.copy(
                    roleSettingsError = error,
                    isStepThreeReady = false
                )
            }
            return false
        }

        if (current.reviewEditStep == 3) {
            _uiState.update { state ->
                state.copy(
                    roleSettingsError = null,
                    isStepThreeReady = true,
                    editingRoleTemplateId = null
                )
            }
            finishReviewEditAfterStepThree()
            return true
        }

        _uiState.update { state ->
            val sections = availableScheduleSections(state.draft.postType)
            state.copy(
                roleSettingsError = null,
                isStepThreeReady = true,
                currentStep = 4,
                editingRoleTemplateId = null,
                activeScheduleSection = state.activeScheduleSection
                    ?.takeIf { it in sections }
                    ?: sections.firstOrNull(),
                selectedPhysicalScheduleDateMillis = validSelectedPhysicalDate(
                    draft = state.draft,
                    currentDate = state.selectedPhysicalScheduleDateMillis
                ),
                isScheduleEditorOpen = false,
                trainingLocationSuggestions = emptyList(),
                isTrainingLocationSearching = false,
                trainingLocationSearchError = null,
                scheduleError = null,
                showScheduleErrors = false
            )
        }

        return true
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

    // ---------------------------------------------------------------------
    // Step 4: schedule
    // ---------------------------------------------------------------------

    fun selectScheduleSection(section: ScheduleType) {
        if (section !in availableScheduleSections(_uiState.value.draft.postType)) {
            return
        }

        _uiState.update { state ->
            state.copy(
                activeScheduleSection = section,
                scheduleError = null,
                showScheduleErrors = false
            )
        }
    }

    fun selectPhysicalScheduleDate(dateMillis: Long) {
        val date = CreatePostValidator.startOfDayMillis(dateMillis)
        if (date !in CreatePostValidator.physicalScheduleDates(_uiState.value.draft)) {
            return
        }

        _uiState.update { state ->
            state.copy(
                selectedPhysicalScheduleDateMillis = date,
                scheduleError = null,
                showScheduleErrors = false
            )
        }
    }

    fun openScheduleItemEditor(itemId: String) {
        val current = _uiState.value
        val pausedDraft = current.scheduleEditorDraft
        if (pausedDraft != null && !current.isScheduleEditorOpen) {
            setScheduleError(
                "You have unfinished schedule input. Resume or discard it before editing another item."
            )
            return
        }

        val item = current.draft.scheduleItems.firstOrNull { existing ->
            existing.draftId == itemId
        } ?: return

        trainingLocationSearchJob?.cancel()

        _uiState.update { state ->
            state.copy(
                activeScheduleSection = item.scheduleType,
                editingScheduleItemId = itemId,
                scheduleEditorDraft = item,
                isScheduleEditorOpen = true,
                trainingLocationSuggestions = emptyList(),
                isTrainingLocationSearching = false,
                trainingLocationSearchError = null,
                scheduleError = null,
                showScheduleErrors = false
            )
        }
    }

    /**
     * Leaves the editor but keeps every typed value in the Step 4 UI state.
     * The overview shows a Resume / Discard card instead of creating an
     * incomplete saved schedule item.
     */
    fun closeScheduleItemEditor() {
        trainingLocationSearchJob?.cancel()

        _uiState.update { state ->
            state.copy(
                isScheduleEditorOpen = false,
                trainingLocationSuggestions = emptyList(),
                isTrainingLocationSearching = false,
                trainingLocationSearchError = null,
                scheduleError = null,
                showScheduleErrors = false
            )
        }
    }

    fun resumeScheduleEditorDraft() {
        val item = _uiState.value.scheduleEditorDraft ?: return
        _uiState.update { state ->
            state.copy(
                activeScheduleSection = item.scheduleType,
                selectedPhysicalScheduleDateMillis = if (
                    item.scheduleType == ScheduleType.PHYSICAL
                ) {
                    item.scheduleDateMillis
                } else {
                    state.selectedPhysicalScheduleDateMillis
                },
                isScheduleEditorOpen = true,
                scheduleError = null,
                showScheduleErrors = false
            )
        }
    }

    fun discardScheduleEditorDraft() {
        trainingLocationSearchJob?.cancel()
        _uiState.update { state ->
            state.copy(
                editingScheduleItemId = null,
                scheduleEditorDraft = null,
                isScheduleEditorOpen = false,
                trainingLocationSuggestions = emptyList(),
                isTrainingLocationSearching = false,
                trainingLocationSearchError = null,
                scheduleError = null,
                showScheduleErrors = false
            )
        }
    }

    /** System Back: item editor -> overview; Review edit -> Review; otherwise Step 3. */
    fun backFromStepFour() {
        val current = _uiState.value

        if (current.isScheduleEditorOpen) {
            closeScheduleItemEditor()
            return
        }

        if (current.reviewEditStep == 4) {
            returnToReviewFromEdit()
            return
        }

        _uiState.update { state ->
            state.copy(
                currentStep = 3,
                isScheduleEditorOpen = false,
                scheduleError = null,
                showScheduleErrors = false,
                isStepFourReady = false
            )
        }
    }

    /**
     * Starts a new Physical activity editor.
     *
     * Do not try to find a free slot here. The organiser chooses the time and
     * overlap validation runs only when Save is pressed.
     */
    fun addPhysicalScheduleItem(dateMillis: Long): String? {
        val current = _uiState.value
        val pausedDraft = current.scheduleEditorDraft
        if (pausedDraft != null) {
            resumeScheduleEditorDraft()
            return pausedDraft.draftId
        }

        val date = CreatePostValidator.startOfDayMillis(dateMillis)

        if (date !in CreatePostValidator.physicalScheduleDates(current.draft)) {
            setScheduleError("Choose one of the Physical event dates.")
            return null
        }

        val roleIds = CreatePostValidator.applicableScheduleRoleIds(
            draft = current.draft,
            scheduleType = ScheduleType.PHYSICAL,
            roleCatalogue = current.roleCatalogue
        )

        if (roleIds.isEmpty()) {
            setScheduleError("Return to Step 2 and select at least one Physical role.")
            return null
        }

        val eventStart = current.draft.physicalStartTimeMinutes
        val eventEnd = current.draft.physicalEndTimeMinutes
        if (eventStart == null || eventEnd == null || eventEnd <= eventStart) {
            setScheduleError("Return to Step 1 and set a valid Physical event time.")
            return null
        }

        val newId = UUID.randomUUID().toString()
        val newItem = ScheduleItemDraft(
            draftId = newId,
            scheduleType = ScheduleType.PHYSICAL,
            scheduleDateMillis = date,
            appliesToAllRoles = roleIds.size > 1,
            targetRoleTemplateIds = roleIds.singleOrNull()?.let(::listOf).orEmpty()
        )

        openNewScheduleEditor(
            item = newItem,
            selectedPhysicalDate = date
        )
        return newId
    }

    /** Starts a new Training / Briefing editor without saving an incomplete item. */
    fun addTrainingScheduleItem(): String? {
        val current = _uiState.value
        val pausedDraft = current.scheduleEditorDraft
        if (pausedDraft != null) {
            resumeScheduleEditorDraft()
            return pausedDraft.draftId
        }

        val roleIds = CreatePostValidator.applicableScheduleRoleIds(
            draft = current.draft,
            scheduleType = ScheduleType.TRAINING,
            roleCatalogue = current.roleCatalogue
        )

        if (roleIds.isEmpty()) {
            setScheduleError("Return to Step 2 and select at least one role.")
            return null
        }

        val today = CreatePostValidator.startOfDayMillis()
        val starts = listOfNotNull(
            current.draft.physicalStartDateMillis,
            current.draft.remoteStartDateMillis
        ).map(CreatePostValidator::startOfDayMillis)

        val suggestedDate = starts.minOrNull()
            ?.let(::previousDayMillis)
            ?.coerceAtLeast(today)
            ?: today

        val newId = UUID.randomUUID().toString()
        val newItem = ScheduleItemDraft(
            draftId = newId,
            scheduleType = ScheduleType.TRAINING,
            scheduleDateMillis = suggestedDate,
            appliesToAllRoles = roleIds.size > 1,
            targetRoleTemplateIds = roleIds.singleOrNull()?.let(::listOf).orEmpty(),
            trainingMode = TrainingMode.ONLINE,
            trainingTimeZoneId = null,
            closingRoleTemplateIds = emptyList()
        )

        openNewScheduleEditor(newItem)
        return newId
    }

    /** Starts a new date-based Remote milestone editor. */
    fun addRemoteScheduleItem(): String? {
        val current = _uiState.value
        val pausedDraft = current.scheduleEditorDraft
        if (pausedDraft != null) {
            resumeScheduleEditorDraft()
            return pausedDraft.draftId
        }

        val start = current.draft.remoteStartDateMillis
            ?.let(CreatePostValidator::startOfDayMillis)
            ?: run {
                setScheduleError("Return to Step 1 and set the Remote start date.")
                return null
            }
        val due = current.draft.remoteDueDateMillis
            ?.let(CreatePostValidator::startOfDayMillis)
            ?: run {
                setScheduleError("Return to Step 1 and set the Remote due date.")
                return null
            }

        val roleIds = CreatePostValidator.applicableScheduleRoleIds(
            draft = current.draft,
            scheduleType = ScheduleType.REMOTE,
            roleCatalogue = current.roleCatalogue
        )

        if (roleIds.isEmpty()) {
            setScheduleError("Return to Step 2 and select at least one Remote role.")
            return null
        }

        val latestDate = current.draft.scheduleItems
            .filter { item -> item.scheduleType == ScheduleType.REMOTE }
            .mapNotNull { item -> item.scheduleDateMillis }
            .map(CreatePostValidator::startOfDayMillis)
            .maxOrNull()

        val suggestedDate = if (latestDate == null) {
            start
        } else {
            CreatePostValidator.nextDayMillis(latestDate)
                .coerceAtMost(due)
                .coerceAtLeast(start)
        }

        val newId = UUID.randomUUID().toString()
        val newItem = ScheduleItemDraft(
            draftId = newId,
            scheduleType = ScheduleType.REMOTE,
            scheduleDateMillis = suggestedDate,
            appliesToAllRoles = roleIds.size > 1,
            targetRoleTemplateIds = roleIds.singleOrNull()?.let(::listOf).orEmpty()
        )

        openNewScheduleEditor(newItem)
        return newId
    }

    fun removeScheduleItem(itemId: String) {
        updateStepFourDraft { draft ->
            draft.copy(
                scheduleItems = draft.scheduleItems.filterNot { item ->
                    item.draftId == itemId
                }
            )
        }

        if (_uiState.value.editingScheduleItemId == itemId) {
            discardScheduleEditorDraft()
        }
    }

    fun updateScheduleEditorTitle(text: String) {
        updateScheduleEditor { item ->
            item.copy(title = text.take(120))
        }
    }

    fun updateScheduleEditorNotes(text: String) {
        updateScheduleEditor { item ->
            item.copy(notes = text.take(500))
        }
    }

    fun updateScheduleEditorLocation(text: String) {
        updateScheduleEditor { item ->
            if (item.scheduleType == ScheduleType.PHYSICAL) {
                item.copy(location = text.take(180))
            } else {
                item
            }
        }
    }

    fun updateScheduleEditorDate(dateMillis: Long) {
        val date = CreatePostValidator.startOfDayMillis(dateMillis)
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return

        val accepted = when (item.scheduleType) {
            ScheduleType.PHYSICAL -> false

            ScheduleType.REMOTE -> {
                val start = current.draft.remoteStartDateMillis
                    ?.let(CreatePostValidator::startOfDayMillis)
                val due = current.draft.remoteDueDateMillis
                    ?.let(CreatePostValidator::startOfDayMillis)
                start != null && due != null && date in start..due
            }

            ScheduleType.TRAINING -> {
                date >= CreatePostValidator.startOfDayMillis()
            }
        }

        if (accepted) {
            updateScheduleEditor { existing ->
                existing.copy(scheduleDateMillis = date)
            }
        }
    }

    fun updateScheduleEditorStartTime(
        hour: Int,
        minute: Int
    ): String? {
        val time = hour * 60 + minute
        if (time !in 0..1439) {
            return "Choose a valid start time."
        }

        val item = _uiState.value.scheduleEditorDraft
            ?: return "This schedule editor is no longer open."

        if (item.scheduleType == ScheduleType.REMOTE) {
            return "Remote milestones do not use a clock time."
        }

        if (item.scheduleType == ScheduleType.PHYSICAL) {
            val eventStart = _uiState.value.draft.physicalStartTimeMinutes
                ?: return "Set the event start time in Step 1 first."
            val eventEnd = _uiState.value.draft.physicalEndTimeMinutes
                ?: return "Set the event end time in Step 1 first."

            if (time < eventStart || time >= eventEnd) {
                return "Start time must be inside the Physical event time."
            }
        }

        updateScheduleEditor { current ->
            current.copy(
                startTimeMinutes = time,
                endTimeMinutes = current.endTimeMinutes
                    ?.takeIf { end -> end > time }
            )
        }
        return null
    }

    fun updateScheduleEditorEndTime(
        hour: Int,
        minute: Int
    ): String? {
        val time = hour * 60 + minute
        if (time !in 0..1439) {
            return "Choose a valid end time."
        }

        val item = _uiState.value.scheduleEditorDraft
            ?: return "This schedule editor is no longer open."

        if (item.scheduleType == ScheduleType.REMOTE) {
            return "Remote milestones do not use a clock time."
        }

        val start = item.startTimeMinutes
            ?: return "Select the start time first."
        if (time <= start) {
            return "End time must be later than the start time."
        }

        if (item.scheduleType == ScheduleType.PHYSICAL) {
            val eventEnd = _uiState.value.draft.physicalEndTimeMinutes
                ?: return "Set the event end time in Step 1 first."
            if (time > eventEnd) {
                return "End time must be inside the Physical event time."
            }
        }

        updateScheduleEditor { current ->
            current.copy(endTimeMinutes = time)
        }
        return null
    }

    fun updateTrainingMode(trainingMode: TrainingMode) {
        val hasEventLocation = _uiState.value.draft.physicalLocation != null

        updateScheduleEditor { item ->
            if (item.scheduleType != ScheduleType.TRAINING) {
                item
            } else {
                when (trainingMode) {
                    TrainingMode.ONLINE -> item.copy(
                        trainingMode = TrainingMode.ONLINE,
                        trainingLocationMode = null,
                        trainingLocationQuery = "",
                        trainingLocation = null,
                        location = "",
                        trainingTimeZoneId = null
                    )

                    TrainingMode.ONSITE -> {
                        val locationMode = if (hasEventLocation) {
                            TrainingLocationMode.EVENT_LOCATION
                        } else {
                            TrainingLocationMode.TBA
                        }

                        item.copy(
                            trainingMode = TrainingMode.ONSITE,
                            trainingLocationMode = locationMode,
                            trainingLocationQuery = "",
                            trainingLocation = null,
                            location = if (locationMode == TrainingLocationMode.EVENT_LOCATION) {
                                eventLocationText(_uiState.value.draft)
                            } else {
                                ""
                            },
                            onlinePlatform = "",
                            meetingLink = "",
                            trainingTimeZoneId = null
                        )
                    }
                }
            }
        }

        clearTrainingLocationSearchUi()
    }

    fun updateTrainingOnlinePlatform(text: String) {
        updateScheduleEditor { item ->
            if (
                item.scheduleType == ScheduleType.TRAINING &&
                item.trainingMode == TrainingMode.ONLINE
            ) {
                item.copy(onlinePlatform = text.take(100))
            } else {
                item
            }
        }
    }

    fun updateTrainingMeetingLink(text: String) {
        updateScheduleEditor { item ->
            if (
                item.scheduleType == ScheduleType.TRAINING &&
                item.trainingMode == TrainingMode.ONLINE
            ) {
                item.copy(meetingLink = text.take(500))
            } else {
                item
            }
        }
    }

    fun updateTrainingLocationMode(mode: TrainingLocationMode) {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return
        if (
            item.scheduleType != ScheduleType.TRAINING ||
            item.trainingMode != TrainingMode.ONSITE
        ) {
            return
        }

        if (mode == TrainingLocationMode.EVENT_LOCATION) {
            val eventLocation = eventLocationText(current.draft)
            if (eventLocation.isBlank()) {
                setScheduleError("The Physical event location is not available for this post.")
                return
            }
        }

        trainingLocationSearchJob?.cancel()
        updateScheduleEditor { existing ->
            existing.copy(
                trainingLocationMode = mode,
                trainingLocationQuery = "",
                trainingLocation = null,
                location = when (mode) {
                    TrainingLocationMode.EVENT_LOCATION -> eventLocationText(current.draft)
                    TrainingLocationMode.CUSTOM,
                    TrainingLocationMode.TBA -> ""
                },
                trainingTimeZoneId = null
            )
        }
        clearTrainingLocationSearchUi()
    }

    fun onTrainingLocationQueryChanged(query: String) {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return
        if (
            item.scheduleType != ScheduleType.TRAINING ||
            item.trainingMode != TrainingMode.ONSITE ||
            item.trainingLocationMode != TrainingLocationMode.CUSTOM
        ) {
            return
        }

        trainingLocationSearchJob?.cancel()

        updateScheduleEditor { existing ->
            existing.copy(
                trainingLocationQuery = query,
                trainingLocation = null,
                location = ""
            )
        }

        _uiState.update { state ->
            state.copy(
                trainingLocationSuggestions = emptyList(),
                isTrainingLocationSearching = false,
                trainingLocationSearchError = null
            )
        }

        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return

        trainingLocationSearchJob = viewModelScope.launch {
            delay(350)

            if (BuildConfig.GEOAPIFY_API_KEY.isBlank()) {
                _uiState.update { state ->
                    state.copy(
                        isTrainingLocationSearching = false,
                        trainingLocationSearchError = "Geoapify API key is missing."
                    )
                }
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    isTrainingLocationSearching = true,
                    trainingLocationSearchError = null
                )
            }

            try {
                val results = locationService.searchLocations(
                    query = cleanQuery,
                    biasLatitude = locationBiasLatitude,
                    biasLongitude = locationBiasLongitude
                )

                val latest = _uiState.value.scheduleEditorDraft
                if (
                    latest?.trainingLocationMode == TrainingLocationMode.CUSTOM &&
                    latest.trainingLocationQuery.trim() == cleanQuery &&
                    latest.trainingLocation == null
                ) {
                    _uiState.update { state ->
                        state.copy(
                            trainingLocationSuggestions = results,
                            isTrainingLocationSearching = false,
                            trainingLocationSearchError = if (results.isEmpty()) {
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
                val latest = _uiState.value.scheduleEditorDraft
                if (latest?.trainingLocationQuery?.trim() == cleanQuery) {
                    _uiState.update { state ->
                        state.copy(
                            trainingLocationSuggestions = emptyList(),
                            isTrainingLocationSearching = false,
                            trainingLocationSearchError = "Unable to search locations."
                        )
                    }
                }
            }
        }
    }

    fun onTrainingLocationSelected(location: LocationSuggestion) {
        trainingLocationSearchJob?.cancel()

        updateScheduleEditor { item ->
            if (
                item.scheduleType == ScheduleType.TRAINING &&
                item.trainingMode == TrainingMode.ONSITE &&
                item.trainingLocationMode == TrainingLocationMode.CUSTOM
            ) {
                item.copy(
                    trainingLocationQuery = location.address.ifBlank {
                        location.displayName
                    },
                    trainingLocation = location,
                    location = location.address.ifBlank {
                        location.displayName
                    }.take(180),
                    trainingTimeZoneId = null
                )
            } else {
                item
            }
        }

        clearTrainingLocationSearchUi()
    }

    fun clearTrainingLocation() {
        trainingLocationSearchJob?.cancel()
        updateScheduleEditor { item ->
            if (item.trainingLocationMode == TrainingLocationMode.CUSTOM) {
                item.copy(
                    trainingLocationQuery = "",
                    trainingLocation = null,
                    location = "",
                    trainingTimeZoneId = null
                )
            } else {
                item
            }
        }
        clearTrainingLocationSearchUi()
    }

    /**
     * Toggles a role only when no other saved Training already owns that
     * role's application cutoff. Moving a cutoff is a separate confirmed
     * action so a later Training can never silently replace an earlier choice.
     */
    fun toggleTrainingClosingRole(roleTemplateId: String) {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return
        if (item.scheduleType != ScheduleType.TRAINING) return

        val targetedRoleIds = effectiveEditorTargetRoleIds(
            state = current,
            item = item
        )
        if (roleTemplateId !in targetedRoleIds) return

        val isAlreadyChecked =
            roleTemplateId in item.closingRoleTemplateIds

        if (!isAlreadyChecked && otherTrainingApplicationCutoff(roleTemplateId) != null) {
            // The UI will offer Move Cutoff only when this editor is earlier.
            // Never create two owners through the normal toggle action.
            return
        }

        updateScheduleEditor { existing ->
            val changedClosingRoles = if (isAlreadyChecked) {
                existing.closingRoleTemplateIds - roleTemplateId
            } else {
                existing.closingRoleTemplateIds + roleTemplateId
            }

            existing.copy(
                closingRoleTemplateIds = changedClosingRoles.distinct()
            )
        }
    }

    /**
     * Returns the saved Training that currently owns this role's cutoff,
     * excluding the Training that is being edited. Normally there is at most
     * one; minByOrNull keeps old test data deterministic until SQL migration.
     */
    fun otherTrainingApplicationCutoff(
        roleTemplateId: String
    ): ScheduleItemDraft? {
        val current = _uiState.value
        val editorId = current.scheduleEditorDraft?.draftId

        return current.draft.scheduleItems
            .asSequence()
            .filter { item ->
                item.draftId != editorId &&
                    item.scheduleType == ScheduleType.TRAINING &&
                    roleTemplateId in item.closingRoleTemplateIds
            }
            .minByOrNull { item ->
                trainingStartOrderValue(item) ?: Long.MAX_VALUE
            }
    }

    /**
     * Called only after the organiser confirms moving an existing cutoff to
     * this earlier Training. The old saved owner is not changed yet; the move
     * is committed atomically to the wizard draft only when Save is pressed.
     */
    fun moveTrainingApplicationCutoff(roleTemplateId: String) {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return
        if (item.scheduleType != ScheduleType.TRAINING) return

        val targetedRoleIds = effectiveEditorTargetRoleIds(
            state = current,
            item = item
        )
        if (roleTemplateId !in targetedRoleIds) return

        val oldCutoff = otherTrainingApplicationCutoff(roleTemplateId) ?: return
        val newStart = trainingStartOrderValue(item) ?: return
        val oldStart = trainingStartOrderValue(oldCutoff) ?: return

        // Moving is offered only to an earlier Training. A same-time or later
        // Training cannot replace the existing owner.
        if (newStart >= oldStart) return

        updateScheduleEditor { existing ->
            existing.copy(
                closingRoleTemplateIds =
                    (existing.closingRoleTemplateIds + roleTemplateId)
                        .distinct()
            )
        }
    }


    fun updateScheduleEditorAppliesToAll(appliesToAll: Boolean) {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return
        val availableRoleIds = CreatePostValidator.applicableScheduleRoleIds(
            draft = current.draft,
            scheduleType = item.scheduleType,
            roleCatalogue = current.roleCatalogue
        )

        if (availableRoleIds.isEmpty()) return

        updateScheduleEditor { existing ->
            when {
                availableRoleIds.size == 1 -> {
                    val targets = availableRoleIds
                    existing.copy(
                        appliesToAllRoles = false,
                        targetRoleTemplateIds = targets,
                        closingRoleTemplateIds =
                            existing.closingRoleTemplateIds
                                .filter { roleId -> roleId in targets }
                    )
                }

                appliesToAll -> existing.copy(
                    appliesToAllRoles = true,
                    targetRoleTemplateIds = emptyList(),
                    closingRoleTemplateIds =
                        existing.closingRoleTemplateIds
                            .filter { roleId -> roleId in availableRoleIds }
                )

                else -> {
                    val targets = existing.targetRoleTemplateIds
                        .filter { roleId -> roleId in availableRoleIds }
                        .ifEmpty { listOf(availableRoleIds.first()) }

                    existing.copy(
                        appliesToAllRoles = false,
                        targetRoleTemplateIds = targets,
                        closingRoleTemplateIds =
                            existing.closingRoleTemplateIds
                                .filter { roleId -> roleId in targets }
                    )
                }
            }
        }
    }

    fun toggleScheduleEditorRole(roleTemplateId: String) {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return
        val availableRoleIds = CreatePostValidator.applicableScheduleRoleIds(
            draft = current.draft,
            scheduleType = item.scheduleType,
            roleCatalogue = current.roleCatalogue
        )

        if (roleTemplateId !in availableRoleIds) return

        updateScheduleEditor { existing ->
            val currentTargets = if (existing.appliesToAllRoles) {
                emptyList()
            } else {
                existing.targetRoleTemplateIds
                    .filter { roleId -> roleId in availableRoleIds }
            }

            val changedTargets = if (roleTemplateId in currentTargets) {
                currentTargets - roleTemplateId
            } else {
                currentTargets + roleTemplateId
            }.distinct()

            existing.copy(
                appliesToAllRoles = false,
                targetRoleTemplateIds = changedTargets,
                closingRoleTemplateIds =
                    existing.closingRoleTemplateIds
                        .filter { roleId -> roleId in changedTargets }
            )
        }
    }

    /** Validates the temporary editor buffer without saving it to CreatePostDraft. */
    fun validateScheduleEditor(): Boolean {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return false
        val cleanedItem = cleanScheduleItem(item, current.draft)
        val cutoffMoveError = trainingCutoffMoveError(
            state = current,
            item = cleanedItem
        )
        val candidateDraft = draftWithEditorItem(current.draft, cleanedItem)

        val error = cutoffMoveError ?: CreatePostValidator.validateScheduleItem(
            draft = candidateDraft,
            item = cleanedItem,
            roleCatalogue = current.roleCatalogue
        )

        _uiState.update { state ->
            state.copy(
                scheduleEditorDraft = cleanedItem,
                scheduleError = error,
                showScheduleErrors = error != null,
                isStepFourReady = false
            )
        }

        return error == null
    }

    /** Commits a valid editor buffer to the shared CreatePostDraft. */
    fun saveScheduleEditor(): Boolean {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return false
        val cleanedItem = cleanScheduleItem(item, current.draft)
        val cutoffMoveError = trainingCutoffMoveError(
            state = current,
            item = cleanedItem
        )
        val candidateDraft = draftWithEditorItem(current.draft, cleanedItem)

        val error = cutoffMoveError ?: CreatePostValidator.validateScheduleItem(
            draft = candidateDraft,
            item = cleanedItem,
            roleCatalogue = current.roleCatalogue
        )

        if (error != null) {
            _uiState.update { state ->
                state.copy(
                    scheduleEditorDraft = cleanedItem,
                    scheduleError = error,
                    showScheduleErrors = true,
                    isStepFourReady = false
                )
            }
            return false
        }

        trainingLocationSearchJob?.cancel()
        _uiState.update { state ->
            state.copy(
                draft = candidateDraft,
                activeScheduleSection = cleanedItem.scheduleType,
                selectedPhysicalScheduleDateMillis = if (
                    cleanedItem.scheduleType == ScheduleType.PHYSICAL
                ) {
                    cleanedItem.scheduleDateMillis
                } else {
                    state.selectedPhysicalScheduleDateMillis
                },
                editingScheduleItemId = null,
                scheduleEditorDraft = null,
                isScheduleEditorOpen = false,
                trainingLocationSuggestions = emptyList(),
                isTrainingLocationSearching = false,
                trainingLocationSearchError = null,
                scheduleError = null,
                showScheduleErrors = false,
                isStepFourReady = false
            )
        }
        return true
    }

    fun getScheduleEditorWarning(): String? {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return null
        if (!CreatePostValidator.trainingStartsWithinShortNotice(item)) {
            return null
        }

        return CreatePostValidator.scheduleItemWarning(
            draft = draftWithEditorItem(current.draft, item),
            item = item
        )
    }

    /** Used by the overview to mark only saved items invalidated by earlier steps. */
    fun getScheduleItemValidationMessage(itemId: String): String? {
        val current = _uiState.value
        val item = current.draft.scheduleItems.firstOrNull { existing ->
            existing.draftId == itemId
        } ?: return null

        return CreatePostValidator.validateScheduleItem(
            draft = current.draft,
            item = item,
            roleCatalogue = current.roleCatalogue
        )
    }

    fun getScheduleItemWarning(itemId: String): String? {
        val current = _uiState.value
        val item = current.draft.scheduleItems.firstOrNull { existing ->
            existing.draftId == itemId
        } ?: return null

        return CreatePostValidator.scheduleItemWarning(
            draft = current.draft,
            item = item
        )
    }

    /**
     * Copies one complete Physical day to another event date.
     * The copied activities receive new local draft IDs. Training and Remote
     * items on the target date are never touched.
     */
    fun copyPhysicalScheduleDay(
        sourceDateMillis: Long,
        targetDateMillis: Long,
        replaceExisting: Boolean
    ): Boolean {
        val current = _uiState.value
        val sourceDate = CreatePostValidator.startOfDayMillis(sourceDateMillis)
        val targetDate = CreatePostValidator.startOfDayMillis(targetDateMillis)
        val allowedDates = CreatePostValidator.physicalScheduleDates(current.draft)

        if (
            sourceDate == targetDate ||
            sourceDate !in allowedDates ||
            targetDate !in allowedDates
        ) {
            setScheduleError("Choose two different Physical event dates.")
            return false
        }

        val sourceItems = current.draft.scheduleItems.filter { item ->
            item.scheduleType == ScheduleType.PHYSICAL &&
                item.scheduleDateMillis?.let(
                    CreatePostValidator::startOfDayMillis
                ) == sourceDate
        }

        if (sourceItems.isEmpty()) {
            setScheduleError("There is no Physical timetable to copy from this day.")
            return false
        }

        val sourceError = sourceItems.firstNotNullOfOrNull { item ->
            CreatePostValidator.validateScheduleItem(
                draft = current.draft,
                item = item,
                roleCatalogue = current.roleCatalogue
            )
        }

        if (sourceError != null) {
            setScheduleError(
                "Fix the source day's timetable before copying it. $sourceError"
            )
            return false
        }

        val targetHasItems = physicalScheduleDayHasItems(targetDate)
        if (targetHasItems && !replaceExisting) {
            return false
        }

        val keptItems = if (replaceExisting) {
            current.draft.scheduleItems.filterNot { item ->
                item.scheduleType == ScheduleType.PHYSICAL &&
                    item.scheduleDateMillis?.let(
                        CreatePostValidator::startOfDayMillis
                    ) == targetDate
            }
        } else {
            current.draft.scheduleItems
        }

        val copies = sourceItems.map { source ->
            source.copy(
                draftId = UUID.randomUUID().toString(),
                scheduleDateMillis = targetDate
            )
        }

        val candidateDraft = current.draft.copy(
            scheduleItems = keptItems + copies
        )

        val copiedError = copies.firstNotNullOfOrNull { item ->
            CreatePostValidator.validateScheduleItem(
                draft = candidateDraft,
                item = item,
                roleCatalogue = current.roleCatalogue
            )
        }

        if (copiedError != null) {
            setScheduleError(
                "The copied timetable conflicts with the target day. $copiedError"
            )
            return false
        }

        _uiState.update { state ->
            state.copy(
                draft = candidateDraft,
                selectedPhysicalScheduleDateMillis = targetDate,
                scheduleError = null,
                showScheduleErrors = false,
                isStepFourReady = false
            )
        }
        return true
    }

    fun physicalScheduleDayHasItems(dateMillis: Long): Boolean {
        val date = CreatePostValidator.startOfDayMillis(dateMillis)
        return _uiState.value.draft.scheduleItems.any { item ->
            item.scheduleType == ScheduleType.PHYSICAL &&
                item.scheduleDateMillis?.let(
                    CreatePostValidator::startOfDayMillis
                ) == date
        }
    }

    fun canCopyPhysicalScheduleDay(dateMillis: Long): Boolean {
        val current = _uiState.value
        val date = CreatePostValidator.startOfDayMillis(dateMillis)
        val items = current.draft.scheduleItems.filter { item ->
            item.scheduleType == ScheduleType.PHYSICAL &&
                item.scheduleDateMillis?.let(
                    CreatePostValidator::startOfDayMillis
                ) == date
        }

        return items.isNotEmpty() && items.all { item ->
            CreatePostValidator.validateScheduleItem(
                draft = current.draft,
                item = item,
                roleCatalogue = current.roleCatalogue
            ) == null
        }
    }

    fun validateScheduleForContinue(): Boolean {
        val current = _uiState.value
        if (current.scheduleEditorDraft != null) {
            setScheduleError("You have unfinished schedule input. Resume it and save, or discard it before continuing.")
            return false
        }

        val error = CreatePostValidator.validateStepFour(
            draft = current.draft,
            roleCatalogue = current.roleCatalogue
        )

        _uiState.update { state ->
            state.copy(
                scheduleError = error,
                showScheduleErrors = error != null,
                isStepFourReady = error == null
            )
        }
        return error == null
    }

    fun getScheduleProceedWarning(): String? {
        return CreatePostValidator.scheduleProceedWarning(
            draft = _uiState.value.draft
        )
    }

    // ---------------------------------------------------------------------
    // Review Summary navigation
    // ---------------------------------------------------------------------

    /**
     * Opens Review Summary after Step 4 has been validated.
     * Save Draft and Publish are both available from Review.
     */
    fun openReviewSummary() {
        if (!validateScheduleForContinue()) return

        _uiState.update { state ->
            state.copy(
                currentStep = 5,
                reviewEditStep = null,
                editingRoleTemplateId = null,
                isScheduleEditorOpen = false,
                scheduleError = null,
                showScheduleErrors = false,
                saveDraftError = null,
                publishError = null
            )
        }
    }

    /** System Back from Review follows the normal wizard order to Step 4. */
    fun backFromReview() {
        _uiState.update { state ->
            val sections = availableScheduleSections(state.draft.postType)
            state.copy(
                currentStep = 4,
                reviewEditStep = null,
                activeScheduleSection = state.activeScheduleSection
                    ?.takeIf { it in sections }
                    ?: sections.firstOrNull(),
                selectedPhysicalScheduleDateMillis = validSelectedPhysicalDate(
                    draft = state.draft,
                    currentDate = state.selectedPhysicalScheduleDateMillis
                ),
                editingScheduleItemId = null,
                isScheduleEditorOpen = false,
                scheduleError = null,
                showScheduleErrors = false
            )
        }
    }

    /**
     * Review has one Edit action for each major wizard section. The edited
     * page reuses the existing form and keeps the same shared CreatePostDraft.
     */
    fun editStepFromReview(step: Int) {
        if (step !in 1..4 || _uiState.value.currentStep != 5) return

        _uiState.update { state ->
            val defaultRoleFilter = when (state.draft.postType) {
                VolunteerPostType.PHYSICAL -> VolunteerRoleMode.PHYSICAL
                VolunteerPostType.REMOTE -> VolunteerRoleMode.REMOTE
                VolunteerPostType.HYBRID ->
                    state.roleModeFilter ?: VolunteerRoleMode.PHYSICAL
                null -> state.roleModeFilter
            }
            val sections = availableScheduleSections(state.draft.postType)

            state.copy(
                currentStep = step,
                reviewEditStep = step,
                roleModeFilter = if (step == 2) {
                    defaultRoleFilter
                } else {
                    state.roleModeFilter
                },
                roleSearchQuery = if (step == 2) "" else state.roleSearchQuery,
                editingRoleTemplateId = null,
                activeScheduleSection = if (step == 4) {
                    state.activeScheduleSection
                        ?.takeIf { it in sections }
                        ?: sections.firstOrNull()
                } else {
                    state.activeScheduleSection
                },
                selectedPhysicalScheduleDateMillis = if (step == 4) {
                    validSelectedPhysicalDate(
                        draft = state.draft,
                        currentDate = state.selectedPhysicalScheduleDateMillis
                    )
                } else {
                    state.selectedPhysicalScheduleDateMillis
                },
                editingScheduleItemId = if (step == 4) null else state.editingScheduleItemId,
                isScheduleEditorOpen = false,
                showValidationErrors = if (step == 1) false else state.showValidationErrors,
                showRoleSelectionErrors = if (step == 2) false else state.showRoleSelectionErrors,
                roleSettingsError = if (step == 3) null else state.roleSettingsError,
                scheduleError = if (step == 4) null else state.scheduleError,
                showScheduleErrors = if (step == 4) false else state.showScheduleErrors,
                publishError = null
            )
        }
    }

    /**
     * Back from an Edit opened by Review returns to Review without clearing
     * the shared draft. This matches the wizard's existing live-edit model.
     */
    fun returnToReviewFromEdit() {
        _uiState.update { state ->
            state.copy(
                currentStep = 5,
                reviewEditStep = null,
                editingRoleTemplateId = null,
                isScheduleEditorOpen = false,
                publishError = null
            )
        }
    }

    /**
     * Step 1 can invalidate later sections when dates, mode or capacities are
     * changed. Save Changes therefore sends the organiser only to the first
     * dependent section that now needs attention.
     */
    private fun finishReviewEditAfterStepOne() {
        val current = _uiState.value
        val stepTwoErrors = CreatePostValidator.validateStepTwo(
            draft = current.draft,
            roleCatalogue = current.roleCatalogue
        )

        if (stepTwoErrors.hasErrors()) {
            val defaultFilter = when (current.draft.postType) {
                VolunteerPostType.PHYSICAL -> VolunteerRoleMode.PHYSICAL
                VolunteerPostType.REMOTE -> VolunteerRoleMode.REMOTE
                VolunteerPostType.HYBRID ->
                    current.roleModeFilter ?: VolunteerRoleMode.PHYSICAL
                null -> current.roleModeFilter
            }

            _uiState.update { state ->
                state.copy(
                    currentStep = 2,
                    reviewEditStep = 2,
                    roleModeFilter = defaultFilter,
                    roleSearchQuery = "",
                    roleSelectionErrors = stepTwoErrors,
                    showRoleSelectionErrors = true,
                    isStepTwoReady = false
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                reviewEditStep = 2,
                roleSelectionErrors = stepTwoErrors,
                showRoleSelectionErrors = false,
                isStepTwoReady = true
            )
        }
        finishReviewEditAfterStepTwo()
    }

    /**
     * Role selection changes may add a role that has never been configured.
     * Reuse openStepThree() so Supabase recommendations/defaults are prepared
     * before deciding whether Review can be shown again.
     */
    private fun finishReviewEditAfterStepTwo() {
        _uiState.update { state ->
            state.copy(reviewEditStep = 3)
        }

        openStepThree()

        if (_uiState.value.isStepThreeReady) {
            finishReviewEditAfterStepThree()
        }
    }

    /**
     * Step 3 changes can affect Step 4 validation. If Schedule needs repair,
     * keep the organiser in the Review-edit context so Save Changes eventually
     * returns to Review instead of restarting the wizard.
     */
    private fun finishReviewEditAfterStepThree() {
        val current = _uiState.value

        val scheduleError = if (current.scheduleEditorDraft != null) {
            "You have unfinished schedule input. Resume it and save, or discard it before returning to Review."
        } else {
            CreatePostValidator.validateStepFour(
                draft = current.draft,
                roleCatalogue = current.roleCatalogue
            )
        }

        if (scheduleError != null) {
            _uiState.update { state ->
                val sections = availableScheduleSections(state.draft.postType)
                state.copy(
                    currentStep = 4,
                    reviewEditStep = 4,
                    activeScheduleSection = state.activeScheduleSection
                        ?.takeIf { it in sections }
                        ?: sections.firstOrNull(),
                    selectedPhysicalScheduleDateMillis = validSelectedPhysicalDate(
                        draft = state.draft,
                        currentDate = state.selectedPhysicalScheduleDateMillis
                    ),
                    scheduleError = scheduleError,
                    showScheduleErrors = true,
                    isStepFourReady = false
                )
            }
            return
        }

        returnToReviewFromEdit()
    }

    fun saveDraft(context: Context) {
        saveDraftInternal(
            context = context,
            allowMinimumLeadTimeIssue = false
        )
    }

    /**
     * The organiser explicitly chose to keep an outdated date in a Draft.
     * Revalidate everything else, then save without bypassing any other rule.
     */
    fun confirmSaveDraftWithDateWarning(context: Context) {
        _uiState.update { state ->
            state.copy(saveDraftDateWarning = null)
        }
        saveDraftInternal(
            context = context,
            allowMinimumLeadTimeIssue = true
        )
    }

    fun dismissSaveDraftDateWarning() {
        _uiState.update { state ->
            state.copy(saveDraftDateWarning = null)
        }
    }

    private fun saveDraftInternal(
        context: Context,
        allowMinimumLeadTimeIssue: Boolean
    ) {
        val current = _uiState.value
        if (current.isSavingDraft || current.isPublishing) return
        if (!validateScheduleForContinue()) return

        val dateWarning = CreatePostValidator
            .minimumLeadTimeIssueMessage(_uiState.value.draft)

        val validationError = postSaveValidationError(
            state = _uiState.value,
            ignoreMinimumLeadTime = dateWarning != null
        )
        if (validationError != null) {
            _uiState.update { state ->
                state.copy(
                    saveDraftError = validationError,
                    saveDraftDateWarning = null,
                    publishError = null,
                    publishDateBlockMessage = null
                )
            }
            return
        }

        if (dateWarning != null && !allowMinimumLeadTimeIssue) {
            _uiState.update { state ->
                state.copy(
                    saveDraftError = null,
                    saveDraftDateWarning = dateWarning,
                    publishError = null,
                    publishDateBlockMessage = null
                )
            }
            return
        }

        // Set busy state before launching so a fast double tap cannot start
        // a second database save.
        _uiState.update { state ->
            state.copy(
                isSavingDraft = true,
                saveDraftError = null,
                saveDraftDateWarning = null,
                publishError = null,
                publishDateBlockMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val snapshot = _uiState.value
                val thumbnail = prepareThumbnailForSave(
                    context = context.applicationContext,
                    thumbnailUri = snapshot.draft.thumbnailUri
                )

                val result = createPostRepository.saveDraft(
                    draft = snapshot.draft,
                    roleCatalogue = snapshot.roleCatalogue,
                    thumbnail = thumbnail
                )

                // The complete post now exists in Supabase with status DRAFT.
                // Clear only the local wizard state; reopening saved drafts is
                // separate Manage Posts work.
                _uiState.update { state ->
                    state.copy(
                        draft = CreatePostDraft(),
                        currentStep = 5,
                        reviewEditStep = null,
                        editingScheduleItemId = null,
                        scheduleEditorDraft = null,
                        isScheduleEditorOpen = false,
                        scheduleError = null,
                        showScheduleErrors = false,
                        isStepFourReady = false,
                        isSavingDraft = false,
                        saveDraftError = null,
                        saveDraftDateWarning = null,
                        savedDraftPostId = result.postId,
                        isPublishing = false,
                        publishError = null,
                        publishDateBlockMessage = null,
                        publishedPostId = null,
                        pendingPostType = null,
                        isPostTypeCommitted = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                e.printStackTrace()
                _uiState.update { state ->
                    state.copy(
                        isSavingDraft = false,
                        saveDraftError = e.message
                            ?: "Could not save this volunteer post as a draft."
                    )
                }
            }
        }
    }

    fun publishPost(context: Context) {
        val current = _uiState.value
        if (current.isSavingDraft || current.isPublishing) return
        if (!validateScheduleForContinue()) return

        val dateBlockMessage = CreatePostValidator
            .minimumLeadTimeIssueMessage(_uiState.value.draft)

        val validationError = postSaveValidationError(
            state = _uiState.value,
            ignoreMinimumLeadTime = dateBlockMessage != null
        )
        if (validationError != null) {
            _uiState.update { state ->
                state.copy(
                    saveDraftError = null,
                    saveDraftDateWarning = null,
                    publishError = validationError,
                    publishDateBlockMessage = null
                )
            }
            return
        }

        if (dateBlockMessage != null) {
            _uiState.update { state ->
                state.copy(
                    saveDraftError = null,
                    saveDraftDateWarning = null,
                    publishError = null,
                    publishDateBlockMessage = dateBlockMessage
                )
            }
            return
        }

        // Publish and Save Draft share the same draft, so lock both actions
        // before starting the asynchronous database work.
        _uiState.update { state ->
            state.copy(
                isPublishing = true,
                saveDraftError = null,
                saveDraftDateWarning = null,
                publishError = null,
                publishDateBlockMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val snapshot = _uiState.value
                val thumbnail = prepareThumbnailForSave(
                    context = context.applicationContext,
                    thumbnailUri = snapshot.draft.thumbnailUri
                )

                val result = createPostRepository.publishPost(
                    draft = snapshot.draft,
                    roleCatalogue = snapshot.roleCatalogue,
                    thumbnail = thumbnail
                )

                // Publishing is complete, so the editable draft is cleared.
                // publishedPostId switches the route to the success screen.
                _uiState.update { state ->
                    state.copy(
                        draft = CreatePostDraft(),
                        currentStep = 5,
                        reviewEditStep = null,
                        editingScheduleItemId = null,
                        scheduleEditorDraft = null,
                        isScheduleEditorOpen = false,
                        scheduleError = null,
                        showScheduleErrors = false,
                        isStepFourReady = false,
                        isSavingDraft = false,
                        saveDraftError = null,
                        saveDraftDateWarning = null,
                        savedDraftPostId = null,
                        isPublishing = false,
                        publishError = null,
                        publishDateBlockMessage = null,
                        publishedPostId = result.postId,
                        pendingPostType = null,
                        isPostTypeCommitted = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                e.printStackTrace()
                _uiState.update { state ->
                    state.copy(
                        isPublishing = false,
                        publishError = e.message
                            ?: "Could not publish this volunteer post."
                    )
                }
            }
        }
    }

    fun dismissPublishDateBlock() {
        _uiState.update { state ->
            state.copy(publishDateBlockMessage = null)
        }
    }

    /**
     * A failed final Publish should not make the organiser hunt through five
     * steps. Return directly to Post Details and expose the live date error.
     */
    fun fixPublishDateFromReview() {
        val refreshedErrors = CreatePostValidator.validateStepOne(
            _uiState.value.draft
        )

        _uiState.update { state ->
            state.copy(
                currentStep = 1,
                reviewEditStep = 1,
                errors = refreshedErrors,
                showValidationErrors = true,
                isStepOneReady = false,
                saveDraftError = null,
                saveDraftDateWarning = null,
                publishError = null,
                publishDateBlockMessage = null
            )
        }
    }

    private fun postSaveValidationError(
        state: CreatePostUiState,
        ignoreMinimumLeadTime: Boolean = false
    ): String? {
        var stepOneErrors = CreatePostValidator.validateStepOne(state.draft)
        if (ignoreMinimumLeadTime) {
            stepOneErrors = CreatePostValidator.withoutMinimumLeadTimeErrors(
                draft = state.draft,
                errors = stepOneErrors
            )
        }

        if (stepOneErrors.hasErrors()) {
            return "Post Details are no longer valid. Return to Step 1 and review them."
        }

        val stepTwoErrors = CreatePostValidator.validateStepTwo(
            draft = state.draft,
            roleCatalogue = state.roleCatalogue
        )
        if (stepTwoErrors.hasErrors()) {
            return "Role capacities are no longer valid. Return to Step 2 and review them."
        }

        val stepThreeError = CreatePostValidator.validateStepThree(
            draft = state.draft,
            roleCatalogue = state.roleCatalogue
        )
        if (stepThreeError != null) {
            return stepThreeError
        }

        return CreatePostValidator.validateStepFour(
            draft = state.draft,
            roleCatalogue = state.roleCatalogue
        )
    }

    private suspend fun prepareThumbnailForSave(
        context: Context,
        thumbnailUri: String?
    ): PublishThumbnail? {
        if (thumbnailUri.isNullOrBlank()) return null

        return withContext(Dispatchers.IO) {
            val uri = Uri.parse(thumbnailUri)
            val mimeType = context.contentResolver.getType(uri)
                ?: error("Could not determine the selected thumbnail type.")

            if (!mimeType.startsWith("image/")) {
                error("The selected thumbnail is not an image.")
            }

            val bytes = context.contentResolver
                .openInputStream(uri)
                ?.use { input -> input.readBytes() }
                ?: error("Could not read the selected thumbnail.")

            val maxBytes = 5 * 1024 * 1024
            if (bytes.size > maxBytes) {
                error("Thumbnail must be 5 MB or smaller.")
            }

            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?: "jpg"

            PublishThumbnail(
                bytes = bytes,
                mimeType = mimeType,
                fileExtension = extension
            )
        }
    }

    private fun openNewScheduleEditor(
        item: ScheduleItemDraft,
        selectedPhysicalDate: Long? = null
    ) {
        trainingLocationSearchJob?.cancel()
        _uiState.update { state ->
            state.copy(
                activeScheduleSection = item.scheduleType,
                selectedPhysicalScheduleDateMillis = selectedPhysicalDate
                    ?: state.selectedPhysicalScheduleDateMillis,
                editingScheduleItemId = null,
                scheduleEditorDraft = item,
                isScheduleEditorOpen = true,
                trainingLocationSuggestions = emptyList(),
                isTrainingLocationSearching = false,
                trainingLocationSearchError = null,
                scheduleError = null,
                showScheduleErrors = false,
                isStepFourReady = false
            )
        }
    }

    private fun updateScheduleEditor(
        transform: (ScheduleItemDraft) -> ScheduleItemDraft
    ) {
        _uiState.update { state ->
            val item = state.scheduleEditorDraft ?: return@update state
            state.copy(
                scheduleEditorDraft = transform(item),
                scheduleError = null,
                showScheduleErrors = false,
                isStepFourReady = false
            )
        }
    }

    private fun cleanScheduleItem(
        item: ScheduleItemDraft,
        draft: CreatePostDraft
    ): ScheduleItemDraft {
        val cleaned = item.copy(
            title = item.title.trim(),
            location = item.location.trim(),
            targetRoleTemplateIds = item.targetRoleTemplateIds.distinct(),
            closingRoleTemplateIds = item.closingRoleTemplateIds.distinct(),
            notes = item.notes.trim(),
            trainingLocationQuery = item.trainingLocationQuery.trim(),
            onlinePlatform = item.onlinePlatform.trim(),
            meetingLink = item.meetingLink.trim(),
            trainingTimeZoneId = null
        )

        if (
            cleaned.scheduleType != ScheduleType.TRAINING ||
            cleaned.trainingMode != TrainingMode.ONSITE
        ) {
            return cleaned
        }

        return when (cleaned.trainingLocationMode) {
            TrainingLocationMode.EVENT_LOCATION -> cleaned.copy(
                location = eventLocationText(draft),
                trainingLocationQuery = "",
                trainingLocation = null
            )

            TrainingLocationMode.CUSTOM -> cleaned.copy(
                location = cleaned.trainingLocation
                    ?.let { location ->
                        location.address.ifBlank { location.displayName }
                    }
                    .orEmpty()
                    .take(180)
            )

            TrainingLocationMode.TBA -> cleaned.copy(
                location = "",
                trainingLocationQuery = "",
                trainingLocation = null
            )

            null -> cleaned
        }
    }

    private fun draftWithEditorItem(
        draft: CreatePostDraft,
        item: ScheduleItemDraft
    ): CreatePostDraft {
        val editingId = _uiState.value.editingScheduleItemId
        val insertedItems = if (editingId == null) {
            draft.scheduleItems + item
        } else {
            draft.scheduleItems.map { existing ->
                if (existing.draftId == editingId) item else existing
            }
        }

        // A role can have only one Training responsible for its application
        // cutoff. If the organiser confirmed a move to the editor item, clear
        // that role from every other Training in the candidate draft.
        val claimedRoleIds = if (item.scheduleType == ScheduleType.TRAINING) {
            item.closingRoleTemplateIds.toSet()
        } else {
            emptySet()
        }

        val items = if (claimedRoleIds.isEmpty()) {
            insertedItems
        } else {
            insertedItems.map { existing ->
                if (existing.draftId == item.draftId) {
                    existing
                } else {
                    existing.copy(
                        closingRoleTemplateIds =
                            existing.closingRoleTemplateIds
                                .filterNot { roleId -> roleId in claimedRoleIds }
                    )
                }
            }
        }

        return draft.copy(scheduleItems = items)
    }

    private fun eventLocationText(draft: CreatePostDraft): String {
        val location = draft.physicalLocation ?: return ""
        return location.address.ifBlank { location.displayName }.take(180)
    }

    private fun clearTrainingLocationSearchUi() {
        _uiState.update { state ->
            state.copy(
                trainingLocationSuggestions = emptyList(),
                isTrainingLocationSearching = false,
                trainingLocationSearchError = null
            )
        }
    }

    private fun updateStepFourDraft(
        change: (CreatePostDraft) -> CreatePostDraft
    ) {
        _uiState.update { current ->
            current.copy(
                draft = change(current.draft),
                scheduleError = null,
                showScheduleErrors = false,
                isStepFourReady = false
            )
        }
    }

    private fun setScheduleError(message: String) {
        _uiState.update { state ->
            state.copy(
                scheduleError = message,
                showScheduleErrors = true,
                isStepFourReady = false
            )
        }
    }

    private fun availableScheduleSections(
        postType: VolunteerPostType?
    ): List<ScheduleType> {
        return when (postType) {
            VolunteerPostType.PHYSICAL -> listOf(
                ScheduleType.PHYSICAL,
                ScheduleType.TRAINING
            )

            VolunteerPostType.REMOTE -> listOf(
                ScheduleType.REMOTE,
                ScheduleType.TRAINING
            )

            VolunteerPostType.HYBRID -> listOf(
                ScheduleType.PHYSICAL,
                ScheduleType.REMOTE,
                ScheduleType.TRAINING
            )

            null -> emptyList()
        }
    }

    private fun validSelectedPhysicalDate(
        draft: CreatePostDraft,
        currentDate: Long?
    ): Long? {
        val dates = CreatePostValidator.physicalScheduleDates(draft)
        return currentDate
            ?.let(CreatePostValidator::startOfDayMillis)
            ?.takeIf { it in dates }
            ?: dates.firstOrNull()
    }

    private fun previousDayMillis(dateMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = CreatePostValidator.startOfDayMillis(dateMillis)
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
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

    private fun trainingCutoffMoveError(
        state: CreatePostUiState,
        item: ScheduleItemDraft
    ): String? {
        if (item.scheduleType != ScheduleType.TRAINING) return null
        if (item.closingRoleTemplateIds.isEmpty()) return null

        val newStart = trainingStartOrderValue(item) ?: return null
        val rolesById = state.roleCatalogue.associateBy { role ->
            role.roleTemplateId
        }

        item.closingRoleTemplateIds.distinct().forEach { roleId ->
            val otherCutoff = state.draft.scheduleItems
                .asSequence()
                .filter { saved ->
                    saved.draftId != item.draftId &&
                        saved.scheduleType == ScheduleType.TRAINING &&
                        roleId in saved.closingRoleTemplateIds
                }
                .minByOrNull { saved ->
                    trainingStartOrderValue(saved) ?: Long.MAX_VALUE
                }
                ?: return@forEach

            val otherStart = trainingStartOrderValue(otherCutoff)
                ?: return@forEach

            if (newStart >= otherStart) {
                val roleName = rolesById[roleId]?.roleName ?: roleId
                return "$roleName already has an earlier or same-time application-closing training. Uncheck this role or move this training earlier."
            }
        }

        return null
    }

    private fun effectiveEditorTargetRoleIds(
        state: CreatePostUiState,
        item: ScheduleItemDraft
    ): Set<String> {
        val applicableRoleIds = CreatePostValidator.applicableScheduleRoleIds(
            draft = state.draft,
            scheduleType = item.scheduleType,
            roleCatalogue = state.roleCatalogue
        )

        return if (item.appliesToAllRoles) {
            applicableRoleIds.toSet()
        } else {
            item.targetRoleTemplateIds
                .filter { roleId -> roleId in applicableRoleIds }
                .toSet()
        }
    }

    private fun trainingStartOrderValue(item: ScheduleItemDraft): Long? {
        val date = item.scheduleDateMillis ?: return null
        val startMinutes = item.startTimeMinutes ?: return null
        return CreatePostValidator.startOfDayMillis(date) +
            startMinutes * 60L * 1000L
    }

    /**
     * Step 2 can remove a role after Step 4 has already been configured. Keep
     * saved schedule targets and cutoff-role selections inside the current
     * selected role set so stale ROLE IDs cannot reach the database.
     */
    private fun cleanScheduleRoleReferences(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): CreatePostDraft {
        return draft.copy(
            scheduleItems = draft.scheduleItems.map { item ->
                cleanScheduleItemRoleReferences(
                    item = item,
                    draft = draft,
                    roleCatalogue = roleCatalogue
                )
            }
        )
    }

    private fun cleanScheduleItemRoleReferences(
        item: ScheduleItemDraft,
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): ScheduleItemDraft {
        val applicableRoleIds = CreatePostValidator.applicableScheduleRoleIds(
            draft = draft,
            scheduleType = item.scheduleType,
            roleCatalogue = roleCatalogue
        )
        val applicableSet = applicableRoleIds.toSet()
        val cleanedTargets = item.targetRoleTemplateIds
            .filter { roleId -> roleId in applicableSet }
            .distinct()
        val effectiveTargets = if (item.appliesToAllRoles) {
            applicableSet
        } else {
            cleanedTargets.toSet()
        }

        return item.copy(
            targetRoleTemplateIds = cleanedTargets,
            closingRoleTemplateIds = if (
                item.scheduleType == ScheduleType.TRAINING
            ) {
                item.closingRoleTemplateIds
                    .filter { roleId -> roleId in effectiveTargets }
                    .distinct()
            } else {
                emptyList()
            }
        )
    }

    private fun updateStepTwoDraft(
        change: (CreatePostDraft) -> CreatePostDraft
    ) {
        _uiState.update { current ->
            val changedDraft = change(current.draft)
            val newDraft = cleanScheduleRoleReferences(
                draft = changedDraft,
                roleCatalogue = current.roleCatalogue
            )
            val errors = CreatePostValidator.validateStepTwo(
                draft = newDraft,
                roleCatalogue = current.roleCatalogue
            )

            current.copy(
                draft = newDraft,
                scheduleEditorDraft = current.scheduleEditorDraft?.let { item ->
                    cleanScheduleItemRoleReferences(
                        item = item,
                        draft = newDraft,
                        roleCatalogue = current.roleCatalogue
                    )
                },
                roleSelectionErrors = errors,
                isStepTwoReady = false,
                isStepThreeReady = false,
                isStepFourReady = false,
                scheduleError = null,
                showScheduleErrors = false
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
        trainingLocationSearchJob?.cancel()
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
                isStepThreeReady = false,
                isStepFourReady = false,
                showRoleSelectionErrors = false,
                scheduleError = null,
                showScheduleErrors = false,
                saveDraftError = null,
                publishError = null
            )
        }
    }

    private fun parsePositiveNumber(text: String): Int? {
        val digitsOnly = text.filter { it.isDigit() }.take(4)
        if (digitsOnly.isBlank()) return null

        return digitsOnly.toIntOrNull()?.takeIf { it > 0 }
    }
}
