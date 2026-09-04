package com.example.volunteerlink.organisation.screens

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Renders the Organisation dashboard using the state produced by OrganisationHomeViewModel.
//
// The page combines identity, post/activity summaries and attention items into presentation sections while
// callbacks handle navigation to deeper workflows.
//
// A cached snapshot can be shown as read-only continuity when available; server mutations are never performed
// directly from this composable.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.components.OrganisationDivider
import com.example.volunteerlink.organisation.components.OrganisationOfflineStatusCard
import com.example.volunteerlink.organisation.components.OrganisationSectionSurface
import com.example.volunteerlink.organisation.home.model.HomeAttentionItem
import com.example.volunteerlink.organisation.home.model.OrganisationHomeUiState
import com.example.volunteerlink.organisation.viewmodel.OrganisationHomeViewModel
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary

/**
 * Organisation dashboard.
 *
 * The screen only renders OrganisationHomeUiState. Supabase reads and all
 * date-dependent rules stay in the repository/ViewModel/evaluator layer.
 */
@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationHomeScreen
 *
 * Renders the Organisation Home screen from state supplied by the owning ViewModel/repository-facing
 * coordinator.
 *
 * The composable maps state to Material3 UI and emits callbacks; it does not become the source of truth for
 * persisted VolunteerLink data.
 */
fun OrganisationHomeScreen(
    onViewAllPosts: () -> Unit,
    onPostClick: (String) -> Unit,
    onAttentionClick: (HomeAttentionItem) -> Unit,
    onOrganisationProfileSelected: () -> Unit = {},   // NEW
    viewModel: OrganisationHomeViewModel = viewModel()
) {
    // Collect StateFlow with lifecycle awareness so the dashboard redraws only while the screen is active.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    var hasHandledFirstResume by rememberSaveable { mutableStateOf(false) }
    // Refresh after returning to Home so counts/alerts reflect actions completed on other screens.
    // The first ON_RESUME is ignored because the ViewModel already performs its initial load.
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

    OrganisationHomeContent(
        uiState = uiState,
        onViewAllPosts = onViewAllPosts,
        onPostClick = onPostClick,
        onAttentionClick = onAttentionClick,
        onRetry = viewModel::refresh,
        onOrganisationProfileSelected = onOrganisationProfileSelected   // NEW
    )
}

@Composable
/**
 * Renders the organisation home content content block used in the organisation Home dashboard flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — OrganisationHomeContent
 *
 * Renders the reusable Organisation Home Content portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 *
 * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
 * RLS/RPC ownership checks still make the final authorization decision.
 *
 * Uses AppClock for business-date/time decisions so the same code works with the project test clock and normal
 * device time.
 */
private fun OrganisationHomeContent(
    uiState: OrganisationHomeUiState,
    onViewAllPosts: () -> Unit,
    onPostClick: (String) -> Unit,
    onAttentionClick: (HomeAttentionItem) -> Unit,
    onRetry: () -> Unit,
    onOrganisationProfileSelected: () -> Unit   // NEW
) {
    // A single UI state drives the three screen modes: loading, error, or dashboard content.
    when {
        uiState.isLoading -> {
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
                    text = "Loading your organisation...",
                    modifier = Modifier.padding(top = 12.dp),
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }

        uiState.errorMessage != null -> {
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
                    text = "Couldn't load Home",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = uiState.errorMessage,
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

        else -> {
            // LazyColumn keeps the dashboard efficient while allowing independent sections to appear only when needed.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VolunteerLinkBackground)
                    .statusBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item(key = "home_header") {
                    OrganisationHomeHeader(
                        organisationName = uiState.organisationName,
                        nowMillis = AppClock.nowMillis(),
                        // The Home snapshot belongs to the organisation resolved for
                        // this exact request. Do not read the shared profile cache here:
                        // it can still contain the previous account during a login switch.
                        profileImageUrl = uiState.profileImageUrl,
                        onAvatarSelected = onOrganisationProfileSelected
                    )
                }

                if (uiState.isShowingCachedData) {
                    item(key = "home_offline_status") {
                        OrganisationOfflineStatusCard(
                            lastSyncedAtEpochMillis = uiState.lastSyncedAtEpochMillis,
                            isSyncing = uiState.isRefreshing,
                            onSyncSelected = onRetry,
                            modifier = Modifier.padding(
                                horizontal = VolunteerLinkScreenHorizontalPadding
                            )
                        )
                    }
                }

                if (uiState.attentionItems.isNotEmpty()) {
                    item(key = "home_attention") {
                        OrganisationAttentionSection(
                            items = uiState.attentionItems,
                            onItemClick = onAttentionClick,
                            modifier = Modifier.padding(horizontal = VolunteerLinkScreenHorizontalPadding)
                        )
                    }
                }

                item(key = "home_post_summary") {
                    OrganisationPostSummarySection(
                        ongoingCount = uiState.ongoingCount,
                        upcomingCount = uiState.upcomingCount,
                        draftCount = uiState.draftCount,
                        onViewAllPosts = onViewAllPosts,
                        modifier = Modifier.padding(horizontal = VolunteerLinkScreenHorizontalPadding)
                    )
                }

                item(key = "happening_now") {
                    Column(
                        modifier = Modifier.padding(horizontal = VolunteerLinkScreenHorizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        HomeSectionHeading(
                            title = "Happening Now",
                            actionLabel = if (uiState.ongoingPosts.size > 2) "View all" else null,
                            onAction = onViewAllPosts
                        )

                        if (uiState.ongoingPosts.isEmpty()) {
                            HomeEmptyMessage(
                                text = "No volunteering activities are currently ongoing."
                            )
                        } else {
                            val visibleOngoing = uiState.ongoingPosts.take(2)
                            OrganisationSectionSurface(contentPadding = 14.dp) {
                                visibleOngoing.forEachIndexed { index, post ->
                                    OngoingPostCard(
                                        post = post,
                                        onClick = { onPostClick(post.postId) }
                                    )
                                    if (index != visibleOngoing.lastIndex) {
                                        OrganisationDivider()
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "coming_up") {
                    Column(
                        modifier = Modifier.padding(horizontal = VolunteerLinkScreenHorizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        HomeSectionHeading(
                            title = "Coming Up",
                            actionLabel = if (uiState.upcomingPosts.size > 2) {
                                "View all"
                            } else {
                                null
                            },
                            onAction = onViewAllPosts
                        )

                        if (uiState.upcomingPosts.isEmpty()) {
                            HomeEmptyMessage(
                                text = "No upcoming published posts yet."
                            )
                        } else {
                            val visibleUpcoming = uiState.upcomingPosts.take(2)
                            OrganisationSectionSurface(contentPadding = 14.dp) {
                                visibleUpcoming.forEachIndexed { index, post ->
                                    UpcomingPostRow(
                                        post = post,
                                        showDivider = index != visibleUpcoming.lastIndex,
                                        onClick = { onPostClick(post.postId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
