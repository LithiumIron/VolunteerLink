package com.example.volunteerlink.organisation.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.volunteerlink.organisation.create.components.DateSelectionField
import com.example.volunteerlink.organisation.create.components.TimeSelectionField
import com.example.volunteerlink.organisation.create.components.formatTime
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDuration
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMode
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveNeedDraft
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

@Composable
fun ImpactWeaveDraftListScreen(
    drafts: List<ImpactWeaveDraft>,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onDraftClick: (Int) -> Unit,
    planningDeadlineFor: (Long?) -> Long?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
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
                    top = 18.dp
                )
            ) {
                Text(
                    text = "Plan an activity with partner organisations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = "Describe the activity and support you need before finding partner organisations.",
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

        if (drafts.isEmpty()) {
            item(key = "impact_empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Impact Weave drafts yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = "Your activity plans will appear here after you complete the first step.",
                        modifier = Modifier.padding(top = 7.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = VolunteerLinkTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item(key = "impact_draft_heading") {
                OrganisationSectionHeader(
                    title = "Drafts",
                    subtitle = "Continue planning before partner matching",
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp,
                        bottom = 4.dp
                    )
                )
            }

            items(drafts, key = { it.draftId }) { draft ->
                ImpactWeaveDraftRow(
                    draft = draft,
                    planningDeadlineMillis = planningDeadlineFor(draft.startDateMillis),
                    onClick = { onDraftClick(draft.draftId) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ImpactWeaveDraftRow(
    draft: ImpactWeaveDraft,
    planningDeadlineMillis: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OrganisationSectionSurface(
        modifier = modifier.padding(top = 10.dp),
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = VolunteerLinkSoftGreenSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.group),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = VolunteerLinkPrimaryGreen
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = draft.title,
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    OrganisationStatusPill(
                        text = "DRAFT",
                        color = VolunteerLinkPrimaryGreen,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Text(
                    text = buildString {
                        append(draft.mode?.displayName ?: "Activity")
                        draft.startDateMillis?.let {
                            append(" · ")
                            append(formatDate(it))
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary
                )

                Text(
                    text = "${draft.needs.size} support ${if (draft.needs.size == 1) "need" else "needs"}",
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )

                if (planningDeadlineMillis != null) {
                    Text(
                        text = "Partnership planning target: ${formatDate(planningDeadlineMillis)}",
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }
        }
    }
}

@Composable
fun ImpactWeaveActivityPlanScreen(
    draft: ImpactWeaveDraft,
    minimumStartDateMillis: Long,
    planningDeadlineMillis: Long?,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
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
            venueSuggestions = locationService.searchVenues(query)
            if (venueSuggestions.isEmpty()) {
                venueSearchError = "No matching venues or addresses found."
            }
        } catch (_: Exception) {
            venueSuggestions = emptyList()
            venueSearchError = "Unable to search venues right now."
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
                subtitle = "Give the partnership plan a clear name and choose its activity mode."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = onTitleChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Activity title") },
                        placeholder = { Text("Example: Community Health Day") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        isError = errors["title"] != null
                    )
                    FormErrorText(errors["title"])
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
fun ImpactWeaveSupportNeededScreen(
    draft: ImpactWeaveDraft,
    onBack: () -> Unit,
    onAddNeed: (String, String, String, Int) -> Unit,
    onUpdateNeed: (Int, String, String, String, Int) -> Unit,
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
    val canReview = draft.needs.isNotEmpty() && (!venueRequired || hasVenueNeed)

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
                                text = "No venue is confirmed yet. Add the type and capacity you need for ${draft.areaLocation?.generalAreaName ?: "the preferred area"}.",
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
                            "Add a venue requirement before reviewing this Impact Weave draft."

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
                        "${need.resourceName} will be removed from this draft."
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
private fun AddEditImpactWeaveNeedSheet(
    need: ImpactWeaveNeedDraft?,
    requiredSupportType: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int) -> Unit
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
    val amount = amountText.toIntOrNull()
    val amountError = showErrors && (amount == null || amount <= 0)

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
                        "Describe the type of venue and capacity this activity needs."
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
                                        checkError = "This step requires a venue. Describe a venue type and the number of people it should hold."
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
                val isVenue = supportType == "VENUE"

                Text(
                    text = if (need == null) "Review support need" else "Edit support need",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Check the details below before adding it to the draft.",
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
                    label = { Text(if (isVenue) "Capacity needed" else "Quantity needed") },
                    placeholder = { Text(if (isVenue) "e.g. 150 people" else "e.g. 2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Enter a value greater than 0.") }
                    } else {
                        null
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
                        if (amount != null && amount > 0) {
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
                    Text(if (need == null) "Add to draft" else "Save changes")
                }
            }
        }
    }
}

@Composable
fun ImpactWeaveReviewScreen(
    draft: ImpactWeaveDraft,
    planningDeadlineMillis: Long?,
    onBack: () -> Unit,
    onEditActivity: () -> Unit,
    onEditNeeds: () -> Unit,
    onSaveDraft: () -> Unit
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
                subtitle = "Check the activity details before saving this draft."
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = draft.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = draft.mode?.displayName.orEmpty(),
                            modifier = Modifier.padding(top = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = CreateGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = onEditActivity) {
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
                    TextButton(onClick = onEditNeeds) {
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
            Text(
                text = "Partner matching and invitations are not connected yet. This currently saves only the prototype draft in the app session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item(key = "review_save") {
            Button(
                onClick = onSaveDraft,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CreateGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Save Draft",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item(key = "review_end_space") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
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
private fun FormErrorText(message: String?) {
    if (!message.isNullOrBlank()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

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

private fun needAmountLabel(need: ImpactWeaveNeedDraft): String {
    return if (need.supportType == "VENUE") {
        "Capacity ${need.capacityRequired ?: 0}"
    } else {
        "${need.quantityRequired ?: 0} needed"
    }
}

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

private fun addDays(dateMillis: Long, days: Int): Long {
    return java.util.Calendar.getInstance().apply {
        timeInMillis = dateMillis
        add(java.util.Calendar.DAY_OF_YEAR, days)
    }.timeInMillis
}

private fun formatDate(dateMillis: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(Date(dateMillis))
}
