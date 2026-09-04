package com.example.volunteerlink.organisation.create.steps

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Provides a reusable section used by the Create Post wizard for Review Summary Role Sections.
//
// The composables read CreatePostUiState/CreatePostDraft values and emit callbacks; they do not call Supabase
// directly.
//
// Validation messages are supplied from CreatePostViewModel/CreatePostValidator so the same business rules apply
// regardless of which UI component displays the field.
//
// Breaking large steps into section files keeps layout code readable while the ViewModel remains the single owner
// of mutable workflow state.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.ui.theme.ReviewText
import com.example.volunteerlink.ui.theme.ReviewSecondaryText
import com.example.volunteerlink.ui.theme.ReviewBorder
import com.example.volunteerlink.ui.theme.ReviewSoftSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
/**
 * Renders the review roles capacity section section used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ReviewRolesCapacitySection
 *
 * Renders the reusable Review Roles Capacity Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ReviewRolesCapacitySection(
    uiState: CreatePostUiState,
    onEdit: () -> Unit
) {
    val draft = uiState.draft
    val templatesById = uiState.roleCatalogue.associateBy {
        it.roleTemplateId
    }
    val physicalRoles = draft.selectedRoles.filter { selected ->
        templatesById[selected.roleTemplateId]?.roleMode ==
            VolunteerRoleMode.PHYSICAL
    }
    val remoteRoles = draft.selectedRoles.filter { selected ->
        templatesById[selected.roleTemplateId]?.roleMode ==
            VolunteerRoleMode.REMOTE
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ReviewSectionHeader(
            title = "Roles & Capacity",
            onEdit = onEdit
        )

        ReviewCapacityStats(
            physicalCapacity = physicalRoles.sumOf { it.capacity },
            remoteCapacity = remoteRoles.sumOf { it.capacity },
            totalCapacity = draft.selectedRoles.sumOf { it.capacity },
            totalRoleCount = draft.selectedRoles.size,
            showPhysical = physicalRoles.isNotEmpty(),
            showRemote = remoteRoles.isNotEmpty()
        )

        ReviewWhiteCard {
            if (draft.selectedRoles.isEmpty()) {
                ReviewEmptyText("No roles selected.")
            } else {
                if (physicalRoles.isNotEmpty()) {
                    ReviewRoleGroup(
                        title = "Physical",
                        roles = physicalRoles,
                        templatesById = templatesById
                    )
                }

                if (physicalRoles.isNotEmpty() && remoteRoles.isNotEmpty()) {
                    HorizontalDivider(color = ReviewBorder)
                }

                if (remoteRoles.isNotEmpty()) {
                    ReviewRoleGroup(
                        title = "Remote",
                        roles = remoteRoles,
                        templatesById = templatesById
                    )
                }
            }
        }
    }
}


@Composable
/**
 * Renders the review role settings section section used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ReviewRoleSettingsSection
 *
 * Renders the reusable Review Role Settings Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ReviewRoleSettingsSection(
    uiState: CreatePostUiState,
    onEdit: () -> Unit
) {
    val draft = uiState.draft
    val templatesById = uiState.roleCatalogue.associateBy {
        it.roleTemplateId
    }
    var expandedRoleId by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ReviewSectionHeader(
            title = "Role Settings",
            onEdit = onEdit
        )

        if (
            draft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM &&
            draft.sharedSubmissionResponsibleRoleTemplateId != null
        ) {
            val responsibleName = templatesById[
                draft.sharedSubmissionResponsibleRoleTemplateId
            ]?.roleName ?: draft.sharedSubmissionResponsibleRoleTemplateId

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ReviewSoftSurface,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Shared deliverable lead",
                        style = MaterialTheme.typography.labelMedium,
                        color = ReviewSecondaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = responsibleName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ReviewText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (draft.selectedRoles.isEmpty()) {
            ReviewWhiteCard {
                ReviewEmptyText("No role settings to review.")
            }
        } else {
            draft.selectedRoles.forEach { selectedRole ->
                val template = templatesById[selectedRole.roleTemplateId]
                val isExpanded = expandedRoleId == selectedRole.roleTemplateId

                ReviewRoleSettingsCard(
                    selectedRole = selectedRole,
                    template = template,
                    isExpanded = isExpanded,
                    onToggleExpanded = {
                        expandedRoleId = if (isExpanded) {
                            null
                        } else {
                            selectedRole.roleTemplateId
                        }
                    }
                )
            }
        }
    }
}


@Composable
/**
 * Renders the UI represented by review capacity stats for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ReviewCapacityStats
 *
 * Handles the Compose/UI responsibility for review capacity stats.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun ReviewCapacityStats(
    physicalCapacity: Int,
    remoteCapacity: Int,
    totalCapacity: Int,
    totalRoleCount: Int,
    showPhysical: Boolean,
    showRemote: Boolean
) {
    ReviewWhiteCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (showPhysical && showRemote) {
                ReviewStat(
                    value = physicalCapacity.toString(),
                    label = "Physical"
                )
                ReviewStat(
                    value = remoteCapacity.toString(),
                    label = "Remote"
                )
                ReviewStat(
                    value = totalCapacity.toString(),
                    label = "Total"
                )
            } else {
                ReviewStat(
                    value = totalRoleCount.toString(),
                    label = if (totalRoleCount == 1) "Role" else "Roles"
                )
                ReviewStat(
                    value = totalCapacity.toString(),
                    label = "Total slots"
                )
            }
        }
    }
}


@Composable
/**
 * Renders the UI represented by review role group for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ReviewRoleGroup
 *
 * Handles the Compose/UI responsibility for review role group.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun ReviewRoleGroup(
    title: String,
    roles: List<SelectedRoleDraft>,
    templatesById: Map<String, CreateRoleTemplate>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = CreateGreen,
            fontWeight = FontWeight.Bold
        )

        roles.forEach { selected ->
            ReviewRoleCapacityRow(
                roleName = templatesById[selected.roleTemplateId]
                    ?.roleName
                    ?: selected.roleTemplateId,
                capacity = selected.capacity,
                level = templatesById[selected.roleTemplateId]
                    ?.defaultLevel
                    ?.displayName
            )
        }
    }
}


@Composable
/**
 * Renders the review role capacity row row used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ReviewRoleCapacityRow
 *
 * Renders the reusable Review Role Capacity Row portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ReviewRoleCapacityRow(
    roleName: String,
    capacity: Int,
    level: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = roleName,
                style = MaterialTheme.typography.bodyMedium,
                color = ReviewText,
                fontWeight = FontWeight.SemiBold
            )
            level?.let { levelText ->
                Text(
                    text = levelText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ReviewSecondaryText
                )
            }
        }

        Text(
            text = capacity.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = CreateGreen,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
/**
 * Renders the review role settings card card used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ReviewRoleSettingsCard
 *
 * Renders the reusable Review Role Settings Card portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ReviewRoleSettingsCard(
    selectedRole: SelectedRoleDraft,
    template: CreateRoleTemplate?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val skillsById = template?.skillsPractised
        ?.associateBy { it.skillId }
        .orEmpty()
    val responsibilities = selectedRole.responsibilities
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val questions = selectedRole.screeningQuestions
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val requiredCount = selectedRole.requiredSkillExperience.size

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ReviewBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = template?.roleName ?: selectedRole.roleTemplateId,
                        style = MaterialTheme.typography.titleMedium,
                        color = ReviewText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append(template?.defaultLevel?.displayName ?: "Unknown level")
                            append(" · ")
                            append(
                                selectedRole.applicationMethod?.displayName
                                    ?: "Application method not set"
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ReviewSecondaryText
                    )
                }

                ReviewChevron(isExpanded = isExpanded)
            }

            if (!selectedRole.isConfigured) {
                Text(
                    text = "Needs review",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = buildString {
                    append("${selectedRole.practisedSkillIds.size} skills")
                    append(" · ${responsibilities.size} responsibilities")
                    if (requiredCount > 0) {
                        append(" · $requiredCount required")
                    }
                    if (questions.isNotEmpty()) {
                        append(" · ${questions.size} questions")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = ReviewSecondaryText
            )

            if (isExpanded) {
                HorizontalDivider(color = ReviewBorder)

                if (selectedRole.practisedSkillIds.isNotEmpty()) {
                    ReviewDetailHeading("Skills")
                    selectedRole.practisedSkillIds.forEach { skillId ->
                        val experience = selectedRole.requiredSkillExperience[skillId]
                        ReviewSkillRow(
                            skillName = skillsById[skillId]?.name ?: skillId,
                            requiredExperience = experience
                        )
                    }
                }

                if (responsibilities.isNotEmpty()) {
                    ReviewDetailHeading("Responsibilities")
                    responsibilities.forEach { text ->
                        ReviewBulletText(text)
                    }
                }

                if (questions.isNotEmpty()) {
                    ReviewDetailHeading("Screening Questions")
                    questions.forEachIndexed { index, text ->
                        ReviewNumberedText(
                            number = index + 1,
                            text = text
                        )
                    }
                }

                selectedRole.roleNotes
                    .takeIf { it.isNotBlank() }
                    ?.let { notes ->
                        ReviewDetailHeading("Role Notes")
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ReviewText
                        )
                    }

                selectedRole.individualSubmissionRequirement
                    .takeIf { it.isNotBlank() }
                    ?.let { requirement ->
                        ReviewDetailHeading("Individual Deliverable")
                        Text(
                            text = requirement,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ReviewText
                        )
                    }
            }
        }
    }
}


@Composable
/**
 * Renders the review skill row row used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ReviewSkillRow
 *
 * Renders the reusable Review Skill Row portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ReviewSkillRow(
    skillName: String,
    requiredExperience: Int?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = skillName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = ReviewText,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (requiredExperience == null) {
                "Practised"
            } else {
                "Required · $requiredExperience exp."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (requiredExperience == null) {
                ReviewSecondaryText
            } else {
                CreateGreen
            },
            fontWeight = if (requiredExperience == null) {
                FontWeight.Normal
            } else {
                FontWeight.SemiBold
            }
        )
    }
}
