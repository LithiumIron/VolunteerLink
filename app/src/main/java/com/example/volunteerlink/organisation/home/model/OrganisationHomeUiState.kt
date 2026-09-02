package com.example.volunteerlink.organisation.home.model

import com.example.volunteerlink.data.post.PostTimingState

/**
 * Ready-to-display state for Organisation Home.
 *
 * Home will only render this state. Supabase querying and date calculations stay
 * outside the Composable so the screen remains easy to read and maintain.
 */
data class OrganisationHomeUiState(
    val isLoading: Boolean = false,
    val organisationName: String = "",
    val ongoingCount: Int = 0,
    val upcomingCount: Int = 0,
    val draftCount: Int = 0,
    val attentionItems: List<HomeAttentionItem> = emptyList(),
    val ongoingPosts: List<HomePostItem> = emptyList(),
    val upcomingPosts: List<HomePostItem> = emptyList(),
    val isShowingCachedData: Boolean = false,
    val lastSyncedAtEpochMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

data class HomePostItem(
    val postId: String,
    val title: String,
    val mode: String,
    val databaseStatus: String,
    val timingState: PostTimingState,
    // Earliest/latest overall dates remain useful for sorting and non-Hybrid rows.
    val startDate: String?,
    val endDate: String?,
    val locationName: String? = null,
    // Hybrid needs both timelines so Home does not mix a Remote date range with a
    // Physical venue and make the card ambiguous.
    val physicalStartDate: String? = null,
    val physicalEndDate: String? = null,
    val physicalLocationName: String? = null,
    val physicalTimingState: PostTimingState? = null,
    val remoteStartDate: String? = null,
    val remoteEndDate: String? = null,
    val remoteTimingState: PostTimingState? = null
)

enum class HomeAttentionSeverity {
    URGENT,
    WARNING,
    NEEDS_REVIEW,
    REVIEW
}

enum class HomeAttentionType {
    APPLICATIONS_TO_REVIEW,
    POST_COMPLETION_REVIEW,
    DRAFT_START_TOO_SOON,
    DRAFT_START_DATE_PASSED,
}

data class HomeAttentionItem(
    val type: HomeAttentionType,
    val severity: HomeAttentionSeverity,
    val postId: String,
    val postTitle: String,
    val message: String,
    val contextLabel: String? = null,
    val scheduleItemId: String? = null,
    val scheduleTitle: String? = null,
    val scheduleDate: String? = null,
    val daysRemaining: Int? = null
)
