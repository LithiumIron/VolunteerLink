package com.example.volunteerlink.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSuccess
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning

private data class VolunteerApplicationTimelineStep(
    val title: String,
    val supportingText: String,
    val state: String
)

@Composable
fun VolunteerMyApplicationsScreen(
    onBackSelected: () -> Unit,
    onVolunteerApplicationSelected: (
        applicationId: Int
    ) -> Unit
) {
    var selectedStatusFilter by
        rememberSaveable {
            mutableStateOf("All")
        }

    val statusFilters =
        listOf(
            "All",
            "Pending",
            "Accepted",
            "Rejected",
            "Completed"
        )

    val filteredApplications =
        VolunteerOpportunitySessionStore
            .volunteerApplications
            .filter { volunteerApplication ->
                when (selectedStatusFilter) {
                    "Pending" ->
                        volunteerApplication.applicationStatus ==
                                VolunteerApplicationStatus.PENDING

                    "Accepted" ->
                        volunteerApplication.applicationStatus ==
                                VolunteerApplicationStatus.ACCEPTED

                    "Rejected" ->
                        volunteerApplication.applicationStatus ==
                            VolunteerApplicationStatus.REJECTED

                    "Completed" ->
                        volunteerApplication.applicationStatus ==
                            VolunteerApplicationStatus.COMPLETED

                    else -> true
                }
            }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        VolunteerApplicationsTopBar(
            title = "My Applications",
            onBackSelected = onBackSelected
        )

        LazyRow(
            modifier = Modifier.padding(
                top = 12.dp
            ),
            contentPadding = PaddingValues(
                horizontal =
                    VolunteerLinkScreenHorizontalPadding
            ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = statusFilters,
                key = { statusFilter ->
                    statusFilter
                }
            ) { statusFilter ->
                FilterChip(
                    selected =
                        selectedStatusFilter ==
                                statusFilter,
                    onClick = {
                        selectedStatusFilter =
                            statusFilter
                    },
                    label = {
                        Text(
                            text = statusFilter,
                            fontSize = 11.sp
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors =
                        FilterChipDefaults
                            .filterChipColors(
                                containerColor =
                                    VolunteerLinkSurface,
                                labelColor =
                                    VolunteerLinkTextSecondary,
                                selectedContainerColor =
                                    VolunteerLinkPrimaryGreen,
                                selectedLabelColor =
                                    Color.White
                            )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        VolunteerLinkScreenHorizontalPadding,
                    vertical = 12.dp
                ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = "Application history",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Text(
                text =
                    "${filteredApplications.size} total",
                fontSize = 11.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        if (filteredApplications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {
                Text(
                    text = "No applications found",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Applications matching this status will appear here.",
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start =
                        VolunteerLinkScreenHorizontalPadding,
                    end =
                        VolunteerLinkScreenHorizontalPadding,
                    bottom = 24.dp
                ),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredApplications,
                    key = { volunteerApplication ->
                        volunteerApplication.applicationId
                    }
                ) { volunteerApplication ->
                    VolunteerApplicationListCard(
                        volunteerApplication =
                            volunteerApplication,
                        onSelected = {
                            onVolunteerApplicationSelected(
                                volunteerApplication.applicationId
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VolunteerApplicationDetailsScreen(
    volunteerApplicationId: Int,
    onBackSelected: () -> Unit,
    onVolunteerOpportunitySelected: (
        eventId: Int
    ) -> Unit,
    onVolunteerRoleSelected: (
        eventId: Int,
        roleId: Int
    ) -> Unit,
    onCertificateSelected: (
        applicationId: Int
    ) -> Unit,
    volunteerOpportunityViewModel:
        VolunteerOpportunityViewModel
) {
    val opportunityUiState by
        volunteerOpportunityViewModel.uiState
            .collectAsStateWithLifecycle()

    var shouldShowCancelDialog by
        rememberSaveable {
            mutableStateOf(false)
        }

    val volunteerApplication =
        VolunteerOpportunitySessionStore
            .findApplicationById(
                volunteerApplicationId
            )

    if (volunteerApplication == null) {
        VolunteerApplicationDetailsNotFoundScreen(
            onBackSelected = onBackSelected
        )
        return
    }

    val volunteerOpportunityEvent =
        VolunteerOpportunitySessionStore.findEventById(
            volunteerApplication.applicationEventId
        )

    val volunteerOpportunityRole =
        volunteerApplication.applicationRoleId
            ?.let { applicationRoleId ->
                VolunteerOpportunitySessionStore.findRoleById(
                    eventId =
                        volunteerApplication.applicationEventId,
                    roleId = applicationRoleId
                )
            }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        VolunteerApplicationsTopBar(
            title = "Application Details",
            onBackSelected = onBackSelected
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal =
                    VolunteerLinkScreenHorizontalPadding,
                vertical = 18.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            item(
                key = "application_status"
            ) {
                VolunteerApplicationStatusCard(
                    volunteerApplication =
                        volunteerApplication
                )
            }

            item(
                key = "application_timeline"
            ) {
                VolunteerApplicationTimelineCard(
                    volunteerApplication =
                        volunteerApplication
                )
            }

            item(
                key = "application_information"
            ) {
                VolunteerApplicationInformationCard(
                    volunteerApplication =
                        volunteerApplication,
                    eventDate =
                        volunteerOpportunityEvent?.eventDate
                            ?: volunteerApplication.applicationEventDate,
                    eventTime =
                        volunteerOpportunityEvent?.eventTime
                            ?: volunteerApplication.applicationEventTime,
                    eventLocation =
                        volunteerOpportunityEvent
                            ?.let { event ->
                                event.eventFullAddress
                                    .ifBlank {
                                        event.eventLocation
                                    }
                            }
                            ?: volunteerApplication
                                .applicationEventLocation
                )
            }

            item(
                key = "application_actions"
            ) {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(9.dp)
                ) {
                    if (
                        volunteerApplication.applicationStatus ==
                        VolunteerApplicationStatus.COMPLETED
                    ) {
                        Button(
                            onClick = {
                                onCertificateSelected(
                                    volunteerApplication.applicationId
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor =
                                    VolunteerLinkPrimaryGreen
                            )
                        ) {
                            Text(
                                text = "View & Download Certificate",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (volunteerOpportunityEvent != null) {
                        OutlinedButton(
                            onClick = {
                                onVolunteerOpportunitySelected(
                                    volunteerOpportunityEvent.eventId
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape =
                                RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = VolunteerLinkPrimaryGreen
                            )
                        ) {
                            Text(
                                text = "View Opportunity",
                                fontWeight = FontWeight.Bold,
                                color = VolunteerLinkPrimaryGreen
                            )
                        }
                    }

                    if (
                        volunteerOpportunityRole != null
                    ) {
                        OutlinedButton(
                            onClick = {
                                onVolunteerRoleSelected(
                                    volunteerApplication
                                        .applicationEventId,
                                    volunteerOpportunityRole.roleId
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape =
                                RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    VolunteerLinkPrimaryGreen
                            )
                        ) {
                            Text(
                                text = "View Role Details",
                                fontWeight = FontWeight.Bold,
                                color =
                                    VolunteerLinkPrimaryGreen
                            )
                        }
                    }

                    if (
                        volunteerApplication.applicationStatus ==
                        VolunteerApplicationStatus.PENDING
                    ) {
                        TextButton(
                            onClick = {
                                shouldShowCancelDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cancel Application",
                                fontWeight = FontWeight.SemiBold,
                                color = VolunteerLinkError
                            )
                        }
                    }
                }
            }
        }
    }

    if (shouldShowCancelDialog) {
        AlertDialog(
            onDismissRequest = {
                shouldShowCancelDialog = false
            },
            title = {
                Text(
                    text = "Cancel application?"
                )
            },
            text = {
                Column {
                    Text(
                        text =
                            "This application will be marked as cancelled."
                    )

                    opportunityUiState
                        .applicationActionError
                        ?.let { errorMessage ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                color = VolunteerLinkError,
                                fontSize = 12.sp
                            )
                        }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        volunteerOpportunityViewModel
                            .cancelApplication(
                                applicationId =
                                    volunteerApplicationId,
                                onSuccess = {
                                    shouldShowCancelDialog = false
                                }
                            )
                    },
                    enabled =
                        !opportunityUiState
                            .isApplicationActionRunning
                ) {
                    Text(
                        text =
                            if (
                                opportunityUiState
                                    .isApplicationActionRunning
                            ) {
                                "Cancelling..."
                            } else {
                                "Cancel application"
                            },
                        color = VolunteerLinkError
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        shouldShowCancelDialog = false
                    }
                ) {
                    Text("Keep application")
                }
            }
        )
    }
}

@Composable
private fun VolunteerApplicationsTopBar(
    title: String,
    onBackSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkPrimaryGreen)
            .statusBarsPadding()
            .height(56.dp)
            .padding(
                start = 4.dp,
                end = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackSelected
        ) {
            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun VolunteerApplicationListCard(
    volunteerApplication:
        VolunteerOpportunityApplication,
    onSelected: () -> Unit
) {
    Card(
        onClick = onSelected,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkBorderColour
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        volunteerApplication
                            .applicationEventTitle,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.size(8.dp)
                )

                VolunteerApplicationStatusBadge(
                    applicationStatus =
                        volunteerApplication
                            .applicationStatus
                )
            }

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    volunteerApplication.applicationRoleTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkPrimaryGreen
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text =
                    volunteerApplication
                        .applicationOrganisationName,
                fontSize = 11.sp,
                color = VolunteerLinkTextSecondary
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            HorizontalDivider(
                color = VolunteerLinkBorderColour
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        "Submitted ${volunteerApplication.applicationSubmittedDate}",
                    fontSize = 10.sp,
                    color = VolunteerLinkTextSecondary
                )

                Text(
                    text = "View  ›",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
            }
        }
    }
}

@Composable
private fun VolunteerApplicationStatusCard(
    volunteerApplication:
        VolunteerOpportunityApplication
) {
    val statusColour =
        volunteerApplicationStatusColour(
            volunteerApplication.applicationStatus
        )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColour.copy(
                alpha = 0.10f
            )
        ),
        border = BorderStroke(
            width = 1.dp,
            color = statusColour.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(27.dp),
                        tint = statusColour
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        volunteerApplicationStatusText(
                            volunteerApplication
                                .applicationStatus
                        ),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColour
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text =
                        volunteerApplication
                            .applicationStatusMessage
                            .ifBlank {
                                "The latest application status is shown here."
                            },
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }
    }
}

@Composable
private fun VolunteerApplicationTimelineCard(
    volunteerApplication:
        VolunteerOpportunityApplication
) {
    val steps =
        volunteerApplicationTimelineSteps(
            volunteerApplication
        )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkBorderColour
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Application Journey",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text =
                    "Follow what has happened and what comes next.",
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 10.sp,
                color = VolunteerLinkTextSecondary
            )

            Spacer(modifier = Modifier.height(13.dp))

            steps.forEachIndexed { index, step ->
                VolunteerApplicationTimelineRow(
                    step = step,
                    showConnector = index < steps.lastIndex
                )
            }
        }
    }
}

@Composable
private fun VolunteerApplicationTimelineRow(
    step: VolunteerApplicationTimelineStep,
    showConnector: Boolean
) {
    val stepColour = when (step.state) {
        "COMPLETE" -> VolunteerLinkSuccess
        "CURRENT" -> VolunteerLinkInformation
        "ERROR" -> VolunteerLinkError
        else -> VolunteerLinkBorderColour
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = CircleShape,
                color = stepColour.copy(
                    alpha =
                        if (step.state == "PENDING") 0.20f
                        else 1f
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (step.state == "COMPLETE") {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Completed step",
                            modifier = Modifier.size(17.dp),
                            tint = Color.White
                        )
                    } else {
                        Text(
                            text =
                                if (step.state == "ERROR") "!"
                                else "•",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color =
                                if (step.state == "PENDING") {
                                    VolunteerLinkTextSecondary
                                } else {
                                    Color.White
                                }
                        )
                    }
                }
            }

            if (showConnector) {
                Box(
                    modifier = Modifier
                        .size(
                            width = 2.dp,
                            height = 34.dp
                        )
                        .background(
                            if (step.state == "COMPLETE") {
                                VolunteerLinkSuccess.copy(alpha = 0.35f)
                            } else {
                                VolunteerLinkBorderColour
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.size(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = step.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (step.state == "PENDING") {
                            VolunteerLinkTextSecondary
                        } else {
                            VolunteerLinkTextPrimary
                        }
                )

                if (step.state == "CURRENT") {
                    Text(
                        text = "  CURRENT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkInformation
                    )
                }
            }
            Text(
                text = step.supportingText,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

private fun volunteerApplicationTimelineSteps(
    application: VolunteerOpportunityApplication
): List<VolunteerApplicationTimelineStep> {
    val submitted =
        VolunteerApplicationTimelineStep(
            title = "Application submitted",
            supportingText =
                "Sent on ${application.applicationSubmittedDate}.",
            state = "COMPLETE"
        )

    return when (application.applicationStatus) {
        VolunteerApplicationStatus.PENDING ->
            listOf(
                submitted,
                VolunteerApplicationTimelineStep(
                    title = "Organisation review",
                    supportingText =
                        "The organisation is reviewing your application.",
                    state = "CURRENT"
                ),
                VolunteerApplicationTimelineStep(
                    title = "Decision",
                    supportingText =
                        "You will see the decision here once it is made.",
                    state = "PENDING"
                )
            )

        VolunteerApplicationStatus.ACCEPTED ->
            listOf(
                submitted,
                VolunteerApplicationTimelineStep(
                    title = "Application accepted",
                    supportingText =
                        "Your place for this role has been confirmed.",
                    state = "COMPLETE"
                ),
                VolunteerApplicationTimelineStep(
                    title = "Complete the volunteer role",
                    supportingText =
                        "Attend or submit the work. Accepted is not yet Completed.",
                    state = "CURRENT"
                ),
                VolunteerApplicationTimelineStep(
                    title = "Organisation verification",
                    supportingText =
                        "Verified completion will update Skill Path and certificate.",
                    state = "PENDING"
                )
            )

        VolunteerApplicationStatus.REJECTED ->
            listOf(
                submitted,
                VolunteerApplicationTimelineStep(
                    title = "Organisation review completed",
                    supportingText =
                        "The organisation assessed the application.",
                    state = "COMPLETE"
                ),
                VolunteerApplicationTimelineStep(
                    title = "Not selected for this role",
                    supportingText =
                        application.applicationRejectionReason
                            ?.takeIf(String::isNotBlank)
                            ?: "Open the decision details for more information.",
                    state = "ERROR"
                )
            )

        VolunteerApplicationStatus.COMPLETED ->
            listOf(
                submitted,
                VolunteerApplicationTimelineStep(
                    title = "Application accepted",
                    supportingText =
                        "The organisation confirmed your place.",
                    state = "COMPLETE"
                ),
                VolunteerApplicationTimelineStep(
                    title = "Volunteer role completed",
                    supportingText =
                        "Completed ${application.applicationCompletedDate ?: "and recorded"}.",
                    state = "COMPLETE"
                ),
                VolunteerApplicationTimelineStep(
                    title = "Verified achievement issued",
                    supportingText =
                        "Skill Path evidence and certificate are now available.",
                    state = "CURRENT"
                )
            )

        VolunteerApplicationStatus.CANCELLED ->
            listOf(
                submitted,
                VolunteerApplicationTimelineStep(
                    title = "Application cancelled",
                    supportingText =
                        "This application is closed and will not be reviewed.",
                    state = "ERROR"
                )
            )
    }
}

@Composable
private fun VolunteerApplicationInformationCard(
    volunteerApplication:
        VolunteerOpportunityApplication,
    eventDate: String?,
    eventTime: String?,
    eventLocation: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkBorderColour
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text =
                    volunteerApplication.applicationEventTitle,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    volunteerApplication
                        .applicationOrganisationName,
                fontSize = 12.sp,
                color = VolunteerLinkTextSecondary
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            HorizontalDivider(
                color = VolunteerLinkBorderColour
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            VolunteerApplicationInformationRow(
                label = "Role",
                value =
                    volunteerApplication.applicationRoleTitle
            )

            VolunteerApplicationInformationRow(
                label = "Submitted",
                value =
                    volunteerApplication
                        .applicationSubmittedDate
            )

            if (
                eventDate != null ||
                eventTime != null
            ) {
                VolunteerApplicationInformationRow(
                    label = "Event schedule",
                    value = listOfNotNull(
                        eventDate,
                        eventTime
                    ).joinToString(" • ")
                )
            }

            if (!eventLocation.isNullOrBlank()) {
                VolunteerApplicationInformationRow(
                    label = "Location",
                    value = eventLocation
                )
            }

            volunteerApplication
                .applicationRejectionReason
                ?.let { rejectionReason ->
                    VolunteerApplicationInformationRow(
                        label = "Reason",
                        value = rejectionReason
                    )
                }

            if (
                volunteerApplication
                    .applicationVerifiedMinutes != null
            ) {
                VolunteerApplicationInformationRow(
                    label = "Verified service",
                    value =
                        formatVerifiedServiceTime(
                            volunteerApplication
                                .applicationVerifiedMinutes
                                ?: 0
                        )
                )
            }

            volunteerApplication.applicationCompletedDate
                ?.let { completedDate ->
                    VolunteerApplicationInformationRow(
                        label = "Completed",
                        value = completedDate
                    )
                }

            volunteerApplication.applicationCertificateId
                ?.let { certificateId ->
                    VolunteerApplicationInformationRow(
                        label = "Certificate",
                        value = certificateId
                    )
                }

            volunteerApplication.applicationOrganisationFeedback
                ?.takeIf(String::isNotBlank)
                ?.let { feedback ->
                    VolunteerApplicationInformationRow(
                        label = "Feedback",
                        value = feedback
                    )
                }
        }
    }
}

private fun formatVerifiedServiceTime(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours == 0 -> "$remainingMinutes minutes"
        remainingMinutes == 0 -> "$hours hours"
        else -> "$hours hours $remainingMinutes minutes"
    }
}

@Composable
private fun VolunteerApplicationInformationRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.38f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = VolunteerLinkTextSecondary
        )

        Text(
            text = value,
            modifier = Modifier.weight(0.62f),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = VolunteerLinkTextPrimary
        )
    }
}

@Composable
private fun VolunteerApplicationStatusBadge(
    applicationStatus: VolunteerApplicationStatus
) {
    val statusColour =
        volunteerApplicationStatusColour(
            applicationStatus
        )

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = statusColour.copy(alpha = 0.12f)
    ) {
        Text(
            text =
                volunteerApplicationStatusText(
                    applicationStatus
                ),
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = statusColour
        )
    }
}

private fun volunteerApplicationStatusText(
    applicationStatus: VolunteerApplicationStatus
): String {
    return when (applicationStatus) {
        VolunteerApplicationStatus.PENDING ->
            "Pending"

        VolunteerApplicationStatus.ACCEPTED ->
            "Accepted"

        VolunteerApplicationStatus.REJECTED ->
            "Rejected"

        VolunteerApplicationStatus.COMPLETED ->
            "Completed"

        VolunteerApplicationStatus.CANCELLED ->
            "Cancelled"
    }
}

private fun volunteerApplicationStatusColour(
    applicationStatus: VolunteerApplicationStatus
): Color {
    return when (applicationStatus) {
        VolunteerApplicationStatus.PENDING ->
            VolunteerLinkWarning

        VolunteerApplicationStatus.ACCEPTED ->
            VolunteerLinkSuccess

        VolunteerApplicationStatus.REJECTED ->
            VolunteerLinkError

        VolunteerApplicationStatus.COMPLETED ->
            VolunteerLinkInformation

        VolunteerApplicationStatus.CANCELLED ->
            VolunteerLinkTextSecondary
    }
}

@Composable
private fun VolunteerApplicationDetailsNotFoundScreen(
    onBackSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "Application not found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            onClick = onBackSelected,
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    VolunteerLinkPrimaryGreen
            )
        ) {
            Text("Return")
        }
    }
}
