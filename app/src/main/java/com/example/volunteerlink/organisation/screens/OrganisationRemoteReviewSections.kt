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
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingDecisionType
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
    onCompletionDecision: (PostManagementPerson, Boolean, String?) -> Unit,
    onChangeCompletionDecision: (PostManagementPerson) -> Unit,
    onFeedbackChange: (PostManagementPerson, String) -> Unit,
    onStageChange: (PostManagementRemoteReviewStage) -> Unit,
    onFinalize: () -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = if (review.canEdit) session.stage else PostManagementRemoteReviewStage.FINISH
    var notCompletedPerson by remember { mutableStateOf<PostManagementPerson?>(null) }
    var notCompletedReason by remember { mutableStateOf("") }
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

            PostManagementRemoteReviewStage.COMPLETION -> RemoteCompletionReviewStage(
                review = review,
                session = session,
                busy = isSaving,
                onBack = { onStageChange(PostManagementRemoteReviewStage.SUBMISSION) },
                onContinue = { onStageChange(PostManagementRemoteReviewStage.FEEDBACK) },
                onCompleted = { person -> onCompletionDecision(person, true, null) },
                onNotCompleted = { person ->
                    notCompletedReason = ""
                    notCompletedPerson = person
                },
                onChangeDecision = onChangeCompletionDecision,
                onViewProfile = onViewProfile
            )

            PostManagementRemoteReviewStage.FEEDBACK -> RemoteFeedbackReviewStage(
                review = review,
                session = session,
                busy = isSaving,
                onFeedbackChange = onFeedbackChange,
                onBack = { onStageChange(PostManagementRemoteReviewStage.COMPLETION) },
                onContinue = { onStageChange(PostManagementRemoteReviewStage.FINISH) }
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

    notCompletedPerson?.let { person ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) notCompletedPerson = null },
            title = { RemoteReviewDialogTitle("Mark Not Completed") },
            text = {
                Column {
                    Text(
                        text = "Give a clear reason for ${person.fullName}. This reason becomes part of the final Remote evaluation.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = VolunteerLinkTextSecondary
                    )
                    OutlinedTextField(
                        value = notCompletedReason,
                        onValueChange = { notCompletedReason = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        label = { Text("Reason") },
                        placeholder = { Text("Why was the participation not completed?") },
                        minLines = 3,
                        maxLines = 5,
                        enabled = !isSaving
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSaving && notCompletedReason.isNotBlank(),
                    onClick = {
                        onCompletionDecision(person, false, notCompletedReason.trim())
                        notCompletedPerson = null
                        notCompletedReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkWarning)
                ) {
                    Text("Save Decision", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !isSaving, onClick = { notCompletedPerson = null }) {
                    Text("Cancel")
                }
            },
            containerColor = VolunteerLinkSurface
        )
    }

    if (confirmFinalize) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) confirmFinalize = false },
            title = { RemoteReviewDialogTitle("Finalize Remote project?") },
            text = {
                Text(
                    text = "This is the final save. Completion decisions and final feedback will be committed, certificates and verified skills will be issued only for Completed volunteers, and the project becomes read-only.",
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
                "Review one stage at a time. Submission decisions are settled before volunteer completion."
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
    val extensionConflict = extensionRequired && review.items.any {
        session.missingActionFor(it.itemKey) == PostManagementRemoteMissingAction.CONTINUE_WITHOUT_WORK
    }
    val deadlineReady = !extensionRequired || !session.newEndDate.isNullOrBlank()

    RemoteReviewStageSurface(
        title = "Submission Review",
        subtitle = "The project deadline has passed. Resolve every deliverable before deciding volunteer completion."
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
                    text = "If any unresolved work needs more time, set one new project deadline below. The same date applies to every unresolved Individual or Shared deliverable.",
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
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
                conflict = extensionConflict,
                enabled = !busy,
                onSelected = onNewEndDateChange,
                modifier = Modifier.padding(top = 14.dp)
            )
        }

        if (!allResolved) {
            Text(
                text = "Review every unresolved deliverable before continuing.",
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        Button(
            onClick = onContinue,
            enabled = !busy && allResolved && deadlineReady && !extensionConflict,
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
                        PostManagementRemoteSubmissionDecisionType.ACCEPT -> "Draft decision · Accept"
                        PostManagementRemoteSubmissionDecisionType.REQUEST_REVISION -> "Draft decision · Request Revision"
                        PostManagementRemoteSubmissionDecisionType.NOT_ACCEPT -> "Draft decision · Not Accept"
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
                        busy = busy,
                        onSelected = onMissingAction,
                        modifier = Modifier.padding(top = 9.dp)
                    )
                }

                "NOT_SUBMITTED" -> RemoteMissingWorkActions(
                    selected = missingAction,
                    revisionWasRequested = false,
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
    }
}

@Composable
private fun RemoteDeadlineExtensionPanel(
    review: PostManagementRemoteReview,
    selectedDate: String?,
    conflict: Boolean,
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
                text = "Choose one new project deadline. It applies to everyone whose Remote work is still unresolved; already accepted work stays accepted.",
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

            if (conflict) {
                Text(
                    text = "Because the extension is project-wide, unresolved work cannot also be marked Continue Without Submission. Give those volunteers more time instead.",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                    color = VolunteerLinkError
                )
            }
        }
    }
}

@Composable
private fun RemoteCompletionReviewStage(
    review: PostManagementRemoteReview,
    session: PostManagementRemoteReviewSession,
    busy: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onCompleted: (PostManagementPerson) -> Unit,
    onNotCompleted: (PostManagementPerson) -> Unit,
    onChangeDecision: (PostManagementPerson) -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit
) {
    val allDecided = review.participants.all {
        session.completionDecisionFor(it.roleTemplateId, it.userId) != null
    }

    RemoteReviewStageSurface(
        title = "Completion",
        subtitle = "Accepted work makes a volunteer eligible for Completed. It does not complete them automatically."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            review.participants.forEach { person ->
                val decision = session.completionDecisionFor(person.roleTemplateId, person.userId)
                val eligible = review.canComplete(person)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = VolunteerLinkBackground,
                    border = BorderStroke(1.dp, VolunteerLinkBorderColour)
                ) {
                    Column(modifier = Modifier.padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(person.fullName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
                                Text(person.roleName, modifier = Modifier.padding(top = 2.dp), fontSize = 10.sp, color = VolunteerLinkTextSecondary)
                            }
                            RemoteSubmissionStatusPill(
                                if (eligible) "WORK_ACCEPTED" else "WORK_NOT_ACCEPTED"
                            )
                        }

                        if (decision == null) {
                            Text(
                                text = if (eligible) {
                                    "Accepted work allows either Completed or Not Completed."
                                } else {
                                    "Without accepted work, this volunteer can only be marked Not Completed."
                                },
                                modifier = Modifier.padding(top = 9.dp),
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = VolunteerLinkTextSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 9.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (eligible) {
                                    Button(
                                        onClick = { onCompleted(person) },
                                        enabled = !busy,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                                    ) {
                                        Text("Completed", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onNotCompleted(person) },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, VolunteerLinkWarning)
                                ) {
                                    Text("Not Completed", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkWarning)
                                }
                            }
                        } else {
                            RemoteDraftDecisionNotice(
                                title = if (decision.decision == PostManagementPendingDecisionType.COMPLETED) {
                                    "Selected · Completed"
                                } else {
                                    "Selected · Not Completed"
                                },
                                detail = decision.reason,
                                modifier = Modifier.padding(top = 9.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onViewProfile(person) },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Profile", fontSize = 9.sp) }
                                TextButton(
                                    onClick = { onChangeDecision(person) },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Change Decision", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
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
                enabled = !busy && allDecided,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
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
    onContinue: () -> Unit
) {
    val completed = review.participants.filter { person ->
        session.completionDecisionFor(person.roleTemplateId, person.userId)?.decision ==
            PostManagementPendingDecisionType.COMPLETED
    }

    RemoteReviewStageSurface(
        title = "Feedback",
        subtitle = "Final feedback is optional and only applies to volunteers marked Completed."
    ) {
        if (completed.isEmpty()) {
            Text(
                text = "No Completed volunteers require final feedback.",
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
                            Text(person.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
                            Text(person.roleName, modifier = Modifier.padding(top = 2.dp), fontSize = 9.sp, color = VolunteerLinkTextSecondary)
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
            "Check every outcome before the final database save."
        } else {
            "These Remote completion outcomes are finalized and read-only."
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            review.participants.forEach { person ->
                val draft = session.completionDecisionFor(person.roleTemplateId, person.userId)
                val evaluation = evaluations.firstOrNull {
                    it.roleTemplateId == person.roleTemplateId && it.userId == person.userId
                }
                val finalDecision = draft?.decision?.name ?: person.completionStatus
                val key = remoteReviewParticipationKey(person.roleTemplateId, person.userId)
                val reason = draft?.reason ?: evaluation?.completionReason
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
