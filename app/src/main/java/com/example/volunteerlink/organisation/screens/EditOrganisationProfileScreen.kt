package com.example.volunteerlink.organisation.screens

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements the Organisation UI associated with Edit Organisation Profile Screen.
//
// The composable layer is responsible for layout, interaction and displaying loading/error/validation state;
// business rules and persistence are delegated to ViewModels/repositories.
//
// This separation makes it clear during maintenance which code changes appearance versus which code changes real
// server data.
//
// Where the screen displays cached information, server-changing actions remain disabled or routed through a fresh
// authenticated repository operation.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.volunteerlink.data.location.CurrentLocationResolver
import com.example.volunteerlink.data.saveProfileImage
import com.example.volunteerlink.shared.OrganisationTypeOptions
import com.example.volunteerlink.organisation.auth.OrganisationSessionStore
import com.example.volunteerlink.shared.countryStates
import com.example.volunteerlink.organisation.organisationFieldColours
import com.example.volunteerlink.organisation.repository.OrganisationProfileRepository
import kotlinx.coroutines.launch

/**
 * DETAILED DECLARATION — EmailChangeStep
 *
 * Domain/UI type for Email Change Step used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private enum class EmailChangeStep { NONE, ENTER_EMAIL, WAITING_FOR_LINK }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * DETAILED BEHAVIOUR — EditOrganisationProfileScreen
 *
 * Renders the Edit Organisation Profile screen from state supplied by the owning ViewModel/repository-facing
 * coordinator.
 *
 * The composable maps state to Material3 UI and emits callbacks; it does not become the source of truth for
 * persisted VolunteerLink data.
 */
fun EditOrganisationProfileScreen(
    onBack: () -> Unit = {},
    // Called after a successful save. Clears the cached profileData in
    // OrganisationSessionStore so OrganisationProfileScreen refetches
    // instead of showing the pre-edit values after navigating back.
    onSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // OrganisationProfileScreen loads the profile into
    // OrganisationSessionStore and blocks its own UI (including the edit
    // icon) until that finishes — so by the time this screen is reachable,
    // the cache is already populated. Reading it directly here means no
    // second fetch and no second spinner on tapping edit.
    val cachedProfile = OrganisationSessionStore.profileData

    var isSaving by remember { mutableStateOf(false) }

    var organisationName by remember { mutableStateOf(cachedProfile?.organisationName ?: "") }
    var contactPhone by remember { mutableStateOf(cachedProfile?.registeredPhone ?: "") }
    var description by remember { mutableStateOf(cachedProfile?.bio ?: "") }
    var profileImageUrl by remember { mutableStateOf(cachedProfile?.profileImageUrl) }

    var loginEmailState by remember { mutableStateOf(cachedProfile?.loginEmail ?: "") }

    var emailChangeStep by remember { mutableStateOf(EmailChangeStep.NONE) }
    var newEmailInput by remember { mutableStateOf("") }
    var isProcessingEmailChange by remember { mutableStateOf(false) }
    var emailChangeError by remember { mutableStateOf<String?>(null) }

    val emailInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(emailInteractionSource) {
        emailInteractionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                newEmailInput = ""
                emailChangeError = null
                emailChangeStep = EmailChangeStep.ENTER_EMAIL
            }
        }
    }

    var organisationType by remember { mutableStateOf(cachedProfile?.organisationType ?: "") }
    var country by remember { mutableStateOf(cachedProfile?.country ?: "") }
    var stateRegion by remember { mutableStateOf(cachedProfile?.stateRegion ?: "") }
    var locationName by remember { mutableStateOf(cachedProfile?.locationName ?: "") }

    var showOrganisationTypeDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    var isCountryMenuExpanded by remember { mutableStateOf(false) }
    var isStateMenuExpanded by remember { mutableStateOf(false) }
    var isLocationMenuExpanded by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val imageUrl = saveProfileImage(context = context, uri = uri)
                if (imageUrl != null) {
                    profileImageUrl = imageUrl
                }
            }
        }
    }

    // While waiting, check every few seconds whether the email actually
// changed server-side — this is what catches the link being clicked
// on a completely different device.
    LaunchedEffect(emailChangeStep) {
        if (emailChangeStep == EmailChangeStep.WAITING_FOR_LINK) {
            val originalEmail = loginEmailState
            while (emailChangeStep == EmailChangeStep.WAITING_FOR_LINK) {
                kotlinx.coroutines.delay(3000)
                val refreshed = OrganisationProfileRepository.refreshLoginEmail()
                if (refreshed != null && refreshed != originalEmail) {
                    loginEmailState = refreshed
                    OrganisationSessionStore.clearProfileData()
                    emailChangeStep = EmailChangeStep.NONE
                    break
                }
            }
        }
    }

    // Same tap-to-open-dialog mechanism used on the sign-up screen — a
    // readOnly field's own interactionSource is the reliable way to
    // detect a tap, since a plain clickable{} on a readOnly field can
    // silently never fire.
    val organisationTypeInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(organisationTypeInteractionSource) {
        organisationTypeInteractionSource.interactions.collect {
            if (it is PressInteraction.Release) showOrganisationTypeDialog = true
        }
    }

    val locationInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(locationInteractionSource) {
        locationInteractionSource.interactions.collect {
            if (it is PressInteraction.Release) showLocationDialog = true
        }
    }

    val availableStates = countryStates[country]?.keys?.toList() ?: emptyList()
    val availableLocations = countryStates[country]?.get(stateRegion) ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .height(70.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "EDIT ORGANISATION PROFILE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // No loading gate here anymore — cachedProfile above already came
        // pre-loaded from OrganisationSessionStore.

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // LOGO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUrl != null) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Organisation logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Business,
                            contentDescription = "Add organisation logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tap to change logo",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (!profileImageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Remove photo",
                    color = Color(0xFFC62828),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { profileImageUrl = null }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {

                // ORGANISATION NAME
                OutlinedTextField(
                    value = organisationName,
                    onValueChange = { organisationName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Organisation name") },
                    singleLine = true,
                    enabled = !isSaving,
                    colors = organisationFieldColours()
                )

                Spacer(Modifier.height(13.dp))

                // ORGANISATION TYPE
                Box {
                    OutlinedTextField(
                        value = organisationType,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSaving,
                        interactionSource = organisationTypeInteractionSource,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Organisation type") },
                        trailingIcon = { Text("›") },
                        colors = organisationFieldColours()
                    )

                    if (showOrganisationTypeDialog) {
                        AlertDialog(
                            onDismissRequest = { showOrganisationTypeDialog = false },
                            title = { Text("Select organisation type") },
                            text = {
                                Column {
                                    OrganisationTypeOptions.forEach { option ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = organisationType == option,
                                                onClick = {
                                                    organisationType = option
                                                    showOrganisationTypeDialog = false
                                                }
                                            )
                                            Text(
                                                text = option,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showOrganisationTypeDialog = false }) {
                                    Text("Done")
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(13.dp))

                // LOGIN EMAIL — tap to start the Supabase Auth email-change flow.
                Box {
                    OutlinedTextField(
                        value = loginEmailState,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSaving,
                        interactionSource = emailInteractionSource,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Login email") },
                        trailingIcon = { Text("›") },
                        colors = organisationFieldColours()
                    )
                }

                Spacer(Modifier.height(13.dp))

                // CONTACT PHONE
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contact phone") },
                    singleLine = true,
                    enabled = !isSaving,
                    colors = organisationFieldColours()
                )

                Spacer(Modifier.height(13.dp))

                // USE CURRENT LOCATION (optional shortcut — manual picker below still
// works exactly as before)
                var isResolvingCurrentLocation by remember { mutableStateOf(false) }
                var currentLocationMessage by remember { mutableStateOf<String?>(null) }
                var cancelCurrentLocationRequest by remember { mutableStateOf<(() -> Unit)?>(null) }

                DisposableEffect(Unit) {
                    onDispose { cancelCurrentLocationRequest?.invoke() }
                }

                /**
                 * DETAILED BEHAVIOUR — beginCurrentLocationResolution
                 *
                 * Handles the Compose/UI responsibility for begin current location resolution.
                 *
                 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
                 * ViewModel/repository layers.
                 */
                fun beginCurrentLocationResolution() {
                    if (!CurrentLocationResolver.isLocationEnabled(context)) {
                        isResolvingCurrentLocation = false
                        currentLocationMessage = "Turn on your device's Location setting, then try again."
                        return
                    }
                    cancelCurrentLocationRequest?.invoke()
                    isResolvingCurrentLocation = true
                    currentLocationMessage = "Getting your location…"
                    cancelCurrentLocationRequest = CurrentLocationResolver.resolve(
                        context = context,
                        countryStates = countryStates,
                        scope = scope
                    ) { outcome ->
                        isResolvingCurrentLocation = false
                        currentLocationMessage = outcome.message
                        outcome.match?.let { match ->
                            country = match.country
                            stateRegion = match.stateRegion
                            locationName = match.locationName
                        }
                    }
                }

                val currentLocationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    ) {
                        beginCurrentLocationResolution()
                    } else {
                        isResolvingCurrentLocation = false
                        currentLocationMessage = "Location permission was not granted."
                    }
                }

                TextButton(
                    onClick = {
                        if (CurrentLocationResolver.hasLocationPermission(context)) {
                            beginCurrentLocationResolution()
                        } else {
                            isResolvingCurrentLocation = true
                            currentLocationMessage = "Waiting for location permission…"
                            currentLocationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    },
                    enabled = !isSaving && !isResolvingCurrentLocation
                ) {
                    if (isResolvingCurrentLocation) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (isResolvingCurrentLocation) "Locating..." else "Use current location")
                }

                currentLocationMessage?.let { message ->
                    Text(
                        text = message,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // LOCATION (tap to open cascading picker — same pattern
                // and same countryStates data as the sign-up screen)
                Box {
                    OutlinedTextField(
                        value = when {
                            locationName.isNotEmpty() && stateRegion.isNotEmpty() && country.isNotEmpty() ->
                                "$locationName, $stateRegion, $country"
                            country.isNotEmpty() -> country
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSaving,
                        interactionSource = locationInteractionSource,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Location") },
                        trailingIcon = { Text("›") },
                        placeholder = { Text("Select location") },
                        colors = organisationFieldColours()
                    )

                    if (showLocationDialog) {
                        AlertDialog(
                            onDismissRequest = { showLocationDialog = false },
                            title = { Text("Select location") },
                            text = {
                                Column {
                                    Text("Country", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.height(6.dp))

                                    ExposedDropdownMenuBox(
                                        expanded = isCountryMenuExpanded,
                                        onExpandedChange = { isCountryMenuExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = country,
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(),
                                            placeholder = { Text("Select country") },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = isCountryMenuExpanded
                                                )
                                            },
                                            singleLine = true
                                        )
                                        ExposedDropdownMenu(
                                            expanded = isCountryMenuExpanded,
                                            onDismissRequest = { isCountryMenuExpanded = false }
                                        ) {
                                            countryStates.keys.forEach { selectedCountry ->
                                                DropdownMenuItem(
                                                    text = { Text(selectedCountry) },
                                                    onClick = {
                                                        country = selectedCountry
                                                        stateRegion = ""
                                                        locationName = ""
                                                        isCountryMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    Text("State / region", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.height(6.dp))

                                    ExposedDropdownMenuBox(
                                        expanded = isStateMenuExpanded,
                                        onExpandedChange = {
                                            if (country.isNotEmpty()) isStateMenuExpanded = it
                                        }
                                    ) {
                                        OutlinedTextField(
                                            value = stateRegion,
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = country.isNotEmpty(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(),
                                            placeholder = {
                                                Text(if (country.isEmpty()) "Select country first" else "Select state / region")
                                            },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = isStateMenuExpanded
                                                )
                                            },
                                            singleLine = true
                                        )
                                        ExposedDropdownMenu(
                                            expanded = isStateMenuExpanded,
                                            onDismissRequest = { isStateMenuExpanded = false }
                                        ) {
                                            availableStates.forEach { selectedState ->
                                                DropdownMenuItem(
                                                    text = { Text(selectedState) },
                                                    onClick = {
                                                        stateRegion = selectedState
                                                        locationName = ""
                                                        isStateMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    Text("Location", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.height(6.dp))

                                    ExposedDropdownMenuBox(
                                        expanded = isLocationMenuExpanded,
                                        onExpandedChange = {
                                            if (stateRegion.isNotEmpty()) isLocationMenuExpanded = it
                                        }
                                    ) {
                                        OutlinedTextField(
                                            value = locationName,
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = stateRegion.isNotEmpty(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(),
                                            placeholder = {
                                                Text(if (stateRegion.isEmpty()) "Select state first" else "Select location")
                                            },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = isLocationMenuExpanded
                                                )
                                            },
                                            singleLine = true
                                        )
                                        ExposedDropdownMenu(
                                            expanded = isLocationMenuExpanded,
                                            onDismissRequest = { isLocationMenuExpanded = false }
                                        ) {
                                            availableLocations.forEach { selectedLocation ->
                                                DropdownMenuItem(
                                                    text = { Text(selectedLocation) },
                                                    onClick = {
                                                        locationName = selectedLocation
                                                        isLocationMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (country.isNotEmpty() && stateRegion.isNotEmpty() && locationName.isNotEmpty()) {
                                            showLocationDialog = false
                                        }
                                    }
                                ) {
                                    Text("Done")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLocationDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    if (emailChangeStep != EmailChangeStep.NONE) {
                        AlertDialog(
                            onDismissRequest = {
                                if (!isProcessingEmailChange) {
                                    emailChangeStep = EmailChangeStep.NONE
                                    emailChangeError = null
                                }
                            },
                            title = {
                                Text(
                                    if (emailChangeStep == EmailChangeStep.ENTER_EMAIL)
                                        "Change login email"
                                    else
                                        "Check your email"
                                )
                            },
                            text = {
                                Column {
                                    if (emailChangeStep == EmailChangeStep.ENTER_EMAIL) {
                                        Text(
                                            "We'll send a confirmation link to your new email.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = newEmailInput,
                                            onValueChange = {
                                                newEmailInput = it
                                                emailChangeError = null
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            enabled = !isProcessingEmailChange,
                                            label = { Text("New email") }
                                        )
                                        emailChangeError?.let { message ->
                                            Spacer(Modifier.height(6.dp))
                                            Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                "We sent a link to $newEmailInput. Click it from any device — " +
                                                        "this screen will update automatically once confirmed.",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                if (emailChangeStep == EmailChangeStep.ENTER_EMAIL) {
                                    TextButton(
                                        enabled = !isProcessingEmailChange,
                                        onClick = {
                                            scope.launch {
                                                val trimmed = newEmailInput.trim()
                                                if (trimmed.isBlank()) {
                                                    emailChangeError = "Enter an email address."
                                                    return@launch
                                                }
                                                isProcessingEmailChange = true
                                                val success = OrganisationProfileRepository.requestEmailChange(trimmed)
                                                isProcessingEmailChange = false
                                                if (success) {
                                                    emailChangeStep = EmailChangeStep.WAITING_FOR_LINK
                                                } else {
                                                    emailChangeError = "Couldn't send link. Try again shortly."
                                                }
                                            }
                                        }
                                    ) { Text(if (isProcessingEmailChange) "Sending..." else "Send link") }
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        emailChangeStep = EmailChangeStep.NONE
                                        emailChangeError = null
                                    }
                                ) { Text(if (emailChangeStep == EmailChangeStep.WAITING_FOR_LINK) "Close" else "Cancel") }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(13.dp))

                // DESCRIPTION (bio)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    label = { Text("About your organisation") },
                    singleLine = false,
                    enabled = !isSaving,
                    colors = organisationFieldColours()
                )

                Spacer(Modifier.height(30.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                val success = OrganisationProfileRepository.updateProfile(
                                    organisationName = organisationName,
                                    registeredPhone = contactPhone,
                                    bio = description,
                                    locationName = locationName,
                                    stateRegion = stateRegion,
                                    country = country,
                                    profileImageUrl = profileImageUrl
                                )
                                if (success) {
                                    onSaved()
                                    onBack()
                                } else {
                                    println("Failed to update organisation profile")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (isSaving) "SAVING..." else "SAVE",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}