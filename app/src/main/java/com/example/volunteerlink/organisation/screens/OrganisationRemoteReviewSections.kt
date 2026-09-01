package com.example.volunteerlink.organisation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.organisation.manage.model.PostManagementEvaluation
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteMissingAction
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReview
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewItem
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewSession
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewStage
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmissionDecisionType
import com.example.volunteerlink.organisation.manage.model.remoteReviewParticipationKey
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
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

private val RemoteReviewCardShape = RoundedCornerShape(16.dp)
private val RemoteReviewPillShape = RoundedCornerShape(50)

@Composable
internal fun PostManagementRemoteReviewContent(
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    text = "This is the final save. Submission Review has already settled every volunteer outcome. Optional feedback will be saved, certificates and verified skills will be issued only for Completed volunteers, and the project becomes read-only.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
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
private fun RemoteReviewFlowHeader(
    stage: PostManagementRemoteReviewStage,
    finalized: Boolean
) {
    val stages = PostManagementRemoteReviewStage.entries
    val activeIndex = stages.indexOf(stage)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (finalized) "Remote Review Complete" else "Remote Review",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )
        Text(
            text = if (finalized) {
                "This Remote review is finalized and read-only."
            } else {
                "Review submissions first. Accepted work becomes Completed automatically before Feedback and Finish."
            },
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = VolunteerLinkTextSecondary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stages.forEachIndexed { index, item ->
                val completed = finalized || index < activeIndex
                val active = !finalized && index == activeIndex
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(29.dp),
                        shape = CircleShape,
                        color = when {
                            completed -> VolunteerLinkPrimaryGreen
                            active -> VolunteerLinkSoftGreenSurface
                            else -> VolunteerLinkSurface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (completed || active) VolunteerLinkPrimaryGreen else VolunteerLinkBorderColour
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (completed) "✓" else (index + 1).toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    completed -> VolunteerLinkSurface
                                    active -> VolunteerLinkPrimaryGreen
                                    else -> VolunteerLinkTextSecondary
                                }
                            )
                        }
                    }
                    Text(
                        text = item.name.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) },
                        modifier = Modifier.padding(top = 5.dp),
                        fontSize = 11.sp,
                        fontWeight = if (active || completed) FontWeight.Bold else FontWeight.Medium,
                        color = if (active || completed) VolunteerLinkPrimaryGreen else VolunteerLinkTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
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
        title = "Submission Review",
        subtitle = "The project deadline has passed. Resolve every deliverable. Accepted work becomes Completed automatically."
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            color = VolunteerLinkWarning.copy(alpha = 0.07f),
            border = BorderStroke(1.dp, VolunteerLinkWarning.copy(alpha = 0.24f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Project deadline passed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkWarning
                )
                Text(
                    text = "Current deadline: ${formatRemoteDate(review.currentDeadline)}",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 11.sp,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = if (review.submissionMode.equals("SHARED_TEAM", ignoreCase = true)) {
                        "Shared Team has one deliverable. Accept completes the Remote team automatically; Not Accept or Continue Without Submission makes the unresolved Remote team Not Completed. Request Revision or Give More Time extends the whole team deadline."
                    } else {
                        "Individual decisions can be mixed. Accept completes that volunteer automatically. Not Accept or Continue Without Submission makes only that Remote participation Not Completed. Give More Time or Request Revision keeps work open under a new deadline."
                    },
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }

        if (review.items.isEmpty()) {
            Text(
                text = "All Remote volunteer outcomes are already resolved. Continue to optional feedback.",
                modifier = Modifier.padding(top = 14.dp),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            review.items.forEach { item ->
                RemoteSubmissionReviewItemCard(
                    item = item,
                    session = session,
                    busy = busy,
                    onViewSubmission = { onViewSubmission(item) },
                    onMissingAction = { giveMoreTime ->
                        onMissingAction(item.itemKey, giveMoreTime)
                    }
                )
            }
        }

        if (extensionRequired) {
            RemoteDeadlineExtensionPanel(
                review = review,
                selectedDate = session.newEndDate,
                enabled = !busy,
                onSelected = onNewEndDateChange,
                modifier = Modifier.padding(top = 14.dp)
            )
        }

        if (!allResolved) {
            Text(
                text = "Review every unresolved deliverable before continuing. You can press the save button to see which item is still missing a decision.",
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        Button(
            onClick = onContinue,
            // Keep the action usable. The ViewModel performs the authoritative
            // validation and explains the exact unresolved item/date instead of
            // leaving the organisation with an unexplained disabled button.
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
        ) {
            Text(
                text = when {
                    busy -> "Saving..."
                    extensionRequired -> "Save & Extend Deadline"
                    else -> "Save & Continue"
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = VolunteerLinkBackground,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (item.isShared) "Team Submission" else item.person?.fullName.orEmpty(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = if (item.isShared) {
                            "Shared Team · ${item.roleName}"
                        } else {
                            item.roleName
                        },
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 10.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
                RemoteSubmissionStatusPill(status)
            }

            if (item.isResubmission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = VolunteerLinkPrimaryGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen.copy(alpha = 0.22f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = "Revised submission received",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkPrimaryGreen
                        )
                        Text(
                            text = if (item.isShared) {
                                "Latest team version is ready for review."
                            } else {
                                "Latest version is ready for review."
                            },
                            modifier = Modifier.padding(top = 2.dp),
                            fontSize = 9.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }
            }

            item.requirement?.takeIf { it.isNotBlank() }?.let { requirement ->
                Text(
                    text = requirement,
                    modifier = Modifier.padding(top = 9.dp),
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = VolunteerLinkTextPrimary
                )
            }

            if (submission != null) {
                val fileName = submission.filePath?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                Text(
                    text = fileName ?: if (!submission.submissionUrl.isNullOrBlank()) {
                        "Submission link provided"
                    } else {
                        "Submission received"
                    },
                    modifier = Modifier.padding(top = 9.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                submission.submittedAt?.takeIf { it.isNotBlank() }?.let { submittedAt ->
                    Text(
                        text = "Submitted ${formatRemoteDateTime(submittedAt)}",
                        modifier = Modifier.padding(top = 3.dp),
                        fontSize = 9.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
                if (item.isShared && !item.submittedByName.isNullOrBlank()) {
                    Text(
                        text = "Submitted by ${item.submittedByName}",
                        modifier = Modifier.padding(top = 3.dp),
                        fontSize = 9.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                OutlinedButton(
                    onClick = onViewSubmission,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp)
                ) {
                    Text("View Submission", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (draft != null) {
                RemoteDraftDecisionNotice(
                    title = when (draft.decision) {
                        PostManagementRemoteSubmissionDecisionType.ACCEPT -> "Draft decision · Accept → Completed"
                        PostManagementRemoteSubmissionDecisionType.REQUEST_REVISION -> "Draft decision · Request Revision"
                        PostManagementRemoteSubmissionDecisionType.NOT_ACCEPT -> "Draft decision · Not Accept → Not Completed"
                    },
                    detail = draft.feedback,
                    modifier = Modifier.padding(top = 9.dp)
                )
            } else when (status) {
                "PENDING_REVIEW" -> Text(
                    text = "Open the submission to choose Accept, Request Revision, or Not Accept.",
                    modifier = Modifier.padding(top = 9.dp),
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                    color = VolunteerLinkTextSecondary
                )

                "REVISION_REQUESTED" -> {
                    if (!submission?.feedback.isNullOrBlank()) {
                        Text(
                            text = "Previous revision request: ${submission?.feedback}",
                            modifier = Modifier.padding(top = 9.dp),
                            fontSize = 9.sp,
                            lineHeight = 14.sp,
                            color = VolunteerLinkWarning
                        )
                    }
                    RemoteMissingWorkActions(
                        selected = missingAction,
                        revisionWasRequested = true,
                        sharedTeam = item.isShared,
                        busy = busy,
                        onSelected = onMissingAction,
                        modifier = Modifier.padding(top = 9.dp)
                    )
                }

                "NOT_SUBMITTED" -> RemoteMissingWorkActions(
                    selected = missingAction,
                    revisionWasRequested = false,
                    sharedTeam = item.isShared,
                    busy = busy,
                    onSelected = onMissingAction,
                    modifier = Modifier.padding(top = 9.dp)
                )
            }
        }
    }
}

@Composable
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
            text = if (revisionWasRequested) {
                "No revised version was received by the deadline."
            } else {
                "No submission was received by the deadline."
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = VolunteerLinkWarning
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onSelected(true) },
                enabled = !busy,
                modifier = Modifier.weight(1f),
                border = BorderStroke(
                    1.dp,
                    if (selected == PostManagementRemoteMissingAction.GIVE_MORE_TIME) {
                        VolunteerLinkPrimaryGreen
                    } else {
                        VolunteerLinkBorderColour
                    }
                )
            ) {
                Text("Give More Time", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onSelected(false) },
                enabled = !busy,
                modifier = Modifier.weight(1f),
                border = BorderStroke(
                    1.dp,
                    if (selected == PostManagementRemoteMissingAction.CONTINUE_WITHOUT_WORK) {
                        VolunteerLinkWarning
                    } else {
                        VolunteerLinkBorderColour
                    }
                )
            ) {
                Text(
                    if (revisionWasRequested) "Continue Without Revision" else "Continue Without Submission",
                    fontSize = 8.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (selected == PostManagementRemoteMissingAction.CONTINUE_WITHOUT_WORK) {
            Text(
                text = when {
                    sharedTeam && revisionWasRequested ->
                        "Saving this will mark every unresolved Remote volunteer Not Completed because the requested shared revision was not received."
                    sharedTeam ->
                        "Saving this will mark every unresolved Remote volunteer Not Completed because the shared submission was not received."
                    revisionWasRequested ->
                        "Saving this will mark this Remote participation Not Completed because the requested revision was not received."
                    else ->
                        "Saving this will mark this Remote participation Not Completed because no submission was received."
                },
                modifier = Modifier.padding(top = 7.dp),
                fontSize = 9.sp,
                lineHeight = 14.sp,
                color = VolunteerLinkWarning
            )
        }
    }
}

@Composable
private fun RemoteDeadlineExtensionPanel(
    review: PostManagementRemoteReview,
    selectedDate: String?,
    enabled: Boolean,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val minimumDate = remember(review.currentDeadline, review.todayDate) {
        maxRemoteDate(
            addRemoteDays(review.currentDeadline, 1),
            addRemoteDays(review.todayDate, 1)
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = VolunteerLinkPrimaryGreen.copy(alpha = 0.07f),
        border = BorderStroke(1.2.dp, VolunteerLinkPrimaryGreen.copy(alpha = 0.32f))
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text(
                text = "Deadline extension required",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
            Text(
                text = if (review.submissionMode.equals("SHARED_TEAM", ignoreCase = true)) {
                    "Choose one new deadline for the shared team deliverable. The whole team receives this extension."
                } else {
                    "Choose one new project deadline for Individual work kept open through Give More Time or Request Revision. Volunteers finalized as Not Completed do not return to this review."
                },
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = VolunteerLinkTextSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RemoteCompactInfo("Current", formatRemoteDate(review.currentDeadline), Modifier.weight(1f))
                RemoteCompactInfo("Today", formatRemoteDate(review.todayDate), Modifier.weight(1f))
            }

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
                    .fillMaxWidth()
                    .padding(top = 11.dp),
                border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen)
            ) {
                Text(
                    text = selectedDate?.let { "New deadline · ${formatRemoteDate(it)}" }
                        ?: "Select New Project Deadline",
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
            }

        }
    }
}

@Composable
private fun RemoteFeedbackReviewStage(
    review: PostManagementRemoteReview,
    session: PostManagementRemoteReviewSession,
    busy: Boolean,
    onFeedbackChange: (PostManagementPerson, String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit
) {
    val completed = review.participants.filter { person ->
        person.completionStatus.equals("COMPLETED", ignoreCase = true)
    }

    RemoteReviewStageSurface(
        title = "Feedback",
        subtitle = "Final feedback is optional and only applies to volunteers whose Remote work was accepted and completed."
    ) {
        if (completed.isEmpty()) {
            Text(
                text = "There are no Completed volunteers to receive final feedback.",
                fontSize = 11.sp,
                color = VolunteerLinkTextSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                completed.forEach { person ->
                    val key = remoteReviewParticipationKey(person.roleTemplateId, person.userId)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = VolunteerLinkBackground,
                        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(person.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
                                    Text(person.roleName, modifier = Modifier.padding(top = 2.dp), fontSize = 9.sp, color = VolunteerLinkTextSecondary)
                                }
                                RemoteSubmissionStatusPill("COMPLETED")
                            }
                            OutlinedTextField(
                                value = session.feedbackByParticipation[key].orEmpty(),
                                onValueChange = { onFeedbackChange(person, it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 9.dp),
                                label = { Text("Final feedback (optional)") },
                                placeholder = { Text("Share a short comment about the volunteer's work.") },
                                minLines = 2,
                                maxLines = 4,
                                enabled = !busy
                            )
                            TextButton(
                                onClick = { onViewProfile(person) },
                                enabled = !busy,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text("View Profile", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack, enabled = !busy, modifier = Modifier.weight(1f)) {
                Text("Back", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onContinue,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
            ) {
                Text("Review Summary", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RemoteFinishReviewStage(
    review: PostManagementRemoteReview,
    session: PostManagementRemoteReviewSession,
    evaluations: List<PostManagementEvaluation>,
    busy: Boolean,
    onBack: () -> Unit,
    onFinalize: () -> Unit
) {
    RemoteReviewStageSurface(
        title = if (review.canEdit) "Finish" else "Final Review",
        subtitle = if (review.canEdit) {
            "Check the automatic submission outcomes and optional feedback before closing the project."
        } else {
            "These Remote completion outcomes are finalized and read-only."
        }
    ) {
        if (review.participants.isEmpty()) {
            Text(
                text = if (review.canEdit) {
                    "All Remote volunteer outcomes were settled during Submission Review. Finalize the project to close the post."
                } else {
                    "No unresolved Remote volunteer remains."
                },
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        Column(
            modifier = if (review.participants.isEmpty()) Modifier.padding(top = 8.dp) else Modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            review.participants.forEach { person ->
                val evaluation = evaluations.firstOrNull {
                    it.roleTemplateId == person.roleTemplateId && it.userId == person.userId
                }
                val finalDecision = person.completionStatus
                val key = remoteReviewParticipationKey(person.roleTemplateId, person.userId)
                val reason = evaluation?.completionReason ?: person.decisionNote
                val feedback = session.feedbackByParticipation[key] ?: evaluation?.feedback

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    color = VolunteerLinkBackground,
                    border = BorderStroke(1.dp, VolunteerLinkBorderColour)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(person.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
                                Text(person.roleName, modifier = Modifier.padding(top = 2.dp), fontSize = 9.sp, color = VolunteerLinkTextSecondary)
                            }
                            RemoteSubmissionStatusPill(finalDecision)
                        }
                        if (!reason.isNullOrBlank()) {
                            Text(
                                text = "Reason: $reason",
                                modifier = Modifier.padding(top = 8.dp),
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = VolunteerLinkTextPrimary
                            )
                        }
                        if (!feedback.isNullOrBlank()) {
                            Text(
                                text = "Feedback: $feedback",
                                modifier = Modifier.padding(top = 8.dp),
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = VolunteerLinkTextPrimary
                            )
                        }
                    }
                }
            }
        }

        if (review.canEdit) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onBack, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text("Back", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onFinalize,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                ) {
                    Text(if (busy) "Finalizing..." else "Finalize Project", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RemoteReviewStageSurface(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RemoteReviewCardShape,
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
            Text(
                subtitle,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = VolunteerLinkTextSecondary
            )
            Column(modifier = Modifier.padding(top = 14.dp), content = content)
        }
    }
}

@Composable
private fun RemoteDraftDecisionNotice(
    title: String,
    detail: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        color = VolunteerLinkSoftGreenSurface,
        border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen.copy(alpha = 0.24f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkPrimaryGreen)
            if (!detail.isNullOrBlank()) {
                Text(detail, modifier = Modifier.padding(top = 3.dp), fontSize = 9.sp, lineHeight = 13.sp, color = VolunteerLinkTextSecondary)
            }
        }
    }
}

@Composable
private fun RemoteReviewMessageStrip(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = VolunteerLinkWarning.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, VolunteerLinkWarning.copy(alpha = 0.22f))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = VolunteerLinkTextPrimary
        )
    }
}

@Composable
private fun RemoteSubmissionStatusPill(status: String) {
    val normalized = status.uppercase(Locale.US)
    val (label, foreground) = when (normalized) {
        "PENDING_REVIEW" -> "Pending Review" to VolunteerLinkWarning
        "REVISION_REQUESTED" -> "Revision Requested" to VolunteerLinkWarning
        "ACCEPTED", "WORK_ACCEPTED", "COMPLETED" -> (
            if (normalized == "WORK_ACCEPTED") {
                "Work Accepted"
            } else {
                normalized.replace('_', ' ').lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
            }
        ) to VolunteerLinkPrimaryGreen
        "NOT_ACCEPTED", "WORK_NOT_ACCEPTED", "NOT_COMPLETED" -> when (normalized) {
            "WORK_NOT_ACCEPTED" -> "Work Not Accepted"
            "NOT_COMPLETED" -> "Not Completed"
            else -> "Not Accepted"
        } to VolunteerLinkError
        "NOT_SUBMITTED" -> "No Submission" to VolunteerLinkWarning
        else -> normalized.replace('_', ' ').lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) } to VolunteerLinkTextSecondary
    }
    Surface(
        shape = RemoteReviewPillShape,
        color = foreground.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, foreground.copy(alpha = 0.20f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = foreground,
            maxLines = 1
        )
    }
}

@Composable
private fun RemoteCompactInfo(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = VolunteerLinkTextSecondary)
        Text(value, modifier = Modifier.padding(top = 3.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
    }
}

@Composable
private fun RemoteReviewDialogTitle(text: String) {
    Text(text, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
}

private fun parseRemoteDate(value: String): Date? = runCatching {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
}.getOrNull()

private fun formatRemoteDate(value: String): String {
    val date = parseRemoteDate(value) ?: return value
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
}

private fun formatRemoteDateTime(value: String): String {
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

private fun addRemoteDays(value: String, days: Int): String {
    val date = parseRemoteDate(value) ?: return value
    val calendar = Calendar.getInstance().apply { time = date; add(Calendar.DAY_OF_MONTH, days) }
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

private fun maxRemoteDate(first: String, second: String): String {
    val a = parseRemoteDate(first) ?: return second
    val b = parseRemoteDate(second) ?: return first
    return if (a.after(b)) first else second
}
