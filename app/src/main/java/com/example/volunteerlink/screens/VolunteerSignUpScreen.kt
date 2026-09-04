package com.example.volunteerlink.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.data.location.CurrentLocationResolver
import com.example.volunteerlink.shared.SharedOtpVerificationSection
import com.example.volunteerlink.shared.authFieldColours
import com.example.volunteerlink.shared.countryCallingCodes
import com.example.volunteerlink.shared.countryStates
import com.example.volunteerlink.shared.isValidAuthPhoneNumber

// Purpose: Checks is volunteer sign up form valid before allowing the next authentication or application step.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
private fun isVolunteerSignUpFormValid(
    fullName: String,
    email: String,
    combinedPhone: String,
    password: String
): Boolean {
    return fullName.isNotBlank() &&
            email.isNotBlank() &&
            isValidAuthPhoneNumber(combinedPhone.trim()) &&
            password.length >= 6
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Purpose: Collects volunteer identity, location, phone and password, then confirms the password before registration.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
fun VolunteerSignUpScreen(
    onBackSelected: () -> Unit,
    onSignedUp: () -> Unit,
    volunteerAuthViewModel: VolunteerAuthViewModel = viewModel()
) {
    val uiState by volunteerAuthViewModel.uiState
        .collectAsStateWithLifecycle()

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    // Holds ONLY the local number the user types (e.g. "12-1234567") —
    // never the country calling code. The code is shown as a fixed,
    // non-editable OutlinedTextField "prefix" instead of living inside
    // this value, so there's nothing here for the user to backspace
    // through or delete.
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }

    var showConfirmPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var confirmPasswordError by rememberSaveable { mutableStateOf<String?>(null) }

    var country by rememberSaveable { mutableStateOf("") }
    var stateRegion by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }

    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var isResolvingCurrentLocation by rememberSaveable { mutableStateOf(false) }
    var currentLocationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var cancelCurrentLocationRequest by remember { mutableStateOf<(() -> Unit)?>(null) }

    var isCountryMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isStateMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isCityMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // Keep the selected location level so the next dependent location/phone choice can be validated.
    val availableStates =
        countryStates[country]?.keys?.toList() ?: emptyList()

    // Keep the calculated available cities value because later validation or Compose content reuses it.
    val availableCities =
        countryStates[country]?.get(stateRegion) ?: emptyList()

    // Keep the selected location level so the next dependent location/phone choice can be validated.
    val locationContext = LocalContext.current
    // Keep the selected location level so the next dependent location/phone choice can be validated.
    val locationScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { cancelCurrentLocationRequest?.invoke() }
    }

    // Keep the selected location level so the next dependent location/phone choice can be validated.
    val locationInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(locationInteractionSource) {
        locationInteractionSource.interactions.collect {
            if (it is PressInteraction.Release) showLocationDialog = true
        }
    }

    // Prepare the phone value and country calling code before validation and account creation.
    val phoneCallingCode = countryCallingCodes[country] ?: ""

    // Prepare the phone value and country calling code before validation and account creation.
    val combinedPhone = "$phoneCallingCode $phone".trim()

    // Prepare the phone value and country calling code before validation and account creation.
    val phoneError = phone.isNotBlank() && !isValidAuthPhoneNumber(combinedPhone)

    // Purpose: Handles begin current location resolution as a focused step shared by this Volunteer flow.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
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
                city = match.locationName
            }
        }
    }

    // Keep the selected location level so the next dependent location/phone choice can be validated.
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

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onSignedUp()
    }

    // Calculate this Boolean once so every following UI branch uses the same decision.
    val isBusy = uiState.isSubmitting || uiState.isCheckingSession

    // Calculate this Boolean once so every following UI branch uses the same decision.
    val isFormValid = isVolunteerSignUpFormValid(
        fullName = fullName,
        email = email,
        combinedPhone = combinedPhone,
        password = password
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        IconButton(onClick = onBackSelected, enabled = !isBusy) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (uiState.needsEmailConfirmation) {
            SharedOtpVerificationSection(
                email = uiState.pendingVerificationEmail.orEmpty(),
                otpCode = otpCode,
                onOtpCodeChange = {
                    otpCode = it
                    volunteerAuthViewModel.clearError()
                },
                isBusy = isBusy,
                errorMessage = uiState.errorMessage,
                onVerify = { volunteerAuthViewModel.verifySignUpOtp(otpCode) },
                onUseDifferentEmail = { volunteerAuthViewModel.cancelSignUpVerification() },
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
                Text(
                    text = "Create a volunteer account",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    text = "Find opportunities and track your verified volunteering hours.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(26.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        volunteerAuthViewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full name") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                    },
                    singleLine = true,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = authFieldColours()
                )

                Spacer(Modifier.height(13.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        volunteerAuthViewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email address") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Mail, contentDescription = null)
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

                Spacer(Modifier.height(13.dp))

                // =================================================
                // USE CURRENT LOCATION (optional shortcut — manual
                // cascade picker below still works exactly as before)
                // =================================================

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
                // country -> state/region -> city cascade — same as
                // OrganisationSignUpScreen)
                // =================================================

                Box {
                    OutlinedTextField(
                        value = when {
                            city.isNotEmpty() && stateRegion.isNotEmpty() && country.isNotEmpty() ->
                                "$city, $stateRegion, $country"
                            country.isNotEmpty() -> country
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isBusy,
                        interactionSource = locationInteractionSource,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Location (optional)") },
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        trailingIcon = { Text("›") },
                        placeholder = { Text("Select location") },
                        shape = RoundedCornerShape(12.dp),
                        colors = authFieldColours()
                    )

                    if (showLocationDialog) {
                        AlertDialog(
                            onDismissRequest = { showLocationDialog = false },
                            title = { Text("Select location") },
                            text = {
                                Column {
                                    // COUNTRY
                                    Text(text = "Country", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCountryMenuExpanded)
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
                                                        // phone (the local number) is left
                                                        // untouched here — only the fixed
                                                        // prefix shown on the field changes,
                                                        // since it's derived from `country`.
                                                        country = selectedCountry
                                                        stateRegion = ""
                                                        city = ""
                                                        isCountryMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // STATE / REGION
                                    Text(text = "State / region", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStateMenuExpanded)
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
                                                        city = ""
                                                        isStateMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // CITY
                                    Text(text = "City", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.height(6.dp))

                                    ExposedDropdownMenuBox(
                                        expanded = isCityMenuExpanded,
                                        onExpandedChange = {
                                            if (stateRegion.isNotEmpty()) isCityMenuExpanded = it
                                        }
                                    ) {
                                        OutlinedTextField(
                                            value = city,
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = stateRegion.isNotEmpty(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(),
                                            placeholder = {
                                                Text(if (stateRegion.isEmpty()) "Select state first" else "Select city")
                                            },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCityMenuExpanded)
                                            },
                                            singleLine = true
                                        )

                                        ExposedDropdownMenu(
                                            expanded = isCityMenuExpanded,
                                            onDismissRequest = { isCityMenuExpanded = false }
                                        ) {
                                            availableCities.forEach { selectedCity ->
                                                DropdownMenuItem(
                                                    text = { Text(selectedCity) },
                                                    onClick = {
                                                        city = selectedCity
                                                        isCityMenuExpanded = false
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
                                        // Location is optional overall, but if the user has
                                        // started picking, require the full chain before closing.
                                        if (country.isEmpty() || (stateRegion.isNotEmpty() && city.isEmpty())) {
                                            // leave dialog open until chain is complete or cleared
                                        }
                                        showLocationDialog = false
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
                }

                Spacer(Modifier.height(13.dp))

                // =================================================
                // PHONE — the calling code is a fixed `prefix` on the
                // field itself, not part of `value`. It still reads as
                // one continuous "+60 12-1234567" field visually, but
                // the user's cursor/selection/backspace can never reach
                // the code, since `phone` only ever holds the local
                // number.
                // =================================================

                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        phone = input.filter { c -> c.isDigit() || c == '-' }
                        volunteerAuthViewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contact phone") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Phone, contentDescription = null)
                    },
                    prefix = if (phoneCallingCode.isNotEmpty()) {
                        { Text("$phoneCallingCode ") }
                    } else {
                        null
                    },
                    placeholder = { Text("e.g. 12-456789") },
                    singleLine = true,
                    enabled = !isBusy,
                    isError = phoneError,
                    supportingText = {
                        Text(
                            text = when {
                                phoneCallingCode.isEmpty() ->
                                    "Select a location above to set your country code."
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



                Spacer(Modifier.height(13.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        volunteerAuthViewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
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

                uiState.errorMessage?.let { message ->
                    Spacer(Modifier.height(11.dp))
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(22.dp))

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
                        Text(text = "Create account", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(24.dp))
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

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                confirmPasswordError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm password") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            isError = confirmPasswordError != null,
                            supportingText = {
                                confirmPasswordError?.let { error -> Text(error) }
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
                                    confirmPasswordError = "Please confirm your password."
                                }
                                password != confirmPassword -> {
                                    confirmPasswordError = "Passwords do not match."
                                }
                                else -> {
                                    showConfirmPasswordDialog = false
                                    confirmPasswordError = null

                                    volunteerAuthViewModel.signUp(
                                        fullName = fullName,
                                        email = email,
                                        phone = combinedPhone,
                                        password = password,
                                        locationName = city,
                                        stateRegion = stateRegion,
                                        country = country
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