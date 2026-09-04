package com.example.volunteerlink.organisation.screens.chat

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements Organisation chat presentation/interaction associated with Organisation Camera Utils.
//
// The screen/component renders shared chat models and emits repository-facing actions through callbacks or
// coroutine calls rather than editing database tables directly.
//
// Sent messages, membership and read state remain Supabase-authoritative; only unsent text may be kept in account-
// scoped local storage as a convenience.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Creates a brand-new empty file under cacheDir/images and returns a content:// Uri for it
 * (via the FileProvider declared in AndroidManifest.xml). Pass this Uri into
 * ActivityResultContracts.TakePicture().launch(uri) - the camera app writes the
 * photo bytes directly into this file, no CAMERA-intent extras needed.
 */
/**
 * DETAILED BEHAVIOUR — createOrganisationCameraImageUri
 *
 * Handles the Compose/UI responsibility for create organisation camera image uri.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun createOrganisationCameraImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(imagesDir, "chat_photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * DETAILED BEHAVIOUR — createOrganisationCameraVideoUri
 *
 * Handles the Compose/UI responsibility for create organisation camera video uri.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun createOrganisationCameraVideoUri(context: Context): Uri {
    val videosDir = File(context.cacheDir, "videos").apply { mkdirs() }
    val file = File(videosDir, "chat_video_${System.currentTimeMillis()}.mp4")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * DETAILED BEHAVIOUR — createOrganisationVoiceRecordingFile
 *
 * Handles the Compose/UI responsibility for create organisation voice recording file.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun createOrganisationVoiceRecordingFile(context: Context): File {
    val audioDir = File(context.cacheDir, "audio").apply { mkdirs() }
    return File(audioDir, "voice_note_${System.currentTimeMillis()}.m4a")
}
