package com.example.volunteerlink.organisation.create.components

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Contains reusable Create Post UI building blocks for Create Post Pickers.
//
// Components are intentionally presentation-focused: values and callbacks come from the parent step/ViewModel, so
// a reusable field does not secretly own business state or perform database writes.
//
// Shared components keep spacing, colours, validation presentation and interaction patterns consistent across
// Physical, Remote and Hybrid forms.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.CreatePostValidator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.volunteerlink.ui.theme.CreateGreen

/**
 * Reusable date, time and image pickers used by Create Post.
 *
 * They are grouped here because all three are temporary UI picker concerns;
 * the selected values themselves are still stored in CreatePostViewModel.
 */

@Composable
/**
 * DETAILED BEHAVIOUR — DateSelectionField
 *
 * Renders the reusable Date Selection Field portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun DateSelectionField(
    label: String,
    selectedDateMillis: Long?,
    minimumDateMillis: Long,
    maximumDateMillis: Long? = null,
    errorMessage: String? = null,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current

    FormSelectionField(
        label = label,
        value = formatDate(selectedDateMillis),
        placeholder = "Select date",
        iconRes = R.drawable.calendar,
        errorMessage = errorMessage,
        modifier = modifier,
        enabled = enabled,
        onClick = {
            // Keep an outdated date visible in the form so the organiser can
            // keep the current invalid value visible, while opening the picker on the first valid
            // date instead of focusing a now-disabled day.
            val initialDate = selectedDateMillis
                ?.takeIf { selected ->
                    selected >= minimumDateMillis &&
                        (maximumDateMillis == null || selected <= maximumDateMillis)
                }
                ?: minimumDateMillis

            val initialCalendar = Calendar.getInstance().apply {
                timeInMillis = initialDate
            }

            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selected = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    onDateSelected(selected)
                },
                initialCalendar.get(Calendar.YEAR),
                initialCalendar.get(Calendar.MONTH),
                initialCalendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = minimumDateMillis
                maximumDateMillis?.let { datePicker.maxDate = it }
            }.show()
        }
    )
}

@Composable
/**
 * Renders the time selection field input field used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — TimeSelectionField
 *
 * Renders the reusable Time Selection Field portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun TimeSelectionField(
    label: String,
    selectedTimeMinutes: Int?,
    dialogInitialTimeMinutes: Int? = selectedTimeMinutes,
    errorMessage: String? = null,
    onDialogOpened: () -> Unit = {},
    onTimeSelected: (hour24: Int, minute: Int) -> String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    FormSelectionField(
        label = label,
        value = formatTime(selectedTimeMinutes),
        placeholder = "Set time",
        errorMessage = errorMessage,
        modifier = modifier,
        enabled = enabled,
        onClick = {
            onDialogOpened()
            showDialog = true
        }
    )

    if (showDialog && enabled) {
        KeyboardTimeInputDialog(
            title = label,
            initialTimeMinutes = dialogInitialTimeMinutes,
            onDismiss = {
                showDialog = false
                onDialogOpened()
            },
            onConfirm = onTimeSelected
        )
    }
}

/**
 * Formats the date used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — formatDate
 *
 * Handles the Compose/UI responsibility for format date.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun formatDate(dateMillis: Long?): String {
    if (dateMillis == null) return ""

    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(Date(dateMillis))
}

/**
 * Formats the time used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — formatTime
 *
 * Handles the Compose/UI responsibility for format time.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun formatTime(minutesAfterMidnight: Int?): String {
    if (minutesAfterMidnight == null) return ""

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutesAfterMidnight / 60)
        set(Calendar.MINUTE, minutesAfterMidnight % 60)
    }

    return SimpleDateFormat(
        "h:mm a",
        Locale.getDefault()
    ).format(calendar.time)
}

/**
 * Returns the minimum create post start date millis value required by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — minimumCreatePostStartDateMillis
 *
 * Handles the Compose/UI responsibility for minimum create post start date millis.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 *
 * Runs the shared CreatePostValidator so navigation/save behaviour uses the same validation rules as the rest
 * of the wizard.
 */
fun minimumCreatePostStartDateMillis(): Long {
    return CreatePostValidator.minimumStartDateMillis()
}

/** Keyboard-first 12-hour time input used instead of the clock-face picker. */
@Composable
/**
 * DETAILED BEHAVIOUR — KeyboardTimeInputDialog
 *
 * Renders the Keyboard Time Input Dialog modal interaction and keeps temporary form/confirmation UI separate
 * from the underlying server action.
 *
 * The confirm callback is only emitted after the dialog-side input/choice is in an acceptable state; the
 * ViewModel/repository performs the real mutation.
 */
private fun KeyboardTimeInputDialog(
    title: String,
    initialTimeMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (hour24: Int, minute: Int) -> String?
) {
    val initialHour24 = initialTimeMinutes?.div(60) ?: 9
    val initialMinute = initialTimeMinutes?.rem(60) ?: 0
    val initialIsPm = initialHour24 >= 12
    val initialHour12 = when (val hour = initialHour24 % 12) {
        0 -> 12
        else -> hour
    }

    var hourText by remember(initialTimeMinutes) {
        mutableStateOf(initialHour12.toString())
    }
    var minuteText by remember(initialTimeMinutes) {
        mutableStateOf(initialMinute.toString().padStart(2, '0'))
    }
    var isPm by remember(initialTimeMinutes) {
        mutableStateOf(initialIsPm)
    }
    var inputError by remember { mutableStateOf<String?>(null) }

    val hourFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        hourFocusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Type in time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { value ->
                            hourText = value
                                .filter { it.isDigit() }
                                .take(2)
                            inputError = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(hourFocusRequester),
                        label = { Text("Hour") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { value ->
                            minuteText = value
                                .filter { it.isDigit() }
                                .take(2)
                            inputError = null
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Minute") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimePeriodOption(
                        text = "AM",
                        selected = !isPm,
                        onClick = {
                            isPm = false
                            inputError = null
                        }
                    )

                    TimePeriodOption(
                        text = "PM",
                        selected = isPm,
                        onClick = {
                            isPm = true
                            inputError = null
                        }
                    )
                }

                if (inputError != null) {
                    Text(
                        text = inputError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour12 = hourText.toIntOrNull()
                    val minute = minuteText.toIntOrNull()

                    when {
                        hour12 == null || hour12 !in 1..12 -> {
                            inputError = "Enter an hour from 1 to 12."
                        }

                        minute == null || minute !in 0..59 -> {
                            inputError = "Enter minutes from 00 to 59."
                        }

                        else -> {
                            val hour24 = when {
                                isPm && hour12 != 12 -> hour12 + 12
                                !isPm && hour12 == 12 -> 0
                                else -> hour12
                            }

                            val error = onConfirm(hour24, minute)
                            if (error == null) {
                                keyboardController?.hide()
                                onDismiss()
                            } else {
                                inputError = error
                            }
                        }
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
/**
 * Renders the UI represented by time period option for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — TimePeriodOption
 *
 * Handles the Compose/UI responsibility for time period option.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun TimePeriodOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) {
            CreateGreen.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (selected) CreateGreen else MaterialTheme.colorScheme.outline
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 10.dp
            ),
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                CreateGreen
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/** Optional thumbnail picker with a real preview and visible file name. */
@Composable
/**
 * DETAILED BEHAVIOUR — ThumbnailPickerSection
 *
 * Renders the reusable Thumbnail Picker Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 *
 * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without leaving
 * the UI in an assumed-success state.
 */
fun ThumbnailPickerSection(
    thumbnailUri: String?,
    onThumbnailChanged: (String?) -> Unit,
    enabled: Boolean = true
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
            text = "Thumbnail (Optional · Max 5 MB)",
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
                onClick = { launcher.launch(arrayOf("image/*")) },
                enabled = enabled
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
                            enabled = enabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CreateGreen
                            )
                        ) {
                            Text("Change Image")
                        }

                        OutlinedButton(
                            onClick = { onThumbnailChanged(null) },
                            enabled = enabled
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
/**
 * Renders the UI represented by remember image bitmap for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — rememberImageBitmap
 *
 * Handles the Compose/UI responsibility for remember image bitmap.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 *
 * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
 *
 * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without leaving
 * the UI in an assumed-success state.
 */
private fun rememberImageBitmap(
    uri: Uri
): androidx.compose.runtime.State<ImageBitmap?> {
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

/**
 * Returns the display name used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — getDisplayName
 *
 * Handles the Compose/UI responsibility for get display name.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 *
 * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without leaving
 * the UI in an assumed-success state.
 */
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
