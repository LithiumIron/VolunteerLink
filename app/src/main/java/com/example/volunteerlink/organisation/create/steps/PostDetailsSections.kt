package com.example.volunteerlink.organisation.create.steps

// FILE OVERVIEW:
/*
 * PostDetailsSections contains presentation code for the organisation Create/Edit Post flow.
 * It focuses on rendering state and forwarding user actions through callbacks/ViewModels,
 * keeping database access and business rules outside the composables where possible.
 */


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
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateSectionCard
import com.example.volunteerlink.organisation.create.components.DateSelectionField
import com.example.volunteerlink.organisation.create.components.EditRestrictionNotice
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
    val editPolicy = uiState.editPolicy
    val canEditPhysicalDates = uiState.impactWeaveDraftId == null &&
        (!uiState.isExistingPostEdit || editPolicy?.canEditPhysicalDates != false)
    val canEditPhysicalCore = uiState.impactWeaveDraftId == null &&
        (!uiState.isExistingPostEdit || editPolicy?.canEditPhysicalCore != false)
    val canEditMeetingPoint = !uiState.isExistingPostEdit || editPolicy?.canEditPhysicalMeetingPoint != false
    val canEditPhysicalCapacity = !uiState.isExistingPostEdit || editPolicy?.canEditPhysicalCapacity != false

    // Observe AppClock directly so changing the Supabase test date while the
    // app is already open recalculates the 7-day minimum immediately.
    val clockState by AppClock.state.collectAsStateWithLifecycle()
    val minimumStartDateMillis = remember(clockState.refreshVersion) {
        CreatePostValidator.minimumStartDateMillis()
    }
    val physicalDraftDateAttention = if (
        uiState.isExistingPostEdit && editPolicy?.postStatus == "DRAFT"
    ) {
        CreatePostValidator.draftStartDateAttention(draft.physicalStartDateMillis)
    } else {
        null
    }
    val physicalStartDateError = when {
        physicalDraftDateAttention != null -> physicalDraftDateAttention
        uiState.isExistingPostEdit -> errors.physicalStartDate
        draft.physicalStartDateMillis == null -> errors.physicalStartDate
        else -> CreatePostValidator.minimumLeadTimeError(draft.physicalStartDateMillis)
    }

    CreateSectionCard(
        title = "Event Schedule",
        subtitle = if (uiState.impactWeaveDraftId != null) {
            "Final schedule from Impact Weave. Reschedule from the partnership plan before entering Create Post."
        } else if (uiState.isExistingPostEdit && !canEditPhysicalDates) {
            "Published event dates are final and cannot be changed."
        } else {
            "Choose when the physical event will take place. Start dates must be at least 7 days from today."
        }
    ) {
        if (physicalDraftDateAttention != null) {
            EditRestrictionNotice(
                title = "Start date needs attention",
                message = physicalDraftDateAttention
            )
        }
        if (uiState.impactWeaveDraftId != null) {
            EditRestrictionNotice(
                title = "Final partnership schedule",
                message = "Dates and times are locked to the schedule accepted by partner organisations."
            )
        } else if (uiState.isExistingPostEdit && !canEditPhysicalDates) {
            EditRestrictionNotice(
                title = "Event dates locked",
                message = "Physical and Hybrid event dates become final once the post is published."
            )
        }
        if (uiState.isExistingPostEdit && !canEditPhysicalCore) {
            EditRestrictionNotice(
                title = "Time editing locked",
                message = "Event time can no longer be changed because volunteers or the event lifecycle already depend on it."
            )
        }
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
                enabled = canEditPhysicalDates,
                modifier = Modifier.weight(1f)
            )

            DurationOption(
                title = "Multiple Days",
                selected = draft.isMultiDayPhysicalEvent,
                onClick = { viewModel.updateIsMultiDay(true) },
                enabled = canEditPhysicalDates,
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
                    errorMessage = physicalStartDateError,
                    onDateSelected = viewModel::updatePhysicalStartDate,
                    enabled = canEditPhysicalDates,
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
                    enabled = canEditPhysicalDates,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            DateSelectionField(
                label = "Event Date",
                selectedDateMillis = draft.physicalStartDateMillis,
                minimumDateMillis = minimumStartDateMillis,
                errorMessage = physicalStartDateError,
                onDateSelected = viewModel::updatePhysicalStartDate,
                enabled = canEditPhysicalDates
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
                enabled = canEditPhysicalCore,
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
                enabled = canEditPhysicalCore,
                modifier = Modifier.weight(1f)
            )
        }
    }

    CreateSectionCard(
        title = "Event Location",
        subtitle = if (uiState.impactWeaveDraftId != null) {
            "Confirmed partnership venue. This location is locked."
        } else {
            "Search broadly for an area, venue, building, street or address. Select a Geoapify result so its coordinates can be saved."
        }
    ) {
        if (uiState.isExistingPostEdit && (!canEditPhysicalCore || !canEditMeetingPoint)) {
            EditRestrictionNotice(
                title = "Location editing limited",
                message = if (!canEditPhysicalCore && canEditMeetingPoint) {
                    "Event location is fixed. The meeting point can still be updated before the event starts."
                } else {
                    "Event location and meeting point can no longer be changed."
                }
            )
        }
        LocationAutocompleteField(
            query = draft.physicalLocationQuery,
            selectedLocation = draft.physicalLocation,
            suggestions = uiState.locationSuggestions,
            isSearching = uiState.isLocationSearching,
            searchError = uiState.locationSearchError,
            validationError = errors.physicalLocation,
            placeholder = "Search an area, venue or address",
            onQueryChanged = viewModel::onLocationQueryChanged,
            onLocationSelected = viewModel::onLocationSelected,
            onClearLocation = viewModel::clearLocation,
            enabled = canEditPhysicalCore
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
            enabled = canEditMeetingPoint,
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
                errorMessage = errors.physicalCapacity,
                enabled = canEditPhysicalCapacity
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
    val editPolicy = uiState.editPolicy
    val canEditRemoteStart = !uiState.isExistingPostEdit || editPolicy?.canEditRemoteStart != false
    val canEditRemoteDue = !uiState.isExistingPostEdit || editPolicy?.canEditRemoteDueDate != false
    val canEditRemoteSetup = !uiState.isExistingPostEdit || editPolicy?.canEditRemoteSubmissionSetup != false
    val canEditRemoteCapacity = !uiState.isExistingPostEdit || editPolicy?.canEditRemoteCapacity != false
    val remoteDueExtensionOnly = uiState.isExistingPostEdit &&
        canEditRemoteDue &&
        editPolicy?.minimumRemoteDueDateMillis != null

    // Remote dates use the same observable AppClock as Physical dates.
    val clockState by AppClock.state.collectAsStateWithLifecycle()
    val minimumStartDateMillis = remember(clockState.refreshVersion) {
        CreatePostValidator.minimumStartDateMillis()
    }
    val remoteDraftDateAttention = if (
        uiState.isExistingPostEdit && editPolicy?.postStatus == "DRAFT"
    ) {
        CreatePostValidator.draftStartDateAttention(draft.remoteStartDateMillis)
    } else {
        null
    }
    val remoteStartDateError = when {
        remoteDraftDateAttention != null -> remoteDraftDateAttention
        uiState.isExistingPostEdit -> errors.remoteStartDate
        draft.remoteStartDateMillis == null -> errors.remoteStartDate
        else -> CreatePostValidator.minimumLeadTimeError(draft.remoteStartDateMillis)
    }

    CreateSectionCard(
        title = "Remote Project Timeline",
        subtitle = if (
            uiState.isExistingPostEdit &&
            (!canEditRemoteStart || !canEditRemoteDue || remoteDueExtensionOnly)
        ) {
            "This existing post has timeline limits based on volunteer and submission history."
        } else {
            "Set the working period for the remote part. Start dates must be at least 7 days from today."
        }
    ) {
        if (remoteDraftDateAttention != null) {
            EditRestrictionNotice(
                title = "Start date needs attention",
                message = remoteDraftDateAttention
            )
        }
        if (
            uiState.isExistingPostEdit &&
            (!canEditRemoteStart || !canEditRemoteDue || remoteDueExtensionOnly)
        ) {
            EditRestrictionNotice(
                title = "Timeline editing limited",
                message = when {
                    !canEditRemoteDue -> editPolicy?.remoteDueDateLockedReason
                        ?: "The Remote due date is locked."
                    !canEditRemoteStart && remoteDueExtensionOnly ->
                        "Start date is locked. The due date may only be extended; it cannot be shortened after volunteers have joined or Individual work has been submitted."
                    !canEditRemoteStart ->
                        "Start date is locked. The due date can still be changed where allowed."
                    remoteDueExtensionOnly ->
                        "The due date may only be extended; it cannot be shortened after volunteers have joined or Individual work has been submitted."
                    else -> "Some timeline dates are restricted."
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DateSelectionField(
                label = "Start Date",
                selectedDateMillis = draft.remoteStartDateMillis,
                minimumDateMillis = minimumStartDateMillis,
                errorMessage = remoteStartDateError,
                onDateSelected = viewModel::updateRemoteStartDate,
                enabled = canEditRemoteStart,
                modifier = Modifier.weight(1f)
            )

            val minimumDueDateFromStart = draft.remoteStartDateMillis?.let {
                CreatePostValidator.nextDayMillis(it)
            } ?: minimumStartDateMillis
            val minimumDueDate = if (uiState.isExistingPostEdit) {
                maxOf(
                    minimumDueDateFromStart,
                    editPolicy?.minimumRemoteDueDateMillis ?: minimumDueDateFromStart
                )
            } else {
                minimumDueDateFromStart
            }

            DateSelectionField(
                label = "Due Date",
                selectedDateMillis = draft.remoteDueDateMillis,
                minimumDateMillis = minimumDueDate,
                errorMessage = errors.remoteDueDate,
                onDateSelected = viewModel::updateRemoteDueDate,
                enabled = canEditRemoteDue,
                modifier = Modifier.weight(1f)
            )
        }
    }

    CreateSectionCard(
        title = "Remote Submission Setup",
        subtitle = "Choose how completed remote work will be submitted."
    ) {
        if (uiState.isExistingPostEdit && !canEditRemoteSetup) {
            EditRestrictionNotice(
                title = "Submission setup locked",
                message = "Existing applicants, volunteers or submitted work already depend on this setup."
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SubmissionModeOption(
                title = "Shared Team Deliverable",
                description = "The remote team works toward one final output. A responsible role can be chosen later.",
                selected = draft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM,
                enabled = canEditRemoteSetup,
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
                enabled = canEditRemoteSetup,
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
                enabled = canEditRemoteSetup,
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
                errorMessage = errors.remoteCapacity,
                enabled = canEditRemoteCapacity
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
    val editPolicy = uiState.editPolicy
    val canEditPhysicalCapacity = !uiState.isExistingPostEdit || editPolicy?.canEditPhysicalCapacity != false
    val canEditRemoteCapacity = !uiState.isExistingPostEdit || editPolicy?.canEditRemoteCapacity != false

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
                enabled = canEditPhysicalCapacity,
                modifier = Modifier.weight(1f)
            )

            VolunteerCapacityField(
                value = draft.hybridRemoteVolunteerCapacity,
                onValueChanged = viewModel::updateHybridRemoteVolunteerCapacity,
                label = "Remote",
                errorMessage = errors.hybridRemoteCapacity,
                enabled = canEditRemoteCapacity,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
/**
 * Renders the UI represented by duration option for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun DurationOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = when {
            !enabled -> Color(0xFFF1F2F0)
            selected -> Color(0xFFE5EFE1)
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            when {
                !enabled -> Color(0xFFC7CBC5)
                selected -> CreateGreen
                else -> MaterialTheme.colorScheme.outlineVariant
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
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                selected -> CreateGreen
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
/**
 * Renders the UI represented by submission mode option for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun SubmissionModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = when {
            !enabled -> Color(0xFFF1F2F0)
            selected -> Color(0xFFE5EFE1)
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            when {
                !enabled -> Color(0xFFC7CBC5)
                selected -> CreateGreen
                else -> MaterialTheme.colorScheme.outlineVariant
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
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    selected -> CreateGreen
                    else -> MaterialTheme.colorScheme.onSurface
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
