package com.example.volunteerlink.organisation.create.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.components.CreateCardBackground
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateLightGreen
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel

/** Step 3 of Create Post: configure every role selected in Step 2. */
@Composable
fun RoleSettingsStep(
    uiState: CreatePostUiState,
    viewModel: CreatePostViewModel,
    onBack: () -> Unit
) {
    val editingRoleId = uiState.editingRoleTemplateId
    val editingRole = uiState.draft.selectedRoles.firstOrNull {
        it.roleTemplateId == editingRoleId
    }
    val editingTemplate = uiState.roleCatalogue.firstOrNull {
        it.roleTemplateId == editingRoleId
    }

    if (editingRole != null && editingTemplate != null) {
        RoleConfigurationEditor(
            uiState = uiState,
            selectedRole = editingRole,
            template = editingTemplate,
            viewModel = viewModel
        )
    } else {
        RoleSettingsOverview(
            uiState = uiState,
            onBack = onBack,
            onOpenRole = viewModel::openRoleEditor,
            onResponsibleRoleChanged =
                viewModel::updateSharedSubmissionResponsibleRole,
            onContinue = {
                viewModel.continueFromStepThree()
            }
        )
    }
}

@Composable
fun RoleSettingsOverview(
    uiState: CreatePostUiState,
    onBack: () -> Unit,
    onOpenRole: (String) -> Unit,
    onResponsibleRoleChanged: (String) -> Unit,
    onContinue: () -> Unit
) {
    val readyCount = uiState.draft.selectedRoles.count { it.isConfigured }
    val totalRoles = uiState.draft.selectedRoles.size
    val allRolesReady = totalRoles > 0 && readyCount == totalRoles
    val remoteReady = isRemoteSubmissionReady(
        draft = uiState.draft,
        catalogue = uiState.roleCatalogue
    )
    val canContinue = allRolesReady && remoteReady

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 36.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StepThreeHeader(
                onBack = onBack,
                isEditingFromReview = uiState.reviewEditStep == 3
            )
        }

        item {
            SetupProgressCard(
                readyCount = readyCount,
                totalRoles = totalRoles
            )
        }

        if (uiState.draft.postType != VolunteerPostType.PHYSICAL) {
            item {
                RemoteSubmissionOverviewCard(
                    draft = uiState.draft,
                    catalogue = uiState.roleCatalogue,
                    onResponsibleRoleChanged = onResponsibleRoleChanged
                )
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Selected Roles",
                    style = MaterialTheme.typography.titleLarge,
                    color = CreateGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Review one role at a time. Fixed role information and recommended skills come from Supabase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        uiState.draft.selectedRoles.forEach { selectedRole ->
            val template = uiState.roleCatalogue.firstOrNull {
                it.roleTemplateId == selectedRole.roleTemplateId
            }

            if (template != null) {
                item(key = "role_settings_${selectedRole.roleTemplateId}") {
                    RoleOverviewCard(
                        template = template,
                        selectedRole = selectedRole,
                        onOpen = {
                            onOpenRole(selectedRole.roleTemplateId)
                        }
                    )
                }
            }
        }

        item {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CreateGreen
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (uiState.reviewEditStep == 3) "Save Changes" else "Continue to Schedule",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (!canContinue || uiState.roleSettingsError != null) {
            item {
                Text(
                    text = uiState.roleSettingsError ?: when {
                        !allRolesReady ->
                            "Review and save every selected role before continuing."

                        !remoteReady &&
                                uiState.draft.remoteSubmissionMode ==
                                RemoteSubmissionMode.SHARED_TEAM ->
                            "Choose which Remote role will submit the shared team deliverable."

                        else ->
                            "Complete the Remote submission setup before continuing."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RoleConfigurationEditor(
    uiState: CreatePostUiState,
    selectedRole: SelectedRoleDraft,
    template: CreateRoleTemplate,
    viewModel: CreatePostViewModel
) {
    val listState = rememberLazyListState()
    val canSave = viewModel.canSaveRoleConfiguration(
        selectedRole.roleTemplateId
    )

    // Save & Next swaps the role while this composable remains visible.
    // Resetting here prevents the next role from opening at the old scroll spot.
    LaunchedEffect(selectedRole.roleTemplateId) {
        listState.scrollToItem(0)
    }

    // OrganisationNavigationHost already provides the app's IME/window setup.
    // Do not add imePadding here or the keyboard inset is counted twice,
    // which creates the large blank space below Step 3 fields.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 40.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = viewModel::closeRoleEditor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.back),
                        contentDescription = "Back to role overview",
                        modifier = Modifier.size(30.dp),
                        colorFilter = ColorFilter.tint(CreateGreen)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Configure Role",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CreateGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Step 3 of 5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            RoleIdentityCard(
                template = template,
                selectedRole = selectedRole
            )
        }

        item {
            RoleInformationSection(template)
        }

        item {
            SkillsPractisedSection(
                template = template,
                selectedRole = selectedRole,
                onToggleSkill = { skillId ->
                    viewModel.togglePractisedSkill(
                        roleTemplateId = selectedRole.roleTemplateId,
                        skillId = skillId
                    )
                }
            )
        }

        item {
            RequiredSkillsSection(
                template = template,
                selectedRole = selectedRole,
                onToggleRequiredSkill = { skillId ->
                    viewModel.toggleRequiredSkill(
                        roleTemplateId = selectedRole.roleTemplateId,
                        skillId = skillId
                    )
                },
                onIncreaseExperience = { skillId ->
                    viewModel.increaseRequiredSkillExperience(
                        roleTemplateId = selectedRole.roleTemplateId,
                        skillId = skillId
                    )
                },
                onDecreaseExperience = { skillId ->
                    viewModel.decreaseRequiredSkillExperience(
                        roleTemplateId = selectedRole.roleTemplateId,
                        skillId = skillId
                    )
                }
            )
        }

        item {
            ResponsibilitiesSection(
                responsibilities = selectedRole.responsibilities,
                onAdd = {
                    viewModel.addResponsibility(selectedRole.roleTemplateId)
                },
                onUpdate = { index, text ->
                    viewModel.updateResponsibility(
                        roleTemplateId = selectedRole.roleTemplateId,
                        index = index,
                        text = text
                    )
                },
                onRemove = { index ->
                    viewModel.removeResponsibility(
                        roleTemplateId = selectedRole.roleTemplateId,
                        index = index
                    )
                }
            )
        }

        item {
            ApplicantMethodSection(
                template = template,
                selectedRole = selectedRole,
                onMethodChanged = { method ->
                    viewModel.updateRoleApplicationMethod(
                        roleTemplateId = selectedRole.roleTemplateId,
                        method = method
                    )
                },
                onAddQuestion = {
                    viewModel.addScreeningQuestion(
                        selectedRole.roleTemplateId
                    )
                },
                onUpdateQuestion = { index, text ->
                    viewModel.updateScreeningQuestion(
                        roleTemplateId = selectedRole.roleTemplateId,
                        index = index,
                        text = text
                    )
                },
                onRemoveQuestion = { index ->
                    viewModel.removeScreeningQuestion(
                        roleTemplateId = selectedRole.roleTemplateId,
                        index = index
                    )
                }
            )
        }

        item {
            RoleSubmissionAndNotesSection(
                draft = uiState.draft,
                template = template,
                selectedRole = selectedRole,
                onRoleNotesChanged = { text ->
                    viewModel.updateRoleNotes(
                        roleTemplateId = selectedRole.roleTemplateId,
                        text = text
                    )
                },
                onIndividualSubmissionRequirementChanged = { text ->
                    viewModel.updateIndividualSubmissionRequirement(
                        roleTemplateId = selectedRole.roleTemplateId,
                        text = text
                    )
                }
            )
        }

        if (uiState.roleSettingsError != null || !canSave) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = RoleSettingsOrangeBackground
                ) {
                    Text(
                        text = uiState.roleSettingsError ?: roleSaveHint(
                            draft = uiState.draft,
                            template = template,
                            selectedRole = selectedRole
                        ),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = RoleSettingsOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (
                            viewModel.saveRoleConfiguration(
                                selectedRole.roleTemplateId
                            )
                        ) {
                            viewModel.closeRoleEditor()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = canSave,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save Role")
                }

                Button(
                    onClick = {
                        viewModel.saveAndOpenNextRole(
                            selectedRole.roleTemplateId
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = canSave,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreateGreen
                    )
                ) {
                    Text("Save & Next")
                }
            }
        }
    }
}

@Composable
fun StepThreeHeader(
    onBack: () -> Unit,
    isEditingFromReview: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.back),
                contentDescription = "Exit Create Post",
                modifier = Modifier.size(30.dp),
                colorFilter = ColorFilter.tint(CreateGreen)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = "Role Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = CreateGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isEditingFromReview) "Editing from Review · Role Settings" else "Step 3 of 5",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SetupProgressCard(
    readyCount: Int,
    totalRoles: Int
) {
    val progress = if (totalRoles > 0) {
        readyCount.toFloat() / totalRoles.toFloat()
    } else {
        0f
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CreateLightGreen
        ),
        border = BorderStroke(1.dp, RoleSettingsBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Image(
                        painter = painterResource(R.drawable.responsibility),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(26.dp),
                        colorFilter = ColorFilter.tint(CreateGreen)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "$readyCount of $totalRoles roles ready",
                        style = MaterialTheme.typography.titleMedium,
                        color = CreateGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (readyCount == totalRoles && totalRoles > 0) {
                            "All role details are ready."
                        } else {
                            "Open each role to review its details."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = Color(0xFFD4DED1),
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(
                            color = CreateGreen,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@Composable
fun RemoteSubmissionOverviewCard(
    draft: CreatePostDraft,
    catalogue: List<CreateRoleTemplate>,
    onResponsibleRoleChanged: (String) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                        painter = painterResource(R.drawable.remote_project),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(24.dp),
                        colorFilter = ColorFilter.tint(CreateGreen)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Remote Submission",
                        style = MaterialTheme.typography.titleMedium,
                        color = CreateGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (draft.remoteSubmissionMode) {
                            RemoteSubmissionMode.SHARED_TEAM ->
                                "One final team deliverable is submitted for the remote work."

                            RemoteSubmissionMode.INDIVIDUAL ->
                                "Every remote volunteer submits the output assigned to their role."

                            null ->
                                "Remote submission setup is incomplete."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (draft.remoteSubmissionMode) {
                RemoteSubmissionMode.SHARED_TEAM -> {
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
                                text = "Project Deliverable",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = draft.sharedDeliverable.ifBlank {
                                    "No shared deliverable entered."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = CreateGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Responsible Remote role",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose the role that will be responsible for the final team submission.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val templatesById = catalogue.associateBy {
                        it.roleTemplateId
                    }
                    val remoteRoles = draft.selectedRoles.filter { selectedRole ->
                        templatesById[selectedRole.roleTemplateId]?.roleMode ==
                                VolunteerRoleMode.REMOTE
                    }

                    remoteRoles.forEach { selectedRole ->
                        val template = templatesById[selectedRole.roleTemplateId]
                            ?: return@forEach
                        val selected =
                            draft.sharedSubmissionResponsibleRoleTemplateId ==
                                    selectedRole.roleTemplateId

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onResponsibleRoleChanged(
                                        selectedRole.roleTemplateId
                                    )
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) CreateLightGreen else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (selected) Color(0xFFBED2B7) else RoleSettingsBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = CreateGreen
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = template.roleName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${selectedRole.capacity} ${if (selectedRole.capacity == 1) "volunteer" else "volunteers"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                RemoteSubmissionMode.INDIVIDUAL -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CreateLightGreen
                    ) {
                        Text(
                            text = "Each Remote role will define its own Individual Deliverable while you configure the role.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = CreateGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                null -> {
                    Text(
                        text = "Return to Step 1 and choose a Remote submission setup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoleSettingsOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

fun isRemoteSubmissionReady(
    draft: CreatePostDraft,
    catalogue: List<CreateRoleTemplate>
): Boolean {
    if (draft.postType == VolunteerPostType.PHYSICAL) return true

    return when (draft.remoteSubmissionMode) {
        RemoteSubmissionMode.SHARED_TEAM -> {
            if (draft.sharedDeliverable.isBlank()) return false
            val responsibleRoleId =
                draft.sharedSubmissionResponsibleRoleTemplateId
                    ?: return false
            val template = catalogue.firstOrNull {
                it.roleTemplateId == responsibleRoleId
            } ?: return false

            template.roleMode == VolunteerRoleMode.REMOTE &&
                    draft.selectedRoles.any {
                        it.roleTemplateId == responsibleRoleId
                    }
        }

        RemoteSubmissionMode.INDIVIDUAL -> true
        null -> false
    }
}

fun roleSaveHint(
    draft: CreatePostDraft,
    template: CreateRoleTemplate,
    selectedRole: SelectedRoleDraft
): String {
    return when {
        selectedRole.practisedSkillIds.size !in 2..4 ->
            "Select between 2 and 4 Skills Practised."

        selectedRole.responsibilities.none { it.isNotBlank() } ->
            "Add at least one responsibility."

        template.roleMode == VolunteerRoleMode.REMOTE &&
                draft.remoteSubmissionMode == RemoteSubmissionMode.INDIVIDUAL &&
                selectedRole.individualSubmissionRequirement.isBlank() ->
            "Add the Individual Deliverable for this Remote role."

        template.roleMode == VolunteerRoleMode.REMOTE &&
                draft.remoteSubmissionMode == null ->
            "Return to Step 1 and choose a Remote submission setup."

        else ->
            "Review the role details before saving."
    }
}
