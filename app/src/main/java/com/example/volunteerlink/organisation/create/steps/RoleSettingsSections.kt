package com.example.volunteerlink.organisation.create.steps

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Provides a reusable section used by the Create Post wizard for Role Settings Sections.
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


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.ui.theme.CreateLightGreen
import com.example.volunteerlink.ui.theme.CreateCardBackground
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.ui.theme.CreateLightGreen
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreateRoleSkill
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.RoleApplicationMethod
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode

import com.example.volunteerlink.ui.theme.RoleSettingsBorder
import com.example.volunteerlink.ui.theme.RoleSettingsOrange
import com.example.volunteerlink.ui.theme.RoleSettingsOrangeBackground
import com.example.volunteerlink.ui.theme.RoleSettingsPurple
import com.example.volunteerlink.ui.theme.RoleSettingsPurpleBackground

@Composable
/**
 * Renders the role information section section used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — RoleInformationSection
 *
 * Renders the reusable Role Information Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun RoleInformationSection(
    template: CreateRoleTemplate
) {
    RoleSettingsSectionCard(
        iconRes = roleAreaIconRes(template.roleArea),
        title = "About this role",
        subtitle = "VolunteerLink's fixed role information loaded from the role catalogue."
    ) {
        Text(
            text = template.description,
            style = MaterialTheme.typography.bodyMedium
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF3F6F1)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Primary Skill Path",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = template.skillPathName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CreateGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
/**
 * Renders the skills practised section section used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — SkillsPractisedSection
 *
 * Renders the reusable Skills Practised Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun SkillsPractisedSection(
    template: CreateRoleTemplate,
    selectedRole: SelectedRoleDraft,
    enabled: Boolean = true,
    onToggleSkill: (String) -> Unit
) {
    RoleSettingsSectionCard(
        iconRes = R.drawable.skill,
        title = "Skills Practised",
        subtitle = "Tick 2–4 skills volunteers will actually practise. Recommended skills are VolunteerLink's starting suggestion."
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFF8E8)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Recommended by VolunteerLink",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7B5B00)
                )

                Text(
                    text = "The most relevant skills are pre-selected. You can adjust them to match this opportunity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        template.skillsPractised.forEach { skill ->
            val isSelected = skill.skillId in selectedRole.practisedSkillIds
            val isRecommended = template.recommendedSkills.any {
                it.skillId == skill.skillId
            }
            val isRequired =
                skill.skillId in selectedRole.requiredSkillExperience

            // Selected skills can be removed only above the minimum of two.
            // Unselected skills can be added only below the maximum of four.
            // Required skills stay locked until their requirement is removed.
            val canToggle = enabled && when {
                isRequired -> false
                isSelected -> selectedRole.practisedSkillIds.size > 2
                else -> selectedRole.practisedSkillIds.size < 4
            }

            PractisedSkillRow(
                skill = skill,
                checked = isSelected,
                recommended = isRecommended,
                lockedByRequirement = isRequired,
                enabled = canToggle,
                onClick = {
                    onToggleSkill(skill.skillId)
                }
            )
        }

        Text(
            text = "${selectedRole.practisedSkillIds.size} selected · Minimum 2, maximum 4 skills.",
            style = MaterialTheme.typography.bodySmall,
            color = CreateGreen,
            fontWeight = FontWeight.SemiBold
        )

        if (selectedRole.requiredSkillExperience.isNotEmpty()) {
            Text(
                text = "Grey tick = Required. Change it to Not required below before removing it from Skills Practised.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
/**
 * Renders the required skills section section used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — RequiredSkillsSection
 *
 * Renders the reusable Required Skills Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun RequiredSkillsSection(
    template: CreateRoleTemplate,
    selectedRole: SelectedRoleDraft,
    enabled: Boolean = true,
    onToggleRequiredSkill: (String) -> Unit,
    onIncreaseExperience: (String) -> Unit,
    onDecreaseExperience: (String) -> Unit
) {
    RoleSettingsSectionCard(
        iconRes = R.drawable.skill,
        title = "Skill Requirements",
        subtitle = if (template.defaultLevel == VolunteerRoleLevel.BEGINNER) {
            "Beginner roles stay open to volunteers without previous verified skill experience."
        } else {
            "Required skills are also practised skills. Each requirement can ask for 1–8 verified experiences."
        }
    ) {
        if (template.defaultLevel == VolunteerRoleLevel.BEGINNER) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CreateLightGreen
            ) {
                Text(
                    text = "None · Beginner roles cannot require previous verified skill experience.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = CreateGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF3F6F1)
            ) {
                Text(
                    text = "1 verified experience = 1 completed and verified volunteer role that practised that skill.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            template.skillsPractised.forEach { skill ->
                val isPractised =
                    skill.skillId in selectedRole.practisedSkillIds
                val minimumExperience =
                    selectedRole.requiredSkillExperience[skill.skillId]
                val isRequired = minimumExperience != null
                val canToggleRequirement =
                    isPractised ||
                            selectedRole.practisedSkillIds.size < 4

                RequiredSkillCard(
                    skill = skill,
                    isPractised = isPractised,
                    isRequired = isRequired,
                    minimumExperience = minimumExperience ?: 1,
                    canToggleRequirement = canToggleRequirement,
                    enabled = enabled,
                    onToggleRequired = {
                        onToggleRequiredSkill(skill.skillId)
                    },
                    onIncreaseExperience = {
                        onIncreaseExperience(skill.skillId)
                    },
                    onDecreaseExperience = {
                        onDecreaseExperience(skill.skillId)
                    }
                )
            }

            Text(
                text = if (selectedRole.requiredSkillExperience.isEmpty()) {
                    "No previous skill experience required."
                } else {
                    "${selectedRole.requiredSkillExperience.size} required skill${if (selectedRole.requiredSkillExperience.size == 1) "" else "s"}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
/**
 * Renders the responsibilities section section used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ResponsibilitiesSection
 *
 * Renders the reusable Responsibilities Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ResponsibilitiesSection(
    responsibilities: List<String>,
    enabled: Boolean = true,
    onAdd: () -> Unit,
    onUpdate: (Int, String) -> Unit,
    onRemove: (Int) -> Unit
) {
    RoleSettingsSectionCard(
        iconRes = R.drawable.responsibility,
        title = "Responsibilities *",
        subtitle = "Required · Add at least one specific duty for this opportunity."
    ) {
        responsibilities.forEachIndexed { index, responsibility ->
            OutlinedTextField(
                value = responsibility,
                onValueChange = { text ->
                    onUpdate(index, text)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                label = {
                    Text(text = "Responsibility ${index + 1}")
                },
                placeholder = {
                    Text(text = "Example: Welcome arriving participants")
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            onRemove(index)
                        },
                        enabled = enabled
                    ) {
                        Image(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = "Remove responsibility",
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.error
                            )
                        )
                    }
                },
                supportingText = {
                    Text("${responsibility.length} / 160")
                },
                minLines = 1,
                maxLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedButton(
            onClick = onAdd,
            enabled = enabled && responsibilities.lastOrNull()?.isNotBlank() != false,
            shape = RoundedCornerShape(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(CreateGreen)
            )

            Spacer(modifier = Modifier.width(7.dp))
            Text(text = "Add Responsibility")
        }
    }
}

@Composable
/**
 * Renders the applicant method section section used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ApplicantMethodSection
 *
 * Renders the reusable Applicant Method Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ApplicantMethodSection(
    template: CreateRoleTemplate,
    selectedRole: SelectedRoleDraft,
    methodEnabled: Boolean = true,
    questionsEnabled: Boolean = true,
    onMethodChanged: (RoleApplicationMethod) -> Unit,
    onAddQuestion: () -> Unit,
    onUpdateQuestion: (Int, String) -> Unit,
    onRemoveQuestion: (Int) -> Unit
) {
    val method = selectedRole.applicationMethod
    val recommendedMethod = when (template.defaultLevel) {
        VolunteerRoleLevel.BEGINNER ->
            RoleApplicationMethod.INSTANT_JOIN

        VolunteerRoleLevel.INTERMEDIATE,
        VolunteerRoleLevel.ADVANCED ->
            RoleApplicationMethod.REVIEW_APPLICANTS
    }

    RoleSettingsSectionCard(
        iconRes = R.drawable.review_applicants,
        title = "Application Method",
        subtitle = if (methodEnabled) {
            "Choose how volunteers join this role. VolunteerLink preselects a recommendation, but you can change it."
        } else {
            "The current application method is locked for this existing role."
        }
    ) {
        ApplicationMethodOption(
            method = RoleApplicationMethod.INSTANT_JOIN,
            description = "Volunteers join immediately while places are available.",
            selected = method == RoleApplicationMethod.INSTANT_JOIN,
            recommended = recommendedMethod == RoleApplicationMethod.INSTANT_JOIN,
            enabled = methodEnabled,
            onClick = {
                onMethodChanged(RoleApplicationMethod.INSTANT_JOIN)
            }
        )

        ApplicationMethodOption(
            method = RoleApplicationMethod.REVIEW_APPLICANTS,
            description = "Review volunteers before accepting them into this role.",
            selected = method == RoleApplicationMethod.REVIEW_APPLICANTS,
            recommended = recommendedMethod == RoleApplicationMethod.REVIEW_APPLICANTS,
            enabled = methodEnabled,
            onClick = {
                onMethodChanged(RoleApplicationMethod.REVIEW_APPLICANTS)
            }
        )

        Text(
            text = when (template.defaultLevel) {
                VolunteerRoleLevel.BEGINNER ->
                    "Instant Join is recommended for Beginner roles when you want a faster, low-friction sign-up."

                VolunteerRoleLevel.INTERMEDIATE ->
                    "Review Applicants is recommended for Intermediate roles, but Instant Join is still available when speed matters more."

                VolunteerRoleLevel.ADVANCED ->
                    "Review Applicants is recommended for Advanced roles because they usually carry more responsibility, but you can still choose Instant Join."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (method == RoleApplicationMethod.REVIEW_APPLICANTS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Applicant Questions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (questionsEnabled) {
                            "Optional · Up to 3 short screening questions."
                        } else {
                            "Locked · Previous application answers depend on these questions."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (questionsEnabled) {
                        "${selectedRole.screeningQuestions.size} / 3"
                    } else {
                        "Locked"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (questionsEnabled) {
                        CreateGreen
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            selectedRole.screeningQuestions.forEachIndexed { index, question ->
                OutlinedTextField(
                    value = question,
                    onValueChange = { text ->
                        onUpdateQuestion(index, text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = questionsEnabled,
                    label = {
                        Text(text = "Question ${index + 1}")
                    },
                    placeholder = {
                        Text(text = "Example: Briefly describe any relevant experience.")
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onRemoveQuestion(index)
                            },
                            enabled = questionsEnabled
                        ) {
                            Image(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = "Remove applicant question",
                                modifier = Modifier.size(20.dp),
                                colorFilter = ColorFilter.tint(
                                    if (questionsEnabled) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    }
                                )
                            )
                        }
                    },
                    supportingText = {
                        Text("${question.length} / 180")
                    },
                    minLines = 1,
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedButton(
                onClick = onAddQuestion,
                enabled = questionsEnabled &&
                        selectedRole.screeningQuestions.size < 3 &&
                        selectedRole.screeningQuestions.lastOrNull()?.isNotBlank() != false,
                shape = RoundedCornerShape(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(
                        if (questionsEnabled) {
                            CreateGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                )

                Spacer(modifier = Modifier.width(7.dp))
                Text(text = "Add Question")
            }
        }
    }
}

@Composable
/**
 * Renders the UI represented by application method option for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ApplicationMethodOption
 *
 * Handles the Compose/UI responsibility for application method option.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun ApplicationMethodOption(
    method: RoleApplicationMethod,
    description: String,
    selected: Boolean,
    recommended: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = when {
                !enabled -> Color(0xFFF1F2F0)
                selected -> CreateLightGreen
                else -> CreateCardBackground
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                !enabled -> Color(0xFFC7CBC5)
                selected -> CreateGreen
                else -> RoleSettingsBorder
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CreateGreen
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = method.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) {
                            CreateGreen
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.Bold
                    )

                    if (recommended) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFE8F3E3)
                        ) {
                            Text(
                                text = "Recommended",
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 3.dp
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = CreateGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
/**
 * Renders the role submission and notes section section used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — RoleSubmissionAndNotesSection
 *
 * Renders the reusable Role Submission And Notes Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun RoleSubmissionAndNotesSection(
    draft: CreatePostDraft,
    template: CreateRoleTemplate,
    selectedRole: SelectedRoleDraft,
    notesEnabled: Boolean = true,
    individualDeliverableEnabled: Boolean = true,
    onRoleNotesChanged: (String) -> Unit,
    onIndividualSubmissionRequirementChanged: (String) -> Unit
) {
    when (template.roleMode) {
        VolunteerRoleMode.PHYSICAL -> {
            RoleSettingsSectionCard(
                iconRes = R.drawable.instructions,
                title = "Role Notes",
                subtitle = "Optional · Add special information not already covered by responsibilities or event details."
            ) {
                OutlinedTextField(
                    value = selectedRole.roleNotes,
                    onValueChange = onRoleNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = notesEnabled,
                    placeholder = {
                        Text("Example: This role will use the QR scanner provided at Counter B.")
                    },
                    supportingText = {
                        Text("${selectedRole.roleNotes.length} / 400")
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        VolunteerRoleMode.REMOTE -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (draft.remoteSubmissionMode) {
                    RemoteSubmissionMode.SHARED_TEAM -> {
                        RoleSettingsSectionCard(
                            iconRes = R.drawable.remote_project,
                            title = "Shared Team Deliverable",
                            subtitle = "No separate output is required from each volunteer in this role."
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = CreateLightGreen
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Team's final deliverable",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = draft.sharedDeliverable,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CreateGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = "Use Responsibilities above to explain this role's contribution. The responsible Remote role is selected on the Step 3 overview.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    RemoteSubmissionMode.INDIVIDUAL -> {
                        RoleSettingsSectionCard(
                            iconRes = R.drawable.instructions,
                            title = "Individual Deliverable *",
                            subtitle = "Required · Describe what EACH volunteer assigned to this role must submit."
                        ) {
                            OutlinedTextField(
                                value = selectedRole.individualSubmissionRequirement,
                                onValueChange = onIndividualSubmissionRequirementChanged,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = individualDeliverableEnabled,
                                placeholder = {
                                    Text("Example: Each volunteer submits 2 final poster designs in PNG format.")
                                },
                                supportingText = {
                                    Text(
                                        "${selectedRole.individualSubmissionRequirement.length} / 500"
                                    )
                                },
                                minLines = 3,
                                maxLines = 5,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    null -> {
                        RoleSettingsSectionCard(
                            iconRes = R.drawable.remote_project,
                            title = "Remote Submission",
                            subtitle = "Return to Step 1 and choose how Remote work will be submitted."
                        ) {
                            Text(
                                text = "Remote submission setup is incomplete.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RoleSettingsOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                RoleSettingsSectionCard(
                    iconRes = R.drawable.instructions,
                    title = "Role Notes",
                    subtitle = "Optional · Add extra instructions or working arrangements not already covered above."
                ) {
                    OutlinedTextField(
                        value = selectedRole.roleNotes,
                        onValueChange = onRoleNotesChanged,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = notesEnabled,
                        placeholder = {
                            Text("Example: Canva access and the shared Drive folder will be provided after joining.")
                        },
                        supportingText = {
                            Text("${selectedRole.roleNotes.length} / 400")
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

// Step 3 UI helpers stay in this file because they are only used by Role Settings.
// Keeping them here avoids creating a fragmented file for every small card or row.


@Composable
/**
 * DETAILED BEHAVIOUR — RoleSettingsSectionCard
 *
 * Renders the reusable Role Settings Section Card portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun RoleSettingsSectionCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CreateCardBackground
        ),
        border = BorderStroke(1.dp, RoleSettingsBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = CreateLightGreen
                ) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(21.dp),
                        colorFilter = ColorFilter.tint(CreateGreen)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = CreateGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}

@Composable
/**
 * Renders the role identity card card used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — RoleIdentityCard
 *
 * Renders the reusable Role Identity Card portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun RoleIdentityCard(
    template: CreateRoleTemplate,
    selectedRole: SelectedRoleDraft
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CreateLightGreen
        ),
        border = BorderStroke(1.dp, RoleSettingsBorder)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White
            ) {
                Image(
                    painter = painterResource(roleAreaIconRes(template.roleArea)),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(28.dp),
                    colorFilter = ColorFilter.tint(CreateGreen)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = template.roleName,
                    style = MaterialTheme.typography.titleLarge,
                    color = CreateGreen,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${template.roleMode.displayName} · ${selectedRole.capacity} ${if (selectedRole.capacity == 1) "volunteer" else "volunteers"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RoleLevelBadge(template.defaultLevel)
            }
        }
    }
}

@Composable
/**
 * Renders the role overview card card used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — RoleOverviewCard
 *
 * Renders the reusable Role Overview Card portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun RoleOverviewCard(
    template: CreateRoleTemplate,
    selectedRole: SelectedRoleDraft,
    onOpen: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CreateCardBackground
        ),
        border = BorderStroke(1.dp, RoleSettingsBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(roleAreaIconRes(template.roleArea)),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(CreateGreen)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = template.roleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${template.roleMode.displayName} · ${selectedRole.capacity} ${if (selectedRole.capacity == 1) "volunteer" else "volunteers"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onOpen,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CreateGreen)
                ) {
                    Image(
                        painter = painterResource(
                            if (selectedRole.isConfigured) R.drawable.edit else R.drawable.responsibility
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(CreateGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (selectedRole.isConfigured) "Edit" else "Review",
                        color = CreateGreen
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoleLevelBadge(template.defaultLevel)
                selectedRole.applicationMethod?.let { method ->
                    RoleApplicationMethodBadge(method)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        if (selectedRole.isConfigured) R.drawable.tick else R.drawable.responsibility
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(
                        if (selectedRole.isConfigured) CreateGreen else RoleSettingsOrange
                    )
                )

                Spacer(modifier = Modifier.width(7.dp))

                Text(
                    text = if (selectedRole.isConfigured) "Ready" else "Needs Review",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedRole.isConfigured) CreateGreen else RoleSettingsOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
/**
 * Renders the role level badge badge used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — RoleLevelBadge
 *
 * Renders the reusable Role Level Badge portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun RoleLevelBadge(level: VolunteerRoleLevel) {
    val background: Color
    val foreground: Color

    when (level) {
        VolunteerRoleLevel.BEGINNER -> {
            background = Color(0xFFE5F3E2)
            foreground = Color(0xFF2A5A2B)
        }

        VolunteerRoleLevel.INTERMEDIATE -> {
            background = Color(0xFFEDE7F6)
            foreground = Color(0xFF644B87)
        }

        VolunteerRoleLevel.ADVANCED -> {
            background = Color(0xFFFFE9D6)
            foreground = Color(0xFF9A4E11)
        }
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background
    ) {
        Text(
            text = level.displayName,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
/**
 * Renders the role application method badge badge used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — RoleApplicationMethodBadge
 *
 * Renders the reusable Role Application Method Badge portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun RoleApplicationMethodBadge(method: RoleApplicationMethod) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFF1F3EF)
    ) {
        Text(
            text = method.displayName,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = CreateGreen,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
/**
 * Renders the practised skill row row used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — PractisedSkillRow
 *
 * Renders the reusable Practised Skill Row portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun PractisedSkillRow(
    skill: CreateRoleSkill,
    checked: Boolean,
    recommended: Boolean,
    lockedByRequirement: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (checked) Color(0xFFF2F7F0) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (checked) Color(0xFFCEDDC8) else RoleSettingsBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled && !lockedByRequirement,
                colors = CheckboxDefaults.colors(
                    checkedColor = CreateGreen,
                    checkmarkColor = Color.White,
                    disabledCheckedColor = Color(0xFF8A9388),
                    disabledUncheckedColor = RoleSettingsBorder
                )
            )

            Text(
                text = skill.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )

            if (recommended) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = RoleSettingsPurpleBackground
                ) {
                    Text(
                        text = "Recommended",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = RoleSettingsPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
/**
 * Renders the required skill card card used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — RequiredSkillCard
 *
 * Renders the reusable Required Skill Card portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun RequiredSkillCard(
    skill: CreateRoleSkill,
    isPractised: Boolean,
    isRequired: Boolean,
    minimumExperience: Int,
    canToggleRequirement: Boolean,
    enabled: Boolean = true,
    onToggleRequired: () -> Unit,
    onIncreaseExperience: () -> Unit,
    onDecreaseExperience: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isRequired) {
                RoleSettingsOrangeBackground
            } else {
                Color.Transparent
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isRequired) {
                RoleSettingsOrange.copy(alpha = 0.65f)
            } else {
                RoleSettingsBorder
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = enabled && (canToggleRequirement || isRequired),
                        onClick = onToggleRequired
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = skill.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isRequired) FontWeight.SemiBold else FontWeight.Normal
                    )

                    if (!isPractised && !isRequired) {
                        Text(
                            text = if (canToggleRequirement) {
                                "Making this required also selects it under Skills Practised"
                            } else {
                                "Maximum 4 practised skills. Remove another skill first."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isRequired) {
                        RoleSettingsOrangeBackground
                    } else {
                        Color(0xFFF1F3EF)
                    }
                ) {
                    Text(
                        text = if (isRequired) "Required" else "Not required",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRequired) {
                            RoleSettingsOrange
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isRequired) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            if (isRequired) {
                Text(
                    text = "Minimum verified experience",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoleSettingsOrange,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecreaseExperience,
                        enabled = enabled && minimumExperience > 1,
                        modifier = Modifier.size(42.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RoleSettingsOrange.copy(alpha = 0.55f))
                    ) {
                        Image(
                            painter = painterResource(R.drawable.remove),
                            contentDescription = "Decrease minimum experience",
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(RoleSettingsOrange)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoleSettingsOrangeBackground
                    ) {
                        Text(
                            text = minimumExperience.toString(),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = RoleSettingsOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onIncreaseExperience,
                        enabled = enabled && minimumExperience < 8,
                        modifier = Modifier.size(42.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RoleSettingsOrange.copy(alpha = 0.55f))
                    ) {
                        Image(
                            painter = painterResource(R.drawable.add),
                            contentDescription = "Increase minimum experience",
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(RoleSettingsOrange)
                        )
                    }

                    Text(
                        text = if (minimumExperience == 1) {
                            "verified experience"
                        } else {
                            "verified experiences"
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Returns the role area icon res value required by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — roleAreaIconRes
 *
 * Handles the Compose/UI responsibility for role area icon res.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun roleAreaIconRes(roleArea: String): Int {
    return when (roleArea) {
        "General Event Support" -> R.drawable.role_general_support
        "Registration & Guest Support" -> R.drawable.role_registration
        "Logistics & Distribution" -> R.drawable.role_logistics
        "Crowd & Safety Support" -> R.drawable.role_safety
        "Community Engagement & Activity Support" -> R.drawable.role_community
        "Media & Event Documentation" -> R.drawable.role_media
        "Graphic & Visual Design" -> R.drawable.role_graphic_design
        "Writing & Content" -> R.drawable.role_writing
        "Social Media & Digital Campaigns" -> R.drawable.role_social_media
        "Research & Data" -> R.drawable.role_research_data
        "Administration & Documentation" -> R.drawable.role_administration
        "Digital & Technical Support" -> R.drawable.role_technical_support
        else -> R.drawable.role_general_support
    }
}
