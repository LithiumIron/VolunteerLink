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


// Organisation type options and country/state/location data now live in
// OrganisationSignUpOptions.kt (same package, no import needed).



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


        // ====================================================
        // SCROLLABLE FORM
        // ====================================================

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {

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
                colors = organisationFieldColours()
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
                    colors = organisationFieldColours()
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
                colors = organisationFieldColours()
            )


            Spacer(
                Modifier.height(13.dp)
            )


            // =================================================
            // PHONE
            // =================================================

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
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
                placeholder = {
                    Text("e.g. 0123456789")
                },
                singleLine = true,
                enabled = !isBusy,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                colors = organisationFieldColours()
            )


            Spacer(
                Modifier.height(13.dp)
            )


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
                    colors = organisationFieldColours()
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
                colors = organisationFieldColours()
            )


            // =================================================
            // EMAIL CONFIRMATION MESSAGE
            // =================================================

            if (uiState.needsEmailConfirmation) {

                Spacer(
                    Modifier.height(11.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {

                    Text(
                        text = "Account created. Check your email to confirm it, " +
                                "then sign in.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            // =================================================
            // ERROR MESSAGE
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

            Button(
                onClick = {

                    organisationAuthViewModel.signUp(
                        email = email,
                        password = password,
                        organisationName = organisationName,
                        contactPhone = phone,
                        locationName = locationName,
                        stateRegion = stateRegion,
                        country = country,
                        organisationType = organisationType
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isBusy,
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
}