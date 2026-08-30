package com.example.volunteerlink.organisation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingDecisionSource
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingDecisionType
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingReviewDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReview
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReviewEntry
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReviewSession
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReviewStage
import com.example.volunteerlink.organisation.manage.model.PostManagementVolunteerAttendanceDateStatus
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkCardContentPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkCardCornerRadius
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSuccess
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning
import java.text.SimpleDateFormat
import java.util.Locale

private val ReviewStageShape = RoundedCornerShape(VolunteerLinkCardCornerRadius)
private val ReviewActionShape = RoundedCornerShape(10.dp)
private val ReviewPillShape = RoundedCornerShape(50)

/**
 * One consistent four-stage Physical close-out flow.
 *
 * Attendance corrections are persisted immediately. Completion decisions and
 * feedback remain local to the ViewModel until Finalize Event is pressed.
 */
@Composable
internal fun PostManagementPhysicalReviewContent(
    review: PostManagementPhysicalReview,
    session: PostManagementPhysicalReviewSession,
    isUpdatingReview: Boolean,
    isUpdatingAttendance: Boolean,
    reviewActionMessage: String?,
    attendanceActionMessage: String?,
    onCompleteAllReady: () -> Unit,
    onReportIssue: (PostManagementPerson, String) -> Unit,
    onSelectVolunteerDecision: (PostManagementPerson, Boolean, String?) -> Unit,
    onChangeDecision: (PostManagementPerson) -> Unit,
    onSaveFeedback: (List<String>, String, String?) -> Unit,
    onStageChange: (PostManagementPhysicalReviewStage) -> Unit,
    onReviewDraftDirtyChanged: (Boolean) -> Unit,
    onFinalizeReview: () -> Unit,
    onMarkPresent: (PostManagementPerson, String) -> Unit,
    onRequestMarkAbsent: (PostManagementPerson, String) -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit,
    modifier: Modifier = Modifier
) {
    val busy = isUpdatingReview || isUpdatingAttendance
    val stage = if (review.canEdit) session.stage else PostManagementPhysicalReviewStage.FINISH
    val allEntries = (review.ready + review.needsReview + review.completed + review.notCompleted)
        .distinctBy(::reviewKey)
    val baseUnresolved = (review.ready + review.needsReview).distinctBy(::reviewKey)
    val pendingByKey = session.decisions.associateBy(::decisionKey)

    val undecidedReady = review.ready.filter { pendingByKey[reviewKey(it)] == null }
    val undecidedExceptions = review.needsReview.filter { pendingByKey[reviewKey(it)] == null }
    val selectedDecisions = session.decisions.mapNotNull { decision ->
        allEntries.firstOrNull {
            it.person.roleTemplateId == decision.roleTemplateId &&
                it.person.userId == decision.userId
        }?.let { it to decision }
    }
    val selectedFullAttendanceComplete = selectedDecisions.filter { (entry, decision) ->
        decision.decision == PostManagementPendingDecisionType.COMPLETED &&
            decision.source == PostManagementPendingDecisionSource.FULL_ATTENDANCE &&
            entry.absentDays == 0
    }
    val selectedIndividualComplete = selectedDecisions.filter { (entry, decision) ->
        decision.decision == PostManagementPendingDecisionType.COMPLETED &&
            (decision.source != PostManagementPendingDecisionSource.FULL_ATTENDANCE || entry.absentDays > 0)
    }
    val selectedNotCompleted = selectedDecisions.filter { (_, decision) ->
        decision.decision == PostManagementPendingDecisionType.NOT_COMPLETED
    }
    val allDecided = baseUnresolved.all { pendingByKey[reviewKey(it)] != null }

    var confirmFinalize by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ReviewFlowHeader(stage = stage, finalized = !review.canEdit)

        if (!reviewActionMessage.isNullOrBlank()) ReviewMessageStrip(reviewActionMessage)
        if (!attendanceActionMessage.isNullOrBlank()) ReviewMessageStrip(attendanceActionMessage)

        when (stage) {
            PostManagementPhysicalReviewStage.ATTENDANCE -> AttendanceReviewStage(
                entries = baseUnresolved,
                busy = busy,
                onContinue = { onStageChange(PostManagementPhysicalReviewStage.COMPLETION) },
                onMarkPresent = onMarkPresent,
                onRequestMarkAbsent = onRequestMarkAbsent,
                onViewProfile = onViewProfile
            )

            PostManagementPhysicalReviewStage.COMPLETION -> CompletionReviewStage(
                ready = undecidedReady,
                decisionExceptions = undecidedExceptions,
                selectedFullAttendanceComplete = selectedFullAttendanceComplete,
                selectedIndividualComplete = selectedIndividualComplete,
                selectedNotCompleted = selectedNotCompleted,
                allReadyForWorkIssue = review.ready,
                allUnresolved = baseUnresolved,
                allDecided = allDecided,
                hasDraftInput = session.touched,
                busy = busy,
                onCompleteReadyAndContinue = {
                    onCompleteAllReady()
                    onStageChange(PostManagementPhysicalReviewStage.FEEDBACK)
                },
                onBack = { onStageChange(PostManagementPhysicalReviewStage.ATTENDANCE) },
                onContinue = { onStageChange(PostManagementPhysicalReviewStage.FEEDBACK) },
                onReportIssue = onReportIssue,
                onSelectVolunteerDecision = onSelectVolunteerDecision,
                onChangeDecision = onChangeDecision,
                onReviewDraftDirtyChanged = onReviewDraftDirtyChanged,
                onMarkPresent = onMarkPresent,
                onRequestMarkAbsent = onRequestMarkAbsent,
                onViewProfile = onViewProfile
            )

            PostManagementPhysicalReviewStage.FEEDBACK -> FeedbackReviewStage(
                review = review,
                session = session,
                allEntries = allEntries,
                busy = busy,
                onSaveFeedback = onSaveFeedback,
                onReviewDraftDirtyChanged = onReviewDraftDirtyChanged,
                onBack = { onStageChange(PostManagementPhysicalReviewStage.COMPLETION) },
                onContinue = { onStageChange(PostManagementPhysicalReviewStage.FINISH) }
            )

            PostManagementPhysicalReviewStage.FINISH -> FinishReviewStage(
                review = review,
                session = session,
                allEntries = allEntries,
                busy = busy,
                onBack = { onStageChange(PostManagementPhysicalReviewStage.FEEDBACK) },
                onFinalize = { confirmFinalize = true }
            )
        }
    }

    if (confirmFinalize) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmFinalize = false },
            title = { ReviewDialogTitle("Finalize event review?") },
            text = {
                Text(
                    text = "This is the final save. Completion decisions, verified hours and feedback will be committed, and the event review becomes read-only.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextSecondary
                )
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        confirmFinalize = false
                        onFinalizeReview()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                ) {
                    Text("Finalize Event", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { confirmFinalize = false }) {
                    Text("Cancel")
                }
            },
            containerColor = VolunteerLinkSurface
        )
    }
}

@Composable
private fun ReviewDialogTitle(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = VolunteerLinkTextPrimary
    )
}

/** Flat flow header shared by every stage. It is deliberately not another card. */
@Composable
private fun ReviewFlowHeader(
    stage: PostManagementPhysicalReviewStage,
    finalized: Boolean
) {
    val stages = PostManagementPhysicalReviewStage.entries
    val activeIndex = stages.indexOf(stage)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (finalized) "Event Review Complete" else "Event Review",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )
        Text(
            text = if (finalized) {
                "This review is finalized and read-only."
            } else {
                "Review one stage at a time. You can go back before finalizing."
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
                ReviewStageStep(
                    number = index + 1,
                    label = item.name.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) },
                    completed = finalized || index < activeIndex,
                    active = !finalized && index == activeIndex,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReviewStageStep(
    number: Int,
    label: String,
    completed: Boolean,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val background = when {
        completed -> VolunteerLinkPrimaryGreen
        active -> VolunteerLinkSoftGreenSurface
        else -> VolunteerLinkSurface
    }
    val border = when {
        completed || active -> VolunteerLinkPrimaryGreen
        else -> VolunteerLinkBorderColour
    }
    val foreground = when {
        completed -> VolunteerLinkSurface
        active -> VolunteerLinkPrimaryGreen
        else -> VolunteerLinkTextSecondary
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(29.dp),
            shape = CircleShape,
            color = background,
            border = BorderStroke(1.dp, border)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) {
                    Icon(
                        painter = painterResource(R.drawable.tick),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = foreground
                    )
                } else {
                    Text(
                        text = number.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = foreground
                    )
                }
            }
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 5.dp),
            fontSize = 12.sp,
            fontWeight = if (active || completed) FontWeight.Bold else FontWeight.Medium,
            color = if (active || completed) VolunteerLinkPrimaryGreen else VolunteerLinkTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** One white container per active stage. Sections inside are kept flat. */
@Composable
private fun ReviewStageSurface(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ReviewStageShape,
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(VolunteerLinkCardContentPadding)) {
            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = VolunteerLinkTextSecondary
            )
            Column(modifier = Modifier.padding(top = 14.dp), content = content)
        }
    }
}

@Composable
private fun ReviewSectionHeading(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
private fun ReviewSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 14.dp),
        color = VolunteerLinkBorderColour
    )
}

@Composable
private fun ReviewStageFooter(
    backLabel: String?,
    backEnabled: Boolean = true,
    primaryLabel: String,
    primaryEnabled: Boolean,
    onBack: (() -> Unit)?,
    onPrimary: () -> Unit,
    helperText: String? = null,
    primaryColor: Color = VolunteerLinkPrimaryGreen
) {
    ReviewSectionDivider()

    if (!helperText.isNullOrBlank()) {
        Text(
            text = helperText,
            modifier = Modifier.padding(bottom = 10.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = VolunteerLinkTextSecondary
        )
    }

    if (backLabel != null && onBack != null) {
        OutlinedButton(
            enabled = backEnabled,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VolunteerLinkPrimaryGreen)
        ) {
            Text(backLabel, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    Button(
        enabled = primaryEnabled,
        onClick = onPrimary,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
    ) {
        Text(primaryLabel, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AttendanceReviewStage(
    entries: List<PostManagementPhysicalReviewEntry>,
    busy: Boolean,
    onContinue: () -> Unit,
    onMarkPresent: (PostManagementPerson, String) -> Unit,
    onRequestMarkAbsent: (PostManagementPerson, String) -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit
) {
    val missingEntries = entries.filter { it.missingCheckInDays > 0 }
    val missingCount = missingEntries.sumOf { it.missingCheckInDays }

    ReviewStageSurface(
        title = "Attendance",
        subtitle = "Check missing attendance only when something looks wrong. Missing past check-ins are treated as Absent · 0h automatically."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReviewStatusPill(
                text = if (missingCount == 0) "Attendance ready" else "$missingCount missing check-in${if (missingCount == 1) "" else "s"}",
                color = if (missingCount == 0) VolunteerLinkSuccess else VolunteerLinkWarning
            )
        }

        Text(
            text = if (missingCount == 0) {
                "No missing check-ins need your attention. You can still review attendance if you want to correct somebody."
            } else {
                "These volunteers are already treated as Absent. Only change them to Present if the organisation forgot to record someone who actually attended."
            },
            modifier = Modifier.padding(top = 9.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = VolunteerLinkTextSecondary
        )

        if (missingEntries.isNotEmpty()) {
            ReviewSectionDivider()
            AttendanceBrowser(
                title = "Review missing check-ins",
                subtitle = "Optional correction",
                entries = missingEntries,
                busy = busy,
                onMarkPresent = onMarkPresent,
                onRequestMarkAbsent = onRequestMarkAbsent,
                onViewProfile = onViewProfile
            )
        }

        ReviewSectionDivider()
        AttendanceBrowser(
            title = "Review all attendance",
            subtitle = "Find someone by role or name",
            entries = entries,
            busy = busy,
            onMarkPresent = onMarkPresent,
            onRequestMarkAbsent = onRequestMarkAbsent,
            onViewProfile = onViewProfile
        )

        ReviewStageFooter(
            backLabel = null,
            primaryLabel = "Continue to Completion",
            primaryEnabled = !busy,
            onBack = null,
            onPrimary = onContinue,
            helperText = "Attendance corrections save immediately."
        )
    }
}

@Composable
private fun CompletionReviewStage(
    ready: List<PostManagementPhysicalReviewEntry>,
    decisionExceptions: List<PostManagementPhysicalReviewEntry>,
    selectedFullAttendanceComplete: List<Pair<PostManagementPhysicalReviewEntry, PostManagementPendingReviewDecision>>,
    selectedIndividualComplete: List<Pair<PostManagementPhysicalReviewEntry, PostManagementPendingReviewDecision>>,
    selectedNotCompleted: List<Pair<PostManagementPhysicalReviewEntry, PostManagementPendingReviewDecision>>,
    allReadyForWorkIssue: List<PostManagementPhysicalReviewEntry>,
    allUnresolved: List<PostManagementPhysicalReviewEntry>,
    allDecided: Boolean,
    hasDraftInput: Boolean,
    busy: Boolean,
    onCompleteReadyAndContinue: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onReportIssue: (PostManagementPerson, String) -> Unit,
    onSelectVolunteerDecision: (PostManagementPerson, Boolean, String?) -> Unit,
    onChangeDecision: (PostManagementPerson) -> Unit,
    onReviewDraftDirtyChanged: (Boolean) -> Unit,
    onMarkPresent: (PostManagementPerson, String) -> Unit,
    onRequestMarkAbsent: (PostManagementPerson, String) -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit
) {
    val selectedNotCompletedKeys = selectedNotCompleted.map { reviewKey(it.first) }.toSet()
    val issueCandidates = allReadyForWorkIssue.filter { reviewKey(it) !in selectedNotCompletedKeys }
    val selectedCompleteCount = selectedFullAttendanceComplete.size + selectedIndividualComplete.size
    val unresolvedExceptionCount = decisionExceptions.size

    val primaryLabel = when {
        unresolvedExceptionCount > 0 -> "Continue to Feedback"
        ready.isNotEmpty() && selectedCompleteCount == 0 && selectedNotCompleted.isEmpty() ->
            "Complete All ${ready.size} & Continue"
        ready.isNotEmpty() -> "Complete Remaining ${ready.size} & Continue"
        allDecided -> "Continue to Feedback"
        else -> "Continue to Feedback"
    }

    val primaryEnabled = !busy && !hasDraftInput && unresolvedExceptionCount == 0 && (ready.isNotEmpty() || allDecided)
    val helperText = when {
        hasDraftInput -> "Finish or cancel the open decision before changing stages."
        unresolvedExceptionCount > 0 -> "$unresolvedExceptionCount volunteer${if (unresolvedExceptionCount == 1) " still needs" else "s still need"} a completion decision."
        ready.isNotEmpty() -> "${ready.size} full-attendance volunteer${if (ready.size == 1) "" else "s"} will be selected as Complete when you continue."
        else -> "All completion decisions are ready."
    }

    ReviewStageSurface(
        title = "Completion",
        subtitle = "Decide only the exceptions. Volunteers with full attendance and no reported work issue can be completed together at the end of this step."
    ) {
        if (decisionExceptions.isNotEmpty()) {
            ReviewSectionHeading(
                title = "Needs a decision · ${decisionExceptions.size}",
                subtitle = "Incomplete attendance does not automatically mean Not Completed."
            )
            Column(
                modifier = Modifier.padding(top = 9.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                decisionExceptions.forEach { entry ->
                    CompletionDecisionCard(
                        entry = entry,
                        busy = busy,
                        onSelectDecision = onSelectVolunteerDecision,
                        onDraftDirtyChanged = onReviewDraftDirtyChanged,
                        onMarkPresent = onMarkPresent,
                        onRequestMarkAbsent = onRequestMarkAbsent,
                        onViewProfile = onViewProfile
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReviewStatusPill("Exceptions handled", VolunteerLinkSuccess)
            }
        }

        if (selectedCompleteCount > 0 || selectedNotCompleted.isNotEmpty()) {
            ReviewSectionDivider()
            SelectedDecisionSection(
                fullAttendanceCompleted = selectedFullAttendanceComplete.map { it.first },
                individualCompleted = selectedIndividualComplete,
                notCompleted = selectedNotCompleted,
                onChangeDecision = onChangeDecision
            )
        }

        if (issueCandidates.isNotEmpty()) {
            ReviewSectionDivider()
            WorkIssueFinder(
                candidates = issueCandidates,
                busy = busy,
                onReportIssue = onReportIssue,
                onDraftDirtyChanged = onReviewDraftDirtyChanged
            )
        }

        ReviewSectionDivider()
        AttendanceBrowser(
            title = "Review or correct attendance",
            subtitle = "Optional · attendance changes save immediately",
            entries = allUnresolved,
            busy = busy,
            onMarkPresent = onMarkPresent,
            onRequestMarkAbsent = onRequestMarkAbsent,
            onViewProfile = onViewProfile
        )

        if (unresolvedExceptionCount == 0 && ready.isNotEmpty()) {
            ReviewSectionDivider()
            ReviewSectionHeading(
                title = if (selectedCompleteCount == 0 && selectedNotCompleted.isEmpty()) {
                    "Ready to complete · ${ready.size}"
                } else {
                    "Remaining volunteers · ${ready.size}"
                },
                subtitle = "Full attendance · no reported work issue"
            )
            Text(
                text = roleSummary(ready),
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        ReviewStageFooter(
            backLabel = "Back to Attendance",
            primaryLabel = primaryLabel,
            primaryEnabled = primaryEnabled,
            backEnabled = !busy && !hasDraftInput,
            onBack = onBack,
            onPrimary = {
                if (ready.isNotEmpty()) onCompleteReadyAndContinue() else onContinue()
            },
            helperText = helperText
        )
    }
}

@Composable
private fun CompletionDecisionCard(
    entry: PostManagementPhysicalReviewEntry,
    busy: Boolean,
    onSelectDecision: (PostManagementPerson, Boolean, String?) -> Unit,
    onDraftDirtyChanged: (Boolean) -> Unit,
    onMarkPresent: (PostManagementPerson, String) -> Unit,
    onRequestMarkAbsent: (PostManagementPerson, String) -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit
) {
    var expanded by rememberSaveable(reviewKey(entry)) { mutableStateOf(false) }
    var choosingNotCompleted by rememberSaveable(reviewKey(entry), "not_completed") { mutableStateOf(false) }
    var reason by rememberSaveable(reviewKey(entry), "reason") { mutableStateOf("") }
    val verified = entry.attendanceSummary.verifiedMinutes

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ReviewActionShape,
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            VolunteerIdentityHeader(
                entry = entry,
                trailingLabel = "${entry.attendanceSummary.attendedDays}/${entry.attendanceSummary.expectedDays} days",
                trailingColor = VolunteerLinkWarning,
                onViewProfile = onViewProfile
            )
            Text(
                text = "${formatMinutesForReview(verified)} verified · ${entry.absentDays} absent ${if (entry.absentDays == 1) "day" else "days"}",
                modifier = Modifier.padding(top = 7.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextSecondary
            )

            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (expanded) "Hide Decision" else "Review Decision")
                ReviewChevron(expanded)
            }

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = VolunteerLinkBorderColour
                )
                AttendanceTimeline(
                    entry = entry,
                    busy = busy,
                    canCorrect = true,
                    onMarkPresent = onMarkPresent,
                    onRequestMarkAbsent = onRequestMarkAbsent
                )

                Text(
                    text = if (verified > 0) {
                        "Complete gives ${formatMinutesForReview(verified)} verified hours. Not Completed gives 0 completion credit."
                    } else {
                        "No verified attendance hours were recorded, so this volunteer cannot be Completed."
                    },
                    modifier = Modifier.padding(top = 10.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextSecondary
                )

                if (!choosingNotCompleted && verified > 0) {
                    Button(
                        enabled = !busy,
                        onClick = { onSelectDecision(entry.person, true, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 11.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                    ) {
                        Text("Complete with ${formatMinutesForReview(verified)}", fontWeight = FontWeight.Bold)
                    }
                }

                if (!choosingNotCompleted) {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { choosingNotCompleted = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        border = BorderStroke(1.dp, VolunteerLinkError),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VolunteerLinkError)
                    ) {
                        Text("Not Completed", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = {
                            reason = it
                            onDraftDirtyChanged(it.isNotBlank())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 11.dp),
                        label = { Text("Reason for Not Completed") },
                        minLines = 2,
                        maxLines = 4
                    )
                    Button(
                        enabled = !busy && reason.isNotBlank(),
                        onClick = {
                            onSelectDecision(entry.person, false, reason.trim())
                            onDraftDirtyChanged(false)
                            choosingNotCompleted = false
                            reason = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkError)
                    ) {
                        Text("Confirm Not Completed", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            choosingNotCompleted = false
                            reason = ""
                            onDraftDirtyChanged(false)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDecisionSection(
    fullAttendanceCompleted: List<PostManagementPhysicalReviewEntry>,
    individualCompleted: List<Pair<PostManagementPhysicalReviewEntry, PostManagementPendingReviewDecision>>,
    notCompleted: List<Pair<PostManagementPhysicalReviewEntry, PostManagementPendingReviewDecision>>,
    onChangeDecision: (PostManagementPerson) -> Unit
) {
    val completedCount = fullAttendanceCompleted.size + individualCompleted.size
    var showBatchPeople by rememberSaveable { mutableStateOf(false) }

    ReviewSectionHeading(
        title = "Decisions selected",
        subtitle = "Temporary until Finalize Event"
    )

    if (completedCount > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReviewStatusPill("Complete · $completedCount", VolunteerLinkSuccess)
        }

        if (fullAttendanceCompleted.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${fullAttendanceCompleted.size} full-attendance volunteer${if (fullAttendanceCompleted.size == 1) "" else "s"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = "Selected together",
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 12.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
                TextButton(onClick = { showBatchPeople = !showBatchPeople }) {
                    Text(if (showBatchPeople) "Hide" else "View")
                    ReviewChevron(showBatchPeople)
                }
            }

            if (showBatchPeople) {
                Column(
                    modifier = Modifier.padding(top = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    fullAttendanceCompleted.forEach { entry ->
                        CompactVolunteerRow(
                            entry = entry,
                            statusText = "Complete · ${formatMinutesForReview(entry.attendanceSummary.verifiedMinutes)}",
                            statusColor = VolunteerLinkSuccess
                        )
                    }
                }
            }
        }

        if (individualCompleted.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                individualCompleted.forEach { (entry, _) ->
                    SelectedDecisionRow(
                        entry = entry,
                        status = "Complete · ${formatMinutesForReview(entry.attendanceSummary.verifiedMinutes)}",
                        statusColor = VolunteerLinkSuccess,
                        reason = null,
                        onChange = { onChangeDecision(entry.person) }
                    )
                }
            }
        }
    }

    if (notCompleted.isNotEmpty()) {
        if (completedCount > 0) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 11.dp),
                color = VolunteerLinkBorderColour
            )
        }
        ReviewStatusPill("Not Completed · ${notCompleted.size}", VolunteerLinkError)
        Column(
            modifier = Modifier.padding(top = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            notCompleted.forEach { (entry, decision) ->
                SelectedDecisionRow(
                    entry = entry,
                    status = "Not Completed",
                    statusColor = VolunteerLinkError,
                    reason = decision.reason,
                    onChange = { onChangeDecision(entry.person) }
                )
            }
        }
    }
}

@Composable
private fun SelectedDecisionRow(
    entry: PostManagementPhysicalReviewEntry,
    status: String,
    statusColor: Color,
    reason: String?,
    onChange: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.person.fullName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = entry.person.roleName,
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
            ReviewStatusPill(status, statusColor, Modifier.padding(start = 8.dp))
        }
        if (!reason.isNullOrBlank()) {
            Text(
                text = reason,
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextPrimary
            )
        }
        TextButton(onClick = onChange, modifier = Modifier.align(Alignment.End)) {
            Text("Change Decision")
        }
    }
}

@Composable
private fun WorkIssueFinder(
    candidates: List<PostManagementPhysicalReviewEntry>,
    busy: Boolean,
    onReportIssue: (PostManagementPerson, String) -> Unit,
    onDraftDirtyChanged: (Boolean) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedRoleId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNameSearch by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var reason by rememberSaveable { mutableStateOf("") }

    val filtered = candidates.filter { entry ->
        (selectedRoleId == null || entry.person.roleTemplateId == selectedRoleId) &&
            (query.isBlank() || entry.person.fullName.contains(query.trim(), ignoreCase = true))
    }
    val selected = candidates.firstOrNull { reviewKey(it) == selectedKey }

    ReviewExpandableHeader(
        title = "Someone did not fulfil their role?",
        subtitle = "Mark a full-attendance volunteer Not Completed",
        expanded = expanded,
        onClick = { expanded = !expanded }
    )

    if (expanded) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
            Text(
                text = "Choose by role",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
            RoleFilterRow(
                entries = candidates,
                selectedRoleId = selectedRoleId,
                onSelected = {
                    selectedRoleId = it
                    selectedKey = null
                    reason = ""
                    onDraftDirtyChanged(false)
                },
                modifier = Modifier.padding(top = 7.dp)
            )

            TextButton(onClick = { showNameSearch = !showNameSearch }) {
                Icon(
                    painter = painterResource(R.drawable.ic_volunteer_search),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = if (showNameSearch) "Hide name search" else "Search by volunteer name",
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            if (showNameSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        selectedKey = null
                        reason = ""
                        onDraftDirtyChanged(false)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Volunteer name") },
                    singleLine = true
                )
            }

            Column(
                modifier = Modifier.padding(top = 7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                filtered.take(12).forEach { entry ->
                    CompactVolunteerRow(
                        entry = entry,
                        statusText = "Full attendance",
                        statusColor = VolunteerLinkSuccess,
                        selected = reviewKey(entry) == selectedKey,
                        onClick = {
                            selectedKey = reviewKey(entry)
                            reason = ""
                            onDraftDirtyChanged(false)
                        }
                    )
                }
            }

            if (selected != null) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        onDraftDirtyChanged(it.isNotBlank())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Why is this volunteer Not Completed?") },
                    minLines = 2,
                    maxLines = 4
                )
                Button(
                    enabled = !busy && reason.isNotBlank(),
                    onClick = {
                        onReportIssue(selected.person, reason.trim())
                        onDraftDirtyChanged(false)
                        selectedKey = null
                        reason = ""
                        expanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkError)
                ) {
                    Text("Mark Not Completed", fontWeight = FontWeight.Bold)
                }
                TextButton(
                    enabled = !busy,
                    onClick = {
                        selectedKey = null
                        reason = ""
                        onDraftDirtyChanged(false)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun FeedbackReviewStage(
    review: PostManagementPhysicalReview,
    session: PostManagementPhysicalReviewSession,
    allEntries: List<PostManagementPhysicalReviewEntry>,
    busy: Boolean,
    onSaveFeedback: (List<String>, String, String?) -> Unit,
    onReviewDraftDirtyChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val pendingCompletedKeys = session.decisions
        .filter { it.decision == PostManagementPendingDecisionType.COMPLETED }
        .map(::decisionKey)
        .toSet()
    val completedCandidates = allEntries.filter { entry ->
        entry.person.completionStatus.equals("COMPLETED", true) || reviewKey(entry) in pendingCompletedKeys
    }
    val completedUserIds = completedCandidates.map { it.person.userId }.toSet()
    val feedbackMap = buildMap<String, String> {
        review.completed
            .filter { it.hasFeedback && it.person.userId in completedUserIds }
            .forEach { put(it.person.userId, it.feedback!!.trim()) }
        session.feedbackByUserId
            .filterKeys { it in completedUserIds }
            .forEach { (userId, feedback) -> put(userId, feedback) }
    }
    val groups = feedbackMap.entries
        .groupBy { it.value }
        .map { (text, rows) -> text to rows.map { it.key } }
        .sortedByDescending { it.second.size }
    val withoutFeedback = completedCandidates.filter { it.person.userId !in feedbackMap }

    var composerOpen by rememberSaveable { mutableStateOf(false) }
    var editingText by rememberSaveable { mutableStateOf<String?>(null) }
    var feedbackText by rememberSaveable { mutableStateOf("") }
    var selectedRoleIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showNameSearch by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val available = if (editingText == null) withoutFeedback else completedCandidates
    val roleUsers: Set<String> = available
        .filter { it.person.roleTemplateId in selectedRoleIds }
        .map { it.person.userId }
        .toSet()
    val recipients: Set<String> = (roleUsers + selectedUserIds)
        .filter { id -> available.any { entry -> entry.person.userId == id } }
        .toSet()

    fun resetComposer() {
        onReviewDraftDirtyChanged(false)
        composerOpen = false
        editingText = null
        feedbackText = ""
        selectedRoleIds = emptySet()
        selectedUserIds = emptySet()
        showNameSearch = false
        query = ""
    }

    ReviewStageSurface(
        title = "Feedback",
        subtitle = "Optional. Write one message for everyone, whole roles, or selected volunteers instead of repeating the same feedback one by one."
    ) {
        if (completedCandidates.isEmpty()) {
            Text(
                text = "There are no Completed volunteers to receive feedback.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${completedCandidates.size} Completed volunteer${if (completedCandidates.size == 1) "" else "s"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = "${feedbackMap.size} currently receiving feedback",
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 13.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
                ReviewStatusPill("Optional", VolunteerLinkPrimaryGreen)
            }

            if (groups.isNotEmpty()) {
                ReviewSectionDivider()
                ReviewSectionHeading(title = "Feedback prepared · ${groups.size}")
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { (text, userIds) ->
                        TemporaryFeedbackGroupRow(
                            feedback = text,
                            userIds = userIds,
                            completed = completedCandidates,
                            onEdit = {
                                composerOpen = true
                                editingText = text
                                feedbackText = text
                                selectedRoleIds = emptySet()
                                selectedUserIds = userIds.toSet()
                            }
                        )
                    }
                }
            }

            ReviewSectionDivider()
            if (!composerOpen) {
                OutlinedButton(
                    enabled = !busy && withoutFeedback.isNotEmpty(),
                    onClick = {
                        composerOpen = true
                        editingText = null
                        feedbackText = ""
                        selectedRoleIds = emptySet()
                        selectedUserIds = emptySet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VolunteerLinkPrimaryGreen)
                ) {
                    Text(if (groups.isEmpty()) "Add Feedback" else "Add Another Feedback")
                }
                if (withoutFeedback.isEmpty() && groups.isNotEmpty()) {
                    Text(
                        text = "Everyone already has feedback prepared.",
                        modifier = Modifier.padding(top = 7.dp),
                        fontSize = 13.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            } else {
                FeedbackComposer(
                    available = available,
                    withoutFeedback = withoutFeedback,
                    editingText = editingText,
                    feedbackText = feedbackText,
                    selectedRoleIds = selectedRoleIds,
                    recipients = recipients,
                    showNameSearch = showNameSearch,
                    query = query,
                    busy = busy,
                    onSelectAllWithoutFeedback = {
                        selectedRoleIds = emptySet()
                        selectedUserIds = withoutFeedback.map { it.person.userId }.toSet()
                        onReviewDraftDirtyChanged(true)
                    },
                    onToggleRole = { roleId ->
                        selectedRoleIds = if (roleId in selectedRoleIds) {
                            selectedRoleIds - roleId
                        } else {
                            selectedRoleIds + roleId
                        }
                        onReviewDraftDirtyChanged(true)
                    },
                    onToggleNameSearch = { showNameSearch = !showNameSearch },
                    onQueryChange = { query = it },
                    onToggleUser = { userId ->
                        selectedUserIds = if (userId in selectedUserIds) {
                            selectedUserIds - userId
                        } else {
                            selectedUserIds + userId
                        }
                        onReviewDraftDirtyChanged(true)
                    },
                    onFeedbackTextChange = {
                        feedbackText = it
                        onReviewDraftDirtyChanged(true)
                    },
                    onCancel = { resetComposer() },
                    onApply = {
                        onSaveFeedback(recipients.toList(), feedbackText.trim(), editingText)
                        resetComposer()
                    }
                )
            }
        }

        ReviewStageFooter(
            backLabel = "Back to Completion",
            primaryLabel = if (feedbackMap.isEmpty()) "Continue Without Feedback" else "Continue to Finish",
            primaryEnabled = !busy && !session.touched,
            backEnabled = !busy && !session.touched,
            onBack = onBack,
            onPrimary = onContinue,
            helperText = if (session.touched) {
                "Apply or cancel the open feedback draft before changing stages."
            } else {
                "Feedback stays temporary until Finalize Event."
            }
        )
    }
}

@Composable
private fun FeedbackComposer(
    available: List<PostManagementPhysicalReviewEntry>,
    withoutFeedback: List<PostManagementPhysicalReviewEntry>,
    editingText: String?,
    feedbackText: String,
    selectedRoleIds: Set<String>,
    recipients: Set<String>,
    showNameSearch: Boolean,
    query: String,
    busy: Boolean,
    onSelectAllWithoutFeedback: () -> Unit,
    onToggleRole: (String) -> Unit,
    onToggleNameSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleUser: (String) -> Unit,
    onFeedbackTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    ReviewSectionHeading(
        title = if (editingText == null) "New feedback" else "Edit feedback",
        subtitle = "Choose recipients first, then write the message once."
    )

    if (editingText == null && withoutFeedback.isNotEmpty()) {
        OutlinedButton(
            onClick = onSelectAllWithoutFeedback,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VolunteerLinkPrimaryGreen)
        ) {
            Text("All without feedback · ${withoutFeedback.size}")
        }
    }

    Text(
        text = "By role",
        modifier = Modifier.padding(top = 12.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = VolunteerLinkTextPrimary
    )
    FeedbackRoleSelector(
        entries = available,
        selectedRoleIds = selectedRoleIds,
        onToggle = onToggleRole,
        modifier = Modifier.padding(top = 7.dp)
    )

    TextButton(onClick = onToggleNameSearch) {
        Icon(
            painter = painterResource(R.drawable.ic_volunteer_search),
            contentDescription = null,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = if (showNameSearch) "Hide individual search" else "Choose specific volunteers by name",
            modifier = Modifier.padding(start = 6.dp)
        )
    }

    if (showNameSearch) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Volunteer name") },
            singleLine = true
        )
        Column(
            modifier = Modifier.padding(top = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            available
                .filter { query.isBlank() || it.person.fullName.contains(query.trim(), true) }
                .take(12)
                .forEach { entry ->
                    val selected = entry.person.userId in recipients
                    CompactVolunteerRow(
                        entry = entry,
                        statusText = if (selected) "Selected" else "Add",
                        statusColor = VolunteerLinkPrimaryGreen,
                        selected = selected,
                        onClick = { onToggleUser(entry.person.userId) }
                    )
                }
        }
    }

    Text(
        text = if (recipients.isEmpty()) {
            "No recipients selected yet"
        } else {
            "${recipients.size} volunteer${if (recipients.size == 1) "" else "s"} selected"
        },
        modifier = Modifier.padding(top = 10.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (recipients.isEmpty()) VolunteerLinkTextSecondary else VolunteerLinkPrimaryGreen
    )

    OutlinedTextField(
        value = feedbackText,
        onValueChange = onFeedbackTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 9.dp),
        label = { Text("Feedback") },
        minLines = 3,
        maxLines = 6
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            enabled = !busy,
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel")
        }
        Button(
            enabled = !busy && recipients.isNotEmpty() && feedbackText.isNotBlank(),
            onClick = onApply,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
        ) {
            Text(if (editingText == null) "Apply" else "Save Changes")
        }
    }
}

@Composable
private fun TemporaryFeedbackGroupRow(
    feedback: String,
    userIds: List<String>,
    completed: List<PostManagementPhysicalReviewEntry>,
    onEdit: () -> Unit
) {
    val recipients = completed.filter { it.person.userId in userIds }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${recipients.size} recipient${if (recipients.size == 1) "" else "s"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
                Text(
                    text = roleSummary(recipients),
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onEdit) {
                Text("Edit")
            }
        }
        Text(
            text = feedback,
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = VolunteerLinkTextPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinishReviewStage(
    review: PostManagementPhysicalReview,
    session: PostManagementPhysicalReviewSession,
    allEntries: List<PostManagementPhysicalReviewEntry>,
    busy: Boolean,
    onBack: () -> Unit,
    onFinalize: () -> Unit
) {
    val pendingByKey = session.decisions.associateBy(::decisionKey)
    val completedEntries = allEntries.filter { entry ->
        entry.person.completionStatus.equals("COMPLETED", true) ||
            pendingByKey[reviewKey(entry)]?.decision == PostManagementPendingDecisionType.COMPLETED
    }
    val notCompletedEntries = allEntries.filter { entry ->
        entry.person.completionStatus.equals("NOT_COMPLETED", true) ||
            pendingByKey[reviewKey(entry)]?.decision == PostManagementPendingDecisionType.NOT_COMPLETED
    }
    val verifiedMinutes = completedEntries.sumOf { entry ->
        if (entry.person.completionStatus.equals("COMPLETED", true)) {
            entry.verifiedMinutes ?: entry.attendanceSummary.verifiedMinutes
        } else {
            entry.attendanceSummary.verifiedMinutes
        }
    }
    val completedUserIds = completedEntries.map { it.person.userId }.toSet()
    val feedbackMap = buildMap<String, String> {
        review.completed
            .filter { it.hasFeedback && it.person.userId in completedUserIds }
            .forEach { put(it.person.userId, it.feedback!!.trim()) }
        session.feedbackByUserId
            .filterKeys { it in completedUserIds }
            .forEach { (userId, feedback) -> put(userId, feedback) }
    }

    ReviewStageSurface(
        title = if (review.canEdit) "Finish" else "Review Complete",
        subtitle = if (review.canEdit) {
            "Check the final outcome before committing it. Nothing becomes final until you press Finalize Event."
        } else {
            "Completion decisions and attendance are now read-only."
        }
    ) {
        ReviewSectionHeading(title = "Completion")
        ReviewSummaryRow("Completed", completedEntries.size.toString())
        ReviewSummaryRow("Not Completed", notCompletedEntries.size.toString())

        ReviewSectionDivider()
        ReviewSectionHeading(title = "Verified participation")
        ReviewSummaryRow("Verified volunteer hours", formatMinutesForReview(verifiedMinutes))

        ReviewSectionDivider()
        ReviewSectionHeading(title = "Feedback")
        ReviewSummaryRow(
            "Volunteers receiving feedback",
            "${feedbackMap.size} / ${completedEntries.size}"
        )

        if (notCompletedEntries.isNotEmpty()) {
            ReviewSectionDivider()
            ReviewSectionHeading(
                title = "Not Completed volunteers · ${notCompletedEntries.size}",
                subtitle = "These volunteers will receive no completion credit."
            )
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                notCompletedEntries.forEach { entry ->
                    val reason = pendingByKey[reviewKey(entry)]?.reason ?: entry.completionReason
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.person.fullName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VolunteerLinkTextPrimary
                                )
                                Text(
                                    text = entry.person.roleName,
                                    modifier = Modifier.padding(top = 2.dp),
                                    fontSize = 12.sp,
                                    color = VolunteerLinkTextSecondary
                                )
                            }
                            ReviewStatusPill("Not Completed", VolunteerLinkError)
                        }
                        if (!reason.isNullOrBlank()) {
                            Text(
                                text = reason,
                                modifier = Modifier.padding(top = 5.dp),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = VolunteerLinkTextPrimary
                            )
                        }
                    }
                }
            }
        }

        if (review.canEdit) {
            ReviewStageFooter(
                backLabel = "Back to Feedback",
                primaryLabel = "Finalize Event",
                primaryEnabled = !busy,
                backEnabled = !busy,
                onBack = onBack,
                onPrimary = onFinalize,
                helperText = "After finalizing, attendance and completion decisions become read-only."
            )
        } else {
            ReviewSectionDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.tick),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = VolunteerLinkPrimaryGreen
                )
                Text(
                    text = "Event review finalized",
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
            }
        }
    }
}

/** Collapsed optional tool used consistently across Attendance and Completion. */
@Composable
private fun ReviewExpandableHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )
        }
        ReviewChevron(expanded)
    }
}

@Composable
private fun AttendanceBrowser(
    title: String,
    subtitle: String,
    entries: List<PostManagementPhysicalReviewEntry>,
    busy: Boolean,
    onMarkPresent: (PostManagementPerson, String) -> Unit,
    onRequestMarkAbsent: (PostManagementPerson, String) -> Unit,
    onViewProfile: (PostManagementPerson) -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    var selectedRoleId by rememberSaveable(title, "role") { mutableStateOf<String?>(null) }
    var showNameSearch by rememberSaveable(title, "search") { mutableStateOf(false) }
    var query by rememberSaveable(title, "query") { mutableStateOf("") }
    var expandedKey by rememberSaveable(title, "entry") { mutableStateOf<String?>(null) }

    val filtered = entries.filter { entry ->
        (selectedRoleId == null || entry.person.roleTemplateId == selectedRoleId) &&
            (query.isBlank() || entry.person.fullName.contains(query.trim(), ignoreCase = true))
    }

    ReviewExpandableHeader(
        title = title,
        subtitle = subtitle,
        expanded = expanded,
        onClick = { expanded = !expanded }
    )

    if (expanded) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
            RoleFilterRow(
                entries = entries,
                selectedRoleId = selectedRoleId,
                onSelected = {
                    selectedRoleId = it
                    expandedKey = null
                }
            )

            TextButton(onClick = { showNameSearch = !showNameSearch }) {
                Icon(
                    painter = painterResource(R.drawable.ic_volunteer_search),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = if (showNameSearch) "Hide name search" else "Search by volunteer name",
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            if (showNameSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        expandedKey = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Volunteer name") },
                    singleLine = true
                )
            }

            Column(
                modifier = Modifier.padding(top = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filtered.forEach { entry ->
                    val key = reviewKey(entry)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ReviewActionShape,
                        color = if (expandedKey == key) {
                            VolunteerLinkSoftGreenSurface.copy(alpha = 0.55f)
                        } else {
                            VolunteerLinkSurface
                        },
                        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedKey = if (expandedKey == key) null else key }
                                    .padding(11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.person.fullName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VolunteerLinkTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = entry.person.roleName,
                                        modifier = Modifier.padding(top = 2.dp),
                                        fontSize = 12.sp,
                                        color = VolunteerLinkTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                ReviewStatusPill(
                                    attendanceSummaryLabel(entry),
                                    if (entry.absentDays > 0) VolunteerLinkWarning else VolunteerLinkSuccess,
                                    Modifier.padding(start = 8.dp)
                                )
                                ReviewChevron(expandedKey == key)
                            }

                            if (expandedKey == key) {
                                HorizontalDivider(color = VolunteerLinkBorderColour)
                                Column(modifier = Modifier.padding(11.dp)) {
                                    AttendanceTimeline(
                                        entry = entry,
                                        busy = busy,
                                        canCorrect = true,
                                        onMarkPresent = onMarkPresent,
                                        onRequestMarkAbsent = onRequestMarkAbsent
                                    )
                                    TextButton(
                                        onClick = { onViewProfile(entry.person) },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("View Profile")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceTimeline(
    entry: PostManagementPhysicalReviewEntry,
    busy: Boolean,
    canCorrect: Boolean,
    onMarkPresent: (PostManagementPerson, String) -> Unit,
    onRequestMarkAbsent: (PostManagementPerson, String) -> Unit
) {
    Text(
        text = "Attendance",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = VolunteerLinkTextPrimary
    )
    Column(
        modifier = Modifier.padding(top = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entry.attendanceSummary.dateStatuses
            .filter { it.expected }
            .forEach { status ->
                AttendanceTimelineRow(
                    person = entry.person,
                    status = status,
                    busy = busy,
                    canCorrect = canCorrect,
                    onMarkPresent = onMarkPresent,
                    onRequestMarkAbsent = onRequestMarkAbsent
                )
            }
    }
}

@Composable
private fun AttendanceTimelineRow(
    person: PostManagementPerson,
    status: PostManagementVolunteerAttendanceDateStatus,
    busy: Boolean,
    canCorrect: Boolean,
    onMarkPresent: (PostManagementPerson, String) -> Unit,
    onRequestMarkAbsent: (PostManagementPerson, String) -> Unit
) {
    val stateText = when {
        status.present -> "Present · ${formatMinutesForReview(status.verifiedMinutes)}"
        status.markedAbsent -> "Absent"
        status.inferredAbsent -> "Absent · no check-in"
        else -> "Pending"
    }
    val stateColor = when {
        status.present -> VolunteerLinkSuccess
        status.markedAbsent || status.inferredAbsent -> VolunteerLinkError
        else -> VolunteerLinkWarning
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = stateColor
        ) {}
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 9.dp)
        ) {
            Text(
                text = formatReviewDate(status.eventDate),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = stateText,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = stateColor
            )
        }

        if (canCorrect) {
            when {
                status.present -> TextButton(
                    enabled = !busy,
                    onClick = { onRequestMarkAbsent(person, status.eventDate) }
                ) {
                    Text("Mark Absent", color = VolunteerLinkError)
                }

                status.markedAbsent || status.inferredAbsent -> TextButton(
                    enabled = !busy,
                    onClick = { onMarkPresent(person, status.eventDate) }
                ) {
                    Text("Mark Present", color = VolunteerLinkPrimaryGreen)
                }

                else -> TextButton(
                    enabled = !busy,
                    onClick = { onMarkPresent(person, status.eventDate) }
                ) {
                    Text("Mark Present", color = VolunteerLinkPrimaryGreen)
                }
            }
        }
    }
}

@Composable
private fun VolunteerIdentityHeader(
    entry: PostManagementPhysicalReviewEntry,
    trailingLabel: String,
    trailingColor: Color,
    onViewProfile: (PostManagementPerson) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier
                .size(42.dp)
                .clickable { onViewProfile(entry.person) },
            shape = CircleShape,
            color = VolunteerLinkSoftGreenSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.person_placeholder),
                    contentDescription = "View ${entry.person.fullName} profile",
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
                text = entry.person.fullName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.person.roleName,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 12.sp,
                color = VolunteerLinkTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        ReviewStatusPill(
            text = trailingLabel,
            color = trailingColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun CompactVolunteerRow(
    entry: PostManagementPhysicalReviewEntry,
    statusText: String,
    statusColor: Color,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Surface(
        modifier = rowModifier,
        shape = ReviewActionShape,
        color = if (selected) VolunteerLinkSoftGreenSurface else VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.person.fullName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.person.roleName,
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ReviewStatusPill(
                text = statusText,
                color = statusColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun RoleFilterRow(
    entries: List<PostManagementPhysicalReviewEntry>,
    selectedRoleId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = entries
        .groupBy { it.person.roleTemplateId }
        .entries
        .sortedBy { it.value.firstOrNull()?.person?.roleName.orEmpty() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        ReviewFilterPill(
            label = "All · ${entries.size}",
            selected = selectedRoleId == null,
            onClick = { onSelected(null) }
        )
        groups.forEach { (roleId, people) ->
            ReviewFilterPill(
                label = "${people.first().person.roleName} · ${people.size}",
                selected = selectedRoleId == roleId,
                onClick = { onSelected(roleId) }
            )
        }
    }
}

@Composable
private fun FeedbackRoleSelector(
    entries: List<PostManagementPhysicalReviewEntry>,
    selectedRoleIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = entries
        .groupBy { it.person.roleTemplateId }
        .entries
        .sortedBy { it.value.firstOrNull()?.person?.roleName.orEmpty() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        groups.forEach { (roleId, people) ->
            ReviewFilterPill(
                label = "${people.first().person.roleName} · ${people.size}",
                selected = roleId in selectedRoleIds,
                onClick = { onToggle(roleId) }
            )
        }
    }
}

@Composable
private fun ReviewFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = ReviewPillShape,
        color = if (selected) VolunteerLinkPrimaryGreen else VolunteerLinkSurface,
        border = BorderStroke(
            1.dp,
            if (selected) VolunteerLinkPrimaryGreen else VolunteerLinkBorderColour
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) VolunteerLinkSurface else VolunteerLinkTextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun ReviewStatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = ReviewPillShape,
        color = color.copy(alpha = 0.09f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
private fun ReviewMessageStrip(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ReviewActionShape,
        color = VolunteerLinkSoftGreenSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = VolunteerLinkTextPrimary
        )
    }
}

@Composable
private fun ReviewSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )
    }
}

@Composable
private fun ReviewChevron(expanded: Boolean) {
    Icon(
        painter = painterResource(
            if (expanded) R.drawable.review_chevron_up else R.drawable.review_chevron_down
        ),
        contentDescription = null,
        modifier = Modifier
            .padding(start = 4.dp)
            .size(17.dp),
        tint = VolunteerLinkPrimaryGreen
    )
}

private fun reviewKey(entry: PostManagementPhysicalReviewEntry): String =
    "${entry.person.roleTemplateId}|${entry.person.userId}"

private fun decisionKey(decision: PostManagementPendingReviewDecision): String =
    "${decision.roleTemplateId}|${decision.userId}"

private fun attendanceSummaryLabel(entry: PostManagementPhysicalReviewEntry): String =
    "${entry.attendanceSummary.attendedDays}/${entry.attendanceSummary.expectedDays} · ${formatMinutesForReview(entry.attendanceSummary.verifiedMinutes)}"

private fun roleSummary(entries: List<PostManagementPhysicalReviewEntry>): String = entries
    .groupBy { it.person.roleName }
    .entries
    .sortedBy { it.key }
    .joinToString(" · ") { (role, people) -> "$role ${people.size}" }
    .ifBlank { "No volunteers" }

private fun formatMinutesForReview(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val remainder = safe % 60
    return when {
        hours > 0 && remainder > 0 -> "${hours}h ${remainder}m"
        hours > 0 -> "${hours}h"
        else -> "${remainder}m"
    }
}

private fun formatReviewDate(value: String): String {
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("d MMM yyyy", Locale.US)
        formatter.format(parser.parse(value)!!)
    }.getOrElse { value }
}
