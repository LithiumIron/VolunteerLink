package com.example.volunteerlink.data

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import java.util.UUID

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