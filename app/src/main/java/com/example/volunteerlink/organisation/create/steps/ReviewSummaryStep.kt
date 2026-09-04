package com.example.volunteerlink.organisation.create.steps

// FILE OVERVIEW:
/*
 * ReviewSummaryStep contains presentation code for the organisation Create/Edit Post flow.
 * It focuses on rendering state and forwarding user actions through callbacks/ViewModels,
 * keeping database access and business rules outside the composables where possible.
 */


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.organisation.create.model.CreatePostUiState

/**
 * Read-only Create Post review.
 *
 * The page intentionally starts compact: major information is visible at a glance,
 * while heavier role and schedule details are expanded only when the organiser asks.
 * Save Draft and Publish sit at the end after the organiser has reviewed the post.
 */
@Composable
fun ReviewSummaryStep(
    uiState: CreatePostUiState,
    onUp: () -> Unit,
    onEditStep: (Int) -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onSaveChanges: () -> Unit = {},
    allowSaveDraft: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF5))
            .statusBarsPadding()
    ) {
        // Keep the long review content scrollable, but do not put the final
        // Save/Publish actions inside it. This makes the two database actions
        // visible at all times instead of hiding them after a long summary.
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 14.dp,
                end = 20.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ReviewSummaryHeader(onUp = onUp)
            }

            if (uiState.scheduleEditorDraft != null) {
                item {
                    ReviewPausedScheduleBanner()
                }
            }

            if (uiState.impactWeaveDraftId != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEAF4E6),
                        border = BorderStroke(1.dp, Color(0xFFC5DABC))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text("Impact Weave support", fontWeight = FontWeight.Bold, color = CreateGreen)
                            Text(
                                "This post keeps the final partnership schedule and venue.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            uiState.impactWeavePartners.forEach { partner ->
                                Text(
                                    "${partner.organisationName}: ${partner.contributionSummary}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReviewSectionHeader(
                        title = "Post Details",
                        onEdit = { onEditStep(1) }
                    )

                    ReviewPostPreviewCard(uiState = uiState)

                    ReviewPostDetailsCard(uiState = uiState)
                }
            }

            item {
                ReviewRolesCapacitySection(
                    uiState = uiState,
                    onEdit = { onEditStep(2) }
                )
            }

            item {
                ReviewRoleSettingsSection(
                    uiState = uiState,
                    onEdit = { onEditStep(3) }
                )
            }

            item {
                ReviewScheduleSection(
                    uiState = uiState,
                    onEdit = { onEditStep(4) }
                )
            }
        }

        // Fixed Review actions. OrganisationNavigationHost already keeps this
        // screen above the app bottom navigation, so no extra system-bar inset
        // is needed here.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    horizontal = 20.dp,
                    vertical = 14.dp
                )
        ) {
            ReviewSummaryActions(
                isExistingPostEdit = uiState.isExistingPostEdit,
                isSavingDraft = uiState.isSavingDraft,
                isPublishing = uiState.isPublishing,
                isSavingChanges = uiState.isSavingChanges,
                canSaveChanges = uiState.editPolicy?.isReadOnly != true,
                errorMessage = if (uiState.isExistingPostEdit) {
                    uiState.saveChangesError ?: uiState.editPolicy?.readOnlyReason
                } else {
                    uiState.saveDraftError ?: uiState.publishError
                },
                onSaveDraft = onSaveDraft,
                onPublish = onPublish,
                onSaveChanges = onSaveChanges,
                allowSaveDraft = allowSaveDraft
            )
        }
    }
}

@Composable
/**
 * Renders the UI represented by review summary actions for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun ReviewSummaryActions(
    isExistingPostEdit: Boolean,
    isSavingDraft: Boolean,
    isPublishing: Boolean,
    isSavingChanges: Boolean,
    canSaveChanges: Boolean = true,
    errorMessage: String?,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onSaveChanges: () -> Unit,
    allowSaveDraft: Boolean = true
) {
    val isBusy = isSavingDraft || isPublishing || isSavingChanges

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (isExistingPostEdit) {
            Button(
                onClick = onSaveChanges,
                enabled = !isBusy && canSaveChanges,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CreateGreen,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isSavingChanges) "Saving Changes..." else "Save Changes",
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // Create mode keeps the original Save Draft + Publish actions.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (allowSaveDraft) {
                    OutlinedButton(
                        onClick = onSaveDraft,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, CreateGreen)
                    ) {
                        Text(
                            text = if (isSavingDraft) "Saving Draft..." else "Save as Draft",
                            color = if (isBusy) CreateGreen.copy(alpha = 0.38f) else CreateGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = onPublish,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreateGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isPublishing) "Publishing..." else "Publish Post",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
/**
 * Renders the review summary header header used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun ReviewSummaryHeader(
    onUp: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onUp,
            modifier = Modifier.size(44.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.back),
                contentDescription = "Exit Create Post",
                modifier = Modifier.size(29.dp),
                colorFilter = ColorFilter.tint(CreateGreen)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Review Your Post",
                style = MaterialTheme.typography.headlineSmall,
                color = CreateGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Check the essentials first. Open details only when you need them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
