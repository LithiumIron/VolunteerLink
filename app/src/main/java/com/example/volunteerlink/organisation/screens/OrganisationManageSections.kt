package com.example.volunteerlink.organisation.screens

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements the Organisation UI associated with Organisation Manage Sections.
//
// The composable layer is responsible for layout, interaction and displaying loading/error/validation state;
// business rules and persistence are delegated to ViewModels/repositories.
//
// This separation makes it clear during maintenance which code changes appearance versus which code changes real
// server data.
//
// Where the screen displays cached information, server-changing actions remain disabled or routed through a fresh
// authenticated repository operation.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.volunteerlink.organisation.components.OrganisationDivider
import com.example.volunteerlink.organisation.components.OrganisationInfoStrip
import com.example.volunteerlink.organisation.components.OrganisationListRow
import com.example.volunteerlink.organisation.components.OrganisationSectionHeader
import com.example.volunteerlink.organisation.components.OrganisationSectionSurface
import com.example.volunteerlink.organisation.components.OrganisationStatusPill
import com.example.volunteerlink.organisation.manage.model.ManagePostItem
import com.example.volunteerlink.organisation.manage.model.ManagePostSection
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning
import java.text.SimpleDateFormat
import java.util.Locale

/** Green Manage header retained exactly as the Organisation visual anchor. */
@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationManageHeader
 *
 * Renders the reusable Organisation Manage Header portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun OrganisationManageHeader(
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
/**
 * Renders the organisation manage sub header header used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — OrganisationManageSubHeader
 *
 * Renders the reusable Organisation Manage Sub Header portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun OrganisationManageSubHeader(
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

/**
 * Existing name retained to avoid touching navigation callers.  It now renders
 * as a native management row instead of a large module card.
 */
@Composable
/**
 * DETAILED BEHAVIOUR — ManageModuleChoiceCard
 *
 * Renders the reusable Manage Module Choice Card portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ManageModuleChoiceCard(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    attentionText: String? = null,
    hasAttention: Boolean = false,
    onClick: () -> Unit
) {
    OrganisationSectionSurface(
        modifier = modifier,
        contentPadding = 0.dp
    ) {
        OrganisationListRow(
            title = title,
            subtitle = summary ?: description,
            supportingText = if (hasAttention && !attentionText.isNullOrBlank()) {
                attentionText
            } else {
                null
            },
            iconRes = iconRes,
            statusText = if (hasAttention) "ACTION" else null,
            statusColor = if (hasAttention) VolunteerLinkWarning else VolunteerLinkPrimaryGreen,
            onClick = onClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
/**
 * Renders the UI represented by manage post section selector for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ManagePostSectionSelector
 *
 * Renders the reusable Manage Post Section Selector portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ManagePostSectionSelector(
    selected: ManagePostSection,
    activeCount: Int,
    draftCount: Int,
    reviewCount: Int,
    completedCount: Int,
    partnerCount: Int,
    activeHasAttention: Boolean,
    draftsHaveAttention: Boolean,
    reviewHasAttention: Boolean,
    onSelected: (ManagePostSection) -> Unit,
    modifier: Modifier = Modifier
) {
    /**
     * Holds the values represented by tab item as one strongly typed model.
     * It supports the Manage Post presentation layer without adding backend responsibilities to the screen.
     */
    /**
     * DETAILED DECLARATION — TabItem
     *
     * Domain/UI type for Tab Item used by the Organisation module.
     *
     * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-
     * typed maps.
     */
    data class TabItem(
        val section: ManagePostSection,
        val label: String,
        val count: Int,
        val attention: Boolean
    )

    val tabs = listOf(
        TabItem(ManagePostSection.ACTIVE, "Active", activeCount, activeHasAttention),
        TabItem(ManagePostSection.DRAFTS, "Drafts", draftCount, draftsHaveAttention),
        TabItem(ManagePostSection.REVIEW, "Needs Review", reviewCount, reviewHasAttention),
        TabItem(ManagePostSection.COMPLETED, "Completed", completedCount, false),
        TabItem(ManagePostSection.PARTNERSHIPS, "Partnerships", partnerCount, false)
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs, key = { it.section.name }) { item ->
            val isSelected = item.section == selected
            Surface(
                modifier = Modifier.clickable { onSelected(item.section) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) VolunteerLinkPrimaryGreen else VolunteerLinkSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) VolunteerLinkPrimaryGreen else com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) VolunteerLinkSurface else VolunteerLinkTextSecondary
                    )
                    Text(
                        text = item.count.toString(),
                        modifier = Modifier.padding(start = 7.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            VolunteerLinkSurface.copy(alpha = 0.84f)
                        } else {
                            VolunteerLinkPrimaryGreen
                        }
                    )
                    if (item.attention) {
                        Surface(
                            modifier = Modifier
                                .padding(start = 7.dp)
                                .size(7.dp),
                            shape = CircleShape,
                            color = if (isSelected) VolunteerLinkSurface else VolunteerLinkError
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
/**
 * Renders the manage active group header header used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ManageActiveGroupHeader
 *
 * Renders the reusable Manage Active Group Header portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ManageActiveGroupHeader(
    title: String,
    count: Int,
    subtitle: String,
    hasAttention: Boolean,
    modifier: Modifier = Modifier
) {
    OrganisationSectionHeader(
        title = "$title · $count",
        subtitle = subtitle,
        modifier = modifier
    )
    if (hasAttention) {
        Text(
            text = "Some posts need attention",
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = VolunteerLinkWarning
        )
    }
}

@Composable
/**
 * Renders the UI represented by manage section overview for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ManageSectionOverview
 *
 * Renders the reusable Manage Section Overview portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ManageSectionOverview(
    title: String,
    detail: String? = null,
    hasAttention: Boolean = false,
    modifier: Modifier = Modifier
) {
    OrganisationSectionHeader(
        title = title,
        subtitle = detail,
        modifier = modifier
    )
    if (hasAttention) {
        Text(
            text = "Action may be required",
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = VolunteerLinkWarning
        )
    }
}

/** Flat post row used throughout the lifecycle list. */
@Composable
/**
 * DETAILED BEHAVIOUR — ManageVolunteerPostCard
 *
 * Renders the reusable Manage Volunteer Post Card portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ManageVolunteerPostCard(
    post: ManagePostItem,
    section: ManagePostSection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeLabel = post.mode.toManageModeLabel()
    val lifecycle = manageLifecycleLabel(post, section)
    val lifecycleColor = manageLifecycleColor(post, section)
    val timeline = manageTimelineText(post)
    val firstAttention = post.attentionItems.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VolunteerLinkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (post.isPartnerPost) Modifier else Modifier.clickable(onClick = onClick)
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
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
                    .padding(start = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.title,
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    OrganisationStatusPill(
                        text = lifecycle,
                        color = lifecycleColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (post.isPartnerPost) {
                    Text(
                        text = "Impact Weave partner · ${post.ownerOrganisationName.orEmpty()}",
                        modifier = Modifier.padding(top = 5.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkPrimaryGreen
                    )
                    if (post.description.isNotBlank()) {
                        Text(
                            text = post.description,
                            modifier = Modifier.padding(top = 4.dp),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = VolunteerLinkTextSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!post.isPartnerPost && !post.contributionSummary.isNullOrBlank()) {
                    Text(
                        text = "Created from Impact Weave",
                        modifier = Modifier.padding(top = 5.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }

                if (post.mode.equals("HYBRID", true)) {
                    Text(
                        text = modeLabel,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextSecondary
                    )
                    ManageHybridPhaseLine(
                        label = "Physical",
                        timing = post.physicalTimingState,
                        dateRange = manageDateRange(post.physicalStartDate, post.physicalEndDate),
                        modifier = Modifier.padding(top = 3.dp)
                    )
                    ManageHybridPhaseLine(
                        label = "Remote",
                        timing = post.remoteTimingState,
                        dateRange = manageDateRange(post.remoteStartDate, post.remoteEndDate),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Text(
                        text = "$modeLabel · $timeline",
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = VolunteerLinkTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (post.mode.equals("HYBRID", true)) {
                    post.physicalLocationName?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(top = 2.dp),
                            fontSize = 12.sp,
                            color = VolunteerLinkTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    post.locationName?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(top = 2.dp),
                            fontSize = 12.sp,
                            color = VolunteerLinkTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (firstAttention != null) {
                    Text(
                        text = firstAttention.title,
                        modifier = Modifier.padding(top = 6.dp),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (firstAttention.severity.name) {
                            "URGENT" -> VolunteerLinkError
                            "WARNING" -> VolunteerLinkWarning
                            "NEEDS_REVIEW", "REVIEW" -> VolunteerLinkInformation
                            else -> VolunteerLinkTextSecondary
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.attentionItems.size > 1) {
                        Text(
                            text = "+${post.attentionItems.size - 1} more",
                            modifier = Modifier.padding(top = 2.dp),
                            fontSize = 12.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }
                post.contributionSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                    Text(
                        text = if (post.isPartnerPost) {
                            "Your contribution: $summary"
                        } else {
                            "Partner support: $summary"
                        },
                        modifier = Modifier.padding(top = 6.dp),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = VolunteerLinkTextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.isPartnerPost) {
                        Text(
                            text = "Read-only partner view",
                            modifier = Modifier.padding(top = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }
            }

            if (!post.isPartnerPost) {
                Icon(
                    painter = painterResource(R.drawable.back),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp, top = 3.dp)
                        .size(17.dp)
                        .rotate(180f),
                    tint = VolunteerLinkTextSecondary.copy(alpha = 0.7f)
                )
            }
        }
        OrganisationDivider(modifier = Modifier.padding(start = 66.dp, end = 12.dp))
    }
}

@Composable
/**
 * Returns the manage empty section message used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ManageEmptySectionMessage
 *
 * Renders the reusable Manage Empty Section Message portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ManageEmptySectionMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    OrganisationInfoStrip(
        title = title,
        message = message,
        modifier = modifier,
        accent = VolunteerLinkPrimaryGreen
    )
}

@Composable
/**
 * Renders the UI represented by manage hybrid phase line for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ManageHybridPhaseLine
 *
 * Handles the Compose/UI responsibility for manage hybrid phase line.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun ManageHybridPhaseLine(
    label: String,
    timing: PostTimingState?,
    dateRange: String,
    modifier: Modifier = Modifier
) {
    val status = when (timing) {
        PostTimingState.ONGOING -> "Ongoing"
        PostTimingState.UPCOMING -> "Upcoming"
        PostTimingState.PAST -> "Ended"
        null -> "Not set"
    }
    Text(
        text = "$label · $status · $dateRange",
        modifier = modifier,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = if (timing == PostTimingState.PAST) {
            VolunteerLinkInformation
        } else {
            VolunteerLinkTextSecondary
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Derives the string value used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — manageModeDrawable
 *
 * Handles the Compose/UI responsibility for manage mode drawable.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun String.manageModeDrawable(): Int = when (uppercase(Locale.US)) {
    "PHYSICAL" -> R.drawable.physical_event
    "REMOTE" -> R.drawable.remote_project
    "HYBRID" -> R.drawable.hybrid_event
    else -> R.drawable.manage
}

/**
 * Derives the string value used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — toManageModeLabel
 *
 * Handles the Compose/UI responsibility for to manage mode label.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun String.toManageModeLabel(): String = when (uppercase(Locale.US)) {
    "PHYSICAL" -> "Physical"
    "REMOTE" -> "Remote"
    "HYBRID" -> "Hybrid"
    else -> lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
}

/**
 * Returns the manage lifecycle label used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — manageLifecycleLabel
 *
 * Handles the Compose/UI responsibility for manage lifecycle label.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun manageLifecycleLabel(post: ManagePostItem, section: ManagePostSection): String = when (section) {
    ManagePostSection.DRAFTS -> "DRAFT"
    ManagePostSection.REVIEW -> "REVIEW"
    ManagePostSection.COMPLETED -> "COMPLETED"
    ManagePostSection.PARTNERSHIPS -> post.databaseStatus
    ManagePostSection.ACTIVE -> when (post.timingState) {
        PostTimingState.ONGOING -> "ONGOING"
        PostTimingState.UPCOMING -> "UPCOMING"
        PostTimingState.PAST -> "ENDED"
        null -> post.databaseStatus
    }
}

/**
 * Derives the manage lifecycle color value used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — manageLifecycleColor
 *
 * Handles the Compose/UI responsibility for manage lifecycle color.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun manageLifecycleColor(post: ManagePostItem, section: ManagePostSection): Color = when (section) {
    ManagePostSection.REVIEW -> VolunteerLinkInformation
    ManagePostSection.COMPLETED -> VolunteerLinkPrimaryGreen
    ManagePostSection.DRAFTS -> VolunteerLinkTextSecondary
    ManagePostSection.PARTNERSHIPS -> VolunteerLinkPrimaryGreen
    ManagePostSection.ACTIVE -> when (post.timingState) {
        PostTimingState.PAST -> VolunteerLinkWarning
        else -> VolunteerLinkPrimaryGreen
    }
}

/**
 * Returns the manage timeline text used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — manageTimelineText
 *
 * Handles the Compose/UI responsibility for manage timeline text.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun manageTimelineText(post: ManagePostItem): String {
    return if (post.mode.equals("HYBRID", true)) {
        val remote = manageDateRange(post.remoteStartDate, post.remoteEndDate)
        val physical = manageDateRange(post.physicalStartDate, post.physicalEndDate)
        "Remote $remote · Physical $physical"
    } else {
        manageDateRange(post.startDate, post.endDate)
    }
}

/**
 * Returns the manage date range value required by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — manageDateRange
 *
 * Handles the Compose/UI responsibility for manage date range.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun manageDateRange(start: String?, end: String?): String {
    val startText = manageShortDate(start)
    val endText = manageShortDate(end)
    return when {
        startText.isBlank() && endText.isBlank() -> "Date not set"
        startText.isBlank() -> endText
        endText.isBlank() || start == end -> startText
        else -> "$startText – $endText"
    }
}

/**
 * Returns the manage short date value required by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — manageShortDate
 *
 * Handles the Compose/UI responsibility for manage short date.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 *
 * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without leaving
 * the UI in an assumed-success state.
 */
fun manageShortDate(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return runCatching {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
        if (date == null) value else SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
    }.getOrDefault(value)
}
