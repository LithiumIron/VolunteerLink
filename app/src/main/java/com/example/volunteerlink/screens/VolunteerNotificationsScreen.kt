package com.example.volunteerlink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.data.VolunteerNotification
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSuccess
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning

private enum class VolunteerNotificationFilter(val label: String) {
    ALL("All"),
    APPLICATIONS("Applications"),
    REMINDERS("Reminders"),
    ACHIEVEMENTS("Achievements")
}

private enum class VolunteerNotificationCategory {
    APPLICATION,
    REMINDER,
    ACHIEVEMENT,
    SYSTEM
}

@Composable
fun VolunteerNotificationsScreen(
    onBackSelected: () -> Unit,
    onApplicationsSelected: () -> Unit,
    onOpportunitySelected: (String) -> Unit,
    onSkillPathSelected: () -> Unit,
    notificationViewModel: VolunteerNotificationViewModel = viewModel()
) {
    val uiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by rememberSaveable {
        mutableStateOf(VolunteerNotificationFilter.ALL)
    }

    val visibleNotifications = uiState.notifications.filter { notification ->
        when (selectedFilter) {
            VolunteerNotificationFilter.ALL -> true
            VolunteerNotificationFilter.APPLICATIONS ->
                notification.category() == VolunteerNotificationCategory.APPLICATION
            VolunteerNotificationFilter.REMINDERS ->
                notification.category() == VolunteerNotificationCategory.REMINDER
            VolunteerNotificationFilter.ACHIEVEMENTS ->
                notification.category() == VolunteerNotificationCategory.ACHIEVEMENT
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VolunteerLinkPrimaryGreen)
                .statusBarsPadding()
                .padding(start = 4.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackSelected) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Notifications",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (uiState.unreadCount == 0) {
                        "You're up to date"
                    } else {
                        "${uiState.unreadCount} unread update" +
                            if (uiState.unreadCount == 1) "" else "s"
                    },
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp
                )
            }
            if (uiState.unreadCount > 0) {
                TextButton(
                    onClick = notificationViewModel::markAllRead,
                    enabled = !uiState.isMarkingRead
                ) {
                    Text(
                        if (uiState.isMarkingRead) "Updating..." else "Mark all read",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(VolunteerNotificationFilter.entries, key = { it.name }) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = VolunteerLinkSurface,
                        selectedContainerColor = VolunteerLinkPrimaryGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VolunteerLinkPrimaryGreen)
            }

            uiState.errorMessage != null && uiState.notifications.isEmpty() ->
                NotificationStateMessage(
                    title = "Notifications unavailable",
                    message = uiState.errorMessage!!,
                    actionLabel = "Retry",
                    onAction = notificationViewModel::refresh
                )

            visibleNotifications.isEmpty() -> NotificationStateMessage(
                title = if (selectedFilter == VolunteerNotificationFilter.ALL) {
                    "You're all caught up"
                } else {
                    "No ${selectedFilter.label.lowercase()} yet"
                },
                message = "Application decisions, event reminders, verified " +
                    "completions and credentials will appear here."
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(visibleNotifications, key = { it.notificationId }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = {
                            when (notification.category()) {
                                VolunteerNotificationCategory.APPLICATION ->
                                    onApplicationsSelected()
                                VolunteerNotificationCategory.ACHIEVEMENT ->
                                    onSkillPathSelected()
                                VolunteerNotificationCategory.REMINDER,
                                VolunteerNotificationCategory.SYSTEM ->
                                    notification.relatedPostId?.let(onOpportunitySelected)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: VolunteerNotification,
    onClick: () -> Unit
) {
    val category = notification.category()
    val accentColour = when (category) {
        VolunteerNotificationCategory.APPLICATION -> VolunteerLinkInformation
        VolunteerNotificationCategory.REMINDER -> VolunteerLinkWarning
        VolunteerNotificationCategory.ACHIEVEMENT -> VolunteerLinkSuccess
        VolunteerNotificationCategory.SYSTEM -> VolunteerLinkPrimaryGreen
    }
    val actionLabel = when (category) {
        VolunteerNotificationCategory.APPLICATION -> "View applications"
        VolunteerNotificationCategory.REMINDER ->
            if (notification.relatedPostId != null) "View opportunity" else null
        VolunteerNotificationCategory.ACHIEVEMENT -> "View verified growth"
        VolunteerNotificationCategory.SYSTEM ->
            if (notification.relatedPostId != null) "View opportunity" else null
    }

    Card(
        onClick = onClick,
        enabled = actionLabel != null,
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                VolunteerLinkSurface
            } else VolunteerLinkSoftGreenSurface
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accentColour.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (category) {
                        VolunteerNotificationCategory.APPLICATION -> Icons.Filled.Description
                        VolunteerNotificationCategory.REMINDER -> Icons.Filled.CalendarMonth
                        VolunteerNotificationCategory.ACHIEVEMENT ->
                            if (notification.notificationType.contains("CERTIFICATE", true)) {
                                Icons.Filled.CheckCircle
                            } else Icons.Filled.School
                        VolunteerNotificationCategory.SYSTEM -> Icons.Filled.Notifications
                    },
                    contentDescription = null,
                    tint = accentColour
                )
            }
            Spacer(Modifier.size(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        notification.title,
                        modifier = Modifier.weight(1f),
                        color = VolunteerLinkTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!notification.isRead) {
                        Box(
                            Modifier
                                .padding(start = 8.dp)
                                .size(8.dp)
                                .background(VolunteerLinkPrimaryGreen, CircleShape)
                        )
                    }
                }
                Text(
                    notification.message,
                    modifier = Modifier.padding(top = 4.dp),
                    color = VolunteerLinkTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatNotificationTime(notification.createdAt),
                        modifier = Modifier.weight(1f),
                        color = VolunteerLinkTextSecondary,
                        fontSize = 10.sp
                    )
                    actionLabel?.let {
                        Text(
                            it,
                            color = VolunteerLinkPrimaryGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = VolunteerLinkPrimaryGreen,
                            modifier = Modifier.padding(start = 3.dp).size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationStateMessage(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).background(VolunteerLinkSoftGreenSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = VolunteerLinkPrimaryGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                title,
                modifier = Modifier.padding(top = 14.dp),
                color = VolunteerLinkTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                message,
                modifier = Modifier.padding(top = 6.dp),
                color = VolunteerLinkTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            actionLabel?.let {
                Button(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VolunteerLinkPrimaryGreen
                    )
                ) { Text(it) }
            }
        }
    }
}

private fun VolunteerNotification.category(): VolunteerNotificationCategory {
    val type = notificationType.uppercase()
    return when {
        type.contains("APPLICATION") || type.contains("PARTICIPATION") ->
            VolunteerNotificationCategory.APPLICATION
        type.contains("REMINDER") || type.contains("SCHEDULE") ||
            type.contains("EVENT") -> VolunteerNotificationCategory.REMINDER
        type.contains("SKILL") || type.contains("CERTIFICATE") ||
            type.contains("BADGE") || type.contains("COMPLETED") ->
            VolunteerNotificationCategory.ACHIEVEMENT
        else -> VolunteerNotificationCategory.SYSTEM
    }
}

private fun formatNotificationTime(timestamp: String): String =
    timestamp.take(16).replace('T', ' ')
