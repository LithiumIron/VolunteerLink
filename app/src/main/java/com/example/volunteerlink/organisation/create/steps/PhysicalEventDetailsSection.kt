package com.example.volunteerlink.organisation.create.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateSectionCard
import com.example.volunteerlink.organisation.create.components.DateSelectionField
import com.example.volunteerlink.organisation.create.components.LocationAutocompleteField
import com.example.volunteerlink.organisation.create.components.TimeSelectionField
import com.example.volunteerlink.organisation.create.components.VolunteerCapacityField
import com.example.volunteerlink.organisation.create.components.minimumCreatePostStartDateMillis
import com.example.volunteerlink.organisation.create.model.CreatePostDateRules
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostStepOneErrors

@Composable
fun PhysicalEventDetailsSection(
    draft: CreatePostDraft,
    errors: CreatePostStepOneErrors,
    physicalTimeError: String?,
    locationSuggestions: List<LocationSuggestion>,
    isLocationSearching: Boolean,
    locationSearchError: String?,
    showVolunteerCapacity: Boolean,
    onMultiDayChanged: (Boolean) -> Unit,
    onStartDateSelected: (Long) -> Unit,
    onEndDateSelected: (Long) -> Unit,
    onStartTimeSelected: (Int, Int) -> Unit,
    onEndTimeSelected: (Int, Int) -> String?,
    onTimeDialogOpened: () -> Unit,
    onLocationSearchChanged: (String) -> Unit,
    onLocationSelected: (LocationSuggestion) -> Unit,
    onClearLocation: () -> Unit,
    onMeetingPointChanged: (String) -> Unit,
    onVolunteerCapacityChanged: (String) -> Unit
) {
    CreateSectionCard(
        title = "Event Schedule",
        subtitle = "Choose when the physical event will take place. Start dates must be at least 7 days from today."
    ) {
        Text(
            text = "Event Duration",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DurationOption(
                title = "One Day",
                selected = !draft.isMultiDayPhysicalEvent,
                onClick = { onMultiDayChanged(false) },
                modifier = Modifier.weight(1f)
            )

            DurationOption(
                title = "Multiple Days",
                selected = draft.isMultiDayPhysicalEvent,
                onClick = { onMultiDayChanged(true) },
                modifier = Modifier.weight(1f)
            )
        }

        if (draft.isMultiDayPhysicalEvent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DateSelectionField(
                    label = "Start Date",
                    selectedDateMillis = draft.physicalStartDateMillis,
                    minimumDateMillis = minimumCreatePostStartDateMillis(),
                    errorMessage = errors.physicalStartDate,
                    onDateSelected = onStartDateSelected,
                    modifier = Modifier.weight(1f)
                )

                val minimumEndDate = draft.physicalStartDateMillis?.let {
                    CreatePostDateRules.nextDayMillis(it)
                } ?: minimumCreatePostStartDateMillis()

                DateSelectionField(
                    label = "End Date",
                    selectedDateMillis = draft.physicalEndDateMillis,
                    minimumDateMillis = minimumEndDate,
                    errorMessage = errors.physicalEndDate,
                    onDateSelected = onEndDateSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            DateSelectionField(
                label = "Event Date",
                selectedDateMillis = draft.physicalStartDateMillis,
                minimumDateMillis = minimumCreatePostStartDateMillis(),
                errorMessage = errors.physicalStartDate,
                onDateSelected = onStartDateSelected
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TimeSelectionField(
                label = "Start Time",
                selectedTimeMinutes = draft.physicalStartTimeMinutes,
                errorMessage = if (draft.physicalStartTimeMinutes == null) {
                    errors.physicalTime
                } else {
                    null
                },
                onTimeSelected = { hour, minute ->
                    onStartTimeSelected(hour, minute)
                    null
                },
                modifier = Modifier.weight(1f)
            )

            val proposedEndTime = draft.physicalStartTimeMinutes?.let {
                (it + 60).coerceAtMost(23 * 60 + 59)
            }

            TimeSelectionField(
                label = "End Time",
                selectedTimeMinutes = draft.physicalEndTimeMinutes,
                dialogInitialTimeMinutes = draft.physicalEndTimeMinutes
                    ?: proposedEndTime,
                errorMessage = physicalTimeError
                    ?: if (draft.physicalStartTimeMinutes != null) {
                        errors.physicalTime
                    } else {
                        null
                    },
                onDialogOpened = onTimeDialogOpened,
                onTimeSelected = onEndTimeSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }

    CreateSectionCard(
        title = "Event Location",
        subtitle = "Select a real location from Geoapify so its address and coordinates can be saved later."
    ) {
        LocationAutocompleteField(
            query = draft.physicalLocationQuery,
            selectedLocation = draft.physicalLocation,
            suggestions = locationSuggestions,
            isSearching = isLocationSearching,
            searchError = locationSearchError,
            validationError = errors.physicalLocation,
            onQueryChanged = onLocationSearchChanged,
            onLocationSelected = onLocationSelected,
            onClearLocation = onClearLocation
        )

        OutlinedTextField(
            value = draft.meetingPoint,
            onValueChange = onMeetingPointChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Meeting Point (Optional)") },
            placeholder = { Text("Example: Main entrance beside the registration desk") },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (showVolunteerCapacity) {
        CreateSectionCard(
            title = "Volunteer Requirement",
            subtitle = "Set the total number of volunteers needed. You will distribute this number across roles in Step 2."
        ) {
            VolunteerCapacityField(
                value = draft.physicalVolunteerCapacity,
                onValueChanged = onVolunteerCapacityChanged,
                errorMessage = errors.physicalCapacity
            )
        }
    }
}

@Composable
private fun DurationOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            Color(0xFFE5EFE1)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (selected) CreateGreen else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = if (selected) CreateGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}
