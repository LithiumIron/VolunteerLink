package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate

/** Selected thumbnail bytes prepared by the ViewModel for publishing. */
data class PublishThumbnail(
    val bytes: ByteArray,
    val mimeType: String,
    val fileExtension: String
)

/** Small result returned after the whole post has been published successfully. */
data class PublishedPostResult(
    val postId: String,
    val thumbnailPath: String?
)

/**
 * Database access used by the Organisation Create Post flow.
 *
 * The UI never talks to Supabase directly. The repository loads the fixed role
 * catalogue and publishes the completed Create Post draft.
 */
interface CreatePostRepository {
    suspend fun loadRoleCatalogue(): List<CreateRoleTemplate>

    suspend fun publishPost(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        thumbnail: PublishThumbnail?
    ): PublishedPostResult
}
