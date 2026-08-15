package com.example.volunteerlink.organisation.create.components

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Optional thumbnail picker with a real preview and visible file name. */
@Composable
fun ThumbnailPickerSection(
    thumbnailUri: String?,
    onThumbnailChanged: (String?) -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not offer persistable permissions.
            }

            onThumbnailChanged(uri.toString())
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Thumbnail (Optional)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "Add an image so volunteers can recognise the opportunity quickly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (thumbnailUri == null) {
            OutlinedButton(
                onClick = { launcher.launch(arrayOf("image/*")) }
            ) {
                Text("Choose Image")
            }
        } else {
            val uri = Uri.parse(thumbnailUri)
            val imageBitmap by rememberImageBitmap(uri)
            val fileName = getDisplayName(context, uri)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = "Selected thumbnail preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = fileName ?: "Selected image",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { launcher.launch(arrayOf("image/*")) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CreateGreen
                            )
                        ) {
                            Text("Change Image")
                        }

                        OutlinedButton(
                            onClick = { onThumbnailChanged(null) }
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberImageBitmap(uri: Uri): androidx.compose.runtime.State<ImageBitmap?> {
    val context = LocalContext.current

    return produceState<ImageBitmap?>(
        initialValue = null,
        key1 = uri
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

private fun getDisplayName(
    context: android.content.Context,
    uri: Uri
): String? {
    return try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }
}
