
package com.example.volunteerlink.screens

// Controls the application flow: form validation, profile review, server submission and success feedback.

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch


@Composable
// Purpose: Controls the selected role application, profile preview, submission request and final success state.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
fun VolunteerApplicationScreen(
    volunteerEventId: Int,
    volunteerRoleId: Int,
    onBackSelected: () -> Unit,
    onReturnHomeSelected: () -> Unit,
    onJoinGroupChat: suspend (String) -> Unit,
    volunteerOpportunityViewModel:
        VolunteerOpportunityViewModel
) {
    // Observe loading and error states while an application action is running.
    val opportunityUiState by
        volunteerOpportunityViewModel.uiState
            .collectAsStateWithLifecycle()

    // Read the selected event and role from the shared session data.
    val volunteerOpportunityEvent =
        VolunteerOpportunitySessionStore.findEventById(
            volunteerEventId
        )

    // Resolve the selected role so eligibility and application-method rules use the correct role.
    val volunteerOpportunityRole =
        VolunteerOpportunitySessionStore.findRoleById(
            eventId = volunteerEventId,
            roleId = volunteerRoleId
        )

    // Stop safely when an old navigation link points to data that no longer exists.
    if (
        volunteerOpportunityEvent == null ||
        volunteerOpportunityRole == null
    ) {
        VolunteerApplicationNotFoundScreen(
            onBackSelected = onBackSelected
        )
        return
    }

    // This local flag changes the screen from the form to the success page after server success.
    var applicationWasSubmitted by
    rememberSaveable(
        volunteerEventId,
        volunteerRoleId
    ) {
        mutableStateOf(false)
    }

    // Keep validated answers while the volunteer checks the profile summary before submission.
    var reviewAnswers by rememberSaveable(volunteerEventId, volunteerRoleId) {
        mutableStateOf<List<String>?>(null)
    }

    // Resolve the current application state before deciding whether to show the form, existing record or success page.
    val existingApplication =
        VolunteerOpportunitySessionStore
            .volunteerApplications
            .firstOrNull { volunteerApplication ->
                volunteerApplication.applicationEventId ==
                        volunteerEventId &&
                volunteerApplication.applicationRoleId ==
                        volunteerRoleId &&
                // A cancelled record is historical. Let the volunteer review and submit again.
                volunteerApplication.applicationStatus !=
                        VolunteerApplicationStatus.CANCELLED
            }

    // Choose one visible state: success, existing record, review dialog, or application form.
    when {
        applicationWasSubmitted -> {
            VolunteerApplicationSuccessScreen(
                volunteerOpportunityEvent =
                    volunteerOpportunityEvent,
                volunteerOpportunityRole =
                    volunteerOpportunityRole,
                onReturnHomeSelected =
                    onReturnHomeSelected,
                onJoinGroupChat = onJoinGroupChat

            )
        }

        existingApplication != null -> {
            VolunteerExistingApplicationScreen(
                volunteerOpportunityEvent =
                    volunteerOpportunityEvent,
                volunteerOpportunityRole =
                    volunteerOpportunityRole,
                volunteerApplication =
                    existingApplication,
                onBackSelected =
                    onBackSelected,
                onReturnHomeSelected =
                    onReturnHomeSelected
            )
        }

        else -> {
            // Keep the calculated answers to review value because later validation or Compose content reuses it.
            val answersToReview = reviewAnswers
            // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
            if (answersToReview != null) {
                // The preview is a confirmation step; it does not submit until Confirm is pressed.
                VolunteerApplicationPreviewDialog(
                    event = volunteerOpportunityEvent,
                    role = volunteerOpportunityRole,
                    answers = answersToReview,
                    busy = opportunityUiState.isApplicationActionRunning,
                    actionError = opportunityUiState.applicationActionError,
                    onBack = { reviewAnswers = null },
                    onConfirm = {
                        volunteerOpportunityViewModel.submitApplication(
                            eventId = volunteerEventId,
                            roleId = volunteerRoleId,
                            answers = answersToReview,
                            onSuccess = { applicationWasSubmitted = true }
                        )
                    }
                )
            } else {
                VolunteerApplicationFormScreen(
                    volunteerOpportunityEvent = volunteerOpportunityEvent,
                    volunteerOpportunityRole = volunteerOpportunityRole,
                    onBackSelected = onBackSelected,
                    isSubmitting = opportunityUiState.isApplicationActionRunning,
                    serverErrorMessage = opportunityUiState.applicationActionError,
                    onApplicationSubmitted = { submittedAnswers ->
                        // Form validation has passed; show the volunteer what the organiser will review.
                        reviewAnswers = submittedAnswers
                    }
                )
            }
        }
    }
}

@Composable
// Purpose: Collects required screening answers and decides whether the volunteer may submit or instantly join.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
private fun VolunteerApplicationFormScreen(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    volunteerOpportunityRole:
    VolunteerOpportunityRole,
    onBackSelected: () -> Unit,
    isSubmitting: Boolean,
    serverErrorMessage: String?,
    onApplicationSubmitted: (List<String>) -> Unit
) {
    // Keep the calculated extra questions value because later validation or Compose content reuses it.
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

    // Resolve the current application state before deciding whether to show the form, existing record or success page.
    val applicationNeedsAdditionalForm =
        volunteerOpportunityRole
            .roleApplicationFlow ==
                VolunteerRoleApplicationFlow
                    .ADDITIONAL_FORM

    // Check the role application method because only Instant Join participants receive immediate chat access.
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
                    // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
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
                    // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
                    if (
                        applicationNeedsAdditionalForm
                    ) {
                        "The organisation requires some " +
                                "additional information before " +
                                "reviewing your application."
                    } else {
                        // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
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

            // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
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

                                // Keep the calculated updated answers value because later validation or Compose content reuses it.
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
                                // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
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
            // Connect this button to the validated action prepared above; loading state prevents duplicate requests.
            Button(
                onClick = {
                    // Calculate this Boolean once so every following UI branch uses the same decision.
                    val hasEmptyRequiredAnswer =
                        applicationNeedsAdditionalForm &&
                                questionAnswers.any {
                                        answer ->

                                    answer.isBlank()
                                }

                    validationErrorMessage =
                        // Choose one result from the current state so incompatible UI outcomes are never shown together.
                        when {
                            hasEmptyRequiredAnswer ->
                                "Please answer every " +
                                        "application question."

                            !applicantConfirmedInformation ->
                                "Please confirm your information " +
                                        "before submitting."

                            else -> null
                        }

                    // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
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
                enabled = !isSubmitting
            ) {
                Text(
                    text =
                        // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
                        if (isSubmitting) {
                            "Submitting..."
                        } else if (applicationIsInstantJoin) {
                            "Join Role"
                        } else {
                            "Submit Application"
                        },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
// Purpose: Displays the application title and sends Back to the navigation callback.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
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
                // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
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
// Purpose: Shows the event and role being applied for so the volunteer can verify the selection.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
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
                    "${volunteerOpportunityEvent.eventDate} - " +
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
// Purpose: Keeps the successful result visible and offers Return Home or Join Group Chat for an Instant Join role.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
private fun VolunteerApplicationSuccessScreen(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    volunteerOpportunityRole:
    VolunteerOpportunityRole,
    onReturnHomeSelected: () -> Unit,
    onJoinGroupChat: suspend (String) -> Unit
) {
    // Check the role application method because only Instant Join participants receive immediate chat access.
    val applicationIsInstantJoin =
        volunteerOpportunityRole
            .roleApplicationMethod ==
                VolunteerRoleApplicationMethod
                    .INSTANT_JOIN

    // Use a Compose-aware coroutine scope for the suspend Join Group Chat request.
    val scope = rememberCoroutineScope()

    var isJoiningGroupChat by rememberSaveable {
        mutableStateOf(false)
    }

    var joinGroupChatError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

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
                        // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
                        if (applicationIsInstantJoin) {
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
                // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
                if (applicationIsInstantJoin) {
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
                // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
                if (applicationIsInstantJoin) {
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

        // Connect this button to the validated action prepared above; loading state prevents duplicate requests.
        Button(
            onClick = {
                // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
                if (!applicationIsInstantJoin) {
                    onReturnHomeSelected()
                    return@Button
                }

                // Keep the calculated post id value because later validation or Compose content reuses it.
                val postId = volunteerOpportunityEvent.eventDatabaseId

                // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
                if (postId.isBlank()) {
                    joinGroupChatError =
                        "This event is still loading. Please try again shortly."
                    return@Button
                }

                scope.launch {
                    isJoiningGroupChat = true
                    joinGroupChatError = null

                    runCatching {
                        onJoinGroupChat(postId)
                    }.onFailure { error ->
                        joinGroupChatError =
                            error.message
                                ?: "Could not join the event group chat."
                    }

                    isJoiningGroupChat = false
                }
            },
            enabled = !isJoiningGroupChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(11.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VolunteerLinkPrimaryGreen,
                contentColor = Color.White
            )
        ) {
            Text(
                text = when {
                    !applicationIsInstantJoin -> "Return to Home"
                    isJoiningGroupChat -> "Joining Group Chat..."
                    else -> "Join Group Chat"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        joinGroupChatError?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = error,
                color = VolunteerLinkError,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
// Purpose: Prevents duplicate applications and directs the volunteer to the existing application record.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
private fun VolunteerExistingApplicationScreen(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    volunteerOpportunityRole:
    VolunteerOpportunityRole,
    volunteerApplication:
    VolunteerOpportunityApplication,
    onBackSelected: () -> Unit,
    onReturnHomeSelected: () -> Unit
) {
    // Keep the calculated status text value because later validation or Compose content reuses it.
    val statusText =
        // Choose one result from the current state so incompatible UI outcomes are never shown together.
        when (
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

    // Keep the calculated status colour value because later validation or Compose content reuses it.
    val statusColour =
        // Choose one result from the current state so incompatible UI outcomes are never shown together.
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
                text = "Already Applied",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "You already submitted an application " +
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

                    // Reject this branch early when its requirement is not met, preventing an invalid request or navigation.
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

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // Connect this button to the validated action prepared above; loading state prevents duplicate requests.
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
// Purpose: Shows a safe fallback when route IDs no longer match an event or role in the shared session.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
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

        // Connect this button to the validated action prepared above; loading state prevents duplicate requests.
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
