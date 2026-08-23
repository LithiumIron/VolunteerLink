package com.example.volunteerlink.organisation.create.steps

import android.content.Context
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
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.TrainingLocationMode
import com.example.volunteerlink.organisation.create.model.TrainingMode
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReviewPostPreviewCard(
    uiState: CreatePostUiState
) {
    val draft = uiState.draft
    val context = LocalContext.current
    var descriptionExpanded by remember { mutableStateOf(false) }

    val thumbnail = remember(draft.thumbnailUri) {
        draft.thumbnailUri
            ?.takeIf { it.isNotBlank() }
            ?.let { uriText ->
                loadReviewThumbnail(
                    context = context,
                    uriText = uriText
                )
            }
    }

    val overallStart = listOfNotNull(
        draft.physicalStartDateMillis,
        draft.remoteStartDateMillis
    ).minOrNull()
    val overallEnd = listOfNotNull(
        draft.physicalEndDateMillis,
        draft.remoteDueDateMillis
    ).maxOrNull()

    val locationSummary = when (draft.postType) {
        VolunteerPostType.PHYSICAL,
        VolunteerPostType.HYBRID ->
            draft.physicalLocation?.displayName
                ?: draft.physicalLocationQuery.takeIf { it.isNotBlank() }
                ?: "Location not set"

        VolunteerPostType.REMOTE -> "Remote opportunity"
        null -> "Location not set"
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ReviewBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = "Post thumbnail",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(154.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ReviewSoftSurface
                    ) {
                        Column(
                            modifier = Modifier.height(154.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(R.drawable.camera),
                                contentDescription = null,
                                modifier = Modifier.size(34.dp),
                                colorFilter = ColorFilter.tint(CreateGreen)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No thumbnail selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = ReviewSecondaryText
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = draft.title.ifBlank { "Untitled volunteer post" },
                    style = MaterialTheme.typography.titleLarge,
                    color = ReviewText,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    draft.postType?.displayName?.let { label ->
                        ReviewMetaChip(label)
                    }
                    draft.category?.displayName?.let { label ->
                        ReviewMetaChip(label)
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = reviewDateRange(overallStart, overallEnd),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ReviewText,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = locationSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ReviewSecondaryText
                    )
                }

                if (draft.description.isNotBlank()) {
                    HorizontalDivider(color = ReviewBorder)

                    Text(
                        text = draft.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ReviewText,
                        maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (draft.description.length > 130) {
                        TextButton(
                            onClick = {
                                descriptionExpanded = !descriptionExpanded
                            }
                        ) {
                            Text(
                                text = if (descriptionExpanded) {
                                    "Show less"
                                } else {
                                    "View full description"
                                },
                                color = CreateGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ReviewPostDetailsCard(
    uiState: CreatePostUiState
) {
    val draft = uiState.draft

    ReviewWhiteCard {
        if (
            draft.postType == VolunteerPostType.PHYSICAL ||
            draft.postType == VolunteerPostType.HYBRID
        ) {
            ReviewPostModeSummary(
                title = "Physical",
                firstLine = reviewDateRange(
                    draft.physicalStartDateMillis,
                    draft.physicalEndDateMillis
                ),
                secondLine = reviewTimeRange(
                    draft.physicalStartTimeMinutes,
                    draft.physicalEndTimeMinutes
                ),
                thirdLine = draft.physicalLocation?.displayName
                    ?: draft.physicalLocationQuery.ifBlank { "Location not set" },
                fourthLine = when (draft.postType) {
                    VolunteerPostType.PHYSICAL ->
                        reviewSlotText(draft.physicalVolunteerCapacity)
                    VolunteerPostType.HYBRID ->
                        reviewSlotText(draft.hybridPhysicalVolunteerCapacity)
                    else -> null
                }
            )

            draft.meetingPoint
                .takeIf { it.isNotBlank() }
                ?.let { meetingPoint ->
                    ReviewCompactLabelValue(
                        label = "Meeting point",
                        value = meetingPoint
                    )
                }
        }

        if (draft.postType == VolunteerPostType.HYBRID) {
            HorizontalDivider(color = ReviewBorder)
        }

        if (
            draft.postType == VolunteerPostType.REMOTE ||
            draft.postType == VolunteerPostType.HYBRID
        ) {
            ReviewPostModeSummary(
                title = "Remote",
                firstLine = reviewDateRange(
                    draft.remoteStartDateMillis,
                    draft.remoteDueDateMillis
                ),
                secondLine = draft.remoteSubmissionMode?.displayName
                    ?: "Submission mode not set",
                thirdLine = when (draft.postType) {
                    VolunteerPostType.REMOTE ->
                        reviewSlotText(draft.remoteVolunteerCapacity)
                    VolunteerPostType.HYBRID ->
                        reviewSlotText(draft.hybridRemoteVolunteerCapacity)
                    else -> null
                }
            )

            if (
                draft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM &&
                draft.sharedDeliverable.isNotBlank()
            ) {
                ReviewCompactLabelValue(
                    label = "Shared deliverable",
                    value = draft.sharedDeliverable
                )
            }
        }
    }
}


/**
 * Loads a small review preview instead of decoding the original photo at full size.
 * This keeps the summary card lightweight even when the organiser selects a large image.
 */
fun loadReviewThumbnail(
    context: Context,
    uriText: String
) = runCatching {
    val uri = Uri.parse(uriText)
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    context.contentResolver
        .openInputStream(uri)
        ?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > 1200 ||
        bounds.outHeight / sampleSize > 1200
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize.coerceAtLeast(1)
    }

    context.contentResolver
        .openInputStream(uri)
        ?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        ?.asImageBitmap()
}.getOrNull()
