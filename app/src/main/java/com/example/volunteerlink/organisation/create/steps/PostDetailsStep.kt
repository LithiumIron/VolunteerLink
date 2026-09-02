package com.example.volunteerlink.organisation.create.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.create.components.CategoryPicker
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateSectionCard
import com.example.volunteerlink.organisation.create.components.EditRestrictionNotice
import com.example.volunteerlink.organisation.create.components.FormError
import com.example.volunteerlink.organisation.create.components.PostTypeCard
import com.example.volunteerlink.organisation.create.components.ThumbnailPickerSection
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel
import com.example.volunteerlink.organisation.create.model.VolunteerPostType

/** Full Step 1 UI. Form data is read from and written back to the ViewModel. */
@Composable
fun PostDetailsStep(
    uiState: CreatePostUiState,
    viewModel: CreatePostViewModel,
    onBack: () -> Unit,
    onStepOneComplete: () -> Unit = {}
) {
    val draft = uiState.draft
    val errors = uiState.visibleErrors
    val editPolicy = uiState.editPolicy
    val isExistingEdit = uiState.isExistingPostEdit
    val canChangePostType = !isExistingEdit || editPolicy?.postStatus == "DRAFT"
    val canEditSharedInfo = !isExistingEdit || editPolicy?.canEditSharedPostInfo != false
    val listState = rememberLazyListState()

    // Draft warnings on Home should remain obvious after opening Edit Post.
    // Only unpublished drafts use the 7-day publication rule here; an already
    // published post that is naturally approaching its start date is not an error.
    val showDraftDateAttention = isExistingEdit && editPolicy?.postStatus == "DRAFT"
    val physicalDraftDateAttention = if (showDraftDateAttention) {
        CreatePostValidator.draftStartDateAttention(draft.physicalStartDateMillis)
            .takeIf {
                draft.postType == VolunteerPostType.PHYSICAL ||
                    draft.postType == VolunteerPostType.HYBRID
            }
    } else {
        null
    }
    val remoteDraftDateAttention = if (showDraftDateAttention) {
        CreatePostValidator.draftStartDateAttention(draft.remoteStartDateMillis)
            .takeIf {
                draft.postType == VolunteerPostType.REMOTE ||
                    draft.postType == VolunteerPostType.HYBRID
            }
    } else {
        null
    }
    val hasDraftDateAttention =
        physicalDraftDateAttention != null || remoteDraftDateAttention != null

    // Continue may reveal an error far above the button. Move the organiser
    // straight to the first section that needs attention instead of making
    // them hunt through the form.
    val firstErrorSectionIndex = run {
        val existingInfoOffset =
            (if (uiState.isExistingPostEdit) 1 else 0) +
                (if (hasDraftDateAttention) 1 else 0)
        val postTypeIndex = 1 + existingInfoOffset
        val postInformationIndex = postTypeIndex + 1
        var nextIndex = postInformationIndex + 1
        val hasPhysical = draft.postType == VolunteerPostType.PHYSICAL ||
            draft.postType == VolunteerPostType.HYBRID
        val hasRemote = draft.postType == VolunteerPostType.REMOTE ||
            draft.postType == VolunteerPostType.HYBRID
        val physicalIndex = if (hasPhysical) nextIndex++ else null
        val remoteIndex = if (hasRemote) nextIndex++ else null
        val hybridCapacityIndex = if (draft.postType == VolunteerPostType.HYBRID) nextIndex else null

        when {
            errors.postType != null -> postTypeIndex
            errors.category != null || errors.title != null || errors.description != null ->
                postInformationIndex
            errors.physicalStartDate != null || errors.physicalEndDate != null ||
                errors.physicalTime != null || errors.physicalLocation != null ||
                errors.physicalCapacity != null -> physicalIndex
            errors.remoteStartDate != null || errors.remoteDueDate != null ||
                errors.remoteCapacity != null || errors.remoteSubmissionMode != null ||
                errors.sharedDeliverable != null -> remoteIndex
            errors.hybridPhysicalCapacity != null || errors.hybridRemoteCapacity != null ->
                hybridCapacityIndex
            else -> null
        }
    }

    val firstDraftAttentionSectionIndex = run {
        if (!hasDraftDateAttention) return@run null

        val existingInfoOffset =
            (if (uiState.isExistingPostEdit) 1 else 0) + 1 // combined attention notice
        val postTypeIndex = 1 + existingInfoOffset
        val postInformationIndex = postTypeIndex + 1
        var nextIndex = postInformationIndex + 1
        val hasPhysical = draft.postType == VolunteerPostType.PHYSICAL ||
            draft.postType == VolunteerPostType.HYBRID
        val hasRemote = draft.postType == VolunteerPostType.REMOTE ||
            draft.postType == VolunteerPostType.HYBRID
        val physicalIndex = if (hasPhysical) nextIndex++ else null
        val remoteIndex = if (hasRemote) nextIndex++ else null

        when {
            physicalDraftDateAttention != null -> physicalIndex
            remoteDraftDateAttention != null -> remoteIndex
            else -> null
        }
    }

    // Opening an outdated draft from Home should land on the first section that
    // actually needs changing instead of leaving the organiser at the top of a
    // long Edit Post form. This runs once for the loaded post.
    LaunchedEffect(uiState.existingPostId) {
        firstDraftAttentionSectionIndex?.let { target ->
            listState.animateScrollToItem(target.coerceAtLeast(0))
        }
    }

    LaunchedEffect(uiState.validationFocusRequest) {
        if (uiState.showValidationErrors && errors.hasErrors()) {
            firstErrorSectionIndex?.let { target ->
                listState.animateScrollToItem(target.coerceAtLeast(0))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onBack) {
                    Image(
                        painter = painterResource(R.drawable.back),
                        contentDescription = "Exit Create Post",
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = when {
                            uiState.isExistingPostEdit -> "Edit Volunteer Post"
                            uiState.reviewEditStep == 1 -> "Edit Post Details"
                            else -> "Create Volunteer Post"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = CreateGreen
                    )

                    Text(
                        text = when {
                            uiState.isExistingPostEdit -> "Manage Post · Step 1 of 5 · Post Details"
                            uiState.reviewEditStep == 1 -> "Editing from Review · Post Details"
                            else -> "Step 1 of 5 · Post Details"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.isExistingPostEdit) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (editPolicy?.isReadOnly == true) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        Color(0xFFF1F7EE)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (editPolicy?.isReadOnly == true) {
                                "This post is read-only"
                            } else {
                                "Editing an existing post"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (editPolicy?.isReadOnly == true) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                CreateGreen
                            }
                        )
                        Text(
                            text = editPolicy?.readOnlyReason
                                ?: "Fields already relied on by applicants or volunteers are locked. Safe future details remain editable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (hasDraftDateAttention) {
            item {
                EditRestrictionNotice(
                    title = "Update before publishing",
                    message = buildString {
                        val affected = mutableListOf<String>()
                        if (physicalDraftDateAttention != null) affected += "Physical start date"
                        if (remoteDraftDateAttention != null) affected += "Remote start date"
                        append(affected.joinToString(" and "))
                        append(if (affected.size == 1) " needs" else " need")
                        append(" attention. The affected date field is highlighted below.")
                    }
                )
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Post Type",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CreateGreen
                    )
                    Text(
                        text = "Choose how volunteers will participate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PostTypeCard(
                        type = VolunteerPostType.PHYSICAL,
                        iconRes = R.drawable.post_physical_event,
                        selected = draft.postType == VolunteerPostType.PHYSICAL,
                        onClick = {
                            viewModel.requestPostTypeChange(
                                VolunteerPostType.PHYSICAL
                            )
                        },
                        enabled = canChangePostType,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    PostTypeCard(
                        type = VolunteerPostType.REMOTE,
                        iconRes = R.drawable.post_remote_project,
                        selected = draft.postType == VolunteerPostType.REMOTE,
                        onClick = {
                            viewModel.requestPostTypeChange(
                                VolunteerPostType.REMOTE
                            )
                        },
                        enabled = canChangePostType,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    PostTypeCard(
                        type = VolunteerPostType.HYBRID,
                        iconRes = R.drawable.post_hybrid_event,
                        selected = draft.postType == VolunteerPostType.HYBRID,
                        onClick = {
                            viewModel.requestPostTypeChange(
                                VolunteerPostType.HYBRID
                            )
                        },
                        enabled = canChangePostType,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                FormError(errors.postType.takeIf { canChangePostType })


            }
        }

        if (draft.postType != null) {
            item {
                CreateSectionCard(
                    title = "Post Information",
                    subtitle = "Give volunteers a clear introduction to this opportunity."
                ) {
                    CategoryPicker(
                        selectedCategory = draft.category,
                        onCategorySelected = viewModel::updateCategory,
                        errorMessage = errors.category,
                        enabled = canEditSharedInfo
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        OutlinedTextField(
                            value = draft.title,
                            onValueChange = viewModel::updateTitle,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Title") },
                            placeholder = {
                                Text(
                                    when (draft.postType) {
                                        VolunteerPostType.PHYSICAL ->
                                            "Example: Community Charity Run"
                                        VolunteerPostType.REMOTE ->
                                            "Example: Social Media Awareness Campaign"
                                        VolunteerPostType.HYBRID ->
                                            "Example: Community Awareness Programme"
                                        null -> "Volunteer opportunity title"
                                    }
                                )
                            },
                            singleLine = true,
                            enabled = canEditSharedInfo,
                            isError = canEditSharedInfo && errors.title != null,
                            shape = RoundedCornerShape(14.dp)
                        )
                        FormError(errors.title.takeIf { canEditSharedInfo })
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        OutlinedTextField(
                            value = draft.description,
                            onValueChange = viewModel::updateDescription,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Description") },
                            placeholder = {
                                Text("Explain the purpose, who volunteers will support, and what the opportunity involves.")
                            },
                            minLines = 4,
                            maxLines = 7,
                            enabled = canEditSharedInfo,
                            isError = canEditSharedInfo && errors.description != null,
                            shape = RoundedCornerShape(14.dp)
                        )
                        FormError(errors.description.takeIf { canEditSharedInfo })
                    }

                    ThumbnailPickerSection(
                        thumbnailUri = draft.thumbnailUri,
                        onThumbnailChanged = viewModel::updateThumbnailUri,
                        enabled = canEditSharedInfo
                    )
                }
            }

            if (
                draft.postType == VolunteerPostType.PHYSICAL ||
                draft.postType == VolunteerPostType.HYBRID
            ) {
                item {
                    PhysicalEventDetailsSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        showVolunteerCapacity =
                            draft.postType == VolunteerPostType.PHYSICAL
                    )
                }
            }

            if (
                draft.postType == VolunteerPostType.REMOTE ||
                draft.postType == VolunteerPostType.HYBRID
            ) {
                item {
                    RemoteProjectDetailsSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        showVolunteerCapacity =
                            draft.postType == VolunteerPostType.REMOTE
                    )
                }
            }

            if (draft.postType == VolunteerPostType.HYBRID) {
                item {
                    HybridVolunteerRequirementSection(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (viewModel.continueFromStepOne()) {
                                onStepOneComplete()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CreateGreen
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (uiState.reviewEditStep == 1) "Save Changes" else "Continue to Add Roles",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (uiState.isStepOneReady) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F7EE)
                        ) {
                            Text(
                                text = if (uiState.reviewEditStep == 1) "Post Details are ready to return to Review." else "Step 1 is complete and ready for role selection.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CreateGreen
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Ask only when switching away from a mode that already contains
    // mode-specific information. Shared fields are never affected.
    uiState.pendingPostType?.let { pendingType ->
        val currentType = draft.postType

        AlertDialog(
            onDismissRequest = viewModel::cancelPostTypeChange,
            title = {
                Text("Switch post type?")
            },
            text = {
                Text(
                    if (currentType != null) {
                        "Switch from ${currentType.displayName} to ${pendingType.displayName}? " +
                                "Your ${currentType.displayName} details will be kept temporarily while you remain on Step 1. " +
                                "If you continue with ${pendingType.displayName}, unused ${currentType.displayName} details will be cleared."
                    } else {
                        "Switch to ${pendingType.displayName}?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmPostTypeChange
                ) {
                    Text("Switch Mode")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelPostTypeChange
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
