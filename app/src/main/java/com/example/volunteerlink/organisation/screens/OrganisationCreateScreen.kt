package com.example.volunteerlink.organisation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.data.location.DeviceLocationHelper
import com.example.volunteerlink.organisation.components.OrganisationModulePage
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.steps.PostDetailsStep
import com.example.volunteerlink.organisation.create.steps.ReviewSummaryStep
import com.example.volunteerlink.organisation.create.steps.RoleSettingsStep
import com.example.volunteerlink.organisation.create.steps.ScheduleStep
import com.example.volunteerlink.organisation.create.steps.SelectRolesStep
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel

/**
 * Organisation Create route.
 *
 * The route owns lifecycle-only concerns (ViewModel collection, permission,
 * exiting and which Create Post step is shown). Individual steps stay reusable
 * and do not own a NavController.
 */
@Composable
fun OrganisationCreateScreen(
    onExitCreate: () -> Unit = {},
    editPostId: String? = null,
    impactWeaveDraftId: String? = null,
    viewModel: CreatePostViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(editPostId) {
        if (!editPostId.isNullOrBlank()) {
            viewModel.loadExistingPostForEdit(editPostId)
        }
    }
    LaunchedEffect(impactWeaveDraftId) {
        impactWeaveDraftId?.takeIf { it.isNotBlank() }
            ?.let(viewModel::loadImpactWeaveForCreate)
    }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var hasRequestedLocationPermission by rememberSaveable { mutableStateOf(false) }

    fun loadLocationBias() {
        DeviceLocationHelper.getApproximateCurrentLocation(context) { location ->
            if (location != null) {
                viewModel.updateLocationSearchBias(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadLocationBias()
        }
        // If denied, Geoapify continues to search globally.
    }

    val needsLocationBiasForPost =
        uiState.draft.postType == VolunteerPostType.PHYSICAL ||
                uiState.draft.postType == VolunteerPostType.HYBRID

    val needsLocationBias = needsLocationBiasForPost

    LaunchedEffect(needsLocationBias) {
        if (!needsLocationBias) return@LaunchedEffect

        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            loadLocationBias()
        } else if (!hasRequestedLocationPermission) {
            hasRequestedLocationPermission = true
            locationPermissionLauncher.launch(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    val requestExit: () -> Unit = {
        if (viewModel.hasUnsavedInput()) {
            showDiscardDialog = true
        } else {
            onExitCreate()
        }
    }

    val requestBack: () -> Unit = {
        if (uiState.publishedPostId != null || uiState.savedDraftPostId != null || uiState.updatedPostId != null) {
            onExitCreate()
        } else {
            when (uiState.currentStep) {
                1 -> {
                    if (uiState.reviewEditStep == 1) {
                        viewModel.returnToReviewFromEdit()
                    } else {
                        requestExit()
                    }
                }

                2 -> {
                    if (uiState.reviewEditStep == 2) {
                        viewModel.returnToReviewFromEdit()
                    } else {
                        viewModel.backToStepOne()
                    }
                }

                3 -> viewModel.backFromStepThree()
                4 -> viewModel.backFromStepFour()
                5 -> viewModel.backFromReview()
                else -> requestExit()
            }
        }
    }

    BackHandler(onBack = requestBack)

    if (uiState.isLoadingImpactWeave) {
        OrganisationModulePage(
            title = "Preparing Volunteer Post",
            message = "Loading the confirmed Impact Weave activity and partner support..."
        )
    } else if (uiState.impactWeaveLoadError != null) {
        OrganisationModulePage(
            title = "Couldn't Open Impact Weave",
            message = uiState.impactWeaveLoadError
                ?: "This plan is no longer ready to become a Volunteer Post."
        )
    } else if (uiState.isLoadingExistingPost) {
        OrganisationModulePage(
            title = "Loading Post",
            message = "Loading the existing Volunteer Post and its edit restrictions..."
        )
    } else if (uiState.existingPostLoadError != null) {
        OrganisationModulePage(
            title = "Couldn't Open Edit",
            message = uiState.existingPostLoadError ?: "Unable to load this post."
        )
    } else if (uiState.updatedPostId != null) {
        OrganisationModulePage(
            title = "Changes Saved",
            message =
                "Your changes were saved successfully. " +
                    "Use Back to return to Post Management."
        )
    } else if (uiState.savedDraftPostId != null) {
        OrganisationModulePage(
            title = "Draft Saved",
            message =
                "Your volunteer post was saved as a draft and is not published yet. " +
                    "Use Back to return to the Organisation area."
        )
    } else if (uiState.publishedPostId != null) {
        OrganisationModulePage(
            title = "Volunteer Post Published",
            message =
                "Your volunteer post was published successfully. " +
                    "Use Back to return to the Organisation area."
        )
    } else when (uiState.currentStep) {
        1 -> {
            PostDetailsStep(
                uiState = uiState,
                viewModel = viewModel,
                onBack = requestExit,
                onStepOneComplete = viewModel::openStepTwo
            )
        }

        2 -> {
            SelectRolesStep(
                uiState = uiState,
                viewModel = viewModel,
                onBack = requestExit
            )
        }

        3 -> {
            RoleSettingsStep(
                uiState = uiState,
                viewModel = viewModel,
                onBack = requestExit
            )
        }

        4 -> {
            ScheduleStep(
                uiState = uiState,
                viewModel = viewModel,
                onBack = requestExit
            )
        }

        5 -> {
            ReviewSummaryStep(
                uiState = uiState,
                onUp = requestExit,
                onEditStep = viewModel::editStepFromReview,
                onSaveDraft = { viewModel.saveDraft(context) },
                onPublish = { viewModel.publishPost(context) },
                onSaveChanges = { viewModel.saveChanges(context) },
                allowSaveDraft = uiState.impactWeaveDraftId == null
            )
        }

        else -> {
            PostDetailsStep(
                uiState = uiState,
                viewModel = viewModel,
                onBack = requestExit,
                onStepOneComplete = viewModel::openStepTwo
            )
        }
    }

    uiState.editRestrictionMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissEditRestrictionMessage,
            title = { Text("This part is locked") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissEditRestrictionMessage) {
                    Text("OK")
                }
            }
        )
    }

    uiState.saveDraftDateWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = viewModel::dismissSaveDraftDateWarning,
            title = {
                Text("Save draft with an outdated start date?")
            },
            text = {
                Text(
                    "$warning\n\n" +
                        "You can still save this draft, but it cannot be published " +
                        "until the start date is updated."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmSaveDraftWithDateWarning(context)
                    }
                ) {
                    Text("Save Draft Anyway")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissSaveDraftDateWarning
                ) {
                    Text("Go Back")
                }
            }
        )
    }

    uiState.publishDateBlockMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPublishDateBlock,
            title = {
                Text("Post can't be published yet")
            },
            text = {
                Text(
                    "$message\n\n" +
                        "Update the start date before publishing this post."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::fixPublishDateFromReview
                ) {
                    Text("Fix Date")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissPublishDateBlock
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
            },
            title = {
                Text(
                    if (uiState.isExistingPostEdit) {
                        "Discard unsaved changes?"
                    } else {
                        "Discard this volunteer post?"
                    }
                )
            },
            text = {
                Text(
                    if (uiState.isExistingPostEdit) {
                        "Your unsaved edits will be lost. The existing post in VolunteerLink will remain unchanged."
                    } else {
                        "Your current Create Post information will be cleared if you leave."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardDraft()
                        onExitCreate()
                    }
                ) {
                    Text(if (uiState.isExistingPostEdit) "Discard Changes" else "Discard")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                    }
                ) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
