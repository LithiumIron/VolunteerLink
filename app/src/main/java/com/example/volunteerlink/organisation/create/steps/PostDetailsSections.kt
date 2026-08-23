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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateSectionCard
import com.example.volunteerlink.organisation.create.components.DateSelectionField
import com.example.volunteerlink.organisation.create.components.FormError
import com.example.volunteerlink.organisation.create.components.LocationAutocompleteField
import com.example.volunteerlink.organisation.create.components.TimeSelectionField
import com.example.volunteerlink.organisation.create.components.VolunteerCapacityField
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel

/** Physical part of Step 1. */
@Composable
fun PhysicalEventDetailsSection(
    uiState: CreatePostUiState,
    viewModel: CreatePostViewModel,
    showVolunteerCapacity: Boolean
) {
    val draft = uiState.draft
    val errors = uiState.visibleErrors

    // Observe AppClock directly so changing the Supabase test date while the
    // app is already open recalculates the 7-day minimum immediately.
    val clockState by AppClock.state.collectAsStateWithLifecycle()
    val minimumStartDateMillis = remember(clockState.refreshVersion) {
        CreatePostValidator.minimumStartDateMillis()
    }

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
                onClick = { viewModel.updateIsMultiDay(false) },
                modifier = Modifier.weight(1f)
            )

            DurationOption(
                title = "Multiple Days",
                selected = draft.isMultiDayPhysicalEvent,
                onClick = { viewModel.updateIsMultiDay(true) },
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
                    minimumDateMillis = minimumStartDateMillis,
                    errorMessage = errors.physicalStartDate,
                    onDateSelected = viewModel::updatePhysicalStartDate,
                    modifier = Modifier.weight(1f)
                )

                val minimumEndDate = draft.physicalStartDateMillis?.let {
                    CreatePostValidator.nextDayMillis(it)
                } ?: minimumStartDateMillis

                DateSelectionField(
                    label = "End Date",
                    selectedDateMillis = draft.physicalEndDateMillis,
                    minimumDateMillis = minimumEndDate,
                    errorMessage = errors.physicalEndDate,
                    onDateSelected = viewModel::updatePhysicalEndDate,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            DateSelectionField(
                label = "Event Date",
                selectedDateMillis = draft.physicalStartDateMillis,
                minimumDateMillis = minimumStartDateMillis,
                errorMessage = errors.physicalStartDate,
                onDateSelected = viewModel::updatePhysicalStartDate
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
                    viewModel.updatePhysicalStartTime(hour, minute)
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
                errorMessage = uiState.physicalTimeError
                    ?: if (draft.physicalStartTimeMinutes != null) {
                        errors.physicalTime
                    } else {
                        null
                    },
                onDialogOpened = viewModel::clearPhysicalTimeError,
                onTimeSelected = viewModel::updatePhysicalEndTime,
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
            suggestions = uiState.locationSuggestions,
            isSearching = uiState.isLocationSearching,
            searchError = uiState.locationSearchError,
            validationError = errors.physicalLocation,
            onQueryChanged = viewModel::onLocationQueryChanged,
            onLocationSelected = viewModel::onLocationSelected,
            onClearLocation = viewModel::clearLocation
        )

        OutlinedTextField(
            value = draft.meetingPoint,
            onValueChange = viewModel::updateMeetingPoint,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Meeting Point (Optional)") },
            placeholder = {
                Text("Example: Main entrance beside the registration desk")
            },
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
                onValueChanged = viewModel::updatePhysicalVolunteerCapacity,
                errorMessage = errors.physicalCapacity
            )
        }
    }
}

/** Remote part of Step 1. */
@Composable
fun RemoteProjectDetailsSection(
    uiState: CreatePostUiState,
    viewModel: CreatePostViewModel,
    showVolunteerCapacity: Boolean
) {
    val draft = uiState.draft
    val errors = uiState.visibleErrors

    // Remote dates use the same observable AppClock as Physical dates.
    val clockState by AppClock.state.collectAsStateWithLifecycle()
    val minimumStartDateMillis = remember(clockState.refreshVersion) {
        CreatePostValidator.minimumStartDateMillis()
    }

    CreateSectionCard(
        title = "Remote Project Timeline",
        subtitle = "Set the working period for the remote part. Start dates must be at least 7 days from today."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DateSelectionField(
                label = "Start Date",
                selectedDateMillis = draft.remoteStartDateMillis,
                minimumDateMillis = minimumStartDateMillis,
                errorMessage = errors.remoteStartDate,
                onDateSelected = viewModel::updateRemoteStartDate,
                modifier = Modifier.weight(1f)
            )

            val minimumDueDate = draft.remoteStartDateMillis?.let {
                CreatePostValidator.nextDayMillis(it)
            } ?: minimumStartDateMillis

            DateSelectionField(
                label = "Due Date",
                selectedDateMillis = draft.remoteDueDateMillis,
                minimumDateMillis = minimumDueDate,
                errorMessage = errors.remoteDueDate,
                onDateSelected = viewModel::updateRemoteDueDate,
                modifier = Modifier.weight(1f)
            )
        }
    }

    CreateSectionCard(
        title = "Remote Submission Setup",
        subtitle = "Choose how completed remote work will be submitted."
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SubmissionModeOption(
                title = "Shared Team Deliverable",
                description = "The remote team works toward one final output. A responsible role can be chosen later.",
                selected = draft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM,
                onClick = {
                    viewModel.updateRemoteSubmissionMode(
                        RemoteSubmissionMode.SHARED_TEAM
                    )
                }
            )

            SubmissionModeOption(
                title = "Individual Deliverables",
                description = "Each remote volunteer submits their own required output. Role-specific requirements are set later.",
                selected = draft.remoteSubmissionMode == RemoteSubmissionMode.INDIVIDUAL,
                onClick = {
                    viewModel.updateRemoteSubmissionMode(
                        RemoteSubmissionMode.INDIVIDUAL
                    )
                }
            )

            FormError(errors.remoteSubmissionMode)
        }

        if (draft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM) {
            OutlinedTextField(
                value = draft.sharedDeliverable,
                onValueChange = viewModel::updateSharedDeliverable,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Shared Project Deliverable") },
                placeholder = {
                    Text("Example: One final awareness campaign report")
                },
                minLines = 2,
                maxLines = 4,
                isError = errors.sharedDeliverable != null,
                shape = RoundedCornerShape(14.dp)
            )

            FormError(errors.sharedDeliverable)
        }
    }

    if (showVolunteerCapacity) {
        CreateSectionCard(
            title = "Volunteer Requirement",
            subtitle = "Set the total number of remote volunteers needed. You will distribute this number across roles in Step 2."
        ) {
            VolunteerCapacityField(
                value = draft.remoteVolunteerCapacity,
                onValueChanged = viewModel::updateRemoteVolunteerCapacity,
                errorMessage = errors.remoteCapacity
            )
        }
    }
}

/** Hybrid-only capacity block so Physical and Remote cards are not duplicated. */
@Composable
fun HybridVolunteerRequirementSection(
    uiState: CreatePostUiState,
    viewModel: CreatePostViewModel
) {
    val draft = uiState.draft
    val errors = uiState.visibleErrors

    CreateSectionCard(
        title = "Hybrid Volunteer Requirement",
        subtitle = "Set separate capacity for the Physical and Remote parts. These totals will be distributed across roles in Step 2."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VolunteerCapacityField(
                value = draft.hybridPhysicalVolunteerCapacity,
                onValueChanged = viewModel::updateHybridPhysicalVolunteerCapacity,
                label = "Physical",
                errorMessage = errors.hybridPhysicalCapacity,
                modifier = Modifier.weight(1f)
            )

            VolunteerCapacityField(
                value = draft.hybridRemoteVolunteerCapacity,
                onValueChanged = viewModel::updateHybridRemoteVolunteerCapacity,
                label = "Remote",
                errorMessage = errors.hybridRemoteCapacity,
                modifier = Modifier.weight(1f)
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
            if (selected) {
                CreateGreen
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
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
            color = if (selected) {
                CreateGreen
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun SubmissionModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            Color(0xFFE5EFE1)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                CreateGreen
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    CreateGreen
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
