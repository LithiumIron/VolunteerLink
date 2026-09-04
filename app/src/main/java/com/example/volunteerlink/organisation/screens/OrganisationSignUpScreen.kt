package com.example.volunteerlink.organisation

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.volunteerlink.data.location.CurrentLocationResolver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.shared.OrganisationTypeOptions
import com.example.volunteerlink.shared.SharedOtpVerificationSection
import com.example.volunteerlink.shared.authFieldColours
import com.example.volunteerlink.shared.countryCallingCodes
import com.example.volunteerlink.shared.countryStates
import com.example.volunteerlink.shared.isValidAuthPhoneNumber

private fun isOrganisationSignUpFormValid(
    organisationName: String,
    organisationType: String,
    email: String,
    combinedPhone: String,
    password: String
): Boolean {
    return organisationName.isNotBlank() &&
            organisationType.isNotBlank() &&
            email.isNotBlank() &&
            isValidAuthPhoneNumber(combinedPhone.trim()) &&
            password.length >= 6
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganisationSignUpScreen(
    onBackSelected: () -> Unit,
    onSignedUp: () -> Unit,
    organisationAuthViewModel: OrganisationAuthViewModel = viewModel()
) {

    val uiState by organisationAuthViewModel.uiState
        .collectAsStateWithLifecycle()

    var showOrganisationTypeDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLocationDialog by rememberSaveable {
        mutableStateOf(false)
    }


    // ========================================================
    // FORM VALUES
    // ========================================================

    var organisationName by rememberSaveable {
        mutableStateOf("")
    }

    var email by rememberSaveable {
        mutableStateOf("")
    }

    // Holds ONLY the local number the user types (e.g. "12-1234567") —
    // never the country calling code. The code is shown as a fixed,
    // non-editable OutlinedTextField "prefix" instead of living inside
    // this value, so there's nothing here for the user to backspace
    // through or delete.
    var phone by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var country by rememberSaveable {
        mutableStateOf("")
    }

    var stateRegion by rememberSaveable {
        mutableStateOf("")
    }

    var locationName by rememberSaveable {
        mutableStateOf("")
    }

    var organisationType by rememberSaveable {
        mutableStateOf("")
    }

    var showConfirmPasswordDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var confirmPassword by rememberSaveable {
        mutableStateOf("")
    }

    var confirmPasswordError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // Verification code entered on the OTP step below, once
    // uiState.needsEmailConfirmation flips true after signUp().
    var otpCode by rememberSaveable {
        mutableStateOf("")
    }

    // The calling code for the selected country — rendered as the phone
    // field's fixed prefix. Empty until a country is picked, same as
    // before.
    val phoneCallingCode = countryCallingCodes[country] ?: ""

    // What's actually sent to sign-up / validated — the fixed code plus
    // whatever local number the user typed. This is what the rest of the
    // screen (validation, the submit call) cares about; `phone` itself
    // never contains the code.
    val combinedPhone = "$phoneCallingCode $phone".trim()

    // Live phone validation — same rule signUp() itself enforces, shown
    // as a red inline error once the user has typed something invalid
    // (stays quiet while the local number is still empty).
    val phoneError = phone.isNotBlank() && !isValidAuthPhoneNumber(combinedPhone)


    // ========================================================
    // DROPDOWN STATES (used INSIDE the location dialog)
    // ========================================================

    var isCountryMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var isStateMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var isLocationMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }


    // ========================================================
    // PRESS DETECTION FOR THE TWO "TAP TO OPEN A DIALOG" FIELDS
    // ========================================================
    // A readOnly OutlinedTextField still consumes touch internally for its
    // own focus/cursor handling, so a plain Modifier.clickable{} on it is
    // unreliable and can silently never fire. Giving the field its own
    // interactionSource and listening for PressInteraction.Release on that
    // is the reliable way to detect a tap — this is the same mechanism
    // ExposedDropdownMenuBox uses internally for its own click-to-open.

    val organisationTypeInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(organisationTypeInteractionSource) {
        organisationTypeInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                showOrganisationTypeDialog = true
            }
        }
    }

    val locationInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(locationInteractionSource) {
        locationInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                showLocationDialog = true
            }
        }
    }


    // ========================================================
    // AVAILABLE OPTIONS BASED ON SELECTION
    // ========================================================

    val availableStates =
        countryStates[country]?.keys?.toList()
            ?: emptyList()

    val availableLocations =
        countryStates[country]?.get(stateRegion)
            ?: emptyList()


    // ========================================================
    // AUTHENTICATION
    // ========================================================

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onSignedUp()
        }
    }

    val isBusy =
        uiState.isSubmitting ||
                uiState.isCheckingSession

    // Recomputed on every recomposition, so the button's enabled state
    // updates live as the user types — no separate "check on click" step.
    val isFormValid = isOrganisationSignUpFormValid(
        organisationName = organisationName,
        organisationType = organisationType,
        email = email,
        combinedPhone = combinedPhone,
        password = password
    )


    // ========================================================
    // MAIN SCREEN
    // ========================================================



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {

        // ====================================================
        // BACK BUTTON
        // ====================================================

        IconButton(
            onClick = onBackSelected,
            enabled = !isBusy
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (uiState.needsEmailConfirmation) {
            // Code entry instead of "click the link in your inbox" —
            // matches VolunteerSignUpScreen's VolunteerOtpVerificationSection.
            SharedOtpVerificationSection(
                email = uiState.pendingVerificationEmail.orEmpty(),
                otpCode = otpCode,
                onOtpCodeChange = {
                    otpCode = it
                    organisationAuthViewModel.clearError()
                },
                isBusy = isBusy,
                isResending = uiState.isResendingEmail,
                errorMessage = uiState.errorMessage,
                onVerify = { organisationAuthViewModel.verifySignUpOtp(otpCode) },
                onResend = organisationAuthViewModel::resendVerificationEmail,
                onUseDifferentEmail = {
                    otpCode = ""
                    organisationAuthViewModel.changeEmail()
                },
                codeLength = 8
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // ====================================================
                // SCROLLABLE FORM
                // ====================================================

                Text(
                    text = "Create an organisation account",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(
                    Modifier.height(7.dp)
                )

                Text(
                    text = "Post volunteer opportunities and review applicants.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    Modifier.height(26.dp)
                )


                // =================================================
                // ORGANISATION NAME
                // =================================================

                OutlinedTextField(
                    value = organisationName,
                    onValueChange = {
                        organisationName = it
                        organisationAuthViewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Organisation name")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Business,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = authFieldColours()
                )


                Spacer(
                    Modifier.height(13.dp)
                )


                // =================================================
                // ORGANISATION TYPE (tap to open a picker dialog)
                // =================================================

                Box {
                    OutlinedTextField(
                        value = organisationType,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isBusy,
                        interactionSource = organisationTypeInteractionSource,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Organisation type")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Business,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            Text("›")
                        },
                        placeholder = {
                            Text("Select organisation type")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = authFieldColours()
                    )

                    if (showOrganisationTypeDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showOrganisationTypeDialog = false
                            },
                            title = {
                                Text("Select organisation type")
                            },
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
                                TextButton(
                                    onClick = {
                                        showOrganisationTypeDialog = false
                                    }
                                ) {
                                    Text("Done")
                                }
                            }
                        )
                    }
                }


                Spacer(
                    Modifier.height(13.dp)
                )


                // =================================================
                // EMAIL
                // =================================================

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        organisationAuthViewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Login email")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Mail,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    enabled = !isBusy,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = authFieldColours()
                )


                Spacer(
                    Modifier.height(13.dp)
                )


                // =================================================
                // PHONE — the calling code is a fixed `prefix` on the
                // field itself, not part of `value`. That means it's
                // rendered inside the field (so it still looks like one
                // continuous "+60 12-1234567" field) but the user's
                // cursor, selection and backspacing can never reach it —
                // there's physically nothing to delete, since `phone`
                // (the editable value) only ever holds the local number.
                // =================================================

                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        phone = input.filter { c -> c.isDigit() || c == '-' }
                        organisationAuthViewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Contact phone")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null
                        )
                    },
                    prefix = if (phoneCallingCode.isNotEmpty()) {
                        { Text("$phoneCallingCode ") }
                    } else {
                        null
                    },
                    placeholder = {
                        Text("e.g. 12-456789")
                    },
                    singleLine = true,
                    enabled = !isBusy,
                    isError = phoneError,
                    supportingText = {
                        Text(
                            text = when {
                                phoneCallingCode.isEmpty() ->
                                    "Select a location below to set your country code."
                                phoneError ->
                                    "Enter a valid phone number (e.g. 12-456789)."
                                else ->
                                    "Country code is set from your selected location."
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = authFieldColours()
                )


                Spacer(
                    Modifier.height(13.dp)
                )

                // =================================================
// USE CURRENT LOCATION (optional shortcut — manual
// picker below still works exactly as before)
// =================================================

                val locationContext = LocalContext.current
                val locationScope = rememberCoroutineScope()
                var isResolvingCurrentLocation by rememberSaveable { mutableStateOf(false) }
                var currentLocationMessage by rememberSaveable { mutableStateOf<String?>(null) }
                var cancelCurrentLocationRequest by remember { mutableStateOf<(() -> Unit)?>(null) }

                DisposableEffect(Unit) {
                    onDispose { cancelCurrentLocationRequest?.invoke() }
                }

                fun beginCurrentLocationResolution() {
                    if (!CurrentLocationResolver.isLocationEnabled(locationContext)) {
                        isResolvingCurrentLocation = false
                        currentLocationMessage = "Turn on your device's Location setting, then try again."
                        return
                    }
                    cancelCurrentLocationRequest?.invoke()
                    isResolvingCurrentLocation = true
                    currentLocationMessage = "Getting your location…"
                    cancelCurrentLocationRequest = CurrentLocationResolver.resolve(
                        context = locationContext,
                        countryStates = countryStates,
                        scope = locationScope
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
                        if (CurrentLocationResolver.hasLocationPermission(locationContext)) {
                            beginCurrentLocationResolution()
                        } else {
                            isResolvingCurrentLocation = true
                            currentLocationMessage = "Waiting for location permission…"
                            currentLocationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    },
                    enabled = !isBusy && !isResolvingCurrentLocation
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

                // =================================================
                // LOCATION (tap to open a picker dialog with the
                // country -> state/region -> location cascade)
                // =================================================

                Box {
                    OutlinedTextField(
                        value = when {
                            locationName.isNotEmpty() &&
                                    stateRegion.isNotEmpty() &&
                                    country.isNotEmpty() ->
                                "$locationName, $stateRegion, $country"

                            country.isNotEmpty() ->
                                country

                            else ->
                                ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isBusy,
                        interactionSource = locationInteractionSource,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Location")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            Text("›")
                        },
                        placeholder = {
                            Text("Select location")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = authFieldColours()
                    )

                    if (showLocationDialog) {

                        AlertDialog(
                            onDismissRequest = {
                                showLocationDialog = false
                            },

                            title = {
                                Text("Select location")
                            },

                            text = {

                                Column {

                                    // COUNTRY

                                    Text(
                                        text = "Country",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Spacer(
                                        Modifier.height(6.dp)
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = isCountryMenuExpanded,
                                        onExpandedChange = {
                                            isCountryMenuExpanded = it
                                        }
                                    ) {

                                        OutlinedTextField(
                                            value = country,
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(),
                                            placeholder = {
                                                Text("Select country")
                                            },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = isCountryMenuExpanded
                                                )
                                            },
                                            singleLine = true
                                        )

                                        ExposedDropdownMenu(
                                            expanded = isCountryMenuExpanded,
                                            onDismissRequest = {
                                                isCountryMenuExpanded = false
                                            }
                                        ) {

                                            countryStates.keys.forEach { selectedCountry ->

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(selectedCountry)
                                                    },
                                                    onClick = {

                                                        // phone (the local
                                                        // number) is left
                                                        // untouched here —
                                                        // only the fixed
                                                        // prefix shown on
                                                        // the field changes,
                                                        // since it's derived
                                                        // from `country`.
                                                        country = selectedCountry
                                                        stateRegion = ""
                                                        locationName = ""

                                                        isCountryMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }


                                    Spacer(
                                        Modifier.height(16.dp)
                                    )


                                    // STATE / REGION

                                    Text(
                                        text = "State / region",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Spacer(
                                        Modifier.height(6.dp)
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = isStateMenuExpanded,
                                        onExpandedChange = {

                                            if (country.isNotEmpty()) {
                                                isStateMenuExpanded = it
                                            }
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
                                                Text(
                                                    if (country.isEmpty())
                                                        "Select country first"
                                                    else
                                                        "Select state / region"
                                                )
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
                                            onDismissRequest = {
                                                isStateMenuExpanded = false
                                            }
                                        ) {

                                            availableStates.forEach { selectedState ->

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(selectedState)
                                                    },
                                                    onClick = {

                                                        stateRegion = selectedState
                                                        locationName = ""

                                                        isStateMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }


                                    Spacer(
                                        Modifier.height(16.dp)
                                    )


                                    // LOCATION

                                    Text(
                                        text = "Location",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Spacer(
                                        Modifier.height(6.dp)
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = isLocationMenuExpanded,
                                        onExpandedChange = {

                                            if (stateRegion.isNotEmpty()) {
                                                isLocationMenuExpanded = it
                                            }
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
                                                Text(
                                                    if (stateRegion.isEmpty())
                                                        "Select state first"
                                                    else
                                                        "Select location"
                                                )
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
                                            onDismissRequest = {
                                                isLocationMenuExpanded = false
                                            }
                                        ) {

                                            availableLocations.forEach { selectedLocation ->

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(selectedLocation)
                                                    },
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

                                        if (
                                            country.isNotEmpty() &&
                                            stateRegion.isNotEmpty() &&
                                            locationName.isNotEmpty()
                                        ) {
                                            showLocationDialog = false
                                        }
                                    }
                                ) {
                                    Text("Done")
                                }
                            },

                            dismissButton = {

                                TextButton(
                                    onClick = {
                                        showLocationDialog = false
                                    }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }


                Spacer(
                    Modifier.height(13.dp)
                )


                // =================================================
                // PASSWORD
                // =================================================

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        organisationAuthViewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Password")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    enabled = !isBusy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = authFieldColours()
                )


                // =================================================
                // ERROR MESSAGE (from an actual Supabase submit)
                // =================================================

                uiState.errorMessage?.let { message ->

                    Spacer(
                        Modifier.height(11.dp)
                    )

                    Text(
                        text = message,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }


                Spacer(
                    Modifier.height(22.dp)
                )


                // =================================================
                // CREATE ACCOUNT BUTTON
                // =================================================
                // Disabled until isFormValid is true — no separate
                // validation-error message. This recomputes on every
                // keystroke since isFormValid is derived state, so the
                // button enables itself the moment the last required
                // field becomes valid.

                Button(
                    onClick = {
                        confirmPassword = ""
                        confirmPasswordError = null
                        showConfirmPasswordDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isBusy && isFormValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Create account",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }


                // =================================================
                // BOTTOM SPACING
                // =================================================

                Spacer(
                    Modifier.height(24.dp)
                )

            }
        }



        if (showConfirmPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isBusy) {
                        showConfirmPasswordDialog = false
                        confirmPassword = ""
                        confirmPasswordError = null
                    }
                },

                title = {
                    Text(
                        text = "Confirm password",
                        fontWeight = FontWeight.Bold
                    )
                },

                text = {
                    Column {

                        Text(
                            text = "Please enter your password again to confirm your account.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            Modifier.height(16.dp)
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                confirmPasswordError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Confirm password")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            isError = confirmPasswordError != null,
                            supportingText = {
                                confirmPasswordError?.let { error ->
                                    Text(error)
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },

                confirmButton = {
                    TextButton(
                        onClick = {

                            when {
                                confirmPassword.isBlank() -> {
                                    confirmPasswordError =
                                        "Please confirm your password."
                                }

                                password != confirmPassword -> {
                                    confirmPasswordError =
                                        "Passwords do not match."
                                }

                                else -> {
                                    showConfirmPasswordDialog = false
                                    confirmPasswordError = null

                                    organisationAuthViewModel.signUp(
                                        email = email,
                                        password = password,
                                        organisationName = organisationName,
                                        contactPhone = combinedPhone,
                                        locationName = locationName,
                                        stateRegion = stateRegion,
                                        country = country,
                                        organisationType = organisationType
                                    )
                                }
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                },

                dismissButton = {
                    TextButton(
                        onClick = {
                            showConfirmPasswordDialog = false
                            confirmPassword = ""
                            confirmPasswordError = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }


    }
}