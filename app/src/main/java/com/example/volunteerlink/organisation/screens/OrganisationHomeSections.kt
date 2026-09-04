package com.example.volunteerlink.organisation.screens

// FILE OVERVIEW:
/*
 * OrganisationHomeSections contains presentation code for the organisation Home dashboard flow.
 * It focuses on rendering state and forwarding user actions through callbacks/ViewModels,
 * keeping database access and business rules outside the composables where possible.
 */


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.components.OrganisationDivider
import com.example.volunteerlink.organisation.components.OrganisationInfoStrip
import com.example.volunteerlink.organisation.components.OrganisationMetric
import com.example.volunteerlink.organisation.components.OrganisationSectionHeader
import com.example.volunteerlink.organisation.components.OrganisationSectionSurface
import com.example.volunteerlink.organisation.components.OrganisationStatusPill
import com.example.volunteerlink.organisation.home.model.HomeAttentionItem
import com.example.volunteerlink.organisation.home.model.HomeAttentionSeverity
import com.example.volunteerlink.organisation.home.model.HomeAttentionType
import com.example.volunteerlink.organisation.home.model.HomePostItem
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Green Organisation header intentionally retained as VolunteerLink identity. */
@Composable
fun OrganisationHomeHeader(
    organisationName: String,
    nowMillis: Long,
    profileImageUrl: String? = null,
    onAvatarSelected: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkPrimaryGreen)
            .padding(horizontal = 24.dp, vertical = 23.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greetingFor(nowMillis),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = VolunteerLinkSurface.copy(alpha = 0.82f)
            )
            Text(
                text = organisationName.ifBlank { "Organisation" },
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 24.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatCurrentDate(nowMillis),
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 12.sp,
                color = VolunteerLinkSurface.copy(alpha = 0.76f)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
        Surface(
            onClick = onAvatarSelected,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = VolunteerLinkSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = organisationInitials(organisationName),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )

                profileImageUrl
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
                    ?.let { safeUrl ->
                        coil.compose.AsyncImage(
                            model = safeUrl,
                            contentDescription = "Your organisation logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
            }
        }
    }
}

/**
 * One subtle attention surface is kept on Home because alerts genuinely deserve
 * emphasis.  Individual alerts are rows rather than nested cards.
 */
@Composable
fun OrganisationAttentionSection(
    items: List<HomeAttentionItem>,
    onItemClick: (HomeAttentionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visibleItems = if (expanded) items else items.take(3)

    Column(modifier = modifier) {
        OrganisationSectionHeader(
            title = "Needs your attention",
            subtitle = "${items.size} ${if (items.size == 1) "item" else "items"} waiting for action"
        )

        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            visibleItems.forEach { item ->
                OrganisationSectionSurface(contentPadding = 0.dp) {
                    AttentionRow(
                        item = item,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }

            if (items.size > 3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Show less" else "Show ${items.size - 3} more",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkPrimaryGreen
                    )
                    Icon(
                        painter = painterResource(
                            if (expanded) R.drawable.review_chevron_up else R.drawable.review_chevron_down
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = VolunteerLinkPrimaryGreen
                    )
                }
            }
        }
    }
}

@Composable
/**
 * Renders the attention row row used in the organisation Home dashboard flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun AttentionRow(
    item: HomeAttentionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (item.severity) {
        HomeAttentionSeverity.URGENT -> VolunteerLinkError
        HomeAttentionSeverity.WARNING -> VolunteerLinkWarning
        HomeAttentionSeverity.NEEDS_REVIEW,
        HomeAttentionSeverity.REVIEW -> VolunteerLinkInformation
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.primaryTitle(),
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                OrganisationStatusPill(
                    text = item.severityLabel(),
                    color = accent,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            item.contextLine()?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = item.message,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
/**
 * Renders the organisation post summary section section used in the organisation Home dashboard flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun OrganisationPostSummarySection(
    ongoingCount: Int,
    upcomingCount: Int,
    draftCount: Int,
    onViewAllPosts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OrganisationSectionHeader(
            title = "Your activity",
            actionLabel = "View posts",
            onAction = onViewAllPosts
        )
        OrganisationSectionSurface(
            modifier = Modifier.padding(top = 10.dp),
            contentPadding = 14.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OrganisationMetric(ongoingCount.toString(), "Ongoing", Modifier.weight(1f))
                OrganisationMetric(upcomingCount.toString(), "Upcoming", Modifier.weight(1f))
                OrganisationMetric(draftCount.toString(), "Drafts", Modifier.weight(1f))
            }
        }
    }
}

@Composable
/**
 * Renders the UI represented by home section heading for the organisation Home dashboard flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun HomeSectionHeading(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    OrganisationSectionHeader(
        title = title,
        actionLabel = actionLabel,
        onAction = onAction
    )
}

/** Existing function name retained so HomeScreen wiring stays unchanged. */
@Composable
fun OngoingPostCard(
    post: HomePostItem,
    onClick: () -> Unit
) {
    HomePostRow(post = post, ongoing = true, onClick = onClick)
}

@Composable
/**
 * Renders the upcoming post row row used in the organisation Home dashboard flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun UpcomingPostRow(
    post: HomePostItem,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    HomePostRow(post = post, ongoing = false, onClick = onClick)
    if (showDivider) OrganisationDivider()
}

@Composable
/**
 * Renders the home post row row used in the organisation Home dashboard flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun HomePostRow(
    post: HomePostItem,
    ongoing: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = VolunteerLinkSoftGreenSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(post.mode.homeModeDrawable()),
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
                    text = if (ongoing) "ONGOING" else "UPCOMING",
                    color = VolunteerLinkPrimaryGreen,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = post.mode.toHomeModeLabel(),
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkPrimaryGreen
            )

            if (post.mode.equals("HYBRID", true)) {
                val remote = formatDateRange(post.remoteStartDate, post.remoteEndDate)
                val physical = formatDateRange(post.physicalStartDate, post.physicalEndDate)
                Text(
                    text = "Remote · $remote",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary
                )
                Text(
                    text = buildString {
                        append("Physical · $physical")
                        post.physicalLocationName?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = buildString {
                        append(formatDateRange(post.startDate, post.endDate))
                        post.locationName?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
/**
 * Returns the home empty message used by the organisation Home dashboard flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun HomeEmptyMessage(text: String) {
    OrganisationInfoStrip(
        title = "Nothing here right now",
        message = text,
        accent = VolunteerLinkPrimaryGreen
    )
}

/**
 * Renders the home attention item item used in the organisation Home dashboard flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun HomeAttentionItem.attentionKindLabel(): String = when (type) {
    HomeAttentionType.APPLICATIONS_TO_REVIEW -> "APPLICATIONS"
    HomeAttentionType.POST_COMPLETION_REVIEW -> "CLOSE-OUT"
    HomeAttentionType.DRAFT_START_TOO_SOON,
    HomeAttentionType.DRAFT_START_DATE_PASSED -> "DRAFT"
    HomeAttentionType.IMPACT_WEAVE_READY,
    HomeAttentionType.IMPACT_WEAVE_DEADLINE_SOON,
    HomeAttentionType.IMPACT_WEAVE_DEADLINE_PASSED,
    HomeAttentionType.IMPACT_WEAVE_ACTIVITY_PASSED,
    HomeAttentionType.IMPACT_WEAVE_PROGRESS -> "IMPACT WEAVE"
}

/**
 * Renders the home attention item item used in the organisation Home dashboard flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun HomeAttentionItem.severityLabel(): String = when (severity) {
    HomeAttentionSeverity.URGENT -> "URGENT"
    HomeAttentionSeverity.WARNING -> "WARNING"
    HomeAttentionSeverity.NEEDS_REVIEW -> "REVIEW"
    HomeAttentionSeverity.REVIEW -> "REVIEW"
}

/**
 * Renders the home attention item item used in the organisation Home dashboard flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun HomeAttentionItem.primaryTitle(): String = postTitle

/**
 * Renders the home attention item item used in the organisation Home dashboard flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun HomeAttentionItem.contextLine(): String? {
    val detailedContext = listOfNotNull(
        contextLabel?.takeIf { it.isNotBlank() },
        scheduleTitle?.takeIf { it.isNotBlank() }
    ).distinct()

    return if (detailedContext.isNotEmpty()) {
        detailedContext.joinToString(" · ")
    } else {
        attentionKindLabel()
            .lowercase(Locale.US)
            .replaceFirstChar { it.titlecase(Locale.US) }
    }
}

/**
 * Derives the string value used by the organisation Home dashboard flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun String.toHomeModeLabel(): String = when (uppercase(Locale.US)) {
    "PHYSICAL" -> "Physical"
    "REMOTE" -> "Remote"
    "HYBRID" -> "Hybrid"
    else -> lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
}

/**
 * Derives the string value used by the organisation Home dashboard flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun String.homeModeDrawable(): Int = when (uppercase(Locale.US)) {
    "PHYSICAL" -> R.drawable.physical_event
    "REMOTE" -> R.drawable.remote_project
    "HYBRID" -> R.drawable.hybrid_event
    else -> R.drawable.manage
}

/**
 * Derives the organisation initials value used by the organisation Home dashboard flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun organisationInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "O"
        parts.size == 1 -> parts.first().take(2).uppercase(Locale.US)
        else -> "${parts.first().first()}${parts.last().first()}".uppercase(Locale.US)
    }
}

/**
 * Derives the greeting for value used by the organisation Home dashboard flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun greetingFor(nowMillis: Long): String {
    val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

/**
 * Formats the current date used by the organisation Home dashboard flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun formatCurrentDate(nowMillis: Long): String =
    SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(nowMillis))

/**
 * Formats the date range used by the organisation Home dashboard flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun formatDateRange(start: String?, end: String?): String {
    val startText = formatSingleDate(start)
    val endText = formatSingleDate(end)
    return when {
        startText == null && endText == null -> "Date not set"
        startText == null -> endText.orEmpty()
        endText == null || start == end -> startText
        else -> "$startText – $endText"
    }
}

/**
 * Formats the single date used by the organisation Home dashboard flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun formatSingleDate(value: String?): String? {
    val date = parseDatabaseDate(value) ?: return null
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
}

/**
 * Parses the database date used by the organisation Home dashboard flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun parseDatabaseDate(value: String?): Date? {
    if (value.isNullOrBlank()) return null
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
    }.getOrNull()
}
