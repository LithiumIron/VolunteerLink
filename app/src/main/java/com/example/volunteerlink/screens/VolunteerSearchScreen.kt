
package com.example.volunteerlink.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.model.VolunteerOpportunityCategory
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import kotlinx.coroutines.delay
import com.example.volunteerlink.R

@Composable
fun VolunteerSearchScreen(
    onBackSelected: () -> Unit,
    onVolunteerOpportunitySelected: (eventId: Int) -> Unit
) {
    var volunteerSearchQuery by rememberSaveable {
        mutableStateOf("")
    }
    var selectedModeFilter by rememberSaveable {
        mutableStateOf("All")
    }
    var selectedCategoryFilter by rememberSaveable {
        mutableStateOf("All categories")
    }
    var selectedLevelFilter by rememberSaveable {
        mutableStateOf("All levels")
    }
    var verifiedOnly by rememberSaveable {
        mutableStateOf(false)
    }
    var showMoreFilters by rememberSaveable {
        mutableStateOf(false)
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        // Let the destination finish composing before requesting IME focus.
        delay(180)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val allOpportunities =
        VolunteerOpportunitySessionStore
            .volunteerOpportunityEvents
            .toList()

    val popularSearchTerms =
        remember(allOpportunities) {
            allOpportunities
                .sortedByDescending { event ->
                    event.eventApplicationCount
                }
                .map { event -> event.eventTitle }
                .distinct()
                .take(6)
        }

    val categoryFilters =
        remember(allOpportunities) {
            listOf("All categories") +
                allOpportunities
                    .map { event -> event.eventCategory.displayName() }
                    .distinct()
                    .sorted()
        }

    val filteredVolunteerOpportunities =
        allOpportunities.filter { event ->
            val query = volunteerSearchQuery.trim()
            val matchesSearchQuery =
                query.isBlank() ||
                    event.eventTitle.contains(query, ignoreCase = true) ||
                    event.eventOrganisationName.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    event.eventLocation.contains(query, ignoreCase = true) ||
                    event.eventCauseName.contains(query, ignoreCase = true) ||
                    event.eventCategory.displayName().contains(
                        query,
                        ignoreCase = true
                    ) ||
                    event.eventVolunteerRoles.any { role ->
                        role.roleTitle.contains(query, ignoreCase = true) ||
                            role.roleSpecificAssignment.contains(
                                query,
                                ignoreCase = true
                            ) ||
                            role.roleExperienceRequirement.contains(
                                query,
                                ignoreCase = true
                            ) ||
                            role.roleResponsibilities.any { responsibility ->
                                responsibility.contains(
                                    query,
                                    ignoreCase = true
                                )
                            } ||
                            role.rolePrimarySkillPath.contains(
                                query,
                                ignoreCase = true
                            ) ||
                            role.roleSkillsPractised.any { skill ->
                                skill.contains(query, ignoreCase = true)
                            }
                    }

            val matchesMode =
                when (selectedModeFilter) {
                    "Physical" ->
                        event.eventOpportunityType == "Physical"
                    "Remote" ->
                        event.eventOpportunityType == "Remote"
                    "Near Me" ->
                        event.eventDistanceKm != null &&
                            event.eventDistanceKm <= 10.0
                    "Long Term" -> event.eventIsLongTerm
                    "Favourites" -> event.eventIsSaved
                    else -> true
                }

            val matchesCategory =
                selectedCategoryFilter == "All categories" ||
                    event.eventCategory.displayName() ==
                        selectedCategoryFilter

            val matchesLevel =
                selectedLevelFilter == "All levels" ||
                    event.eventVolunteerRoles.any { role ->
                        role.roleLevel.equals(
                            selectedLevelFilter,
                            ignoreCase = true
                        )
                    }

            matchesSearchQuery &&
                (selectedModeFilter == "Favourites" || event.eventStatus == "PUBLISHED") &&
                matchesMode &&
                matchesCategory &&
                matchesLevel &&
                (!verifiedOnly || event.eventIsVerifiedOrganisation)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VolunteerLinkPrimaryGreen)
                .statusBarsPadding()
                .padding(start = 4.dp, end = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackSelected) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            OutlinedTextField(
                value = volunteerSearchQuery,
                onValueChange = { volunteerSearchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = "Event, role, skill or organisation",
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                },
                trailingIcon = {
                    if (volunteerSearchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { volunteerSearchQuery = "" }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VolunteerLinkSurface,
                    unfocusedContainerColor = VolunteerLinkSurface,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White,
                    cursorColor = VolunteerLinkPrimaryGreen
                )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (
                volunteerSearchQuery.isBlank() &&
                popularSearchTerms.isNotEmpty()
            ) {
                item(key = "popular_searches_title") {
                    Text(
                        text = "Popular searches",
                        modifier = Modifier.padding(
                            start = VolunteerLinkScreenHorizontalPadding,
                            end = VolunteerLinkScreenHorizontalPadding,
                            top = 15.dp,
                            bottom = 7.dp
                        ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                }

                item(key = "popular_searches") {
                    LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = VolunteerLinkScreenHorizontalPadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(
                            items = popularSearchTerms,
                            key = { term -> term }
                        ) { term ->
                            Surface(
                                onClick = {
                                    volunteerSearchQuery = term
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = VolunteerLinkSoftGreenSurface,
                                border = BorderStroke(
                                    1.dp,
                                    VolunteerLinkBorderColour
                                )
                            ) {
                                Text(
                                    // Unicode escape avoids mojibake when a
                                    // source file is copied between editors.
                                    text = "\uD83D\uDD25  $term",
                                    modifier = Modifier.padding(
                                        horizontal = 11.dp,
                                        vertical = 7.dp
                                    ),
                                    fontSize = 11.sp,
                                    color = VolunteerLinkTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            item(key = "quick_filters_title") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = VolunteerLinkScreenHorizontalPadding,
                            end = VolunteerLinkScreenHorizontalPadding,
                            top = 14.dp,
                            bottom = 6.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter opportunities",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    TextButton(
                        onClick = {
                            showMoreFilters = !showMoreFilters
                        }
                    ) {
                        Text(
                            text =
                                if (showMoreFilters) "Fewer filters"
                                else "More filters",
                            fontSize = 11.sp,
                            color = VolunteerLinkPrimaryGreen
                        )
                    }
                }
            }

            item(key = "mode_filters") {
                VolunteerSearchFilterRow(
                    options = listOf(
                        "All",
                        "Physical",
                        "Near Me",
                        "Remote",
                        "Long Term",
                        "Favourites"
                    ),
                    selectedOption = selectedModeFilter,
                    onSelected = { selectedModeFilter = it }
                )
            }

            if (showMoreFilters) {
                item(key = "category_label") {
                    VolunteerSearchSubheading("Category")
                }
                item(key = "category_filters") {
                    VolunteerSearchFilterRow(
                        options = categoryFilters,
                        selectedOption = selectedCategoryFilter,
                        onSelected = { selectedCategoryFilter = it }
                    )
                }
                item(key = "level_label") {
                    VolunteerSearchSubheading("Role difficulty")
                }
                item(key = "level_filters") {
                    VolunteerSearchFilterRow(
                        options = listOf(
                            "All levels",
                            "Beginner",
                            "Intermediate",
                            "Advanced"
                        ),
                        selectedOption = selectedLevelFilter,
                        onSelected = { selectedLevelFilter = it }
                    )
                }
                item(key = "verified_filter") {
                    FilterChip(
                        selected = verifiedOnly,
                        onClick = { verifiedOnly = !verifiedOnly },
                        label = {
                            Text(
                                text = "Verified organisations only",
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.padding(
                            horizontal = VolunteerLinkScreenHorizontalPadding
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VolunteerLinkPrimaryGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            item(key = "search_result_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = VolunteerLinkScreenHorizontalPadding,
                            vertical = 13.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Matching opportunities",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = "${filteredVolunteerOpportunities.size} found",
                        fontSize = 11.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }

            if (filteredVolunteerOpportunities.isEmpty()) {
                item(key = "empty_results") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No opportunities found",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try another keyword or clear one filter.",
                            fontSize = 12.sp,
                            color = VolunteerLinkTextSecondary
                        )
                        TextButton(
                            onClick = {
                                volunteerSearchQuery = ""
                                selectedModeFilter = "All"
                                selectedCategoryFilter = "All categories"
                                selectedLevelFilter = "All levels"
                                verifiedOnly = false
                            }
                        ) {
                            Text("Clear all filters")
                        }
                    }
                }
            } else {
                items(
                    items = filteredVolunteerOpportunities,
                    key = { event -> "search_${event.eventId}" }
                ) { event ->
                    VolunteerSearchResultCard(
                        volunteerOpportunityEvent = event,
                        onVolunteerOpportunitySelected = {
                            onVolunteerOpportunitySelected(event.eventId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VolunteerSearchFilterRow(
    options: List<String>,
    selectedOption: String,
    onSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(
            horizontal = VolunteerLinkScreenHorizontalPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items(items = options, key = { it }) { option ->
            FilterChip(
                selected = selectedOption == option,
                onClick = { onSelected(option) },
                label = {
                    Text(text = option, fontSize = 11.sp)
                },
                shape = RoundedCornerShape(18.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = VolunteerLinkSurface,
                    labelColor = VolunteerLinkTextSecondary,
                    selectedContainerColor = VolunteerLinkPrimaryGreen,
                    selectedLabelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedOption == option,
                    borderColor = VolunteerLinkBorderColour,
                    selectedBorderColor = VolunteerLinkPrimaryGreen
                )
            )
        }
    }
}

@Composable
private fun VolunteerSearchSubheading(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = VolunteerLinkScreenHorizontalPadding,
            end = VolunteerLinkScreenHorizontalPadding,
            top = 10.dp,
            bottom = 2.dp
        ),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = VolunteerLinkTextSecondary
    )
}

@Composable
private fun VolunteerSearchResultCard(
    volunteerOpportunityEvent: VolunteerOpportunityEvent,
    onVolunteerOpportunitySelected: () -> Unit
) {
    Card(
        onClick = onVolunteerOpportunitySelected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = VolunteerLinkScreenHorizontalPadding,
                vertical = 5.dp
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.Top
        ) {
            VolunteerOpportunityThumbnail(
                storagePath =
                    volunteerOpportunityEvent.eventThumbnailPath,
                fallbackIconResourceId =
                    R.drawable.ic_volunteer_physical_event,
                modifier = Modifier.size(50.dp),
                contentDescription =
                    "${volunteerOpportunityEvent.eventTitle} thumbnail"
            )

            Spacer(modifier = Modifier.size(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volunteerOpportunityEvent.eventTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = volunteerOpportunityEvent.eventOrganisationName,
                    fontSize = 11.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = buildString {
                        append(volunteerOpportunityEvent.eventLocation)
                        volunteerOpportunityEvent.eventDistanceKm?.let {
                            append(" • $it km")
                        }
                    },
                    fontSize = 11.sp,
                    color =
                        if (
                            volunteerOpportunityEvent.eventOpportunityType ==
                            "Remote"
                        ) VolunteerLinkInformation
                        else VolunteerLinkTextSecondary
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text =
                        "${volunteerOpportunityEvent.eventVolunteerRoles.size} roles" +
                            " • ${volunteerOpportunityEvent.eventAvailableSpots} spots",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkPrimaryGreen
                )
            }

            Text(
                text = "›",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
        }
    }
}

private fun VolunteerOpportunityCategory.displayName(): String =
    name.lowercase().replaceFirstChar(Char::uppercase)
