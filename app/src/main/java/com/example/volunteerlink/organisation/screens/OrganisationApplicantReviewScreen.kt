package com.example.volunteerlink.organisation.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.components.OrganisationMessageButton
import com.example.volunteerlink.organisation.components.OrganisationOfflineStatusCard
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementRole
import com.example.volunteerlink.organisation.viewmodel.OrganisationPostManagementViewModel
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Organisation-side view of the exact application the volunteer submitted.
 *
 * The visual language intentionally mirrors VolunteerApplicationScreen: green
 * top bar, the same role summary card, numbered questions, outlined answer
 * fields, and a fixed bottom action area. Submitted answers are read-only.
 */
@Composable
fun OrganisationApplicantReviewScreen(
    postId: String,
    roleTemplateId: String,
    userId: String,
    onBack: () -> Unit,
    onViewVolunteerProfile: (String) -> Unit = {},
    onDecisionSaved: () -> Unit = onBack,
    viewModel: OrganisationPostManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.load(postId)
    }

    val post = uiState.post
    val person = post?.people?.firstOrNull { candidate ->
        candidate.userId == userId &&
            candidate.roleTemplateId == roleTemplateId
    }
    val role = post?.roles?.firstOrNull { candidate ->
        candidate.roleTemplateId == roleTemplateId
    }

    var pendingDecision by remember { mutableStateOf<String?>(null) }
    var declineReason by rememberSaveable(postId, roleTemplateId, userId) {
        mutableStateOf("")
    }

    when {
        uiState.isLoading -> ManageLoadingState()

        uiState.errorMessage != null -> ManageErrorState(
            message = uiState.errorMessage,
            onRetry = viewModel::refresh
        )

        post == null || person == null || role == null -> ManageErrorState(
            message = "This application could not be found.",
            onRetry = viewModel::refresh
        )

        else -> OrganisationApplicantReviewContent(
            post = post,
            role = role,
            person = person,
            isSaving = uiState.isUpdatingApplicant,
            actionMessage = uiState.applicantActionMessage,
            isShowingCachedData = uiState.isShowingCachedData,
            lastSyncedAtEpochMillis = uiState.lastSyncedAtEpochMillis,
            isRefreshing = uiState.isRefreshing,
            onSyncSelected = viewModel::refresh,
            onBack = onBack,
            onViewProfile = { onViewVolunteerProfile(person.userId) },
            onAccept = { pendingDecision = "ACCEPT" },
            onDecline = {
                declineReason = ""
                pendingDecision = "DECLINE"
            }
        )
    }

    if (pendingDecision == "ACCEPT" && person != null) {
        AlertDialog(
            onDismissRequest = { pendingDecision = null },
            title = {
                Text(
                    text = "Accept application?",
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
            },
            text = {
                Text(
                    text = "${person.fullName} will become an accepted volunteer for this role. " +
                        "If this fills the role, the remaining pending applicants for the role will be declined automatically.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextSecondary
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingDecision = null }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDecision = null
                        viewModel.reviewApplicant(
                            person = person,
                            decision = "ACCEPT",
                            onSuccess = onDecisionSaved
                        )
                    }
                ) {
                    Text(
                        text = "Accept",
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            },
            containerColor = VolunteerLinkSurface
        )
    }

    if (pendingDecision == "DECLINE" && person != null) {
        OrganisationApplicantDeclineDialog(
            person = person,
            reason = declineReason,
            isSaving = uiState.isUpdatingApplicant,
            errorMessage = uiState.applicantActionMessage,
            onReasonChanged = { declineReason = it },
            onDismiss = {
                if (!uiState.isUpdatingApplicant) {
                    pendingDecision = null
                    declineReason = ""
                }
            },
            onConfirm = {
                val reason = declineReason.trim()
                if (reason.isNotBlank()) {
                    viewModel.reviewApplicant(
                        person = person,
                        decision = "DECLINE",
                        decisionNote = reason,
                        onSuccess = {
                            pendingDecision = null
                            declineReason = ""
                            onDecisionSaved()
                        }
                    )
                }
            }
        )
    }

}

@Composable
private fun OrganisationApplicantDeclineDialog(
    person: PostManagementPerson,
    reason: String,
    isSaving: Boolean,
    errorMessage: String?,
    onReasonChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VolunteerLinkSurface,
        title = {
            Text(
                text = "Decline application?",
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${person.fullName}'s application will be marked Declined and removed from the active Applicants list.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextSecondary
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = VolunteerLinkError.copy(alpha = 0.07f)
                ) {
                    Text(
                        text = "The reason is required and will be shown to the volunteer in My Applications.",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    enabled = !isSaving,
                    label = { Text("Reason for declining") },
                    placeholder = {
                        Text(
                            text = "Explain why this application was not selected",
                            fontSize = 12.sp
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(11.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VolunteerLinkSurface,
                        unfocusedContainerColor = VolunteerLinkSurface,
                        focusedBorderColor = VolunteerLinkError,
                        unfocusedBorderColor = VolunteerLinkBorderColour,
                        cursorColor = VolunteerLinkPrimaryGreen
                    )
                )

                if (reason.isBlank()) {
                    Text(
                        text = "A reason is required before you can decline this applicant.",
                        modifier = Modifier.padding(top = 7.dp),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = VolunteerLinkError
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = reason.trim().isNotEmpty() && !isSaving,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VolunteerLinkError,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isSaving) "Declining..." else "Decline applicant",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun OrganisationApplicantReviewContent(
    post: PostManagementPost,
    role: PostManagementRole,
    person: PostManagementPerson,
    isSaving: Boolean,
    actionMessage: String?,
    isShowingCachedData: Boolean,
    lastSyncedAtEpochMillis: Long?,
    isRefreshing: Boolean,
    onSyncSelected: () -> Unit,
    onBack: () -> Unit,
    onViewProfile: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val isPending = person.applicationStatus.equals("PENDING", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        OrganisationApplicantReviewTopBar(
            person = person,
            onBack = onBack,
            onViewProfile = onViewProfile
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = VolunteerLinkScreenHorizontalPadding,
                end = VolunteerLinkScreenHorizontalPadding,
                top = 18.dp,
                bottom = 24.dp
            )
        ) {
            if (isShowingCachedData) {
                item(key = "application_offline_status") {
                    OrganisationOfflineStatusCard(
                        lastSyncedAtEpochMillis = lastSyncedAtEpochMillis,
                        isSyncing = isRefreshing,
                        onSyncSelected = onSyncSelected
                    )
                }
            }

            item(key = "application_summary") {
                OrganisationApplicationRoleSummary(
                    post = post,
                    role = role
                )
            }

            item(key = "application_questions_heading") {
                Text(
                    text = "Application Questions",
                    modifier = Modifier.padding(top = 20.dp),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = "Review the answers exactly as submitted by ${person.fullName}.",
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            if (person.screeningQuestions.isEmpty()) {
                item(key = "application_no_questions") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = VolunteerLinkSoftGreenSurface,
                        border = BorderStroke(
                            1.dp,
                            VolunteerLinkPrimaryGreen.copy(alpha = 0.25f)
                        )
                    ) {
                        Text(
                            text = "No additional screening questions were required for this application.",
                            modifier = Modifier.padding(15.dp),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }
            } else {
                items(
                    count = person.screeningQuestions.size,
                    key = { index -> "application_question_$index" }
                ) { index ->
                    val question = person.screeningQuestions[index]
                    val answer = person.screeningAnswers.getOrElse(index) { "" }

                    Text(
                        text = "${index + 1}. $question",
                        modifier = Modifier.padding(top = 16.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextPrimary
                    )

                    OutlinedTextField(
                        value = answer,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 7.dp),
                        readOnly = true,
                        minLines = 3,
                        maxLines = 8,
                        shape = RoundedCornerShape(11.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VolunteerLinkSurface,
                            unfocusedContainerColor = VolunteerLinkSurface,
                            focusedBorderColor = VolunteerLinkPrimaryGreen,
                            unfocusedBorderColor = VolunteerLinkBorderColour,
                            cursorColor = VolunteerLinkPrimaryGreen
                        )
                    )
                }
            }

            if (!actionMessage.isNullOrBlank()) {
                item(key = "applicant_action_error") {
                    Text(
                        text = actionMessage,
                        modifier = Modifier.padding(top = 14.dp),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = VolunteerLinkError
                    )
                }
            }

            if (!isPending) {
                item(key = "application_already_resolved") {
                    Text(
                        text = "This application is ${person.applicationStatus.lowercase(Locale.US)} and can no longer receive a new decision.",
                        modifier = Modifier.padding(top = 14.dp),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = VolunteerLinkSurface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = VolunteerLinkScreenHorizontalPadding,
                    vertical = 12.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    enabled = isPending && !isSaving && !isShowingCachedData,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, VolunteerLinkError),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VolunteerLinkError
                    )
                ) {
                    Text("Decline", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAccept,
                    enabled = isPending && !isSaving && !isShowingCachedData,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VolunteerLinkPrimaryGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isSaving) "Saving..." else "Accept",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OrganisationApplicantReviewTopBar(
    person: PostManagementPerson,
    onBack: () -> Unit,
    onViewProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkPrimaryGreen)
            .statusBarsPadding()
            .height(56.dp)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = "Application",
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        IconButton(
            onClick = onViewProfile,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.person_placeholder),
                contentDescription = "View ${person.fullName} profile",
                modifier = Modifier.size(21.dp),
                tint = Color.White
            )
        }

        OrganisationMessageButton(
            personName = person.fullName,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

@Composable
private fun OrganisationApplicationRoleSummary(
    post: PostManagementPost,
    role: PostManagementRole
) {
    val startDate = when (role.roleMode.uppercase(Locale.US)) {
        "PHYSICAL" -> post.physical?.startDate
        "REMOTE" -> post.remote?.startDate
        else -> null
    }.orEmpty()

    val time = when (role.roleMode.uppercase(Locale.US)) {
        "PHYSICAL" -> listOfNotNull(
            post.physical?.startTime,
            post.physical?.endTime
        ).filter { it.isNotBlank() }.joinToString(" - ")
        "REMOTE" -> "Flexible"
        else -> ""
    }

    val location = when (role.roleMode.uppercase(Locale.US)) {
        "PHYSICAL" -> post.physical?.locationAddress
            ?.takeIf { it.isNotBlank() }
            ?: post.physical?.locationName.orEmpty()
        "REMOTE" -> "Online"
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VolunteerLinkSurface),
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = role.roleName.firstOrNull()?.uppercase() ?: "V",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkPrimaryGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = role.roleName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = post.title,
                        fontSize = 12.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = VolunteerLinkBorderColour)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.organisationName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkPrimaryGreen
            )

            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = buildString {
                    append(formatApplicationDate(startDate))
                    if (time.isNotBlank()) {
                        append("  •  ")
                        append(time)
                    }
                },
                fontSize = 11.sp,
                color = VolunteerLinkTextSecondary
            )

            if (location.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = location,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }
    }
}

fun formatApplicationDate(raw: String): String {
    if (raw.isBlank()) return "Date unavailable"
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val formatter = SimpleDateFormat("d MMM yyyy", Locale.US)
        formatter.format(parser.parse(raw) ?: return@runCatching raw)
    }.getOrDefault(raw)
}
