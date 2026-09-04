
package com.example.volunteerlink.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.R
import com.example.volunteerlink.data.VolunteerHomeFeedEngine
import com.example.volunteerlink.data.VolunteerHomeFeedFilter
import com.example.volunteerlink.data.VolunteerHomeRecommendation
import com.example.volunteerlink.data.VolunteerHomeRecommendationEngine
import com.example.volunteerlink.data.VolunteerMatchFactor
import com.example.volunteerlink.data.VolunteerMatchFactorStatus
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.data.VolunteerPromotionEngine
import com.example.volunteerlink.data.location.DeviceLocationHelper
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
import com.example.volunteerlink.ui.theme.VolunteerLinkTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun VolunteerHomeScreen(
    onVolunteerOpportunitySelected: (eventId: Int) -> Unit = {},
    onVolunteerRoleSelected: (eventId: Int, roleId: Int) -> Unit = { _, _ -> },
    onVolunteerApplicationSelected: (applicationId: Int) -> Unit = {},
    onViewAllApplicationsSelected: () -> Unit = {},
    onVolunteerNotificationsSelected: () -> Unit = {},
    onVolunteerFavouritesSelected: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    onVolunteerSearchSelected: () -> Unit = {},
    isShowingCachedData: Boolean = false,
    syncWarning: String? = null,
    lastSyncedAtEpochMillis: Long? = null,
    onSyncSelected: () -> Unit = {},
    skillPathViewModel:
        VolunteerSkillPathViewModel = viewModel()
) {
    val context = LocalContext.current
    val businessNow = volunteerBusinessTime()
    val applicationClock by com.example.volunteerlink.data.time.AppClock.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedHomeFilter by rememberSaveable {
        mutableStateOf(VolunteerHomeFeedFilter.FOR_YOU)
    }
    val matchLocationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                DeviceLocationHelper.getApproximateCurrentLocation(context) {
                        location ->
                    location?.let {
                        VolunteerOpportunitySessionStore
                            .updateDistancesFromDevice(
                                latitude = it.latitude,
                                longitude = it.longitude
                            )
                    }
                }
            }
        }

    val requestLocationAccess: () -> Unit = {
        matchLocationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Ask through Android directly on every new For You visit while access is
    // disabled. The app never redirects the volunteer to Settings.
    LaunchedEffect(selectedHomeFilter) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (
            selectedHomeFilter == VolunteerHomeFeedFilter.FOR_YOU &&
            !fineGranted &&
            !coarseGranted
        ) {
            requestLocationAccess()
        } else if (fineGranted || coarseGranted) {
            DeviceLocationHelper.getApproximateCurrentLocation(context) {
                    location ->
                location?.let {
                    VolunteerOpportunitySessionStore
                        .updateDistancesFromDevice(
                            latitude = it.latitude,
                            longitude = it.longitude
                        )
                }
            }
        }
    }

    // Refresh distances immediately after the app resumes with location access.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val locationGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                if (locationGranted) {
                    DeviceLocationHelper.getApproximateCurrentLocation(context) {
                            location ->
                        location?.let {
                            VolunteerOpportunitySessionStore
                                .updateDistancesFromDevice(
                                    latitude = it.latitude,
                                    longitude = it.longitude
                                )
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val homeFilterOptions =
        VolunteerHomeFeedFilter.entries

    val skillPathUiState by
        skillPathViewModel.uiState
            .collectAsStateWithLifecycle()

    val currentSkillPathLevels =
        remember(skillPathUiState.skillPaths) {
            skillPathUiState.skillPaths
                .associate { skillPath ->
                    skillPath.name to
                        skillPath.currentLevel
                }
        }

    val currentVolunteerInitials =
        volunteerInitials(
            VolunteerOpportunitySessionStore.profileData?.fullName
        )

    val allVolunteerOpportunityEvents =
        VolunteerOpportunitySessionStore
            .volunteerOpportunityEvents
            .toList()

    val allVolunteerApplications =
        VolunteerOpportunitySessionStore
            .volunteerApplications
            .toList()

    val filteredVolunteerOpportunityEvents =
        remember(
            selectedHomeFilter,
            businessNow,
            applicationClock,
            allVolunteerApplications,
            allVolunteerOpportunityEvents
        ) {
            VolunteerHomeFeedEngine.filter(
                events = allVolunteerOpportunityEvents,
                filter = selectedHomeFilter,
                applications = allVolunteerApplications,
                nowMillis = businessNow
            )
        }

    val promotionFeed = rememberVolunteerPromotionFeed(isShowingCachedData)
    val activePromotions = VolunteerPromotionEngine.activeByPost(promotionFeed.entries, businessNow)
    val orderedVolunteerOpportunityEvents = VolunteerPromotionEngine.prioritize(
        filteredVolunteerOpportunityEvents, promotionFeed.entries, businessNow
    )
    val featuredVolunteerOpportunityEvents = orderedVolunteerOpportunityEvents.filter {
        activePromotions.containsKey(it.eventDatabaseId)
    }

    val recommendedVolunteerOpportunityEvents =
        remember(
            allVolunteerOpportunityEvents,
            businessNow,
            applicationClock,
            allVolunteerApplications,
            currentSkillPathLevels
        ) {
            VolunteerHomeRecommendationEngine.recommend(
                volunteerOpportunityEvents =
                    allVolunteerOpportunityEvents,
                volunteerApplications =
                    allVolunteerApplications,
                currentSkillPathLevels =
                    currentSkillPathLevels,
                nowMillis = businessNow
            ).take(4)
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                VolunteerLinkBackground
            ),

        contentPadding =
            PaddingValues(
                bottom = 110.dp
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

                onVolunteerFavouritesSelected = onVolunteerFavouritesSelected,
                onVolunteerNotificationsSelected =
                    onVolunteerNotificationsSelected,

                unreadNotificationCount =
                    unreadNotificationCount,

                volunteerInitials =
                    currentVolunteerInitials
            )
        }

        if (syncWarning != null) {
            item(key = "sync_warning") {
                Column(Modifier.padding(16.dp)) {
                    Text(syncWarning, color = Color(0xFF895B00))
                    TextButton(onClick = onSyncSelected) { Text("Sync application result") }
                }
            }
        }
        if (isShowingCachedData) {
            item(key = "volunteer_home_offline_status") {
                VolunteerOfflineStatusCard(
                    lastSyncedAtEpochMillis = lastSyncedAtEpochMillis,
                    onSyncSelected = onSyncSelected
                )
            }
        }


        if (promotionFeed.failed) {
            item(key = "promotion_load_notice") { VolunteerPromotionLoadNotice(promotionFeed.retry) }
        }
        if (featuredVolunteerOpportunityEvents.isNotEmpty()) {
            item(key = "volunteer_promoted_opportunities") {
                Column(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                    VolunteerPromotionSection(featuredVolunteerOpportunityEvents, onVolunteerOpportunitySelected)
                }
            }
        }

        item(
            key = "volunteer_home_impact"
        ) {
            VolunteerHomeImpactSummary(
                volunteerApplications =
                    allVolunteerApplications
            )
        }


        item(
            key = "volunteer_home_applications"
        ) {

                VolunteerHomeApplicationSection(

                    volunteerApplications =
                        allVolunteerApplications,

                onVolunteerApplicationSelected =
                    onVolunteerApplicationSelected,

                onViewAllApplicationsSelected =
                    onViewAllApplicationsSelected
            )
        }


        if (
            selectedHomeFilter ==
                VolunteerHomeFeedFilter.FOR_YOU
        ) {

            item(
                key =
                    "volunteer_home_recommended"
            ) {

                VolunteerHomeRecommendationSection(

                    sectionTitle =
                        "Your Best Matches",

                    recommendations =
                        recommendedVolunteerOpportunityEvents,

                    onVolunteerOpportunitySelected =
                        onVolunteerOpportunitySelected,

                    onVolunteerRoleSelected =
                        onVolunteerRoleSelected,

                    onEnableLocationSelected =
                        requestLocationAccess
                )
            }

        } else {

            item(
                key =
                    "volunteer_home_filtered_results"
            ) {

                VolunteerHomeOpportunitySection(

                    sectionTitle =
                        if (
                            selectedHomeFilter ==
                            VolunteerHomeFeedFilter.ALL
                        ) {
                            "All Opportunities"
                        } else {
                            "${selectedHomeFilter.displayName} Opportunities"
                        },

                    volunteerOpportunityEvents =
                        orderedVolunteerOpportunityEvents,
                    promotedPostIds = activePromotions.keys,

                    onVolunteerOpportunitySelected =
                        onVolunteerOpportunitySelected
                )
            }
        }
    }
}

@Composable
private fun VolunteerOfflineStatusCard(
    lastSyncedAtEpochMillis: Long?,
    onSyncSelected: () -> Unit
) {
    val lastSyncText = remember(lastSyncedAtEpochMillis) {
        lastSyncedAtEpochMillis?.let { timestamp ->
            DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
            ).format(Date(timestamp))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = VolunteerLinkScreenHorizontalPadding,
                vertical = 10.dp
            ),
        shape = RoundedCornerShape(16.dp),
        color = VolunteerLinkWarning.copy(alpha = 0.13f),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkWarning.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Offline data",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = lastSyncText?.let {
                        "Showing your last successful sync from $it."
                    } ?: "Showing your last saved volunteer data.",
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            TextButton(onClick = onSyncSelected) {
                Text(
                    text = "SYNC",
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
            }
        }
    }
}


@Composable
private fun VolunteerHomeCompactHeader(
    onVolunteerSearchSelected: () -> Unit,
    homeFilterOptions: List<VolunteerHomeFeedFilter>,
    selectedHomeFilter: VolunteerHomeFeedFilter,
    onHomeFilterSelected:
        (VolunteerHomeFeedFilter) -> Unit,
    onVolunteerNotificationsSelected: () -> Unit,
    onVolunteerFavouritesSelected: () -> Unit,
    unreadNotificationCount: Int,
    volunteerInitials: String
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
                    Arrangement.spacedBy(0.dp)
            ) {


                IconButton(onClick = onVolunteerFavouritesSelected, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.FavoriteBorder, contentDescription = "Favourites", tint = Color.White,
                        modifier = Modifier.offset(x = 3.dp).size(22.dp))
                }
                Box(Modifier.size(48.dp)) {
                    IconButton(
                        onClick =
                            onVolunteerNotificationsSelected
                    ) {
                        Icon(
                            painter = painterResource(
                                id = R.drawable
                                    .ic_volunteer_notifications
                            ),
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.offset(x = (-3).dp).size(22.dp)
                        )
                    }

                    if (unreadNotificationCount > 0) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-3).dp)
                                .size(18.dp),
                            shape = CircleShape,
                            color = VolunteerLinkError
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (
                                        unreadNotificationCount > 9
                                    ) "9+" else unreadNotificationCount.toString(),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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
                            text = volunteerInitials,
                            color = VolunteerLinkPrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
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
                                homeFilterOption
                                    .displayName,

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
private fun VolunteerHomeImpactSummary(
    volunteerApplications:
        List<VolunteerOpportunityApplication>
) {
    val completedApplications =
        volunteerApplications.filter { application ->
            application.applicationStatus ==
                VolunteerApplicationStatus.COMPLETED
        }
    val verifiedMinutes =
        completedApplications.sumOf { application ->
            application.applicationVerifiedMinutes ?: 0
        }
    val activeApplications =
        volunteerApplications.count { application ->
            application.applicationStatus ==
                VolunteerApplicationStatus.PENDING ||
                application.applicationStatus ==
                VolunteerApplicationStatus.ACCEPTED
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = VolunteerLinkScreenHorizontalPadding,
                end = VolunteerLinkScreenHorizontalPadding,
                top = 14.dp
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkPrimaryGreen.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Your Volunteer Impact",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text =
                            "Only organisation-verified completions " +
                                "count towards your record.",
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Text(
                        text = "LIVE RECORD",
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VolunteerHomeImpactMetric(
                    value = completedApplications.size.toString(),
                    label = "Completed roles",
                    modifier = Modifier.weight(1f)
                )
                VolunteerHomeImpactMetric(
                    value = formatVolunteerImpactMinutes(verifiedMinutes),
                    label = "Verified service",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VolunteerHomeImpactMetric(
                    value = completedApplications.count {
                        it.applicationCertificateId != null
                    }.toString(),
                    label = "Certificates",
                    modifier = Modifier.weight(1f)
                )
                VolunteerHomeImpactMetric(
                    value = activeApplications.toString(),
                    label = "Active applications",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun VolunteerHomeImpactMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = VolunteerLinkSoftGreenSurface
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 9.dp
            )
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = VolunteerLinkTextSecondary
            )
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
private fun VolunteerHomeRecommendationSection(
    sectionTitle: String,
    recommendations: List<VolunteerHomeRecommendation>,
    onVolunteerOpportunitySelected:
        (eventId: Int) -> Unit,
    onVolunteerRoleSelected:
        (eventId: Int, roleId: Int) -> Unit,
    onEnableLocationSelected: () -> Unit
) {
    var selectedRecommendation by remember {
        mutableStateOf<VolunteerHomeRecommendation?>(
            null
        )
    }

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
        Text(
            text =
                "Ranked from verified Skill Path evidence, role eligibility " +
                    "and practical access.",
            modifier = Modifier.padding(top = 2.dp),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = VolunteerLinkTextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (recommendations.isEmpty()) {
            VolunteerHomeEmptyCard(
                emptyCardMessage =
                    "No new recommendations are available right now."
            )
        } else {
            recommendations.forEach { recommendation ->
                VolunteerHomeCompactCard(
                    volunteerOpportunityEvent =
                        recommendation.event,
                    recommendation =
                        recommendation,
                    onMatchDetailsSelected = {
                        selectedRecommendation =
                            recommendation
                    },
                    onVolunteerOpportunitySelected = {
                        onVolunteerRoleSelected(
                            recommendation.event.eventId,
                            recommendation.bestRoleId
                        )
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    selectedRecommendation?.let { recommendation ->
        VolunteerMatchDetailsSheet(
            recommendation = recommendation,
            onDismissRequest = {
                selectedRecommendation = null
            },
            onViewOpportunitySelected = {
                selectedRecommendation = null
                onVolunteerRoleSelected(
                    recommendation.event.eventId,
                    recommendation.bestRoleId
                )
            },
            onEnableLocationSelected = {
                onEnableLocationSelected()
            }
        )
    }
}

@Composable
private fun VolunteerHomeOpportunitySection(
    sectionTitle: String,
    volunteerOpportunityEvents:
    List<VolunteerOpportunityEvent>,
    promotedPostIds: Set<String> = emptySet(),
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
                        isPromoted = volunteerOpportunityEvent.eventDatabaseId in promotedPostIds,
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
internal fun VolunteerHomeCompactCard(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    recommendation:
        VolunteerHomeRecommendation? = null,
    onMatchDetailsSelected: () -> Unit = {},
    availabilityNotice: String? = null,
    isPromoted: Boolean = false,
    onVolunteerOpportunitySelected: () -> Unit
) {
    val primaryVolunteerRole =
        recommendation?.let { match ->
            volunteerOpportunityEvent
                .eventVolunteerRoles
                .firstOrNull { role ->
                    role.roleId == match.bestRoleId
                }
        } ?: volunteerOpportunityEvent
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
            VolunteerOpportunityThumbnail(
                storagePath =
                    volunteerOpportunityEvent.eventThumbnailPath,
                fallbackIconResourceId =
                    opportunityCategoryIconResourceId,
                modifier = Modifier.size(52.dp),
                contentDescription =
                    "${volunteerOpportunityEvent.eventTitle} thumbnail"
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (isPromoted) {
                    Surface(color = VolunteerLinkSoftGreenSurface, shape = RoundedCornerShape(6.dp)) {
                        Text("Promoted", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = VolunteerLinkPrimaryGreen)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text =
                            volunteerOpportunityEvent.eventTitle,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    recommendation?.let { match ->
                        VolunteerMatchScoreChip(
                            score = match.score,
                            onSelected =
                                onMatchDetailsSelected
                        )
                    }
                }

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
                            listOf(volunteerOpportunityEvent.eventDate, volunteerOpportunityEvent.eventEndDate)
                                .filter(String::isNotBlank).distinct().joinToString(" – "),
                        fontSize = 11.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                if (!com.example.volunteerlink.data.VolunteerApplicationWindow.canApply(volunteerOpportunityEvent)) {
                    Text("Applications closed · details available", color = Color(0xFF686868), fontSize = 11.sp)
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

                availabilityNotice?.let { notice ->
                    Text(notice, color = VolunteerLinkTextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.height(7.dp))
                }
                recommendation?.let { match ->
                    Text(
                        text =
                            "Best role match: ${match.bestRoleTitle}",
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = VolunteerLinkPrimaryGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )
                }

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
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text =
                                    if (recommendation != null) {
                                        "View Role Details"
                                    } else {
                                        "View Event Details"
                                    },
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 7.dp
                                ),
                                maxLines = 1,
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
}

@Composable
private fun VolunteerMatchScoreChip(
    score: Int,
    onSelected: () -> Unit
) {
    val scoreColour = when {
        score >= 75 -> VolunteerLinkSuccess
        score >= 60 -> VolunteerLinkInformation
        else -> VolunteerLinkWarning
    }

    Surface(
        onClick = onSelected,
        shape = RoundedCornerShape(7.dp),
        color = scoreColour.copy(alpha = 0.12f),
        border = BorderStroke(
            width = 1.dp,
            color = scoreColour.copy(alpha = 0.35f)
        )
    ) {
        Text(
            text = "$score% Match",
            modifier = Modifier.padding(
                horizontal = 7.dp,
                vertical = 4.dp
            ),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = scoreColour,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolunteerMatchDetailsSheet(
    recommendation: VolunteerHomeRecommendation,
    onDismissRequest: () -> Unit,
    onViewOpportunitySelected: () -> Unit,
    onEnableLocationSelected: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = VolunteerLinkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .verticalScroll(
                    rememberScrollState()
                )
                .navigationBarsPadding()
                .padding(
                    start = VolunteerLinkScreenHorizontalPadding,
                    end = VolunteerLinkScreenHorizontalPadding,
                    bottom = 24.dp
                )
        ) {
            Text(
                text = "Your Match Breakdown",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = recommendation.event.eventTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkPrimaryGreen,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text =
                    "Best role: ${recommendation.bestRoleTitle}",
                fontSize = 11.sp,
                color = VolunteerLinkTextSecondary
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape,
                    color = VolunteerLinkSoftGreenSurface,
                    border = BorderStroke(
                        width = 2.dp,
                        color = VolunteerLinkPrimaryGreen
                            .copy(alpha = 0.45f)
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${recommendation.score}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkPrimaryGreen
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = recommendation.matchLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    LinearProgressIndicator(
                        progress = {
                            recommendation.score / 100f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp),
                        color = VolunteerLinkPrimaryGreen,
                        trackColor =
                            VolunteerLinkBorderColour
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = recommendation.reason,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            HorizontalDivider(
                color = VolunteerLinkBorderColour
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                text = "How the score was calculated",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Text(
                text =
                    "Each role is scored separately across six transparent factors. " +
                        "The strongest eligible role becomes this event's match.",
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = VolunteerLinkTextSecondary
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            recommendation.factors.forEach { factor ->
                VolunteerMatchFactorRow(
                    factor = factor
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            if (
                recommendation.event.eventOpportunityType != "Remote" &&
                recommendation.event.eventDistanceKm == null
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1)
                    ),
                    border = BorderStroke(
                        1.dp,
                        VolunteerLinkWarning.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Travel distance is unavailable",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = "Enable approximate location to calculate " +
                                "the distance from your device to this event.",
                            modifier = Modifier.padding(top = 3.dp),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = VolunteerLinkTextSecondary
                        )
                        TextButton(
                            onClick = onEnableLocationSelected
                        ) {
                            Text("Enable location")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        VolunteerLinkSoftGreenSurface
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = VolunteerLinkPrimaryGreen
                        .copy(alpha = 0.22f)
                )
            ) {
                Text(
                    text =
                        "Match scores support your decision; they do not guarantee " +
                            "acceptance. Organisations still review role requirements.",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Button(
                onClick = onViewOpportunitySelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        VolunteerLinkPrimaryGreen,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "View Role Details",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VolunteerMatchFactorRow(
    factor: VolunteerMatchFactor
) {
    val factorColour =
        when (factor.status) {
            VolunteerMatchFactorStatus.STRENGTH ->
                VolunteerLinkSuccess

            VolunteerMatchFactorStatus.OPPORTUNITY ->
                VolunteerLinkInformation

            VolunteerMatchFactorStatus.ATTENTION ->
                VolunteerLinkWarning
        }

    val factorLabel =
        when (factor.status) {
            VolunteerMatchFactorStatus.STRENGTH ->
                "Strength"

            VolunteerMatchFactorStatus.OPPORTUNITY ->
                "Growth"

            VolunteerMatchFactorStatus.ATTENTION ->
                "Check"
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkBackground
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkBorderColour
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(9.dp)
                    .background(
                        color = factorColour,
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text = factor.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextPrimary
                    )

                    Text(
                        text =
                            "${factor.earnedPoints}/${factor.maximumPoints}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = factorColour
                    )
                }

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = factor.explanation,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = VolunteerLinkTextSecondary
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = factorLabel.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = factorColour
                )
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

            VolunteerApplicationStatus.NOT_COMPLETED ->
                "Not Completed"

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

            VolunteerApplicationStatus.NOT_COMPLETED ->
                VolunteerLinkError

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

            VolunteerApplicationStatus.NOT_COMPLETED ->
                Color(0xFFFFEBEE)

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

private fun formatVolunteerImpactMinutes(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        minutes <= 0 -> "0m"
        hours == 0 -> "${remainder}m"
        remainder == 0 -> "${hours}h"
        else -> "${hours}h ${remainder}m"
    }
}

private fun volunteerInitials(fullName: String?): String {
    val trimmed = fullName?.trim().orEmpty()
    if (trimmed.isEmpty()) return "?"

    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "?"
    }
}
