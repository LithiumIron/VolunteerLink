package com.example.volunteerlink.organisation.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.volunteerlink.data.saveProfileImage
import com.example.volunteerlink.organisation.OrganisationTypeOptions
import com.example.volunteerlink.organisation.auth.OrganisationSessionStore
import com.example.volunteerlink.organisation.countryStates
import com.example.volunteerlink.organisation.organisationFieldColours
import com.example.volunteerlink.organisation.repository.OrganisationProfileRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
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
    var contactPhone by remember { mutableStateOf(cachedProfile?.contactPhone ?: "") }
    var contactEmail by remember { mutableStateOf(cachedProfile?.contactEmail ?: "") }
    var description by remember { mutableStateOf(cachedProfile?.description ?: "") }
    var profileImageUrl by remember { mutableStateOf(cachedProfile?.profileImageUrl) }

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

                // LOGIN EMAIL (read-only — belongs to auth.users, not
                // editable from here, same reasoning as the volunteer
                // edit screen)
                // Contact email below is the separate, genuinely editable
                // business contact address stored on organisations.

                // CONTACT EMAIL
                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contact email") },
                    singleLine = true,
                    enabled = !isSaving,
                    placeholder = { Text("Shown to volunteers — can differ from your login email") },
                    colors = organisationFieldColours()
                )

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
                }

                Spacer(Modifier.height(13.dp))

                // DESCRIPTION
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
                                    contactPhone = contactPhone,
                                    contactEmail = contactEmail,
                                    description = description,
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