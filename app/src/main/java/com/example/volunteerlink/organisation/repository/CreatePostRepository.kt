package com.example.volunteerlink.organisation.repository

// FILE OVERVIEW:
/*
 * CreatePostRepository defines or implements data access used by the organisation Create/Edit Post flow.
 * Repository code keeps Supabase/RPC/storage details away from the composables and ViewModels
 * so UI code can work with application models instead of backend-specific responses.
 */


import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.PostEditPolicyInput
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate

/** Selected thumbnail bytes prepared by the ViewModel for saving or publishing. */
data class PublishThumbnail(
    val bytes: ByteArray,
    val mimeType: String,
    val fileExtension: String
)

/** Small result returned after the whole post has been stored successfully. */
data class SavedPostResult(
    val postId: String,
    val thumbnailPath: String?
)


/** Existing normalized post loaded back into the shared Create/Edit wizard. */
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
interface CreatePostRepository {
    /**
     * Loads the role catalogue needed by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    suspend fun loadRoleCatalogue(): List<CreateRoleTemplate>

    /**
     * Loads the existing post for edit needed by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    suspend fun loadExistingPostForEdit(postId: String): ExistingPostEditData

    /**
     * Updates the existing post used by the organisation Create/Edit Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
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
    suspend fun saveDraft(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): SavedPostResult

    /**
     * Publishes the current Volunteer Post data after the required Create/Edit Post checks pass.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    suspend fun publishPost(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?,
        impactWeaveDraftId: String? = null
    ): SavedPostResult
}
