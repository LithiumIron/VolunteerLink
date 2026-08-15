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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateSectionCard
import com.example.volunteerlink.organisation.create.components.DateSelectionField
import com.example.volunteerlink.organisation.create.components.FormError
import com.example.volunteerlink.organisation.create.components.VolunteerCapacityField
import com.example.volunteerlink.organisation.create.components.minimumCreatePostStartDateMillis
import com.example.volunteerlink.organisation.create.model.CreatePostDateRules
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostStepOneErrors
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode

@Composable
fun RemoteProjectDetailsSection(
    draft: CreatePostDraft,
    errors: CreatePostStepOneErrors,
    showVolunteerCapacity: Boolean,
    onStartDateSelected: (Long) -> Unit,
    onDueDateSelected: (Long) -> Unit,
    onVolunteerCapacityChanged: (String) -> Unit,
    onSubmissionModeChanged: (RemoteSubmissionMode) -> Unit,
    onSharedDeliverableChanged: (String) -> Unit
) {
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
                minimumDateMillis = minimumCreatePostStartDateMillis(),
                errorMessage = errors.remoteStartDate,
                onDateSelected = onStartDateSelected,
                modifier = Modifier.weight(1f)
            )

            val minimumDueDate = draft.remoteStartDateMillis?.let {
                CreatePostDateRules.nextDayMillis(it)
            } ?: minimumCreatePostStartDateMillis()

            DateSelectionField(
                label = "Due Date",
                selectedDateMillis = draft.remoteDueDateMillis,
                minimumDateMillis = minimumDueDate,
                errorMessage = errors.remoteDueDate,
                onDateSelected = onDueDateSelected,
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
                    onSubmissionModeChanged(RemoteSubmissionMode.SHARED_TEAM)
                }
            )

            SubmissionModeOption(
                title = "Individual Deliverables",
                description = "Each remote volunteer submits their own required output. Role-specific requirements are set later.",
                selected = draft.remoteSubmissionMode == RemoteSubmissionMode.INDIVIDUAL,
                onClick = {
                    onSubmissionModeChanged(RemoteSubmissionMode.INDIVIDUAL)
                }
            )

            FormError(errors.remoteSubmissionMode)
        }

        if (draft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM) {
            OutlinedTextField(
                value = draft.sharedDeliverable,
                onValueChange = onSharedDeliverableChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Shared Project Deliverable") },
                placeholder = { Text("Example: One final awareness campaign report") },
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
                onValueChanged = onVolunteerCapacityChanged,
                errorMessage = errors.remoteCapacity
            )
        }
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
        color = if (selected) Color(0xFFE5EFE1) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) CreateGreen else MaterialTheme.colorScheme.outlineVariant
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
                color = if (selected) CreateGreen else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
