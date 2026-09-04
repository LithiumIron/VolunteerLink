package com.example.volunteerlink.organisation.viewmodel

// FILE OVERVIEW:
/*
 * CreatePostViewModel coordinates state and user actions for the organisation Create/Edit Post flow.
 * It translates UI events into validation/repository operations and exposes observable state
 * back to Compose so the screen can stay declarative.
 */


import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.BuildConfig
import com.example.volunteerlink.data.location.GeoapifyLocationService
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.create.PostEditPolicyEvaluator
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.CreatePostEditorMode
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.RoleApplicationMethod
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMode
import com.example.volunteerlink.organisation.repository.CreatePostRepository
import com.example.volunteerlink.organisation.repository.ExistingPostEditData
import com.example.volunteerlink.organisation.repository.PublishThumbnail
import com.example.volunteerlink.organisation.repository.SupabaseCreatePostRepository
import com.example.volunteerlink.organisation.repository.SupabaseImpactWeaveRepository
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
    private val impactWeaveRepository = SupabaseImpactWeaveRepository()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState = _uiState.asStateFlow()

    private var locationSearchJob: Job? = null
    private var locationBiasLatitude: Double? = null
    private var locationBiasLongitude: Double? = null

    // Existing-post editing reuses this ViewModel/draft. Keep the loaded DB
    // snapshot privately so unsaved-change checks and final conflict checks do
    // not become UI responsibilities.
    private var originalExistingPost: ExistingPostEditData? = null

    /**
     * Loads the impact weave for create needed by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun loadImpactWeaveForCreate(draftId: String) {
        if (_uiState.value.impactWeaveDraftId == draftId ||
            _uiState.value.isLoadingImpactWeave
        ) return

        _uiState.value = CreatePostUiState(
            impactWeaveDraftId = draftId,
            isLoadingImpactWeave = true
        )
        viewModelScope.launch {
            try {
                val prefill = impactWeaveRepository.loadPostPrefill(draftId)
                _uiState.value = CreatePostUiState(
                    draft = CreatePostDraft(
                        postType = when (prefill.mode) {
                            ImpactWeaveMode.PHYSICAL ->
                                VolunteerPostType.PHYSICAL
                            ImpactWeaveMode.HYBRID ->
                                VolunteerPostType.HYBRID
                        },
                        category = prefill.category,
                        title = prefill.title,
                        description = prefill.description,
                        isMultiDayPhysicalEvent = prefill.startDateMillis != prefill.endDateMillis,
                        physicalStartDateMillis = prefill.startDateMillis,
                        physicalEndDateMillis = prefill.endDateMillis,
                        physicalStartTimeMinutes = prefill.startTimeMinutes,
                        physicalEndTimeMinutes = prefill.endTimeMinutes,
                        physicalLocationQuery = prefill.location.displayName,
                        physicalLocation = prefill.location,
                        remoteStartDateMillis = prefill.startDateMillis,
                        remoteDueDateMillis = prefill.endDateMillis
                    ),
                    impactWeaveDraftId = draftId,
                    impactWeavePartners = prefill.partners,
                    isLoadingImpactWeave = false,
                    isPostTypeCommitted = true
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.value = CreatePostUiState(
                    impactWeaveDraftId = draftId,
                    isLoadingImpactWeave = false,
                    impactWeaveLoadError = safeImpactWeaveLoadError(exception.message.orEmpty())
                )
            }
        }
    }

    /**
     * Loads the existing post for edit needed by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun loadExistingPostForEdit(postId: String) {
        val currentMode = _uiState.value.editorMode
        if (currentMode is CreatePostEditorMode.ExistingPostEdit &&
            currentMode.postId == postId && originalExistingPost != null
        ) return

        _uiState.value = CreatePostUiState(
            editorMode = CreatePostEditorMode.ExistingPostEdit(postId),
            isLoadingExistingPost = true
        )

        viewModelScope.launch {
            try {
                val catalogue = createPostRepository.loadRoleCatalogue()
                val existing = createPostRepository.loadExistingPostForEdit(postId)
                val policy = PostEditPolicyEvaluator.evaluate(existing.policyInput)
                originalExistingPost = existing

                _uiState.value = CreatePostUiState(
                    draft = existing.draft,
                    editorMode = CreatePostEditorMode.ExistingPostEdit(postId),
                    editPolicy = policy,
                    roleCatalogue = catalogue,
                    currentStep = 1,
                    isPostTypeCommitted = existing.databaseStatus.uppercase() != "DRAFT",
                    isLoadingExistingPost = false,
                    isStepOneReady = true,
                    isStepTwoReady = true,
                    isStepThreeReady = true,
                    isStepFourReady = true
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
                _uiState.value = CreatePostUiState(
                    editorMode = CreatePostEditorMode.ExistingPostEdit(postId),
                    isLoadingExistingPost = false,
                    existingPostLoadError = e.message ?: "Could not load this post for editing."
                )
            }
        }
    }

    /**
     * Retries the current operation in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun retryExistingPostLoad() {
        val postId = _uiState.value.existingPostId ?: return
        originalExistingPost = null
        loadExistingPostForEdit(postId)
    }

    /**
     * Closes or clears the edit restriction message in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun dismissEditRestrictionMessage() {
        _uiState.update { it.copy(editRestrictionMessage = null) }
    }

    /**
     * Derives the allow edit value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun allowEdit(allowed: Boolean, message: String): Boolean {
        if (!_uiState.value.isExistingPostEdit || allowed) return true
        _uiState.update { it.copy(editRestrictionMessage = message) }
        return false
    }

    /**
     * Derives the allow impact weave field edit value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun allowImpactWeaveFieldEdit(message: String): Boolean {
        if (_uiState.value.impactWeaveDraftId == null) return true
        _uiState.update { it.copy(editRestrictionMessage = message) }
        return false
    }

    /**
     * Returns the safe impact weave load error used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun safeImpactWeaveLoadError(raw: String): String = when {
        raw.contains("POST_REQUIRES_7_DAYS", true) ->
            "This activity starts in less than 7 days. Reschedule it in Impact Weave before creating a post."
        raw.contains("CONFIRMED_VENUE_REQUIRED", true) ->
            "A partner venue must be confirmed before creating this post."
        raw.contains("PARTNER_RECONFIRMATION_REQUIRED", true) ->
            "A partner must reconfirm the changed schedule before creating this post."
        raw.contains("IMPACT_WEAVE_WAITING", true) ->
            "This Impact Weave plan is still waiting for a partnership response or schedule reconfirmation."
        else -> "Could not prepare this Impact Weave plan for Create Post. Please return and try again."
    }

    /**
     * Derives the role policy value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun rolePolicy(roleTemplateId: String) =
        _uiState.value.editPolicy?.rolePolicies?.get(roleTemplateId)

    /**
     * Derives the current schedule policy value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun currentSchedulePolicy() = _uiState.value.editingScheduleItemId
        ?.let { _uiState.value.editPolicy?.schedulePolicies?.get(it) }


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

        if (currentState.isExistingPostEdit && currentState.editPolicy?.postStatus != "DRAFT") {
            allowEdit(false, "Post Type cannot be changed after a post has been published.")
            return
        }

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

    /**
     * Applies the post type change used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun applyPostTypeChange(type: VolunteerPostType) {
        _uiState.update { current ->
            val newDraft = current.draft.copy(postType = type)

            current.copy(
                draft = newDraft,
                pendingPostType = null,
                errors = if (current.showValidationErrors) {
                    validateStepOneForCurrentEditor(newDraft)
                } else {
                    current.errors
                },
                isStepOneReady = false
            )
        }
    }

    /**
     * Updates the category used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateCategory(category: VolunteerPostCategory) {
        if (!allowImpactWeaveFieldEdit("Impact Weave activity details are final here. Edit them from the Impact Weave plan.")) return
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditSharedPostInfo != false,
                "Category is locked because this opportunity has already started."
            )
        ) return
        updateDraft { it.copy(category = category) }
    }

    /**
     * Updates the title used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateTitle(title: String) {
        if (!allowImpactWeaveFieldEdit("Impact Weave activity details are final here. Edit them from the Impact Weave plan.")) return
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditSharedPostInfo != false,
                "Title is locked because this opportunity has already started."
            )
        ) return
        updateDraft { it.copy(title = title.take(120)) }
    }

    /**
     * Updates the description used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateDescription(description: String) {
        if (!allowImpactWeaveFieldEdit("Impact Weave activity details are final here. Edit them from the Impact Weave plan.")) return
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditSharedPostInfo != false,
                "Description is locked because this opportunity has already started."
            )
        ) return
        updateDraft { it.copy(description = description.take(2000)) }
    }

    /**
     * Updates the thumbnail uri used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateThumbnailUri(uri: String?) {
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditSharedPostInfo != false,
                "Thumbnail is locked because this opportunity has already started."
            )
        ) return
        updateDraft { it.copy(thumbnailUri = uri) }
    }

    // ---------------------------------------------------------------------
    // Physical event
    // ---------------------------------------------------------------------

    fun updateIsMultiDay(isMultiDay: Boolean) {
        if (!allowImpactWeaveFieldEdit("The agreed Impact Weave schedule is final and cannot be changed in Create Post.")) return
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalDates != false,
                "Physical event dates are locked after publication."
            )
        ) return
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

    /**
     * Updates the physical start date used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updatePhysicalStartDate(dateMillis: Long) {
        if (!allowImpactWeaveFieldEdit("The agreed Impact Weave schedule is final and cannot be changed in Create Post.")) return
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalDates != false,
                "Physical event dates are locked after publication."
            )
        ) return
        updateDraft { draft ->
            val normalizedDate = CreatePostValidator.startOfDayMillis(dateMillis)
            val previousPhysicalDates = CreatePostValidator.physicalScheduleDates(draft)

            val endDate = if (!draft.isMultiDayPhysicalEvent) {
                normalizedDate
            } else {
                val currentEnd = draft.physicalEndDateMillis
                    ?.let(CreatePostValidator::startOfDayMillis)

                when {
                    currentEnd != null && currentEnd > normalizedDate -> currentEnd
                    previousPhysicalDates.size > 1 -> addCalendarDays(
                        normalizedDate,
                        previousPhysicalDates.size - 1
                    )
                    else -> CreatePostValidator.nextDayMillis(normalizedDate)
                }
            }

            draft.copy(
                physicalStartDateMillis = normalizedDate,
                physicalEndDateMillis = endDate
            )
        }
    }

    /**
     * Updates the physical end date used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updatePhysicalEndDate(dateMillis: Long) {
        if (!allowImpactWeaveFieldEdit("The agreed Impact Weave schedule is final and cannot be changed in Create Post.")) return
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalDates != false,
                "Physical event dates are locked after publication."
            )
        ) return
        updateDraft { draft ->
            draft.copy(
                physicalEndDateMillis =
                    CreatePostValidator.startOfDayMillis(dateMillis)
            )
        }
    }

    /**
     * Updates the physical start time used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updatePhysicalStartTime(hour: Int, minute: Int) {
        if (!allowImpactWeaveFieldEdit("The agreed Impact Weave schedule is final and cannot be changed in Create Post.")) return
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalCore != false,
                "Physical event time is locked for this post."
            )
        ) return
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
        if (!allowImpactWeaveFieldEdit("The agreed Impact Weave schedule is final and cannot be changed in Create Post.")) {
            return "The agreed Impact Weave schedule is final."
        }
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalCore != false,
                "Physical event time is locked for this post."
            )
        ) return "Physical event time is locked for this post."
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

    /**
     * Clears the physical time error for the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun clearPhysicalTimeError() {
        _uiState.update { it.copy(physicalTimeError = null) }
    }

    /**
     * Updates the meeting point used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateMeetingPoint(text: String) {
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalMeetingPoint != false,
                "Meeting point can no longer be changed because the Physical phase has started."
            )
        ) return
        updateDraft { it.copy(meetingPoint = text.take(250)) }
    }

    /**
     * Updates the physical volunteer capacity used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updatePhysicalVolunteerCapacity(text: String) {
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalCapacity != false,
                "Physical capacity can no longer be changed."
            )
        ) return
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

    /**
     * Handles the location query changed event for the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun onLocationQueryChanged(query: String) {
        if (!allowImpactWeaveFieldEdit("The confirmed partnership venue is locked for this post.")) return
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalCore != false,
                "Physical location is locked for this post."
            )
        ) return
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
                // Create Post accepts any real Geoapify location result: an area,
                // locality, venue, building, street or exact address. Device
                // coordinates only bias the ranking and never restrict the search.
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

    /**
     * Handles the location selected event for the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun onLocationSelected(location: LocationSuggestion) {
        if (!allowImpactWeaveFieldEdit("The confirmed partnership venue is locked for this post.")) return
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalCore != false,
                "Physical location is locked for this post."
            )
        ) return
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

    /**
     * Clears the location for the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun clearLocation() {
        if (!allowImpactWeaveFieldEdit("The confirmed partnership venue is locked for this post.")) return
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalCore != false,
                "Physical location is locked for this post."
            )
        ) return
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
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditRemoteStart != false,
                "Remote start date is locked because Remote volunteers already depend on it or the phase has started."
            )
        ) return
        updateDraft { draft ->
            val normalizedDate = CreatePostValidator.startOfDayMillis(dateMillis)
            val currentStart = draft.remoteStartDateMillis
                ?.let(CreatePostValidator::startOfDayMillis)
            val currentDue = draft.remoteDueDateMillis
                ?.let(CreatePostValidator::startOfDayMillis)

            val dueDate = when {
                currentDue == null -> null
                currentDue > normalizedDate -> currentDue
                currentStart != null && currentDue > currentStart -> addCalendarDays(
                    normalizedDate,
                    calendarDayOffset(currentStart, currentDue).coerceAtLeast(1)
                )
                else -> null
            }

            draft.copy(
                remoteStartDateMillis = normalizedDate,
                remoteDueDateMillis = dueDate
            )
        }
    }

    /**
     * Updates the remote due date used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateRemoteDueDate(dateMillis: Long) {
        val policy = _uiState.value.editPolicy
        if (!allowEdit(
                policy?.canEditRemoteDueDate != false,
                policy?.remoteDueDateLockedReason
                    ?: "Remote due date can no longer be changed."
            )
        ) return

        val normalizedDate = CreatePostValidator.startOfDayMillis(dateMillis)
        policy?.minimumRemoteDueDateMillis?.let { minimum ->
            if (_uiState.value.isExistingPostEdit && normalizedDate < minimum) {
                allowEdit(
                    false,
                    "The Remote due date can be extended, but it cannot be shortened after volunteers have joined or Individual work has been submitted."
                )
                return
            }
        }
        updateDraft { draft ->
            draft.copy(remoteDueDateMillis = normalizedDate)
        }
    }

    /**
     * Updates the remote volunteer capacity used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateRemoteVolunteerCapacity(text: String) {
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditRemoteCapacity != false,
                "Remote capacity can no longer be changed."
            )
        ) return
        updateDraft { draft ->
            draft.copy(
                remoteVolunteerCapacity = parsePositiveNumber(text)
            )
        }
    }

    /**
     * Updates the remote submission mode used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateRemoteSubmissionMode(mode: RemoteSubmissionMode) {
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditRemoteSubmissionSetup != false,
                "Remote submission setup is locked because active Remote applicants, joined volunteers, or submitted work depend on it."
            )
        ) return
        updateDraft { draft ->
            draft.copy(remoteSubmissionMode = mode)
        }
    }

    /**
     * Updates the shared deliverable used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateSharedDeliverable(text: String) {
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditRemoteSubmissionSetup != false,
                "Shared deliverable is locked because active Remote applicants, joined volunteers, or submitted work depend on it."
            )
        ) return
        updateDraft { draft ->
            draft.copy(sharedDeliverable = text.take(500))
        }
    }

    // ---------------------------------------------------------------------
    // Hybrid capacities
    // ---------------------------------------------------------------------

    fun updateHybridPhysicalVolunteerCapacity(text: String) {
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditPhysicalCapacity != false,
                "Physical capacity can no longer be changed."
            )
        ) return
        updateDraft { draft ->
            draft.copy(
                hybridPhysicalVolunteerCapacity = parsePositiveNumber(text)
            )
        }
    }

    /**
     * Updates the hybrid remote volunteer capacity used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateHybridRemoteVolunteerCapacity(text: String) {
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditRemoteCapacity != false,
                "Remote capacity can no longer be changed."
            )
        ) return
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

    /**
     * Moves the organisation Create/Edit Post flow back to the previous relevant step or state.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun backToStepOne() {
        _uiState.update { current ->
            current.copy(
                currentStep = 1,
                showRoleSelectionErrors = false,
                isStepTwoReady = false
            )
        }
    }

    /**
     * Retries the current operation in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun retryRoleCatalogue() {
        loadRoleCatalogue(forceReload = true)
    }

    /**
     * Loads the role catalogue needed by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Updates the role search query used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateRoleSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                roleSearchQuery = query.take(80),
                isStepTwoReady = false
            )
        }
    }

    /**
     * Updates the role mode filter used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Adds the role to the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

        val canAddRole = when (template.roleMode) {
            VolunteerRoleMode.PHYSICAL -> current.editPolicy?.canAddPhysicalRole != false
            VolunteerRoleMode.REMOTE -> current.editPolicy?.canAddRemoteRole != false
        }
        if (!allowEdit(
                canAddRole,
                "New ${template.roleMode.displayName} roles can no longer be added to this post."
            )
        ) return

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

    /**
     * Removes the role from the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun removeRole(roleTemplateId: String) {
        val policy = rolePolicy(roleTemplateId)
        if (!allowEdit(
                policy?.canRemove != false,
                policy?.selectionLockedReason
                    ?: "This role cannot be removed because past or current application/volunteer records depend on it."
            )
        ) return
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

    /**
     * Increases the role capacity in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun increaseRoleCapacity(roleTemplateId: String) {
        val policy = rolePolicy(roleTemplateId)
        if (!allowEdit(
                policy?.canChangeCapacity != false,
                "Capacity for this role is locked because applications are closed."
            )
        ) return
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

    /**
     * Decreases the role capacity in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun decreaseRoleCapacity(roleTemplateId: String) {
        val policy = rolePolicy(roleTemplateId)
        if (!allowEdit(
                policy?.canChangeCapacity != false,
                "Capacity for this role is locked because applications are closed."
            )
        ) return
        val minimum = policy?.minimumCapacity ?: 1
        updateStepTwoDraft { draft ->
            draft.copy(
                selectedRoles = draft.selectedRoles.map { selected ->
                    if (
                        selected.roleTemplateId == roleTemplateId &&
                        selected.capacity > minimum
                    ) {
                        selected.copy(capacity = selected.capacity - 1)
                    } else {
                        selected
                    }
                }
            )
        }
    }

    /**
     * Updates the role capacity used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateRoleCapacity(
        roleTemplateId: String,
        text: String
    ) {
        val policy = rolePolicy(roleTemplateId)
        if (!allowEdit(
                policy?.canChangeCapacity != false,
                "Capacity for this role is locked because applications are closed."
            )
        ) return
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
            minimumValue = policy?.minimumCapacity ?: 1,
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

    /**
     * Derives the continue from step two value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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
                validationFocusRequest = if (ready) {
                    state.validationFocusRequest
                } else {
                    state.validationFocusRequest + 1L
                },
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

    /**
     * Toggles the practised skill used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun togglePractisedSkill(
        roleTemplateId: String,
        skillId: String
    ) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeSkills != false,
                "Skills are locked because active applicants or joined volunteers depend on them."
            )
        ) return
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

    /**
     * Toggles the required skill used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun toggleRequiredSkill(
        roleTemplateId: String,
        skillId: String
    ) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeSkills != false,
                "Required skills are locked because active applicants or joined volunteers depend on them."
            )
        ) return
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

    /**
     * Increases the required skill experience in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Decreases the required skill experience in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Derives the change required skill experience value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun changeRequiredSkillExperience(
        roleTemplateId: String,
        skillId: String,
        change: Int
    ) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeSkills != false,
                "Required experience is locked because active applicants or joined volunteers depend on it."
            )
        ) return
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

    /**
     * Adds the responsibility to the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun addResponsibility(roleTemplateId: String) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeResponsibilities != false,
                "Responsibilities are locked because an active applicant, joined volunteer, or activity history depends on them."
            )
        ) return
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

    /**
     * Updates the responsibility used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateResponsibility(
        roleTemplateId: String,
        index: Int,
        text: String
    ) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeResponsibilities != false,
                "Responsibilities are locked because an active applicant, joined volunteer, or activity history depends on them."
            )
        ) return
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

    /**
     * Removes the responsibility from the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun removeResponsibility(
        roleTemplateId: String,
        index: Int
    ) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeResponsibilities != false,
                "Responsibilities are locked because an active applicant, joined volunteer, or activity history depends on them."
            )
        ) return
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
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeApplicationMethod != false,
                "Application method is locked because active applicants, joined volunteers, or historical screening answers depend on it."
            )
        ) return
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

    /**
     * Adds the screening question to the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun addScreeningQuestion(roleTemplateId: String) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeScreeningQuestions != false,
                "Screening questions are locked because active applicants or historical answers depend on them."
            )
        ) return
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

    /**
     * Updates the screening question used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateScreeningQuestion(
        roleTemplateId: String,
        index: Int,
        text: String
    ) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeScreeningQuestions != false,
                "Screening questions are locked because active applicants or historical answers depend on them."
            )
        ) return
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

    /**
     * Removes the screening question from the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun removeScreeningQuestion(
        roleTemplateId: String,
        index: Int
    ) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeScreeningQuestions != false,
                "Screening questions are locked because active applicants or historical answers depend on them."
            )
        ) return
        updateRoleConfiguration(roleTemplateId) { role ->
            role.copy(
                screeningQuestions = role.screeningQuestions
                    .filterIndexed { itemIndex, _ -> itemIndex != index }
            )
        }
    }

    /**
     * Updates the role notes used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateRoleNotes(
        roleTemplateId: String,
        text: String
    ) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeRoleNotes != false,
                "Role notes can no longer be changed because this volunteering phase has started."
            )
        ) return
        updateRoleConfiguration(roleTemplateId) { role ->
            role.copy(roleNotes = text.take(400))
        }
    }

    /**
     * Updates the individual submission requirement used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateIndividualSubmissionRequirement(
        roleTemplateId: String,
        text: String
    ) {
        if (!allowEdit(
                rolePolicy(roleTemplateId)?.canChangeIndividualDeliverable != false,
                "The Remote deliverable is locked because an active applicant, joined volunteer, or submitted work depends on it."
            )
        ) return
        updateRoleConfiguration(roleTemplateId) { role ->
            role.copy(
                individualSubmissionRequirement = text.take(500)
            )
        }
    }

    /**
     * Updates the shared submission responsible role used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateSharedSubmissionResponsibleRole(
        roleTemplateId: String
    ) {
        if (!allowEdit(
                _uiState.value.editPolicy?.canEditRemoteSubmissionSetup != false,
                "The responsible Remote role is locked because active Remote applicants, joined volunteers, or submitted work depend on it."
            )
        ) return
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
                    validationFocusRequest = state.validationFocusRequest + 1L,
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
        val templatesById = current.roleCatalogue.associateBy { it.roleTemplateId }

        // If one role is incomplete, open THAT role immediately and show the
        // first concrete problem. The organiser should not have to search the
        // Step 3 overview to discover which role blocked Continue.
        current.draft.selectedRoles.forEach { selectedRole ->
            val template = templatesById[selectedRole.roleTemplateId]
            if (template == null) {
                _uiState.update { state ->
                    state.copy(
                        roleSettingsError =
                            "This role is no longer available in the role catalogue.",
                        validationFocusRequest = state.validationFocusRequest + 1L,
                        isStepThreeReady = false
                    )
                }
                return false
            }

            val problems = CreatePostValidator.validateRoleConfiguration(
                draft = current.draft,
                selectedRole = selectedRole,
                template = template
            )

            if (!selectedRole.isConfigured || problems.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(
                        editingRoleTemplateId = selectedRole.roleTemplateId,
                        roleSettingsError = problems.firstOrNull()
                            ?: "Review and save this role before continuing.",
                        validationFocusRequest = state.validationFocusRequest + 1L,
                        isStepThreeReady = false
                    )
                }
                return false
            }
        }

        val error = CreatePostValidator.validateStepThree(
            draft = current.draft,
            roleCatalogue = current.roleCatalogue
        )
        val ready = error == null

        if (!ready) {
            _uiState.update { state ->
                state.copy(
                    roleSettingsError = error,
                    validationFocusRequest = state.validationFocusRequest + 1L,
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
                scheduleError = null,
                showScheduleErrors = false
            )
        }

        return true
    }

    /**
     * Moves the organisation Create/Edit Post flow back to the previous relevant step or state.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Selects the physical schedule date used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Opens the schedule item editor in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun openScheduleItemEditor(itemId: String) {
        val current = _uiState.value
        val editSchedulePolicy = current.editPolicy?.schedulePolicies?.get(itemId)
        if (!allowEdit(
                editSchedulePolicy?.canEdit != false,
                editSchedulePolicy?.reason
                    ?: "This schedule item is part of the post's history and cannot be changed."
            )
        ) return

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
        _uiState.update { state ->
            state.copy(
                activeScheduleSection = item.scheduleType,
                editingScheduleItemId = itemId,
                scheduleEditorDraft = item,
                isScheduleEditorOpen = true,
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
        _uiState.update { state ->
            state.copy(
                isScheduleEditorOpen = false,
                scheduleError = null,
                showScheduleErrors = false
            )
        }
    }

    /**
     * Derives the resume schedule editor draft value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Derives the discard schedule editor draft value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun discardScheduleEditorDraft() {
        _uiState.update { state ->
            state.copy(
                editingScheduleItemId = null,
                scheduleEditorDraft = null,
                isScheduleEditorOpen = false,
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
        if (!allowEdit(
                _uiState.value.editPolicy?.canAddPhysicalSchedule != false,
                "New Physical schedule items can no longer be added to this post."
            )
        ) return null
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

    /** Starts a new date-based Remote milestone editor. */
    fun addRemoteScheduleItem(): String? {
        if (!allowEdit(
                _uiState.value.editPolicy?.canAddRemoteSchedule != false,
                "New Remote schedule items can no longer be added to this post."
            )
        ) return null
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

    /**
     * Renders the remove schedule item item used in the organisation Create/Edit Post flow.
     * It receives state and callbacks from its caller so presentation code stays separate from database operations.
     */
    fun removeScheduleItem(itemId: String) {
        val schedulePolicy = _uiState.value.editPolicy?.schedulePolicies?.get(itemId)
        if (!allowEdit(
                schedulePolicy?.canRemove != false,
                schedulePolicy?.reason
                    ?: "This schedule item is part of the post's history and cannot be removed."
            )
        ) return
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

    /**
     * Updates the schedule editor title used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateScheduleEditorTitle(text: String) {
        updateScheduleEditor { item ->
            item.copy(title = text.take(120))
        }
    }

    /**
     * Updates the schedule editor notes used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateScheduleEditorNotes(text: String) {
        updateScheduleEditor { item ->
            item.copy(notes = text.take(500))
        }
    }

    /**
     * Updates the schedule editor location used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun updateScheduleEditorLocation(text: String) {
        updateScheduleEditor { item ->
            if (item.scheduleType == ScheduleType.PHYSICAL) {
                item.copy(location = text.take(180))
            } else {
                item
            }
        }
    }

    /**
     * Updates the schedule editor date used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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
        }

        if (accepted) {
            updateScheduleEditor { existing ->
                existing.copy(scheduleDateMillis = date)
            }
        }
    }

    /**
     * Updates the schedule editor start time used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Updates the schedule editor end time used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Updates the schedule editor applies to all used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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
                    existing.copy(
                        appliesToAllRoles = false,
                        targetRoleTemplateIds = availableRoleIds
                    )
                }

                appliesToAll -> existing.copy(
                    appliesToAllRoles = true,
                    targetRoleTemplateIds = emptyList()
                )

                else -> {
                    val targets = existing.targetRoleTemplateIds
                        .filter { roleId -> roleId in availableRoleIds }
                        .ifEmpty { listOf(availableRoleIds.first()) }

                    existing.copy(
                        appliesToAllRoles = false,
                        targetRoleTemplateIds = targets
                    )
                }
            }
        }
    }

    /**
     * Toggles the schedule editor role used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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
                targetRoleTemplateIds = changedTargets
            )
        }
    }

    /** Validates the temporary editor buffer without saving it to CreatePostDraft. */
    fun validateScheduleEditor(): Boolean {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return false
        val cleanedItem = cleanScheduleItem(item)
        val candidateDraft = draftWithEditorItem(current.draft, cleanedItem)

        val error = CreatePostValidator.validateScheduleItem(
            draft = candidateDraft,
            item = cleanedItem,
            roleCatalogue = current.roleCatalogue
        )

        _uiState.update { state ->
            state.copy(
                scheduleEditorDraft = cleanedItem,
                scheduleError = error,
                showScheduleErrors = error != null,
                validationFocusRequest = if (error == null) {
                    state.validationFocusRequest
                } else {
                    state.validationFocusRequest + 1L
                },
                isStepFourReady = false
            )
        }

        return error == null
    }

    /** Commits a valid editor buffer to the shared CreatePostDraft. */
    fun saveScheduleEditor(): Boolean {
        val current = _uiState.value
        val item = current.scheduleEditorDraft ?: return false
        val cleanedItem = cleanScheduleItem(item)
        val candidateDraft = draftWithEditorItem(current.draft, cleanedItem)

        val error = CreatePostValidator.validateScheduleItem(
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
                scheduleError = null,
                showScheduleErrors = false,
                isStepFourReady = false
            )
        }
        return true
    }

    /** Used by the overview to mark only saved items invalidated by earlier steps. */
    fun getScheduleItemValidationMessage(itemId: String): String? {
        val current = _uiState.value
        val item = current.draft.scheduleItems.firstOrNull { existing ->
            existing.draftId == itemId
        } ?: return null

        return scheduleItemValidationMessageForCurrentEditor(
            draft = current.draft,
            item = item,
            roleCatalogue = current.roleCatalogue
        )
    }

    /**
     * Copies one complete Physical day to another event date.
     * The copied activities receive new local draft IDs. Remote items on the
     * target date are never touched.
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

    /**
     * Derives the physical schedule day has items value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun physicalScheduleDayHasItems(dateMillis: Long): Boolean {
        val date = CreatePostValidator.startOfDayMillis(dateMillis)
        return _uiState.value.draft.scheduleItems.any { item ->
            item.scheduleType == ScheduleType.PHYSICAL &&
                    item.scheduleDateMillis?.let(
                        CreatePostValidator::startOfDayMillis
                    ) == date
        }
    }

    /**
     * Checks whether the organisation Create/Edit Post flow allows copy physical schedule day.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Validates the schedule for continue used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun validateScheduleForContinue(): Boolean {
        val current = _uiState.value
        if (current.scheduleEditorDraft != null) {
            setScheduleError("You have unfinished schedule input. Resume it and save, or discard it before continuing.")
            return false
        }

        val error = validateStepFourForCurrentEditor(
            draft = current.draft,
            roleCatalogue = current.roleCatalogue
        )

        _uiState.update { state ->
            state.copy(
                scheduleError = error,
                showScheduleErrors = error != null,
                validationFocusRequest = if (error == null) {
                    state.validationFocusRequest
                } else {
                    state.validationFocusRequest + 1L
                },
                isStepFourReady = error == null
            )
        }
        return error == null
    }

    /**
     * Returns the schedule proceed warning used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

        val currentPolicy = _uiState.value.editPolicy
        if (_uiState.value.isExistingPostEdit && currentPolicy?.isReadOnly == true) {
            allowEdit(
                allowed = false,
                message = currentPolicy.readOnlyReason ?: "This post is read-only."
            )
            return
        }

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
            validateStepFourForCurrentEditor(
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

    /**
     * Saves the changes for the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun saveChanges(context: Context) {
        val current = _uiState.value
        val mode = current.editorMode as? CreatePostEditorMode.ExistingPostEdit ?: return
        if (current.isSavingChanges || current.isSavingDraft || current.isPublishing) return
        if (!validateScheduleForContinue()) return

        val validationError = postSaveValidationError(
            state = _uiState.value,
            ignoreMinimumLeadTime = true
        )
        if (validationError != null) {
            _uiState.update { it.copy(saveChangesError = validationError) }
            return
        }

        _uiState.update {
            it.copy(
                isSavingChanges = true,
                saveChangesError = null,
                editRestrictionMessage = null
            )
        }

        viewModelScope.launch {
            try {
                // Always refresh dependency/history state immediately before
                // saving. A volunteer may have applied after Edit was opened.
                val latest = createPostRepository.loadExistingPostForEdit(mode.postId)
                val latestPolicy = PostEditPolicyEvaluator.evaluate(latest.policyInput)
                val edited = _uiState.value.draft

                val originalRoleIds = latest.draft.selectedRoles
                    .map { it.roleTemplateId }
                    .toSet()
                val newRoles = edited.selectedRoles.filter {
                    it.roleTemplateId !in originalRoleIds
                }
                for (newRole in newRoles) {
                    val modeForRole = _uiState.value.roleCatalogue.firstOrNull {
                        it.roleTemplateId == newRole.roleTemplateId
                    }?.roleMode ?: continue
                    val allowed = when (modeForRole) {
                        VolunteerRoleMode.PHYSICAL -> latestPolicy.canAddPhysicalRole
                        VolunteerRoleMode.REMOTE -> latestPolicy.canAddRemoteRole
                    }
                    if (!allowed) {
                        _uiState.update {
                            it.copy(
                                isSavingChanges = false,
                                editPolicy = latestPolicy,
                                saveChangesError =
                                    "A new ${modeForRole.displayName} role can no longer be added because the post lifecycle changed while you were editing."
                            )
                        }
                        return@launch
                    }
                }

                val originalScheduleIds = latest.draft.scheduleItems
                    .map { it.draftId }
                    .toSet()
                for (newItem in edited.scheduleItems.filter { it.draftId !in originalScheduleIds }) {
                    val allowed = when (newItem.scheduleType) {
                        ScheduleType.PHYSICAL -> latestPolicy.canAddPhysicalSchedule
                        ScheduleType.REMOTE -> latestPolicy.canAddRemoteSchedule
                    }
                    if (!allowed) {
                        _uiState.update {
                            it.copy(
                                isSavingChanges = false,
                                editPolicy = latestPolicy,
                                saveChangesError =
                                    "A new ${newItem.scheduleType.displayName} item can no longer be added because the post lifecycle changed while you were editing."
                            )
                        }
                        return@launch
                    }
                }

                val unsafeChange = PostEditPolicyEvaluator.validateChanges(
                    original = latest.draft,
                    edited = edited,
                    policy = latestPolicy
                )
                if (unsafeChange != null) {
                    _uiState.update {
                        it.copy(
                            isSavingChanges = false,
                            editPolicy = latestPolicy,
                            saveChangesError = unsafeChange
                        )
                    }
                    return@launch
                }

                if (latest.draft.physicalStartDateMillis != edited.physicalStartDateMillis) {
                    CreatePostValidator.minimumLeadTimeError(edited.physicalStartDateMillis)
                        ?.let { message ->
                            _uiState.update {
                                it.copy(
                                    isSavingChanges = false,
                                    saveChangesError = message
                                )
                            }
                            return@launch
                        }
                }
                if (latest.draft.remoteStartDateMillis != edited.remoteStartDateMillis) {
                    CreatePostValidator.minimumLeadTimeError(edited.remoteStartDateMillis)
                        ?.let { message ->
                            _uiState.update {
                                it.copy(
                                    isSavingChanges = false,
                                    saveChangesError = message
                                )
                            }
                            return@launch
                        }
                }

                val thumbnail = prepareThumbnailForSave(
                    context = context.applicationContext,
                    thumbnailUri = edited.thumbnailUri
                )
                val result = createPostRepository.updateExistingPost(
                    latest = latest,
                    editedDraft = edited,
                    roleCatalogue = _uiState.value.roleCatalogue,
                    thumbnail = thumbnail
                )

                originalExistingPost = latest.copy(draft = edited)
                _uiState.update {
                    it.copy(
                        isSavingChanges = false,
                        saveChangesError = null,
                        updatedPostId = result.postId
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isSavingChanges = false,
                        saveChangesError = e.message ?: "Could not save these changes."
                    )
                }
            }
        }
    }

    /**
     * Saves the draft for the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Closes or clears the save draft date warning in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun dismissSaveDraftDateWarning() {
        _uiState.update { state ->
            state.copy(saveDraftDateWarning = null)
        }
    }

    /**
     * Saves the draft internal for the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun saveDraftInternal(
        context: Context,
        allowMinimumLeadTimeIssue: Boolean
    ) {
        val current = _uiState.value
        if (current.isSavingDraft || current.isPublishing) return
        if (current.impactWeaveDraftId != null) {
            _uiState.update {
                it.copy(saveDraftError = "Impact Weave plans publish directly and cannot create a separate post draft.")
            }
            return
        }
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

    /**
     * Publishes the current Volunteer Post data after the required Create/Edit Post checks pass.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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
                    thumbnail = thumbnail,
                    impactWeaveDraftId = snapshot.impactWeaveDraftId
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

    /**
     * Closes or clears the publish date block in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Returns the post save validation error used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

        return validateStepFourForCurrentEditor(
            draft = state.draft,
            roleCatalogue = state.roleCatalogue
        )
    }

    /**
     * Existing schedules can become invalid under today's AppClock without the
     * organisation changing them (for example, a Physical activity becomes past).
     * An untouched historical item must not block an unrelated Manage Edit.
     * New or modified schedule data still uses the current Create rules.
     */
    private fun validateStepFourForCurrentEditor(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): String? {
        if (!_uiState.value.isExistingPostEdit) {
            return CreatePostValidator.validateStepFour(
                draft = draft,
                roleCatalogue = roleCatalogue
            )
        }

        val original = originalExistingPost?.draft
            ?: return CreatePostValidator.validateStepFour(
                draft = draft,
                roleCatalogue = roleCatalogue
            )

        val contextUnchanged = scheduleValidationContextUnchanged(
            current = draft,
            original = original
        )
        val originalItemsById = original.scheduleItems.associateBy { it.draftId }

        draft.scheduleItems.forEach { item ->
            val error = CreatePostValidator.validateScheduleItem(
                draft = draft,
                item = item,
                roleCatalogue = roleCatalogue
            )

            if (error != null) {
                val unchangedExistingItem = contextUnchanged &&
                        originalItemsById[item.draftId] == item
                if (!unchangedExistingItem) return error
            }
        }

        return null
    }

    /**
     * Derives the schedule item validation message for current editor value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun scheduleItemValidationMessageForCurrentEditor(
        draft: CreatePostDraft,
        item: ScheduleItemDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): String? {
        val error = CreatePostValidator.validateScheduleItem(
            draft = draft,
            item = item,
            roleCatalogue = roleCatalogue
        ) ?: return null

        if (!_uiState.value.isExistingPostEdit) return error
        val original = originalExistingPost?.draft ?: return error
        val originalItem = original.scheduleItems.firstOrNull {
            it.draftId == item.draftId
        } ?: return error

        return if (
            scheduleValidationContextUnchanged(draft, original) &&
            originalItem == item
        ) {
            null
        } else {
            error
        }
    }

    /**
     * Derives the schedule validation context unchanged value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun scheduleValidationContextUnchanged(
        current: CreatePostDraft,
        original: CreatePostDraft
    ): Boolean {
        return current.postType == original.postType &&
                current.physicalStartDateMillis == original.physicalStartDateMillis &&
                current.physicalEndDateMillis == original.physicalEndDateMillis &&
                current.physicalStartTimeMinutes == original.physicalStartTimeMinutes &&
                current.physicalEndTimeMinutes == original.physicalEndTimeMinutes &&
                current.physicalLocation == original.physicalLocation &&
                current.remoteStartDateMillis == original.remoteStartDateMillis &&
                current.remoteDueDateMillis == original.remoteDueDateMillis &&
                current.selectedRoles.map { it.roleTemplateId }.toSet() ==
                original.selectedRoles.map { it.roleTemplateId }.toSet()
    }

    /**
     * Prepares the thumbnail for save for the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Opens the new schedule editor in the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun openNewScheduleEditor(
        item: ScheduleItemDraft,
        selectedPhysicalDate: Long? = null
    ) {
        _uiState.update { state ->
            state.copy(
                activeScheduleSection = item.scheduleType,
                selectedPhysicalScheduleDateMillis = selectedPhysicalDate
                    ?: state.selectedPhysicalScheduleDateMillis,
                editingScheduleItemId = null,
                scheduleEditorDraft = item,
                isScheduleEditorOpen = true,
                scheduleError = null,
                showScheduleErrors = false,
                isStepFourReady = false
            )
        }
    }

    /**
     * Updates the schedule editor used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun updateScheduleEditor(
        transform: (ScheduleItemDraft) -> ScheduleItemDraft
    ) {
        val policy = currentSchedulePolicy()
        if (!allowEdit(
                policy?.canEdit != false,
                policy?.reason
                    ?: "This schedule item is part of the post's history and cannot be changed."
            )
        ) return
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

    /**
     * Renders the clean schedule item item used in the organisation Create/Edit Post flow.
     * It receives state and callbacks from its caller so presentation code stays separate from database operations.
     */
    private fun cleanScheduleItem(
        item: ScheduleItemDraft
    ): ScheduleItemDraft {
        return item.copy(
            title = item.title.trim(),
            location = item.location.trim(),
            targetRoleTemplateIds = item.targetRoleTemplateIds.distinct(),
            notes = item.notes.trim()
        )
    }

    /**
     * Renders the draft with editor item item used in the organisation Create/Edit Post flow.
     * It receives state and callbacks from its caller so presentation code stays separate from database operations.
     */
    private fun draftWithEditorItem(
        draft: CreatePostDraft,
        item: ScheduleItemDraft
    ): CreatePostDraft {
        val editingId = _uiState.value.editingScheduleItemId
        val items = if (editingId == null) {
            draft.scheduleItems + item
        } else {
            draft.scheduleItems.map { existing ->
                if (existing.draftId == editingId) item else existing
            }
        }

        return draft.copy(scheduleItems = items)
    }

    /**
     * Updates the step four draft used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Sets the schedule error used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun setScheduleError(message: String) {
        _uiState.update { state ->
            state.copy(
                scheduleError = message,
                showScheduleErrors = true,
                validationFocusRequest = state.validationFocusRequest + 1L,
                isStepFourReady = false
            )
        }
    }

    /**
     * Returns the available schedule sections value required by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun availableScheduleSections(
        postType: VolunteerPostType?
    ): List<ScheduleType> {
        return when (postType) {
            VolunteerPostType.PHYSICAL -> listOf(ScheduleType.PHYSICAL)
            VolunteerPostType.REMOTE -> listOf(ScheduleType.REMOTE)
            VolunteerPostType.HYBRID -> listOf(
                ScheduleType.PHYSICAL,
                ScheduleType.REMOTE
            )

            null -> emptyList()
        }
    }

    /**
     * Returns the valid selected physical date value required by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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


    /**
     * Updates the role configuration used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Sets the role settings error used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun setRoleSettingsError(message: String) {
        _uiState.update { state ->
            state.copy(
                roleSettingsError = message,
                isStepThreeReady = false
            )
        }
    }

    /**
     * Step 2 can remove a role after Step 4 has already been configured. Keep
     * saved schedule targets inside the current selected role set so stale
     * ROLE IDs cannot reach the database.
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

    /**
     * Derives the clean schedule item role references value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun cleanScheduleItemRoleReferences(
        item: ScheduleItemDraft,
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): ScheduleItemDraft {
        val applicableRoleIds = CreatePostValidator.applicableScheduleRoleIds(
            draft = draft,
            scheduleType = item.scheduleType,
            roleCatalogue = roleCatalogue
        ).toSet()

        return item.copy(
            targetRoleTemplateIds = item.targetRoleTemplateIds
                .filter { roleId -> roleId in applicableRoleIds }
                .distinct()
        )
    }

    /**
     * Updates the step two draft used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Checks whether the role matches the post type required by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Derives the required capacity for mode value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Derives the assigned capacity for mode value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    private fun validateStepOneForCurrentEditor(
        draft: CreatePostDraft
    ): com.example.volunteerlink.organisation.create.model.CreatePostErrors {
        var errors = CreatePostValidator.validateStepOne(draft)
        val original = originalExistingPost?.draft

        if (_uiState.value.isExistingPostEdit && original != null) {
            errors = CreatePostValidator.withoutMinimumLeadTimeErrors(
                draft = draft,
                errors = errors,
                ignorePhysical = draft.physicalStartDateMillis == original.physicalStartDateMillis,
                ignoreRemote = draft.remoteStartDateMillis == original.remoteStartDateMillis
            )
        }

        return errors
    }

    /**
     * Derives the continue from step one value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun continueFromStepOne(): Boolean {
        val currentDraft = _uiState.value.draft
        val errors = validateStepOneForCurrentEditor(currentDraft)
        val ready = !errors.hasErrors()

        _uiState.update { current ->
            current.copy(
                // Clear data for modes that are no longer selected only after the current step validates.
                draft = if (ready) {
                    current.draft.keepOnlySelectedModeData()
                } else {
                    current.draft
                },
                errors = errors,
                showValidationErrors = true,
                validationFocusRequest = if (ready) {
                    current.validationFocusRequest
                } else {
                    current.validationFocusRequest + 1L
                },
                isStepOneReady = ready,
                pendingPostType = null,
                isPostTypeCommitted = ready
            )
        }

        return ready
    }

    /**
     * Checks whether the current Create/Edit Post state has unsaved input.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun hasUnsavedInput(): Boolean {
        val current = _uiState.value
        val original = originalExistingPost
        return if (current.isExistingPostEdit && original != null) {
            current.draft != original.draft
        } else {
            current.hasUnsavedInput()
        }
    }

    /**
     * Derives the discard draft value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    fun discardDraft() {
        locationSearchJob?.cancel()
        _uiState.value = CreatePostUiState()
    }

    /**
     * Step 1 date changes must keep already-saved Step 4 schedule items inside
     * the new event/project window. Without this, moving a post to satisfy the
     * 7-day publish rule leaves the old schedule dates behind and Step 4 can no
     * longer be completed.
     */
    private fun updateDraft(
        change: (CreatePostDraft) -> CreatePostDraft
    ) {
        _uiState.update { current ->
            val requestedDraft = change(current.draft)
            val newDraft = rebaseSavedScheduleDates(
                previous = current.draft,
                updated = requestedDraft
            )
            val rebasedEditorDraft = current.scheduleEditorDraft?.let { item ->
                rebaseScheduleItemDate(
                    previous = current.draft,
                    updated = newDraft,
                    item = item
                )
            }
            val rebasedSelectedPhysicalDate = rebaseSelectedPhysicalDate(
                previous = current.draft,
                updated = newDraft,
                selectedDate = current.selectedPhysicalScheduleDateMillis
            )

            current.copy(
                draft = newDraft,
                selectedPhysicalScheduleDateMillis = rebasedSelectedPhysicalDate,
                scheduleEditorDraft = rebasedEditorDraft,
                errors = if (current.showValidationErrors) {
                    validateStepOneForCurrentEditor(newDraft)
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

    /**
     * Holds the values represented by schedule date window as one strongly typed model.
     * It supports state coordination and user actions for the Create/Edit Post flow.
     */
    private data class ScheduleDateWindow(
        val start: Long,
        val end: Long
    )

    /**
     * Re-maps the saved schedule dates when the organisation Create/Edit Post date range changes.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun rebaseSavedScheduleDates(
        previous: CreatePostDraft,
        updated: CreatePostDraft
    ): CreatePostDraft {
        val physicalChanged = physicalScheduleWindowChanged(previous, updated)
        val remoteChanged = remoteScheduleWindowChanged(previous, updated)

        if (!physicalChanged && !remoteChanged) return updated

        return updated.copy(
            scheduleItems = updated.scheduleItems.map { item ->
                rebaseScheduleItemDate(
                    previous = previous,
                    updated = updated,
                    item = item
                )
            }
        )
    }

    /**
     * Re-maps the schedule item date when the organisation Create/Edit Post date range changes.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun rebaseScheduleItemDate(
        previous: CreatePostDraft,
        updated: CreatePostDraft,
        item: ScheduleItemDraft
    ): ScheduleItemDraft {
        val changed = when (item.scheduleType) {
            ScheduleType.PHYSICAL -> physicalScheduleWindowChanged(previous, updated)
            ScheduleType.REMOTE -> remoteScheduleWindowChanged(previous, updated)
        }
        if (!changed) return item

        val oldWindow = scheduleDateWindow(previous, item.scheduleType)
        val newWindow = scheduleDateWindow(updated, item.scheduleType)
            ?: return item
        val currentDate = item.scheduleDateMillis
            ?.let(CreatePostValidator::startOfDayMillis)
            ?: return item

        return item.copy(
            scheduleDateMillis = rebaseDateIntoWindow(
                date = currentDate,
                oldWindow = oldWindow,
                newWindow = newWindow
            )
        )
    }

    /**
     * Re-maps the selected physical date when the organisation Create/Edit Post date range changes.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun rebaseSelectedPhysicalDate(
        previous: CreatePostDraft,
        updated: CreatePostDraft,
        selectedDate: Long?
    ): Long? {
        if (selectedDate == null) return null
        if (!physicalScheduleWindowChanged(previous, updated)) {
            return validSelectedPhysicalDate(updated, selectedDate)
        }

        val newWindow = scheduleDateWindow(updated, ScheduleType.PHYSICAL)
            ?: return null
        val rebased = rebaseDateIntoWindow(
            date = CreatePostValidator.startOfDayMillis(selectedDate),
            oldWindow = scheduleDateWindow(previous, ScheduleType.PHYSICAL),
            newWindow = newWindow
        )

        return validSelectedPhysicalDate(updated, rebased)
    }

    /**
     * Checks whether the physical schedule window changed compared with the previously loaded value.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun physicalScheduleWindowChanged(
        previous: CreatePostDraft,
        updated: CreatePostDraft
    ): Boolean {
        return previous.isMultiDayPhysicalEvent != updated.isMultiDayPhysicalEvent ||
            normalizedDate(previous.physicalStartDateMillis) != normalizedDate(updated.physicalStartDateMillis) ||
            normalizedDate(previous.physicalEndDateMillis) != normalizedDate(updated.physicalEndDateMillis)
    }

    /**
     * Checks whether the remote schedule window changed compared with the previously loaded value.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun remoteScheduleWindowChanged(
        previous: CreatePostDraft,
        updated: CreatePostDraft
    ): Boolean {
        return normalizedDate(previous.remoteStartDateMillis) != normalizedDate(updated.remoteStartDateMillis) ||
            normalizedDate(previous.remoteDueDateMillis) != normalizedDate(updated.remoteDueDateMillis)
    }

    /**
     * Derives the schedule date window value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun scheduleDateWindow(
        draft: CreatePostDraft,
        scheduleType: ScheduleType
    ): ScheduleDateWindow? {
        return when (scheduleType) {
            ScheduleType.PHYSICAL -> {
                val start = normalizedDate(draft.physicalStartDateMillis) ?: return null
                val requestedEnd = if (draft.isMultiDayPhysicalEvent) {
                    normalizedDate(draft.physicalEndDateMillis) ?: return null
                } else {
                    start
                }
                ScheduleDateWindow(
                    start = start,
                    end = requestedEnd.coerceAtLeast(start)
                )
            }

            ScheduleType.REMOTE -> {
                val start = normalizedDate(draft.remoteStartDateMillis) ?: return null
                val requestedEnd = normalizedDate(draft.remoteDueDateMillis) ?: return null
                ScheduleDateWindow(
                    start = start,
                    end = requestedEnd.coerceAtLeast(start)
                )
            }
        }
    }

    /**
     * If the whole range moved while keeping the same duration, preserve the
     * item's relative day (Day 1 -> Day 1, Day 2 -> Day 2, and so on). When
     * only one edge of the range changed, keep dates that are still valid and
     * move only the dates that fell outside the new range.
     */
    private fun rebaseDateIntoWindow(
        date: Long,
        oldWindow: ScheduleDateWindow?,
        newWindow: ScheduleDateWindow
    ): Long {
        val normalized = CreatePostValidator.startOfDayMillis(date)
        if (oldWindow == null) {
            return normalized.coerceIn(newWindow.start, newWindow.end)
        }

        val oldSpan = calendarDayOffset(oldWindow.start, oldWindow.end)
        val newSpan = calendarDayOffset(newWindow.start, newWindow.end)
        val wholeRangeShifted = oldWindow.start != newWindow.start && oldSpan == newSpan

        if (!wholeRangeShifted && normalized in newWindow.start..newWindow.end) {
            return normalized
        }

        if (!wholeRangeShifted) {
            return normalized.coerceIn(newWindow.start, newWindow.end)
        }

        val relativeDay = calendarDayOffset(oldWindow.start, normalized)
        return addCalendarDays(newWindow.start, relativeDay)
            .coerceIn(newWindow.start, newWindow.end)
    }

    /**
     * Normalises the date into the consistent form used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun normalizedDate(value: Long?): Long? {
        return value?.let(CreatePostValidator::startOfDayMillis)
    }

    /**
     * Derives the calendar day offset value used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun calendarDayOffset(
        fromMillis: Long,
        toMillis: Long
    ): Int {
        val from = CreatePostValidator.startOfDayMillis(fromMillis)
        val to = CreatePostValidator.startOfDayMillis(toMillis)
        if (from == to) return 0

        val direction = if (to > from) 1 else -1
        val calendar = Calendar.getInstance().apply { timeInMillis = from }
        var offset = 0

        while (calendar.timeInMillis != to && kotlin.math.abs(offset) < 3660) {
            calendar.add(Calendar.DAY_OF_YEAR, direction)
            offset += direction
        }

        return offset
    }

    /**
     * Adds the calendar days to the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun addCalendarDays(
        dateMillis: Long,
        days: Int
    ): Long {
        return Calendar.getInstance().apply {
            timeInMillis = CreatePostValidator.startOfDayMillis(dateMillis)
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
    }

    /**
     * Parses the positive number used by the organisation Create/Edit Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun parsePositiveNumber(text: String): Int? {
        val digitsOnly = text.filter { it.isDigit() }.take(4)
        if (digitsOnly.isBlank()) return null

        return digitsOnly.toIntOrNull()?.takeIf { it > 0 }
    }
}
