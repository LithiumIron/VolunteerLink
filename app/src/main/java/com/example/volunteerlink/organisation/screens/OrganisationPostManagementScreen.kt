package com.example.volunteerlink.organisation.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementRole
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewItem
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewSession
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewStage
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmission
import com.example.volunteerlink.organisation.viewmodel.OrganisationPostManagementViewModel
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import kotlinx.coroutines.launch
import java.io.File
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
    val remoteReviewSession = uiState.remoteReviewSession
    val hasUnfinishedReview = reviewSession.hasUnfinishedReview || remoteReviewSession.hasUnfinishedReview
    var confirmLeaveReview by rememberSaveable { mutableStateOf(false) }

    fun discardAndLeave() {
        viewModel.discardReviewSessions()
        onBack()
    }

    BackHandler(enabled = hasUnfinishedReview) {
        confirmLeaveReview = true
    }

    DisposableEffect(hasUnfinishedReview) {
        onExitProtectionChanged(
            hasUnfinishedReview,
            if (hasUnfinishedReview) viewModel::discardReviewSessions else null
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
            isUpdatingRemoteReview = uiState.isUpdatingRemoteReview,
            remoteReviewActionMessage = uiState.remoteReviewActionMessage,
            remoteReviewSession = remoteReviewSession,
            onBack = {
                if (hasUnfinishedReview) confirmLeaveReview = true else onBack()
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
            onDownloadRemoteSubmission = viewModel::downloadRemoteSubmission,
            onReviewRemoteSubmission = viewModel::reviewRemoteSubmission,
            onSetRemoteSubmissionDecision = viewModel::setRemoteSubmissionDecision,
            onSetRemoteMissingAction = viewModel::setRemoteMissingAction,
            onSetRemoteReviewNewEndDate = viewModel::setRemoteReviewNewEndDate,
            onSaveRemoteSubmissionStage = viewModel::saveRemoteSubmissionReviewStage,
            onSetRemoteCompletionDecision = viewModel::setRemoteCompletionDecision,
            onChangeRemoteCompletionDecision = viewModel::changeRemoteCompletionDecision,
            onSetRemoteFeedback = viewModel::setRemoteFeedback,
            onRemoteReviewStageChange = viewModel::setRemoteReviewStage,
            onFinalizeRemoteReview = viewModel::finalizeRemoteReviewPost,
            onStartAttendancePolling = viewModel::startAttendancePolling,
            onStopAttendancePolling = viewModel::stopAttendancePolling
        )
    }

    if (uiState.isUpdatingReview || uiState.isUpdatingRemoteReview) {
        val isRemoteOperation = uiState.isUpdatingRemoteReview
        val isRemoteFinalize = isRemoteOperation &&
            uiState.remoteReviewSession.stage == PostManagementRemoteReviewStage.FINISH
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = when {
                        isRemoteFinalize -> "Finalizing Remote project..."
                        isRemoteOperation -> "Saving Remote review..."
                        else -> "Finalizing event..."
                    },
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
                        text = when {
                            isRemoteFinalize ->
                                "Saving Remote completion decisions and final feedback. Please wait."
                            isRemoteOperation ->
                                "Saving submission decisions and the project-wide deadline. Please wait."
                            else ->
                                "Saving completion decisions, verified hours and feedback. Please wait."
                        },
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


    if (uiState.remoteReviewFinalizeSucceeded) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRemoteReviewFinalizeSuccess,
            title = {
                Text(
                    text = "Remote review completed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
            },
            text = {
                Text(
                    text = "The Remote project review has been finalized successfully. The post is now Completed and the saved outcomes are read-only.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::dismissRemoteReviewFinalizeSuccess,
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
                    text = "Leave review?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
            },
            text = {
                Text(
                    text = "Your temporary review choices have not been finalized. Leaving now will discard those draft choices. Database changes that were already saved, such as attendance or a committed Remote deadline extension, will stay.",
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
    isUpdatingRemoteReview: Boolean,
    remoteReviewActionMessage: String?,
    remoteReviewSession: PostManagementRemoteReviewSession,
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
    onDownloadRemoteSubmission: suspend (PostManagementRemoteSubmission) -> ByteArray,
    onReviewRemoteSubmission: suspend (PostManagementRemoteSubmission, String, String?) -> Unit,
    onSetRemoteSubmissionDecision: (PostManagementRemoteSubmission, String, String?) -> Unit,
    onSetRemoteMissingAction: (String, Boolean) -> Unit,
    onSetRemoteReviewNewEndDate: (String?) -> Unit,
    onSaveRemoteSubmissionStage: () -> Unit,
    onSetRemoteCompletionDecision: (PostManagementPerson, Boolean, String?) -> Unit,
    onChangeRemoteCompletionDecision: (PostManagementPerson) -> Unit,
    onSetRemoteFeedback: (PostManagementPerson, String) -> Unit,
    onRemoteReviewStageChange: (PostManagementRemoteReviewStage) -> Unit,
    onFinalizeRemoteReview: () -> Unit,
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
    var selectedRemoteSubmission by remember { mutableStateOf<PostManagementRemoteSubmission?>(null) }
    var selectedRemoteSubmissionPerson by remember { mutableStateOf<PostManagementPerson?>(null) }
    var isOpeningRemoteSubmission by remember { mutableStateOf(false) }
    var isDownloadingRemoteSubmission by remember { mutableStateOf(false) }
    var pendingRemoteDownloadSubmission by remember {
        mutableStateOf<PostManagementRemoteSubmission?>(null)
    }
    var remoteSubmissionFileError by remember { mutableStateOf<String?>(null) }
    var remoteSubmissionDownloadMessage by remember { mutableStateOf<String?>(null) }
    var revisionSubmission by remember { mutableStateOf<PostManagementRemoteSubmission?>(null) }
    var revisionFeedback by remember { mutableStateOf("") }
    var acceptSubmission by remember { mutableStateOf<PostManagementRemoteSubmission?>(null) }
    var notAcceptSubmission by remember { mutableStateOf<PostManagementRemoteSubmission?>(null) }
    var isReviewingRemoteSubmission by remember { mutableStateOf(false) }
    var remoteSubmissionReviewError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val remoteSubmissionDownloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val submission = pendingRemoteDownloadSubmission
        val destination = result.data?.data

        if (
            result.resultCode == Activity.RESULT_OK &&
            destination != null &&
            submission != null
        ) {
            isDownloadingRemoteSubmission = true
            remoteSubmissionFileError = null
            remoteSubmissionDownloadMessage = null

            coroutineScope.launch {
                try {
                    val bytes = onDownloadRemoteSubmission(submission)
                    saveRemoteSubmissionFile(
                        context = context,
                        destination = destination,
                        bytes = bytes
                    )
                    remoteSubmissionDownloadMessage = "File downloaded successfully."
                } catch (exception: Exception) {
                    remoteSubmissionFileError = exception.message
                        ?: "Unable to download this submission."
                } finally {
                    isDownloadingRemoteSubmission = false
                    pendingRemoteDownloadSubmission = null
                }
            }
        } else {
            pendingRemoteDownloadSubmission = null
        }
    }

    val selectedTab = runCatching {
        PostManagementTab.valueOf(selectedTabName)
    }.getOrDefault(PostManagementTab.OVERVIEW)

    val selectedPeopleTab = runCatching {
        PostManagementPeopleTab.valueOf(selectedPeopleTabName)
    }.getOrDefault(PostManagementPeopleTab.APPLICANTS)

    val physicalAttendance = post.physicalAttendance
    val physicalReview = post.physicalReview
    val remoteReview = post.remoteReview
    val showPhysicalReview =
        post.mode.equals("PHYSICAL", ignoreCase = true) &&
            physicalReview != null &&
            (
                post.physicalTimingState == PostTimingState.PAST ||
                    post.databaseStatus.equals("COMPLETED", ignoreCase = true)
            )
    val showRemoteReview =
        post.mode.equals("REMOTE", ignoreCase = true) &&
            remoteReview != null &&
            (
                post.remoteTimingState == PostTimingState.PAST ||
                    post.databaseStatus.equals("COMPLETED", ignoreCase = true)
            )
    val showReview = showPhysicalReview || showRemoteReview

    LaunchedEffect(showReview) {
        if (showReview && selectedTabName == PostManagementTab.PEOPLE.name) {
            selectedTabName = PostManagementTab.REVIEW.name
        } else if (!showReview && selectedTabName == PostManagementTab.REVIEW.name) {
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
                    showReviewTab = showReview,
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

                    if (
                        selectedPeopleTab == PostManagementPeopleTab.VOLUNTEERS &&
                        post.remoteTimingState == PostTimingState.ONGOING &&
                        post.remote?.submissionMode.equals("SHARED_TEAM", ignoreCase = true)
                    ) {
                        val remote = post.remote
                        if (remote != null) {
                            val sharedSubmission = post.remoteSubmissions
                                .filter { submission ->
                                    submission.submissionType.equals("SHARED", ignoreCase = true)
                                }
                                .maxWithOrNull(
                                    compareBy<PostManagementRemoteSubmission> {
                                        it.submittedAt.orEmpty()
                                    }.thenBy { it.submissionId }
                                )
                            val sharedSubmissionIsResubmission = sharedSubmission?.let { submission ->
                                post.isRemoteResubmission(submission)
                            } == true
                            val responsibleRoleName = post.roles.firstOrNull { role ->
                                role.roleTemplateId == remote.responsibleRoleTemplateId
                            }?.roleName
                            val submittedByName = sharedSubmission?.userId?.let { submittedByUserId ->
                                post.volunteers.firstOrNull { volunteer ->
                                    volunteer.userId == submittedByUserId
                                }?.fullName
                            }

                            item(key = "remote_shared_submission") {
                                PostManagementRemoteTeamSubmissionCard(
                                    deliverable = remote.sharedDeliverable,
                                    responsibleRoleName = responsibleRoleName,
                                    dueDate = remote.effectiveEndDate,
                                    submission = sharedSubmission,
                                    submittedByName = submittedByName,
                                    isResubmission = sharedSubmissionIsResubmission,
                                    onViewSubmission = { submission ->
                                        selectedRemoteSubmission = submission
                                        selectedRemoteSubmissionPerson = null
                                        remoteSubmissionFileError = null
                                    }
                                )
                            }
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
                                    },
                                    remoteSubmissionLabel = when {
                                        selectedPeopleTab != PostManagementPeopleTab.VOLUNTEERS -> null
                                        post.remoteTimingState != PostTimingState.ONGOING -> null
                                        !role.roleMode.equals("REMOTE", ignoreCase = true) -> null
                                        post.remote?.submissionMode.equals("SHARED_TEAM", ignoreCase = true) &&
                                            role.roleTemplateId == post.remote?.responsibleRoleTemplateId -> {
                                            "Submitting role"
                                        }
                                        post.remote?.submissionMode.equals("INDIVIDUAL", ignoreCase = true) -> {
                                            "Submits own work"
                                        }
                                        else -> null
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

                                val showRemoteIndividualSubmission =
                                    selectedPeopleTab == PostManagementPeopleTab.VOLUNTEERS &&
                                        person.roleMode.equals("REMOTE", ignoreCase = true) &&
                                        post.remoteTimingState == PostTimingState.ONGOING &&
                                        post.remote?.submissionMode.equals("INDIVIDUAL", ignoreCase = true)

                                val individualSubmission = if (showRemoteIndividualSubmission) {
                                    post.remoteSubmissions
                                        .filter { submission ->
                                            submission.submissionType.equals("INDIVIDUAL", ignoreCase = true) &&
                                                submission.roleTemplateId == person.roleTemplateId &&
                                                submission.userId == person.userId
                                        }
                                        .maxWithOrNull(
                                            compareBy<PostManagementRemoteSubmission> {
                                                it.submittedAt.orEmpty()
                                            }.thenBy { it.submissionId }
                                        )
                                } else {
                                    null
                                }
                                val individualSubmissionIsResubmission = individualSubmission?.let { submission ->
                                    post.isRemoteResubmission(submission)
                                } == true

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
                                    remoteSubmissionRequirement = if (showRemoteIndividualSubmission) {
                                        role.individualSubmissionRequirement
                                    } else {
                                        null
                                    },
                                    remoteSubmissionDueDate = if (showRemoteIndividualSubmission) {
                                        post.remote?.effectiveEndDate
                                    } else {
                                        null
                                    },
                                    remoteSubmission = individualSubmission,
                                    remoteSubmissionIsResubmission = individualSubmissionIsResubmission,
                                    canCorrectAttendance = physicalAttendance?.canCorrectAttendance == true,
                                    isUpdatingAttendance = isUpdatingAttendance,
                                    onMarkPresent = onMarkPresent,
                                    onRequestMarkAbsent = { selected, date ->
                                        absentConfirmationPerson = selected
                                        absentConfirmationDate = date
                                    },
                                    onViewRemoteSubmission = { selected, submission ->
                                        selectedRemoteSubmission = submission
                                        selectedRemoteSubmissionPerson = selected
                                        remoteSubmissionFileError = null
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
                    } else if (showRemoteReview && remoteReview != null) {
                        item(key = "remote_completion_review") {
                            PostManagementRemoteReviewContent(
                                review = remoteReview,
                                session = remoteReviewSession,
                                evaluations = post.evaluations,
                                isSaving = isUpdatingRemoteReview,
                                actionMessage = remoteReviewActionMessage,
                                onViewSubmission = { item ->
                                    val submission = item.latestSubmission
                                    if (submission != null) {
                                        selectedRemoteSubmission = submission
                                        selectedRemoteSubmissionPerson = item.person
                                        remoteSubmissionFileError = null
                                        remoteSubmissionReviewError = null
                                    }
                                },
                                onMissingAction = onSetRemoteMissingAction,
                                onNewEndDateChange = onSetRemoteReviewNewEndDate,
                                onSaveSubmissionStage = onSaveRemoteSubmissionStage,
                                onCompletionDecision = onSetRemoteCompletionDecision,
                                onChangeCompletionDecision = onChangeRemoteCompletionDecision,
                                onFeedbackChange = onSetRemoteFeedback,
                                onStageChange = onRemoteReviewStageChange,
                                onFinalize = onFinalizeRemoteReview,
                                onViewProfile = { selectedPerson = it }
                            )
                        }
                    } else {
                        item(key = "review_unavailable") {
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

    selectedRemoteSubmission?.let { submission ->
        val isShared = submission.submissionType.equals("SHARED", ignoreCase = true)
        val submittedByName = submission.userId?.let { submittedByUserId ->
            post.volunteers.firstOrNull { volunteer ->
                volunteer.userId == submittedByUserId
            }?.fullName
        }
        val roleName = if (isShared) {
            post.roles.firstOrNull { role ->
                role.roleTemplateId == post.remote?.responsibleRoleTemplateId
            }?.roleName
        } else {
            selectedRemoteSubmissionPerson?.roleName
        }
        val isResubmission = post.isRemoteResubmission(submission)
        val canReview =
            submission.status.equals("PENDING_REVIEW", ignoreCase = true) &&
                (
                    post.remoteTimingState == PostTimingState.ONGOING ||
                        (showRemoteReview && remoteReview?.canEdit == true)
                )
        val isRemoteSubmissionBusy =
            isOpeningRemoteSubmission ||
                isDownloadingRemoteSubmission ||
                isReviewingRemoteSubmission

        PostManagementRemoteSubmissionDialog(
            submission = submission,
            personName = selectedRemoteSubmissionPerson?.fullName,
            roleName = roleName,
            submittedByName = submittedByName,
            dueDate = post.remote?.effectiveEndDate.orEmpty(),
            isResubmission = isResubmission,
            canReview = canReview,
            isOpeningFile = isOpeningRemoteSubmission,
            isDownloadingFile = isDownloadingRemoteSubmission,
            isReviewing = isReviewingRemoteSubmission,
            fileActionError = remoteSubmissionFileError,
            downloadMessage = remoteSubmissionDownloadMessage,
            onOpenFile = {
                if (!isRemoteSubmissionBusy) {
                    val submissionUrl = submission.submissionUrl
                        ?.takeIf { it.isNotBlank() }
                    val filePath = submission.filePath
                        ?.takeIf { it.isNotBlank() }

                    if (filePath == null && submissionUrl == null) {
                        remoteSubmissionFileError =
                            "This submission does not contain a file or link."
                    } else {
                        isOpeningRemoteSubmission = true
                        remoteSubmissionFileError = null
                        remoteSubmissionDownloadMessage = null

                        coroutineScope.launch {
                            try {
                                if (filePath != null) {
                                    val bytes = onDownloadRemoteSubmission(submission)
                                    openRemoteSubmissionFile(
                                        context = context,
                                        filePath = filePath,
                                        bytes = bytes
                                    )
                                } else if (submissionUrl != null) {
                                    openRemoteSubmissionUrl(
                                        context = context,
                                        url = submissionUrl
                                    )
                                }
                            } catch (exception: Exception) {
                                remoteSubmissionFileError = exception.message
                                    ?: "Unable to open this submission."
                            } finally {
                                isOpeningRemoteSubmission = false
                            }
                        }
                    }
                }
            },
            onDownloadFile = {
                if (!isRemoteSubmissionBusy) {
                    val filePath = submission.filePath
                        ?.takeIf { it.isNotBlank() }

                    if (filePath == null) {
                        remoteSubmissionFileError =
                            "This submission does not contain a downloadable file."
                    } else {
                        remoteSubmissionFileError = null
                        remoteSubmissionDownloadMessage = null
                        pendingRemoteDownloadSubmission = submission
                        remoteSubmissionDownloadLauncher.launch(
                            createRemoteSubmissionDownloadIntent(filePath)
                        )
                    }
                }
            },
            onRequestRevision = {
                if (!isRemoteSubmissionBusy && canReview) {
                    remoteSubmissionReviewError = null
                    revisionFeedback = ""
                    revisionSubmission = submission
                }
            },
            onAccept = {
                if (!isRemoteSubmissionBusy && canReview) {
                    remoteSubmissionReviewError = null
                    acceptSubmission = submission
                }
            },
            onNotAccept = if (showRemoteReview && remoteReview?.canEdit == true) {
                {
                    if (!isRemoteSubmissionBusy && canReview) {
                        remoteSubmissionReviewError = null
                        notAcceptSubmission = submission
                    }
                }
            } else {
                null
            },
            onDismiss = {
                if (!isRemoteSubmissionBusy) {
                    selectedRemoteSubmission = null
                    selectedRemoteSubmissionPerson = null
                    remoteSubmissionFileError = null
                    remoteSubmissionDownloadMessage = null
                    remoteSubmissionReviewError = null
                }
            }
        )
    }

    revisionSubmission?.let { submission ->
        PostManagementRequestRevisionDialog(
            isShared = submission.submissionType.equals("SHARED", ignoreCase = true),
            dueDate = post.remote?.effectiveEndDate.orEmpty(),
            feedback = revisionFeedback,
            needsProjectDeadlineExtension = showRemoteReview,
            isSaving = isReviewingRemoteSubmission,
            errorMessage = remoteSubmissionReviewError,
            onFeedbackChange = {
                revisionFeedback = it
                remoteSubmissionReviewError = null
            },
            onDismiss = {
                if (!isReviewingRemoteSubmission) {
                    revisionSubmission = null
                    remoteSubmissionReviewError = null
                }
            },
            onConfirm = {
                if (revisionFeedback.isBlank()) {
                    remoteSubmissionReviewError = "Please explain what needs to be revised."
                } else if (showRemoteReview) {
                    onSetRemoteSubmissionDecision(
                        submission,
                        "REQUEST_REVISION",
                        revisionFeedback.trim()
                    )
                    revisionSubmission = null
                    selectedRemoteSubmission = null
                    selectedRemoteSubmissionPerson = null
                    revisionFeedback = ""
                    remoteSubmissionReviewError = null
                } else if (!isReviewingRemoteSubmission) {
                    isReviewingRemoteSubmission = true
                    remoteSubmissionReviewError = null

                    coroutineScope.launch {
                        try {
                            onReviewRemoteSubmission(
                                submission,
                                "REQUEST_REVISION",
                                revisionFeedback.trim()
                            )
                            revisionSubmission = null
                            selectedRemoteSubmission = null
                            selectedRemoteSubmissionPerson = null
                            revisionFeedback = ""
                        } catch (exception: Exception) {
                            remoteSubmissionReviewError = exception.message
                                ?: "Unable to request revision."
                        } finally {
                            isReviewingRemoteSubmission = false
                        }
                    }
                }
            }
        )
    }

    acceptSubmission?.let { submission ->
        PostManagementAcceptSubmissionDialog(
            isShared = submission.submissionType.equals("SHARED", ignoreCase = true),
            isSaving = isReviewingRemoteSubmission,
            errorMessage = remoteSubmissionReviewError,
            onDismiss = {
                if (!isReviewingRemoteSubmission) {
                    acceptSubmission = null
                    remoteSubmissionReviewError = null
                }
            },
            onConfirm = {
                if (showRemoteReview) {
                    onSetRemoteSubmissionDecision(submission, "ACCEPT", null)
                    acceptSubmission = null
                    selectedRemoteSubmission = null
                    selectedRemoteSubmissionPerson = null
                    remoteSubmissionReviewError = null
                } else if (!isReviewingRemoteSubmission) {
                    isReviewingRemoteSubmission = true
                    remoteSubmissionReviewError = null

                    coroutineScope.launch {
                        try {
                            onReviewRemoteSubmission(
                                submission,
                                "ACCEPT",
                                null
                            )
                            acceptSubmission = null
                            selectedRemoteSubmission = null
                            selectedRemoteSubmissionPerson = null
                        } catch (exception: Exception) {
                            remoteSubmissionReviewError = exception.message
                                ?: "Unable to accept this submission."
                        } finally {
                            isReviewingRemoteSubmission = false
                        }
                    }
                }
            }
        )
    }

    notAcceptSubmission?.let { submission ->
        PostManagementNotAcceptSubmissionDialog(
            isShared = submission.submissionType.equals("SHARED", ignoreCase = true),
            onDismiss = { notAcceptSubmission = null },
            onConfirm = {
                onSetRemoteSubmissionDecision(submission, "NOT_ACCEPT", null)
                notAcceptSubmission = null
                selectedRemoteSubmission = null
                selectedRemoteSubmissionPerson = null
                remoteSubmissionReviewError = null
            }
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

private fun createRemoteSubmissionDownloadIntent(filePath: String): Intent {
    val fileName = filePath.substringAfterLast('/')
        .takeIf { it.isNotBlank() }
        ?: "remote-submission"

    return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = remoteSubmissionMimeType(fileName)
        putExtra(Intent.EXTRA_TITLE, fileName)
    }
}

private fun saveRemoteSubmissionFile(
    context: Context,
    destination: Uri,
    bytes: ByteArray
) {
    val outputStream = context.contentResolver.openOutputStream(destination)
        ?: error("Unable to open the selected download location.")

    outputStream.use { stream ->
        stream.write(bytes)
        stream.flush()
    }
}

private fun remoteSubmissionMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "")
    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension.lowercase(Locale.US))
        ?: "application/octet-stream"
}

private fun openRemoteSubmissionFile(
    context: Context,
    filePath: String,
    bytes: ByteArray
) {
    val originalName = filePath.substringAfterLast('/')
        .takeIf { it.isNotBlank() }
        ?: "remote-submission"
    val safeName = originalName.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val cacheDirectory = File(context.cacheDir, "remote_submissions").apply {
        mkdirs()
    }
    val file = File(cacheDirectory, safeName).apply {
        writeBytes(bytes)
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val mimeType = remoteSubmissionMimeType(safeName)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(Intent.createChooser(intent, "Open submission"))
    } catch (_: ActivityNotFoundException) {
        throw IllegalStateException("No app is available to open this file type.")
    }
}

private fun openRemoteSubmissionUrl(
    context: Context,
    url: String
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        throw IllegalStateException("No app is available to open this submission link.")
    }
}

private fun PostManagementPost.isRemoteResubmission(
    submission: PostManagementRemoteSubmission
): Boolean {
    if (!submission.status.equals("PENDING_REVIEW", ignoreCase = true)) return false

    return remoteSubmissions.any { previous ->
        previous.submissionId != submission.submissionId &&
            previous.status.equals("REVISION_REQUESTED", ignoreCase = true) &&
            previous.submissionType.equals(submission.submissionType, ignoreCase = true) &&
            when {
                submission.submissionType.equals("SHARED", ignoreCase = true) -> true
                submission.submissionType.equals("INDIVIDUAL", ignoreCase = true) ->
                    previous.roleTemplateId == submission.roleTemplateId &&
                        previous.userId == submission.userId
                else -> false
            }
    }
}
