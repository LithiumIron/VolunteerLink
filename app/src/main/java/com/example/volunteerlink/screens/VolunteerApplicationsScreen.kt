
package com.example.volunteerlink.screens

// Shows application history, filters, status details, cancellation and reapplication actions.

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
// Purpose: Displays the volunteer application history and applies the selected status or Today filter.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
fun VolunteerMyApplicationsScreen(
    onBackSelected: () -> Unit,
    onVolunteerApplicationSelected: (
        applicationId: Int
    ) -> Unit
) {
    // Use the shared app clock so the Today filters match the database test/real time setting.
    val businessNow = volunteerBusinessTime()
    var selectedStatusFilter by
    rememberSaveable {
        mutableStateOf("All")
    }

    // These labels are also handled by the filtering rules below.
    val statusFilters =
        listOf(
            "All",
            "Today",
            "Physical Today",
            "Waiting to sync",
            "Pending",
            "Accepted",
            "Rejected",
            "Completed",
            "Not Completed",
            "Cancelled"
        )

    // Filter only the already-loaded application list; this does not make a new database request.
    val filteredApplications =
        VolunteerOpportunitySessionStore
            .volunteerApplications
            .filter { volunteerApplication ->
                // Choose one mutually exclusive UI result from the current status or user selection.
                when (selectedStatusFilter) {
                    "Today" -> volunteerApplicationIsRelevantToday(volunteerApplication, businessNow)
                    "Physical Today" -> volunteerApplicationIsPhysicalToday(volunteerApplication, businessNow)
                    "Waiting to sync" -> volunteerApplication.applicationDatabaseId.startsWith("offline|")
                    "Cancelled" -> volunteerApplication.applicationStatus == VolunteerApplicationStatus.CANCELLED
                    "Pending" ->
                        !volunteerApplication.applicationDatabaseId.startsWith("offline|") &&
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

                    "Not Completed" ->
                        volunteerApplication.applicationStatus ==
                                VolunteerApplicationStatus.NOT_COMPLETED

                    else -> true
                }
            }

    // Arrange the following screen content vertically inside the available space.
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
            // Render one reusable row/card for each item in the prepared list.
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
                        // Display the prepared label; business rules are calculated before reaching this UI call.
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

        // Arrange the following controls horizontally and keep their alignment consistent.
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
            // Display the prepared label; business rules are calculated before reaching this UI call.
            Text(
                text = "Application history",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            // Display the prepared label; business rules are calculated before reaching this UI call.
            Text(
                text =
                    "${filteredApplications.size} total",
                fontSize = 11.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        // Check this condition before showing the action, preventing an invalid Volunteer flow from continuing.
        if (filteredApplications.isEmpty()) {
            // Arrange the following screen content vertically inside the available space.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {
                // Display the prepared label; business rules are calculated before reaching this UI call.
                Text(
                    text = "No applications found",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                // Display the prepared label; business rules are calculated before reaching this UI call.
                Text(
                    text =
                        "Applications matching this status will appear here.",
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        } else {
            // Render this content as a lazy scrolling list so only visible items need to be composed.
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
                // Render one reusable row/card for each item in the prepared list.
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
// Purpose: Explains one application status, timeline, attendance, submission and certificate actions.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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
    var shouldShowEditDialog by rememberSaveable { mutableStateOf(false) }
    var shouldShowDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCancellationReason by rememberSaveable { mutableStateOf("") }
    var cancellationDetails by rememberSaveable { mutableStateOf("") }
    var formAnswers by remember { mutableStateOf(emptyList<String>()) }

    // Resolve or prepare the application data used by the next status, cancellation or navigation decision.
    val volunteerApplication =
        VolunteerOpportunitySessionStore
            .findApplicationById(
                volunteerApplicationId
            )

    // Check this condition before showing the action, preventing an invalid Volunteer flow from continuing.
    if (volunteerApplication == null) {
        VolunteerApplicationDetailsNotFoundScreen(
            onBackSelected = onBackSelected
        )
        // Keep this Compose block separate so its visual state follows the value prepared above.
        return
    }

    // Resolve or prepare event data from the shared dashboard snapshot for this part of the screen.
    val volunteerOpportunityEvent =
        VolunteerOpportunitySessionStore.findEventById(
            volunteerApplication.applicationEventId
        )

    // Resolve or prepare event data from the shared dashboard snapshot for this part of the screen.
    val volunteerOpportunityRole =
        volunteerApplication.applicationRoleId
            ?.let { applicationRoleId ->
                VolunteerOpportunitySessionStore.findRoleById(
                    eventId =
                        volunteerApplication.applicationEventId,
                    roleId = applicationRoleId
                )
            }

    // Name the calculated phone contact is relevant value because later UI branches reuse it during this Compose pass.
    val phoneContactIsRelevant =
        volunteerApplication.applicationStatus == VolunteerApplicationStatus.ACCEPTED &&
            volunteerOpportunityEvent != null &&
            volunteerOpportunityRole != null &&
            volunteerOpportunityEvent.eventDatabaseId.isNotBlank() &&
            volunteerOpportunityRole.roleTemplateId.isNotBlank() &&
            volunteerOpportunityRole.roleMode.uppercase() in setOf("PHYSICAL", "REMOTE")

    LaunchedEffect(
        volunteerApplication.applicationDatabaseId,
        volunteerOpportunityEvent?.eventDatabaseId,
        volunteerOpportunityRole?.roleTemplateId,
        phoneContactIsRelevant
    ) {
        if (phoneContactIsRelevant) {
            volunteerOpportunityViewModel.loadEventPhoneContact(
                postId = volunteerOpportunityEvent!!.eventDatabaseId,
                roleTemplateId = volunteerOpportunityRole!!.roleTemplateId
            )
        } else {
            volunteerOpportunityViewModel.clearEventPhoneContact()
        }
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
                // Resolve or prepare the application data used by the next status, cancellation or navigation decision.
                val applicationIsRemote =
                    volunteerOpportunityRole?.roleMode == "REMOTE" ||
                            volunteerApplication.applicationRoleMode == "REMOTE" ||
                            volunteerOpportunityEvent
                                ?.eventOpportunityType == "Remote"

                VolunteerApplicationInformationCard(
                    volunteerApplication =
                        volunteerApplication,
                    applicationIsRemote =
                        applicationIsRemote,
                    eventDate = volunteerOpportunityEvent?.let {
                        com.example.volunteerlink.data.VolunteerScheduleText.date(
                            if (applicationIsRemote) it.eventRemoteStartDate else it.eventPhysicalStartDate)
                    } ?: volunteerApplication.applicationEventDate,
                    eventEndDate = volunteerOpportunityEvent?.let {
                        com.example.volunteerlink.data.VolunteerScheduleText.date(
                            if (applicationIsRemote) it.eventRemoteEndDate else it.eventPhysicalEndDate)
                    },
                    eventTime =
                        if (applicationIsRemote) "See submission deadline below" else
                            volunteerOpportunityEvent?.let {
                                "${com.example.volunteerlink.data.VolunteerScheduleText.time(it.eventPhysicalStartTime)} - " +
                                    "${com.example.volunteerlink.data.VolunteerScheduleText.time(it.eventPhysicalEndTime)}"
                            } ?: volunteerApplication.applicationEventTime,
                    eventLocation =
                        if (applicationIsRemote) {
                            "Online"
                        } else {
                            volunteerOpportunityEvent
                                ?.let { event ->
                                    event.eventFullAddress
                                        .ifBlank {
                                            event.eventLocation
                                        }
                                }
                                ?: volunteerApplication
                                    .applicationEventLocation
                        }
                )
            }

            if (volunteerOpportunityEvent != null && volunteerOpportunityRole != null) {
                item(key = "my_role_arrangements") {
                    VolunteerRoleInformationCard(volunteerOpportunityEvent, volunteerOpportunityRole, includeTasks = true)
                }

                if (phoneContactIsRelevant) {
                    item(key = "event_phone_contact") {
                        VolunteerApplicationEventPhoneContactCard(
                            organisationName = volunteerOpportunityEvent.eventOrganisationName,
                            phoneContactState = opportunityUiState.eventPhoneContact,
                            onEnabledChange = { enabled ->
                                volunteerOpportunityViewModel.setEventPhoneContactEnabled(
                                    postId = volunteerOpportunityEvent.eventDatabaseId,
                                    roleTemplateId = volunteerOpportunityRole.roleTemplateId,
                                    enabled = enabled
                                )
                            }
                        )
                    }
                }

                if (volunteerOpportunityRole.roleMode == "PHYSICAL" &&
                    volunteerApplication.applicationStatus in listOf(
                        VolunteerApplicationStatus.ACCEPTED, VolunteerApplicationStatus.COMPLETED,
                        VolunteerApplicationStatus.NOT_COMPLETED)) {
                    item(key = "my_attendance") {
                        VolunteerAttendanceCard(volunteerOpportunityEvent, volunteerOpportunityRole, volunteerApplication)
                    }
                }
            }

            if (volunteerApplication.applicationRoleMode == "REMOTE" ||
                volunteerOpportunityRole?.roleMode == "REMOTE" ||
                volunteerOpportunityEvent?.eventOpportunityType == "Remote") {
                item(key = "remote_submission") {
                    VolunteerRemoteSubmissionCard(
                        participationId = volunteerApplication.applicationDatabaseId,
                        dataVersion = opportunityUiState.dataVersion,
                        onRefreshApplications = { volunteerOpportunityViewModel.refresh() }
                    )
                }
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
                        !volunteerApplication.applicationDatabaseId.startsWith("offline|") &&
                        volunteerApplication.applicationStatus == VolunteerApplicationStatus.PENDING &&
                        volunteerOpportunityRole?.roleExtraApplicationQuestions?.isNotEmpty() == true
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Name the calculated questions value because later UI branches reuse it during this Compose pass.
                                val questions = volunteerOpportunityRole.roleExtraApplicationQuestions
                                formAnswers = questions.mapIndexed { index, _ ->
                                    volunteerApplication.applicationScreeningAnswers
                                        .getOrElse(index) { "" }
                                }
                                volunteerOpportunityViewModel.clearApplicationActionError()
                                shouldShowEditDialog = true
                            },
                            enabled = com.example.volunteerlink.data.VolunteerApplicationWindow.beforeStart(volunteerOpportunityEvent, volunteerOpportunityRole),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Edit Application", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (
                        !volunteerApplication.applicationDatabaseId.startsWith("offline|") &&
                        volunteerApplication.applicationStatus in setOf(
                            VolunteerApplicationStatus.PENDING,
                            VolunteerApplicationStatus.ACCEPTED
                        )
                    ) {
                        TextButton(
                            onClick = {
                                volunteerOpportunityViewModel.clearApplicationActionError()
                                selectedCancellationReason = ""
                                cancellationDetails = ""
                                shouldShowCancelDialog = true
                            },
                            enabled = !opportunityUiState.isApplicationActionRunning &&
                                com.example.volunteerlink.data.VolunteerApplicationWindow.beforeStart(volunteerOpportunityEvent, volunteerOpportunityRole),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cancel Application",
                                fontWeight = FontWeight.SemiBold,
                                color = VolunteerLinkError
                            )
                        }
                        if (!com.example.volunteerlink.data.VolunteerApplicationWindow.beforeStart(volunteerOpportunityEvent, volunteerOpportunityRole)) {
                            Text(
                                "Cancellation unavailable: this role has started or its dates need syncing.",
                                color = VolunteerLinkTextSecondary, fontSize = 12.sp
                            )
                        }
                    }

                    if (
                        volunteerApplication.applicationStatus ==
                        VolunteerApplicationStatus.CANCELLED
                    ) {
                        Button(
                            onClick = {
                                volunteerOpportunityViewModel.clearApplicationActionError()
                                volunteerOpportunityRole?.let { role ->
                                    onVolunteerRoleSelected(
                                        volunteerApplication.applicationEventId,
                                        role.roleId
                                    )
                                }
                            },
                            enabled = volunteerOpportunityRole != null &&
                                com.example.volunteerlink.data.VolunteerApplicationWindow.canApply(
                                    volunteerOpportunityEvent,
                                    volunteerOpportunityRole
                                ),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VolunteerLinkPrimaryGreen
                            )
                        ) { Text("Review and Apply Again", fontWeight = FontWeight.Bold) }

                        TextButton(
                            onClick = {
                                volunteerOpportunityViewModel.clearApplicationActionError()
                                shouldShowDeleteDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete application record", color = VolunteerLinkError)
                        }
                    }

                    if (
                        volunteerApplication.applicationStatus ==
                        VolunteerApplicationStatus.REJECTED
                    ) {
                        Text(
                            text =
                                "You cannot apply again for this same role. " +
                                    "You may choose another open role in this opportunity.",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }
            }
        }
    }

    if (shouldShowCancelDialog) {
        AlertDialog(
            titleContentColor = VolunteerLinkTextPrimary,
            textContentColor = VolunteerLinkTextSecondary,
            containerColor = Color.White,
            onDismissRequest = {
                shouldShowCancelDialog = false
            },
            title = {
                Text(
                    text = "Cancel application?"
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text =
                            if (volunteerApplication.applicationStatus == VolunteerApplicationStatus.ACCEPTED)
                                "The organisation has reserved this role for you. Cancelling will release your place and notify the organisation."
                            else "Select why you need to cancel this application."
                    )

                    Spacer(Modifier.height(10.dp))
                    cancellationReasons.forEach { reason ->
                        FilterChip(
                            selected = selectedCancellationReason == reason,
                            onClick = {
                                selectedCancellationReason = reason
                                volunteerOpportunityViewModel.clearApplicationActionError()
                            },
                            label = { Text(reason, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (selectedCancellationReason == "Other") {
                        OutlinedTextField(
                            value = cancellationDetails,
                            onValueChange = {
                                cancellationDetails = it
                                volunteerOpportunityViewModel.clearApplicationActionError()
                            },
                            label = { Text("Please explain") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

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
                                reason = selectedCancellationReason,
                                details = cancellationDetails.trim(),
                                onSuccess = {
                                    shouldShowCancelDialog = false
                                }
                            )
                    },
                    enabled =
                        !opportunityUiState.isApplicationActionRunning
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

    if (shouldShowEditDialog) {
        // Name the calculated questions value because later UI branches reuse it during this Compose pass.
        val questions = volunteerOpportunityRole?.roleExtraApplicationQuestions.orEmpty()
        AlertDialog(
            containerColor = Color.White,
            titleContentColor = VolunteerLinkTextPrimary,
            textContentColor = VolunteerLinkTextSecondary,
            onDismissRequest = {
                shouldShowEditDialog = false
            },
            title = { Text("Edit application") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (questions.isEmpty()) {
                        Text("This role has no additional screening questions.")
                    }
                    questions.forEachIndexed { index, question ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = question,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VolunteerLinkTextPrimary
                            )

                            OutlinedTextField(
                                value = formAnswers.getOrElse(index) { "" },
                                onValueChange = { answer ->
                                    formAnswers = formAnswers.toMutableList().also {
                                        while (it.size <= index) {
                                            it.add("")
                                        }
                                        it[index] = answer
                                    }
                                },
                                label = {
                                    Text("Your answer")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }
                    opportunityUiState.applicationActionError?.let {
                        Text(it, color = VolunteerLinkError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !opportunityUiState.isApplicationActionRunning &&
                            formAnswers.all { it.isNotBlank() },
                    onClick = {
                        volunteerOpportunityViewModel.updatePendingApplication(
                            volunteerApplicationId,
                            formAnswers
                        ) { shouldShowEditDialog = false }
                    }
                ) {
                    Text(if (opportunityUiState.isApplicationActionRunning) "Saving..." else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    shouldShowEditDialog = false
                }) { Text("Back") }
            }
        )
    }

    if (shouldShowDeleteDialog) {
        AlertDialog(
            containerColor = Color.White,
            titleContentColor = VolunteerLinkTextPrimary,
            textContentColor = VolunteerLinkTextSecondary,
            onDismissRequest = { shouldShowDeleteDialog = false },
            title = { Text("Delete application record?") },
            text = {
                Column {
                    Text("This permanently removes this cancelled application and its screening answers. This action cannot be undone.")
                    opportunityUiState.applicationActionError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = VolunteerLinkError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !opportunityUiState.isApplicationActionRunning,
                    onClick = {
                        volunteerOpportunityViewModel.deleteApplication(
                            volunteerApplicationId
                        ) {
                            shouldShowDeleteDialog = false
                            onBackSelected()
                        }
                    }
                ) { Text("Delete permanently", color = VolunteerLinkError) }
            },
            dismissButton = {
                TextButton(onClick = { shouldShowDeleteDialog = false }) {
                    Text("Keep record")
                }
            }
        )
    }
}

// Calculate whether the following UI or action is allowed before it is rendered or executed.
private val cancellationReasons = listOf(
    "Schedule conflict",
    "Personal or family emergency",
    "Health reasons",
    "Transportation issue",
    "Unable to meet the role commitment",
    "Location no longer suitable",
    "Applied by mistake",
    "Other"
)

@Composable
// Purpose: Handles volunteer applications top bar as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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
// Purpose: Renders the volunteer application list card from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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

                if (volunteerApplication.applicationDatabaseId.startsWith("offline|")) {
                    Text("Waiting to sync", color = Color(0xFF895B00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else VolunteerApplicationStatusBadge(
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
                        "${if (volunteerApplication.applicationDatabaseId.startsWith("offline|")) "Saved on device" else "Submitted"} ${volunteerApplication.applicationSubmittedDate}",
                    fontSize = 10.sp,
                    color = VolunteerLinkTextSecondary
                )

                Text(
                    text = "View details",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
            }
        }
    }
}

@Composable
// Purpose: Renders the volunteer application status card from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerApplicationStatusCard(
    volunteerApplication:
    VolunteerOpportunityApplication
) {
    // Translate the stored status into the label, colour or action rules shown to the volunteer.
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
                        if (volunteerApplication.applicationDatabaseId.startsWith("offline|")) "Waiting to sync" else volunteerApplicationStatusText(
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
// Purpose: Renders the volunteer application timeline card from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerApplicationTimelineCard(
    volunteerApplication:
    VolunteerOpportunityApplication
) {
    // Name the calculated steps value because later UI branches reuse it during this Compose pass.
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
// Purpose: Renders the volunteer application timeline row from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerApplicationTimelineRow(
    step: VolunteerApplicationTimelineStep,
    showConnector: Boolean
) {
    // Choose a visual colour that represents the current status without changing business data.
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

// Purpose: Checks whether an accepted role has remote work or a physical schedule on the app business date.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun volunteerApplicationIsRelevantToday(application: VolunteerOpportunityApplication, nowMillis: Long): Boolean {
    if (application.applicationStatus != VolunteerApplicationStatus.ACCEPTED) return false
    // Resolve or prepare event data from the shared dashboard snapshot for this part of the screen.
    val event = VolunteerOpportunitySessionStore.findEventById(application.applicationEventId) ?: return false
    // Resolve or prepare role data used for eligibility, schedule and application controls.
    val role = application.applicationRoleId?.let { VolunteerOpportunitySessionStore.findRoleById(event.eventId, it) } ?: return false
    return runCatching {
        // Name the calculated remote value because later UI branches reuse it during this Compose pass.
        val remote = role.roleMode == "REMOTE"
        // Name the calculated today value because later UI branches reuse it during this Compose pass.
        val today = com.example.volunteerlink.data.VolunteerAttendanceWindow.localDate(
            nowMillis, if (remote) "UTC" else event.eventTimeZone)
        // Name the calculated start value because later UI branches reuse it during this Compose pass.
        val start = if (remote) event.eventRemoteStartDate else event.eventPhysicalStartDate
        // Name the calculated end value because later UI branches reuse it during this Compose pass.
        val end = if (remote) event.eventRemoteEndDate else event.eventPhysicalEndDate
        // Name the calculated assigned dates value because later UI branches reuse it during this Compose pass.
        val assignedDates = role.roleScheduleItems.filter { it.assignedToRole && it.scheduleType == "PHYSICAL" }.map { it.rawDate }
        start.isNotBlank() && end.isNotBlank() && today >= start && today <= end &&
            (remote || assignedDates.isEmpty() || today in assignedDates)
    }.getOrDefault(false)
}

// Purpose: Keeps the Physical Today filter limited to fixed on-site role schedules.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun volunteerApplicationIsPhysicalToday(application: VolunteerOpportunityApplication, nowMillis: Long): Boolean {
    if (!volunteerApplicationIsRelevantToday(application, nowMillis)) return false
    // Resolve or prepare event data from the shared dashboard snapshot for this part of the screen.
    val event = VolunteerOpportunitySessionStore.findEventById(application.applicationEventId) ?: return false
    // Resolve or prepare role data used for eligibility, schedule and application controls.
    val role = application.applicationRoleId?.let { VolunteerOpportunitySessionStore.findRoleById(event.eventId, it) } ?: return false
    return role.roleMode.equals("PHYSICAL", ignoreCase = true)
}

// Purpose: Builds the ordered submitted, accepted, service and verification steps shown in Application Details.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun volunteerApplicationTimelineSteps(
    application: VolunteerOpportunityApplication
): List<VolunteerApplicationTimelineStep> {
    if (application.applicationDatabaseId.startsWith("offline|")) return listOf(
        VolunteerApplicationTimelineStep("Waiting to sync", "Saved on this device only. Not sent for review; no place is reserved.", "CURRENT"),
        VolunteerApplicationTimelineStep("Server confirmation", "Connect and Sync. Instant Join becomes accepted only after the server confirms a place. Review applications still need organisation approval.", "PENDING")
    )
    // Name the calculated submitted value because later UI branches reuse it during this Compose pass.
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

        VolunteerApplicationStatus.NOT_COMPLETED ->
            listOf(
                submitted,
                VolunteerApplicationTimelineStep(
                    title = "Organisation review completed",
                    supportingText = "The organisation reviewed attendance and contribution evidence.",
                    state = "COMPLETE"
                ),
                VolunteerApplicationTimelineStep(
                    title = "Role not completed",
                    supportingText =
                        "The organisation could not verify this role as completed.",
                    state = "ERROR"
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
// Purpose: Renders the volunteer application information card from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerApplicationInformationCard(
    volunteerApplication:
    VolunteerOpportunityApplication,
    applicationIsRemote: Boolean,
    eventDate: String?,
    eventEndDate: String?,
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

            if (eventDate != null || eventTime != null) {
                // Name the calculated schedule text value because later UI branches reuse it during this Compose pass.
                val scheduleText =
                    if (applicationIsRemote) {
                        // Name the calculated end date value because later UI branches reuse it during this Compose pass.
                        val endDate = eventEndDate
                            ?.takeUnless { it == eventDate }
                        listOfNotNull(eventDate, endDate)
                            .joinToString(" - ")
                    } else {
                        listOfNotNull(eventDate, eventEndDate?.takeUnless { it == eventDate })
                            .joinToString(" - ") + (eventTime?.let { "\nCheck-in hours: $it" } ?: "")
                    }
                VolunteerApplicationInformationRow(
                    label = "Work period",
                    value = scheduleText
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
                ?.takeIf {
                    volunteerApplication.applicationStatus in setOf(
                        VolunteerApplicationStatus.REJECTED,
                        VolunteerApplicationStatus.CANCELLED
                    )
                }
                ?.takeIf(String::isNotBlank)
                ?.let { rejectionReason ->
                    VolunteerApplicationInformationRow(
                        label = "Reason",
                        value = rejectionReason
                    )
                }

            volunteerApplication.applicationCompletionReason
                ?.takeIf(String::isNotBlank)
                ?.let { reason ->
                    VolunteerApplicationInformationRow(
                        label = "Completion reason",
                        value = reason
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

// Purpose: Converts verified minutes into a readable hours-and-minutes label.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun formatVerifiedServiceTime(minutes: Int): String {
    // Name the calculated hours value because later UI branches reuse it during this Compose pass.
    val hours = minutes / 60
    // Name the calculated remaining minutes value because later UI branches reuse it during this Compose pass.
    val remainingMinutes = minutes % 60
    return when {
        hours == 0 -> "$remainingMinutes minutes"
        remainingMinutes == 0 -> "$hours hours"
        else -> "$hours hours $remainingMinutes minutes"
    }
}

@Composable
// Purpose: Renders the volunteer application information row from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerApplicationInformationRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = VolunteerLinkTextSecondary
        )

        Text(
            text = value,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Normal,
            color = VolunteerLinkTextPrimary
        )
    }
}

@Composable
// Purpose: Renders the volunteer application status badge from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerApplicationStatusBadge(
    applicationStatus: VolunteerApplicationStatus
) {
    // Translate the stored status into the label, colour or action rules shown to the volunteer.
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

// Purpose: Handles volunteer application status text as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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

        VolunteerApplicationStatus.NOT_COMPLETED ->
            "Not Completed"

        VolunteerApplicationStatus.CANCELLED ->
            "Cancelled"
    }
}

// Purpose: Handles volunteer application status colour as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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

        VolunteerApplicationStatus.NOT_COMPLETED ->
            VolunteerLinkError

        VolunteerApplicationStatus.CANCELLED ->
            VolunteerLinkTextSecondary
    }
}

@Composable
// Purpose: Renders the volunteer application details not found screen and connects user actions to navigation or its ViewModel.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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

@Composable
// Purpose: Renders the volunteer application event phone contact card from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerApplicationEventPhoneContactCard(
    organisationName: String,
    phoneContactState: VolunteerEventPhoneContactUiState,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = VolunteerLinkSurface),
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Opportunity phone contact",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = "Allow $organisationName to call the phone number on your profile while this accepted participation is active.",
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                Switch(
                    checked = phoneContactState.enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = phoneContactState.eligible &&
                        !phoneContactState.isLoading &&
                        !phoneContactState.isUpdating
                )
            }

            // Translate the stored status into the label, colour or action rules shown to the volunteer.
            val statusText = when {
                phoneContactState.isLoading -> "Checking contact permission..."
                phoneContactState.errorMessage != null -> phoneContactState.errorMessage
                !phoneContactState.eligible -> phoneContactState.reason
                    ?: "Phone sharing is not available for this opportunity."
                phoneContactState.enabled -> buildString {
                    append("Your organiser can call you")
                    phoneContactState.availableUntilLabel
                        ?.takeIf { it.isNotBlank() }
                        ?.let { append(" until $it") }
                    append(". You can switch this off at any time.")
                }
                else -> buildString {
                    append("Your phone number stays private unless you turn this on")
                    phoneContactState.availableUntilLabel
                        ?.takeIf { it.isNotBlank() }
                        ?.let { append(". If enabled, access lasts until $it") }
                    append(".")
                }
            }

            Text(
                text = statusText.orEmpty(),
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = if (phoneContactState.enabled) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    phoneContactState.errorMessage != null -> VolunteerLinkError
                    phoneContactState.enabled -> VolunteerLinkPrimaryGreen
                    else -> VolunteerLinkTextSecondary
                }
            )
        }
    }
}
