package com.example.volunteerlink.organisation.screens

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements the Organisation UI associated with Organisation Volunteer Posts Screen.
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.organisation.components.OrganisationOfflineStatusCard
import com.example.volunteerlink.organisation.manage.model.ManagePostSection
import com.example.volunteerlink.organisation.manage.model.OrganisationManageUiState
import com.example.volunteerlink.organisation.viewmodel.OrganisationManageViewModel
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding

/** Volunteer Post lifecycle list. Tapping a card opens its Post Management detail. */
@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationVolunteerPostsScreen
 *
 * Renders the Organisation Volunteer Posts screen from state supplied by the owning ViewModel/repository-facing
 * coordinator.
 *
 * The composable maps state to Material3 UI and emits callbacks; it does not become the source of truth for
 * persisted VolunteerLink data.
 */
fun OrganisationVolunteerPostsScreen(
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: OrganisationManageViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // The list ViewModel survives while Post Detail is on top of it. Refresh on
    // later resumes so a post finalized in Detail moves from Needs Review to
    // Completed as soon as the organisation returns to this list.
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasHandledFirstResume by rememberSaveable { mutableStateOf(false) }
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
        else -> OrganisationVolunteerPostsContent(
            uiState = uiState,
            onBack = onBack,
            onPostClick = onPostClick,
            onSyncSelected = viewModel::refresh
        )
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationVolunteerPostsContent
 *
 * Renders the reusable Organisation Volunteer Posts Content portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
private fun OrganisationVolunteerPostsContent(
    uiState: OrganisationManageUiState,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onSyncSelected: () -> Unit
) {
    var selectedSection by rememberSaveable {
        mutableStateOf(ManagePostSection.ACTIVE)
    }

    val visiblePosts = when (selectedSection) {
        ManagePostSection.ACTIVE -> uiState.activePosts
        ManagePostSection.DRAFTS -> uiState.draftPosts
        ManagePostSection.REVIEW -> uiState.reviewPosts
        ManagePostSection.COMPLETED -> uiState.completedPosts
        ManagePostSection.PARTNERSHIPS -> uiState.partnerPosts
    }

    val emptyCopy = when (selectedSection) {
        ManagePostSection.ACTIVE ->
            "No active posts" to "Upcoming and ongoing published posts will appear here."
        ManagePostSection.DRAFTS ->
            "No saved drafts" to "Draft posts you save from Create will appear here."
        ManagePostSection.REVIEW ->
            "Nothing waiting for review" to "Posts move here after the activity ends until close-out work is finished."
        ManagePostSection.COMPLETED ->
            "No completed posts" to "Completed posts remain available here as history."
        ManagePostSection.PARTNERSHIPS ->
            "No partnership posts" to "Posts created from partnerships you accepted will appear here."
    }

    val sectionOverview = when (selectedSection) {
        ManagePostSection.ACTIVE -> Triple(
            "${uiState.activePosts.size} active posts",
            "${uiState.ongoingPosts.size} ongoing · ${uiState.upcomingPosts.size} upcoming",
            false
        )

        ManagePostSection.DRAFTS -> {
            val detail = when {
                uiState.draftAttentionItemCount == 0 -> "No draft alerts right now"
                uiState.draftAttentionPostCount == 1 ->
                    "${uiState.draftAttentionItemCount} ${attentionItemWordForPosts(uiState.draftAttentionItemCount)} across 1 draft"
                else ->
                    "${uiState.draftAttentionItemCount} ${attentionItemWordForPosts(uiState.draftAttentionItemCount)} across ${uiState.draftAttentionPostCount} drafts"
            }
            Triple("${uiState.draftPosts.size} saved drafts", detail, uiState.draftAttentionItemCount > 0)
        }

        ManagePostSection.REVIEW -> Triple(
            "${uiState.reviewPosts.size} awaiting close-out",
            "These activities have ended and still require post-event review.",
            uiState.reviewPosts.isNotEmpty()
        )

        ManagePostSection.COMPLETED -> Triple(
            "${uiState.completedPosts.size} completed posts",
            "Completed posts are kept here as history.",
            false
        )
        ManagePostSection.PARTNERSHIPS -> Triple(
            "${uiState.partnerPosts.size} partnership posts",
            "Read-only activity details and the support your organisation confirmed.",
            false
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
    ) {
        OrganisationManageSubHeader(
            title = "Volunteer Posts",
            onBack = onBack
        )

        if (uiState.isShowingCachedData) {
            OrganisationOfflineStatusCard(
                lastSyncedAtEpochMillis = uiState.lastSyncedAtEpochMillis,
                isSyncing = uiState.isRefreshing,
                onSyncSelected = onSyncSelected,
                modifier = Modifier.padding(
                    horizontal = VolunteerLinkScreenHorizontalPadding,
                    vertical = 10.dp
                )
            )
        }

        ManagePostSectionSelector(
            selected = selectedSection,
            activeCount = uiState.activePosts.size,
            draftCount = uiState.draftPosts.size,
            reviewCount = uiState.reviewPosts.size,
            completedCount = uiState.completedPosts.size,
            partnerCount = uiState.partnerPosts.size,
            activeHasAttention = uiState.activeAttentionPostCount > 0,
            draftsHaveAttention = uiState.draftAttentionPostCount > 0,
            reviewHasAttention = uiState.reviewPosts.isNotEmpty(),
            onSelected = { selectedSection = it },
            modifier = Modifier.padding(
                horizontal = VolunteerLinkScreenHorizontalPadding,
                vertical = 12.dp
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VolunteerLinkScreenHorizontalPadding,
                end = VolunteerLinkScreenHorizontalPadding,
                top = 2.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item(key = "overview_${selectedSection.name}") {
                ManageSectionOverview(
                    title = sectionOverview.first,
                    detail = sectionOverview.second,
                    hasAttention = sectionOverview.third,
                    modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
                )
            }

            if (visiblePosts.isEmpty()) {
                item(key = "empty_${selectedSection.name}") {
                    ManageEmptySectionMessage(
                        title = emptyCopy.first,
                        message = emptyCopy.second
                    )
                }
            } else if (selectedSection == ManagePostSection.ACTIVE) {
                if (uiState.ongoingPosts.isNotEmpty()) {
                    item(key = "active_ongoing_header") {
                        ManageActiveGroupHeader(
                            title = "Ongoing",
                            count = uiState.ongoingPosts.size,
                            subtitle = "Happening now · ending soonest first",
                            hasAttention = uiState.ongoingPosts.any { it.attentionItems.isNotEmpty() },
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                        )
                    }
                    items(
                        count = uiState.ongoingPosts.size,
                        key = { index -> "ongoing_${uiState.ongoingPosts[index].postId}" }
                    ) { index ->
                        ManageVolunteerPostCard(
                            post = uiState.ongoingPosts[index],
                            section = ManagePostSection.ACTIVE,
                            onClick = { onPostClick(uiState.ongoingPosts[index].postId) }
                        )
                    }
                }

                if (uiState.upcomingPosts.isNotEmpty()) {
                    item(key = "active_upcoming_header") {
                        ManageActiveGroupHeader(
                            title = "Upcoming",
                            count = uiState.upcomingPosts.size,
                            subtitle = "Starting soonest first",
                            hasAttention = uiState.upcomingPosts.any { it.attentionItems.isNotEmpty() },
                            modifier = Modifier.padding(top = if (uiState.ongoingPosts.isNotEmpty()) 22.dp else 12.dp, bottom = 6.dp)
                        )
                    }
                    items(
                        count = uiState.upcomingPosts.size,
                        key = { index -> "upcoming_${uiState.upcomingPosts[index].postId}" }
                    ) { index ->
                        ManageVolunteerPostCard(
                            post = uiState.upcomingPosts[index],
                            section = ManagePostSection.ACTIVE,
                            onClick = { onPostClick(uiState.upcomingPosts[index].postId) }
                        )
                    }
                }
            } else {
                items(
                    count = visiblePosts.size,
                    key = { index -> visiblePosts[index].postId }
                ) { index ->
                    ManageVolunteerPostCard(
                        post = visiblePosts[index],
                        section = selectedSection,
                        onClick = {
                            if (!visiblePosts[index].isPartnerPost) {
                                onPostClick(visiblePosts[index].postId)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * DETAILED BEHAVIOUR — attentionItemWordForPosts
 *
 * Renders the reusable attention Item Word For Posts portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
private fun attentionItemWordForPosts(count: Int): String =
    if (count == 1) "alert" else "alerts"
