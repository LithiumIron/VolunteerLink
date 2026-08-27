package com.example.volunteerlink.organisation.create.steps

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
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReviewScheduleSection(
    uiState: CreatePostUiState,
    onEdit: () -> Unit
) {
    val items = uiState.draft.scheduleItems
    val templatesById = uiState.roleCatalogue.associateBy {
        it.roleTemplateId
    }
    var expandedType by remember {
        mutableStateOf<ScheduleType?>(null)
    }
    var expandedItemId by remember {
        mutableStateOf<String?>(null)
    }

    val physicalCount = items.count { it.scheduleType == ScheduleType.PHYSICAL }
    val remoteCount = items.count { it.scheduleType == ScheduleType.REMOTE }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ReviewSectionHeader(
            title = "Schedule",
            onEdit = onEdit
        )

        ReviewScheduleStats(
            physicalCount = physicalCount,
            remoteCount = remoteCount
        )

        if (items.isEmpty()) {
            ReviewWhiteCard {
                ReviewEmptyText(
                    "No schedule items added. The schedule is optional."
                )
            }
        } else {
            listOf(
                ScheduleType.PHYSICAL,
                ScheduleType.REMOTE
            ).forEach { type ->
                val typeItems = items
                    .filter { it.scheduleType == type }
                    .sortedWith(
                        compareBy<ScheduleItemDraft>(
                            { it.scheduleDateMillis ?: Long.MAX_VALUE },
                            { it.startTimeMinutes ?: Int.MAX_VALUE },
                            { it.title }
                        )
                    )

                if (typeItems.isNotEmpty()) {
                    val isExpanded = expandedType == type

                    ReviewScheduleGroupCard(
                        uiState = uiState,
                        type = type,
                        items = typeItems,
                        templatesById = templatesById,
                        isExpanded = isExpanded,
                        expandedItemId = expandedItemId,
                        onToggleGroup = {
                            expandedType = if (isExpanded) null else type
                            expandedItemId = null
                        },
                        onToggleItem = { itemId ->
                            expandedItemId = if (expandedItemId == itemId) {
                                null
                            } else {
                                itemId
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun ReviewScheduleStats(
    physicalCount: Int,
    remoteCount: Int
) {
    ReviewWhiteCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ReviewStat(
                value = physicalCount.toString(),
                label = "Physical"
            )
            ReviewStat(
                value = remoteCount.toString(),
                label = "Remote"
            )
        }
    }
}


@Composable
fun ReviewScheduleGroupCard(
    uiState: CreatePostUiState,
    type: ScheduleType,
    items: List<ScheduleItemDraft>,
    templatesById: Map<String, CreateRoleTemplate>,
    isExpanded: Boolean,
    expandedItemId: String?,
    onToggleGroup: () -> Unit,
    onToggleItem: (String) -> Unit
) {
    val groupTitle = when (type) {
        ScheduleType.PHYSICAL -> "Physical Activities"
        ScheduleType.REMOTE -> "Remote Milestones"
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ReviewBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleGroup)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = groupTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = ReviewText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ReviewSecondaryText
                    )
                }
                ReviewChevron(isExpanded = isExpanded)
            }

            if (isExpanded) {
                HorizontalDivider(color = ReviewBorder)

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEach { item ->
                        ReviewScheduleItemCard(
                            uiState = uiState,
                            item = item,
                            templatesById = templatesById,
                            isExpanded = expandedItemId == item.draftId,
                            onToggleExpanded = {
                                onToggleItem(item.draftId)
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ReviewScheduleItemCard(
    uiState: CreatePostUiState,
    item: ScheduleItemDraft,
    templatesById: Map<String, CreateRoleTemplate>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val effectiveTargetIds = if (item.appliesToAllRoles) {
        CreatePostValidator.applicableScheduleRoleIds(
            draft = uiState.draft,
            scheduleType = item.scheduleType,
            roleCatalogue = uiState.roleCatalogue
        )
    } else {
        item.targetRoleTemplateIds
    }
    val targetNames = effectiveTargetIds.map { roleId ->
        templatesById[roleId]?.roleName ?: roleId
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded),
        color = ReviewSoftSurface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        text = item.title.ifBlank {
                            when (item.scheduleType) {
                                ScheduleType.PHYSICAL -> "Physical activity"
                                ScheduleType.REMOTE -> "Remote milestone"
                            }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = ReviewText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reviewScheduleSummaryLine(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = ReviewSecondaryText
                    )
                }
                ReviewChevron(isExpanded = isExpanded)
            }

            if (isExpanded) {
                HorizontalDivider(color = ReviewBorder)

                if (item.scheduleType == ScheduleType.PHYSICAL) {
                    ReviewCompactLabelValue(
                        label = "Location",
                        value = item.location.ifBlank {
                            uiState.draft.physicalLocation?.displayName
                                ?: "Main event location"
                        }
                    )
                }

                ReviewCompactLabelValue(
                    label = "Applies to",
                    value = if (targetNames.isEmpty()) {
                        "No roles selected"
                    } else {
                        targetNames.joinToString(" · ")
                    }
                )


                item.notes
                    .takeIf { it.isNotBlank() }
                    ?.let { notes ->
                        ReviewCompactLabelValue(
                            label = "Notes",
                            value = notes
                        )
                    }
            }
        }
    }
}


fun reviewScheduleSummaryLine(
    item: ScheduleItemDraft
): String {
    val date = reviewDate(item.scheduleDateMillis)
    val time = when {
        item.startTimeMinutes != null && item.endTimeMinutes != null ->
            " · ${reviewTime(item.startTimeMinutes)} – ${reviewTime(item.endTimeMinutes)}"
        item.startTimeMinutes != null ->
            " · ${reviewTime(item.startTimeMinutes)}"
        else -> ""
    }

    return date + time
}
