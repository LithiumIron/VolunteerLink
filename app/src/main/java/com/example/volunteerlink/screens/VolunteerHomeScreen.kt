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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkCardContentPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkCardCornerRadius
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSuccess
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning
import com.example.volunteerlink.model.VolunteerOpportunityCategory

@Composable
fun VolunteerHomeScreen(
    onVolunteerOpportunitySelected: (eventId: Int) -> Unit = {},
    onVolunteerApplicationSelected: (applicationId: Int) -> Unit = {},
    onViewAllApplicationsSelected: () -> Unit = {},
    onVolunteerNotificationsSelected: () -> Unit = {},
    onVolunteerSearchSelected: () -> Unit = {}
) {

    var selectedHomeFilter by rememberSaveable {
        mutableStateOf("All")
    }

    val homeFilterOptions =
        listOf(
            "All",
            "Physical",
            "Near Me",
            "Remote",
            "Long Term"
        )


    val filteredVolunteerOpportunityEvents =
        remember(selectedHomeFilter) {

            VolunteerOpportunitySampleData
                .volunteerOpportunityEvents
                .filter { volunteerOpportunityEvent ->

                    when (selectedHomeFilter) {

                        "Physical" ->
                            volunteerOpportunityEvent
                                .eventOpportunityType == "Physical"

                        "Near Me" ->
                            volunteerOpportunityEvent
                                .eventDistanceKm != null &&
                                    volunteerOpportunityEvent
                                        .eventDistanceKm <= 10.0

                        "Remote" ->
                            volunteerOpportunityEvent
                                .eventOpportunityType == "Remote"

                        "Long Term" ->
                            volunteerOpportunityEvent
                                .eventIsLongTerm

                        else -> true
                    }
                }
        }


    val recommendedVolunteerOpportunityEvents =
        remember(
            VolunteerOpportunitySampleData
                .volunteerOpportunityEvents
        ) {

            VolunteerOpportunitySampleData
                .volunteerOpportunityEvents
                .sortedBy { volunteerOpportunityEvent ->

                    volunteerOpportunityEvent
                        .eventDistanceKm
                        ?: Double.MAX_VALUE
                }
                .take(2)
        }


    val mostPopularVolunteerOpportunityEvents =
        remember(
            VolunteerOpportunitySampleData
                .volunteerOpportunityEvents
        ) {

            VolunteerOpportunitySampleData
                .volunteerOpportunityEvents
                .sortedByDescending { volunteerOpportunityEvent ->

                    volunteerOpportunityEvent
                        .eventApplicationCount
                }
                .take(2)
        }


    val remoteVolunteerOpportunityEvents =
        remember(
            VolunteerOpportunitySampleData
                .volunteerOpportunityEvents
        ) {

            VolunteerOpportunitySampleData
                .volunteerOpportunityEvents
                .filter { volunteerOpportunityEvent ->

                    volunteerOpportunityEvent
                        .eventOpportunityType == "Remote"
                }
        }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                VolunteerLinkBackground
            ),

        contentPadding =
            PaddingValues(
                bottom = 20.dp
            )
    ) {


        item(
            key = "volunteer_home_header"
        ) {

            VolunteerHomeCompactHeader(

                onVolunteerSearchSelected =
                    onVolunteerSearchSelected,

                homeFilterOptions =
                    homeFilterOptions,

                selectedHomeFilter =
                    selectedHomeFilter,

                onHomeFilterSelected = {
                        updatedHomeFilter ->

                    selectedHomeFilter =
                        updatedHomeFilter
                },

                onVolunteerNotificationsSelected =
                    onVolunteerNotificationsSelected
            )
        }


        item(
            key = "volunteer_home_applications"
        ) {

            VolunteerHomeApplicationSection(

                volunteerApplications =
                    VolunteerOpportunitySampleData
                        .volunteerApplications,

                onVolunteerApplicationSelected =
                    onVolunteerApplicationSelected,

                onViewAllApplicationsSelected =
                    onViewAllApplicationsSelected
            )
        }


        if (selectedHomeFilter != "All") {

            item(
                key =
                    "volunteer_home_filtered_results"
            ) {

                VolunteerHomeOpportunitySection(

                    sectionTitle =
                        "Matching Opportunities",

                    volunteerOpportunityEvents =
                        filteredVolunteerOpportunityEvents,

                    onVolunteerOpportunitySelected =
                        onVolunteerOpportunitySelected
                )
            }

        } else {


            item(
                key =
                    "volunteer_home_recommended"
            ) {

                VolunteerHomeOpportunitySection(

                    sectionTitle =
                        "Recommended for You",

                    volunteerOpportunityEvents =
                        recommendedVolunteerOpportunityEvents,

                    onVolunteerOpportunitySelected =
                        onVolunteerOpportunitySelected
                )
            }


            item(
                key =
                    "volunteer_home_popular"
            ) {

                VolunteerHomeOpportunitySection(

                    sectionTitle =
                        "Most Popular",

                    volunteerOpportunityEvents =
                        mostPopularVolunteerOpportunityEvents,

                    onVolunteerOpportunitySelected =
                        onVolunteerOpportunitySelected
                )
            }


            if (
                remoteVolunteerOpportunityEvents
                    .isNotEmpty()
            ) {

                item(
                    key =
                        "volunteer_home_remote"
                ) {

                    VolunteerHomeOpportunitySection(

                        sectionTitle =
                            "Remote Opportunities",

                        volunteerOpportunityEvents =
                            remoteVolunteerOpportunityEvents,

                        onVolunteerOpportunitySelected =
                            onVolunteerOpportunitySelected
                    )
                }
            }
        }
    }
}


@Composable
private fun VolunteerHomeCompactHeader(
    onVolunteerSearchSelected: () -> Unit,
    homeFilterOptions: List<String>,
    selectedHomeFilter: String,
    onHomeFilterSelected: (String) -> Unit,
    onVolunteerNotificationsSelected: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                VolunteerLinkPrimaryGreen
            )
            .statusBarsPadding()
            .padding(
                start =
                    VolunteerLinkScreenHorizontalPadding,
                end =
                    VolunteerLinkScreenHorizontalPadding,
                top = 8.dp,
                bottom = 14.dp
            )
    ) {


        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Text(
                text = "VolunteerLink",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )


            Row(
                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {


                IconButton(
                    onClick =
                        onVolunteerNotificationsSelected
                ) {

                    Icon(
                        painter =
                            painterResource(
                                id =
                                    R.drawable
                                        .ic_volunteer_notifications
                            ),

                        contentDescription =
                            "Notifications",

                        tint = Color.White,

                        modifier =
                            Modifier.size(22.dp)
                    )
                }


                Surface(
                    modifier =
                        Modifier.size(32.dp),

                    shape = CircleShape,

                    color = Color.White
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "MH",
                            color =
                                VolunteerLinkPrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(9.dp)
        )


        Surface(
            onClick =
                onVolunteerSearchSelected,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(46.dp),

            shape =
                RoundedCornerShape(11.dp),

            color = Color.White
        ) {

            Row(
                modifier =
                    Modifier.padding(
                        horizontal = 14.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Icon(
                    painter =
                        painterResource(
                            id =
                                R.drawable
                                    .ic_volunteer_search
                        ),

                    contentDescription =
                        "Search opportunities",

                    tint =
                        VolunteerLinkTextSecondary,

                    modifier =
                        Modifier.size(19.dp)
                )


                Text(
                    text =
                        "Search opportunities...",

                    fontSize = 13.sp,

                    color =
                        VolunteerLinkTextSecondary
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(9.dp)
        )


        LazyRow(
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {

            items(
                items =
                    homeFilterOptions,

                key = {
                        homeFilterOption ->

                    "home_filter_$homeFilterOption"
                }
            ) { homeFilterOption ->


                val homeFilterIsSelected =
                    selectedHomeFilter ==
                            homeFilterOption


                FilterChip(
                    selected =
                        homeFilterIsSelected,

                    onClick = {

                        onHomeFilterSelected(
                            homeFilterOption
                        )
                    },

                    label = {

                        Text(
                            text =
                                homeFilterOption,

                            fontSize = 11.sp
                        )
                    },

                    shape =
                        RoundedCornerShape(20.dp),

                    colors =
                        FilterChipDefaults
                            .filterChipColors(

                                containerColor =
                                    Color.White.copy(
                                        alpha = 0.12f
                                    ),

                                labelColor =
                                    Color.White.copy(
                                        alpha = 0.90f
                                    ),

                                selectedContainerColor =
                                    Color.White,

                                selectedLabelColor =
                                    VolunteerLinkPrimaryGreen
                            )
                )
            }
        }
    }
}

@Composable
private fun VolunteerHomeCompactHeader(
    homeSearchQuery: String,
    onHomeSearchQueryChanged: (String) -> Unit,
    homeFilterOptions: List<String>,
    selectedHomeFilter: String,
    onHomeFilterSelected: (String) -> Unit,
    onVolunteerNotificationsSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkSurface)
            .padding(
                horizontal = VolunteerLinkScreenHorizontalPadding,
                vertical = 8.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VolunteerLink",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onVolunteerNotificationsSelected
                ) {
                    Icon(
                        painter = painterResource(
                            id = R.drawable.ic_volunteer_notifications
                        ),
                        contentDescription = "Notifications",
                        tint = VolunteerLinkTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = VolunteerLinkPrimaryGreen
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MH",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = homeSearchQuery,
            onValueChange = onHomeSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            placeholder = {
                Text(
                    text = "Search opportunities...",
                    fontSize = 13.sp
                )
            },
            singleLine = true,
            leadingIcon = {
                Icon(
                    painter = painterResource(
                        id = R.drawable.ic_volunteer_search
                    ),
                    contentDescription = "Search opportunities",
                    tint = VolunteerLinkTextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            },
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor =
                    VolunteerLinkPrimaryGreen,
                unfocusedBorderColor =
                    VolunteerLinkBorderColour,
                focusedContainerColor =
                    VolunteerLinkBackground,
                unfocusedContainerColor =
                    VolunteerLinkBackground,
                cursorColor =
                    VolunteerLinkPrimaryGreen
            )
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(
                items = homeFilterOptions,
                key = { homeFilterOption ->
                    "home_filter_$homeFilterOption"
                }
            ) { homeFilterOption ->

                FilterChip(
                    selected =
                        selectedHomeFilter == homeFilterOption,
                    onClick = {
                        onHomeFilterSelected(homeFilterOption)
                    },
                    label = {
                        Text(
                            text = homeFilterOption,
                            fontSize = 12.sp
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors =
                        FilterChipDefaults.filterChipColors(
                            containerColor =
                                VolunteerLinkSoftGreenSurface,
                            labelColor =
                                VolunteerLinkTextSecondary,
                            selectedContainerColor =
                                VolunteerLinkPrimaryGreen,
                            selectedLabelColor =
                                Color.White
                        )
                )
            }
        }
    }
}

@Composable
private fun VolunteerHomeApplicationSection(
    volunteerApplications:
    List<VolunteerOpportunityApplication>,
    onVolunteerApplicationSelected:
        (applicationId: Int) -> Unit,
    onViewAllApplicationsSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = VolunteerLinkScreenHorizontalPadding,
                end = VolunteerLinkScreenHorizontalPadding,
                top = 14.dp
            )
    ) {
        VolunteerHomeSectionTitleRow(
            sectionTitle = "My Applications",
            sectionActionText = "See all",
            onSectionActionSelected =
                onViewAllApplicationsSelected
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (volunteerApplications.isEmpty()) {
            VolunteerHomeEmptyCard(
                emptyCardMessage =
                    "You haven't applied to any events yet."
            )
        } else {
            volunteerApplications
                .take(2)
                .forEach { volunteerApplication ->

                    VolunteerHomeCompactApplicationCard(
                        volunteerApplication =
                            volunteerApplication,
                        onVolunteerApplicationSelected = {
                            onVolunteerApplicationSelected(
                                volunteerApplication.applicationId
                            )
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                }
        }
    }
}

@Composable
private fun VolunteerHomeOpportunitySection(
    sectionTitle: String,
    volunteerOpportunityEvents:
    List<VolunteerOpportunityEvent>,
    onVolunteerOpportunitySelected:
        (eventId: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = VolunteerLinkScreenHorizontalPadding,
                end = VolunteerLinkScreenHorizontalPadding,
                top = 18.dp
            )
    ) {
        VolunteerHomeSectionTitleRow(
            sectionTitle = sectionTitle
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (volunteerOpportunityEvents.isEmpty()) {
            VolunteerHomeEmptyCard(
                emptyCardMessage =
                    "No opportunities found."
            )
        } else {
            volunteerOpportunityEvents
                .forEach { volunteerOpportunityEvent ->

                    VolunteerHomeCompactCard(
                        volunteerOpportunityEvent =
                            volunteerOpportunityEvent,
                        onVolunteerOpportunitySelected = {
                            onVolunteerOpportunitySelected(
                                volunteerOpportunityEvent.eventId
                            )
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }
        }
    }
}

@Composable
private fun VolunteerHomeSectionTitleRow(
    sectionTitle: String,
    sectionActionText: String? = null,
    onSectionActionSelected: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sectionTitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = VolunteerLinkTextPrimary
        )

        if (sectionActionText != null) {
            TextButton(
                onClick = onSectionActionSelected,
                contentPadding = PaddingValues(
                    horizontal = 4.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    text = sectionActionText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkPrimaryGreen
                )
            }
        }
    }
}

@Composable
private fun VolunteerHomeCompactApplicationCard(
    volunteerApplication:
    VolunteerOpportunityApplication,
    onVolunteerApplicationSelected: () -> Unit
) {
    Card(
        onClick = onVolunteerApplicationSelected,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkBorderColour
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        volunteerApplication.applicationEventTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text =
                        volunteerApplication.applicationRoleTitle,
                    fontSize = 11.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier = Modifier.size(8.dp)
            )

            VolunteerHomeCompactStatusBadge(
                applicationStatus =
                    volunteerApplication.applicationStatus
            )
        }
    }
}

@Composable
private fun VolunteerHomeCompactCard(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    onVolunteerOpportunitySelected: () -> Unit
) {
    val primaryVolunteerRole =
        volunteerOpportunityEvent
            .eventVolunteerRoles
            .firstOrNull()

    val opportunityCategoryIconResourceId =
        when (
            volunteerOpportunityEvent
                .eventCategory
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

    val opportunityLocationIconResourceId =
        if (
            volunteerOpportunityEvent.eventOpportunityType ==
            "Remote"
        ) {
            R.drawable.ic_volunteer_remote_project
        } else {
            R.drawable.ic_volunteer_location
        }

    Card(
        onClick = onVolunteerOpportunitySelected,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            VolunteerLinkCardCornerRadius
        ),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkBorderColour
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(
                VolunteerLinkCardContentPadding
            ),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(10.dp),
                color = VolunteerLinkSoftGreenSurface
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = opportunityCategoryIconResourceId
                        ),
                        contentDescription =
                            "${volunteerOpportunityEvent.eventOpportunityType} opportunity",
                        tint = VolunteerLinkPrimaryGreen,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        volunteerOpportunityEvent.eventTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text =
                            volunteerOpportunityEvent
                                .eventOrganisationName,
                        fontSize = 11.sp,
                        color = VolunteerLinkTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(
                            1f,
                            fill = false
                        )
                    )

                    if (
                        volunteerOpportunityEvent
                            .eventIsVerifiedOrganisation
                    ) {
                        VolunteerHomeVerifiedBadge()
                    }
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = R.drawable.ic_volunteer_calendar
                        ),
                        contentDescription = "Event date",
                        tint = VolunteerLinkTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )

                    Text(
                        text =
                            volunteerOpportunityEvent.eventDate,
                        fontSize = 11.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id =
                                opportunityLocationIconResourceId
                        ),
                        contentDescription =
                            "Event location",
                        tint =
                            if (
                                volunteerOpportunityEvent
                                    .eventOpportunityType ==
                                "Remote"
                            ) {
                                VolunteerLinkInformation
                            } else {
                                VolunteerLinkTextSecondary
                            },
                        modifier = Modifier.size(12.dp)
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
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(5.dp)
                    ) {
                        if (primaryVolunteerRole != null) {
                            VolunteerHomeCompactTag(
                                tagText =
                                    primaryVolunteerRole.roleLevel,
                                tagIsHighlighted =
                                    primaryVolunteerRole.roleLevel ==
                                            "Beginner"
                            )
                        }

                        VolunteerHomeCompactTag(
                            tagText =
                                "${volunteerOpportunityEvent.eventVolunteerRoles.size} roles"
                        )
                    }

                    Surface(
                        onClick =
                            onVolunteerOpportunitySelected,
                        shape = RoundedCornerShape(6.dp),
                        color = VolunteerLinkPrimaryGreen
                    ) {
                        Text(
                            text = "View Details",
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolunteerHomeVerifiedBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFE8F5E9)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 5.dp,
                vertical = 2.dp
            ),
            horizontalArrangement =
                Arrangement.spacedBy(2.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    id = R.drawable.ic_volunteer_verified
                ),
                contentDescription = "Verified organisation",
                tint = Color.Unspecified,
                modifier = Modifier.size(11.dp)
            )

            Text(
                text = "Verified",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = VolunteerLinkSuccess
            )
        }
    }
}

@Composable
private fun VolunteerHomeCompactTag(
    tagText: String,
    tagIsHighlighted: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color =
            if (tagIsHighlighted) {
                Color(0xFFE8F5E9)
            } else {
                VolunteerLinkSoftGreenSurface
            }
    ) {
        Text(
            text = tagText,
            modifier = Modifier.padding(
                horizontal = 6.dp,
                vertical = 2.dp
            ),
            fontSize = 10.sp,
            fontWeight =
                if (tagIsHighlighted) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
            color =
                if (tagIsHighlighted) {
                    VolunteerLinkSuccess
                } else {
                    VolunteerLinkTextSecondary
                }
        )
    }
}

@Composable
private fun VolunteerHomeCompactStatusBadge(
    applicationStatus: VolunteerApplicationStatus
) {
    val statusText =
        when (applicationStatus) {
            VolunteerApplicationStatus.PENDING ->
                "Pending"

            VolunteerApplicationStatus.ACCEPTED ->
                "Accepted"

            VolunteerApplicationStatus.REJECTED ->
                "Rejected"

            VolunteerApplicationStatus.COMPLETED ->
                "Completed"

            VolunteerApplicationStatus.CANCELLED ->
                "Cancelled"
        }

    val statusTextColour =
        when (applicationStatus) {
            VolunteerApplicationStatus.PENDING ->
                VolunteerLinkWarning

            VolunteerApplicationStatus.ACCEPTED ->
                VolunteerLinkSuccess

            VolunteerApplicationStatus.REJECTED ->
                VolunteerLinkError

            VolunteerApplicationStatus.COMPLETED ->
                VolunteerLinkInformation

            VolunteerApplicationStatus.CANCELLED ->
                VolunteerLinkTextSecondary
        }

    val statusBackgroundColour =
        when (applicationStatus) {
            VolunteerApplicationStatus.PENDING ->
                Color(0xFFFFF3E0)

            VolunteerApplicationStatus.ACCEPTED ->
                Color(0xFFE8F5E9)

            VolunteerApplicationStatus.REJECTED ->
                Color(0xFFFFEBEE)

            VolunteerApplicationStatus.COMPLETED ->
                Color(0xFFE3F2FD)

            VolunteerApplicationStatus.CANCELLED ->
                Color(0xFFF1F1F1)
        }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = statusBackgroundColour
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(
                horizontal = 7.dp,
                vertical = 3.dp
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = statusTextColour
        )
    }
}

@Composable
private fun VolunteerHomeEmptyCard(
    emptyCardMessage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkBorderColour
        )
    ) {
        Text(
            text = emptyCardMessage,
            modifier = Modifier.padding(16.dp),
            fontSize = 12.sp,
            color = VolunteerLinkTextSecondary
        )
    }
}