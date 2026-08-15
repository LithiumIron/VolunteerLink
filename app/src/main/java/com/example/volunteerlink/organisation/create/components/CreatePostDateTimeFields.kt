package com.example.volunteerlink.organisation.create.components

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.model.CreatePostDateRules
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DateSelectionField(
    label: String,
    selectedDateMillis: Long?,
    minimumDateMillis: Long,
    maximumDateMillis: Long? = null,
    errorMessage: String? = null,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    FormSelectionField(
        label = label,
        value = formatDate(selectedDateMillis),
        placeholder = "Select date",
        iconRes = R.drawable.calendar,
        errorMessage = errorMessage,
        modifier = modifier,
        onClick = {
            val initialDate = selectedDateMillis
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
fun TimeSelectionField(
    label: String,
    selectedTimeMinutes: Int?,
    dialogInitialTimeMinutes: Int? = selectedTimeMinutes,
    errorMessage: String? = null,
    onDialogOpened: () -> Unit = {},
    onTimeSelected: (hour24: Int, minute: Int) -> String?,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    FormSelectionField(
        label = label,
        value = formatTime(selectedTimeMinutes),
        placeholder = "Set time",
        errorMessage = errorMessage,
        modifier = modifier,
        onClick = {
            onDialogOpened()
            showDialog = true
        }
    )

    if (showDialog) {
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

fun formatDate(dateMillis: Long?): String {
    if (dateMillis == null) return ""

    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(Date(dateMillis))
}

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

fun minimumCreatePostStartDateMillis(): Long {
    return CreatePostDateRules.minimumStartDateMillis()
}
