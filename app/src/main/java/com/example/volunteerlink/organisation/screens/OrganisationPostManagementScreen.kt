package com.example.volunteerlink.organisation.screens

// FILE OVERVIEW:
/*
 * OrganisationPostManagementScreen contains presentation code for the organisation Manage Post flow.
 * It focuses on rendering state and forwarding user actions through callbacks/ViewModels,
 * keeping database access and business rules outside the composables where possible.
 */


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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.data.post.DraftAttentionType
import com.example.volunteerlink.data.post.PostMode
import com.example.volunteerlink.data.post.PostTimingEvaluator
import com.example.volunteerlink.data.post.PostTimingInput
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.organisation.components.OrganisationOfflineStatusCard
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementRole
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewItem
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewSession
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewStage
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmission
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmissionDecisionType
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
    onViewVolunteerProfile: (String) -> Unit = {},
    onMessageVolunteer: (String) -> Unit = {},
    onViewApplication: (roleTemplateId: String, userId: String) -> Unit = { _, _ -> },
    returnToPeopleAfterApplicantReview: Boolean = false,
    openPeopleFromHome: Boolean = false,
    openReviewFromHome: Boolean = false,
    onReturnToPeopleHandled: () -> Unit = {},
    onHomeTargetHandled: () -> Unit = {},
    onExitProtectionChanged: (Boolean, (() -> Unit)?) -> Unit = { _, _ -> },
    viewModel: OrganisationPostManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Keep the main/People tab selection above the loading branch. A refresh used
    // to remove OrganisationPostManagementContent from composition, which reset
    // its local rememberSaveable state back to Overview.
    var selectedTabName by rememberSaveable(postId) {
        mutableStateOf(PostManagementTab.OVERVIEW.name)
    }
    var selectedPeopleTabName by rememberSaveable(postId) {
        mutableStateOf(PostManagementPeopleTab.APPLICANTS.name)
    }

    LaunchedEffect(returnToPeopleAfterApplicantReview) {
        if (returnToPeopleAfterApplicantReview) {
            selectedTabName = PostManagementTab.PEOPLE.name
            onReturnToPeopleHandled()
        }
    }


    LaunchedEffect(openPeopleFromHome, openReviewFromHome) {
        when {
            openReviewFromHome -> {
                selectedTabName = PostManagementTab.REVIEW.name
                onHomeTargetHandled()
            }

            openPeopleFromHome -> {
                selectedTabName = PostManagementTab.PEOPLE.name
                selectedPeopleTabName = PostManagementPeopleTab.APPLICANTS.name
                onHomeTargetHandled()
            }
        }
    }

    LaunchedEffect(postId) {
        viewModel.load(postId)
    }

    // Applicant Review uses its own destination-scoped ViewModel. When a decision
    // succeeds and that screen pops back here, refresh this existing Manage Post
    // screen so the People tab immediately reflects Accepted/Declined state.
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasHandledFirstResume by rememberSaveable(postId) { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasHandledFirstResume) {
                    viewModel.refresh()
                } else {
                    hasHandledFirstResume = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val post = uiState.post
    val reviewSession = uiState.physicalReviewSession
    val remoteReviewSession = uiState.remoteReviewSession
    val hasUnfinishedReview = reviewSession.hasUnfinishedReview || remoteReviewSession.hasUnfinishedReview
    var confirmLeaveReview by rememberSaveable { mutableStateOf(false) }

    /**
     * Derives the discard and leave value used by the organisation Manage Post flow.
     * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
     */
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
            selectedTabName = selectedTabName,
            onSelectedTabNameChange = { selectedTabName = it },
            selectedPeopleTabName = selectedPeopleTabName,
            onSelectedPeopleTabNameChange = { selectedPeopleTabName = it },
            isStartingAttendance = uiState.isStartingAttendance,
            isUpdatingAttendance = uiState.isUpdatingAttendance,
            attendanceActionMessage = uiState.attendanceActionMessage,
            isUpdatingReview = uiState.isUpdatingReview,
            reviewActionMessage = uiState.reviewActionMessage,
            physicalReviewSession = reviewSession,
            isUpdatingRemoteReview = uiState.isUpdatingRemoteReview,
            remoteReviewActionMessage = uiState.remoteReviewActionMessage,
            remoteReviewSession = remoteReviewSession,
            isShowingCachedData = uiState.isShowingCachedData,
            lastSyncedAtEpochMillis = uiState.lastSyncedAtEpochMillis,
            isRefreshing = uiState.isRefreshing,
            isPublishingDraft = uiState.isPublishingDraft,
            draftPublishMessage = uiState.draftPublishMessage,
            draftPublishError = uiState.draftPublishError,
            isLoadingPostGroup = uiState.isLoadingPostGroup,
            isAddingPostGroupMembers = uiState.isAddingPostGroupMembers,
            postGroupConversationId = uiState.postGroupConversationId,
            postGroupEligibleCount = uiState.postGroupEligibleCount,
            postGroupActiveMemberCount = uiState.postGroupActiveMemberCount,
            postGroupMissingCount = uiState.postGroupMissingCount,
            postGroupHasStarted = uiState.postGroupHasStarted,
            postGroupCanAdd = uiState.postGroupCanAdd,
            postGroupActionMessage = uiState.postGroupActionMessage,
            onSyncSelected = viewModel::refresh,
            onPublishDraft = viewModel::publishSavedDraft,
            onAddAllToGroup = viewModel::addAllAcceptedVolunteersToGroup,
            onBack = {
                if (hasUnfinishedReview) confirmLeaveReview = true else onBack()
            },
            onEdit = onEdit,
            onViewVolunteerProfile = onViewVolunteerProfile,
            onMessageVolunteer = onMessageVolunteer,
            onViewApplication = onViewApplication,
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
                                "Saving final Remote feedback and completing the project. Please wait."
                            isRemoteOperation ->
                                "Saving submission decisions and the Remote deadline. Please wait."
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
                    text = if (post?.mode.equals("HYBRID", ignoreCase = true)) {
                        "Physical review completed"
                    } else {
                        "Event review completed"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
            },
            text = {
                Text(
                    text = if (post?.mode.equals("HYBRID", ignoreCase = true) &&
                        !post?.databaseStatus.equals("COMPLETED", ignoreCase = true)
                    ) {
                        "The Physical side has been finalized successfully. Remote activity and Remote review remain independent, so the Hybrid post stays open until both sides are finished."
                    } else {
                        "The review has been finalized successfully. This post has moved from Needs Review to Completed. Saved attendance, completion decisions and feedback are now read-only."
                    },
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
                    text = if (post?.mode.equals("HYBRID", ignoreCase = true) &&
                        !post?.databaseStatus.equals("COMPLETED", ignoreCase = true)
                    ) {
                        "The Remote side has been finalized successfully. Physical activity and Physical review remain independent, so the Hybrid post stays open until both sides are finished."
                    } else {
                        "The Remote project review has been finalized successfully. The post is now Completed and the saved outcomes are read-only."
                    },
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
/**
 * Renders the organisation post management content content block used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun OrganisationPostManagementContent(
    post: PostManagementPost,
    selectedTabName: String,
    onSelectedTabNameChange: (String) -> Unit,
    selectedPeopleTabName: String,
    onSelectedPeopleTabNameChange: (String) -> Unit,
    isStartingAttendance: Boolean,
    isUpdatingAttendance: Boolean,
    attendanceActionMessage: String?,
    isUpdatingReview: Boolean,
    reviewActionMessage: String?,
    physicalReviewSession: com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReviewSession,
    isUpdatingRemoteReview: Boolean,
    remoteReviewActionMessage: String?,
    remoteReviewSession: PostManagementRemoteReviewSession,
    isShowingCachedData: Boolean,
    lastSyncedAtEpochMillis: Long?,
    isRefreshing: Boolean,
    isPublishingDraft: Boolean,
    draftPublishMessage: String?,
    draftPublishError: String?,
    isLoadingPostGroup: Boolean,
    isAddingPostGroupMembers: Boolean,
    postGroupConversationId: String?,
    postGroupEligibleCount: Int,
    postGroupActiveMemberCount: Int,
    postGroupMissingCount: Int,
    postGroupHasStarted: Boolean,
    postGroupCanAdd: Boolean,
    postGroupActionMessage: String?,
    onSyncSelected: () -> Unit,
    onPublishDraft: () -> Unit,
    onAddAllToGroup: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onViewVolunteerProfile: (String) -> Unit,
    onMessageVolunteer: (String) -> Unit,
    onViewApplication: (roleTemplateId: String, userId: String) -> Unit,
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
    onSetRemoteFeedback: (PostManagementPerson, String) -> Unit,
    onRemoteReviewStageChange: (PostManagementRemoteReviewStage) -> Unit,
    onFinalizeRemoteReview: () -> Unit,
    onStartAttendancePolling: () -> Unit,
    onStopAttendancePolling: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedRoleId by rememberSaveable { mutableStateOf<String?>(null) }
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
        post.mode.uppercase(Locale.US) in setOf("PHYSICAL", "HYBRID") &&
            physicalReview != null &&
            (
                post.physicalTimingState == PostTimingState.PAST ||
                    post.databaseStatus.equals("COMPLETED", ignoreCase = true)
            )
    val showRemoteReview =
        post.mode.uppercase(Locale.US) in setOf("REMOTE", "HYBRID") &&
            remoteReview != null &&
            (
                post.remoteTimingState == PostTimingState.PAST ||
                    post.databaseStatus.equals("COMPLETED", ignoreCase = true)
            )
    val normalizedDatabaseStatus = post.databaseStatus.trim().uppercase(Locale.US)
    val isDraft = normalizedDatabaseStatus == "DRAFT"
    val showReview = !isDraft && (showPhysicalReview || showRemoteReview)
    val showPeople = !isDraft &&
        (!showReview || post.mode.equals("HYBRID", ignoreCase = true))

    val draftAttention = if (isDraft) {
        PostMode.fromDatabaseValue(post.mode)?.let { mode ->
            PostTimingEvaluator.evaluateDraftAttention(
                input = PostTimingInput(
                    mode = mode,
                    physicalStartDate = post.physical?.startDate,
                    physicalEndDate = post.physical?.endDate,
                    remoteStartDate = post.remote?.startDate,
                    remoteEndDate = post.remote?.effectiveEndDate
                )
            )
        }
    } else {
        null
    }
    val draftPublishBlockMessage = when (draftAttention?.type) {
        DraftAttentionType.START_TOO_SOON ->
            "The volunteering start is less than 7 days away. Edit the start date before publishing."
        DraftAttentionType.START_DATE_PASSED ->
            "The volunteering start date has passed. Edit the date before publishing."
        else -> null
    }
    val effectiveSelectedTab = if (isDraft) {
        PostManagementTab.OVERVIEW
    } else {
        selectedTab
    }

    var selectedHybridReviewSideName by rememberSaveable(post.postId) {
        mutableStateOf(
            if (showPhysicalReview) {
                PostManagementHybridReviewSide.PHYSICAL.name
            } else {
                PostManagementHybridReviewSide.REMOTE.name
            }
        )
    }
    val selectedHybridReviewSide = runCatching {
        PostManagementHybridReviewSide.valueOf(selectedHybridReviewSideName)
    }.getOrDefault(
        if (showPhysicalReview) PostManagementHybridReviewSide.PHYSICAL
        else PostManagementHybridReviewSide.REMOTE
    )

    LaunchedEffect(showPhysicalReview, showRemoteReview) {
        if (selectedHybridReviewSide == PostManagementHybridReviewSide.PHYSICAL && !showPhysicalReview && showRemoteReview) {
            selectedHybridReviewSideName = PostManagementHybridReviewSide.REMOTE.name
        } else if (selectedHybridReviewSide == PostManagementHybridReviewSide.REMOTE && !showRemoteReview && showPhysicalReview) {
            selectedHybridReviewSideName = PostManagementHybridReviewSide.PHYSICAL.name
        }
    }

    LaunchedEffect(isDraft, showReview, showPeople) {
        if (isDraft && selectedTabName != PostManagementTab.OVERVIEW.name) {
            onSelectedTabNameChange(PostManagementTab.OVERVIEW.name)
        } else if (!showPeople && selectedTabName == PostManagementTab.PEOPLE.name) {
            onSelectedTabNameChange(if (showReview) PostManagementTab.REVIEW.name else PostManagementTab.OVERVIEW.name)
        } else if (!showReview && selectedTabName == PostManagementTab.REVIEW.name) {
            onSelectedTabNameChange(if (showPeople) PostManagementTab.PEOPLE.name else PostManagementTab.OVERVIEW.name)
        }
    }

    val shouldPollAttendance =
        !isShowingCachedData &&
            effectiveSelectedTab == PostManagementTab.PEOPLE &&
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

    val openReviewApplicantRoleIds = post.roles
        .filter { role ->
            role.isApplicationOpen &&
                role.applicationMethod.equals("REVIEW_APPLICANTS", ignoreCase = true)
        }
        .map { it.roleTemplateId }
        .toSet()
    val openApplicants = post.applicants.filter { applicant ->
        applicant.roleTemplateId in openReviewApplicantRoleIds
    }
    // Do not keep an empty Applicants tab after every pending row has been
    // accepted/declined/auto-declined. It reappears as soon as a new PENDING
    // Review Applicants row exists.
    val showApplicantsTab = openApplicants.isNotEmpty()

    LaunchedEffect(showApplicantsTab) {
        if (
            !showApplicantsTab &&
            selectedPeopleTabName != PostManagementPeopleTab.VOLUNTEERS.name
        ) {
            onSelectedPeopleTabNameChange(PostManagementPeopleTab.VOLUNTEERS.name)
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

    val showEdit = !isShowingCachedData && when (normalizedDatabaseStatus) {
        "DRAFT" -> true
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
            showEdit = showEdit,
            showEditAttention = isDraft && draftAttention?.needsAttention == true
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
            if (isShowingCachedData) {
                item(key = "post_offline_status") {
                    OrganisationOfflineStatusCard(
                        lastSyncedAtEpochMillis = lastSyncedAtEpochMillis,
                        isSyncing = isRefreshing,
                        onSyncSelected = onSyncSelected
                    )
                }
            }

            item(key = "post_summary") {
                PostManagementSummaryCard(post)
            }

            if (!draftPublishMessage.isNullOrBlank()) {
                item(key = "draft_publish_success") {
                    Text(
                        text = draftPublishMessage,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }

            if (!remoteReviewActionMessage.isNullOrBlank() && !showRemoteReview) {
                item(key = "remote_review_saved_message") {
                    Text(
                        text = remoteReviewActionMessage,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }

            item(key = "post_tabs") {
                PostManagementMainTabs(
                    selected = effectiveSelectedTab,
                    pendingApplicantCount = if (isDraft) 0 else openApplicants.size,
                    showPeopleTab = showPeople,
                    showReviewTab = showReview,
                    onSelected = { onSelectedTabNameChange(it.name) }
                )
            }

            when (effectiveSelectedTab) {
                PostManagementTab.OVERVIEW -> {
                    item(key = "overview") {
                        PostManagementOverview(post)
                    }

                    if (isDraft) {
                        item(key = "draft_publish_action") {
                            PostManagementDraftPublishSection(
                                isPublishing = isPublishingDraft,
                                blockMessage = draftPublishBlockMessage
                                    ?: if (isShowingCachedData) {
                                        "Reconnect to the internet before publishing this draft."
                                    } else {
                                        null
                                    },
                                errorMessage = draftPublishError,
                                enabled = !isShowingCachedData &&
                                    draftPublishBlockMessage == null &&
                                    !isPublishingDraft,
                                onPublish = onPublishDraft
                            )
                        }
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
                        if (selectedPeopleTab == PostManagementPeopleTab.VOLUNTEERS) {
                            PostManagementGroupSyncCard(
                                groupExists = postGroupConversationId != null,
                                eligibleCount = postGroupEligibleCount,
                                activeMemberCount = postGroupActiveMemberCount,
                                missingCount = postGroupMissingCount,
                                hasStarted = postGroupHasStarted,
                                canAdd = postGroupCanAdd,
                                isLoading = isLoadingPostGroup,
                                isAdding = isAddingPostGroupMembers,
                                actionMessage = postGroupActionMessage,
                                onAddAll = onAddAllToGroup,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        PostManagementPeopleControls(
                            selectedTab = selectedPeopleTab,
                            showApplicantsTab = showApplicantsTab,
                            applicantCount = openApplicants.size,
                            volunteerCount = post.volunteers.size,
                            query = searchQuery,
                            selectedRoleId = selectedRoleId,
                            roles = rolesForSelectedPeopleTab,
                            onTabSelected = {
                                onSelectedPeopleTabNameChange(it.name)
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
                                        person.completionStatus.uppercase(Locale.US) in
                                            setOf("IN_PROGRESS", "NEEDS_REVIEW") &&
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
                                    applicationMethod = role.applicationMethod,
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
                                    onViewProfile = { onViewVolunteerProfile(it.userId) },
                                    onMessage = { onMessageVolunteer(it.userId) },
                                    onViewApplication = { selected ->
                                        onViewApplication(
                                            selected.roleTemplateId,
                                            selected.userId
                                        )
                                    },
                                    onToggleShortlist = onToggleShortlist
                                )
                            }
                        }
                    }
                }

                PostManagementTab.REVIEW -> {
                    if (post.mode.equals("HYBRID", ignoreCase = true) && showPhysicalReview && showRemoteReview) {
                        item(key = "hybrid_review_selector") {
                            PostManagementHybridReviewSelector(
                                selected = selectedHybridReviewSide,
                                showPhysical = showPhysicalReview,
                                showRemote = showRemoteReview,
                                onSelected = { selectedHybridReviewSideName = it.name }
                            )
                        }
                    }

                    val showSelectedPhysical = showPhysicalReview &&
                        (
                            !post.mode.equals("HYBRID", ignoreCase = true) ||
                                !showRemoteReview ||
                                selectedHybridReviewSide == PostManagementHybridReviewSide.PHYSICAL
                            )
                    val showSelectedRemote = showRemoteReview &&
                        (
                            !post.mode.equals("HYBRID", ignoreCase = true) ||
                                !showPhysicalReview ||
                                selectedHybridReviewSide == PostManagementHybridReviewSide.REMOTE
                            )

                    val review = physicalReview
                    if (showSelectedPhysical && review != null) {
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
                                onViewProfile = { onViewVolunteerProfile(it.userId) }
                            )
                        }
                    } else if (showSelectedRemote && remoteReview != null) {
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
                                onFeedbackChange = onSetRemoteFeedback,
                                onStageChange = onRemoteReviewStageChange,
                                onFinalize = onFinalizeRemoteReview,
                                onViewProfile = { onViewVolunteerProfile(it.userId) }
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
                    revisionFeedback = remoteReviewSession.submissionDecisions
                        .firstOrNull { draft ->
                            draft.submissionId == submission.submissionId &&
                                draft.decision == PostManagementRemoteSubmissionDecisionType.REQUEST_REVISION
                        }
                        ?.feedback
                        .orEmpty()
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
        val isEditingRevisionDraft = showRemoteReview &&
            remoteReviewSession.submissionDecisions.any { draft ->
                draft.submissionId == submission.submissionId &&
                    draft.decision == PostManagementRemoteSubmissionDecisionType.REQUEST_REVISION
            }
        PostManagementRequestRevisionDialog(
            isShared = submission.submissionType.equals("SHARED", ignoreCase = true),
            dueDate = post.remote?.effectiveEndDate.orEmpty(),
            feedback = revisionFeedback,
            isEditingDraft = isEditingRevisionDraft,
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

/**
 * Creates the remote submission download intent used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
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

/**
 * Saves the remote submission file for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
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

/**
 * Derives the remote submission mime type value used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun remoteSubmissionMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "")
    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension.lowercase(Locale.US))
        ?: "application/octet-stream"
}

/**
 * Opens the remote submission file in the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
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

/**
 * Opens the remote submission url in the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
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

/**
 * Derives the post management post value used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
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
