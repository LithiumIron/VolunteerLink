package com.example.volunteerlink.organisation.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines the data contract used by CreatePostViewModel without exposing Supabase-specific types to the
// UI/business layer.
//
// The interface separates operations for role catalogue loading, new-post save/publish, existing-post hydration
// and existing-post update.
//
// The Supabase implementation is responsible for translating CreatePostDraft into normalized database payloads and
// storage operations.
//
// Using an interface makes validation/navigation code testable without requiring a live backend.
//
// Architectural layer: Data/repository layer.
// ============================================================================


import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.PostEditPolicyInput
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate

/** Selected thumbnail bytes prepared by the ViewModel for saving or publishing. */
/**
 * DETAILED DECLARATION — PublishThumbnail
 *
 * Domain/UI type for Publish Thumbnail used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PublishThumbnail(
    val bytes: ByteArray,
    val mimeType: String,
    val fileExtension: String
)

/** Small result returned after the whole post has been stored successfully. */
/**
 * DETAILED DECLARATION — SavedPostResult
 *
 * Domain/UI type for Saved Post Result used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class SavedPostResult(
    val postId: String,
    val thumbnailPath: String?
)


/** Existing normalized post loaded back into the shared Create/Edit wizard. */
/**
 * DETAILED DECLARATION — ExistingPostEditData
 *
 * Domain/UI type for Existing Post Edit Data used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class ExistingPostEditData(
    val postId: String,
    val databaseStatus: String,
    val originalUpdatedAt: String,
    val existingThumbnailPath: String?,
    val draft: CreatePostDraft,
    val policyInput: PostEditPolicyInput
)

/**
 * Database access used by the Organisation Create Post flow.
 *
 * The UI never talks to Supabase directly. The repository loads the fixed role
 * catalogue and stores the completed Create Post draft as either DRAFT or PUBLISHED.
 */
/**
 * DETAILED DECLARATION — CreatePostRepository
 *
 * Contract for Create Post Repository. Callers depend on this abstraction rather than a concrete Supabase
 * implementation.
 *
 * Implementations may perform network/storage work, while ViewModels and Compose remain expressed in
 * VolunteerLink domain types.
 */
interface CreatePostRepository {
    /**
     * Loads the role catalogue needed by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadRoleCatalogue
     *
     * Performs the repository/data-layer operation for load role catalogue.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadRoleCatalogue(): List<CreateRoleTemplate>

    /**
     * Loads the existing post for edit needed by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadExistingPostForEdit
     *
     * Performs the repository/data-layer operation for load existing post for edit.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadExistingPostForEdit(postId: String): ExistingPostEditData

    /**
     * Updates the existing post used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — updateExistingPost
     *
     * Performs the repository/data-layer operation for update existing post.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun updateExistingPost(
        latest: ExistingPostEditData,
        editedDraft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): SavedPostResult

    /**
     * Saves the draft for the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — saveDraft
     *
     * Performs the repository/data-layer operation for save draft.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun saveDraft(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): SavedPostResult

    /**
     * Publishes the current Volunteer Post data after the required Create/Edit Post checks pass.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — publishPost
     *
     * Performs the repository/data-layer operation for publish post.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun publishPost(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?,
        impactWeaveDraftId: String? = null
    ): SavedPostResult
}
