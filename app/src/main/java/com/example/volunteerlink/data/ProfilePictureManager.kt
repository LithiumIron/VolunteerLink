package com.example.volunteerlink.data

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Provides shared profile-image storage helpers used when an Organisation selects or updates profile media.
//
// The helper isolates image-path/upload handling from the profile composables and returns a storage path that the
// profile repository can persist.
//
// The database stores the path/reference rather than embedding image bytes in profile rows.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import android.content.Context
import android.net.Uri
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import java.util.UUID

/**
 * DETAILED BEHAVIOUR — saveProfileImage
 *
 * Implements the current VolunteerLink responsibility for save profile image in this support/model layer.
 *
 * Reads/maps Supabase table data from `profile-images` (normalized VolunteerLink records used by this
 * workflow).
 *
 * Uses Supabase Storage for binary/file content while database rows keep only the controlled storage path and
 * metadata.
 *
 * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without leaving
 * the UI in an assumed-success state.
 */
suspend fun saveProfileImage(
    context: Context,
    uri: Uri
): String? {

    return try {

        val currentUser = supabase.auth.currentUserOrNull()
            ?: return null

        val inputStream =
            context.contentResolver.openInputStream(uri)
                ?: return null

        val imageBytes = inputStream.use {
            it.readBytes()
        }

        val filePath =
            "${currentUser.id}/${UUID.randomUUID()}.jpg"

        supabase.storage
            .from("profile-images")
            .upload(
                path = filePath,
                data = imageBytes
            )

        supabase.storage
            .from("profile-images")
            .publicUrl(filePath)

    } catch (e: Exception) {

        e.printStackTrace()
        null
    }
}