package com.example.volunteerlink.organisation.create.steps

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.components.CreateGreen
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
    onPublish: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF5))
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 14.dp,
            end = 20.dp,
            bottom = 48.dp
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

        item {
            ReviewSummaryActions(
                isSavingDraft = uiState.isSavingDraft,
                isPublishing = uiState.isPublishing,
                errorMessage = uiState.saveDraftError ?: uiState.publishError,
                onSaveDraft = onSaveDraft,
                onPublish = onPublish
            )
        }
    }
}

@Composable
fun ReviewSummaryActions(
    isSavingDraft: Boolean,
    isPublishing: Boolean,
    errorMessage: String?,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit
) {
    val isBusy = isSavingDraft || isPublishing

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

        OutlinedButton(
            onClick = onSaveDraft,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, CreateGreen)
        ) {
            Text(
                text = if (isSavingDraft) "Saving Draft..." else "Save as Draft",
                color = if (isBusy) {
                    CreateGreen.copy(alpha = 0.38f)
                } else {
                    CreateGreen
                },
                fontWeight = FontWeight.SemiBold
            )
        }

        Button(
            onClick = onPublish,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
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

@Composable
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
