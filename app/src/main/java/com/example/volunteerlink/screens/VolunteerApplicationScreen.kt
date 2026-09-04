
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import com.example.volunteerlink.model.VolunteerRoleApplicationFlow
import com.example.volunteerlink.model.VolunteerRoleApplicationMethod
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

@Composable
fun VolunteerApplicationScreen(
    volunteerEventId: Int,
    volunteerRoleId: Int,
    onBackSelected: () -> Unit,
    onReturnHomeSelected: () -> Unit,
    volunteerOpportunityViewModel:
        VolunteerOpportunityViewModel
) {
    val opportunityUiState by
        volunteerOpportunityViewModel.uiState
            .collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(volunteerEventId, volunteerRoleId) {
        volunteerOpportunityViewModel.clearApplicationActionError()
    }

    val volunteerOpportunityEvent =
        VolunteerOpportunitySessionStore.findEventById(
            volunteerEventId
        )

    val volunteerOpportunityRole =
        VolunteerOpportunitySessionStore.findRoleById(
            eventId = volunteerEventId,
            roleId = volunteerRoleId
        )

    if (
        volunteerOpportunityEvent == null ||
        volunteerOpportunityRole == null
    ) {
        VolunteerApplicationNotFoundScreen(
            onBackSelected = onBackSelected
        )
        return
    }

    var applicationWasSubmitted by
    rememberSaveable(
        volunteerEventId,
        volunteerRoleId
    ) {
        mutableStateOf(false)
    }

    val existingApplication =
        VolunteerOpportunitySessionStore
            .volunteerApplications
            .firstOrNull { volunteerApplication ->
                volunteerApplication.applicationEventId ==
                        volunteerEventId &&
                        volunteerApplication.applicationRoleId ==
                        volunteerRoleId
            }

    val phoneContactIsRelevant =
        existingApplication?.applicationStatus == VolunteerApplicationStatus.ACCEPTED &&
            volunteerOpportunityEvent.eventDatabaseId.isNotBlank() &&
            volunteerOpportunityRole.roleTemplateId.isNotBlank()

    androidx.compose.runtime.LaunchedEffect(
        volunteerOpportunityEvent.eventDatabaseId,
        volunteerOpportunityRole.roleTemplateId,
        phoneContactIsRelevant
    ) {
        if (phoneContactIsRelevant) {
            volunteerOpportunityViewModel.loadEventPhoneContact(
                postId = volunteerOpportunityEvent.eventDatabaseId,
                roleTemplateId = volunteerOpportunityRole.roleTemplateId
            )
        } else {
            volunteerOpportunityViewModel.clearEventPhoneContact()
        }
    }

    val activeApplicationInPost =
        VolunteerOpportunitySessionStore.activeApplicationForEvent(
            volunteerEventId
        )

    val otherPendingApplication =
        VolunteerOpportunitySessionStore.pendingApplicationForEvent(
            volunteerEventId
        )?.takeIf { application ->
            application.applicationRoleId != volunteerRoleId
        }

    val otherActiveApplication = activeApplicationInPost?.takeIf { it.applicationRoleId != volunteerRoleId }
    val applicationBlockedMessage: String? = when {
        otherActiveApplication?.applicationDatabaseId?.startsWith("offline|") == true -> "Sync your earlier request before changing roles."
        VolunteerOpportunitySessionStore.volunteerApplications.any {
            it.applicationEventId == volunteerEventId && it.applicationStatus in setOf(
                VolunteerApplicationStatus.COMPLETED, VolunteerApplicationStatus.NOT_COMPLETED)
        } -> "Your participation is finalized. You cannot take a second role in this event."
        else -> null
    }

    var pendingInstantSwitchAnswers by rememberSaveable {
        mutableStateOf<List<String>?>(null)
    }
    var confirmedPrevious by androidx.compose.runtime.remember {
        mutableStateOf<VolunteerOpportunityApplication?>(null)
    }
    var previewAnswers by rememberSaveable(volunteerEventId, volunteerRoleId) {
        mutableStateOf<List<String>?>(null)
    }

    when {
        applicationWasSubmitted -> {
            VolunteerApplicationSuccessScreen(
                resultMessage = opportunityUiState.lastApplicationResult,
                volunteerOpportunityEvent =
                    volunteerOpportunityEvent,
                volunteerOpportunityRole =
                    volunteerOpportunityRole,
                onReturnHomeSelected =
                    onReturnHomeSelected
            )
        }

        existingApplication != null && existingApplication.applicationStatus != VolunteerApplicationStatus.CANCELLED -> {
            VolunteerExistingApplicationScreen(
                volunteerOpportunityEvent =
                    volunteerOpportunityEvent,
                volunteerOpportunityRole =
                    volunteerOpportunityRole,
                volunteerApplication =
                    existingApplication,
                phoneContactState = opportunityUiState.eventPhoneContact,
                onPhoneContactEnabledChange = { enabled ->
                    volunteerOpportunityViewModel.setEventPhoneContactEnabled(
                        postId = volunteerOpportunityEvent.eventDatabaseId,
                        roleTemplateId = volunteerOpportunityRole.roleTemplateId,
                        enabled = enabled
                    )
                },
                onBackSelected =
                    onBackSelected,
                onReturnHomeSelected =
                    onReturnHomeSelected
            )
        }

        else -> {
            VolunteerApplicationFormScreen(
                volunteerOpportunityEvent =
                    volunteerOpportunityEvent,
                volunteerOpportunityRole =
                    volunteerOpportunityRole,
                onBackSelected =
                    onBackSelected,
                isSubmitting =
                    opportunityUiState
                        .isApplicationActionRunning,
                serverErrorMessage =
                    applicationBlockedMessage ?:
                        opportunityUiState.applicationActionError ?: if (
                            !com.example.volunteerlink.data.VolunteerApplicationWindow.canApply(
                                volunteerOpportunityEvent,
                                volunteerOpportunityRole
                            )
                        ) com.example.volunteerlink.data.VolunteerApplicationWindow.reason(
                            volunteerOpportunityEvent,
                            volunteerOpportunityRole
                        ) else null,
                submissionAllowed = applicationBlockedMessage == null,
                onApplicationSubmitted = { submittedAnswers ->
                    if (volunteerOpportunityRole.roleApplicationMethod == VolunteerRoleApplicationMethod.REVIEW_APPLICANTS) {
                        previewAnswers = submittedAnswers
                    } else if (
                        otherActiveApplication != null
                    ) {
                        confirmedPrevious = otherActiveApplication
                        pendingInstantSwitchAnswers = submittedAnswers
                    } else {
                        volunteerOpportunityViewModel.submitApplication(
                            eventId = volunteerEventId,
                            roleId = volunteerRoleId,
                            answers = submittedAnswers,
                            onSuccess = {
                                applicationWasSubmitted = true
                            }
                        )
                    }
                }
            )
        }
    }

    previewAnswers?.let { answers ->
        VolunteerApplicationPreviewDialog(volunteerOpportunityEvent, volunteerOpportunityRole, answers,
            opportunityUiState.isApplicationActionRunning, opportunityUiState.applicationActionError,
            onBack = { previewAnswers = null }, onConfirm = {
                if (otherActiveApplication != null) {
                    confirmedPrevious = otherActiveApplication
                    pendingInstantSwitchAnswers = answers
                    previewAnswers = null
                } else {
                    volunteerOpportunityViewModel.submitApplication(volunteerEventId, volunteerRoleId, answers) {
                        previewAnswers = null
                        applicationWasSubmitted = true
                    }
                }
            })
    }

    val switchAnswers = pendingInstantSwitchAnswers
    if (switchAnswers != null && confirmedPrevious != null) {
        AlertDialog(
            titleContentColor = VolunteerLinkTextPrimary,
            textContentColor = VolunteerLinkTextSecondary,
            containerColor = Color.White,
            onDismissRequest = {
                pendingInstantSwitchAnswers = null
            },
            title = {
                Text(
                    text = "Change your role?"
                )
            },
            text = {
                Text(
                    text =
                        "Your current role is ${confirmedPrevious?.applicationRoleTitle}. " +
                            "Changing to ${volunteerOpportunityRole.roleTitle} cancels that application and gives up any accepted place. " +
                            (if (volunteerOpportunityRole.roleApplicationMethod == VolunteerRoleApplicationMethod.REVIEW_APPLICANTS)
                                "The new role needs a fresh organisation review; acceptance is not guaranteed. "
                            else "The new role is confirmed only if a place is still available. ") +
                            "If this change fails, this request will not cancel your previous role. Internet connection is required."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingInstantSwitchAnswers = null
                        volunteerOpportunityViewModel
                            .submitApplication(
                                eventId = volunteerEventId,
                                roleId = volunteerRoleId,
                                answers = switchAnswers,
                                confirmedPrevious = confirmedPrevious,
                                onSuccess = {
                                    applicationWasSubmitted = true
                                }
                            )
                    }
                ) {
                    Text("Confirm change")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingInstantSwitchAnswers = null
                    }
                ) {
                    Text("Keep current role")
                }
            }
        )
    }
}

@Composable
private fun VolunteerApplicationFormScreen(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    volunteerOpportunityRole:
    VolunteerOpportunityRole,
    onBackSelected: () -> Unit,
    isSubmitting: Boolean,
    serverErrorMessage: String?,
    submissionAllowed: Boolean,
    onApplicationSubmitted: (List<String>) -> Unit
) {
    val extraQuestions =
        volunteerOpportunityRole
            .roleExtraApplicationQuestions

    var questionAnswers by
    rememberSaveable(
        volunteerOpportunityEvent.eventId,
        volunteerOpportunityRole.roleId
    ) {
        mutableStateOf(
            ArrayList(
                List(extraQuestions.size) {
                    ""
                }
            )
        )
    }

    var applicantConfirmedInformation by
    rememberSaveable {
        mutableStateOf(false)
    }

    var validationErrorMessage by
    rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val applicationNeedsAdditionalForm =
        volunteerOpportunityRole
            .roleApplicationFlow ==
                VolunteerRoleApplicationFlow
                    .ADDITIONAL_FORM

    val applicationIsInstantJoin =
        volunteerOpportunityRole
            .roleApplicationMethod ==
                VolunteerRoleApplicationMethod
                    .INSTANT_JOIN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                VolunteerLinkBackground
            )
    ) {
        VolunteerApplicationTopBar(
            applicationNeedsAdditionalForm =
                applicationNeedsAdditionalForm,
            applicationIsInstantJoin =
                applicationIsInstantJoin,
            onBackSelected =
                onBackSelected
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal =
                        VolunteerLinkScreenHorizontalPadding
                )
        ) {
            Spacer(
                modifier = Modifier.height(18.dp)
            )

            VolunteerApplicationRoleSummary(
                volunteerOpportunityEvent =
                    volunteerOpportunityEvent,
                volunteerOpportunityRole =
                    volunteerOpportunityRole
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text =
                    if (
                        applicationNeedsAdditionalForm
                    ) {
                        "Application Questions"
                    } else {
                        "Confirm Application"
                    },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    if (
                        applicationNeedsAdditionalForm
                    ) {
                        "The organisation requires some " +
                                "additional information before " +
                                "reviewing your application."
                    } else {
                        if (applicationIsInstantJoin) {
                            "This role supports instant joining. " +
                                    "Confirm your availability to secure the role."
                        } else {
                            "Review the role information before submitting."
                        }
                    },
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )

            if (applicationNeedsAdditionalForm) {
                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                extraQuestions
                    .forEachIndexed {
                            questionIndex,
                            applicationQuestion ->

                        Text(
                            text =
                                "${questionIndex + 1}. " +
                                        applicationQuestion,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                VolunteerLinkTextPrimary
                        )

                        Spacer(
                            modifier =
                                Modifier.height(7.dp)
                        )

                        OutlinedTextField(
                            value =
                                questionAnswers[
                                    questionIndex
                                ],
                            onValueChange = {
                                    updatedAnswer ->

                                val updatedAnswers =
                                    ArrayList(
                                        questionAnswers
                                    )

                                updatedAnswers[
                                    questionIndex
                                ] = updatedAnswer

                                questionAnswers =
                                    updatedAnswers

                                validationErrorMessage =
                                    null
                            },
                            modifier =
                                Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text =
                                        "Enter your answer",
                                    fontSize = 12.sp
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            shape =
                                RoundedCornerShape(11.dp),
                            colors =
                                OutlinedTextFieldDefaults
                                    .colors(
                                        focusedContainerColor =
                                            VolunteerLinkSurface,
                                        unfocusedContainerColor =
                                            VolunteerLinkSurface,
                                        focusedBorderColor =
                                            VolunteerLinkPrimaryGreen,
                                        unfocusedBorderColor =
                                            VolunteerLinkBorderColour,
                                        cursorColor =
                                            VolunteerLinkPrimaryGreen
                                    )
                        )

                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )
                    }
            } else {
                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(12.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                VolunteerLinkSoftGreenSurface
                        ),
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color =
                                VolunteerLinkPrimaryGreen
                                    .copy(alpha = 0.25f)
                        )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(15.dp)
                    ) {
                        Text(
                            text =
                                "What happens after submission?",
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                VolunteerLinkPrimaryGreen
                        )

                        Spacer(
                            modifier =
                                Modifier.height(7.dp)
                        )

                        Text(
                            text =
                                if (applicationIsInstantJoin) {
                                    "Your place will be confirmed immediately. " +
                                            "The role will appear in My Applications."
                                } else {
                                    "Your application will be sent to " +
                                            volunteerOpportunityEvent
                                                .eventOrganisationName +
                                            ". You can check its status " +
                                            "from My Applications."
                                },
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color =
                                VolunteerLinkTextSecondary
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            VolunteerLinkSurface
                    ),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            VolunteerLinkBorderColour
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp,
                            vertical = 8.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked =
                            applicantConfirmedInformation,
                        onCheckedChange = {
                                isChecked ->

                            applicantConfirmedInformation =
                                isChecked

                            validationErrorMessage =
                                null
                        },
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor =
                                    VolunteerLinkPrimaryGreen,
                                uncheckedColor =
                                    VolunteerLinkTextSecondary
                            )
                    )

                    Text(
                        text =
                            "I confirm that the information " +
                                    "provided is accurate and I am " +
                                    "available for this role.",
                        modifier =
                            Modifier.weight(1f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color =
                            VolunteerLinkTextPrimary
                    )
                }
            }

            validationErrorMessage
                ?.let { errorMessage ->

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.Medium,
                        color = VolunteerLinkError
                    )
                }

            serverErrorMessage
                ?.let { errorMessage ->
                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = VolunteerLinkError
                    )
                }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VolunteerLinkSurface,
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = {
                    val hasEmptyRequiredAnswer =
                        applicationNeedsAdditionalForm &&
                                questionAnswers.any {
                                        answer ->

                                    answer.isBlank()
                                }

                    validationErrorMessage =
                        when {
                            hasEmptyRequiredAnswer ->
                                "Please answer every " +
                                        "application question."

                            !applicantConfirmedInformation ->
                                "Please confirm your information " +
                                        "before submitting."

                            else -> null
                        }

                    if (
                        validationErrorMessage == null
                    ) {
                        onApplicationSubmitted(
                            questionAnswers.toList()
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            VolunteerLinkScreenHorizontalPadding,
                        vertical = 12.dp
                    )
                    .height(50.dp),
                shape =
                    RoundedCornerShape(11.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            VolunteerLinkPrimaryGreen,
                        contentColor =
                            Color.White
                    ),
                contentPadding =
                    PaddingValues(
                        horizontal = 16.dp
                    ),
                enabled = !isSubmitting &&
                    submissionAllowed &&
                    com.example.volunteerlink.data.VolunteerApplicationWindow.canApply(
                        volunteerOpportunityEvent,
                        volunteerOpportunityRole
                    )
            ) {
                Text(
                    text =
                        if (isSubmitting) {
                            "Submitting..."
                        } else if (applicationIsInstantJoin) {
                            "Join Role"
                        } else {
                            "Review Application"
                        },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VolunteerApplicationTopBar(
    applicationNeedsAdditionalForm: Boolean,
    applicationIsInstantJoin: Boolean,
    onBackSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                VolunteerLinkPrimaryGreen
            )
            .statusBarsPadding()
            .height(56.dp)
            .padding(
                start = 4.dp,
                end = 16.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
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
            text =
                if (
                    applicationNeedsAdditionalForm
                ) {
                    "Role Application"
                } else if (applicationIsInstantJoin) {
                    "Instant Join"
                } else {
                    "Quick Application"
                },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun VolunteerApplicationRoleSummary(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    volunteerOpportunityRole:
    VolunteerOpportunityRole
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    VolunteerLinkSurface
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color = VolunteerLinkBorderColour
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color =
                        VolunteerLinkSoftGreenSurface
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                volunteerOpportunityRole
                                    .roleTitle
                                    .firstOrNull()
                                    ?.uppercase()
                                    ?: "V",
                            fontSize = 17.sp,
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                VolunteerLinkPrimaryGreen
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
                            volunteerOpportunityRole
                                .roleTitle,
                        fontSize = 16.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            VolunteerLinkTextPrimary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            volunteerOpportunityEvent
                                .eventTitle,
                        fontSize = 12.sp,
                        color =
                            VolunteerLinkTextSecondary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            HorizontalDivider(
                color = VolunteerLinkBorderColour
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text =
                    volunteerOpportunityEvent
                        .eventOrganisationName,
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    VolunteerLinkPrimaryGreen
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    "${volunteerOpportunityEvent.eventDate}  •  " +
                            volunteerOpportunityEvent
                                .eventTime,
                fontSize = 11.sp,
                color =
                    VolunteerLinkTextSecondary
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text =
                    volunteerOpportunityEvent
                        .eventFullAddress
                        .ifBlank {
                            volunteerOpportunityEvent
                                .eventLocation
                        },
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color =
                    VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
private fun VolunteerApplicationSuccessScreen(
    resultMessage: String?,
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    volunteerOpportunityRole:
    VolunteerOpportunityRole,
    onReturnHomeSelected: () -> Unit
) {
    val applicationIsInstantJoin =
        volunteerOpportunityRole
            .roleApplicationMethod ==
                VolunteerRoleApplicationMethod
                    .INSTANT_JOIN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                VolunteerLinkBackground
            )
            .statusBarsPadding()
            .padding(
                horizontal =
                    VolunteerLinkScreenHorizontalPadding
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(82.dp),
            shape = CircleShape,
            color = Color(0xFFE8F5E9)
        ) {
            Box(
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector =
                        Icons.Filled.CheckCircle,
                    contentDescription =
                        if (resultMessage?.startsWith("Waiting to sync") == true) {
                            "Saved on device, waiting to sync"
                        } else if (applicationIsInstantJoin) {
                            "Role joined"
                        } else {
                            "Application submitted"
                        },
                    modifier =
                        Modifier.size(52.dp),
                    tint = VolunteerLinkSuccess
                )
            }
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Text(
            text =
                if (resultMessage?.startsWith("Waiting to sync") == true) {
                    "Waiting to sync"
                } else if (applicationIsInstantJoin) {
                    "Role Joined"
                } else {
                    "Application Submitted"
                },
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        Text(
            text =
                if (resultMessage != null) {
                    resultMessage
                } else if (applicationIsInstantJoin) {
                    "Your place for " +
                            volunteerOpportunityRole
                                .roleTitle +
                            " with " +
                            volunteerOpportunityEvent
                                .eventOrganisationName +
                            " is confirmed."
                } else {
                    "Your application for " +
                        volunteerOpportunityRole
                            .roleTitle +
                        " has been sent to " +
                        volunteerOpportunityEvent
                            .eventOrganisationName +
                        "."
                },
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = VolunteerLinkTextSecondary
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                "You can track the latest status " +
                        "from My Applications.",
            fontSize = 12.sp,
            color = VolunteerLinkTextSecondary
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Button(
            onClick = onReturnHomeSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(11.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        VolunteerLinkPrimaryGreen,
                    contentColor =
                        Color.White
                )
        ) {
            Text(
                text = "Return to Home",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VolunteerExistingApplicationScreen(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    volunteerOpportunityRole:
    VolunteerOpportunityRole,
    volunteerApplication:
    VolunteerOpportunityApplication,
    phoneContactState: VolunteerEventPhoneContactUiState,
    onPhoneContactEnabledChange: (Boolean) -> Unit,
    onBackSelected: () -> Unit,
    onReturnHomeSelected: () -> Unit
) {
    val waitingToSync = volunteerApplication.applicationDatabaseId.startsWith("offline|")
    val statusText =
        if (waitingToSync) "Waiting to sync" else when (
            volunteerApplication.applicationStatus
        ) {
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

    val statusColour =
        when (
            volunteerApplication.applicationStatus
        ) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                VolunteerLinkBackground
            )
    ) {
        VolunteerApplicationTopBar(
            applicationNeedsAdditionalForm =
                volunteerOpportunityRole
                    .roleApplicationFlow ==
                        VolunteerRoleApplicationFlow
                            .ADDITIONAL_FORM,
            applicationIsInstantJoin =
                volunteerOpportunityRole
                    .roleApplicationMethod ==
                        VolunteerRoleApplicationMethod
                            .INSTANT_JOIN,
            onBackSelected =
                onBackSelected
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = VolunteerLinkScreenHorizontalPadding,
                    vertical = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color =
                    statusColour.copy(
                        alpha = 0.12f
                    )
            ) {
                Box(
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.CheckCircle,
                        contentDescription =
                            "Existing application",
                        modifier =
                            Modifier.size(46.dp),
                        tint = statusColour
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = if (waitingToSync) "Waiting to sync" else "Already Applied",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    if (waitingToSync) "This request is saved on your device. No place is reserved. Connect and Sync to receive the server result."
                    else "You already submitted an application " +
                            "for ${volunteerOpportunityRole.roleTitle}.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = VolunteerLinkTextSecondary
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            VolunteerLinkSurface
                    ),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            VolunteerLinkBorderColour
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text =
                            volunteerOpportunityEvent
                                .eventTitle,
                        fontSize = 14.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            VolunteerLinkTextPrimary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            volunteerOpportunityEvent
                                .eventOrganisationName,
                        fontSize = 12.sp,
                        color =
                            VolunteerLinkTextSecondary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    HorizontalDivider(
                        color =
                            VolunteerLinkBorderColour
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Application status",
                            fontSize = 12.sp,
                            color =
                                VolunteerLinkTextSecondary
                        )

                        Surface(
                            shape =
                                RoundedCornerShape(6.dp),
                            color =
                                statusColour.copy(
                                    alpha = 0.12f
                                )
                        ) {
                            Text(
                                text = statusText,
                                modifier =
                                    Modifier.padding(
                                        horizontal = 9.dp,
                                        vertical = 5.dp
                                    ),
                                fontSize = 11.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color = statusColour
                            )
                        }
                    }

                    if (
                        volunteerApplication
                            .applicationStatusMessage
                            .isNotBlank()
                    ) {
                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        Text(
                            text =
                                volunteerApplication
                                    .applicationStatusMessage,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color =
                                VolunteerLinkTextSecondary
                        )
                    }
                }
            }

            if (
                volunteerApplication.applicationStatus == VolunteerApplicationStatus.ACCEPTED &&
                volunteerOpportunityRole.roleMode.uppercase() in setOf("PHYSICAL", "REMOTE")
            ) {
                Spacer(modifier = Modifier.height(14.dp))

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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Event phone contact",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VolunteerLinkTextPrimary
                                )
                                Text(
                                    text = "Allow ${volunteerOpportunityEvent.eventOrganisationName} to call the phone number on your profile while you are participating in this opportunity.",
                                    modifier = Modifier.padding(top = 4.dp),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = VolunteerLinkTextSecondary
                                )
                            }

                            Switch(
                                checked = phoneContactState.enabled,
                                onCheckedChange = onPhoneContactEnabledChange,
                                enabled = phoneContactState.eligible &&
                                    !phoneContactState.isLoading &&
                                    !phoneContactState.isUpdating
                            )
                        }

                        when {
                            phoneContactState.isLoading -> {
                                Text(
                                    text = "Checking contact permission...",
                                    modifier = Modifier.padding(top = 10.dp),
                                    fontSize = 11.sp,
                                    color = VolunteerLinkTextSecondary
                                )
                            }

                            phoneContactState.errorMessage != null -> {
                                Text(
                                    text = phoneContactState.errorMessage,
                                    modifier = Modifier.padding(top = 10.dp),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = VolunteerLinkError
                                )
                            }

                            !phoneContactState.eligible -> {
                                Text(
                                    text = phoneContactState.reason
                                        ?: "Phone sharing is not available for this opportunity.",
                                    modifier = Modifier.padding(top = 10.dp),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = VolunteerLinkTextSecondary
                                )
                            }

                            phoneContactState.enabled -> {
                                Text(
                                    text = buildString {
                                        append("Your organiser can call you for this opportunity")
                                        phoneContactState.availableUntilLabel
                                            ?.takeIf { it.isNotBlank() }
                                            ?.let { append(" until $it") }
                                        append(". Access expires automatically.")
                                    },
                                    modifier = Modifier.padding(top = 10.dp),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VolunteerLinkPrimaryGreen
                                )
                            }

                            else -> {
                                Text(
                                    text = buildString {
                                        append("Your phone number stays private unless you turn this on")
                                        phoneContactState.availableUntilLabel
                                            ?.takeIf { it.isNotBlank() }
                                            ?.let { append(". If enabled, access lasts until $it") }
                                        append(".")
                                    },
                                    modifier = Modifier.padding(top = 10.dp),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = VolunteerLinkTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Button(
                onClick =
                    onReturnHomeSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape =
                    RoundedCornerShape(11.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            VolunteerLinkPrimaryGreen,
                        contentColor =
                            Color.White
                    )
            ) {
                Text(
                    text = "View My Applications",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VolunteerApplicationNotFoundScreen(
    onBackSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                VolunteerLinkBackground
            )
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "Application unavailable",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                "The selected event or role " +
                        "could not be found.",
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            onClick = onBackSelected,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        VolunteerLinkPrimaryGreen
                )
        ) {
            Text("Return")
        }
    }
}
