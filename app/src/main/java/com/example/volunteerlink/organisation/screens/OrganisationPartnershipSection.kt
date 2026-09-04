package com.example.volunteerlink.organisation.screens

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements the Organisation UI associated with Organisation Partnership Section.
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


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.data.ai.GroqService
import com.example.volunteerlink.data.ai.OrganisationSupportAnalysis
import com.example.volunteerlink.data.location.GeoapifyLocationService
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.create.components.LocationAutocompleteField
import com.example.volunteerlink.organisation.repository.OrganisationProfileData
import com.example.volunteerlink.organisation.repository.OrganisationProfileRepository
import com.example.volunteerlink.organisation.repository.OrganisationSupportData
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Holds the values represented by partnership support item as one strongly typed model.
 * It supports the Impact Weave and partnership presentation layer without adding backend responsibilities to the screen.
 */
/**
 * DETAILED DECLARATION — PartnershipSupportItem
 *
 * Domain/UI type for Partnership Support Item used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipSupportItem(
    val id: String,
    val supportDescription: String,
    val supportType: String,
    val resourceName: String,
    val amount: Int?,
    val venueLocation: LocationSuggestion? = null
)

@Composable
/**
 * Renders the organisation partnership section section used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — OrganisationPartnershipSection
 *
 * Renders the reusable Organisation Partnership Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 *
 * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than blocking
 * the UI thread.
 */
fun OrganisationPartnershipSection(
    profileData: OrganisationProfileData
) {
    val scope = rememberCoroutineScope()
    var openToPartnership by remember(profileData.organisationId) {
        mutableStateOf(profileData.openToPartnership)
    }
    val supports = remember(profileData.organisationId) {
        mutableStateListOf<PartnershipSupportItem>().apply {
            addAll(profileData.supports.map { it.toPartnershipSupportItem() })
        }
    }

    var showSupportSheet by remember { mutableStateOf(false) }
    var supportsExpanded by remember { mutableStateOf(false) }
    var editingSupport by remember { mutableStateOf<PartnershipSupportItem?>(null) }
    var supportToRemove by remember { mutableStateOf<PartnershipSupportItem?>(null) }
    var partnershipError by remember { mutableStateOf<String?>(null) }
    var isSavingToggle by remember { mutableStateOf(false) }
    var isSavingSupport by remember { mutableStateOf(false) }
    var isRemovingSupport by remember { mutableStateOf(false) }
    var supportSaveError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profileData.openToPartnership) {
        openToPartnership = profileData.openToPartnership
    }

    LaunchedEffect(profileData.supports) {
        supports.clear()
        supports.addAll(profileData.supports.map { it.toPartnershipSupportItem() })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp)
    ) {
        Text(
            text = "Partnerships",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = VolunteerLinkBorderColour,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Open to partnerships",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Let other organisations discover what you can contribute through Impact Weave.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = openToPartnership,
                enabled = !isSavingToggle,
                onCheckedChange = { checked ->
                    val previousValue = openToPartnership
                    openToPartnership = checked
                    partnershipError = null

                    scope.launch {
                        isSavingToggle = true
                        val saved = OrganisationProfileRepository
                            .updateOpenToPartnership(checked)

                        if (!saved) {
                            openToPartnership = previousValue
                            partnershipError = "Could not update partnership availability. Please try again."
                        }

                        isSavingToggle = false
                    }
                }
            )
        }

        partnershipError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = supports.isNotEmpty()) {
                        supportsExpanded = !supportsExpanded
                    }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "What we can provide",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (supports.isEmpty()) {
                        "Resources your organisation can contribute to a partnership."
                    } else {
                        "${supports.size} ${if (supports.size == 1) "item" else "items"} · ${if (supportsExpanded) "Tap to hide" else "Tap to expand"}"
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = {
                    editingSupport = null
                    supportSaveError = null
                    showSupportSheet = true
                }
            ) {
                Text(
                    text = "+ Add",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (!openToPartnership && supports.isNotEmpty()) {
            Text(
                text = "These resources stay saved, but Impact Weave will ignore them while partnerships are turned off.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (supports.isEmpty()) {
            Text(
                text = "No support added yet. Try vans, chairs, equipment, refreshments, a venue, or specialist support.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else if (supportsExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                supports.forEachIndexed { index, support ->
                    PartnershipSupportRow(
                        support = support,
                        onEdit = {
                            editingSupport = support
                            supportSaveError = null
                            showSupportSheet = true
                        },
                        onRemove = { supportToRemove = support }
                    )

                    if (index < supports.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }

    if (showSupportSheet) {
        AddEditSupportSheet(
            support = editingSupport,
            isSaving = isSavingSupport,
            saveError = supportSaveError,
            onDismiss = {
                if (!isSavingSupport) {
                    showSupportSheet = false
                    editingSupport = null
                    supportSaveError = null
                }
            },
            onSave = { supportDescription, supportType, resourceName, amount, venueLocation ->
                val existing = editingSupport
                supportSaveError = null

                scope.launch {
                    isSavingSupport = true

                    val savedSupport = if (existing == null) {
                        OrganisationProfileRepository.addSupport(
                            supportDescription = supportDescription,
                            supportType = supportType,
                            resourceName = resourceName,
                            amount = amount,
                            locationName = venueLocation?.address
                                ?.takeIf { it.isNotBlank() }
                                ?: venueLocation?.displayName,
                            country = venueLocation?.country,
                            latitude = venueLocation?.latitude,
                            longitude = venueLocation?.longitude
                        )
                    } else {
                        OrganisationProfileRepository.updateSupport(
                            supportId = existing.id,
                            supportDescription = supportDescription,
                            supportType = supportType,
                            resourceName = resourceName,
                            amount = amount,
                            locationName = venueLocation?.address
                                ?.takeIf { it.isNotBlank() }
                                ?: venueLocation?.displayName,
                            country = venueLocation?.country,
                            latitude = venueLocation?.latitude,
                            longitude = venueLocation?.longitude
                        )
                    }

                    if (savedSupport == null) {
                        supportSaveError = "Could not save this support right now. Please try again."
                    } else {
                        val savedItem = savedSupport.toPartnershipSupportItem()

                        if (existing == null) {
                            supports.add(savedItem)
                        } else {
                            val index = supports.indexOfFirst { it.id == existing.id }
                            if (index >= 0) {
                                supports[index] = savedItem
                            }
                        }

                        showSupportSheet = false
                        editingSupport = null
                        supportSaveError = null
                        supportsExpanded = true
                    }

                    isSavingSupport = false
                }
            }
        )
    }

    supportToRemove?.let { support ->
        AlertDialog(
            onDismissRequest = { supportToRemove = null },
            title = { Text("Remove support?") },
            text = {
                Text(
                    "${support.resourceName} will be removed from what your organisation can provide."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isRemovingSupport,
                    onClick = {
                        scope.launch {
                            isRemovingSupport = true
                            partnershipError = null

                            val removed = OrganisationProfileRepository
                                .removeSupport(support.id)

                            if (removed) {
                                supports.removeAll { it.id == support.id }
                                supportToRemove = null
                            } else {
                                partnershipError = "Could not remove this support right now. Please try again."
                                supportToRemove = null
                            }

                            isRemovingSupport = false
                        }
                    }
                ) {
                    Text(
                        text = if (isRemovingSupport) "Removing..." else "Remove",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isRemovingSupport,
                    onClick = { supportToRemove = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
/**
 * Renders the partnership support row row used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — PartnershipSupportRow
 *
 * Renders the reusable Partnership Support Row portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
private fun PartnershipSupportRow(
    support: PartnershipSupportItem,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val typeLabel = supportTypeLabel(support.supportType)
    val amountText = if (support.supportType == "VENUE") {
        support.amount?.let { "Capacity $it" } ?: "Capacity not specified"
    } else {
        support.amount?.let { "$it available" } ?: "Quantity not specified"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = support.resourceName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$typeLabel · $amountText",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (support.supportType == "VENUE") {
                support.venueLocation?.let { location ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = location.displayName,
                        fontSize = 11.sp,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        TextButton(onClick = onEdit) {
            Text(
                text = "Edit",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        TextButton(onClick = onRemove) {
            Text(
                text = "Remove",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Adds the edit support sheet to the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — AddEditSupportSheet
 *
 * Handles the Compose/UI responsibility for add edit support sheet.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun AddEditSupportSheet(
    support: PartnershipSupportItem?,
    isSaving: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (
        supportDescription: String,
        supportType: String,
        resourceName: String,
        amount: Int?,
        venueLocation: LocationSuggestion?
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    val groqService = remember { GroqService() }
    val locationService = remember { GeoapifyLocationService() }

    var description by remember(support) {
        mutableStateOf(support?.supportDescription.orEmpty())
    }

    var analysis by remember(support) {
        mutableStateOf<OrganisationSupportAnalysis?>(
            support?.let {
                OrganisationSupportAnalysis(
                    isValid = true,
                    supportType = it.supportType,
                    resourceName = it.resourceName,
                    quantity = if (it.supportType == "VENUE") null else it.amount,
                    capacity = if (it.supportType == "VENUE") it.amount else null,
                    reason = null
                )
            }
        )
    }

    var resourceName by remember(support) {
        mutableStateOf(support?.resourceName ?: "")
    }
    var amountText by remember(support) {
        mutableStateOf(support?.amount?.toString() ?: "")
    }
    var isAnalysing by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    var showErrors by remember { mutableStateOf(false) }

    var venueQuery by remember(support) {
        mutableStateOf(support?.venueLocation?.displayName ?: "")
    }
    var venueLocation by remember(support) {
        mutableStateOf(support?.venueLocation)
    }
    var venueSuggestions by remember {
        mutableStateOf<List<LocationSuggestion>>(emptyList())
    }
    var isSearchingVenue by remember { mutableStateOf(false) }
    var venueSearchError by remember { mutableStateOf<String?>(null) }

    val supportType = analysis?.supportType
    val isVenue = supportType == "VENUE"
    val amount = amountText.toIntOrNull()
    val amountError = showErrors && if (isVenue) {
        amountText.isNotBlank() && (amount == null || amount <= 0)
    } else {
        amount == null || amount <= 0
    }
    val venueError = if (showErrors && isVenue && venueLocation == null) {
        "Select the venue location from the suggestions."
    } else {
        null
    }

    LaunchedEffect(venueQuery, venueLocation, isVenue) {
        if (!isVenue || venueLocation != null) {
            venueSuggestions = emptyList()
            isSearchingVenue = false
            return@LaunchedEffect
        }

        val cleanQuery = venueQuery.trim()
        if (cleanQuery.length < 3) {
            venueSuggestions = emptyList()
            venueSearchError = null
            isSearchingVenue = false
            return@LaunchedEffect
        }

        delay(350)
        isSearchingVenue = true
        venueSearchError = null

        try {
            venueSuggestions = locationService.searchVenues(cleanQuery)
            if (venueSuggestions.isEmpty()) {
                venueSearchError = "No matching venues found."
            }
        } catch (_: Exception) {
            venueSuggestions = emptyList()
            venueSearchError = "Unable to search locations right now."
        } finally {
            isSearchingVenue = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            if (analysis == null) {
                Text(
                    text = if (support == null) {
                        "Add partnership support"
                    } else {
                        "Change partnership support"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Describe one thing your organisation can contribute. We’ll arrange the details for you.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        analysisError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What can you provide?") },
                    placeholder = { Text("e.g. 2 passenger vans") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isAnalysing
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Add one type of support at a time. Try: 50 folding chairs, 2 English lecturers, or a community hall for 100 people.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                analysisError?.let { error ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (description.isBlank()) {
                            analysisError = "Describe what your organisation can provide first."
                        } else {
                            scope.launch {
                                isAnalysing = true
                                analysisError = null

                                try {
                                    val result = groqService.analyseOrganisationSupport(description)

                                    if (result.isValid) {
                                        analysis = result
                                        resourceName = result.resourceName
                                        amountText = (
                                            if (result.supportType == "VENUE") {
                                                result.capacity
                                            } else {
                                                result.quantity
                                            }
                                        )?.toString().orEmpty()

                                        if (result.supportType != "VENUE") {
                                            venueLocation = null
                                            venueQuery = ""
                                        }
                                    } else {
                                        analysisError = result.reason
                                    }
                                } catch (exception: Exception) {
                                    val errorMessage = exception.message.orEmpty()
                                    analysisError = when {
                                        errorMessage.contains("GROQ_API_KEY is missing") ->
                                            "Support checking is not configured on this build."

                                        errorMessage.contains("401") ->
                                            "Support checking is unavailable right now. Please try again."

                                        errorMessage.contains("403") ->
                                            "Support checking is unavailable right now. Please try again."

                                        errorMessage.contains("404") ->
                                            "Support checking is unavailable right now. Please try again."

                                        else ->
                                            "Could not check this support right now. Check your connection and try again."
                                    }
                                } finally {
                                    isAnalysing = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAnalysing
                ) {
                    if (isAnalysing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Checking...")
                    } else {
                        Text("Check support")
                    }
                }
            } else {
                Text(
                    text = if (support == null) "Review support" else "Edit support",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Check the details below before saving.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Support details",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = resourceName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = supportTypeLabel(supportType.orEmpty()),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            amountText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(if (isVenue) "Capacity (optional)" else "Quantity available")
                    },
                    placeholder = {
                        Text(if (isVenue) "e.g. 100" else "e.g. 2")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isSaving,
                    isError = amountError,
                    supportingText = when {
                        amountError -> {
                            {
                                Text(
                                    if (isVenue) {
                                        "Enter a capacity greater than 0, or leave it blank if unknown."
                                    } else {
                                        "Enter a quantity greater than 0."
                                    }
                                )
                            }
                        }
                        isVenue -> {
                            { Text("Leave blank if this venue does not have a known fixed capacity.") }
                        }
                        else -> null
                    }
                )

                if (isVenue) {
                    Spacer(modifier = Modifier.height(10.dp))

                    LocationAutocompleteField(
                        query = venueQuery,
                        selectedLocation = venueLocation,
                        suggestions = venueSuggestions,
                        isSearching = isSearchingVenue,
                        searchError = venueSearchError,
                        validationError = venueError,
                        label = "Venue location",
                        placeholder = "Search venue or address",
                        onQueryChanged = {
                            venueQuery = it
                            venueLocation = null
                        },
                        onLocationSelected = { location ->
                            venueLocation = location
                            venueQuery = location.displayName
                            venueSuggestions = emptyList()
                            venueSearchError = null
                        },
                        onClearLocation = {
                            venueLocation = null
                            venueQuery = ""
                        },
                        enabled = !isSaving
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        analysis = null
                        analysisError = null
                        showErrors = false
                    }
                ) {
                    Text("Change description and check again")
                }

                saveError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        showErrors = true

                        val amountIsValid = if (isVenue) {
                            amountText.isBlank() || (amount != null && amount > 0)
                        } else {
                            amount != null && amount > 0
                        }

                        if (amountIsValid && (!isVenue || venueLocation != null)) {
                            onSave(
                                description.trim(),
                                supportType.orEmpty(),
                                resourceName.trim(),
                                amount,
                                if (isVenue) venueLocation else null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Saving...")
                    } else {
                        Text(if (support == null) "Add to profile" else "Save changes")
                    }
                }
            }
        }
    }
}

/**
 * Returns the support type label used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — supportTypeLabel
 *
 * Handles the Compose/UI responsibility for support type label.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun supportTypeLabel(value: String): String {
    return when (value) {
        "VENUE" -> "Venue"
        "EQUIPMENT" -> "Equipment"
        "FURNITURE" -> "Furniture"
        "TRANSPORT" -> "Transport"
        "SUPPLIES" -> "Supplies"
        "REFRESHMENTS" -> "Refreshments"
        "SPECIALIST" -> "Specialist"
        else -> value
    }
}

/**
 * Derives the organisation support data value used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — toPartnershipSupportItem
 *
 * Renders the reusable to Partnership Support Item portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 *
 * Works with structured location suggestions/coordinates so free-text search is separated from the final
 * location fields saved with the post/plan.
 */
private fun OrganisationSupportData.toPartnershipSupportItem(): PartnershipSupportItem {
    val amount = if (supportType == "VENUE") {
        capacity
    } else {
        quantity
    }

    val venueLocation = if (
        supportType == "VENUE" &&
        !locationName.isNullOrBlank() &&
        latitude != null &&
        longitude != null
    ) {
        LocationSuggestion(
            placeId = supportId,
            name = locationName,
            address = locationName,
            city = null,
            state = null,
            country = country,
            latitude = latitude,
            longitude = longitude
        )
    } else {
        null
    }

    return PartnershipSupportItem(
        id = supportId,
        supportDescription = supportDescription,
        supportType = supportType,
        resourceName = resourceName,
        amount = amount,
        venueLocation = venueLocation
    )
}
