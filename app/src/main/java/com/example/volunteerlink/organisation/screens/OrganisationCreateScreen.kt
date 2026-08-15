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
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.steps.PostDetailsStep
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel

/**
 * Organisation Create route.
 *
 * The route owns lifecycle-only concerns (ViewModel collection, permission and
 * exiting). Step 1 itself remains a reusable UI function.
 */
@Composable
fun OrganisationCreateScreen(
    onExitCreate: () -> Unit = {},
    viewModel: CreatePostViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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

    val needsPhysicalLocation =
        uiState.draft.postType == VolunteerPostType.PHYSICAL ||
                uiState.draft.postType == VolunteerPostType.HYBRID

    LaunchedEffect(needsPhysicalLocation) {
        if (!needsPhysicalLocation) return@LaunchedEffect

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

    BackHandler(onBack = requestExit)

    PostDetailsStep(
        uiState = uiState,
        viewModel = viewModel,
        onBack = requestExit
    )

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
            },
            title = {
                Text("Discard this volunteer post?")
            },
            text = {
                Text(
                    "Your current Create Post information will be cleared if you leave."
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
                    Text("Discard")
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
