package com.example.volunteerlink.organisation.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.organisation.home.model.HomeAttentionItem
import com.example.volunteerlink.organisation.home.model.HomeAttentionSeverity
import com.example.volunteerlink.organisation.home.model.HomeAttentionType
import com.example.volunteerlink.organisation.home.model.HomePostItem
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkCardCornerRadius
import com.example.volunteerlink.ui.theme.VolunteerLinkError
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

private val HomeCardShape = RoundedCornerShape(VolunteerLinkCardCornerRadius)
private val HomeSmallShape = RoundedCornerShape(12.dp)

@Composable
internal fun OrganisationHomeHeader(
    organisationName: String,
    nowMillis: Long
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
            }
        }
    }
}

@Composable
internal fun OrganisationAttentionSection(
    items: List<HomeAttentionItem>,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visibleItems = if (expanded) items else items.take(3)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        HomeSectionHeading(title = "Needs Your Attention")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = HomeCardShape,
            color = VolunteerLinkSurface,
            border = BorderStroke(1.dp, VolunteerLinkBorderColour),
            shadowElevation = 1.dp
        ) {
            Column {
                visibleItems.forEach { item ->
                    AttentionRow(item = item)
                }

                if (items.size > 3) {
                    val hiddenCount = items.size - 3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (expanded) {
                                "Show less"
                            } else {
                                "Show $hiddenCount more"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VolunteerLinkPrimaryGreen
                        )

                        Icon(
                            painter = painterResource(
                                if (expanded) {
                                    R.drawable.review_chevron_up
                                } else {
                                    R.drawable.review_chevron_down
                                }
                            ),
                            contentDescription = if (expanded) {
                                "Show fewer attention items"
                            } else {
                                "Show more attention items"
                            },
                            modifier = Modifier.size(16.dp),
                            tint = VolunteerLinkPrimaryGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttentionRow(item: HomeAttentionItem) {
    val accent = when (item.severity) {
        HomeAttentionSeverity.URGENT -> VolunteerLinkError
        HomeAttentionSeverity.WARNING -> VolunteerLinkWarning
        HomeAttentionSeverity.NEEDS_REVIEW -> VolunteerLinkWarning
        HomeAttentionSeverity.REVIEW -> VolunteerLinkPrimaryGreen
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.055f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.attentionKindLabel(),
                fontSize = 10.sp,
                letterSpacing = 0.7.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextSecondary
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = accent.copy(alpha = 0.10f)
            ) {
                Text(
                    text = item.severityLabel(),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }

        Text(
            text = item.primaryTitle(),
            modifier = Modifier.padding(top = 7.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        item.contextLine()?.let { context ->
            Text(
                text = context,
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 11.sp,
                color = VolunteerLinkTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = item.message,
            modifier = Modifier.padding(top = 6.dp),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = VolunteerLinkTextSecondary
        )
        }
    }
}

@Composable
internal fun OrganisationPostSummarySection(
    ongoingCount: Int,
    upcomingCount: Int,
    draftCount: Int,
    onViewAllPosts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        HomeSectionHeading(
            title = "Your Posts",
            actionLabel = "View all",
            onAction = onViewAllPosts
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = HomeCardShape,
            color = VolunteerLinkSoftGreenSurface,
            border = BorderStroke(1.dp, VolunteerLinkBorderColour)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PostSummaryValue(
                    modifier = Modifier.weight(1f),
                    value = ongoingCount,
                    label = "Ongoing"
                )
                SummaryDivider()
                PostSummaryValue(
                    modifier = Modifier.weight(1f),
                    value = upcomingCount,
                    label = "Upcoming"
                )
                SummaryDivider()
                PostSummaryValue(
                    modifier = Modifier.weight(1f),
                    value = draftCount,
                    label = "Drafts"
                )
            }
        }
    }
}

@Composable
private fun PostSummaryValue(
    modifier: Modifier,
    value: Int,
    label: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkPrimaryGreen
        )
        Text(
            text = label,
            modifier = Modifier.padding(top = 2.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = VolunteerLinkTextSecondary
        )
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(VolunteerLinkBorderColour)
    )
}

@Composable
internal fun HomeSectionHeading(
    title: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        if (actionLabel != null) {
            Text(
                text = actionLabel,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
        }
    }
}

@Composable
internal fun OngoingPostCard(post: HomePostItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = HomeCardShape,
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = HomeSmallShape,
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(post.mode.homeModeDrawable()),
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = VolunteerLinkPrimaryGreen
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 11.dp)
                ) {
                    Text(
                        text = post.title,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = post.mode.toHomeModeLabel(),
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }

            if (post.mode.equals("HYBRID", ignoreCase = true)) {
                HybridTimelineBlock(
                    post = post,
                    modifier = Modifier.padding(top = 13.dp)
                )
            } else {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.calendar),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = VolunteerLinkTextSecondary
                    )
                    Text(
                        text = formatDateRange(post.startDate, post.endDate),
                        modifier = Modifier.padding(start = 6.dp),
                        fontSize = 12.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                if (!post.locationName.isNullOrBlank()) {
                    Text(
                        text = post.locationName,
                        modifier = Modifier.padding(top = 5.dp),
                        fontSize = 12.sp,
                        color = VolunteerLinkTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HybridTimelineBlock(
    post: HomePostItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        HybridPeriodRow(
            label = "Remote",
            timingState = post.remoteTimingState,
            startDate = post.remoteStartDate,
            endDate = post.remoteEndDate
        )

        HybridPeriodRow(
            label = "Physical",
            timingState = post.physicalTimingState,
            startDate = post.physicalStartDate,
            endDate = post.physicalEndDate,
            locationName = post.physicalLocationName
        )
    }
}

@Composable
private fun HybridPeriodRow(
    label: String,
    timingState: PostTimingState?,
    startDate: String?,
    endDate: String?,
    locationName: String? = null
) {
    if (startDate.isNullOrBlank() || endDate.isNullOrBlank()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(VolunteerLinkPrimaryGreen)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 9.dp)
        ) {
            Text(
                text = buildString {
                    append(label)
                    timingState.hybridPeriodStatus(startDate)?.let { status ->
                        append(" · ")
                        append(status)
                    }
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextPrimary
            )

            Text(
                text = buildString {
                    append(formatDateRange(startDate, endDate))
                    if (!locationName.isNullOrBlank()) {
                        append(" · ")
                        append(locationName)
                    }
                },
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = VolunteerLinkTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun UpcomingPostRow(
    post: HomePostItem,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        DateBadge(date = post.startDate)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = post.title,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (post.mode.equals("HYBRID", ignoreCase = true)) {
                Text(
                    text = "Hybrid",
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = VolunteerLinkPrimaryGreen
                )

                CompactHybridPeriodLine(
                    label = "Remote",
                    timingState = post.remoteTimingState,
                    startDate = post.remoteStartDate,
                    endDate = post.remoteEndDate,
                    modifier = Modifier.padding(top = 4.dp)
                )
                CompactHybridPeriodLine(
                    label = "Physical",
                    timingState = post.physicalTimingState,
                    startDate = post.physicalStartDate,
                    endDate = post.physicalEndDate,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                Text(
                    text = buildString {
                        append(post.mode.toHomeModeLabel())
                        val range = formatDateRange(post.startDate, post.endDate)
                        if (range.isNotBlank()) {
                            append(" · ")
                            append(range)
                        }
                    },
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 11.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp, start = 60.dp),
            color = VolunteerLinkBorderColour
        )
    }
}

@Composable
private fun CompactHybridPeriodLine(
    label: String,
    timingState: PostTimingState?,
    startDate: String?,
    endDate: String?,
    modifier: Modifier = Modifier
) {
    if (startDate.isNullOrBlank() || endDate.isNullOrBlank()) return

    Text(
        text = buildString {
            append(label)
            append(" · ")
            append(formatDateRange(startDate, endDate))
            timingState.hybridPeriodStatus(startDate)?.let { status ->
                append(" · ")
                append(status)
            }
        },
        modifier = modifier,
        fontSize = 10.sp,
        color = VolunteerLinkTextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DateBadge(date: String?) {
    val parts = dateBadgeParts(date)

    Surface(
        modifier = Modifier.size(48.dp),
        shape = HomeSmallShape,
        color = VolunteerLinkSoftGreenSurface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = parts.first,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
            Text(
                text = parts.second,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
internal fun HomeEmptyMessage(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = HomeSmallShape,
        color = VolunteerLinkSoftGreenSurface.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = VolunteerLinkTextSecondary
        )
    }
}

@Composable
private fun HomeDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = VolunteerLinkBorderColour
    )
}

private fun HomeAttentionItem.attentionKindLabel(): String {
    return when (type) {
        HomeAttentionType.APPLICATIONS_TO_REVIEW -> "APPLICATION"
        HomeAttentionType.POST_COMPLETION_REVIEW -> "CLOSE-OUT"
        HomeAttentionType.DRAFT_START_TOO_SOON,
        HomeAttentionType.DRAFT_START_DATE_PASSED -> "DRAFT"
        HomeAttentionType.TRAINING_DETAILS_WARNING,
        HomeAttentionType.TRAINING_DETAILS_URGENT,
        HomeAttentionType.TRAINING_OUTDATED -> "TRAINING"
    }
}

private fun HomeAttentionItem.severityLabel(): String {
    return when (severity) {
        HomeAttentionSeverity.URGENT -> "Urgent"
        HomeAttentionSeverity.WARNING -> "Warning"
        HomeAttentionSeverity.NEEDS_REVIEW -> "Needs Review"
        HomeAttentionSeverity.REVIEW -> "Review"
    }
}

private fun HomeAttentionItem.primaryTitle(): String {
    return when (type) {
        HomeAttentionType.TRAINING_DETAILS_WARNING,
        HomeAttentionType.TRAINING_DETAILS_URGENT,
        HomeAttentionType.TRAINING_OUTDATED -> scheduleTitle ?: postTitle
        else -> postTitle
    }
}

private fun HomeAttentionItem.contextLine(): String? {
    return when (type) {
        HomeAttentionType.APPLICATIONS_TO_REVIEW -> contextLabel ?: "Application review"
        HomeAttentionType.POST_COMPLETION_REVIEW -> contextLabel ?: "Post-event close-out"

        HomeAttentionType.TRAINING_DETAILS_WARNING,
        HomeAttentionType.TRAINING_DETAILS_URGENT,
        HomeAttentionType.TRAINING_OUTDATED -> buildString {
            append(postTitle)
            formatSingleDate(scheduleDate)?.let { date ->
                append(" · ")
                append(date)
            }
        }

        HomeAttentionType.DRAFT_START_TOO_SOON,
        HomeAttentionType.DRAFT_START_DATE_PASSED -> null
    }
}

private fun PostTimingState?.hybridPeriodStatus(startDate: String?): String? {
    return when (this) {
        PostTimingState.ONGOING -> "Ongoing"
        PostTimingState.UPCOMING -> formatSingleDate(startDate)?.let { "Starts $it" }
        PostTimingState.PAST -> "Ended"
        null -> null
    }
}

private fun String.toHomeModeLabel(): String {
    return when (uppercase(Locale.US)) {
        "PHYSICAL" -> "Physical"
        "REMOTE" -> "Remote"
        "HYBRID" -> "Hybrid"
        else -> lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
    }
}

private fun String.homeModeDrawable(): Int {
    return when (uppercase(Locale.US)) {
        "REMOTE" -> R.drawable.remote_project
        "HYBRID" -> R.drawable.hybrid_event
        else -> R.drawable.physical_event
    }
}

private fun organisationInitials(name: String): String {
    val words = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    if (words.isEmpty()) return "O"
    if (words.size == 1) return words.first().take(2).uppercase(Locale.US)

    return (words[0].take(1) + words[1].take(1)).uppercase(Locale.US)
}

private fun greetingFor(nowMillis: Long): String {
    val hour = Calendar.getInstance().apply {
        timeInMillis = nowMillis
    }.get(Calendar.HOUR_OF_DAY)

    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun formatCurrentDate(nowMillis: Long): String {
    return SimpleDateFormat("EEEE, d MMMM", Locale.US)
        .format(Date(nowMillis))
}

private fun formatDateRange(start: String?, end: String?): String {
    val startDate = parseDatabaseDate(start) ?: return ""
    val endDate = parseDatabaseDate(end) ?: startDate

    val startCalendar = Calendar.getInstance().apply { time = startDate }
    val endCalendar = Calendar.getInstance().apply { time = endDate }

    val sameDay = startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR) &&
        startCalendar.get(Calendar.DAY_OF_YEAR) == endCalendar.get(Calendar.DAY_OF_YEAR)

    if (sameDay) {
        return SimpleDateFormat("d MMM", Locale.US).format(startDate)
    }

    val sameMonth = startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR) &&
        startCalendar.get(Calendar.MONTH) == endCalendar.get(Calendar.MONTH)

    return if (sameMonth) {
        val startDay = SimpleDateFormat("d", Locale.US).format(startDate)
        val endPart = SimpleDateFormat("d MMM", Locale.US).format(endDate)
        "$startDay–$endPart"
    } else {
        val formatter = SimpleDateFormat("d MMM", Locale.US)
        "${formatter.format(startDate)} – ${formatter.format(endDate)}"
    }
}

private fun formatSingleDate(value: String?): String? {
    val parsed = parseDatabaseDate(value) ?: return null
    return SimpleDateFormat("d MMM", Locale.US).format(parsed)
}

private fun dateBadgeParts(date: String?): Pair<String, String> {
    val parsed = parseDatabaseDate(date) ?: return "--" to "DATE"
    return SimpleDateFormat("d", Locale.US).format(parsed) to
        SimpleDateFormat("MMM", Locale.US).format(parsed).uppercase(Locale.US)
}

private fun parseDatabaseDate(value: String?): Date? {
    if (value.isNullOrBlank()) return null
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }.parse(value)
    }.getOrNull()
}
