package com.example.volunteerlink.organisation.create.model

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines the Create Post state/model structures associated with Create Post Ui State.
//
// These models are UI/business-layer data: they let the five-step wizard hold incomplete user input before it is
// converted to a validated repository payload.
//
// Database ids/rows are introduced only when a real saved draft or published post is persisted; local autosave
// serializes the same draft state for recovery without making it authoritative.
//
// Architectural layer: Domain/UI model layer.
// ============================================================================


import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.create.PostEditPolicy
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePostPartner


/** Which database action the shared post editor represents. */
/**
 * DETAILED DECLARATION — CreatePostEditorMode
 *
 * Contract for Create Post Editor Mode. Callers depend on this abstraction rather than a concrete Supabase
 * implementation.
 *
 * Implementations may perform network/storage work, while ViewModels and Compose remain expressed in
 * VolunteerLink domain types.
 */
sealed interface CreatePostEditorMode {
    /**
     * DETAILED DECLARATION — NewPost
     *
     * Single shared instance for New Post so related rules/state are defined once for the application process.
     */
    data object NewPost : CreatePostEditorMode
    /**
     * Holds the values represented by existing post edit as one strongly typed model.
     * It keeps related Create/Edit Post values together so callers do not pass disconnected fields around.
     */
    /**
     * DETAILED DECLARATION — ExistingPostEdit
     *
     * Domain/UI type for Existing Post Edit used by the Organisation module.
     *
     * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-
     * typed maps.
     */
    data class ExistingPostEdit(val postId: String) : CreatePostEditorMode
}

/** Field-level validation messages for Create Post Step 1. */
/**
 * DETAILED DECLARATION — CreatePostErrors
 *
 * Domain/UI type for Create Post Errors used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class CreatePostErrors(
    val postType: String? = null,
    val category: String? = null,
    val title: String? = null,
    val description: String? = null,
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
    /**
     * Checks whether the current Create/Edit Post state has errors.
     * Keeping this transformation near the model makes the data flow easier to understand.
     */
    /**
     * DETAILED BEHAVIOUR — hasErrors
     *
     * Implements the current VolunteerLink responsibility for has errors in this support/model layer.
     */
    fun hasErrors(): Boolean {
        return listOf(
            postType,
            category,
            title,
            description,
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

/** Step 2 capacity-allocation messages. */
/**
 * DETAILED DECLARATION — RoleSelectionErrors
 *
 * Domain/UI type for Role Selection Errors used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class RoleSelectionErrors(
    val general: String? = null,
    val physical: String? = null,
    val remote: String? = null
) {
    /**
     * Checks whether the current Create/Edit Post state has errors.
     * Keeping this transformation near the model makes the data flow easier to understand.
     */
    /**
     * DETAILED BEHAVIOUR — hasErrors
     *
     * Implements the current VolunteerLink responsibility for has errors in this support/model layer.
     */
    fun hasErrors(): Boolean {
        return general != null || physical != null || remote != null
    }
}

/**
 * Everything the Create Post UI currently needs to display.
 * MutableStateFlow stays private inside CreatePostViewModel.
 */
/**
 * DETAILED DECLARATION — CreatePostUiState
 *
 * Immutable snapshot of all UI-visible state required by Create Post Ui State.
 *
 * Keeping loading/data/error/action flags together makes recomposition deterministic and avoids hidden mutable
 * state in individual composables.
 */
data class CreatePostUiState(
    val draft: CreatePostDraft = CreatePostDraft(),
    val impactWeaveDraftId: String? = null,
    val isLoadingImpactWeave: Boolean = false,
    val impactWeaveLoadError: String? = null,
    val impactWeavePartners: List<ImpactWeavePostPartner> = emptyList(),

    // The same Step 1-5 editor is reused by Manage > Edit.
    val editorMode: CreatePostEditorMode = CreatePostEditorMode.NewPost,
    val editPolicy: PostEditPolicy? = null,
    val isLoadingExistingPost: Boolean = false,
    val existingPostLoadError: String? = null,
    val editRestrictionMessage: String? = null,
    val isSavingChanges: Boolean = false,
    val saveChangesError: String? = null,
    val updatedPostId: String? = null,
    val locationSuggestions: List<LocationSuggestion> = emptyList(),
    val isLocationSearching: Boolean = false,
    val locationSearchError: String? = null,
    val physicalTimeError: String? = null,
    val errors: CreatePostErrors = CreatePostErrors(),
    val showValidationErrors: Boolean = false,
    // Incremented only when a validation action fails. Long forms observe this
    // value to jump to the relevant error once, without fighting normal typing.
    val validationFocusRequest: Long = 0L,
    val isStepOneReady: Boolean = false,

    // Current Create Post page. Steps 1-4 are editable form pages and
    // Step 5 is the read-only Review Summary.
    val currentStep: Int = 1,

    // Non-null only while the organiser is editing one section from Review.
    // System Back / a successful Save Changes returns to Review instead of
    // walking through the normal wizard sequence.
    val reviewEditStep: Int? = null,

    // Step 2 role catalogue / role selection state.
    val roleCatalogue: List<CreateRoleTemplate> = emptyList(),
    val isRoleCatalogueLoading: Boolean = false,
    val roleCatalogueError: String? = null,
    val roleSearchQuery: String = "",
    val roleModeFilter: VolunteerRoleMode? = null,
    val roleSelectionErrors: RoleSelectionErrors = RoleSelectionErrors(),
    val showRoleSelectionErrors: Boolean = false,
    val isStepTwoReady: Boolean = false,

    // Step 3 role-settings state.
    val editingRoleTemplateId: String? = null,
    val roleSettingsError: String? = null,
    val isStepThreeReady: Boolean = false,

    // Step 4 schedule state. The editor uses a temporary buffer so pressing
    // Add never creates an incomplete saved schedule item.
    val activeScheduleSection: ScheduleType? = null,
    val selectedPhysicalScheduleDateMillis: Long? = null,
    val editingScheduleItemId: String? = null,
    val scheduleEditorDraft: ScheduleItemDraft? = null,
    val isScheduleEditorOpen: Boolean = false,
    val scheduleError: String? = null,
    val showScheduleErrors: Boolean = false,
    val isStepFourReady: Boolean = false,

    // Save Draft and Publish run from Review. Their IDs are kept only long
    // enough to show the matching success screen after local input is cleared.
    val isSavingDraft: Boolean = false,
    val saveDraftError: String? = null,
    val saveDraftDateWarning: String? = null,
    val savedDraftPostId: String? = null,
    val isPublishing: Boolean = false,
    val publishError: String? = null,
    val publishDateBlockMessage: String? = null,
    val publishedPostId: String? = null,

    // Post type switching state.
    val pendingPostType: VolunteerPostType? = null,
    val isPostTypeCommitted: Boolean = false
) {
    val isExistingPostEdit: Boolean
        get() = editorMode is CreatePostEditorMode.ExistingPostEdit

    val existingPostId: String?
        get() = (editorMode as? CreatePostEditorMode.ExistingPostEdit)?.postId

    /** Hide field errors until the organiser first presses Continue. */
    val visibleErrors: CreatePostErrors
        get() = if (showValidationErrors) errors else CreatePostErrors()

    val visibleRoleSelectionErrors: RoleSelectionErrors
        get() = if (showRoleSelectionErrors) {
            roleSelectionErrors
        } else {
            RoleSelectionErrors()
        }

    /**
     * Checks whether the current Create/Edit Post state has unsaved input.
     * Keeping this transformation near the model makes the data flow easier to understand.
     */
    /**
     * DETAILED BEHAVIOUR — hasUnsavedInput
     *
     * Implements the current VolunteerLink responsibility for has unsaved input in this support/model layer.
     */
    fun hasUnsavedInput(): Boolean {
        return draft.hasMeaningfulContent()
    }
}


