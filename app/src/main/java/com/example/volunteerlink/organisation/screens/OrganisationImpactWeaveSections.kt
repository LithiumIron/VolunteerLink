package com.example.volunteerlink.organisation.screens

// FILE OVERVIEW:
/*
 * OrganisationImpactWeaveSections contains presentation code for the organisation Impact Weave and partnership flow.
 * It focuses on rendering state and forwarding user actions through callbacks/ViewModels,
 * keeping database access and business rules outside the composables where possible.
 */


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.volunteerlink.R
import com.example.volunteerlink.data.ai.GroqService
import com.example.volunteerlink.data.ai.OrganisationSupportAnalysis
import com.example.volunteerlink.data.location.GeoapifyLocationService
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.components.OrganisationPrimaryButton
import com.example.volunteerlink.organisation.components.OrganisationSectionHeader
import com.example.volunteerlink.organisation.components.OrganisationSectionSurface
import com.example.volunteerlink.organisation.components.OrganisationStatusPill
import com.example.volunteerlink.organisation.create.components.CreateSectionCard
import com.example.volunteerlink.organisation.create.components.CategoryPicker
import com.example.volunteerlink.organisation.create.components.DateSelectionField
import com.example.volunteerlink.organisation.create.components.TimeSelectionField
import com.example.volunteerlink.organisation.create.components.formatTime
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveActivePlan
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDuration
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMatchResults
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMode
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveNeedMatchResult
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveNeedDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePartnershipState
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveSupportCandidate
import com.example.volunteerlink.organisation.repository.PartnershipRequestItem
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
/**
 * Renders the impact weave landing screen screen used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun ImpactWeaveLandingScreen(
    activePlans: List<ImpactWeaveActivePlan>,
    isLoadingActivePlans: Boolean,
    activePlansError: String?,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onOpenPlan: (ImpactWeaveActivePlan) -> Unit,
    onRetryLoad: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "impact_header") {
            OrganisationManageSubHeader(
                title = "Impact Weave",
                onBack = onBack
            )
        }

        item(key = "impact_intro") {
            OrganisationSectionSurface(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp
                )
            ) {
                Text(
                    text = "Plan an activity with partner organisations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = "Describe the activity and support you need, review it, then find organisations that may be able to help.",
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextSecondary
                )
                Text(
                    text = "Impact Weave activities must start at least 10 days ahead: a short partnership-planning period plus the existing 7-day volunteer recruitment period.",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = VolunteerLinkTextSecondary
                )
                OrganisationPrimaryButton(
                    text = "Start Impact Weave",
                    onClick = onStart,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        item(key = "active_plans_header") {
            OrganisationSectionHeader(
                title = "Plans & history",
                subtitle = "Active, converted and disposed plans remain available here.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
        }

        when {
            isLoadingActivePlans && activePlans.isEmpty() -> {
                item(key = "active_plans_loading") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CreateGreen,
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }

            activePlansError != null && activePlans.isEmpty() -> {
                item(key = "active_plans_error") {
                    OrganisationSectionSurface(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = activePlansError,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = onRetryLoad,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            activePlans.isEmpty() -> {
                item(key = "active_plans_empty") {
                    Text(
                        text = "No Impact Weave plans yet.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }

            else -> {
                items(
                    items = activePlans,
                    key = { it.draftId }
                ) { plan ->
                    ImpactWeaveActivePlanCard(
                        plan = plan,
                        onClick = { onOpenPlan(plan) }
                    )
                }
            }
        }
    }
}

@Composable
/**
 * Renders the impact weave active plan card card used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ImpactWeaveActivePlanCard(
    plan: ImpactWeaveActivePlan,
    onClick: () -> Unit
) {
    OrganisationSectionSurface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${plan.mode.displayName} · ${formatDate(plan.startDateMillis)}",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
            OrganisationStatusPill(
                text = plan.status.lowercase().replaceFirstChar { it.titlecase() },
                color = CreateGreen
            )
        }

        Text(
            text = if (plan.needsCount == 1) "1 support need" else "${plan.needsCount} support needs",
            modifier = Modifier.padding(top = 10.dp),
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary
        )
        Text(
            text = if (plan.status.equals("DISPOSED", true) || plan.status.equals("CONVERTED", true)) {
                "Tap to view read-only history"
            } else {
                "Tap to view current partner matches"
            },
            modifier = Modifier.padding(top = 5.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = CreateGreen
        )
    }
}

@Composable
/**
 * Renders the impact weave activity plan screen screen used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun ImpactWeaveActivityPlanScreen(
    draft: ImpactWeaveDraft,
    minimumStartDateMillis: Long,
    planningDeadlineMillis: Long?,
    onBack: () -> Unit,
    onCategorySelected: (VolunteerPostCategory) -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onModeSelected: (ImpactWeaveMode) -> Unit,
    onDurationSelected: (ImpactWeaveDuration) -> Unit,
    onStartDateSelected: (Long) -> Unit,
    onEndDateSelected: (Long) -> Unit,
    onStartTimeSelected: (Int, Int) -> String?,
    onEndTimeSelected: (Int, Int) -> String?,
    onAreaQueryChanged: (String) -> Unit,
    onAreaSelected: (LocationSuggestion) -> Unit,
    onAreaCleared: () -> Unit,
    onHasExistingVenueChanged: (Boolean) -> Unit,
    onVenueQueryChanged: (String) -> Unit,
    onVenueSelected: (LocationSuggestion) -> Unit,
    onVenueCleared: () -> Unit,
    errorsProvider: () -> Map<String, String>,
    onContinue: () -> Boolean
) {
    val locationService = remember { GeoapifyLocationService() }
    var areaSuggestions by remember(draft.draftId) {
        mutableStateOf<List<LocationSuggestion>>(emptyList())
    }
    var venueSuggestions by remember(draft.draftId) {
        mutableStateOf<List<LocationSuggestion>>(emptyList())
    }
    var isSearchingArea by remember { mutableStateOf(false) }
    var isSearchingVenue by remember { mutableStateOf(false) }
    var areaSearchError by remember { mutableStateOf<String?>(null) }
    var venueSearchError by remember { mutableStateOf<String?>(null) }
    var showErrors by remember(draft.draftId) { mutableStateOf(false) }

    val errors = if (showErrors) errorsProvider() else emptyMap()

    LaunchedEffect(draft.areaQuery, draft.areaLocation, draft.hasExistingVenue) {
        if (draft.hasExistingVenue != false || draft.areaLocation != null) {
            areaSuggestions = emptyList()
            isSearchingArea = false
            return@LaunchedEffect
        }

        val query = draft.areaQuery.trim()
        if (query.length < 2) {
            areaSuggestions = emptyList()
            areaSearchError = null
            isSearchingArea = false
            return@LaunchedEffect
        }

        delay(350)
        isSearchingArea = true
        areaSearchError = null
        try {
            areaSuggestions = locationService.searchAreas(query)
            if (areaSuggestions.isEmpty()) {
                areaSearchError = "No matching city, district or general area found."
            }
        } catch (_: Exception) {
            areaSuggestions = emptyList()
            areaSearchError = "Unable to search areas right now."
        } finally {
            isSearchingArea = false
        }
    }

    LaunchedEffect(draft.venueQuery, draft.existingVenueLocation, draft.hasExistingVenue) {
        if (draft.hasExistingVenue != true || draft.existingVenueLocation != null) {
            venueSuggestions = emptyList()
            isSearchingVenue = false
            return@LaunchedEffect
        }

        val query = draft.venueQuery.trim()
        if (query.length < 3) {
            venueSuggestions = emptyList()
            venueSearchError = null
            isSearchingVenue = false
            return@LaunchedEffect
        }

        delay(350)
        isSearchingVenue = true
        venueSearchError = null
        try {
            venueSuggestions = locationService.searchEventLocations(query)
            if (venueSuggestions.isEmpty()) {
                venueSearchError = "No matching event locations found."
            }
        } catch (_: Exception) {
            venueSuggestions = emptyList()
            venueSearchError = "Unable to search event locations right now."
        } finally {
            isSearchingVenue = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "plan_header") {
            ImpactWeaveWizardHeader(
                title = "Impact Weave",
                subtitle = "Step 1 of 3 · Activity Plan",
                onBack = onBack
            )
        }

        item(key = "plan_timing_rule") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F7EE)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Plan at least 10 days ahead",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CreateGreen
                    )
                    Text(
                        text = buildString {
                            append("Earliest activity: ${formatDate(minimumStartDateMillis)}")
                            planningDeadlineMillis?.let {
                                append(" · Partnership target: ${formatDate(it)}")
                            }
                        },
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "plan_activity") {
            CreateSectionCard(
                title = "Activity Information",
                subtitle = "Give partner organisations the same clear introduction that volunteers will later see in Create Post."
            ) {
                CategoryPicker(
                    selectedCategory = draft.category,
                    onCategorySelected = onCategorySelected,
                    errorMessage = errors["category"]
                )

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = onTitleChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") },
                        placeholder = { Text("Example: Community Health Day") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        isError = errors["title"] != null
                    )
                    FormErrorText(errors["title"])
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedTextField(
                        value = draft.description,
                        onValueChange = onDescriptionChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Description") },
                        placeholder = {
                            Text("Explain the activity purpose, who it supports, what will happen, and why partnership support is needed.")
                        },
                        minLines = 4,
                        maxLines = 7,
                        shape = RoundedCornerShape(14.dp),
                        isError = errors["description"] != null
                    )
                    FormErrorText(errors["description"])
                }

                Text(
                    text = "Activity Mode",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImpactWeaveModeOption(
                        title = "Physical",
                        description = "Partnership support for an in-person activity",
                        selected = draft.mode == ImpactWeaveMode.PHYSICAL,
                        onClick = { onModeSelected(ImpactWeaveMode.PHYSICAL) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    ImpactWeaveModeOption(
                        title = "Hybrid",
                        description = "Impact Weave supports the physical side",
                        selected = draft.mode == ImpactWeaveMode.HYBRID,
                        onClick = { onModeSelected(ImpactWeaveMode.HYBRID) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                FormErrorText(errors["mode"])
            }
        }

        item(key = "plan_when") {
            CreateSectionCard(
                title = if (draft.mode == ImpactWeaveMode.HYBRID) {
                    "Physical Schedule"
                } else {
                    "Event Schedule"
                },
                subtitle = if (draft.mode == ImpactWeaveMode.HYBRID) {
                    "Set the dates and times for the physical side of this Hybrid activity."
                } else {
                    "Choose when the physical activity will take place."
                }
            ) {
                Text(
                    text = "Event Duration",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImpactWeaveSimpleOption(
                        title = "One Day",
                        selected = draft.duration == ImpactWeaveDuration.ONE_DAY,
                        onClick = { onDurationSelected(ImpactWeaveDuration.ONE_DAY) },
                        modifier = Modifier.weight(1f)
                    )
                    ImpactWeaveSimpleOption(
                        title = "Multiple Days",
                        selected = draft.duration == ImpactWeaveDuration.MULTIPLE_DAYS,
                        onClick = { onDurationSelected(ImpactWeaveDuration.MULTIPLE_DAYS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (draft.duration == ImpactWeaveDuration.ONE_DAY) {
                    DateSelectionField(
                        label = "Event Date",
                        selectedDateMillis = draft.startDateMillis,
                        minimumDateMillis = minimumStartDateMillis,
                        errorMessage = errors["startDate"],
                        onDateSelected = onStartDateSelected
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DateSelectionField(
                            label = "Start Date",
                            selectedDateMillis = draft.startDateMillis,
                            minimumDateMillis = minimumStartDateMillis,
                            errorMessage = errors["startDate"],
                            onDateSelected = onStartDateSelected,
                            modifier = Modifier.weight(1f)
                        )
                        DateSelectionField(
                            label = "End Date",
                            selectedDateMillis = draft.endDateMillis,
                            minimumDateMillis = draft.startDateMillis?.let { addDays(it, 1) }
                                ?: addDays(minimumStartDateMillis, 1),
                            errorMessage = errors["endDate"],
                            onDateSelected = onEndDateSelected,
                            enabled = draft.startDateMillis != null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimeSelectionField(
                        label = "Start Time",
                        selectedTimeMinutes = draft.startTimeMinutes,
                        errorMessage = errors["startTime"],
                        onTimeSelected = onStartTimeSelected,
                        modifier = Modifier.weight(1f)
                    )

                    val proposedEndTime = draft.startTimeMinutes?.let {
                        (it + 60).coerceAtMost(23 * 60 + 59)
                    }
                    TimeSelectionField(
                        label = "End Time",
                        selectedTimeMinutes = draft.endTimeMinutes,
                        dialogInitialTimeMinutes = draft.endTimeMinutes ?: proposedEndTime,
                        errorMessage = errors["endTime"],
                        onTimeSelected = onEndTimeSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item(key = "plan_where") {
            CreateSectionCard(
                title = if (draft.mode == ImpactWeaveMode.HYBRID) {
                    "Physical Location"
                } else {
                    "Event Location"
                },
                subtitle = if (draft.mode == ImpactWeaveMode.HYBRID) {
                    "Set the location for the physical side of this Hybrid activity."
                } else {
                    "Tell us whether the exact venue is already confirmed."
                }
            ) {
                Text(
                    text = "Do you already have a venue?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImpactWeaveSimpleOption(
                        title = "Yes",
                        selected = draft.hasExistingVenue == true,
                        onClick = { onHasExistingVenueChanged(true) },
                        modifier = Modifier.weight(1f)
                    )
                    ImpactWeaveSimpleOption(
                        title = "No",
                        selected = draft.hasExistingVenue == false,
                        onClick = { onHasExistingVenueChanged(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
                FormErrorText(errors["hasVenue"])

                if (draft.hasExistingVenue == true) {
                    Text(
                        text = "Select the exact venue. The general area will be taken from the selected location automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ImpactWeaveLocationPicker(
                        query = draft.venueQuery,
                        selectedLocation = draft.existingVenueLocation,
                        suggestions = venueSuggestions,
                        isSearching = isSearchingVenue,
                        searchError = venueSearchError,
                        validationError = errors["venue"],
                        label = "Venue",
                        placeholder = "Search venue or address",
                        selectedLabel = "Venue selected",
                        onQueryChanged = onVenueQueryChanged,
                        onLocationSelected = { location ->
                            venueSuggestions = emptyList()
                            venueSearchError = null
                            onVenueSelected(location)
                        },
                        onClearLocation = onVenueCleared
                    )

                    draft.areaLocation?.let { area ->
                        Text(
                            text = "General area: ${area.generalAreaName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (draft.hasExistingVenue == false) {
                    Text(
                        text = "Choose only the preferred general area, such as a city, town or district. The exact venue will be requested from partners in Step 2.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ImpactWeaveLocationPicker(
                        query = draft.areaQuery,
                        selectedLocation = draft.areaLocation,
                        suggestions = areaSuggestions,
                        isSearching = isSearchingArea,
                        searchError = areaSearchError,
                        validationError = errors["area"],
                        label = "Preferred general area",
                        placeholder = "Search city, town or district",
                        selectedLabel = "Preferred area",
                        onQueryChanged = onAreaQueryChanged,
                        onLocationSelected = { location ->
                            areaSuggestions = emptyList()
                            areaSearchError = null
                            onAreaSelected(location)
                        },
                        onClearLocation = onAreaCleared
                    )

                    Text(
                        text = "A Venue support need will be required before you can continue to Review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CreateGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item(key = "plan_continue") {
            Button(
                onClick = {
                    showErrors = true
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CreateGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Continue to Support Needed",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item(key = "plan_end_space") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
/**
 * Renders the impact weave wizard header header used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ImpactWeaveWizardHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        androidx.compose.material3.IconButton(onClick = onBack) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.back),
                contentDescription = "Back",
                modifier = Modifier.size(30.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CreateGreen
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
/**
 * Renders the UI represented by impact weave simple option for the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun ImpactWeaveSimpleOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFE5EFE1) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) CreateGreen else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = if (selected) CreateGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
/**
 * Renders the UI represented by impact weave mode option for the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun ImpactWeaveModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFFE5EFE1) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) CreateGreen else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) CreateGreen else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
/**
 * Renders the impact weave location picker picker used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ImpactWeaveLocationPicker(
    query: String,
    selectedLocation: LocationSuggestion?,
    suggestions: List<LocationSuggestion>,
    isSearching: Boolean,
    searchError: String?,
    validationError: String?,
    label: String,
    placeholder: String,
    selectedLabel: String,
    onQueryChanged: (String) -> Unit,
    onLocationSelected: (LocationSuggestion) -> Unit,
    onClearLocation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedLocation == null) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                isError = validationError != null
            )

            if (isSearching) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CreateGreen
                    )
                    Text(
                        text = "Searching locations...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (suggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFDCE5D8)),
                    shadowElevation = 2.dp
                ) {
                    Column {
                        Text(
                            text = "Location Suggestions",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = CreateGreen
                        )
                        suggestions.forEachIndexed { index, location ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLocationSelected(location) }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = location.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (
                                        location.address.isNotBlank() &&
                                        location.address != location.displayName
                                    ) {
                                        Text(
                                            text = location.address,
                                            modifier = Modifier.padding(top = 2.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    text = "Select",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = CreateGreen
                                )
                            }
                            if (index < suggestions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    color = Color(0xFFE8E8E8)
                                )
                            }
                        }
                    }
                }
            }

            searchError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FormErrorText(validationError)
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color(0xFFBDD4B5),
                        RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1F7EE)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = CreateGreen
                        )
                        TextButton(onClick = onClearLocation) {
                            Text("Change")
                        }
                    }
                    Text(
                        text = selectedLocation.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (
                        selectedLocation.address.isNotBlank() &&
                        selectedLocation.address != selectedLocation.displayName
                    ) {
                        Text(
                            text = selectedLocation.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Renders the impact weave support needed screen screen used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun ImpactWeaveSupportNeededScreen(
    draft: ImpactWeaveDraft,
    onBack: () -> Unit,
    onAddNeed: (String, String, String, Int?) -> Unit,
    onUpdateNeed: (Int, String, String, String, Int?) -> Unit,
    onRemoveNeed: (Int) -> Unit,
    onContinue: () -> Boolean
) {
    var showNeedSheet by remember { mutableStateOf(false) }
    var editingNeed by remember { mutableStateOf<ImpactWeaveNeedDraft?>(null) }
    var requiredSupportType by remember { mutableStateOf<String?>(null) }
    var removeNeed by remember { mutableStateOf<ImpactWeaveNeedDraft?>(null) }
    var showNeedsError by remember { mutableStateOf(false) }

    val venueRequired = draft.hasExistingVenue == false
    val hasVenueNeed = draft.needs.any { it.supportType == "VENUE" }
    val incompleteQuantityNeed = draft.needs.firstOrNull { need ->
        need.supportType != "VENUE" && (need.quantityRequired == null || need.quantityRequired <= 0)
    }
    val invalidVenueNeed = draft.needs.firstOrNull { need ->
        need.supportType == "VENUE" && need.capacityRequired != null && need.capacityRequired <= 0
    }
    val canReview = draft.needs.isNotEmpty() &&
        (!venueRequired || hasVenueNeed) &&
        incompleteQuantityNeed == null &&
        invalidVenueNeed == null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "need_header") {
            ImpactWeaveWizardHeader(
                title = "Impact Weave",
                subtitle = "Step 2 of 3 · Support Needed",
                onBack = onBack
            )
        }

        item(key = "need_form") {
            CreateSectionCard(
                title = "Support Needed",
                subtitle = "Add one physical resource or specialist need at a time. Volunteer manpower belongs in the Volunteer Post later."
            ) {
                if (venueRequired && !hasVenueNeed) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF7E8),
                        border = BorderStroke(1.dp, Color(0xFFE3C472))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Venue required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5F4815)
                            )
                            Text(
                                text = "No venue is confirmed yet. Add the type of venue you need for ${draft.areaLocation?.generalAreaName ?: "the preferred area"}. Include capacity if you know it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    editingNeed = null
                                    requiredSupportType = "VENUE"
                                    showNeedSheet = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CreateGreen)
                            ) {
                                Text(
                                    text = "Add Venue Requirement",
                                    fontWeight = FontWeight.SemiBold,
                                    color = CreateGreen
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Examples",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "2 passenger vans · 100 meal packs · 2 English lecturers · a hall for 150 people",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        editingNeed = null
                        requiredSupportType = null
                        showNeedSheet = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CreateGreen)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = CreateGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Support Need",
                        fontWeight = FontWeight.SemiBold,
                        color = CreateGreen
                    )
                }
            }
        }

        item(key = "need_list") {
            CreateSectionCard(
                title = "Added Support",
                subtitle = if (draft.needs.isEmpty()) {
                    "No support needs added yet."
                } else {
                    "${draft.needs.size} ${if (draft.needs.size == 1) "item" else "items"} added."
                }
            ) {
                if (draft.needs.isEmpty()) {
                    Text(
                        text = "Add what partner organisations could contribute to this activity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        draft.needs.forEach { need ->
                            ImpactWeaveNeedRow(
                                need = need,
                                onEdit = {
                                    editingNeed = need
                                    requiredSupportType = null
                                    showNeedSheet = true
                                },
                                onRemove = { removeNeed = need }
                            )
                        }
                    }
                }
            }
        }

        if (showNeedsError && !canReview) {
            item(key = "need_validation") {
                FormErrorText(
                    when {
                        draft.needs.isEmpty() ->
                            "Add at least one support need before continuing."

                        venueRequired && !hasVenueNeed ->
                            "Add a venue requirement before reviewing this Impact Weave plan."

                        incompleteQuantityNeed != null ->
                            "Add a quantity for ${incompleteQuantityNeed.resourceName.ifBlank { supportTypeLabel(incompleteQuantityNeed.supportType) }} before reviewing."

                        invalidVenueNeed != null ->
                            "Check the venue capacity. Leave it blank if unknown, or enter a value greater than 0."

                        else -> "Complete the required support details."
                    }
                )
            }
        }

        item(key = "need_continue") {
            Button(
                onClick = {
                    showNeedsError = true
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CreateGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Review Impact Weave",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item(key = "need_end_space") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showNeedSheet) {
        AddEditImpactWeaveNeedSheet(
            need = editingNeed,
            requiredSupportType = requiredSupportType,
            onDismiss = {
                showNeedSheet = false
                editingNeed = null
                requiredSupportType = null
            },
            onSave = { originalText, supportType, resourceName, amount ->
                val current = editingNeed
                if (current == null) {
                    onAddNeed(originalText, supportType, resourceName, amount)
                } else {
                    onUpdateNeed(
                        current.needId,
                        originalText,
                        supportType,
                        resourceName,
                        amount
                    )
                }
                showNeedSheet = false
                editingNeed = null
                requiredSupportType = null
                showNeedsError = false
            }
        )
    }

    removeNeed?.let { need ->
        AlertDialog(
            onDismissRequest = { removeNeed = null },
            title = { Text("Remove support need?") },
            text = {
                Text(
                    if (venueRequired && need.supportType == "VENUE") {
                        "${need.resourceName} will be removed. You will need another venue requirement before you can continue."
                    } else {
                        "${need.resourceName} will be removed from this plan."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveNeed(need.needId)
                        removeNeed = null
                    }
                ) {
                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { removeNeed = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
/**
 * Renders the impact weave need row row used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ImpactWeaveNeedRow(
    need: ImpactWeaveNeedDraft,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = need.resourceName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${supportTypeLabel(need.supportType)} · ${needAmountLabel(need)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onEdit) {
                Text("Edit", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onRemove) {
                Text(
                    "Remove",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Adds the edit impact weave need sheet to the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun AddEditImpactWeaveNeedSheet(
    need: ImpactWeaveNeedDraft?,
    requiredSupportType: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val checkingService = remember { GroqService() }

    var description by remember(need) {
        mutableStateOf(need?.originalText.orEmpty())
    }
    var analysis by remember(need) {
        mutableStateOf<OrganisationSupportAnalysis?>(
            need?.let {
                OrganisationSupportAnalysis(
                    isValid = true,
                    supportType = it.supportType,
                    resourceName = it.resourceName,
                    quantity = it.quantityRequired,
                    capacity = it.capacityRequired,
                    reason = null
                )
            }
        )
    }
    var amountText by remember(need) {
        mutableStateOf(need?.amount?.toString().orEmpty())
    }
    var isChecking by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var showErrors by remember { mutableStateOf(false) }

    val supportType = analysis?.supportType
    val isVenue = supportType == "VENUE"
    val amount = amountText.toIntOrNull()
    val amountError = showErrors && if (isVenue) {
        amountText.isNotBlank() && (amount == null || amount <= 0)
    } else {
        amount == null || amount <= 0
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            if (analysis == null) {
                Text(
                    text = when {
                        requiredSupportType == "VENUE" -> "Add venue requirement"
                        need == null -> "Add support need"
                        else -> "Change support need"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (requiredSupportType == "VENUE") {
                        "Describe the type of venue this activity needs. Include capacity if you know it."
                    } else {
                        "Describe one thing this activity still needs. We’ll arrange the details for you."
                    },
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        checkError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    label = { Text("What support do you need?") },
                    placeholder = {
                        Text(
                            if (requiredSupportType == "VENUE") {
                                "e.g. a hall for around 150 people"
                            } else {
                                "e.g. 2 passenger vans"
                            }
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isChecking
                )

                Text(
                    text = "Add one item at a time. General volunteers and helpers will be added later as Volunteer Post roles.",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                checkError?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
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

                Button(
                    onClick = {
                        if (description.isBlank()) {
                            checkError = "Describe the support you need first."
                        } else {
                            scope.launch {
                                isChecking = true
                                checkError = null
                                try {
                                    val result = checkingService.analyseImpactWeaveNeed(description)
                                    if (
                                        result.isValid &&
                                        requiredSupportType != null &&
                                        result.supportType != requiredSupportType
                                    ) {
                                        checkError = "This step requires a venue. Describe the type of place this activity needs."
                                    } else if (result.isValid) {
                                        analysis = result
                                        amountText = (
                                            if (result.supportType == "VENUE") {
                                                result.capacity
                                            } else {
                                                result.quantity
                                            }
                                        )?.toString().orEmpty()
                                    } else {
                                        checkError = result.reason
                                    }
                                } catch (exception: Exception) {
                                    val message = exception.message.orEmpty()
                                    checkError = when {
                                        message.contains("GROQ_API_KEY is missing") ->
                                            "Support checking is not configured on this build."
                                        message.contains("401") ||
                                            message.contains("403") ||
                                            message.contains("404") ->
                                            "Support checking is unavailable right now. Please try again."
                                        else ->
                                            "Could not check this support right now. Check your connection and try again."
                                    }
                                } finally {
                                    isChecking = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking...")
                    } else {
                        Text("Check support")
                    }
                }
            } else {
                Text(
                    text = if (need == null) "Review support need" else "Edit support need",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Check the details below before adding it to the plan.",
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Support details",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = analysis?.resourceName.orEmpty(),
                            modifier = Modifier.padding(top = 6.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = supportTypeLabel(supportType.orEmpty()),
                            modifier = Modifier.padding(top = 3.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() }) {
                            amountText = value
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    label = { Text(if (isVenue) "Capacity needed (optional)" else "Quantity needed") },
                    placeholder = { Text(if (isVenue) "e.g. 150" else "e.g. 2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
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
                            { Text("Leave blank when the venue does not have a known fixed capacity.") }
                        }
                        else -> null
                    }
                )

                TextButton(
                    onClick = {
                        analysis = null
                        checkError = null
                        showErrors = false
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Change description and check again")
                }

                Button(
                    onClick = {
                        showErrors = true
                        val amountIsValid = if (isVenue) {
                            amountText.isBlank() || (amount != null && amount > 0)
                        } else {
                            amount != null && amount > 0
                        }

                        if (amountIsValid) {
                            onSave(
                                description.trim(),
                                supportType.orEmpty(),
                                analysis?.resourceName.orEmpty().trim(),
                                amount
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(if (need == null) "Add Support" else "Save changes")
                }
            }
        }
    }
}

@Composable
/**
 * Renders the impact weave review screen screen used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun ImpactWeaveReviewScreen(
    draft: ImpactWeaveDraft,
    planningDeadlineMillis: Long?,
    onBack: () -> Unit,
    onEditActivity: () -> Unit,
    onEditNeeds: () -> Unit,
    isFindingPartners: Boolean,
    findPartnersError: String?,
    onFindPartners: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "review_header") {
            ImpactWeaveWizardHeader(
                title = "Impact Weave",
                subtitle = "Step 3 of 3 · Review",
                onBack = onBack
            )
        }

        item(key = "review_activity") {
            CreateSectionCard(
                title = "Activity Summary",
                subtitle = "Check the activity details before finding partner organisations."
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = draft.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = listOfNotNull(
                                draft.category?.displayName,
                                draft.mode?.displayName
                            ).joinToString(" · "),
                            modifier = Modifier.padding(top = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = CreateGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (draft.description.isNotBlank()) {
                            Text(
                                text = draft.description,
                                modifier = Modifier.padding(top = 7.dp),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = onEditActivity,
                        enabled = !isFindingPartners
                    ) {
                        Text("Edit")
                    }
                }

                ImpactWeaveSummaryLine(
                    iconRes = R.drawable.calendar,
                    label = if (draft.mode == ImpactWeaveMode.HYBRID) {
                        "Physical schedule"
                    } else {
                        "Schedule"
                    },
                    value = formatDraftSchedule(draft)
                )

                if (draft.hasExistingVenue == true) {
                    ImpactWeaveSummaryLine(
                        iconRes = R.drawable.ic_volunteer_location,
                        label = "Venue",
                        value = draft.existingVenueLocation?.displayName.orEmpty()
                    )
                    draft.areaLocation?.let { area ->
                        ImpactWeaveSummaryLine(
                            iconRes = R.drawable.ic_volunteer_map,
                            label = "General area",
                            value = area.generalAreaName
                        )
                    }
                } else {
                    ImpactWeaveSummaryLine(
                        iconRes = R.drawable.ic_volunteer_map,
                        label = "Preferred area",
                        value = draft.areaLocation?.generalAreaName.orEmpty()
                    )
                    ImpactWeaveSummaryLine(
                        iconRes = R.drawable.ic_volunteer_location,
                        label = "Venue",
                        value = "Requested from partners"
                    )
                }

                planningDeadlineMillis?.let { deadline ->
                    Text(
                        text = "Partnership planning target: ${formatDate(deadline)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = CreateGreen
                    )
                }
            }
        }

        item(key = "review_needs") {
            CreateSectionCard(
                title = "Support Needs",
                subtitle = "${draft.needs.size} ${if (draft.needs.size == 1) "item" else "items"} added."
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onEditNeeds,
                        enabled = !isFindingPartners
                    ) {
                        Text("Edit Support")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    draft.needs.forEach { need ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(13.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = need.resourceName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${supportTypeLabel(need.supportType)} · ${needAmountLabel(need)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "review_note") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F7EE)
            ) {
                Text(
                    text = "Find Partners saves this plan as Matching and searches real support records from organisations that are open to partnership. No invitation or acceptance is sent yet.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "review_find") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                findPartnersError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = onFindPartners,
                    enabled = !isFindingPartners,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CreateGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isFindingPartners) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Starting matching...",
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Find Partners",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item(key = "review_end_space") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
/**
 * Renders the impact weave match results screen screen used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun ImpactWeaveMatchResultsScreen(
    draft: ImpactWeaveDraft,
    results: ImpactWeaveMatchResults?,
    isLoading: Boolean,
    errorMessage: String?,
    sentOrganisationIds: Set<String>,
    partnershipStates: Map<String, ImpactWeavePartnershipState>,
    sendingOrganisationId: String?,
    requestError: String?,
    requestSuccess: String?,
    isSavingPlanChange: Boolean,
    planChangeError: String?,
    planChangeSuccess: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSendRequest: (String, String, List<PartnershipRequestItem>) -> Unit,
    onClearRequestFeedback: () -> Unit,
    onUpdateDetails: (VolunteerPostCategory, String, String) -> Unit,
    onReschedule: (Long, Long, Int, Int) -> Unit,
    onDispose: () -> Unit,
    onCreatePost: (String) -> Unit,
    onViewOrganisationProfile: (String) -> Unit,
    onClearPlanFeedback: () -> Unit,
    minimumStartDateMillis: Long,
    minimumPostDateMillis: Long
) {
    var requestCandidate by remember { mutableStateOf<ImpactWeaveSupportCandidate?>(null) }
    var showEditDetails by remember { mutableStateOf(false) }
    var showReschedule by remember { mutableStateOf(false) }
    var showDispose by remember { mutableStateOf(false) }

    LaunchedEffect(sentOrganisationIds, requestCandidate?.organisationId) {
        val organisationId = requestCandidate?.organisationId ?: return@LaunchedEffect
        if (organisationId in sentOrganisationIds) {
            requestCandidate = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "match_header") {
            ImpactWeaveWizardHeader(
                title = "Partner Matches",
                subtitle = "Suitable partner options for ${draft.title}",
                onBack = onBack
            )
        }

        if (isLoading && results == null) {
            item(key = "match_loading") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = CreateGreen)
                    Text(
                        text = "Checking partner support...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "VolunteerLink checks resource compatibility, quantities, capacities and location.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (errorMessage != null && results == null) {
            item(key = "match_error") {
                CreateSectionCard(
                    title = "Could not load partner matches",
                    subtitle = errorMessage
                ) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CreateGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        } else if (results != null) {
            val isReadOnlyPlan = draft.persistedStatus.equals("DISPOSED", true) ||
                draft.persistedStatus.equals("CONVERTED", true)
            val recommendedPartners = buildPartnerOrganisationGroups(results)
            val recommendedOrganisationIds = recommendedPartners
                .mapTo(mutableSetOf()) { it.organisationId }
            val contactedOrganisationIds = partnershipStates.keys
            val alternativePartners = buildAlternativePartnerOrganisationGroups(
                results = results,
                excludedOrganisationIds = recommendedOrganisationIds + contactedOrganisationIds
            )
            val availableRecommendedPartners = recommendedPartners.filterNot {
                it.organisationId in contactedOrganisationIds
            }
            val partnershipRequests = partnershipStates.values
                .filter { state ->
                    !isReadOnlyPlan || state.status.equals("ACCEPTED", ignoreCase = true)
                }
                .sortedWith(
                    compareBy<ImpactWeavePartnershipState> { partnershipStatusSortOrder(it.status) }
                        .thenBy { it.organisationName.lowercase() }
                )

            item(key = "match_overall") {
                PartnerMatchingSummaryCard(
                    results = results,
                    organisationCount = recommendedPartners.size,
                    isReadOnly = isReadOnlyPlan,
                    persistedStatus = draft.persistedStatus.orEmpty(),
                    acceptedPartnerCount = partnershipRequests.count {
                        it.status.equals("ACCEPTED", ignoreCase = true)
                    }
                )
            }

            item(key = "plan_actions") {
                ImpactWeavePlanActionsCard(
                    status = draft.persistedStatus.orEmpty(),
                    startDateMillis = draft.startDateMillis,
                    hasConfirmedVenue = draft.hasExistingVenue == true ||
                        results.needResults.any {
                            it.need.supportType.equals("VENUE", true) && it.need.isFulfilled
                        },
                    hasWaitingPartnershipResponse = partnershipRequests.any { state ->
                        state.status.equals("PENDING", ignoreCase = true) ||
                            state.status.equals("RECONFIRMATION_REQUIRED", ignoreCase = true)
                    },
                    minimumPostDateMillis = minimumPostDateMillis,
                    isSaving = isSavingPlanChange,
                    errorMessage = planChangeError,
                    successMessage = planChangeSuccess,
                    onEditDetails = {
                        onClearPlanFeedback()
                        showEditDetails = true
                    },
                    onReschedule = {
                        onClearPlanFeedback()
                        showReschedule = true
                    },
                    onDispose = { showDispose = true },
                    onCreatePost = {
                        draft.databaseDraftId?.let(onCreatePost)
                    }
                )
            }

            item(key = "confirmed_progress_heading") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Support Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = "Accepted partnership support increases each countable requirement from 0 toward its target. Unmatched needs stay here at 0 instead of being separated into another section.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(
                items = results.needResults,
                key = { "confirmed_${it.need.needId}" }
            ) { needResult ->
                ConfirmedSupportProgressCard(
                    result = needResult,
                    isReadOnly = isReadOnlyPlan
                )
            }

            if (results.needResults.any { it.need.supportType == "VENUE" && it.usesWiderVenueArea }) {
                item(key = "wider_venue_notice") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF7E8),
                        border = BorderStroke(1.dp, Color(0xFFE3C472))
                    ) {
                        Text(
                            text = draft.areaLocation?.generalAreaName
                                ?.takeIf { it.isNotBlank() }
                                ?.let { "No suitable venue was found near $it, so wider-area venue options are included." }
                                ?: "No suitable venue was found near the preferred area, so wider-area venue options are included.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 17.sp,
                            color = Color(0xFF5F4815)
                        )
                    }
                }
            }

            if (partnershipRequests.isNotEmpty()) {
                item(key = "partnership_request_heading") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Partnership requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = if (isReadOnlyPlan) {
                                "Accepted partner organisations and the support recorded when this Impact Weave became read-only."
                            } else {
                                "Live invitation status and the exact support currently requested or confirmed."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(
                    items = partnershipRequests,
                    key = { "partnership_state_${it.invitationId}" }
                ) { request ->
                    ImpactWeavePartnershipStateCard(
                        request = request,
                        onViewProfile = { onViewOrganisationProfile(request.organisationId) }
                    )
                }
            }

            requestSuccess?.let { message ->
                item(key = "request_success") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEAF4E6),
                        border = BorderStroke(1.dp, Color(0xFFC5DABC))
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CreateGreen
                        )
                    }
                }
            }

            if (!isReadOnlyPlan && recommendedPartners.isEmpty()) {
                item(key = "no_recommended_partners") {
                    CreateSectionCard(
                        title = "No suitable partners yet",
                        subtitle = "VolunteerLink could not find a verified organisation that is currently open to partnership and directly matches these needs."
                    ) {
                        Text(
                            text = "You can reopen this plan later to check again when partnership profiles change.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (!isReadOnlyPlan && availableRecommendedPartners.isNotEmpty()) {
                item(key = "recommended_partner_heading") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Recommended partners",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = "Partners you have not contacted yet. Each organisation appears once.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(
                    items = availableRecommendedPartners,
                    key = { "recommended_${it.organisationId}" }
                ) { partner ->
                    PartnerOrganisationMatchCard(
                        partner = partner,
                        requestSent = false,
                        isSending = sendingOrganisationId == partner.organisationId,
                        onViewProfile = { onViewOrganisationProfile(partner.organisationId) },
                        onRequestSupport = {
                            onClearRequestFeedback()
                            requestCandidate = partner.representativeCandidate
                        }
                    )
                }
            }

            if (!isReadOnlyPlan && alternativePartners.isNotEmpty()) {
                item(key = "alternative_partner_heading") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Possible alternatives",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = "Related options that do not fully meet the current requirement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(
                    items = alternativePartners,
                    key = { "alternative_${it.organisationId}" }
                ) { partner ->
                    AlternativePartnerOrganisationCard(
                        partner = partner,
                        onViewProfile = { onViewOrganisationProfile(partner.organisationId) }
                    )
                }
            }
        }
    }

    if (showEditDetails) {
        ImpactWeaveDetailsDialog(
            draft = draft,
            isSaving = isSavingPlanChange,
            errorMessage = planChangeError,
            onDismiss = { if (!isSavingPlanChange) showEditDetails = false },
            onSave = { category, title, description ->
                onUpdateDetails(category, title, description)
            }
        )
    }

    if (showReschedule) {
        ImpactWeaveRescheduleDialog(
            draft = draft,
            minimumStartDateMillis = minimumStartDateMillis,
            isSaving = isSavingPlanChange,
            errorMessage = planChangeError,
            onDismiss = { if (!isSavingPlanChange) showReschedule = false },
            onSave = { startDate, endDate, startTime, endTime ->
                onReschedule(startDate, endDate, startTime, endTime)
            }
        )
    }

    if (showDispose) {
        AlertDialog(
            onDismissRequest = { if (!isSavingPlanChange) showDispose = false },
            title = { Text("Dispose Impact Weave plan?") },
            text = {
                Text("All pending and accepted partnerships will be cancelled and each partner will be notified. The conversation history is kept.")
            },
            confirmButton = {
                Button(
                    enabled = !isSavingPlanChange,
                    onClick = {
                        showDispose = false
                        onDispose()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Dispose plan") }
            },
            dismissButton = {
                TextButton(onClick = { showDispose = false }) { Text("Keep plan") }
            }
        )
    }

    val selectedCandidate = requestCandidate
    if (selectedCandidate != null && results != null) {
        val options = remember(results, selectedCandidate.supportId) {
            buildPartnerRequestOptions(results, selectedCandidate)
        }

        PartnershipRequestDialog(
            draft = draft,
            organisationName = selectedCandidate.organisationName,
            options = options,
            isSending = sendingOrganisationId == selectedCandidate.organisationId,
            errorMessage = requestError,
            onDismiss = {
                if (sendingOrganisationId == null) {
                    requestCandidate = null
                    onClearRequestFeedback()
                }
            },
            onSend = { requestItems ->
                onSendRequest(
                    selectedCandidate.organisationId,
                    selectedCandidate.organisationName,
                    requestItems
                )
            }
        )
    }
}

@Composable
/**
 * Renders the impact weave plan actions card card used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ImpactWeavePlanActionsCard(
    status: String,
    startDateMillis: Long?,
    hasConfirmedVenue: Boolean,
    hasWaitingPartnershipResponse: Boolean,
    minimumPostDateMillis: Long,
    isSaving: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onEditDetails: () -> Unit,
    onReschedule: () -> Unit,
    onDispose: () -> Unit,
    onCreatePost: () -> Unit
) {
    val normalizedStatus = status.uppercase(Locale.ROOT)
    val terminal = normalizedStatus == "DISPOSED" || normalizedStatus == "CONVERTED"
    val statusAllowsPost = normalizedStatus == "MATCHING" ||
        normalizedStatus == "PARTIAL" || normalizedStatus == "READY"
    val dateAllowsPost = startDateMillis != null && startDateMillis >= minimumPostDateMillis
    val canCreatePost = statusAllowsPost && !hasWaitingPartnershipResponse &&
        hasConfirmedVenue && dateAllowsPost && !terminal
    CreateSectionCard(
        title = when {
            normalizedStatus == "DISPOSED" -> "Disposed plan"
            normalizedStatus == "CONVERTED" -> "Volunteer Post created"
            canCreatePost -> "Ready for volunteers"
            else -> "Manage activity plan"
        },
        subtitle = when {
            terminal -> "This plan is kept as read-only history, including its partnership record."
            canCreatePost -> "Confirmed support can be partial. Continue to add volunteer roles and capacity; the agreed activity details stay locked."
            normalizedStatus == "WAITING" || hasWaitingPartnershipResponse -> "A partnership request or schedule reconfirmation is still waiting for a response. Finish that first before creating the Volunteer Post."
            statusAllowsPost && !hasConfirmedVenue -> "A confirmed physical venue is required before this plan can become a Volunteer Post."
            statusAllowsPost && !dateAllowsPost -> "Create Post is locked because the activity starts in less than 7 days. Reschedule it before continuing."
            else -> "You can create the Volunteer Post without waiting for every support need to be fulfilled, as long as no partnership response is still pending."
        }
    ) {
        if (statusAllowsPost && !terminal) {
            Button(
                onClick = onCreatePost,
                enabled = !isSaving && canCreatePost,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CreateGreen),
                shape = RoundedCornerShape(13.dp)
            ) { Text("Create Volunteer Post", fontWeight = FontWeight.Bold) }
        }
        if (!terminal) Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEditDetails,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Edit details") }
            OutlinedButton(
                onClick = onReschedule,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Reschedule") }
        }
        if (!terminal) Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onDispose,
                enabled = !isSaving
            ) {
                Text("Dispose plan", color = MaterialTheme.colorScheme.error)
            }
        }
        successMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = CreateGreen)
        }
        errorMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
/**
 * Renders the impact weave details dialog dialog used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ImpactWeaveDetailsDialog(
    draft: ImpactWeaveDraft,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (VolunteerPostCategory, String, String) -> Unit
) {
    var category by remember(draft.databaseDraftId) { mutableStateOf(draft.category) }
    var title by remember(draft.databaseDraftId) { mutableStateOf(draft.title) }
    var description by remember(draft.databaseDraftId) { mutableStateOf(draft.description) }
    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .imePadding(),
            shape = RoundedCornerShape(28.dp),
            color = VolunteerLinkSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Edit activity details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "These text changes do not affect confirmed support.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VolunteerLinkTextSecondary
                )
                CategoryPicker(category, { category = it }, null, !isSaving)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 3,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
                    Button(
                        enabled = !isSaving && category != null && title.trim().length >= 3 && description.isNotBlank(),
                        onClick = { category?.let { onSave(it, title, description) } },
                        colors = ButtonDefaults.buttonColors(containerColor = CreateGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (isSaving) "Saving..." else "Save") }
                }
            }
        }
    }
}

@Composable
/**
 * Renders the impact weave reschedule dialog dialog used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ImpactWeaveRescheduleDialog(
    draft: ImpactWeaveDraft,
    minimumStartDateMillis: Long,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Int, Int) -> Unit
) {
    var startDate by remember(draft.databaseDraftId) { mutableStateOf(draft.startDateMillis) }
    var endDate by remember(draft.databaseDraftId) { mutableStateOf(draft.endDateMillis) }
    var startTime by remember(draft.databaseDraftId) { mutableStateOf(draft.startTimeMinutes) }
    var endTime by remember(draft.databaseDraftId) { mutableStateOf(draft.endTimeMinutes) }
    val validationError = when {
        startDate == null || endDate == null || startTime == null || endTime == null ->
            "Complete every schedule field."
        startDate!! < minimumStartDateMillis ->
            "The new start date must be at least 10 days from today."
        endDate!! < startDate!! -> "End date cannot be before the start date."
        endDate == startDate && endTime!! <= startTime!! ->
            "End time must be later than the start time for a one-day activity."
        draft.startDateMillis == startDate && draft.endDateMillis == endDate &&
            draft.startTimeMinutes == startTime && draft.endTimeMinutes == endTime ->
            "Change at least one date or time before updating."
        else -> null
    }
    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = VolunteerLinkSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Change activity schedule", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFF7E8)) {
                Text(
                    "Accepted partners will need to reconfirm. Their support pauses until they respond.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D5318)
                )
                }
                DateSelectionField("Start date", startDate, minimumStartDateMillis, onDateSelected = {
                    startDate = it
                    if (endDate == null || endDate!! < it) endDate = it
                }, enabled = !isSaving)
                DateSelectionField("End date", endDate, startDate ?: minimumStartDateMillis, onDateSelected = {
                    endDate = it
                }, enabled = !isSaving)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeSelectionField("Start time", startTime, onTimeSelected = { hour, minute ->
                        startTime = hour * 60 + minute
                        if (endDate == startDate && endTime != null && endTime!! <= startTime!!) {
                            endTime = null
                        }
                        null
                    }, modifier = Modifier.weight(1f), enabled = !isSaving)
                    TimeSelectionField("End time", endTime, onTimeSelected = { hour, minute ->
                        endTime = hour * 60 + minute
                        null
                    }, modifier = Modifier.weight(1f), enabled = !isSaving)
                }
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
                    Button(
                        enabled = !isSaving && validationError == null,
                        onClick = { onSave(startDate!!, endDate!!, startTime!!, endTime!!) },
                        colors = ButtonDefaults.buttonColors(containerColor = CreateGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (isSaving) "Saving..." else "Update schedule") }
                }
            }
        }
    }
}

@Composable
/**
 * Renders the confirmed support progress card card used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ConfirmedSupportProgressCard(
    result: ImpactWeaveNeedMatchResult,
    isReadOnly: Boolean = false
) {
    val need = result.need
    val complete = need.isFulfilled
    val hasSuitableProvider = result.directMatches.isNotEmpty()
    val hasRelatedAlternative = result.alternativeMatches.isNotEmpty()
    val isVenue = need.supportType == "VENUE"
    val total = (need.quantityRequired ?: 0).coerceAtLeast(0)
    val confirmed = need.confirmedQuantity.coerceIn(0, total.coerceAtLeast(0))
    val remaining = (total - confirmed).coerceAtLeast(0)

    val statusText = when {
        complete -> if (isVenue) "Venue secured" else "Fulfilled"
        isReadOnly && isVenue -> "Not secured"
        isReadOnly && confirmed > 0 -> "Partially confirmed"
        isReadOnly -> "Not confirmed"
        isVenue && hasSuitableProvider -> "Venue available"
        isVenue -> "No venue match yet"
        confirmed > 0 -> "In progress"
        hasSuitableProvider -> "Partner available"
        else -> "No match yet"
    }

    val statusBackground = when {
        complete -> Color(0xFFEAF4E6)
        isReadOnly -> Color(0xFFF3F4F1)
        !hasSuitableProvider && confirmed == 0 -> Color(0xFFFFF3E8)
        else -> Color(0xFFF3F4F1)
    }
    val statusColor = when {
        complete -> CreateGreen
        isReadOnly -> MaterialTheme.colorScheme.onSurfaceVariant
        !hasSuitableProvider && confirmed == 0 -> Color(0xFF8A4B14)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            when {
                complete -> Color(0xFFBBD6B2)
                !hasSuitableProvider && confirmed == 0 -> Color(0xFFE9C9A7)
                else -> Color(0xFFDDE2DA)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = need.resourceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = need.supportType.lowercase(Locale.ROOT)
                            .replaceFirstChar { it.titlecase(Locale.ROOT) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBackground
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            if (isVenue) {
                val venueMessage = when {
                    complete -> "A partner venue was confirmed for this activity."
                    isReadOnly -> "No venue was confirmed before this Impact Weave became read-only."
                    hasSuitableProvider -> "Suitable venue option${if (result.directMatches.size == 1) "" else "s"} found. Confirmation will happen after a partner accepts the request."
                    hasRelatedAlternative -> "No suitable direct venue match yet. Related venue options are shown below for review."
                    else -> "No verified organisation that is currently open to partnership provides a suitable venue yet."
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        complete -> Color(0xFFF1F7EE)
                        hasSuitableProvider -> Color(0xFFF7F8F5)
                        else -> Color(0xFFFFF6EC)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when {
                                complete -> "Venue secured"
                                isReadOnly -> "Venue not secured"
                                hasSuitableProvider -> "Suitable venue available"
                                else -> "No suitable venue found yet"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                complete -> CreateGreen
                                hasSuitableProvider -> VolunteerLinkTextPrimary
                                else -> Color(0xFF8A4B14)
                            }
                        )
                        Text(
                            text = venueMessage,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = need.capacityRequired?.let { "Required capacity: $it people" }
                                ?: "No minimum capacity specified",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val fraction = if (total > 0) confirmed.toFloat() / total.toFloat() else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$confirmed / $total confirmed",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CreateGreen
                    )
                    if (!complete) {
                        Text(
                            text = "$remaining remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(50)),
                    color = CreateGreen,
                    trackColor = Color(0xFFE7EDE3)
                )

                if (!complete) {
                    val matchingMessage = when {
                        isReadOnly ->
                            "Locked history: $confirmed of $total was confirmed when this Impact Weave became read-only."
                        hasSuitableProvider && result.directMatches.size == 1 ->
                            "1 suitable partner is currently available for this requirement."
                        hasSuitableProvider ->
                            "${result.directMatches.size} suitable partners are currently available for this requirement."
                        hasRelatedAlternative ->
                            "No direct match yet. Related alternatives are shown below for review."
                        confirmed > 0 ->
                            "No additional suitable partner is currently available for the remaining $remaining."
                        else ->
                            "No suitable partner found yet. This requirement remains at 0 until a partner accepts support."
                    }

                    Text(
                        text = matchingMessage,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp,
                        color = if (hasSuitableProvider) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Color(0xFF8A4B14)
                        }
                    )
                }

                Text(
                    text = need.originalText,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
/**
 * Renders the partner matching summary card card used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun PartnerMatchingSummaryCard(
    results: ImpactWeaveMatchResults,
    organisationCount: Int,
    isReadOnly: Boolean = false,
    persistedStatus: String = "",
    acceptedPartnerCount: Int = 0
) {
    val normalizedStatus = persistedStatus.uppercase(Locale.ROOT)
    CreateSectionCard(
        title = if (isReadOnly) "Partnership history" else "Partner Matching",
        subtitle = if (isReadOnly) {
            when (normalizedStatus) {
                "CONVERTED" ->
                    "This Impact Weave has already created a Volunteer Post. The support progress and accepted partners below are locked as read-only history."
                "DISPOSED" ->
                    "This Impact Weave was disposed. Its partnership record is kept as read-only history."
                else ->
                    "This Impact Weave is read-only. Its confirmed support record is shown below."
            }
        } else if (organisationCount == 1) {
            "1 partner organisation matches ${results.needsWithPotentialSupport} of ${results.needResults.size} needs."
        } else {
            "$organisationCount partner organisations match ${results.needsWithPotentialSupport} of ${results.needResults.size} needs."
        }
    ) {
        Text(
            text = if (isReadOnly) {
                if (acceptedPartnerCount == 1) {
                    "1 organisation accepted partnership support for this activity."
                } else {
                    "$acceptedPartnerCount organisations accepted partnership support for this activity."
                }
            } else {
                "Only verified organisations that are currently open to partnership are recommended. Matching is not confirmed support; progress starts after an invitation is accepted."
            },
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Holds the values represented by partner organisation match group as one strongly typed model.
 * It supports the Impact Weave and partnership presentation layer without adding backend responsibilities to the screen.
 */
private data class PartnerOrganisationMatchGroup(
    val organisationId: String,
    val organisationName: String,
    val options: List<PartnerRequestOption>,
    val representativeCandidate: ImpactWeaveSupportCandidate,
    val nearestDistanceKm: Double?
)

/**
 * Builds the partner organisation groups used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun buildPartnerOrganisationGroups(
    results: ImpactWeaveMatchResults
): List<PartnerOrganisationMatchGroup> {
    val representatives = results.needResults
        .flatMap { it.directMatches }
        .distinctBy { it.organisationId }

    return representatives.mapNotNull { representative ->
        val options = buildPartnerRequestOptions(results, representative)
        if (options.isEmpty()) return@mapNotNull null

        PartnerOrganisationMatchGroup(
            organisationId = representative.organisationId,
            organisationName = representative.organisationName,
            options = options,
            representativeCandidate = representative,
            nearestDistanceKm = options.mapNotNull { it.candidate.distanceKm }.minOrNull()
        )
    }.sortedWith(
        compareByDescending<PartnerOrganisationMatchGroup> { it.options.size }
            .thenBy { it.nearestDistanceKm ?: Double.MAX_VALUE }
            .thenBy { it.organisationName.lowercase() }
    )
}

/**
 * Builds the alternative partner organisation groups used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun buildAlternativePartnerOrganisationGroups(
    results: ImpactWeaveMatchResults,
    excludedOrganisationIds: Set<String>
): List<PartnerOrganisationMatchGroup> {
    val alternativeCandidates = results.needResults
        .flatMap { result -> result.alternativeMatches.map { result to it } }
        .filterNot { (_, candidate) -> candidate.organisationId in excludedOrganisationIds }

    return alternativeCandidates
        .groupBy { (_, candidate) -> candidate.organisationId }
        .mapNotNull { (_, entries) ->
            val representative = entries.firstOrNull()?.second ?: return@mapNotNull null
            val options = entries
                .groupBy { (result, _) -> result.need.needId }
                .values
                .mapNotNull { sameNeed ->
                    val best = sameNeed.minWithOrNull(
                        compareBy<Pair<ImpactWeaveNeedMatchResult, ImpactWeaveSupportCandidate>> {
                            it.second.distanceKm ?: Double.MAX_VALUE
                        }.thenByDescending { it.second.quantity ?: it.second.capacity ?: 0 }
                    ) ?: return@mapNotNull null
                    PartnerRequestOption(best.first, best.second)
                }

            PartnerOrganisationMatchGroup(
                organisationId = representative.organisationId,
                organisationName = representative.organisationName,
                options = options,
                representativeCandidate = representative,
                nearestDistanceKm = options.mapNotNull { it.candidate.distanceKm }.minOrNull()
            )
        }
        .sortedWith(
            compareByDescending<PartnerOrganisationMatchGroup> { it.options.size }
                .thenBy { it.nearestDistanceKm ?: Double.MAX_VALUE }
                .thenBy { it.organisationName.lowercase() }
        )
}

/**
 * Derives the partnership status sort order value used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun partnershipStatusSortOrder(status: String): Int = when (status.uppercase(Locale.ROOT)) {
    "RECONFIRMATION_REQUIRED" -> 0
    "PENDING" -> 1
    "ACCEPTED" -> 2
    "DECLINED" -> 3
    "FULFILLED_ELSEWHERE" -> 4
    "CANCELLED" -> 5
    else -> 6
}

@Composable
/**
 * Renders the impact weave partnership state card card used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ImpactWeavePartnershipStateCard(
    request: ImpactWeavePartnershipState,
    onViewProfile: () -> Unit
) {
    var showAllItems by remember(request.invitationId) { mutableStateOf(false) }
    val status = request.status.uppercase(Locale.ROOT)
    val accepted = status == "ACCEPTED"
    val pending = status == "PENDING"
    val title = when {
        accepted -> "Accepted partnership"
        pending && request.revisionNumber > 1 -> "Updated request awaiting response"
        pending -> "Awaiting response"
        status == "DECLINED" -> "Request declined"
        status == "FULFILLED_ELSEWHERE" -> "Support fulfilled elsewhere"
        status == "RECONFIRMATION_REQUIRED" -> "Reconfirmation required"
        status == "CANCELLED" -> "Request cancelled"
        else -> request.status.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    }
    val badgeBackground = when (status) {
        "ACCEPTED" -> Color(0xFFE5F1E1)
        "PENDING" -> Color(0xFFF4F0DB)
        "RECONFIRMATION_REQUIRED" -> Color(0xFFFFF2D8)
        "DECLINED", "CANCELLED", "FULFILLED_ELSEWHERE" -> Color(0xFFF5E9E6)
        else -> Color(0xFFF0F1EE)
    }
    val borderColor = when (status) {
        "ACCEPTED" -> Color(0xFFBFD7B7)
        "PENDING" -> Color(0xFFE0D6A9)
        "RECONFIRMATION_REQUIRED" -> Color(0xFFE8C778)
        else -> Color(0xFFDDE1DA)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onViewProfile),
                    shape = CircleShape,
                    color = Color(0xFFE7F1E3)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = request.organisationName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "O",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreateGreen
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                        .clickable(onClick = onViewProfile),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = request.organisationName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (accepted) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (accepted) CreateGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "View organisation profile ›",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CreateGreen
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBackground
                ) {
                    Text(
                        text = when (status) {
                            "ACCEPTED" -> "Accepted"
                            "PENDING" -> if (request.revisionNumber > 1) "Updated" else "Pending"
                            "RECONFIRMATION_REQUIRED" -> "Reconfirm"
                            "FULFILLED_ELSEWHERE" -> "No longer needed"
                            else -> request.status.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
                        },
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (accepted) CreateGreen else VolunteerLinkTextPrimary
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE6EAE4))

            Text(
                text = when {
                    accepted -> "Confirmed support"
                    pending -> "Current request"
                    else -> "Request details"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextPrimary
            )

            if (request.items.isEmpty()) {
                Text(
                    text = when (status) {
                        "FULFILLED_ELSEWHERE" -> "No active support remains because these needs were fulfilled by another partner."
                        "DECLINED" -> "This invitation was declined and does not contribute to support progress."
                        else -> "No active support items remain on this invitation."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val visibleItems = if (showAllItems) request.items else request.items.take(3)

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    visibleItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(7.dp),
                                shape = CircleShape,
                                color = if (accepted) CreateGreen else Color(0xFF8B7A38)
                            ) {}

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = item.resourceName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VolunteerLinkTextPrimary
                                )
                                val providerName = item.providerResourceName
                                    ?.takeIf { it.isNotBlank() }
                                    ?: item.resourceName
                                val amount = if (item.supportType.equals("VENUE", ignoreCase = true)) {
                                    item.capacityProvided?.let { "Capacity $it" } ?: "Venue"
                                } else {
                                    item.quantityProvided?.toString() ?: "Support"
                                }
                                Text(
                                    text = if (accepted) {
                                        "$providerName · Confirmed $amount"
                                    } else {
                                        "$providerName · Requested $amount"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (request.items.size > 3) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAllItems = !showAllItems },
                            shape = RoundedCornerShape(11.dp),
                            color = Color(0xFFF2F7EF)
                        ) {
                            Text(
                                text = if (showAllItems) {
                                    "Show fewer support items"
                                } else {
                                    val hiddenCount = request.items.size - 3
                                    "View $hiddenCount more support ${if (hiddenCount == 1) "item" else "items"}"
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CreateGreen,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (pending && request.revisionNumber > 1) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(11.dp),
                    color = Color(0xFFFFF7E8)
                ) {
                    Text(
                        text = "VolunteerLink reduced this request to what is still needed. The partner must accept the updated request before it counts toward progress.",
                        modifier = Modifier.padding(11.dp),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp,
                        color = Color(0xFF6F5417)
                    )
                }
            }
        }
    }
}

@Composable
/**
 * Renders the partner organisation match card card used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun PartnerOrganisationMatchCard(
    partner: PartnerOrganisationMatchGroup,
    requestSent: Boolean,
    isSending: Boolean,
    onViewProfile: () -> Unit,
    onRequestSupport: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD7E3D2))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onViewProfile),
                    shape = CircleShape,
                    color = Color(0xFFE7F1E3)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = partner.organisationName
                                .trim()
                                .firstOrNull()
                                ?.uppercaseChar()
                                ?.toString()
                                ?: "O",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreateGreen
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                        .clickable(onClick = onViewProfile),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = partner.organisationName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = if (partner.options.size == 1) {
                            "Can support 1 requirement"
                        } else {
                            "Can support ${partner.options.size} requirements"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "View organisation profile ›",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CreateGreen
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFEAF4E6)
                ) {
                    Text(
                        text = "Eligible",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CreateGreen
                    )
                }
            }

            partner.nearestDistanceKm?.let { distance ->
                Text(
                    text = if (distance < 1.0) "Nearest support <1 km away" else "Nearest support ${distance.roundToInt()} km away",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = Color(0xFFE6EAE4))

            Text(
                text = "Can provide",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                partner.options.forEach { option ->
                    PartnerOrganisationSupportLine(option)
                }
            }

            when {
                requestSent -> Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEAF4E6)
                ) {
                    Text(
                        text = "Partnership request sent",
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = CreateGreen
                    )
                }

                isSending -> Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sending request...")
                }

                else -> Button(
                    onClick = onRequestSupport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CreateGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Request Support",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
/**
 * Renders the UI represented by partner organisation support line for the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun PartnerOrganisationSupportLine(option: PartnerRequestOption) {
    val result = option.needResult
    val candidate = option.candidate
    val need = result.need

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(7.dp),
            shape = CircleShape,
            color = CreateGreen
        ) {}

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = need.resourceName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = "${candidate.resourceName} · ${candidateAvailabilityLabel(candidate, result)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (need.supportType != "VENUE") {
                val required = need.quantityRequired ?: 0
                val remaining = (required - need.confirmedQuantity).coerceAtLeast(0)
                val available = candidate.quantity ?: 0
                val usable = minOf(remaining, available).coerceAtLeast(0)
                if (remaining > 0 && available > 0) {
                    Text(
                        text = if (available >= remaining) {
                            "Can cover the full $remaining currently remaining"
                        } else {
                            "Can contribute $usable of $remaining currently remaining"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (available >= remaining) CreateGreen else Color(0xFF7A5A16)
                    )
                }
            }

            candidate.locationName
                ?.takeIf { it.isNotBlank() && need.supportType == "VENUE" }
                ?.let { location ->
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
    }
}

@Composable
/**
 * Renders the alternative partner organisation card card used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun AlternativePartnerOrganisationCard(
    partner: PartnerOrganisationMatchGroup,
    onViewProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8F8F8),
        border = BorderStroke(1.dp, Color(0xFFE1E1E1))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewProfile),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = partner.organisationName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View profile ›",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CreateGreen
                )
            }
            partner.options.forEach { option ->
                Text(
                    text = "• ${option.needResult.need.resourceName}: ${option.candidate.resourceName} (${candidateAvailabilityLabel(option.candidate, option.needResult)})",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Holds the values represented by partner request option as one strongly typed model.
 * It supports the Impact Weave and partnership presentation layer without adding backend responsibilities to the screen.
 */
private data class PartnerRequestOption(
    val needResult: ImpactWeaveNeedMatchResult,
    val candidate: ImpactWeaveSupportCandidate
)

/**
 * Builds the partner request options used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun buildPartnerRequestOptions(
    results: ImpactWeaveMatchResults,
    selectedCandidate: ImpactWeaveSupportCandidate
): List<PartnerRequestOption> {
    return results.needResults.mapNotNull { needResult ->
        if (needResult.need.isFulfilled) return@mapNotNull null
        val candidates = needResult.directMatches.filter {
            it.organisationId == selectedCandidate.organisationId
        }
        if (candidates.isEmpty()) return@mapNotNull null

        val candidate = if (candidates.any { it.supportId == selectedCandidate.supportId }) {
            candidates.first { it.supportId == selectedCandidate.supportId }
        } else if (needResult.need.supportType == "VENUE") {
            candidates.first()
        } else {
            candidates.maxWithOrNull(
                compareBy<ImpactWeaveSupportCandidate> { it.quantity ?: 0 }
                    .thenBy { -(it.distanceKm ?: Double.MAX_VALUE) }
            ) ?: candidates.first()
        }

        PartnerRequestOption(
            needResult = needResult,
            candidate = candidate
        )
    }
}

@Composable
/**
 * Renders the partnership request dialog dialog used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun PartnershipRequestDialog(
    draft: ImpactWeaveDraft,
    organisationName: String,
    options: List<PartnerRequestOption>,
    isSending: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSend: (List<PartnershipRequestItem>) -> Unit
) {
    val selected = remember(options) {
        mutableStateMapOf<String, Boolean>().apply {
            options.forEach { option -> put(option.needResult.need.needId, true) }
        }
    }
    val amounts = remember(options) {
        mutableStateMapOf<String, String>().apply {
            options.forEach { option ->
                val need = option.needResult.need
                if (need.supportType != "VENUE") {
                    val required = need.quantityRequired ?: 0
                    val remaining = (required - need.confirmedQuantity).coerceAtLeast(0)
                    val available = option.candidate.quantity ?: 0
                    put(need.needId, minOf(remaining, available).coerceAtLeast(0).toString())
                }
            }
        }
    }

    val requestItems = options.mapNotNull { option ->
        val need = option.needResult.need
        if (selected[need.needId] != true) return@mapNotNull null

        if (need.supportType == "VENUE") {
            PartnershipRequestItem(
                needId = need.needId,
                supportId = option.candidate.supportId,
                requestedAmount = null
            )
        } else {
            val requested = amounts[need.needId]?.toIntOrNull() ?: return@mapNotNull null
            PartnershipRequestItem(
                needId = need.needId,
                supportId = option.candidate.supportId,
                requestedAmount = requested
            )
        }
    }

    val hasInvalidAmount = options.any { option ->
        val need = option.needResult.need
        if (selected[need.needId] != true || need.supportType == "VENUE") {
            false
        } else {
            val amount = amounts[need.needId]?.toIntOrNull()
            val required = need.quantityRequired ?: 0
            val remaining = (required - need.confirmedQuantity).coerceAtLeast(0)
            val available = option.candidate.quantity ?: 0
            amount == null || amount <= 0 || amount > remaining || amount > available
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isSending) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(26.dp),
            color = VolunteerLinkBackground,
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Request Partnership",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = CreateGreen
                    )
                    Text(
                        text = "To $organisationName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = "Review the activity and the exact support proposal before sending.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RequestActivityOverviewCard(draft)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Support Proposal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = "Select the requirements to include. One invitation can contain several support items from the same organisation.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    options.forEach { option ->
                        val need = option.needResult.need
                        val checked = selected[need.needId] == true
                        val amount = amounts[need.needId].orEmpty()
                        val required = need.quantityRequired ?: 0
                        val confirmed = need.confirmedQuantity.coerceAtLeast(0)
                        val remaining = (required - confirmed).coerceAtLeast(0)
                        val available = option.candidate.quantity ?: 0
                        val amountNumber = amount.toIntOrNull()
                        val amountInvalid = checked && need.supportType != "VENUE" &&
                            (amountNumber == null || amountNumber <= 0 ||
                                amountNumber > remaining || amountNumber > available)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (checked) Color(0xFFBFD5B8) else Color(0xFFDDE2DA)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = checked,
                                        enabled = !isSending && !need.isFulfilled,
                                        onCheckedChange = { selected[need.needId] = it }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = need.resourceName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = VolunteerLinkTextPrimary
                                        )
                                        Text(
                                            text = need.supportType.lowercase(Locale.ROOT)
                                                .replaceFirstChar { it.titlecase(Locale.ROOT) },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (need.supportType != "VENUE") {
                                    val progress = if (required > 0) {
                                        confirmed.toFloat() / required.toFloat()
                                    } else 0f
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "$confirmed / $required confirmed",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CreateGreen
                                        )
                                        Text(
                                            text = "$remaining remaining",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = CreateGreen,
                                        trackColor = Color(0xFFE7EDE3)
                                    )
                                }

                                ProposalDetailBlock(
                                    label = "Requirement",
                                    primary = need.originalText,
                                    secondary = if (need.supportType == "VENUE") {
                                        need.capacityRequired?.let { "Target capacity: $it people" }
                                            ?: "No minimum capacity specified"
                                    } else {
                                        "Target: ${need.quantityRequired ?: 0}"
                                    }
                                )

                                if (need.supportType == "VENUE") {
                                    ProposalDetailBlock(
                                        label = "What we propose",
                                        primary = "Use ${option.candidate.resourceName} as the activity venue",
                                        secondary = option.candidate.capacity?.let { "Listed capacity: $it people" }
                                            ?: "Capacity not listed"
                                    )
                                } else if (checked) {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(
                                            text = "What we propose",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CreateGreen
                                        )
                                        OutlinedTextField(
                                            value = amount,
                                            enabled = !isSending,
                                            onValueChange = { value ->
                                                amounts[need.needId] = value.filter { it.isDigit() }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = { Text("Requested quantity") },
                                            supportingText = {
                                                Text("Up to ${minOf(remaining, available)} can be requested now")
                                            },
                                            isError = amountInvalid,
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }

                                ProposalDetailBlock(
                                    label = "What they can provide",
                                    primary = option.candidate.supportDescription,
                                    secondary = buildString {
                                        append(option.candidate.resourceName)
                                        if (need.supportType == "VENUE") {
                                            option.candidate.capacity?.let { append(" · Capacity $it") }
                                            option.candidate.locationName?.takeIf { it.isNotBlank() }
                                                ?.let { append(" · $it") }
                                        } else {
                                            append(" · Available ${option.candidate.quantity ?: 0}")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    errorMessage?.let { message ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        enabled = !isSending,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        enabled = !isSending && requestItems.isNotEmpty() && !hasInvalidAmount,
                        onClick = { onSend(requestItems) },
                        modifier = Modifier.weight(1.35f),
                        colors = ButtonDefaults.buttonColors(containerColor = CreateGreen),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(7.dp))
                            Text("Sending...")
                        } else {
                            Text("Send Request", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
/**
 * Renders the request activity overview card card used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun RequestActivityOverviewCard(draft: ImpactWeaveDraft) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF1F7EE),
        border = BorderStroke(1.dp, Color(0xFFC9DCC3))
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = "About the Activity",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = CreateGreen
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = draft.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = VolunteerLinkTextPrimary
                )
                draft.category?.let { category ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White
                    ) {
                        Text(
                            text = category.displayName,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CreateGreen
                        )
                    }
                }
            }

            if (draft.description.isNotBlank()) {
                Text(
                    text = draft.description,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextPrimary
                )
            }

            HorizontalDivider(color = Color(0xFFD8E4D4))

            Text(
                text = "${draft.mode?.displayName.orEmpty()} · ${formatDraftSchedule(draft)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = requestActivityLocationLabel(draft),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
/**
 * Renders the UI represented by proposal detail block for the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun ProposalDetailBlock(
    label: String,
    primary: String,
    secondary: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF7F9F6)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CreateGreen
            )
            Text(
                text = primary,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                color = VolunteerLinkTextPrimary
            )
            if (secondary.isNotBlank()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Requests the activity location label for the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun requestActivityLocationLabel(draft: ImpactWeaveDraft): String {
    return if (draft.hasExistingVenue == true) {
        draft.existingVenueLocation?.let { location ->
            location.address.takeIf { it.isNotBlank() } ?: location.displayName
        }.orEmpty().ifBlank { draft.areaLocation?.generalAreaName.orEmpty() }
    } else {
        draft.areaLocation?.generalAreaName.orEmpty().let { area ->
            if (area.isBlank()) "Preferred area not available" else "Preferred activity area: $area"
        }
    }
}

/**
 * Returns the matching need amount label used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun matchingNeedAmountLabel(need: com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDatabaseNeed): String {
    return if (need.supportType == "VENUE") {
        need.capacityRequired?.let { "Capacity $it" } ?: "Capacity not specified"
    } else {
        "${need.quantityRequired ?: 0} needed"
    }
}

/**
 * Checks whether the organisation Impact Weave and partnership flow allows idate availability label.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun candidateAvailabilityLabel(
    candidate: ImpactWeaveSupportCandidate,
    result: ImpactWeaveNeedMatchResult
): String {
    return if (result.need.supportType == "VENUE") {
        when {
            candidate.capacity != null && result.need.capacityRequired != null &&
                candidate.capacity < result.need.capacityRequired ->
                "Capacity ${candidate.capacity} · below required ${result.need.capacityRequired}"
            candidate.capacity != null -> "Capacity ${candidate.capacity}"
            result.need.capacityRequired != null -> "Capacity not listed"
            else -> "Venue available · capacity not listed"
        }
    } else {
        "Can provide ${candidate.quantity ?: 0}"
    }
}

@Composable
/**
 * Renders the UI represented by impact weave summary line for the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun ImpactWeaveSummaryLine(
    iconRes: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF3F5F2)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = VolunteerLinkPrimaryGreen
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextSecondary
            )
            Text(
                text = value,
                modifier = Modifier.padding(top = 1.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = VolunteerLinkTextPrimary
            )
        }
    }
}

@Composable
/**
 * Returns the form error text used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun FormErrorText(message: String?) {
    if (!message.isNullOrBlank()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Returns the support type label used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
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
 * Returns the need amount label used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun needAmountLabel(need: ImpactWeaveNeedDraft): String {
    return if (need.supportType == "VENUE") {
        need.capacityRequired?.let { "Capacity $it" } ?: "Capacity not specified"
    } else {
        need.quantityRequired?.let { "$it needed" } ?: "Quantity not specified"
    }
}

/**
 * Formats the draft schedule used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun formatDraftSchedule(draft: ImpactWeaveDraft): String {
    val startDate = draft.startDateMillis?.let(::formatDate).orEmpty()
    val endDate = draft.endDateMillis?.let(::formatDate).orEmpty()
    val startTime = formatTime(draft.startTimeMinutes)
    val endTime = formatTime(draft.endTimeMinutes)

    val dateText = if (draft.duration == ImpactWeaveDuration.ONE_DAY) {
        startDate
    } else {
        "$startDate - $endDate"
    }
    return "$dateText · $startTime - $endTime"
}

/**
 * Adds the days to the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun addDays(dateMillis: Long, days: Int): Long {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = dateMillis
        add(java.util.Calendar.DAY_OF_YEAR, days)
    }.timeInMillis
}

/**
 * Formats the date used by the organisation Impact Weave and partnership flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun formatDate(dateMillis: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(Date(dateMillis))
}
