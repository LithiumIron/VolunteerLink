package com.example.volunteerlink.organisation.screens

// FILE OVERVIEW:
/*
 * OrganisationManageScreen contains presentation code for the organisation Manage Post flow.
 * It focuses on rendering state and forwarding user actions through callbacks/ViewModels,
 * keeping database access and business rules outside the composables where possible.
 */


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.components.OrganisationOfflineStatusCard
import com.example.volunteerlink.organisation.components.OrganisationSectionHeader
import com.example.volunteerlink.organisation.manage.model.OrganisationManageUiState
import com.example.volunteerlink.organisation.viewmodel.OrganisationManageViewModel
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary

/** Manage starts with the three distinct areas from the original prototype. */
@Composable
fun OrganisationManageScreen(
    onVolunteerPostsClick: () -> Unit,
    onImpactWeaveClick: () -> Unit,
    onPromotionsClick: () -> Unit,
    viewModel: OrganisationManageViewModel = viewModel()
) {
    // Observe management counts, attention indicators and cached-state information from the ViewModel.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    var hasHandledFirstResume by rememberSaveable { mutableStateOf(false) }
    // Reload Manage after returning from Post Management, Impact Weave, reviews or promotions.
    // This prevents stale counts after the organisation performs an action on a child screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasHandledFirstResume) {
                    viewModel.refresh()
                } else {
                    hasHandledFirstResume = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        uiState.isLoading -> ManageLoadingState()
        uiState.errorMessage != null -> ManageErrorState(
            message = uiState.errorMessage,
            onRetry = viewModel::refresh
        )
        else -> ManageLandingContent(
            uiState = uiState,
            onVolunteerPostsClick = onVolunteerPostsClick,
            onImpactWeaveClick = onImpactWeaveClick,
            onPromotionsClick = onPromotionsClick,
            onSyncSelected = viewModel::refresh
        )
    }
}

@Composable
/**
 * Renders the manage landing content content block used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ManageLandingContent(
    uiState: OrganisationManageUiState,
    onVolunteerPostsClick: () -> Unit,
    onImpactWeaveClick: () -> Unit,
    onPromotionsClick: () -> Unit,
    onSyncSelected: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item(key = "manage_header") {
            OrganisationManageHeader(
                title = "Manage",
                subtitle = "Everything your organisation is running"
            )
        }

        if (uiState.isShowingCachedData) {
            item(key = "manage_offline_status") {
                OrganisationOfflineStatusCard(
                    lastSyncedAtEpochMillis = uiState.lastSyncedAtEpochMillis,
                    isSyncing = uiState.isRefreshing,
                    onSyncSelected = onSyncSelected,
                    modifier = Modifier.padding(
                        start = VolunteerLinkScreenHorizontalPadding,
                        end = VolunteerLinkScreenHorizontalPadding,
                        top = 12.dp
                    )
                )
            }
        }

        item(key = "manage_volunteering_heading") {
            OrganisationSectionHeader(
                title = "Volunteering",
                subtitle = "Posts, participants and review work",
                modifier = Modifier.padding(
                    start = VolunteerLinkScreenHorizontalPadding,
                    end = VolunteerLinkScreenHorizontalPadding,
                    top = 24.dp,
                    bottom = 2.dp
                )
            )
        }

        item(key = "manage_posts") {
            ManageModuleChoiceCard(
                iconRes = R.drawable.manage,
                title = "Volunteer Posts",
                description = "Manage posts before, during and after volunteering.",
                summary = buildPostSummary(uiState),
                attentionText = buildManageAttentionSummary(uiState),
                hasAttention = uiState.managementAttentionPostCount > 0,
                onClick = onVolunteerPostsClick,
                modifier = Modifier.padding(horizontal = VolunteerLinkScreenHorizontalPadding)
            )
        }

        item(key = "manage_partnership_heading") {
            OrganisationSectionHeader(
                title = "Partnerships",
                subtitle = "Organisation collaboration and shared support",
                modifier = Modifier.padding(
                    start = VolunteerLinkScreenHorizontalPadding,
                    end = VolunteerLinkScreenHorizontalPadding,
                    top = 24.dp,
                    bottom = 2.dp
                )
            )
        }

        item(key = "manage_impact_weave") {
            ManageModuleChoiceCard(
                iconRes = R.drawable.group,
                title = "Impact Weave",
                description = "Partnership planning, confirmed support and collaboration history.",
                summary = if (uiState.impactWeaveAttentionCount > 0) {
                    "${uiState.impactWeaveAttentionCount} Impact Weave ${if (uiState.impactWeaveAttentionCount == 1) "item needs" else "items need"} attention"
                } else {
                    null
                },
                attentionText = buildImpactWeaveAttentionSummary(uiState),
                hasAttention = uiState.impactWeaveAttentionCount > 0,
                onClick = onImpactWeaveClick,
                modifier = Modifier.padding(horizontal = VolunteerLinkScreenHorizontalPadding)
            )
        }

        item(key = "manage_visibility_heading") {
            OrganisationSectionHeader(
                title = "Visibility",
                subtitle = "Promote opportunities that need more reach",
                modifier = Modifier.padding(
                    start = VolunteerLinkScreenHorizontalPadding,
                    end = VolunteerLinkScreenHorizontalPadding,
                    top = 24.dp,
                    bottom = 2.dp
                )
            )
        }

        item(key = "manage_promotions") {
            ManageModuleChoiceCard(
                iconRes = R.drawable.ongoing_posts,
                title = "Promotions",
                description = "Manage featured post promotions.",
                onClick = onPromotionsClick,
                modifier = Modifier.padding(horizontal = VolunteerLinkScreenHorizontalPadding)
            )
        }
    }
}

/**
 * Renders the build post summary summary block used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun buildPostSummary(uiState: OrganisationManageUiState): String {
    return buildList {
        add("${uiState.activePosts.size} active")
        add("${uiState.draftPosts.size} drafts")
        if (uiState.reviewPosts.isNotEmpty()) {
            add("${uiState.reviewPosts.size} need close-out")
        }
        if (uiState.completedPosts.isNotEmpty()) {
            add("${uiState.completedPosts.size} completed")
        }
    }.joinToString(" · ")
}

/**
 * Renders the build manage attention summary summary block used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun buildManageAttentionSummary(uiState: OrganisationManageUiState): String {
    val alertPosts = uiState.attentionPostCount
    val reviewPosts = uiState.reviewAttentionPostCount

    return when {
        alertPosts == 0 && reviewPosts == 0 -> "No posts need attention right now"
        alertPosts > 0 && reviewPosts > 0 ->
            "$alertPosts ${postWord(alertPosts)} have alerts · $reviewPosts await close-out"
        alertPosts > 0 -> "$alertPosts ${postWord(alertPosts)} have alerts"
        else -> "$reviewPosts ${postWord(reviewPosts)} await close-out"
    }
}

/**
 * Derives the post word value used by the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun postWord(count: Int): String = if (count == 1) "post" else "posts"

/**
 * Renders the build impact weave attention summary summary block used in the organisation Manage Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun buildImpactWeaveAttentionSummary(
    uiState: OrganisationManageUiState
): String? {
    val first = uiState.impactWeaveAttention.firstOrNull() ?: return null
    val prefix = "Impact Weave · ${first.title}"
    return if (uiState.impactWeaveAttention.size > 1) {
        "$prefix — ${first.message}  +${uiState.impactWeaveAttention.size - 1} more"
    } else {
        "$prefix — ${first.message}"
    }
}

@Composable
/**
 * Renders the UI represented by manage loading state for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun ManageLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = VolunteerLinkPrimaryGreen)
        Text(
            text = "Loading management data...",
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary
        )
    }
}

@Composable
/**
 * Renders the UI represented by manage error state for the organisation Manage Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun ManageErrorState(
    message: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Couldn't load Manage",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )
        Text(
            text = message ?: "Unable to load organisation posts.",
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Try Again")
        }
    }
}

/** Temporary destination shells so the three Manage choices navigate cleanly. */
@Composable
fun OrganisationManageEmptyModuleScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
    ) {
        OrganisationManageSubHeader(
            title = title,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
