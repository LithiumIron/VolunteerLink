package com.example.volunteerlink.organisation.create.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Keyboard-first 12-hour time input used instead of the clock-face picker. */
@Composable
fun KeyboardTimeInputDialog(
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
            color = if (selected) CreateGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}
