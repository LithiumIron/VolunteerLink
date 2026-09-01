package com.example.volunteerlink.organisation.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.components.OrganisationDivider
import com.example.volunteerlink.organisation.components.OrganisationInfoStrip
import com.example.volunteerlink.organisation.components.OrganisationListRow
import com.example.volunteerlink.organisation.components.OrganisationMessageButton
import com.example.volunteerlink.organisation.components.OrganisationMetric
import com.example.volunteerlink.organisation.components.OrganisationSectionHeader
import com.example.volunteerlink.organisation.components.OrganisationSectionSurface
import com.example.volunteerlink.organisation.components.OrganisationStatusPill
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceDay
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalAttendance
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementRole
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmission
import com.example.volunteerlink.organisation.manage.model.PostManagementScheduleItem
import com.example.volunteerlink.organisation.manage.model.PostManagementVolunteerAttendanceSummary
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkCardCornerRadius
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSuccess
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning
import java.util.Locale

internal enum class PostManagementTab {
    OVERVIEW,
    PEOPLE,
    REVIEW
}

internal enum class PostManagementPeopleTab {
    APPLICANTS,
    VOLUNTEERS
}

private val PostManagementCardShape = RoundedCornerShape(VolunteerLinkCardCornerRadius)
private val PostManagementSmallShape = RoundedCornerShape(10.dp)
private val PostManagementPillShape = RoundedCornerShape(50)

@Composable
internal fun PostManagementTopBar(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    showEdit: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkPrimaryGreen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
            text = "Manage Post",
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkSurface
        )

        if (showEdit) {
            TextButton(onClick = onEdit) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = VolunteerLinkSurface
                )
                Text(
                    text = "Edit",
                    modifier = Modifier.padding(start = 5.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkSurface
                )
            }
        }
    }
}

@Composable
internal fun PostManagementSummaryCard(
    post: PostManagementPost,
    modifier: Modifier = Modifier
) {
    OrganisationSectionSurface(
        modifier = modifier.padding(vertical = 2.dp),
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(13.dp),
                color = VolunteerLinkSoftGreenSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(post.mode.postManagementModeDrawable()),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = VolunteerLinkPrimaryGreen
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = post.title,
                    fontSize = 21.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    PostManagementLifecycleBadge(post)
                    OrganisationStatusPill(
                        text = post.mode.toPostManagementModeLabel().uppercase(Locale.US),
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }
        }

        if (!post.category.isNullOrBlank()) {
            Text(
                text = post.category.toReadableDatabaseLabel(),
                modifier = Modifier.padding(top = 14.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
        }

        Text(
            text = post.description,
            modifier = Modifier.padding(top = 5.dp),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = VolunteerLinkTextSecondary,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        val phaseLines = when {
            post.mode.equals("HYBRID", true) -> listOf(
                Triple("Remote", post.remote?.startDate to post.remote?.effectiveEndDate, null),
                Triple("Physical", post.physical?.startDate to post.physical?.endDate, post.physical?.locationName)
            )
            post.mode.equals("PHYSICAL", true) -> listOf(
                Triple("Physical", post.physical?.startDate to post.physical?.endDate, post.physical?.locationName)
            )
            else -> listOf(
                Triple("Remote", post.remote?.startDate to post.remote?.effectiveEndDate, null)
            )
        }

        OrganisationDivider(modifier = Modifier.padding(top = 16.dp))
        phaseLines.forEachIndexed { index, phase ->
            val dateText = postManagementDateRange(phase.second.first, phase.second.second)
            Text(
                text = buildString {
                    append(phase.first)
                    append(" · ")
                    append(dateText)
                    phase.third?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                },
                modifier = Modifier.padding(top = if (index == 0) 12.dp else 7.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
internal fun PostManagementMainTabs(
    selected: PostManagementTab,
    pendingApplicantCount: Int,
    showReviewTab: Boolean,
    onSelected: (PostManagementTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = buildList {
        add(PostManagementTab.OVERVIEW to "Overview")
        if (showReviewTab) add(PostManagementTab.REVIEW to "Review")
        else add(PostManagementTab.PEOPLE to "People")
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = VolunteerLinkSoftGreenSurface.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            tabs.forEach { (tab, label) ->
                PostManagementMainTabItem(
                    label = label,
                    selected = selected == tab,
                    hasNotification = tab == PostManagementTab.PEOPLE && pendingApplicantCount > 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun PostManagementMainTabItem(
    label: String,
    selected: Boolean,
    hasNotification: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) VolunteerLinkPrimaryGreen else Color.Transparent,
        border = null,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) VolunteerLinkSurface else VolunteerLinkTextSecondary
            )
            if (hasNotification) {
                Surface(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(7.dp),
                    shape = CircleShape,
                    color = VolunteerLinkError
                ) {}
            }
        }
    }
}

@Composable
internal fun PostManagementOverview(
    post: PostManagementPost,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OrganisationSectionSurface { PostManagementDetailsCard(post) }
        OrganisationSectionSurface { PostManagementParticipationCard(post) }
        OrganisationSectionSurface { PostManagementRoleSection(post) }
        OrganisationSectionSurface { PostManagementScheduleSection(post) }
    }
}

@Composable
private fun PostManagementDetailsCard(post: PostManagementPost) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OrganisationSectionHeader(
            title = "Opportunity details",
            subtitle = "Key information volunteers see for this post"
        )
        Column(modifier = Modifier.padding(top = 8.dp)) {
            if (post.physical != null) {
                Text(
                    text = "Physical",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
                PostManagementInfoLine(
                    label = "Date",
                    value = postManagementDateRange(post.physical.startDate, post.physical.endDate),
                    modifier = Modifier.padding(top = 8.dp)
                )
                PostManagementInfoLine(
                    label = "Time",
                    value = "${post.physical.startTime.toPostManagementTime()} – ${post.physical.endTime.toPostManagementTime()}",
                    modifier = Modifier.padding(top = 7.dp)
                )
                PostManagementInfoLine(
                    label = "Location",
                    value = buildString {
                        append(post.physical.locationName)
                        if (!post.physical.locationAddress.isNullOrBlank()) append(" · ${post.physical.locationAddress}")
                    },
                    modifier = Modifier.padding(top = 7.dp)
                )
                if (!post.physical.meetingPoint.isNullOrBlank()) {
                    PostManagementInfoLine(
                        label = "Meeting point",
                        value = post.physical.meetingPoint,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }
            }

            if (post.physical != null && post.remote != null) {
                OrganisationDivider(modifier = Modifier.padding(vertical = 14.dp))
            }

            if (post.remote != null) {
                Text(
                    text = "Remote",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
                PostManagementInfoLine(
                    label = "Date",
                    value = postManagementDateRange(post.remote.startDate, post.remote.effectiveEndDate),
                    modifier = Modifier.padding(top = 8.dp)
                )
                PostManagementInfoLine(
                    label = "Submission",
                    value = post.remote.submissionMode.toReadableDatabaseLabel(),
                    modifier = Modifier.padding(top = 7.dp)
                )
                if (!post.remote.sharedDeliverable.isNullOrBlank()) {
                    PostManagementInfoLine(
                        label = "Deliverable",
                        value = post.remote.sharedDeliverable,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PostManagementInfoLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            modifier = Modifier.width(104.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = VolunteerLinkTextSecondary
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = VolunteerLinkTextPrimary
        )
    }
}

@Composable
private fun PostManagementCompactInfoBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = VolunteerLinkTextSecondary
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 3.dp),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = VolunteerLinkTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PostManagementParticipationCard(post: PostManagementPost) {
    val accepted = post.volunteers.size
    val pending = post.applicants.size
    val totalRoleCapacity = post.roles.sumOf { it.capacity }

    Column(modifier = Modifier.fillMaxWidth()) {
        OrganisationSectionHeader(title = "Participation")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OrganisationMetric(accepted.toString(), "Joined", Modifier.weight(1f))
            OrganisationMetric(pending.toString(), "Applicants", Modifier.weight(1f))
            OrganisationMetric(totalRoleCapacity.toString(), "Role spaces", Modifier.weight(1f))
        }
        if (pending > 0) {
            Text(
                text = "$pending application${if (pending == 1) "" else "s"} waiting",
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkWarning
            )
        }
    }
}

@Composable
private fun PostManagementStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (highlight) {
            VolunteerLinkWarning.copy(alpha = 0.08f)
        } else {
            VolunteerLinkBackground
        },
        border = BorderStroke(
            1.dp,
            if (highlight) VolunteerLinkWarning.copy(alpha = 0.22f)
            else VolunteerLinkBorderColour
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) VolunteerLinkWarning else VolunteerLinkPrimaryGreen
            )
            Text(
                text = label,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 12.sp,
                color = VolunteerLinkTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PostManagementRoleSection(post: PostManagementPost) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OrganisationSectionHeader(
            title = "Roles",
            subtitle = if (post.roles.isEmpty()) null else "${post.roles.size} role${if (post.roles.size == 1) "" else "s"}"
        )
        if (post.roles.isEmpty()) {
            PostManagementEmptyCopy("No roles have been added to this post.")
        } else {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                post.roles.forEachIndexed { index, role ->
                    PostManagementRoleRow(
                        role = role,
                        acceptedCount = post.volunteers.count { it.roleTemplateId == role.roleTemplateId },
                        pendingCount = post.applicants.count { it.roleTemplateId == role.roleTemplateId }
                    )
                    if (index != post.roles.lastIndex) OrganisationDivider()
                }
            }
        }
    }
}

@Composable
private fun PostManagementRoleRow(
    role: PostManagementRole,
    acceptedCount: Int,
    pendingCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = role.roleName,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = "${role.defaultLevel.toReadableDatabaseLabel()} · ${role.roleMode.toReadableDatabaseLabel()} · ${role.applicationMethod.toApplicationMethodLabel()}",
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = VolunteerLinkTextSecondary
            )
            if (pendingCount > 0) {
                Text(
                    text = "$pendingCount pending application${if (pendingCount == 1) "" else "s"}",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkWarning
                )
            }
        }
        Text(
            text = "$acceptedCount / ${role.capacity}",
            modifier = Modifier.padding(start = 10.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkPrimaryGreen
        )
    }
}

@Composable
private fun PostManagementScheduleSection(post: PostManagementPost) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OrganisationSectionHeader(
            title = "Schedule",
            subtitle = if (post.schedules.isEmpty()) null else "${post.schedules.size} item${if (post.schedules.size == 1) "" else "s"}"
        )
        if (post.schedules.isEmpty()) {
            PostManagementEmptyCopy("No additional schedule items have been added.")
        } else {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                post.schedules.forEachIndexed { index, schedule ->
                    PostManagementScheduleRow(schedule)
                    if (index != post.schedules.lastIndex) OrganisationDivider(modifier = Modifier.padding(start = 48.dp))
                }
            }
        }
    }
}

@Composable
private fun PostManagementScheduleRow(schedule: PostManagementScheduleItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(11.dp),
            color = VolunteerLinkSoftGreenSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.calendar),
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = VolunteerLinkPrimaryGreen
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = schedule.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkTextPrimary
                )
                OrganisationStatusPill(
                    text = schedule.scheduleType.toReadableDatabaseLabel().uppercase(Locale.US),
                    color = VolunteerLinkPrimaryGreen,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = buildString {
                    append(schedule.scheduleDate.toPostManagementShortDate())
                    if (!schedule.startTime.isNullOrBlank()) append(" · ${schedule.startTime.toPostManagementTime()}")
                    if (!schedule.endTime.isNullOrBlank()) append("–${schedule.endTime.toPostManagementTime()}")
                },
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
                color = VolunteerLinkTextSecondary
            )
            schedule.location?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun PostManagementTodayAttendanceCard(
    attendance: PostManagementPhysicalAttendance,
    selectedDate: String,
    selectedSession: PostManagementAttendanceDay?,
    selectedEligibleVolunteerCount: Int,
    selectedPresentCount: Int,
    isStartingAttendance: Boolean,
    actionMessage: String?,
    onStartAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = selectedDate == attendance.todayDate
    val session = selectedSession

    OrganisationSectionSurface(
        modifier = modifier,
        contentPadding = 16.dp
    ) {
        OrganisationSectionHeader(
            title = if (isToday) "Today's attendance" else "Attendance",
            subtitle = buildString {
                append(selectedDate.toPostManagementShortDate())
                if (attendance.attendanceWindowLabel.isNotBlank()) {
                    append(" · PIN window ${attendance.attendanceWindowLabel}")
                }
            }
        )

        if (session == null) {
            val message = when {
                !isToday -> "Attendance was not started for this date."
                attendance.canStartAttendance -> "Attendance has not started for today."
                !attendance.startBlockedReason.isNullOrBlank() -> attendance.startBlockedReason
                attendance.eligiblePhysicalVolunteerCount <= 0 ->
                    "No Physical volunteers are scheduled for today."
                else -> "Attendance has not started for today."
            }

            Text(
                text = message,
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = VolunteerLinkTextSecondary
            )

            if (isToday && attendance.canStartAttendance) {
                Button(
                    onClick = onStartAttendance,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isStartingAttendance,
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                ) {
                    Text(
                        text = if (isStartingAttendance) "Starting Attendance..." else "Start Attendance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(14.dp),
                color = VolunteerLinkSoftGreenSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attendance PIN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextSecondary
                        )
                        Text(
                            text = session.pinCode.toSixDigitPinDisplay(),
                            modifier = Modifier.padding(top = 3.dp),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkPrimaryGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$selectedPresentCount / $selectedEligibleVolunteerCount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = if (isToday) "checked in" else "present",
                            modifier = Modifier.padding(top = 2.dp),
                            fontSize = 12.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }
            }

            Text(
                text = if (isToday) {
                    "Share the 6-digit PIN with today's Physical volunteers."
                } else {
                    "This was the PIN used for this Physical event day."
                },
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )

            if (isToday && !attendance.isLiveWindowOpen) {
                Text(
                    text = "Volunteer PIN check-in is closed. Organisation corrections are still available below.",
                    modifier = Modifier.padding(top = 7.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkWarning
                )
            }
        }

        if (!actionMessage.isNullOrBlank()) {
            OrganisationInfoStrip(
                title = "Attendance update",
                message = actionMessage,
                modifier = Modifier.padding(top = 10.dp),
                accent = VolunteerLinkError
            )
        }
    }
}

@Composable
internal fun PostManagementRemoteTeamSubmissionCard(
    deliverable: String?,
    responsibleRoleName: String?,
    dueDate: String,
    submission: PostManagementRemoteSubmission?,
    submittedByName: String?,
    isResubmission: Boolean = false,
    onViewSubmission: (PostManagementRemoteSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    OrganisationSectionSurface(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                OrganisationSectionHeader(
                    title = "Team submission",
                    subtitle = "Shared Remote deliverable"
                )
            }
            PostManagementSubmissionStatusPill(
                status = submission?.status ?: "NOT_SUBMITTED",
                modifier = Modifier.padding(start = 10.dp, top = 2.dp)
            )
        }

        if (!deliverable.isNullOrBlank()) {
            Text(
                text = deliverable,
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = VolunteerLinkTextPrimary
            )
        }

        OrganisationDivider(modifier = Modifier.padding(vertical = 12.dp))
        PostManagementInfoLine(
            label = "Due",
            value = dueDate.toPostManagementShortDate()
        )
        if (!responsibleRoleName.isNullOrBlank()) {
            PostManagementInfoLine(
                label = "Submitting role",
                value = responsibleRoleName,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (submission == null) {
            Text(
                text = "No team submission has been received yet.",
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkWarning
            )
        } else {
            val fileName = submission.filePath
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: if (!submission.submissionUrl.isNullOrBlank()) "Submitted link" else "Submission"

            if (
                submission.status.equals("REVISION_REQUESTED", ignoreCase = true) &&
                !submission.feedback.isNullOrBlank()
            ) {
                OrganisationInfoStrip(
                    title = "Revision requested",
                    message = submission.feedback,
                    modifier = Modifier.padding(top = 12.dp),
                    accent = VolunteerLinkWarning
                )
            }

            OrganisationDivider(modifier = Modifier.padding(top = 12.dp))
            OrganisationListRow(
                title = fileName,
                subtitle = buildString {
                    if (!submittedByName.isNullOrBlank()) append("Submitted by $submittedByName")
                    if (!submission.submittedAt.isNullOrBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(submission.submittedAt.toPostManagementDateTime())
                    }
                }.takeIf { it.isNotBlank() },
                supportingText = if (isResubmission) "Revised submission · tap to review" else "Tap to review submission",
                iconRes = R.drawable.remote_project,
                statusColor = VolunteerLinkPrimaryGreen,
                onClick = { onViewSubmission(submission) },
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun PostManagementRemoteSubmissionBlock(
    requirement: String?,
    dueDate: String,
    submission: PostManagementRemoteSubmission?,
    isResubmission: Boolean = false,
    onViewSubmission: (PostManagementRemoteSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OrganisationDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Individual deliverable",
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
            PostManagementSubmissionStatusPill(
                status = submission?.status ?: "NOT_SUBMITTED",
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        if (!requirement.isNullOrBlank()) {
            Text(
                text = requirement,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        Text(
            text = "Due ${dueDate.toPostManagementShortDate()}",
            modifier = Modifier.padding(top = 7.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = VolunteerLinkTextSecondary
        )

        if (submission == null) {
            Text(
                text = "No submission received yet.",
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkWarning
            )
        } else {
            val fileName = submission.filePath
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: if (!submission.submissionUrl.isNullOrBlank()) "Submitted link" else "Submission"

            if (
                submission.status.equals("REVISION_REQUESTED", ignoreCase = true) &&
                !submission.feedback.isNullOrBlank()
            ) {
                OrganisationInfoStrip(
                    title = "Revision requested",
                    message = submission.feedback,
                    modifier = Modifier.padding(top = 10.dp),
                    accent = VolunteerLinkWarning
                )
            }

            OrganisationListRow(
                title = fileName,
                subtitle = submission.submittedAt
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "Submitted ${it.toPostManagementDateTime()}" },
                supportingText = if (isResubmission) "Revised submission · tap to review" else "Tap to review submission",
                iconRes = R.drawable.remote_project,
                statusColor = VolunteerLinkPrimaryGreen,
                onClick = { onViewSubmission(submission) },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
internal fun PostManagementRemoteSubmissionDialog(
    submission: PostManagementRemoteSubmission,
    personName: String?,
    roleName: String?,
    submittedByName: String?,
    dueDate: String,
    isResubmission: Boolean = false,
    canReview: Boolean,
    isOpeningFile: Boolean,
    isDownloadingFile: Boolean,
    isReviewing: Boolean,
    fileActionError: String?,
    downloadMessage: String?,
    onOpenFile: () -> Unit,
    onDownloadFile: () -> Unit,
    onRequestRevision: () -> Unit,
    onAccept: () -> Unit,
    onNotAccept: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isShared = submission.submissionType.equals("SHARED", true)
    val fileName = submission.filePath?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
    val hasFile = !submission.filePath.isNullOrBlank()
    val hasLink = !submission.submissionUrl.isNullOrBlank()
    val isBusy = isOpeningFile || isDownloadingFile || isReviewing
    val submittedValue = submission.submittedAt?.takeIf { it.isNotBlank() }?.toPostManagementDateTime() ?: "Not recorded"
    val dueValue = dueDate.takeIf { it.isNotBlank() }?.toPostManagementShortDate() ?: "Not recorded"
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = { if (!isBusy) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = VolunteerLinkSurface,
            shadowElevation = 7.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isShared) "Review Team Submission" else "Review Submission",
                            fontSize = 21.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = if (isShared) "Shared Team Deliverable" else personName ?: "Individual Deliverable",
                            modifier = Modifier.padding(top = 4.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VolunteerLinkPrimaryGreen
                        )
                        if (!roleName.isNullOrBlank()) {
                            Text(roleName, modifier = Modifier.padding(top = 2.dp), fontSize = 13.sp, color = VolunteerLinkTextSecondary)
                        }
                    }
                    TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Close") }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .verticalScroll(scrollState)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Submission",
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        PostManagementSubmissionStatusPill(submission.status)
                    }
                    OrganisationDivider(modifier = Modifier.padding(top = 9.dp))

                    if (isResubmission) {
                        Text(
                            text = "Revised submission received",
                            modifier = Modifier.padding(top = 11.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VolunteerLinkPrimaryGreen
                        )
                    }

                    PostManagementInfoLine("File", fileName ?: if (hasLink) "Submission link" else "No file or link", Modifier.padding(top = 12.dp))
                    PostManagementInfoLine("Submitted", submittedValue, Modifier.padding(top = 8.dp))
                    PostManagementInfoLine("Due", dueValue, Modifier.padding(top = 8.dp))
                    if (isShared && !submittedByName.isNullOrBlank()) {
                        PostManagementInfoLine("Submitted by", submittedByName, Modifier.padding(top = 8.dp))
                    }

                    if (hasFile || hasLink) {
                        OrganisationDivider(modifier = Modifier.padding(vertical = 14.dp))
                        OrganisationSectionHeader(title = "File actions")
                        OutlinedButton(
                            onClick = onOpenFile,
                            enabled = !isBusy,
                            modifier = Modifier
                                .padding(top = 9.dp)
                                .fillMaxWidth()
                                .height(50.dp),
                            border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen)
                        ) {
                            Text(if (isOpeningFile) "Opening..." else "Open Submission", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkPrimaryGreen)
                        }
                        if (hasFile) {
                            OutlinedButton(
                                onClick = onDownloadFile,
                                enabled = !isBusy,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(if (isDownloadingFile) "Downloading..." else "Download File", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (!fileActionError.isNullOrBlank()) {
                        OrganisationInfoStrip(
                            title = "Unable to open file",
                            message = fileActionError,
                            modifier = Modifier.padding(top = 12.dp),
                            accent = VolunteerLinkError
                        )
                    }
                    if (!downloadMessage.isNullOrBlank()) {
                        OrganisationInfoStrip(
                            title = "File saved",
                            message = downloadMessage,
                            modifier = Modifier.padding(top = 12.dp),
                            accent = VolunteerLinkPrimaryGreen
                        )
                    }

                    if (submission.status.equals("REVISION_REQUESTED", true) && !submission.feedback.isNullOrBlank()) {
                        OrganisationInfoStrip(
                            title = "Revision feedback",
                            message = submission.feedback,
                            modifier = Modifier.padding(top = 12.dp),
                            accent = VolunteerLinkWarning
                        )
                    }

                    if (canReview) {
                        OrganisationDivider(modifier = Modifier.padding(vertical = 16.dp))
                        OrganisationSectionHeader(
                            title = "Decision",
                            subtitle = "Accepted Remote work becomes Completed automatically."
                        )

                        SubmissionDecisionRow(
                            title = "Accept work",
                            subtitle = "Accept this submission and complete the Remote participation.",
                            color = VolunteerLinkPrimaryGreen,
                            enabled = !isBusy,
                            onClick = onAccept,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        SubmissionDecisionRow(
                            title = "Request revision",
                            subtitle = "Ask for changes and keep the work open under an extended deadline.",
                            color = VolunteerLinkWarning,
                            enabled = !isBusy,
                            onClick = onRequestRevision,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        if (onNotAccept != null) {
                            SubmissionDecisionRow(
                                title = "Not accept",
                                subtitle = "Reject this work and mark the Remote participation Not Completed.",
                                color = VolunteerLinkError,
                                enabled = !isBusy,
                                onClick = onNotAccept,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}



@Composable
private fun SubmissionDecisionRow(
    title: String,
    subtitle: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
            Icon(
                painter = painterResource(R.drawable.back),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(17.dp)
                    .rotate(180f),
                tint = color
            )
        }
        OrganisationDivider()
    }
}

@Composable
internal fun PostManagementRequestRevisionDialog(
    isShared: Boolean,
    dueDate: String,
    feedback: String,
    isEditingDraft: Boolean = false,
    needsProjectDeadlineExtension: Boolean = false,
    isSaving: Boolean,
    errorMessage: String?,
    onFeedbackChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = {
            Text(
                text = if (isEditingDraft) "Edit Revision Request" else "Request Revision",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = if (isShared) {
                        "Explain what the team should change before resubmitting."
                    } else {
                        "Explain what the volunteer should change before resubmitting."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextSecondary
                )

                OutlinedTextField(
                    value = feedback,
                    onValueChange = onFeedbackChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text("Revision feedback") },
                    placeholder = { Text("What needs to be changed?") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isSaving,
                    isError = !errorMessage.isNullOrBlank()
                )

                if (dueDate.isNotBlank()) {
                    PostManagementInfoLine(
                        label = if (needsProjectDeadlineExtension) "Previous deadline" else "Due",
                        value = dueDate.toPostManagementShortDate(),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                Text(
                    text = if (needsProjectDeadlineExtension) {
                        if (isEditingDraft) {
                            "Update the draft feedback here. It will be committed together with the Submission stage and the new project deadline."
                        } else {
                            "This is a draft revision request. After reviewing all unresolved work, set one new project-wide deadline in the Submission stage."
                        }
                    } else {
                        "The existing project deadline stays the same while the project is ongoing."
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextSecondary
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = VolunteerLinkError
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VolunteerLinkWarning
                )
            ) {
                Text(
                    text = when {
                        isSaving -> "Saving..."
                        isEditingDraft -> "Update Request"
                        else -> "Request Revision"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        },
        containerColor = VolunteerLinkSurface
    )
}

@Composable
internal fun PostManagementAcceptSubmissionDialog(
    isShared: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = {
            Text(
                text = "Accept Submission?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = if (isShared) {
                        "This accepts the shared team deliverable. No separate Remote completion decision is needed; unresolved Remote team members become Completed automatically during final Remote review."
                    } else {
                        "This accepts the volunteer's submitted work. No separate Remote completion decision is needed; accepted work becomes Completed automatically during final Remote review."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextSecondary
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(top = 9.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = VolunteerLinkError
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VolunteerLinkPrimaryGreen
                )
            ) {
                Text(
                    text = if (isSaving) "Saving..." else "Accept",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        },
        containerColor = VolunteerLinkSurface
    )
}

@Composable
internal fun PostManagementNotAcceptSubmissionDialog(
    isShared: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Not Accept Submission?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
        },
        text = {
            Text(
                text = if (isShared) {
                    "This marks the latest Shared Team deliverable as Not Accepted. Team members will not be eligible for Completed from this deliverable."
                } else {
                    "This marks the latest submitted work as Not Accepted. This volunteer will not be eligible for Completed from this deliverable."
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = VolunteerLinkTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkError)
            ) {
                Text("Not Accept", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = VolunteerLinkSurface
    )
}

private fun remoteSubmissionFileLabel(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "")
        .trim()
        .uppercase(Locale.US)

    return if (extension.isBlank()) {
        "Uploaded file"
    } else {
        "$extension submission"
    }
}

@Composable
private fun PostManagementSubmissionStatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val normalized = status.uppercase(Locale.US)
    val label = when (normalized) {
        "NOT_SUBMITTED" -> "Not Submitted"
        "PENDING_REVIEW" -> "Pending Review"
        "REVISION_REQUESTED" -> "Revision Requested"
        "ACCEPTED" -> "Accepted"
        "NOT_ACCEPTED" -> "Not Accepted"
        else -> status.toReadableDatabaseLabel()
    }
    val foreground = when (normalized) {
        "ACCEPTED" -> VolunteerLinkPrimaryGreen
        "PENDING_REVIEW" -> VolunteerLinkInformation
        "REVISION_REQUESTED" -> VolunteerLinkWarning
        "NOT_ACCEPTED" -> VolunteerLinkError
        else -> VolunteerLinkTextSecondary
    }

    Surface(
        modifier = modifier,
        shape = PostManagementPillShape,
        color = foreground.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, foreground.copy(alpha = 0.18f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            color = foreground
        )
    }
}

@Composable
private fun PostManagementResubmissionNotice(
    isShared: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = VolunteerLinkPrimaryGreen.copy(alpha = 0.10f),
        border = BorderStroke(1.2.dp, VolunteerLinkPrimaryGreen.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)
        ) {
            Text(
                text = "Revised submission received",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
            Text(
                text = if (isShared) {
                    "Latest team version is ready for review."
                } else {
                    "Latest version is ready for review."
                },
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                color = VolunteerLinkTextPrimary
            )
        }
    }
}

@Composable
private fun PostManagementResubmittedPill(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = PostManagementPillShape,
        color = VolunteerLinkPrimaryGreen.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen.copy(alpha = 0.18f))
    ) {
        Text(
            text = "Resubmitted",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkPrimaryGreen
        )
    }
}

@Composable
internal fun PostManagementAttendanceDaySelector(
    dates: List<String>,
    selectedDate: String?,
    actionMessage: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dates.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Attendance day",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(
                count = dates.size,
                key = { index -> dates[index] }
            ) { index ->
                val date = dates[index]
                PostManagementRoleFilterChip(
                    text = date.toPostManagementShortDate(),
                    selected = selectedDate == date,
                    onClick = { onSelected(date) }
                )
            }
        }

        if (!actionMessage.isNullOrBlank()) {
            Text(
                text = actionMessage,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkError
            )
        }
    }
}

@Composable
private fun PostManagementVolunteerAttendanceBlock(
    summary: PostManagementVolunteerAttendanceSummary,
    selectedDate: String,
    todayDate: String,
    canCorrectAttendance: Boolean,
    isUpdatingAttendance: Boolean,
    onMarkPresent: () -> Unit,
    onRequestMarkAbsent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedStatus = summary.statusFor(selectedDate)

    Column(modifier = modifier.fillMaxWidth()) {
        OrganisationDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Attendance",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = "${summary.verifiedMinutes.toVerifiedTimeLabel()} verified",
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
            Text(
                text = "${summary.attendedDays} / ${summary.expectedDays} days",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
        }

        val statusText = when {
            selectedStatus == null || !selectedStatus.expected -> {
                "${selectedDate.toPostManagementShortDate()} · Not scheduled"
            }
            selectedStatus.present -> {
                "${selectedDate.toPostManagementShortDate()} · Present"
            }
            selectedStatus.markedAbsent -> {
                "${selectedDate.toPostManagementShortDate()} · Absent"
            }
            selectedDate == todayDate -> {
                "${selectedDate.toPostManagementShortDate()} · Not checked in yet"
            }
            else -> {
                "${selectedDate.toPostManagementShortDate()} · Absent"
            }
        }

        Text(
            text = statusText,
            modifier = Modifier.padding(top = 7.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selectedStatus?.present == true) {
                VolunteerLinkPrimaryGreen
            } else {
                VolunteerLinkTextSecondary
            }
        )

        if (
            canCorrectAttendance &&
            selectedStatus != null &&
            selectedStatus.expected
        ) {
            if (selectedStatus.present) {
                OutlinedButton(
                    onClick = onRequestMarkAbsent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp)
                        .height(48.dp),
                    enabled = !isUpdatingAttendance
                ) {
                    Text(
                        text = if (isUpdatingAttendance) "Updating..." else "Mark Absent",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onMarkPresent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp)
                        .height(48.dp),
                    enabled = !isUpdatingAttendance,
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                ) {
                    Text(
                        text = if (isUpdatingAttendance) "Updating..." else "Mark Present",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
internal fun PostManagementMarkAbsentDialog(
    person: PostManagementPerson,
    eventDate: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Mark ${person.fullName} as absent?",
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
        },
        text = {
            Text(
                text = "This will mark the volunteer absent for " +
                        "${eventDate.toPostManagementShortDate()}, set verified time for that day to 0, " +
                        "and prevent another PIN check-in for this date. You can use Mark Present to change the decision.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = VolunteerLinkTextSecondary
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Mark Absent",
                    color = VolunteerLinkError,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
internal fun PostManagementPeopleControls(
    selectedTab: PostManagementPeopleTab,
    showApplicantsTab: Boolean,
    applicantCount: Int,
    volunteerCount: Int,
    query: String,
    selectedRoleId: String?,
    roles: List<PostManagementRole>,
    onTabSelected: (PostManagementPeopleTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onRoleSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    OrganisationSectionSurface(
        modifier = modifier,
        contentPadding = 14.dp
    ) {
        if (showApplicantsTab) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = VolunteerLinkSoftGreenSurface.copy(alpha = 0.52f),
                border = BorderStroke(1.dp, VolunteerLinkBorderColour)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PostManagementPeopleTabItem(
                        text = "Applicants $applicantCount",
                        iconRes = R.drawable.applications,
                        selected = selectedTab == PostManagementPeopleTab.APPLICANTS,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(PostManagementPeopleTab.APPLICANTS) }
                    )
                    PostManagementPeopleTabItem(
                        text = "Volunteers $volunteerCount",
                        iconRes = R.drawable.profile,
                        selected = selectedTab == PostManagementPeopleTab.VOLUNTEERS,
                        modifier = Modifier.weight(1f),
                        onClick = { onTabSelected(PostManagementPeopleTab.VOLUNTEERS) }
                    )
                }
            }
        } else {
            OrganisationSectionHeader(
                title = "Volunteers",
                subtitle = "$volunteerCount joined"
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            label = { Text("Search people") },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_volunteer_search),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = VolunteerLinkTextSecondary
                )
            }
        )

        if (roles.isNotEmpty()) {
            Text(
                text = "Role",
                modifier = Modifier.padding(top = 14.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextSecondary
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "all_roles") {
                    PostManagementRoleFilterChip(
                        text = "All roles",
                        selected = selectedRoleId == null,
                        onClick = { onRoleSelected(null) }
                    )
                }
                items(
                    count = roles.size,
                    key = { index -> roles[index].roleTemplateId }
                ) { index ->
                    val role = roles[index]
                    PostManagementRoleFilterChip(
                        text = role.roleName,
                        selected = selectedRoleId == role.roleTemplateId,
                        onClick = { onRoleSelected(role.roleTemplateId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostManagementPeopleTabItem(
    text: String,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) VolunteerLinkPrimaryGreen else Color.Transparent,
        border = null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (selected) VolunteerLinkSurface else VolunteerLinkTextSecondary
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 6.dp),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) VolunteerLinkSurface else VolunteerLinkTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PostManagementRoleFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = PostManagementPillShape,
        color = if (selected) VolunteerLinkPrimaryGreen else VolunteerLinkSurface,
        border = BorderStroke(
            1.dp,
            if (selected) VolunteerLinkPrimaryGreen else VolunteerLinkBorderColour
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) VolunteerLinkSurface else VolunteerLinkTextSecondary,
            maxLines = 1
        )
    }
}

@Composable
internal fun PostManagementPeopleRoleHeader(
    role: PostManagementRole,
    selectedTab: PostManagementPeopleTab,
    applicantCount: Int,
    volunteerCount: Int,
    remoteSubmissionLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = role.roleName,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!remoteSubmissionLabel.isNullOrBlank()) {
                PostManagementRemoteRolePill(text = remoteSubmissionLabel)
            }

            if (selectedTab == PostManagementPeopleTab.APPLICANTS) {
                PostManagementApplicationWindowPill(role = role)
            }
        }

        Text(
            text = when (selectedTab) {
                PostManagementPeopleTab.APPLICANTS -> {
                    val noun = if (applicantCount == 1) "applicant" else "applicants"
                    buildString {
                        append(role.roleMode.toReadableDatabaseLabel())
                        append(" · ")
                        append(applicantCount)
                        append(" ")
                        append(noun)
                        role.applicationCutoffDate?.let { cutoff ->
                            append(" · Closes ")
                            append(cutoff.toPostManagementShortDate())
                        }
                    }
                }

                PostManagementPeopleTab.VOLUNTEERS -> {
                    "${role.roleMode.toReadableDatabaseLabel()} · " +
                            "$volunteerCount / ${role.capacity} joined"
                }
            },
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = VolunteerLinkTextSecondary
        )

        OrganisationDivider(modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun PostManagementRemoteRolePill(
    text: String,
    modifier: Modifier = Modifier
) {
    OrganisationStatusPill(
        text = text,
        color = VolunteerLinkPrimaryGreen,
        modifier = modifier
    )
}

@Composable
private fun PostManagementApplicationWindowPill(
    role: PostManagementRole,
    modifier: Modifier = Modifier
) {
    OrganisationStatusPill(
        text = if (role.isApplicationOpen) "Open" else "Closed",
        color = if (role.isApplicationOpen) VolunteerLinkPrimaryGreen else VolunteerLinkWarning,
        modifier = modifier
    )
}

@Composable
internal fun PostManagementPersonCard(
    person: PostManagementPerson,
    isApplicant: Boolean,
    isApplicationOpen: Boolean,
    attendanceSummary: PostManagementVolunteerAttendanceSummary? = null,
    attendanceSelectedDate: String? = null,
    attendanceTodayDate: String? = null,
    remoteSubmissionRequirement: String? = null,
    remoteSubmissionDueDate: String? = null,
    remoteSubmission: PostManagementRemoteSubmission? = null,
    remoteSubmissionIsResubmission: Boolean = false,
    canCorrectAttendance: Boolean = false,
    isUpdatingAttendance: Boolean = false,
    onMarkPresent: (PostManagementPerson, String) -> Unit = { _, _ -> },
    onRequestMarkAbsent: (PostManagementPerson, String) -> Unit = { _, _ -> },
    onViewRemoteSubmission: (PostManagementPerson, PostManagementRemoteSubmission) -> Unit = { _, _ -> },
    onViewProfile: (PostManagementPerson) -> Unit,
    onToggleShortlist: (PostManagementPerson) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onViewProfile(person) },
                    shape = CircleShape,
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.person_placeholder),
                            contentDescription = "View ${person.fullName} profile",
                            modifier = Modifier.size(24.dp),
                            tint = VolunteerLinkPrimaryGreen
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                        .clickable { onViewProfile(person) }
                ) {
                    Text(
                        text = person.fullName,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(person.defaultLevel.toReadableDatabaseLabel())
                            if (!person.city.isNullOrBlank()) {
                                append(" · ")
                                append(person.city)
                            }
                        },
                        modifier = Modifier.padding(top = 3.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = VolunteerLinkTextSecondary
                    )
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OrganisationStatusPill(
                            text = if (isApplicant) "Pending" else person.completionStatus.toCompletionLabel(),
                            color = if (isApplicant) VolunteerLinkInformation else person.completionStatus.toCompletionColor()
                        )
                        Text(
                            text = "View profile",
                            modifier = Modifier.padding(start = 9.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VolunteerLinkPrimaryGreen
                        )
                    }
                }

                OrganisationMessageButton(
                    personName = person.fullName,
                    modifier = Modifier.padding(start = 6.dp)
                )

                if (isApplicant) {
                    IconButton(
                        onClick = { onToggleShortlist(person) },
                        modifier = Modifier.size(38.dp),
                        enabled = isApplicationOpen
                    ) {
                        Icon(
                            painter = painterResource(
                                if (person.isShortlisted) R.drawable.shortlist_filled else R.drawable.shortlist_outline
                            ),
                            contentDescription = if (person.isShortlisted) "Remove from shortlist" else "Add to shortlist",
                            modifier = Modifier.size(20.dp),
                            tint = when {
                                !isApplicationOpen -> VolunteerLinkTextSecondary.copy(alpha = 0.35f)
                                person.isShortlisted -> VolunteerLinkWarning
                                else -> VolunteerLinkTextSecondary
                            }
                        )
                    }
                }

            }

            if (
                attendanceSummary != null &&
                !attendanceSelectedDate.isNullOrBlank() &&
                !attendanceTodayDate.isNullOrBlank()
            ) {
                PostManagementVolunteerAttendanceBlock(
                    summary = attendanceSummary,
                    selectedDate = attendanceSelectedDate,
                    todayDate = attendanceTodayDate,
                    canCorrectAttendance = canCorrectAttendance,
                    isUpdatingAttendance = isUpdatingAttendance,
                    onMarkPresent = { onMarkPresent(person, attendanceSelectedDate) },
                    onRequestMarkAbsent = { onRequestMarkAbsent(person, attendanceSelectedDate) },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (!remoteSubmissionDueDate.isNullOrBlank()) {
                PostManagementRemoteSubmissionBlock(
                    requirement = remoteSubmissionRequirement,
                    dueDate = remoteSubmissionDueDate,
                    submission = remoteSubmission,
                    isResubmission = remoteSubmissionIsResubmission,
                    onViewSubmission = { submission -> onViewRemoteSubmission(person, submission) },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (isApplicant) {
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(48.dp),
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = if (isApplicationOpen) {
                            VolunteerLinkPrimaryGreen.copy(alpha = 0.18f)
                        } else {
                            VolunteerLinkWarning.copy(alpha = 0.12f)
                        },
                        disabledContentColor = if (isApplicationOpen) {
                            VolunteerLinkPrimaryGreen.copy(alpha = 0.60f)
                        } else {
                            VolunteerLinkWarning.copy(alpha = 0.75f)
                        }
                    )
                ) {
                    Text(
                        text = if (isApplicationOpen) "Review Application" else "Application Closed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isApplicant && !isApplicationOpen) {
                Text(
                    text = "Application review is closed for this role.",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkWarning
                )
            }
        }
    }
}

@Composable
internal fun PostManagementPeopleEmptyState(
    selectedTab: PostManagementPeopleTab,
    hasFilters: Boolean,
    modifier: Modifier = Modifier
) {
    val title: String
    val message: String

    if (hasFilters) {
        title = "No matching people"
        message = "Try another name or role filter."
    } else if (selectedTab == PostManagementPeopleTab.APPLICANTS) {
        title = "No applicants"
        message = "Pending applications for this post will appear here."
    } else {
        title = "No volunteers yet"
        message = "People who join or are accepted into a role will appear here."
    }

    ManageEmptySectionMessage(
        title = title,
        message = message,
        modifier = modifier
    )
}

@Composable
internal fun PostManagementProfilePreviewDialog(
    person: PostManagementPerson,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = person.fullName,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = person.roleName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
                Text(
                    text = "${person.defaultLevel.toReadableDatabaseLabel()} · ${person.roleMode.toReadableDatabaseLabel()}",
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 11.sp,
                    color = VolunteerLinkTextSecondary
                )

                if (!person.city.isNullOrBlank()) {
                    Text(
                        text = person.city,
                        modifier = Modifier.padding(top = 12.dp),
                        fontSize = 11.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                Text(
                    text = "About",
                    modifier = Modifier.padding(top = 14.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = person.bio?.takeIf { it.isNotBlank() }
                        ?: "No bio has been added yet.",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = VolunteerLinkTextSecondary
                )

                Text(
                    text = "Participation",
                    modifier = Modifier.padding(top = 14.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = "Application: ${person.applicationStatus.toReadableDatabaseLabel()}\n" +
                            "Completion: ${person.completionStatus.toCompletionLabel()}",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


@Composable
private fun PostManagementSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PostManagementCardShape,
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PostManagementEmptyCopy(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = VolunteerLinkTextSecondary
    )
}

@Composable
private fun PostManagementLifecycleBadge(post: PostManagementPost) {
    val style = when (post.databaseStatus.uppercase(Locale.US)) {
        "DRAFT" -> BadgeStyle(
            "Draft",
            VolunteerLinkTextSecondary.copy(alpha = 0.08f),
            VolunteerLinkTextSecondary,
            VolunteerLinkBorderColour
        )
        "COMPLETED" -> BadgeStyle(
            "Completed",
            VolunteerLinkSoftGreenSurface,
            VolunteerLinkPrimaryGreen,
            VolunteerLinkBorderColour
        )
        "CANCELLED" -> BadgeStyle(
            "Cancelled",
            VolunteerLinkError.copy(alpha = 0.08f),
            VolunteerLinkError,
            VolunteerLinkError.copy(alpha = 0.20f)
        )
        else -> when (post.timingState) {
            PostTimingState.ONGOING -> BadgeStyle(
                "Ongoing",
                VolunteerLinkPrimaryGreen,
                VolunteerLinkSurface,
                VolunteerLinkPrimaryGreen
            )
            PostTimingState.UPCOMING -> BadgeStyle(
                "Upcoming",
                VolunteerLinkInformation.copy(alpha = 0.09f),
                VolunteerLinkInformation,
                VolunteerLinkInformation.copy(alpha = 0.22f)
            )
            PostTimingState.PAST -> BadgeStyle(
                "Needs Review",
                VolunteerLinkWarning.copy(alpha = 0.09f),
                VolunteerLinkWarning,
                VolunteerLinkWarning.copy(alpha = 0.22f)
            )
            null -> BadgeStyle(
                "Published",
                VolunteerLinkSoftGreenSurface,
                VolunteerLinkPrimaryGreen,
                VolunteerLinkBorderColour
            )
        }
    }

    Surface(
        shape = PostManagementPillShape,
        color = style.container,
        border = BorderStroke(1.dp, style.border)
    ) {
        Text(
            text = style.label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = style.content
        )
    }
}

private data class BadgeStyle(
    val label: String,
    val container: Color,
    val content: Color,
    val border: Color
)

@Composable
private fun PostManagementNeutralPill(text: String) {
    Surface(
        shape = PostManagementPillShape,
        color = VolunteerLinkSoftGreenSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkPrimaryGreen,
            maxLines = 1
        )
    }
}

@Composable
private fun PostManagementPhaseLine(
    label: String,
    @DrawableRes iconRes: Int,
    startDate: String?,
    endDate: String?,
    location: String?,
    timing: PostTimingState?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = PostManagementSmallShape,
            color = VolunteerLinkSoftGreenSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = VolunteerLinkPrimaryGreen
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                PostManagementPhaseBadge(
                    timing = timing,
                    modifier = Modifier.padding(start = 7.dp)
                )
            }
            Text(
                text = postManagementDateRange(startDate, endDate),
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = VolunteerLinkTextSecondary
            )
            if (!location.isNullOrBlank()) {
                Text(
                    text = location,
                    modifier = Modifier.padding(top = 2.dp),
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
private fun PostManagementPhaseBadge(
    timing: PostTimingState?,
    modifier: Modifier = Modifier
) {
    val label = when (timing) {
        PostTimingState.ONGOING -> "Ongoing"
        PostTimingState.UPCOMING -> "Upcoming"
        PostTimingState.PAST -> "Ended"
        null -> "Scheduled"
    }
    val background = when (timing) {
        PostTimingState.ONGOING -> VolunteerLinkPrimaryGreen
        PostTimingState.UPCOMING -> VolunteerLinkInformation.copy(alpha = 0.08f)
        PostTimingState.PAST, null -> VolunteerLinkTextSecondary.copy(alpha = 0.07f)
    }
    val foreground = when (timing) {
        PostTimingState.ONGOING -> VolunteerLinkSurface
        PostTimingState.UPCOMING -> VolunteerLinkInformation
        PostTimingState.PAST, null -> VolunteerLinkTextSecondary
    }

    Surface(
        modifier = modifier,
        shape = PostManagementPillShape,
        color = background
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = foreground
        )
    }
}

@DrawableRes
private fun String.postManagementModeDrawable(): Int = when (uppercase(Locale.US)) {
    "PHYSICAL" -> R.drawable.physical_event
    "REMOTE" -> R.drawable.remote_project
    "HYBRID" -> R.drawable.manage_hybrid
    else -> R.drawable.manage
}

private fun String.toPostManagementModeLabel(): String = when (uppercase(Locale.US)) {
    "PHYSICAL" -> "Physical"
    "REMOTE" -> "Remote"
    "HYBRID" -> "Hybrid"
    else -> this
}

private fun String.toApplicationMethodLabel(): String = when (uppercase(Locale.US)) {
    "INSTANT_JOIN" -> "Instant Join"
    "REVIEW_APPLICANTS" -> "Review Applicants"
    else -> toReadableDatabaseLabel()
}

private fun String.toCompletionLabel(): String = when (uppercase(Locale.US)) {
    "IN_PROGRESS" -> "Joined"
    "NEEDS_REVIEW" -> "Needs Review"
    "COMPLETED" -> "Completed"
    "NOT_COMPLETED" -> "Not Completed"
    else -> toReadableDatabaseLabel()
}

private fun String.toCompletionColor(): Color = when (uppercase(Locale.US)) {
    "IN_PROGRESS" -> VolunteerLinkPrimaryGreen
    "NEEDS_REVIEW" -> VolunteerLinkInformation
    "COMPLETED" -> VolunteerLinkSuccess
    "NOT_COMPLETED" -> VolunteerLinkError
    else -> VolunteerLinkTextSecondary
}

private fun String.toReadableDatabaseLabel(): String {
    return lowercase(Locale.US)
        .split("_")
        .joinToString(" ") { word ->
            word.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
}

private fun postManagementDateRange(start: String?, end: String?): String {
    if (start.isNullOrBlank() && end.isNullOrBlank()) return "Date not set"
    if (start.isNullOrBlank()) return end.toPostManagementShortDate()
    if (end.isNullOrBlank() || start == end) return start.toPostManagementShortDate()
    return "${start.toPostManagementShortDate()} – ${end.toPostManagementShortDate()}"
}

private fun String.toSixDigitPinDisplay(): String {
    val digits = filter(Char::isDigit).take(6)
    return if (digits.length == 6) {
        "${digits.take(3)} ${digits.takeLast(3)}"
    } else {
        this
    }
}

private fun Int.toVerifiedTimeLabel(): String {
    val safeMinutes = coerceAtLeast(0)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun String?.toPostManagementShortDate(): String {
    val value = this?.takeIf { it.isNotBlank() } ?: return "Date not set"
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
    return "$day $month ${parts[0]}"
}

private fun String?.toPostManagementDateTime(): String {
    val value = this?.takeIf { it.isNotBlank() } ?: return "Date not set"
    val datePart = value.substringBefore("T")
    val timePart = value.substringAfter("T", "").take(5)
    val dateLabel = datePart.toPostManagementShortDate()
    return if (timePart.length == 5 && timePart.contains(":")) {
        "$dateLabel · ${timePart.toPostManagementTime()}"
    } else {
        dateLabel
    }
}

private fun String.toPostManagementTime(): String {
    val parts = split(":")
    if (parts.size < 2) return this
    val hour = parts[0].toIntOrNull() ?: return this
    val minute = parts[1]
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val suffix = if (hour >= 12) "PM" else "AM"
    return "$displayHour:$minute $suffix"
}
