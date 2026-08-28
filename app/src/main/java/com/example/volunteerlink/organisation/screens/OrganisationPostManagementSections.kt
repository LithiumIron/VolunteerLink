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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceDay
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalAttendance
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementRole
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
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning
import java.util.Locale

internal enum class PostManagementTab {
    OVERVIEW,
    PEOPLE
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PostManagementCardShape,
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(post.mode.postManagementModeDrawable()),
                            contentDescription = null,
                            modifier = Modifier.size(23.dp),
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
                        fontSize = 18.sp,
                        lineHeight = 23.sp,
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
                        PostManagementNeutralPill(post.mode.toPostManagementModeLabel())
                    }
                }
            }

            if (!post.category.isNullOrBlank()) {
                Text(
                    text = post.category.toReadableDatabaseLabel(),
                    modifier = Modifier.padding(top = 13.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
            }

            Text(
                text = post.description,
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (post.mode.equals("HYBRID", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = VolunteerLinkBorderColour)
                PostManagementPhaseLine(
                    label = "Remote",
                    iconRes = R.drawable.remote_project,
                    startDate = post.remote?.startDate,
                    endDate = post.remote?.endDate,
                    location = null,
                    timing = post.remoteTimingState,
                    modifier = Modifier.padding(top = 12.dp)
                )
                PostManagementPhaseLine(
                    label = "Physical",
                    iconRes = R.drawable.physical_event,
                    startDate = post.physical?.startDate,
                    endDate = post.physical?.endDate,
                    location = post.physical?.locationName,
                    timing = post.physicalTimingState,
                    modifier = Modifier.padding(top = 10.dp)
                )
            } else if (post.mode.equals("PHYSICAL", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = VolunteerLinkBorderColour)
                PostManagementPhaseLine(
                    label = "Physical",
                    iconRes = R.drawable.physical_event,
                    startDate = post.physical?.startDate,
                    endDate = post.physical?.endDate,
                    location = post.physical?.locationName,
                    timing = post.physicalTimingState,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else if (post.mode.equals("REMOTE", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = VolunteerLinkBorderColour)
                PostManagementPhaseLine(
                    label = "Remote",
                    iconRes = R.drawable.remote_project,
                    startDate = post.remote?.startDate,
                    endDate = post.remote?.endDate,
                    location = null,
                    timing = post.remoteTimingState,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
internal fun PostManagementMainTabs(
    selected: PostManagementTab,
    pendingApplicantCount: Int,
    onSelected: (PostManagementTab) -> Unit,
    modifier: Modifier = Modifier
) {
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
            PostManagementMainTabItem(
                label = "Overview",
                selected = selected == PostManagementTab.OVERVIEW,
                hasNotification = false,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(PostManagementTab.OVERVIEW) }
            )
            PostManagementMainTabItem(
                label = "People",
                selected = selected == PostManagementTab.PEOPLE,
                hasNotification = pendingApplicantCount > 0,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(PostManagementTab.PEOPLE) }
            )
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
        shape = RoundedCornerShape(9.dp),
        color = if (selected) VolunteerLinkPrimaryGreen else VolunteerLinkSurface
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) VolunteerLinkSurface else VolunteerLinkTextSecondary
            )
            if (hasNotification) {
                Surface(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(8.dp),
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PostManagementDetailsCard(post)
        PostManagementParticipationCard(post)
        PostManagementRoleSection(post)
        PostManagementScheduleSection(post)
    }
}

@Composable
private fun PostManagementDetailsCard(post: PostManagementPost) {
    PostManagementSectionCard(title = "Opportunity Details") {
        if (post.physical != null) {
            Text(
                text = "Physical",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
            PostManagementInfoLine(
                label = "Date",
                value = postManagementDateRange(
                    post.physical.startDate,
                    post.physical.endDate
                ),
                modifier = Modifier.padding(top = 7.dp)
            )
            PostManagementInfoLine(
                label = "Time",
                value = "${post.physical.startTime.toPostManagementTime()} – ${post.physical.endTime.toPostManagementTime()}",
                modifier = Modifier.padding(top = 5.dp)
            )
            PostManagementInfoLine(
                label = "Location",
                value = buildString {
                    append(post.physical.locationName)
                    if (!post.physical.locationAddress.isNullOrBlank()) {
                        append("\n")
                        append(post.physical.locationAddress)
                    }
                },
                modifier = Modifier.padding(top = 5.dp)
            )
            if (!post.physical.meetingPoint.isNullOrBlank()) {
                PostManagementInfoLine(
                    label = "Meeting point",
                    value = post.physical.meetingPoint,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }

        if (post.physical != null && post.remote != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = VolunteerLinkBorderColour
            )
        }

        if (post.remote != null) {
            Text(
                text = "Remote",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
            PostManagementInfoLine(
                label = "Date",
                value = postManagementDateRange(
                    post.remote.startDate,
                    post.remote.endDate
                ),
                modifier = Modifier.padding(top = 7.dp)
            )
            PostManagementInfoLine(
                label = "Submission",
                value = post.remote.submissionMode.toReadableDatabaseLabel(),
                modifier = Modifier.padding(top = 5.dp)
            )
            if (!post.remote.sharedDeliverable.isNullOrBlank()) {
                PostManagementInfoLine(
                    label = "Deliverable",
                    value = post.remote.sharedDeliverable,
                    modifier = Modifier.padding(top = 5.dp)
                )
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
            modifier = Modifier.width(86.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = VolunteerLinkTextSecondary
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = VolunteerLinkTextPrimary
        )
    }
}

@Composable
private fun PostManagementParticipationCard(post: PostManagementPost) {
    val accepted = post.volunteers.size
    val pending = post.applicants.size
    val totalRoleCapacity = post.roles.sumOf { it.capacity }

    PostManagementSectionCard(title = "Participation") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            PostManagementStat(
                value = accepted.toString(),
                label = "Joined",
                modifier = Modifier.weight(1f)
            )
            PostManagementStat(
                value = pending.toString(),
                label = "Applicants",
                modifier = Modifier.weight(1f),
                highlight = pending > 0
            )
            PostManagementStat(
                value = totalRoleCapacity.toString(),
                label = "Role spaces",
                modifier = Modifier.weight(1f)
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
                fontSize = 9.sp,
                color = VolunteerLinkTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PostManagementRoleSection(post: PostManagementPost) {
    PostManagementSectionCard(
        title = "Roles",
        subtitle = if (post.roles.isEmpty()) null else "${post.roles.size} role${if (post.roles.size == 1) "" else "s"}"
    ) {
        if (post.roles.isEmpty()) {
            PostManagementEmptyCopy("No roles have been added to this post.")
        } else {
            post.roles.forEachIndexed { index, role ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 11.dp),
                        color = VolunteerLinkBorderColour
                    )
                }
                PostManagementRoleRow(
                    role = role,
                    acceptedCount = post.volunteers.count {
                        it.roleTemplateId == role.roleTemplateId
                    },
                    pendingCount = post.applicants.count {
                        it.roleTemplateId == role.roleTemplateId
                    }
                )
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = role.roleName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = "${role.defaultLevel.toReadableDatabaseLabel()} · ${role.roleMode.toReadableDatabaseLabel()}",
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 10.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
            Text(
                text = "$acceptedCount / ${role.capacity}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            PostManagementNeutralPill(role.applicationMethod.toApplicationMethodLabel())

            if (pendingCount > 0) {
                Surface(
                    shape = PostManagementPillShape,
                    color = VolunteerLinkWarning.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, VolunteerLinkWarning.copy(alpha = 0.20f))
                ) {
                    Text(
                        text = "$pendingCount pending",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkWarning
                    )
                }
            }
        }
    }
}

@Composable
private fun PostManagementScheduleSection(post: PostManagementPost) {
    PostManagementSectionCard(
        title = "Schedule",
        subtitle = if (post.schedules.isEmpty()) null else "${post.schedules.size} item${if (post.schedules.size == 1) "" else "s"}"
    ) {
        if (post.schedules.isEmpty()) {
            PostManagementEmptyCopy("No additional schedule items have been added.")
        } else {
            post.schedules.forEachIndexed { index, schedule ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = VolunteerLinkBorderColour
                    )
                }
                PostManagementScheduleRow(schedule)
            }
        }
    }
}

@Composable
private fun PostManagementScheduleRow(schedule: PostManagementScheduleItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = PostManagementSmallShape,
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
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                PostManagementNeutralPill(schedule.scheduleType.toReadableDatabaseLabel())
            }
            Text(
                text = buildString {
                    append(schedule.scheduleDate.toPostManagementShortDate())
                    if (!schedule.startTime.isNullOrBlank()) {
                        append(" · ")
                        append(schedule.startTime.toPostManagementTime())
                    }
                    if (!schedule.endTime.isNullOrBlank()) {
                        append("–")
                        append(schedule.endTime.toPostManagementTime())
                    }
                },
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 10.sp,
                color = VolunteerLinkTextSecondary
            )

            val place = schedule.location?.takeIf { it.isNotBlank() }

            if (!place.isNullOrBlank()) {
                Text(
                    text = place,
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 10.sp,
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PostManagementCardShape,
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ongoing_posts),
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = VolunteerLinkPrimaryGreen
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = if (isToday) "Today's Attendance" else "Attendance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = selectedDate.toPostManagementShortDate(),
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 10.sp,
                        color = VolunteerLinkTextSecondary
                    )
                    if (attendance.attendanceWindowLabel.isNotBlank()) {
                        Text(
                            text = "Volunteer PIN window · ${attendance.attendanceWindowLabel}",
                            modifier = Modifier.padding(top = 2.dp),
                            fontSize = 9.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }
            }

            val session = selectedSession
            if (session == null) {
                Text(
                    text = when {
                        !isToday -> "Attendance was not started for this date."
                        attendance.canStartAttendance -> "Attendance has not started for today."
                        !attendance.startBlockedReason.isNullOrBlank() -> attendance.startBlockedReason
                        attendance.eligiblePhysicalVolunteerCount <= 0 ->
                            "No Physical volunteers are scheduled for today."
                        else -> "Attendance has not started for today."
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = VolunteerLinkTextSecondary
                )

                if (isToday && attendance.canStartAttendance) {
                    Button(
                        onClick = onStartAttendance,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        enabled = !isStartingAttendance
                    ) {
                        Text(
                            text = if (isStartingAttendance) {
                                "Starting Attendance..."
                            } else {
                                "Start Attendance"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = PostManagementSmallShape,
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ATTENDANCE PIN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextSecondary
                        )
                        Text(
                            text = session.pinCode.toSixDigitPinDisplay(),
                            modifier = Modifier.padding(top = 3.dp),
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkPrimaryGreen
                        )
                        Text(
                            text = if (isToday) {
                                "Share this 6-digit PIN with today's Physical volunteers."
                            } else {
                                "PIN used for this Physical event day."
                            },
                            modifier = Modifier.padding(top = 4.dp),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }

                if (isToday && !attendance.isLiveWindowOpen) {
                    Text(
                        text = "Volunteer PIN check-in is closed. Organisation corrections are still available below.",
                        modifier = Modifier.padding(top = 9.dp),
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isToday) "Checked in today" else "Present on this day",
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextSecondary
                    )
                    Text(
                        text = "$selectedPresentCount / $selectedEligibleVolunteerCount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }

            if (!actionMessage.isNullOrBlank()) {
                Text(
                    text = actionMessage,
                    modifier = Modifier.padding(top = 9.dp),
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    color = VolunteerLinkError
                )
            }
        }
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
            text = "Attendance Day",
            fontSize = 11.sp,
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
                modifier = Modifier.padding(top = 7.dp),
                fontSize = 9.sp,
                lineHeight = 13.sp,
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PostManagementSmallShape,
        color = VolunteerLinkBackground,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance",
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = "${summary.attendedDays} / ${summary.expectedDays} days",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
            }

            Text(
                text = "${summary.verifiedMinutes.toVerifiedTimeLabel()} verified",
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 9.sp,
                color = VolunteerLinkTextSecondary
            )

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
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 9.sp,
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
                            .padding(top = 8.dp),
                        enabled = !isUpdatingAttendance
                    ) {
                        Text(
                            text = if (isUpdatingAttendance) "Updating..." else "Mark Absent",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onMarkPresent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        enabled = !isUpdatingAttendance
                    ) {
                        Text(
                            text = if (isUpdatingAttendance) "Updating..." else "Mark Present",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                fontSize = 11.sp,
                lineHeight = 16.sp,
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
    Column(modifier = modifier.fillMaxWidth()) {
        if (showApplicantsTab) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
                color = VolunteerLinkSoftGreenSurface
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
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
            Text(
                text = "Volunteers $volunteerCount",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
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
                    modifier = Modifier.size(19.dp),
                    tint = VolunteerLinkTextSecondary
                )
            }
        )

        if (roles.isNotEmpty()) {
            Text(
                text = "Filter by role",
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextSecondary
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
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
        shape = RoundedCornerShape(8.dp),
        color = if (selected) VolunteerLinkSurface else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (selected) {
                    VolunteerLinkPrimaryGreen
                } else {
                    VolunteerLinkTextSecondary
                }
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 5.dp),
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) VolunteerLinkPrimaryGreen else VolunteerLinkTextSecondary,
                maxLines = 1
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
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            fontSize = 10.sp,
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = role.roleName,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

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
            modifier = Modifier.padding(top = 3.dp),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = VolunteerLinkTextSecondary
        )

        HorizontalDivider(
            modifier = Modifier.padding(top = 9.dp),
            color = VolunteerLinkBorderColour
        )
    }
}

@Composable
private fun PostManagementApplicationWindowPill(
    role: PostManagementRole,
    modifier: Modifier = Modifier
) {
    val isOpen = role.isApplicationOpen
    val background = if (isOpen) {
        VolunteerLinkPrimaryGreen.copy(alpha = 0.10f)
    } else {
        VolunteerLinkWarning.copy(alpha = 0.10f)
    }
    val foreground = if (isOpen) VolunteerLinkPrimaryGreen else VolunteerLinkWarning

    Surface(
        modifier = modifier,
        shape = PostManagementPillShape,
        color = background,
        border = BorderStroke(1.dp, foreground.copy(alpha = 0.22f))
    ) {
        Text(
            text = if (isOpen) "OPEN" else "CLOSED",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = foreground
        )
    }
}

@Composable
internal fun PostManagementPersonCard(
    person: PostManagementPerson,
    isApplicant: Boolean,
    isApplicationOpen: Boolean,
    attendanceSummary: PostManagementVolunteerAttendanceSummary? = null,
    attendanceSelectedDate: String? = null,
    attendanceTodayDate: String? = null,
    canCorrectAttendance: Boolean = false,
    isUpdatingAttendance: Boolean = false,
    onMarkPresent: (PostManagementPerson, String) -> Unit = { _, _ -> },
    onRequestMarkAbsent: (PostManagementPerson, String) -> Unit = { _, _ -> },
    onViewProfile: (PostManagementPerson) -> Unit,
    onToggleShortlist: (PostManagementPerson) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PostManagementCardShape,
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.person_placeholder),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
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
                        text = person.fullName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary,
                        maxLines = 1,
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
                        fontSize = 10.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (isApplicant) {
                        IconButton(
                            onClick = { onToggleShortlist(person) },
                            modifier = Modifier.size(32.dp),
                            enabled = isApplicationOpen
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (person.isShortlisted) {
                                        R.drawable.shortlist_filled
                                    } else {
                                        R.drawable.shortlist_outline
                                    }
                                ),
                                contentDescription = if (person.isShortlisted) {
                                    "Remove from shortlist"
                                } else {
                                    "Add to shortlist"
                                },
                                modifier = Modifier.size(20.dp),
                                tint = when {
                                    !isApplicationOpen -> VolunteerLinkTextSecondary.copy(alpha = 0.35f)
                                    person.isShortlisted -> VolunteerLinkWarning
                                    else -> VolunteerLinkTextSecondary
                                }
                            )
                        }
                    }

                    Surface(
                        shape = PostManagementPillShape,
                        color = if (isApplicant) {
                            VolunteerLinkWarning.copy(alpha = 0.09f)
                        } else {
                            VolunteerLinkSoftGreenSurface
                        }
                    ) {
                        Text(
                            text = if (isApplicant) "Pending" else person.completionStatus.toCompletionLabel(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isApplicant) VolunteerLinkWarning else VolunteerLinkPrimaryGreen
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
                    onMarkPresent = {
                        onMarkPresent(person, attendanceSelectedDate)
                    },
                    onRequestMarkAbsent = {
                        onRequestMarkAbsent(person, attendanceSelectedDate)
                    },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onViewProfile(person) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.profile),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Profile",
                        modifier = Modifier.padding(start = 5.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isApplicant) {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f),
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
                            text = if (isApplicationOpen) "Review" else "Closed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = VolunteerLinkPrimaryGreen.copy(alpha = 0.18f),
                            disabledContentColor = VolunteerLinkPrimaryGreen.copy(alpha = 0.60f)
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chat),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Message",
                            modifier = Modifier.padding(start = 5.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isApplicant && !isApplicationOpen) {
                Text(
                    text = "Application review is closed for this role.",
                    modifier = Modifier.padding(top = 7.dp),
                    fontSize = 9.sp,
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
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
        fontSize = 11.sp,
        lineHeight = 16.sp,
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
            fontSize = 9.sp,
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
            fontSize = 8.sp,
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
                    fontSize = 11.sp,
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
            fontSize = 8.sp,
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
