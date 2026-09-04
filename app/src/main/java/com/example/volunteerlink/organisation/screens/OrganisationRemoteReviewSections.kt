package com.example.volunteerlink.organisation.screens

// FILE OVERVIEW:
/*
 * OrganisationRemoteReviewSections contains presentation code for the organisation Manage Post flow.
 * It focuses on rendering state and forwarding user actions through callbacks/ViewModels,
 * keeping database access and business rules outside the composables where possible.
 */


import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.components.OrganisationDivider
import com.example.volunteerlink.organisation.components.OrganisationInfoStrip
import com.example.volunteerlink.organisation.components.OrganisationListRow
import com.example.volunteerlink.organisation.components.OrganisationMessageButton
import com.example.volunteerlink.organisation.components.OrganisationPrimaryButton
import com.example.volunteerlink.organisation.components.OrganisationSectionHeader
import com.example.volunteerlink.organisation.components.OrganisationSectionSurface
import com.example.volunteerlink.organisation.components.OrganisationStatusPill
import com.example.volunteerlink.organisation.manage.model.PostManagementEvaluation
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteMissingAction
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReview
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewItem
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewSession
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewStage
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmissionDecisionType
import com.example.volunteerlink.organisation.manage.model.remoteReviewParticipationKey
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
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

private val RemoteReviewPillShape = RoundedCornerShape(50)

@Composable
/**
 * Renders the post management remote review content content block used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun PostManagementRemoteReviewContent(
    review: PostManagementRemoteReview,
    session: PostManagementRemoteReviewSession,
    evaluations: List<PostManagementEvaluation>,
    isSaving: Boolean,
    actionMessage: String?,
    onViewSubmission: (PostManagementRemoteReviewItem) -> Unit,
    onMissingAction: (String, Boolean) -> Unit,
    onNewEndDateChange: (String?) -> Unit,
    onSaveSubmissionStage: () -> Unit,
    onFeedbackChange: (PostManagementPerson, String) -> Unit,
    onStageChange: (PostManagementRemoteReviewStage) -> Unit,
    onFinalize: () -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = if (review.canEdit) session.stage else PostManagementRemoteReviewStage.FINISH
    var confirmFinalize by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        RemoteReviewFlowHeader(stage = stage, finalized = !review.canEdit)

        if (!actionMessage.isNullOrBlank()) {
            RemoteReviewMessageStrip(actionMessage)
        }

        when (stage) {
            PostManagementRemoteReviewStage.SUBMISSION -> RemoteSubmissionReviewStage(
                review = review,
                session = session,
                busy = isSaving,
                onViewSubmission = onViewSubmission,
                onMissingAction = onMissingAction,
                onNewEndDateChange = onNewEndDateChange,
                onContinue = onSaveSubmissionStage
            )

            PostManagementRemoteReviewStage.FEEDBACK -> RemoteFeedbackReviewStage(
                review = review,
                session = session,
                busy = isSaving,
                onFeedbackChange = onFeedbackChange,
                onBack = { onStageChange(PostManagementRemoteReviewStage.SUBMISSION) },
                onContinue = { onStageChange(PostManagementRemoteReviewStage.FINISH) },
                onViewProfile = onViewProfile
            )

            PostManagementRemoteReviewStage.FINISH -> RemoteFinishReviewStage(
                review = review,
                session = session,
                evaluations = evaluations,
                busy = isSaving,
                onBack = { onStageChange(PostManagementRemoteReviewStage.FEEDBACK) },
                onFinalize = { confirmFinalize = true }
            )
        }
    }

    if (confirmFinalize) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) confirmFinalize = false },
            title = { RemoteReviewDialogTitle("Finalize Remote project?") },
            text = {
                Text(
                    text = "Submission decisions are already settled. Finalizing saves optional feedback, issues completion records for Completed volunteers, and closes this project as read-only.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextSecondary
                )
            },
            confirmButton = {
                Button(
                    enabled = !isSaving,
                    onClick = {
                        confirmFinalize = false
                        onFinalize()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                ) {
                    Text("Finalize Project", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !isSaving, onClick = { confirmFinalize = false }) {
                    Text("Cancel")
                }
            },
            containerColor = VolunteerLinkSurface
        )
    }
}

@Composable
/**
 * Renders the remote review flow header header used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun RemoteReviewFlowHeader(
    stage: PostManagementRemoteReviewStage,
    finalized: Boolean
) {
    val stages = PostManagementRemoteReviewStage.entries
    val activeIndex = stages.indexOf(stage)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (finalized) "Remote Review Complete" else "Remote Review",
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )
        Text(
            text = if (finalized) {
                "This review is finalized and read-only."
            } else {
                "Submission → Feedback → Finish"
            },
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 14.sp,
            color = VolunteerLinkTextSecondary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stages.forEachIndexed { index, item ->
                val complete = finalized || index < activeIndex
                val active = !finalized && index == activeIndex
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = when {
                            complete -> VolunteerLinkPrimaryGreen
                            active -> VolunteerLinkSoftGreenSurface
                            else -> VolunteerLinkSurface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (complete || active) VolunteerLinkPrimaryGreen else VolunteerLinkBorderColour
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (complete) "✓" else (index + 1).toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (complete) VolunteerLinkSurface else if (active) VolunteerLinkPrimaryGreen else VolunteerLinkTextSecondary
                            )
                        }
                    }
                    Text(
                        text = item.name.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) },
                        modifier = Modifier.padding(start = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = if (active || complete) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active || complete) VolunteerLinkPrimaryGreen else VolunteerLinkTextSecondary
                    )
                }
            }
        }
        OrganisationDivider(modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
/**
 * Renders the UI represented by remote submission review stage for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteSubmissionReviewStage(
    review: PostManagementRemoteReview,
    session: PostManagementRemoteReviewSession,
    busy: Boolean,
    onViewSubmission: (PostManagementRemoteReviewItem) -> Unit,
    onMissingAction: (String, Boolean) -> Unit,
    onNewEndDateChange: (String?) -> Unit,
    onContinue: () -> Unit
) {
    val allResolved = review.items.all { item ->
        when (item.currentStatus.uppercase(Locale.US)) {
            "ACCEPTED", "NOT_ACCEPTED" -> true
            "PENDING_REVIEW" -> session.submissionDecisionFor(item.itemKey) != null
            "REVISION_REQUESTED", "NOT_SUBMITTED" -> session.missingActionFor(item.itemKey) != null
            else -> false
        }
    }
    val extensionRequired = session.submissionDecisions.any {
        it.decision == PostManagementRemoteSubmissionDecisionType.REQUEST_REVISION
    } || review.items.any {
        session.missingActionFor(it.itemKey) == PostManagementRemoteMissingAction.GIVE_MORE_TIME
    }

    RemoteReviewStageSurface(
        title = "Submission review",
        subtitle = "Resolve every deliverable. Accepted work becomes Completed automatically."
    ) {
        OrganisationInfoStrip(
            title = "Remote deadline passed",
            message = "Current deadline · ${formatRemoteDate(review.currentDeadline)}",
            accent = VolunteerLinkWarning,
            iconRes = R.drawable.calendar
        )

        if (review.items.isEmpty()) {
            Text(
                text = "All Remote outcomes are already resolved. Continue to optional feedback.",
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = VolunteerLinkTextSecondary
            )
        } else {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                review.items.forEachIndexed { index, item ->
                    RemoteSubmissionReviewItemCard(
                        item = item,
                        session = session,
                        busy = busy,
                        onViewSubmission = { onViewSubmission(item) },
                        onMissingAction = { giveMoreTime -> onMissingAction(item.itemKey, giveMoreTime) }
                    )
                    if (index != review.items.lastIndex) OrganisationDivider()
                }
            }
        }

        if (extensionRequired) {
            RemoteDeadlineExtensionPanel(
                review = review,
                selectedDate = session.newEndDate,
                enabled = !busy,
                onSelected = onNewEndDateChange,
                modifier = Modifier.padding(top = 18.dp)
            )
        }

        if (!allResolved) {
            Text(
                text = "Every unresolved deliverable needs a decision before this stage can be saved.",
                modifier = Modifier.padding(top = 14.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        OrganisationPrimaryButton(
            text = when {
                busy -> "Saving..."
                extensionRequired -> "Save & Extend Remote Deadline"
                else -> "Save & Continue"
            },
            onClick = onContinue,
            enabled = !busy,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
/**
 * Renders the remote submission review item card card used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun RemoteSubmissionReviewItemCard(
    item: PostManagementRemoteReviewItem,
    session: PostManagementRemoteReviewSession,
    busy: Boolean,
    onViewSubmission: () -> Unit,
    onMissingAction: (Boolean) -> Unit
) {
    val submission = item.latestSubmission
    val status = item.currentStatus.uppercase(Locale.US)
    val draft = session.submissionDecisionFor(item.itemKey)
    val missingAction = session.missingActionFor(item.itemKey)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.isShared) "Team submission" else item.person?.fullName.orEmpty(),
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = if (item.isShared) "Shared Team · ${item.roleName}" else item.roleName,
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (!item.isShared && item.person != null) {
                    OrganisationMessageButton(personName = item.person.fullName)
                }
                Box(modifier = Modifier.padding(top = if (!item.isShared && item.person != null) 6.dp else 0.dp)) {
                    RemoteSubmissionStatusPill(status)
                }
            }
        }

        item.requirement?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        if (submission != null) {
            val submissionTitle = submission.filePath
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: if (!submission.submissionUrl.isNullOrBlank()) "Submitted link" else "Submission"

            OrganisationListRow(
                title = submissionTitle,
                subtitle = buildString {
                    submission.submittedAt?.takeIf { it.isNotBlank() }?.let {
                        append("Submitted ${formatRemoteDateTime(it)}")
                    }
                    if (item.isShared && !item.submittedByName.isNullOrBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append("by ${item.submittedByName}")
                    }
                }.takeIf { it.isNotBlank() },
                supportingText = if (item.isResubmission) "Revised submission · tap to review" else "Tap to open and review",
                iconRes = R.drawable.remote_project,
                statusColor = VolunteerLinkPrimaryGreen,
                enabled = !busy,
                onClick = onViewSubmission,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (draft != null) {
            RemoteDraftDecisionNotice(
                title = when (draft.decision) {
                    PostManagementRemoteSubmissionDecisionType.ACCEPT -> "Accept work → Completed"
                    PostManagementRemoteSubmissionDecisionType.REQUEST_REVISION -> "Request revision → Extension required"
                    PostManagementRemoteSubmissionDecisionType.NOT_ACCEPT -> "Not accept → Not Completed"
                },
                detail = draft.feedback,
                modifier = Modifier.padding(top = 10.dp)
            )
        } else when (status) {
            "PENDING_REVIEW" -> Text(
                text = "Open the submission to choose a decision.",
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                color = VolunteerLinkTextSecondary
            )

            "REVISION_REQUESTED" -> {
                if (!submission?.feedback.isNullOrBlank()) {
                    Text(
                        text = "Previous request · ${submission?.feedback}",
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = VolunteerLinkWarning
                    )
                }
                RemoteMissingWorkActions(
                    selected = missingAction,
                    revisionWasRequested = true,
                    sharedTeam = item.isShared,
                    busy = busy,
                    onSelected = onMissingAction,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            "NOT_SUBMITTED" -> RemoteMissingWorkActions(
                selected = missingAction,
                revisionWasRequested = false,
                sharedTeam = item.isShared,
                busy = busy,
                onSelected = onMissingAction,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
/**
 * Renders the UI represented by remote missing work actions for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteMissingWorkActions(
    selected: PostManagementRemoteMissingAction?,
    revisionWasRequested: Boolean,
    sharedTeam: Boolean,
    busy: Boolean,
    onSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (revisionWasRequested) "No revised version was received by the deadline." else "No submission was received by the deadline.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = VolunteerLinkWarning
        )

        RemoteActionChoice(
            title = "Give more time",
            subtitle = if (sharedTeam) "Keep the shared deliverable open under a new project deadline." else "Keep this participation open under a new project deadline.",
            selected = selected == PostManagementRemoteMissingAction.GIVE_MORE_TIME,
            color = VolunteerLinkPrimaryGreen,
            enabled = !busy,
            onClick = { onSelected(true) },
            modifier = Modifier.padding(top = 10.dp)
        )
        RemoteActionChoice(
            title = if (revisionWasRequested) "Continue without revision" else "Continue without submission",
            subtitle = if (sharedTeam) "Mark every unresolved Remote team member Not Completed." else "Mark this Remote participation Not Completed.",
            selected = selected == PostManagementRemoteMissingAction.CONTINUE_WITHOUT_WORK,
            color = VolunteerLinkWarning,
            enabled = !busy,
            onClick = { onSelected(false) },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
/**
 * Renders the UI represented by remote action choice for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteActionChoice(
    title: String,
    subtitle: String,
    selected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) color.copy(alpha = 0.08f) else VolunteerLinkSurface,
        border = BorderStroke(1.dp, if (selected) color else VolunteerLinkBorderColour)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (selected) color else VolunteerLinkTextPrimary)
                Text(subtitle, modifier = Modifier.padding(top = 3.dp), fontSize = 12.sp, lineHeight = 17.sp, color = VolunteerLinkTextSecondary)
            }
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.tick),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(19.dp),
                    tint = color
                )
            }
        }
    }
}

@Composable
/**
 * Renders the remote deadline extension panel panel used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun RemoteDeadlineExtensionPanel(
    review: PostManagementRemoteReview,
    selectedDate: String?,
    enabled: Boolean,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val minimumDate = remember(review.currentDeadline, review.todayDate) {
        maxRemoteDate(addRemoteDays(review.currentDeadline, 1), addRemoteDays(review.todayDate, 1))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OrganisationSectionHeader(
            title = "New Remote deadline",
            subtitle = if (review.submissionMode.equals("SHARED_TEAM", true)) {
                "The shared team receives one revised Remote deadline."
            } else {
                "Only unresolved work stays open; finalized outcomes remain unchanged."
            }
        )
        Text(
            text = "Current · ${formatRemoteDate(review.currentDeadline)}    Today · ${formatRemoteDate(review.todayDate)}",
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 12.sp,
            color = VolunteerLinkTextSecondary
        )
        OutlinedButton(
            onClick = {
                val initial = parseRemoteDate(selectedDate ?: minimumDate) ?: Date()
                val calendar = Calendar.getInstance().apply { time = initial }
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val chosen = Calendar.getInstance().apply {
                            set(year, month, day, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time
                        onSelected(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(chosen))
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    datePicker.minDate = parseRemoteDate(minimumDate)?.time ?: System.currentTimeMillis()
                }.show()
            },
            enabled = enabled,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(52.dp),
            border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen)
        ) {
            Text(
                text = selectedDate?.let { formatRemoteDate(it) } ?: "Choose new Remote deadline",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
        }
    }
}

@Composable
/**
 * Renders the UI represented by remote feedback review stage for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteFeedbackReviewStage(
    review: PostManagementRemoteReview,
    session: PostManagementRemoteReviewSession,
    busy: Boolean,
    onFeedbackChange: (PostManagementPerson, String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit
) {
    val completed = review.participants.filter { it.completionStatus.equals("COMPLETED", true) }
    val roles = completed.distinctBy { it.roleTemplateId }.sortedBy { it.roleName }
    var selectedRoleId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var composerOpen by rememberSaveable { mutableStateOf(false) }
    var composerText by rememberSaveable { mutableStateOf("") }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editingOriginalKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    val feedbackFor = { person: PostManagementPerson ->
        session.feedbackByParticipation[remoteReviewParticipationKey(person.roleTemplateId, person.userId)].orEmpty()
    }
    val filtered = completed.filter { person ->
        (selectedRoleId == null || person.roleTemplateId == selectedRoleId) &&
            (query.isBlank() || person.fullName.contains(query.trim(), true) || person.roleName.contains(query.trim(), true))
    }
    val groups = completed
        .filter { feedbackFor(it).isNotBlank() }
        .groupBy { feedbackFor(it) }
        .entries
        .sortedBy { it.key.lowercase(Locale.US) }
    val withoutFeedback = completed.filter { feedbackFor(it).isBlank() }

    /**
     * Resets the composer for the organisation Manage Post flow.
     * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
     */
    fun resetComposer() {
        composerOpen = false
        composerText = ""
        selectedKeys = emptySet()
        editingOriginalKeys = emptySet()
    }

    RemoteReviewStageSurface(
        title = "Feedback",
        subtitle = "Optional feedback for Completed volunteers. Use the same role filtering and grouped-feedback pattern as Physical review."
    ) {
        if (completed.isEmpty()) {
            OrganisationInfoStrip(
                title = "No Completed volunteers",
                message = "There is nobody eligible for final feedback in this Remote review."
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    RemoteFilterChip("All roles", selectedRoleId == null) { selectedRoleId = null }
                }
                items(roles, key = { it.roleTemplateId }) { person ->
                    RemoteFilterChip(person.roleName, selectedRoleId == person.roleTemplateId) {
                        selectedRoleId = person.roleTemplateId
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                singleLine = true,
                label = { Text("Search volunteer") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_volunteer_search),
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = VolunteerLinkTextSecondary
                    )
                }
            )

            Text(
                text = "${completed.size - withoutFeedback.size} of ${completed.size} completed volunteers have feedback",
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 13.sp,
                color = VolunteerLinkTextSecondary
            )

            if (!composerOpen) {
                if (groups.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        groups.forEachIndexed { index, group ->
                            val visibleRecipients = group.value.filter { it in filtered }
                            if (visibleRecipients.isNotEmpty()) {
                                RemoteFeedbackGroupRow(
                                    feedback = group.key,
                                    recipients = visibleRecipients,
                                    onEdit = {
                                        composerText = group.key
                                        selectedKeys = group.value.map { remoteReviewParticipationKey(it.roleTemplateId, it.userId) }.toSet()
                                        editingOriginalKeys = selectedKeys
                                        composerOpen = true
                                    },
                                    onViewProfile = onViewProfile
                                )
                                if (index != groups.lastIndex) OrganisationDivider()
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        composerText = ""
                        selectedKeys = withoutFeedback.map { remoteReviewParticipationKey(it.roleTemplateId, it.userId) }.toSet()
                        editingOriginalKeys = emptySet()
                        composerOpen = true
                    },
                    enabled = !busy && withoutFeedback.isNotEmpty(),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(50.dp),
                    border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen)
                ) {
                    Text(
                        text = if (groups.isEmpty()) "Add Feedback" else "Add Another Feedback",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            } else {
                OrganisationDivider(modifier = Modifier.padding(vertical = 14.dp))
                OrganisationSectionHeader(
                    title = if (editingOriginalKeys.isEmpty()) "New feedback" else "Edit feedback",
                    subtitle = "Choose recipients, then write the message once."
                )

                if (editingOriginalKeys.isEmpty() && withoutFeedback.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            selectedKeys = withoutFeedback.map { remoteReviewParticipationKey(it.roleTemplateId, it.userId) }.toSet()
                        }
                    ) {
                        Text("Select all without feedback · ${withoutFeedback.size}", color = VolunteerLinkPrimaryGreen, fontWeight = FontWeight.SemiBold)
                    }
                }

                val composerCandidates = filtered
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    composerCandidates.forEach { person ->
                        val key = remoteReviewParticipationKey(person.roleTemplateId, person.userId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                                }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = key in selectedKeys,
                                onCheckedChange = {
                                    selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                                }
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(person.fullName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = VolunteerLinkTextPrimary)
                                Text(person.roleName, modifier = Modifier.padding(top = 2.dp), fontSize = 12.sp, color = VolunteerLinkTextSecondary)
                            }
                        }
                    }
                }

                Text(
                    text = "${selectedKeys.size} volunteer${if (selectedKeys.size == 1) "" else "s"} selected",
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedKeys.isEmpty()) VolunteerLinkTextSecondary else VolunteerLinkPrimaryGreen
                )

                OutlinedTextField(
                    value = composerText,
                    onValueChange = { composerText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
                    label = { Text("Feedback") },
                    minLines = 3,
                    maxLines = 6,
                    enabled = !busy
                )

                OutlinedButton(
                    onClick = { resetComposer() },
                    enabled = !busy,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                OrganisationPrimaryButton(
                    text = if (editingOriginalKeys.isEmpty()) "Apply Feedback" else "Save Feedback Changes",
                    enabled = !busy && selectedKeys.isNotEmpty() && composerText.isNotBlank(),
                    onClick = {
                        val selectedPeople = completed.filter {
                            remoteReviewParticipationKey(it.roleTemplateId, it.userId) in selectedKeys
                        }
                        selectedPeople.forEach { onFeedbackChange(it, composerText.trim()) }
                        if (editingOriginalKeys.isNotEmpty()) {
                            val removed = editingOriginalKeys - selectedKeys
                            completed.filter {
                                remoteReviewParticipationKey(it.roleTemplateId, it.userId) in removed
                            }.forEach { onFeedbackChange(it, "") }
                        }
                        resetComposer()
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        OrganisationDivider(modifier = Modifier.padding(top = 18.dp))
        OutlinedButton(
            onClick = onBack,
            enabled = !busy && !composerOpen,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Back to Submission", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        OrganisationPrimaryButton(
            text = "Continue to Finish",
            onClick = onContinue,
            enabled = !busy && !composerOpen,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
/**
 * Renders the remote feedback group row row used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun RemoteFeedbackGroupRow(
    feedback: String,
    recipients: List<PostManagementPerson>,
    onEdit: () -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feedback,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = recipients.joinToString(" · ") { it.fullName },
                    modifier = if (recipients.size == 1) {
                        Modifier
                            .padding(top = 5.dp)
                            .clickable { onViewProfile(recipients.first()) }
                    } else {
                        Modifier.padding(top = 5.dp)
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = if (recipients.size == 1) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (recipients.size == 1) VolunteerLinkPrimaryGreen else VolunteerLinkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onEdit) { Text("Edit", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
/**
 * Renders the remote filter chip chip used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun RemoteFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) VolunteerLinkPrimaryGreen else VolunteerLinkSurface,
        border = BorderStroke(1.dp, if (selected) VolunteerLinkPrimaryGreen else VolunteerLinkBorderColour)
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
/**
 * Renders the UI represented by remote finish review stage for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteFinishReviewStage(
    review: PostManagementRemoteReview,
    session: PostManagementRemoteReviewSession,
    evaluations: List<PostManagementEvaluation>,
    busy: Boolean,
    onBack: () -> Unit,
    onFinalize: () -> Unit
) {
    RemoteReviewStageSurface(
        title = if (review.canEdit) "Finish" else "Final review",
        subtitle = if (review.canEdit) "Check the final Remote outcomes before closing the project." else "These outcomes are finalized and read-only."
    ) {
        if (review.participants.isEmpty()) {
            Text(
                text = "No Remote participants remain to summarize.",
                fontSize = 14.sp,
                color = VolunteerLinkTextSecondary
            )
        } else {
            Column(modifier = Modifier.padding(top = 2.dp)) {
                review.participants.forEachIndexed { index, person ->
                    val evaluation = evaluations.firstOrNull {
                        it.roleTemplateId == person.roleTemplateId && it.userId == person.userId
                    }
                    val key = remoteReviewParticipationKey(person.roleTemplateId, person.userId)
                    val reason = evaluation?.completionReason ?: person.decisionNote
                    val feedback = session.feedbackByParticipation[key] ?: evaluation?.feedback

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(person.fullName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = VolunteerLinkTextPrimary)
                            Text(person.roleName, modifier = Modifier.padding(top = 3.dp), fontSize = 12.sp, color = VolunteerLinkTextSecondary)
                            if (!reason.isNullOrBlank()) {
                                Text("Reason · $reason", modifier = Modifier.padding(top = 6.dp), fontSize = 12.sp, lineHeight = 17.sp, color = VolunteerLinkTextSecondary)
                            }
                            if (!feedback.isNullOrBlank()) {
                                Text("Feedback · $feedback", modifier = Modifier.padding(top = 5.dp), fontSize = 12.sp, lineHeight = 17.sp, color = VolunteerLinkTextPrimary)
                            }
                        }
                        RemoteSubmissionStatusPill(person.completionStatus)
                    }
                    if (index != review.participants.lastIndex) OrganisationDivider()
                }
            }
        }

        if (review.canEdit) {
            OrganisationDivider(modifier = Modifier.padding(top = 12.dp))
            OutlinedButton(
                onClick = onBack,
                enabled = !busy,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Back to Feedback", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            OrganisationPrimaryButton(
                text = if (busy) "Finalizing..." else "Finalize Project",
                onClick = onFinalize,
                enabled = !busy,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
/**
 * Renders the UI represented by remote review stage surface for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteReviewStageSurface(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    OrganisationSectionSurface(contentPadding = 16.dp) {
        OrganisationSectionHeader(title = title, subtitle = subtitle)
        Column(modifier = Modifier.padding(top = 12.dp), content = content)
    }
}

@Composable
/**
 * Renders the UI represented by remote draft decision notice for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteDraftDecisionNotice(
    title: String,
    detail: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VolunteerLinkSoftGreenSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkPrimaryGreen)
        if (!detail.isNullOrBlank()) {
            Text(detail, modifier = Modifier.padding(top = 3.dp), fontSize = 12.sp, lineHeight = 17.sp, color = VolunteerLinkTextSecondary)
        }
    }
}

@Composable
/**
 * Renders the UI represented by remote review message strip for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteReviewMessageStrip(message: String) {
    OrganisationInfoStrip(
        title = "Review update",
        message = message,
        accent = if (message.contains("success", true) || message.contains("saved", true)) VolunteerLinkPrimaryGreen else VolunteerLinkWarning
    )
}

@Composable
/**
 * Renders the UI represented by remote submission status pill for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteSubmissionStatusPill(status: String) {
    val normalized = status.uppercase(Locale.US)
    val (label, foreground) = when (normalized) {
        "PENDING_REVIEW" -> "Pending Review" to VolunteerLinkInformation
        "REVISION_REQUESTED" -> "Revision Requested" to VolunteerLinkWarning
        "ACCEPTED", "WORK_ACCEPTED", "COMPLETED" -> when (normalized) {
            "WORK_ACCEPTED" -> "Work Accepted"
            "COMPLETED" -> "Completed"
            else -> "Accepted"
        } to VolunteerLinkPrimaryGreen
        "NOT_ACCEPTED", "WORK_NOT_ACCEPTED", "NOT_COMPLETED" -> when (normalized) {
            "WORK_NOT_ACCEPTED" -> "Work Not Accepted"
            "NOT_COMPLETED" -> "Not Completed"
            else -> "Not Accepted"
        } to VolunteerLinkError
        "NOT_SUBMITTED" -> "No Submission" to VolunteerLinkWarning
        else -> normalized.replace('_', ' ').lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) } to VolunteerLinkTextSecondary
    }
    OrganisationStatusPill(text = label, color = foreground)
}

@Composable
/**
 * Returns the remote review dialog title used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteReviewDialogTitle(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
}

/**
 * Parses the remote date used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun parseRemoteDate(value: String): Date? = runCatching {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
}.getOrNull()

/**
 * Formats the remote date used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun formatRemoteDate(value: String): String {
    val date = parseRemoteDate(value) ?: return value
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
}

/**
 * Formats the remote date time used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun formatRemoteDateTime(value: String): String {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    val parsed = patterns.firstNotNullOfOrNull { pattern ->
        runCatching { SimpleDateFormat(pattern, Locale.US).parse(value) }.getOrNull()
    } ?: return value
    return SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault()).format(parsed)
}

/**
 * Adds the remote days to the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun addRemoteDays(value: String, days: Int): String {
    val date = parseRemoteDate(value) ?: return value
    val calendar = Calendar.getInstance().apply {
        time = date
        add(Calendar.DAY_OF_MONTH, days)
    }
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

/**
 * Returns the max remote date value required by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun maxRemoteDate(first: String, second: String): String {
    val a = parseRemoteDate(first) ?: return second
    val b = parseRemoteDate(second) ?: return first
    return if (a.after(b)) first else second
}
