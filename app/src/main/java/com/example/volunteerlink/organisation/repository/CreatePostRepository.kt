package com.example.volunteerlink.organisation.repository

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
    suspend fun loadRoleCatalogue(): List<CreateRoleTemplate>

    suspend fun loadExistingPostForEdit(postId: String): ExistingPostEditData

    suspend fun updateExistingPost(
        latest: ExistingPostEditData,
        editedDraft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): SavedPostResult

    suspend fun saveDraft(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): SavedPostResult

    suspend fun publishPost(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?,
        impactWeaveDraftId: String? = null
    ): SavedPostResult
}
