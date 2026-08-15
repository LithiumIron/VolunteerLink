package com.example.volunteerlink.organisation.create.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.organisation.create.components.CreateSectionCard
import com.example.volunteerlink.organisation.create.components.VolunteerCapacityField
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostStepOneErrors

/** One Hybrid-only capacity section prevents duplicate requirement cards. */
@Composable
fun HybridVolunteerRequirementSection(
    draft: CreatePostDraft,
    errors: CreatePostStepOneErrors,
    onPhysicalCapacityChanged: (String) -> Unit,
    onRemoteCapacityChanged: (String) -> Unit
) {
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
                onValueChanged = onPhysicalCapacityChanged,
                label = "Physical",
                errorMessage = errors.hybridPhysicalCapacity,
                modifier = Modifier.weight(1f)
            )

            VolunteerCapacityField(
                value = draft.hybridRemoteVolunteerCapacity,
                onValueChanged = onRemoteCapacityChanged,
                label = "Remote",
                errorMessage = errors.hybridRemoteCapacity,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
