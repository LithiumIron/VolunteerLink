package com.example.volunteerlink.organisation.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.organisation.manage.model.ManageAttentionItem
import com.example.volunteerlink.organisation.manage.model.ManageAttentionSeverity
import com.example.volunteerlink.organisation.manage.model.ManagePostItem
import com.example.volunteerlink.organisation.manage.model.ManagePostSection
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkCardCornerRadius
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning
import java.util.Locale

private val ManageCardShape = RoundedCornerShape(VolunteerLinkCardCornerRadius)
private val ManageSmallShape = RoundedCornerShape(12.dp)
private val ManagePillShape = RoundedCornerShape(50)

@Composable
internal fun OrganisationManageHeader(
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkPrimaryGreen)
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Text(
            text = title,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkSurface
        )

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
                color = VolunteerLinkSurface.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
internal fun OrganisationManageSubHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkPrimaryGreen)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.back),
                contentDescription = "Back",
                modifier = Modifier.size(22.dp),
                tint = VolunteerLinkSurface
            )
        }

        Text(
            text = title,
            modifier = Modifier.padding(start = 6.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkSurface
        )
    }
}

@Composable
internal fun ManageModuleChoiceCard(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    attentionText: String? = null,
    hasAttention: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = ManageCardShape,
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = ManageSmallShape,
                color = VolunteerLinkSoftGreenSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = VolunteerLinkPrimaryGreen
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 13.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    if (hasAttention) {
                        ManageNotificationDot(
                            modifier = Modifier.padding(start = 7.dp)
                        )
                    }
                }
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = VolunteerLinkTextSecondary
                )

                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        modifier = Modifier.padding(top = 9.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }

                if (!attentionText.isNullOrBlank()) {
                    Text(
                        text = attentionText,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasAttention) {
                            VolunteerLinkError
                        } else {
                            VolunteerLinkTextSecondary
                        }
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.back),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp)
                    .size(18.dp)
                    .rotate(180f),
                tint = VolunteerLinkPrimaryGreen
            )
        }
    }
}

@Composable
internal fun ManagePostSectionSelector(
    selected: ManagePostSection,
    activeCount: Int,
    draftCount: Int,
    reviewCount: Int,
    completedCount: Int,
    activeHasAttention: Boolean,
    draftsHaveAttention: Boolean,
    reviewHasAttention: Boolean,
    onSelected: (ManagePostSection) -> Unit,
    modifier: Modifier = Modifier
) {
    data class TabItem(
        val section: ManagePostSection,
        val label: String,
        val count: Int,
        val hasNotification: Boolean
    )

    val items = listOf(
        TabItem(ManagePostSection.ACTIVE, "Active", activeCount, activeHasAttention),
        TabItem(ManagePostSection.DRAFTS, "Drafts", draftCount, draftsHaveAttention),
        TabItem(ManagePostSection.REVIEW, "Needs Review", reviewCount, reviewHasAttention),
        TabItem(ManagePostSection.COMPLETED, "Completed", completedCount, false)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val isSelected = item.section == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelected(item.section) },
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) {
                        VolunteerLinkPrimaryGreen
                    } else {
                        VolunteerLinkSurface
                    }
                ) {
                    Box {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    VolunteerLinkSurface
                                } else {
                                    VolunteerLinkTextSecondary
                                },
                                maxLines = 1
                            )
                            Text(
                                text = item.count.toString(),
                                modifier = Modifier.padding(top = 1.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    VolunteerLinkSurface.copy(alpha = 0.85f)
                                } else {
                                    VolunteerLinkTextSecondary.copy(alpha = 0.72f)
                                }
                            )
                        }

                        if (item.hasNotification) {
                            ManageNotificationDot(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 5.dp, end = 5.dp),
                                borderColor = if (isSelected) {
                                    VolunteerLinkPrimaryGreen
                                } else {
                                    VolunteerLinkSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ManageActiveGroupHeader(
    title: String,
    count: Int,
    subtitle: String,
    hasAttention: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Surface(
                    modifier = Modifier.padding(start = 7.dp),
                    shape = ManagePillShape,
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
                if (hasAttention) {
                    ManageNotificationDot(Modifier.padding(start = 7.dp))
                }
            }
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 10.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
private fun ManageNotificationDot(
    modifier: Modifier = Modifier,
    borderColor: Color = VolunteerLinkSurface
) {
    Surface(
        modifier = modifier.size(9.dp),
        shape = CircleShape,
        color = VolunteerLinkError,
        border = BorderStroke(1.dp, borderColor)
    ) {}
}

@Composable
internal fun ManageSectionOverview(
    title: String,
    detail: String? = null,
    hasAttention: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = if (hasAttention) FontWeight.SemiBold else FontWeight.Normal,
                color = if (hasAttention) VolunteerLinkWarning else VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
internal fun ManageVolunteerPostCard(
    post: ManagePostItem,
    section: ManagePostSection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = ManageCardShape,
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = ManageSmallShape,
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(post.mode.manageModeDrawable()),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = VolunteerLinkPrimaryGreen
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ManageLifecycleBadge(
                            post = post,
                            section = section
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = post.mode.toManageModeLabel(),
                            modifier = Modifier.padding(start = 8.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextSecondary
                        )
                    }

                    Text(
                        text = post.title,
                        modifier = Modifier.padding(top = 7.dp),
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (post.mode.equals("HYBRID", true)) {
                ManageHybridTimeline(
                    post = post,
                    modifier = Modifier.padding(top = 14.dp)
                )
            } else {
                ManageSingleModeDetails(
                    post = post,
                    modifier = Modifier.padding(top = 13.dp)
                )
            }

            if (post.attentionItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = VolunteerLinkBorderColour)
                Text(
                    text = "Needs attention",
                    modifier = Modifier.padding(top = 11.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )

                val visible = post.attentionItems.take(2)
                visible.forEach { attention ->
                    ManageAttentionLine(
                        attention = attention,
                        modifier = Modifier.padding(top = 9.dp)
                    )
                }

                if (post.attentionItems.size > 2) {
                    val remaining = post.attentionItems.size - 2
                    Text(
                        text = "+$remaining more ${if (remaining == 1) "item" else "items"}",
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageLifecycleBadge(
    post: ManagePostItem,
    section: ManagePostSection
) {
    val (label, container, content, border) = when (section) {
        ManagePostSection.ACTIVE -> when (post.timingState) {
            PostTimingState.ONGOING -> LifecycleBadgeStyle(
                label = "Ongoing",
                container = VolunteerLinkPrimaryGreen,
                content = VolunteerLinkSurface,
                border = VolunteerLinkPrimaryGreen
            )
            PostTimingState.UPCOMING -> LifecycleBadgeStyle(
                label = "Upcoming",
                container = VolunteerLinkInformation.copy(alpha = 0.10f),
                content = VolunteerLinkInformation,
                border = VolunteerLinkInformation.copy(alpha = 0.25f)
            )
            else -> LifecycleBadgeStyle(
                label = "Active",
                container = VolunteerLinkSoftGreenSurface,
                content = VolunteerLinkPrimaryGreen,
                border = VolunteerLinkBorderColour
            )
        }
        ManagePostSection.DRAFTS -> LifecycleBadgeStyle(
            label = "Draft",
            container = VolunteerLinkTextSecondary.copy(alpha = 0.08f),
            content = VolunteerLinkTextSecondary,
            border = VolunteerLinkBorderColour
        )
        ManagePostSection.REVIEW -> LifecycleBadgeStyle(
            label = "Needs review",
            container = VolunteerLinkWarning.copy(alpha = 0.10f),
            content = VolunteerLinkWarning,
            border = VolunteerLinkWarning.copy(alpha = 0.20f)
        )
        ManagePostSection.COMPLETED -> LifecycleBadgeStyle(
            label = "Completed",
            container = VolunteerLinkSoftGreenSurface,
            content = VolunteerLinkPrimaryGreen,
            border = VolunteerLinkBorderColour
        )
    }

    Surface(
        shape = ManagePillShape,
        color = container,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}

private data class LifecycleBadgeStyle(
    val label: String,
    val container: Color,
    val content: Color,
    val border: Color
)

@Composable
private fun ManageSingleModeDetails(
    post: ManagePostItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.calendar),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = VolunteerLinkPrimaryGreen
            )
            Text(
                text = manageDateRange(post.startDate, post.endDate),
                modifier = Modifier.padding(start = 7.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = VolunteerLinkTextSecondary
            )
        }

        if (!post.locationName.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_volunteer_location),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = VolunteerLinkPrimaryGreen
                )
                Text(
                    text = post.locationName,
                    modifier = Modifier.padding(start = 7.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ManageAttentionLine(
    attention: ManageAttentionItem,
    modifier: Modifier = Modifier
) {
    val accent = when (attention.severity) {
        ManageAttentionSeverity.URGENT -> VolunteerLinkError
        ManageAttentionSeverity.WARNING -> VolunteerLinkWarning
        ManageAttentionSeverity.NEEDS_REVIEW -> VolunteerLinkWarning
        ManageAttentionSeverity.REVIEW -> VolunteerLinkPrimaryGreen
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.055f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = ManagePillShape,
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = when (attention.severity) {
                            ManageAttentionSeverity.URGENT -> "Urgent"
                            ManageAttentionSeverity.WARNING -> "Warning"
                            ManageAttentionSeverity.NEEDS_REVIEW -> "Needs Review"
                            ManageAttentionSeverity.REVIEW -> "Review"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }

                Text(
                    text = attention.kindLabel,
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextSecondary
                )
            }

            Text(
                text = attention.title,
                modifier = Modifier.padding(top = 7.dp),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = attention.message,
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
private fun ManageHybridTimeline(
    post: ManagePostItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = VolunteerLinkSoftGreenSurface.copy(alpha = 0.62f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Hybrid schedule",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextSecondary
            )

            ManageHybridPeriod(
                iconRes = R.drawable.remote_project,
                label = "Remote",
                timing = post.remoteTimingState,
                startDate = post.remoteStartDate,
                endDate = post.remoteEndDate,
                modifier = Modifier.padding(top = 9.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 9.dp),
                color = VolunteerLinkBorderColour
            )

            ManageHybridPeriod(
                iconRes = R.drawable.physical_event,
                label = "Physical",
                timing = post.physicalTimingState,
                startDate = post.physicalStartDate,
                endDate = post.physicalEndDate,
                location = post.physicalLocationName
            )
        }
    }
}

@Composable
private fun ManageHybridPeriod(
    @DrawableRes iconRes: Int,
    label: String,
    timing: PostTimingState?,
    startDate: String?,
    endDate: String?,
    modifier: Modifier = Modifier,
    location: String? = null
) {
    if (startDate.isNullOrBlank() || endDate.isNullOrBlank()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = RoundedCornerShape(9.dp),
            color = VolunteerLinkSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = VolunteerLinkPrimaryGreen
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                HybridPeriodStateBadge(
                    timing = timing,
                    modifier = Modifier.padding(start = 7.dp)
                )
            }
            Text(
                text = manageDateRange(startDate, endDate),
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = VolunteerLinkTextSecondary
            )
            if (!location.isNullOrBlank()) {
                Text(
                    text = location,
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HybridPeriodStateBadge(
    timing: PostTimingState?,
    modifier: Modifier = Modifier
) {
    val label = timing.manageTimingLabel()
    val container = when (timing) {
        PostTimingState.ONGOING -> VolunteerLinkPrimaryGreen
        PostTimingState.UPCOMING -> VolunteerLinkSurface
        PostTimingState.PAST, null -> VolunteerLinkTextSecondary.copy(alpha = 0.08f)
    }
    val content = when (timing) {
        PostTimingState.ONGOING -> VolunteerLinkSurface
        PostTimingState.UPCOMING -> VolunteerLinkPrimaryGreen
        PostTimingState.PAST, null -> VolunteerLinkTextSecondary
    }
    val border = when (timing) {
        PostTimingState.ONGOING -> VolunteerLinkPrimaryGreen
        PostTimingState.UPCOMING -> VolunteerLinkPrimaryGreen.copy(alpha = 0.28f)
        PostTimingState.PAST, null -> VolunteerLinkBorderColour
    }

    Surface(
        modifier = modifier,
        shape = ManagePillShape,
        color = container,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}

@Composable
internal fun ManageEmptySectionMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ManageCardShape,
        color = VolunteerLinkSoftGreenSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@DrawableRes
private fun String.manageModeDrawable(): Int = when (uppercase(Locale.US)) {
    "PHYSICAL" -> R.drawable.physical_event
    "REMOTE" -> R.drawable.remote_project
    // Dedicated Manage icon avoids the filled-square hybrid resource shown in the old card.
    "HYBRID" -> R.drawable.manage_hybrid
    else -> R.drawable.manage
}

private fun String.toManageModeLabel(): String = when (uppercase(Locale.US)) {
    "PHYSICAL" -> "Physical"
    "REMOTE" -> "Remote"
    "HYBRID" -> "Hybrid"
    else -> this
}

private fun PostTimingState?.manageTimingLabel(): String = when (this) {
    PostTimingState.ONGOING -> "Ongoing"
    PostTimingState.UPCOMING -> "Upcoming"
    PostTimingState.PAST -> "Past"
    null -> "Scheduled"
}

private fun manageDateRange(start: String?, end: String?): String {
    if (start.isNullOrBlank() && end.isNullOrBlank()) return "Date not set"
    if (start.isNullOrBlank()) return manageShortDate(end)
    if (end.isNullOrBlank() || start == end) return manageShortDate(start)

    val startParts = start.split("-")
    val endParts = end.split("-")
    if (startParts.size == 3 && endParts.size == 3 && startParts[1] == endParts[1]) {
        return "${startParts[2].toIntOrNull() ?: startParts[2]}–${manageShortDate(end)}"
    }
    return "${manageShortDate(start)} – ${manageShortDate(end)}"
}

private fun manageShortDate(value: String?): String {
    if (value.isNullOrBlank()) return "Date not set"
    val parts = value.split("-")
    if (parts.size != 3) return value
    val month = when (parts[1]) {
        "01" -> "Jan"
        "02" -> "Feb"
        "03" -> "Mar"
        "04" -> "Apr"
        "05" -> "May"
        "06" -> "Jun"
        "07" -> "Jul"
        "08" -> "Aug"
        "09" -> "Sep"
        "10" -> "Oct"
        "11" -> "Nov"
        "12" -> "Dec"
        else -> return value
    }
    val day = parts[2].toIntOrNull() ?: return value
    return "$day $month"
}
