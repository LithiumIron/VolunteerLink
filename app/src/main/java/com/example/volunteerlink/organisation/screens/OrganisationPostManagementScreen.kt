package com.example.volunteerlink.organisation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementRole
import com.example.volunteerlink.organisation.viewmodel.OrganisationPostManagementViewModel
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import java.util.Locale

/**
 * Mobile post-management detail opened from Manage > Volunteer Posts.
 *
 * V1 deliberately keeps one stable screen for Physical, Remote and Hybrid
 * posts. Hybrid simply shows both timelines in the summary instead of creating
 * separate Physical/Remote management dashboards.
 */
@Composable
fun OrganisationPostManagementScreen(
    postId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onExitProtectionChanged: (Boolean, (() -> Unit)?) -> Unit = { _, _ -> },
    viewModel: OrganisationPostManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.load(postId)
    }

    val post = uiState.post
    val reviewSession = uiState.physicalReviewSession
    var confirmLeaveReview by rememberSaveable { mutableStateOf(false) }

    fun discardAndLeave() {
        viewModel.discardPhysicalReviewSession()
        onBack()
    }

    BackHandler(enabled = reviewSession.hasUnfinishedReview) {
        confirmLeaveReview = true
    }

    DisposableEffect(reviewSession.hasUnfinishedReview) {
        onExitProtectionChanged(
            reviewSession.hasUnfinishedReview,
            if (reviewSession.hasUnfinishedReview) viewModel::discardPhysicalReviewSession else null
        )
        onDispose { onExitProtectionChanged(false, null) }
    }

    when {
        uiState.isLoading -> ManageLoadingState()
        uiState.errorMessage != null -> ManageErrorState(
            message = uiState.errorMessage,
            onRetry = viewModel::refresh
        )
        post != null -> OrganisationPostManagementContent(
            post = post,
            isStartingAttendance = uiState.isStartingAttendance,
            isUpdatingAttendance = uiState.isUpdatingAttendance,
            attendanceActionMessage = uiState.attendanceActionMessage,
            isUpdatingReview = uiState.isUpdatingReview,
            reviewActionMessage = uiState.reviewActionMessage,
            physicalReviewSession = reviewSession,
            onBack = {
                if (reviewSession.hasUnfinishedReview) confirmLeaveReview = true else onBack()
            },
            onEdit = onEdit,
            onToggleShortlist = viewModel::toggleApplicantShortlist,
            onStartAttendance = viewModel::startPhysicalAttendance,
            onMarkPresent = viewModel::markVolunteerPresent,
            onMarkAbsent = viewModel::markVolunteerAbsent,
            onCompleteAllReady = viewModel::completeAllReadyPhysical,
            onReportReviewIssue = viewModel::reportPhysicalReviewIssue,
            onFinalizeVolunteer = viewModel::finalizePhysicalVolunteer,
            onChangeDecision = viewModel::changePhysicalReviewDecision,
            onSaveFeedback = viewModel::savePhysicalFeedback,
            onStageChange = viewModel::setPhysicalReviewStage,
            onReviewDraftDirtyChanged = viewModel::setPhysicalReviewDraftDirty,
            onFinalizeReview = viewModel::finalizePhysicalReviewPost,
            onStartAttendancePolling = viewModel::startAttendancePolling,
            onStopAttendancePolling = viewModel::stopAttendancePolling
        )
    }

    if (uiState.isUpdatingReview) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "Finalizing event...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = VolunteerLinkPrimaryGreen,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Saving completion decisions, verified hours and feedback. Please wait.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            },
            confirmButton = {},
            containerColor = VolunteerLinkSurface
        )
    }

    if (uiState.reviewFinalizeSucceeded) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPhysicalReviewFinalizeSuccess,
            title = {
                Text(
                    text = "Event review completed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
            },
            text = {
                Text(
                    text = "The review has been finalized successfully. This post has moved from Needs Review to Completed. Saved attendance, completion decisions and feedback are now read-only.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::dismissPhysicalReviewFinalizeSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = VolunteerLinkSurface
        )
    }

    if (confirmLeaveReview) {
        AlertDialog(
            onDismissRequest = { confirmLeaveReview = false },
            title = {
                Text(
                    text = "Leave event review?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
            },
            text = {
                Text(
                    text = "Your completion or feedback changes have not been finalized. Leaving now will discard those temporary review changes. Saved attendance updates will stay.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmLeaveReview = false
                        discardAndLeave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                ) { Text("Discard Changes & Leave", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeaveReview = false }) {
                    Text("Keep Reviewing")
                }
            },
            containerColor = VolunteerLinkSurface
        )
    }
}

@Composable
private fun OrganisationPostManagementContent(
    post: PostManagementPost,
    isStartingAttendance: Boolean,
    isUpdatingAttendance: Boolean,
    attendanceActionMessage: String?,
    isUpdatingReview: Boolean,
    reviewActionMessage: String?,
    physicalReviewSession: com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReviewSession,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleShortlist: (PostManagementPerson) -> Unit,
    onStartAttendance: () -> Unit,
    onMarkPresent: (PostManagementPerson, String) -> Unit,
    onMarkAbsent: (PostManagementPerson, String) -> Unit,
    onCompleteAllReady: () -> Unit,
    onReportReviewIssue: (PostManagementPerson, String) -> Unit,
    onFinalizeVolunteer: (PostManagementPerson, Boolean, String?) -> Unit,
    onChangeDecision: (PostManagementPerson) -> Unit,
    onSaveFeedback: (List<String>, String, String?) -> Unit,
    onStageChange: (com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReviewStage) -> Unit,
    onReviewDraftDirtyChanged: (Boolean) -> Unit,
    onFinalizeReview: () -> Unit,
    onStartAttendancePolling: () -> Unit,
    onStopAttendancePolling: () -> Unit
) {
    var selectedTabName by rememberSaveable {
        mutableStateOf(PostManagementTab.OVERVIEW.name)
    }
    var selectedPeopleTabName by rememberSaveable {
        mutableStateOf(PostManagementPeopleTab.APPLICANTS.name)
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedRoleId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPerson by remember { mutableStateOf<PostManagementPerson?>(null) }
    var selectedAttendanceDate by rememberSaveable { mutableStateOf<String?>(null) }
    var absentConfirmationPerson by remember { mutableStateOf<PostManagementPerson?>(null) }
    var absentConfirmationDate by remember { mutableStateOf<String?>(null) }

    val selectedTab = runCatching {
        PostManagementTab.valueOf(selectedTabName)
    }.getOrDefault(PostManagementTab.OVERVIEW)

    val selectedPeopleTab = runCatching {
        PostManagementPeopleTab.valueOf(selectedPeopleTabName)
    }.getOrDefault(PostManagementPeopleTab.APPLICANTS)

    val physicalAttendance = post.physicalAttendance
    val physicalReview = post.physicalReview
    val showPhysicalReview =
        post.mode.equals("PHYSICAL", ignoreCase = true) &&
            physicalReview != null &&
            (
                post.physicalTimingState == PostTimingState.PAST ||
                    post.databaseStatus.equals("COMPLETED", ignoreCase = true)
            )

    LaunchedEffect(showPhysicalReview) {
        if (showPhysicalReview && selectedTabName == PostManagementTab.PEOPLE.name) {
            selectedTabName = PostManagementTab.REVIEW.name
        } else if (!showPhysicalReview && selectedTabName == PostManagementTab.REVIEW.name) {
            selectedTabName = PostManagementTab.OVERVIEW.name
        }
    }

    val shouldPollAttendance =
        selectedTab == PostManagementTab.PEOPLE &&
            selectedPeopleTab == PostManagementPeopleTab.VOLUNTEERS &&
            physicalAttendance != null

    DisposableEffect(shouldPollAttendance) {
        if (shouldPollAttendance) {
            onStartAttendancePolling()
        } else {
            onStopAttendancePolling()
        }

        onDispose {
            if (shouldPollAttendance) {
                onStopAttendancePolling()
            }
        }
    }

    // When AppClock moves to a new event day, follow that new day automatically.
    // Without this, a saved Sep 3/Sep 4 selection can remain on screen while
    // the top attendance card is already using Sep 4/Sep 5, which makes the
    // PIN and counters look like they belong to the wrong date.
    LaunchedEffect(physicalAttendance?.todayDate) {
        selectedAttendanceDate = physicalAttendance?.defaultSelectedDate
    }

    // Also recover safely if the set of selectable dates changes and the
    // currently selected date is no longer valid.
    LaunchedEffect(physicalAttendance?.availableDates) {
        val dates = physicalAttendance?.availableDates.orEmpty()
        if (selectedAttendanceDate !in dates) {
            selectedAttendanceDate = physicalAttendance?.defaultSelectedDate
        }
    }

    val openApplicationRoleIds = post.roles
        .filter { it.isApplicationOpen }
        .map { it.roleTemplateId }
        .toSet()
    val showApplicantsTab = openApplicationRoleIds.isNotEmpty()
    val openApplicants = post.applicants.filter { applicant ->
        applicant.roleTemplateId in openApplicationRoleIds
    }

    LaunchedEffect(showApplicantsTab) {
        if (
            !showApplicantsTab &&
            selectedPeopleTabName != PostManagementPeopleTab.VOLUNTEERS.name
        ) {
            selectedPeopleTabName = PostManagementPeopleTab.VOLUNTEERS.name
            selectedRoleId = null
        }
    }

    val peopleForTab = when (selectedPeopleTab) {
        PostManagementPeopleTab.APPLICANTS -> openApplicants
        PostManagementPeopleTab.VOLUNTEERS -> post.volunteers
    }

    // Only offer role filters that actually contain people in the selected tab.
    // This keeps Instant Join roles with no pending applications out of Applicants.
    val rolesForSelectedPeopleTab = post.roles.filter { role ->
        peopleForTab.any { person ->
            person.roleTemplateId == role.roleTemplateId
        }
    }

    val normalizedQuery = searchQuery.trim().lowercase(Locale.US)
    val visiblePeople = peopleForTab.filter { person ->
        val matchesRole = selectedRoleId == null ||
                person.roleTemplateId == selectedRoleId
        val matchesQuery = normalizedQuery.isBlank() ||
                person.fullName.lowercase(Locale.US).contains(normalizedQuery) ||
                person.roleName.lowercase(Locale.US).contains(normalizedQuery) ||
                person.city.orEmpty().lowercase(Locale.US).contains(normalizedQuery)
        matchesRole && matchesQuery
    }

    // Keep the role filter, but still organise the visible people by role.
    // This remains readable when one post has several roles and many people.
    val visibleRoleGroups: List<Pair<PostManagementRole, List<PostManagementPerson>>> =
        post.roles.mapNotNull { role ->
            val peopleInRole = visiblePeople.filter {
                it.roleTemplateId == role.roleTemplateId
            }

            if (peopleInRole.isEmpty()) {
                null
            } else {
                val sortedPeople = if (
                    selectedPeopleTab == PostManagementPeopleTab.APPLICANTS
                ) {
                    peopleInRole.sortedWith(
                        compareByDescending<PostManagementPerson> { it.isShortlisted }
                            .thenBy { it.appliedAt.orEmpty() }
                            .thenBy { it.fullName }
                    )
                } else {
                    peopleInRole.sortedBy { it.fullName }
                }

                role to sortedPeople
            }
        }

    val showEdit = when (post.databaseStatus.uppercase(Locale.US)) {
        "COMPLETED", "CANCELLED" -> false
        else -> post.timingState != PostTimingState.PAST
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
    ) {
        PostManagementTopBar(
            onBack = onBack,
            onEdit = onEdit,
            showEdit = showEdit
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VolunteerLinkScreenHorizontalPadding,
                end = VolunteerLinkScreenHorizontalPadding,
                top = 14.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "post_summary") {
                PostManagementSummaryCard(post)
            }

            item(key = "post_tabs") {
                PostManagementMainTabs(
                    selected = selectedTab,
                    pendingApplicantCount = openApplicants.size,
                    showReviewTab = showPhysicalReview,
                    onSelected = { selectedTabName = it.name }
                )
            }

            when (selectedTab) {
                PostManagementTab.OVERVIEW -> {
                    item(key = "overview") {
                        PostManagementOverview(post)
                    }
                }

                PostManagementTab.PEOPLE -> {
                    if (
                        selectedPeopleTab == PostManagementPeopleTab.VOLUNTEERS &&
                        post.physicalTimingState == PostTimingState.ONGOING
                    ) {
                        physicalAttendance?.let { attendance ->
                            val attendanceDate = selectedAttendanceDate
                                ?: attendance.defaultSelectedDate
                                ?: attendance.todayDate
                            val selectedSession = post.attendanceDays.firstOrNull { day ->
                                day.eventDate == attendanceDate
                            }
                            val selectedEligibleCount = attendance.volunteerSummaries
                                .filter { summary ->
                                    summary.statusFor(attendanceDate)?.expected == true
                                }
                                .map { it.userId }
                                .distinct()
                                .size
                            val selectedPresentCount = attendance.volunteerSummaries
                                .filter { summary ->
                                    summary.statusFor(attendanceDate)?.present == true
                                }
                                .map { it.userId }
                                .distinct()
                                .size

                            item(key = "physical_attendance_${attendanceDate}") {
                                PostManagementTodayAttendanceCard(
                                    attendance = attendance,
                                    selectedDate = attendanceDate,
                                    selectedSession = selectedSession,
                                    selectedEligibleVolunteerCount = selectedEligibleCount,
                                    selectedPresentCount = selectedPresentCount,
                                    isStartingAttendance = isStartingAttendance,
                                    actionMessage = null,
                                    onStartAttendance = onStartAttendance
                                )
                            }
                        }
                    }

                    if (
                        selectedPeopleTab == PostManagementPeopleTab.VOLUNTEERS &&
                        physicalAttendance != null &&
                        physicalAttendance.availableDates.isNotEmpty()
                    ) {
                        item(key = "physical_attendance_day_selector") {
                            PostManagementAttendanceDaySelector(
                                dates = physicalAttendance.availableDates,
                                selectedDate = selectedAttendanceDate,
                                actionMessage = attendanceActionMessage,
                                onSelected = { selectedAttendanceDate = it }
                            )
                        }
                    }

                    item(key = "people_controls") {
                        PostManagementPeopleControls(
                            selectedTab = selectedPeopleTab,
                            showApplicantsTab = showApplicantsTab,
                            applicantCount = openApplicants.size,
                            volunteerCount = post.volunteers.size,
                            query = searchQuery,
                            selectedRoleId = selectedRoleId,
                            roles = rolesForSelectedPeopleTab,
                            onTabSelected = {
                                selectedPeopleTabName = it.name
                                selectedRoleId = null
                            },
                            onQueryChange = { searchQuery = it },
                            onRoleSelected = { selectedRoleId = it }
                        )
                    }

                    if (visiblePeople.isEmpty()) {
                        item(key = "people_empty") {
                            PostManagementPeopleEmptyState(
                                selectedTab = selectedPeopleTab,
                                hasFilters = searchQuery.isNotBlank() || selectedRoleId != null
                            )
                        }
                    } else {
                        visibleRoleGroups.forEach { (role, peopleInRole) ->
                            item(
                                key = "people_role_${role.roleTemplateId}_${selectedPeopleTab.name}"
                            ) {
                                PostManagementPeopleRoleHeader(
                                    role = role,
                                    selectedTab = selectedPeopleTab,
                                    applicantCount = openApplicants.count {
                                        it.roleTemplateId == role.roleTemplateId
                                    },
                                    volunteerCount = post.volunteers.count {
                                        it.roleTemplateId == role.roleTemplateId
                                    }
                                )
                            }

                            items(
                                count = peopleInRole.size,
                                key = { index ->
                                    val person = peopleInRole[index]
                                    "${person.userId}_${person.roleTemplateId}_${selectedPeopleTab.name}"
                                }
                            ) { index ->
                                val person = peopleInRole[index]
                                val showPhysicalAttendance =
                                    selectedPeopleTab == PostManagementPeopleTab.VOLUNTEERS &&
                                        person.roleMode.equals("PHYSICAL", ignoreCase = true) &&
                                        selectedAttendanceDate != null &&
                                        physicalAttendance != null

                                PostManagementPersonCard(
                                    person = person,
                                    isApplicant = selectedPeopleTab == PostManagementPeopleTab.APPLICANTS,
                                    isApplicationOpen = role.isApplicationOpen,
                                    attendanceSummary = if (showPhysicalAttendance) {
                                        physicalAttendance?.summaryFor(person)
                                    } else {
                                        null
                                    },
                                    attendanceSelectedDate = if (showPhysicalAttendance) {
                                        selectedAttendanceDate
                                    } else {
                                        null
                                    },
                                    attendanceTodayDate = physicalAttendance?.todayDate,
                                    canCorrectAttendance = physicalAttendance?.canCorrectAttendance == true,
                                    isUpdatingAttendance = isUpdatingAttendance,
                                    onMarkPresent = onMarkPresent,
                                    onRequestMarkAbsent = { selected, date ->
                                        absentConfirmationPerson = selected
                                        absentConfirmationDate = date
                                    },
                                    onViewProfile = { selectedPerson = it },
                                    onToggleShortlist = onToggleShortlist
                                )
                            }
                        }
                    }
                }

                PostManagementTab.REVIEW -> {
                    val review = physicalReview
                    if (showPhysicalReview && review != null) {
                        item(key = "physical_completion_review") {
                            PostManagementPhysicalReviewContent(
                                review = review,
                                session = physicalReviewSession,
                                isUpdatingReview = isUpdatingReview,
                                isUpdatingAttendance = isUpdatingAttendance,
                                reviewActionMessage = reviewActionMessage,
                                attendanceActionMessage = attendanceActionMessage,
                                onCompleteAllReady = onCompleteAllReady,
                                onReportIssue = onReportReviewIssue,
                                onSelectVolunteerDecision = onFinalizeVolunteer,
                                onChangeDecision = onChangeDecision,
                                onSaveFeedback = onSaveFeedback,
                                onStageChange = onStageChange,
                                onReviewDraftDirtyChanged = onReviewDraftDirtyChanged,
                                onFinalizeReview = onFinalizeReview,
                                onMarkPresent = onMarkPresent,
                                onRequestMarkAbsent = { selected, date ->
                                    absentConfirmationPerson = selected
                                    absentConfirmationDate = date
                                },
                                onViewProfile = { selectedPerson = it }
                            )
                        }
                    } else {
                        item(key = "physical_review_unavailable") {
                            PostManagementPeopleEmptyState(
                                selectedTab = PostManagementPeopleTab.VOLUNTEERS,
                                hasFilters = false
                            )
                        }
                    }
                }
            }
        }
    }

    selectedPerson?.let { person ->
        PostManagementProfilePreviewDialog(
            person = person,
            onDismiss = { selectedPerson = null }
        )
    }

    val absentPerson = absentConfirmationPerson
    val absentDate = absentConfirmationDate
    if (absentPerson != null && absentDate != null) {
        PostManagementMarkAbsentDialog(
            person = absentPerson,
            eventDate = absentDate,
            onDismiss = {
                absentConfirmationPerson = null
                absentConfirmationDate = null
            },
            onConfirm = {
                absentConfirmationPerson = null
                absentConfirmationDate = null
                onMarkAbsent(absentPerson, absentDate)
            }
        )
    }
}
