package com.example.volunteerlink.organisation.home.model

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines immutable Home dashboard data/state associated with Organisation Home Ui State.
//
// The repository/ViewModel fills these models from authenticated Supabase reads and derived timing/application
// information.
//
// Compose consumes these models directly, which keeps raw JSON/table rows out of the Home screen and makes
// loading/error/cached states explicit.
//
// Architectural layer: Domain/UI model layer.
// ============================================================================


import com.example.volunteerlink.data.post.PostTimingState

/**
 * Ready-to-display state for Organisation Home.
 *
 * Home will only render this state. Supabase querying and date calculations stay
 * outside the Composable so the screen remains easy to read and maintain.
 */
/**
 * DETAILED DECLARATION — OrganisationHomeUiState
 *
 * Immutable snapshot of all UI-visible state required by Organisation Home Ui State.
 *
 * Keeping loading/data/error/action flags together makes recomposition deterministic and avoids hidden mutable
 * state in individual composables.
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

/**
 * Holds the values represented by home post item as one strongly typed model.
 * It keeps related Home dashboard values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — HomePostItem
 *
 * Domain/UI type for Home Post Item used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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

/**
 * Lists the supported values represented by home attention severity.
 * It keeps related Home dashboard values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — HomeAttentionSeverity
 *
 * Domain/UI type for Home Attention Severity used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
enum class HomeAttentionSeverity {
    URGENT,
    WARNING,
    NEEDS_REVIEW,
    REVIEW
}

/**
 * Lists the supported values represented by home attention type.
 * It keeps related Home dashboard values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — HomeAttentionType
 *
 * Domain/UI type for Home Attention Type used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
enum class HomeAttentionType {
    APPLICATIONS_TO_REVIEW,
    POST_COMPLETION_REVIEW,
    DRAFT_START_TOO_SOON,
    DRAFT_START_DATE_PASSED,
    IMPACT_WEAVE_READY,
    IMPACT_WEAVE_DEADLINE_SOON,
    IMPACT_WEAVE_DEADLINE_PASSED,
    IMPACT_WEAVE_ACTIVITY_PASSED,
    IMPACT_WEAVE_PROGRESS
}

/**
 * Holds the values represented by home attention item as one strongly typed model.
 * It keeps related Home dashboard values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — HomeAttentionItem
 *
 * Domain/UI type for Home Attention Item used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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
