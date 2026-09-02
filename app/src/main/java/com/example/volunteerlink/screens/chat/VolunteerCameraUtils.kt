package com.example.volunteerlink.screens.chat

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
fun createVolunteerCameraImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(imagesDir, "chat_photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun createVolunteerCameraVideoUri(context: Context): Uri {
    val videosDir = File(context.cacheDir, "videos").apply { mkdirs() }
    val file = File(videosDir, "chat_video_${System.currentTimeMillis()}.mp4")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun createVolunteerVoiceRecordingFile(context: Context): File {
    val audioDir = File(context.cacheDir, "audio").apply { mkdirs() }
    return File(audioDir, "voice_note_${System.currentTimeMillis()}.m4a")
}
