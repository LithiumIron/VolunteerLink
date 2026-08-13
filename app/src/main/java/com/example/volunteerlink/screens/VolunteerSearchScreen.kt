package com.example.volunteerlink.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.data.VolunteerOpportunitySampleData
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

@Composable
fun VolunteerSearchScreen(
    onBackSelected: () -> Unit,
    onVolunteerOpportunitySelected: (
        eventId: Int
    ) -> Unit
) {
    var volunteerSearchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var selectedSearchFilter by rememberSaveable {
        mutableStateOf("All")
    }

    val searchFilterOptions =
        listOf(
            "All",
            "Physical",
            "Near Me",
            "Remote",
            "Long Term"
        )

    val filteredVolunteerOpportunities =
        VolunteerOpportunitySampleData
            .volunteerOpportunityEvents
            .filter { volunteerOpportunityEvent ->
                val matchesSearchQuery =
                    volunteerSearchQuery.isBlank() ||
                            volunteerOpportunityEvent
                                .eventTitle
                                .contains(
                                    volunteerSearchQuery,
                                    ignoreCase = true
                                ) ||
                            volunteerOpportunityEvent
                                .eventOrganisationName
                                .contains(
                                    volunteerSearchQuery,
                                    ignoreCase = true
                                ) ||
                            volunteerOpportunityEvent
                                .eventLocation
                                .contains(
                                    volunteerSearchQuery,
                                    ignoreCase = true
                                ) ||
                            volunteerOpportunityEvent
                                .eventVolunteerRoles
                                .any { volunteerRole ->
                                    volunteerRole.roleTitle.contains(
                                        volunteerSearchQuery,
                                        ignoreCase = true
                                    ) ||
                                            volunteerRole
                                                .roleSkillsPractised
                                                .any { roleSkill ->
                                                    roleSkill.contains(
                                                        volunteerSearchQuery,
                                                        ignoreCase = true
                                                    )
                                                }
                                }

                val matchesSelectedFilter =
                    when (selectedSearchFilter) {
                        "Physical" ->
                            volunteerOpportunityEvent
                                .eventOpportunityType ==
                                    "Physical"

                        "Near Me" ->
                            volunteerOpportunityEvent
                                .eventDistanceKm != null &&
                                    volunteerOpportunityEvent
                                        .eventDistanceKm <= 10.0

                        "Remote" ->
                            volunteerOpportunityEvent
                                .eventOpportunityType ==
                                    "Remote"

                        "Long Term" ->
                            volunteerOpportunityEvent
                                .eventIsLongTerm

                        else -> true
                    }

                matchesSearchQuery &&
                        matchesSelectedFilter
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
                .height(56.dp)
                .padding(
                    start = 4.dp,
                    end = 16.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackSelected
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Find Opportunities",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        OutlinedTextField(
            value = volunteerSearchQuery,
            onValueChange = {
                    updatedSearchQuery ->
                volunteerSearchQuery =
                    updatedSearchQuery
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start =
                        VolunteerLinkScreenHorizontalPadding,
                    end =
                        VolunteerLinkScreenHorizontalPadding,
                    top = 14.dp
                ),
            placeholder = {
                Text(
                    text =
                        "Search event, organisation, role or skill"
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(
                        id =
                            R.drawable
                                .ic_volunteer_search
                    ),
                    contentDescription = "Search",
                    tint =
                        VolunteerLinkTextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor =
                        VolunteerLinkSurface,
                    unfocusedContainerColor =
                        VolunteerLinkSurface,
                    focusedBorderColor =
                        VolunteerLinkPrimaryGreen,
                    unfocusedBorderColor =
                        VolunteerLinkBorderColour,
                    cursorColor =
                        VolunteerLinkPrimaryGreen
                )
        )

        LazyRow(
            modifier = Modifier.padding(
                top = 10.dp
            ),
            contentPadding = PaddingValues(
                horizontal =
                    VolunteerLinkScreenHorizontalPadding
            ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = searchFilterOptions,
                key = { searchFilter ->
                    searchFilter
                }
            ) { searchFilter ->
                FilterChip(
                    selected =
                        selectedSearchFilter ==
                                searchFilter,
                    onClick = {
                        selectedSearchFilter =
                            searchFilter
                    },
                    label = {
                        Text(
                            text = searchFilter,
                            fontSize = 11.sp
                        )
                    },
                    shape =
                        RoundedCornerShape(18.dp),
                    colors =
                        FilterChipDefaults
                            .filterChipColors(
                                containerColor =
                                    VolunteerLinkSurface,
                                labelColor =
                                    VolunteerLinkTextSecondary,
                                selectedContainerColor =
                                    VolunteerLinkPrimaryGreen,
                                selectedLabelColor =
                                    Color.White
                            ),
                    border =
                        FilterChipDefaults
                            .filterChipBorder(
                                enabled = true,
                                selected =
                                    selectedSearchFilter ==
                                            searchFilter,
                                borderColor =
                                    VolunteerLinkBorderColour,
                                selectedBorderColor =
                                    VolunteerLinkPrimaryGreen
                            )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        VolunteerLinkScreenHorizontalPadding,
                    vertical = 12.dp
                ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = "Search Results",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Text(
                text =
                    "${filteredVolunteerOpportunities.size} found",
                fontSize = 11.sp,
                color = VolunteerLinkTextSecondary
            )
        }

        if (
            filteredVolunteerOpportunities
                .isEmpty()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {
                Text(
                    text = "No opportunities found",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Try another keyword or filter.",
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start =
                        VolunteerLinkScreenHorizontalPadding,
                    end =
                        VolunteerLinkScreenHorizontalPadding,
                    bottom = 24.dp
                ),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items =
                        filteredVolunteerOpportunities,
                    key = {
                            volunteerOpportunityEvent ->
                        volunteerOpportunityEvent.eventId
                    }
                ) { volunteerOpportunityEvent ->
                    VolunteerSearchResultCard(
                        volunteerOpportunityEvent =
                            volunteerOpportunityEvent,
                        onVolunteerOpportunitySelected = {
                            onVolunteerOpportunitySelected(
                                volunteerOpportunityEvent
                                    .eventId
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VolunteerSearchResultCard(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    onVolunteerOpportunitySelected: () -> Unit
) {
    val categoryIconResourceId =
        when (
            volunteerOpportunityEvent.eventCategory
        ) {
            VolunteerOpportunityCategory.SPORTS ->
                R.drawable
                    .ic_volunteer_category_sports

            VolunteerOpportunityCategory.COMMUNITY ->
                R.drawable
                    .ic_volunteer_category_community

            VolunteerOpportunityCategory.EDUCATION ->
                R.drawable
                    .ic_volunteer_category_education

            else ->
                R.drawable
                    .ic_volunteer_physical_event
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick =
                    onVolunteerOpportunitySelected
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkBorderColour
        )
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(10.dp),
                color = VolunteerLinkSoftGreenSurface
            ) {
                Box(
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id =
                                categoryIconResourceId
                        ),
                        contentDescription = null,
                        tint =
                            VolunteerLinkPrimaryGreen,
                        modifier =
                            Modifier.size(25.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(11.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        volunteerOpportunityEvent
                            .eventTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 2,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text =
                        volunteerOpportunityEvent
                            .eventOrganisationName,
                    fontSize = 11.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Text(
                    text =
                        if (
                            volunteerOpportunityEvent
                                .eventDistanceKm != null
                        ) {
                            "${volunteerOpportunityEvent.eventLocation}" +
                                    " • " +
                                    "${volunteerOpportunityEvent.eventDistanceKm} km"
                        } else {
                            volunteerOpportunityEvent
                                .eventLocation
                        },
                    fontSize = 11.sp,
                    color =
                        if (
                            volunteerOpportunityEvent
                                .eventOpportunityType ==
                            "Remote"
                        ) {
                            VolunteerLinkInformation
                        } else {
                            VolunteerLinkTextSecondary
                        }
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Text(
                    text =
                        "${volunteerOpportunityEvent.eventVolunteerRoles.size} " +
                                "roles • " +
                                "${volunteerOpportunityEvent.eventAvailableSpots} " +
                                "spots",
                    fontSize = 10.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        VolunteerLinkPrimaryGreen
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