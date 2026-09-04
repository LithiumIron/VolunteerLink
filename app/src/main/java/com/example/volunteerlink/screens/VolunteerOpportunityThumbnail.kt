package com.example.volunteerlink.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val VOLUNTEER_THUMBNAIL_BUCKET = "post-thumbnails"

/**
 * Loads Organisation thumbnails with the authenticated Supabase session and
 * keeps the bytes in app-private storage so the last online image remains
 * available when the volunteer later reopens the app offline.
 */
private suspend fun loadVolunteerThumbnail(
    context: Context,
    storagePath: String
): ImageBitmap? = withContext(Dispatchers.IO) {
    val cacheDirectory = File(
        context.filesDir,
        "volunteer_opportunity_thumbnails"
    ).apply {
        mkdirs()
    }
    val cacheFile = File(
        cacheDirectory,
        (storagePath.hashCode() and Int.MAX_VALUE).toString()
    )

    val bytes = runCatching {
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            cacheFile.readBytes()
        } else {
            supabase.storage
                .from(VOLUNTEER_THUMBNAIL_BUCKET)
                .downloadAuthenticated(storagePath)
                .also(cacheFile::writeBytes)
        }
    }.getOrNull() ?: return@withContext null

    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?.asImageBitmap()
}

@Composable
// Purpose: Handles volunteer opportunity thumbnail as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
fun VolunteerOpportunityThumbnail(
    storagePath: String?,
    @DrawableRes fallbackIconResourceId: Int,
    modifier: Modifier,
    contentDescription: String,
    cornerRadius: Dp = 10.dp
) {
    val context = LocalContext.current
    val thumbnail by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = storagePath
    ) {
        value = storagePath
            ?.takeIf(String::isNotBlank)
            ?.let { loadVolunteerThumbnail(context, it) }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(VolunteerLinkSoftGreenSurface),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(fallbackIconResourceId),
                contentDescription = contentDescription,
                tint = VolunteerLinkPrimaryGreen,
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    }
}
