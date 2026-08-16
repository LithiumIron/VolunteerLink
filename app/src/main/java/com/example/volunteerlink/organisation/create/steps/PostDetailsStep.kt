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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.components.CategoryPicker
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateSectionCard
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
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
                        contentDescription = "Back",
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Create Volunteer Post",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = CreateGreen
                    )

                    Text(
                        text = "Step 1 of 4 · Post Details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                FormError(errors.postType)


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
                        errorMessage = errors.category
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
                            isError = errors.title != null,
                            shape = RoundedCornerShape(14.dp)
                        )
                        FormError(errors.title)
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
                            isError = errors.description != null,
                            shape = RoundedCornerShape(14.dp)
                        )
                        FormError(errors.description)
                    }

                    ThumbnailPickerSection(
                        thumbnailUri = draft.thumbnailUri,
                        onThumbnailChanged = viewModel::updateThumbnailUri
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
                            text = "Continue to Add Roles",
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
                                text = "Step 1 is complete and ready for role selection.",
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
