package com.example.volunteerlink.organisation.manage.model

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines immutable Organisation Manage/Post Management state associated with Organisation Manage Ui State.
//
// The models combine normalized backend data into one screen-oriented representation without changing the
// underlying database structure.
//
// Flags such as loading, cached/offline, timing state and active review action make it explicit when the UI may
// render information versus when it may perform a server mutation.
//
// Architectural layer: Domain/UI model layer.
// ============================================================================


import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.organisation.home.model.OrganisationImpactWeaveAttention

/** Top-level state shared by the Manage landing page and Volunteer Posts list. */
/**
 * DETAILED DECLARATION — OrganisationManageUiState
 *
 * Immutable snapshot of all UI-visible state required by Organisation Manage Ui State.
 *
 * Keeping loading/data/error/action flags together makes recomposition deterministic and avoids hidden mutable
 * state in individual composables.
 */
data class OrganisationManageUiState(
    val isLoading: Boolean = true,
    val organisationName: String = "",
    val activePosts: List<ManagePostItem> = emptyList(),
    val draftPosts: List<ManagePostItem> = emptyList(),
    val reviewPosts: List<ManagePostItem> = emptyList(),
    val completedPosts: List<ManagePostItem> = emptyList(),
    val partnerPosts: List<ManagePostItem> = emptyList(),
    val impactWeaveAttention: List<OrganisationImpactWeaveAttention> = emptyList(),
    val isShowingCachedData: Boolean = false,
    val lastSyncedAtEpochMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Counts live draft/application attention items. Ended posts that
     * need close-out are tracked separately by the Needs Review lifecycle.
     */
    val attentionItemCount: Int
        get() = actionablePosts.sumOf { it.attentionItems.size }

    val attentionPostCount: Int
        get() = actionablePosts.count { it.attentionItems.isNotEmpty() }

    val activeAttentionItemCount: Int
        get() = activePosts.sumOf { it.attentionItems.size }

    val activeAttentionPostCount: Int
        get() = activePosts.count { it.attentionItems.isNotEmpty() }

    val draftAttentionItemCount: Int
        get() = draftPosts.sumOf { it.attentionItems.size }

    val draftAttentionPostCount: Int
        get() = draftPosts.count { it.attentionItems.isNotEmpty() }

    val ongoingPosts: List<ManagePostItem>
        get() = activePosts.filter { it.timingState == PostTimingState.ONGOING }

    val upcomingPosts: List<ManagePostItem>
        get() = activePosts.filter { it.timingState == PostTimingState.UPCOMING }

    val reviewAttentionPostCount: Int
        get() = reviewPosts.size

    val impactWeaveAttentionCount: Int
        get() = impactWeaveAttention.size

    /**
     * Manage treats unfinished post-event close-out as attention too. It is
     * deliberately labelled Needs Review rather than ordinary Review.
     */
    val managementAttentionPostCount: Int
        get() = attentionPostCount + reviewAttentionPostCount

    private val actionablePosts: List<ManagePostItem>
        get() = activePosts + draftPosts
}

/**
 * Lists the supported values represented by manage post section.
 * It keeps related Manage Post values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — ManagePostSection
 *
 * Domain/UI type for Manage Post Section used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
enum class ManagePostSection {
    ACTIVE,
    DRAFTS,
    REVIEW,
    COMPLETED,
    PARTNERSHIPS
}

/**
 * Holds the values represented by manage post item as one strongly typed model.
 * It keeps related Manage Post values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — ManagePostItem
 *
 * Domain/UI type for Manage Post Item used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class ManagePostItem(
    val postId: String,
    val title: String,
    val description: String = "",
    val mode: String,
    val databaseStatus: String,
    val timingState: PostTimingState? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val locationName: String? = null,
    val physicalStartDate: String? = null,
    val physicalEndDate: String? = null,
    val physicalLocationName: String? = null,
    val physicalTimingState: PostTimingState? = null,
    val remoteStartDate: String? = null,
    val remoteEndDate: String? = null,
    val remoteTimingState: PostTimingState? = null,
    val attentionItems: List<ManageAttentionItem> = emptyList(),
    val isPartnerPost: Boolean = false,
    val ownerOrganisationName: String? = null,
    val contributionSummary: String? = null
)

/**
 * Lists the supported values represented by manage attention severity.
 * It keeps related Manage Post values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — ManageAttentionSeverity
 *
 * Domain/UI type for Manage Attention Severity used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
enum class ManageAttentionSeverity {
    URGENT,
    WARNING,
    NEEDS_REVIEW,
    REVIEW
}

/**
 * Lists the supported values represented by manage attention type.
 * It keeps related Manage Post values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — ManageAttentionType
 *
 * Domain/UI type for Manage Attention Type used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
enum class ManageAttentionType {
    APPLICATIONS_TO_REVIEW,
    DRAFT_START_TOO_SOON,
    DRAFT_START_DATE_PASSED,
    COMPLETION_REVIEW
}

/**
 * Holds the values represented by manage attention item as one strongly typed model.
 * It keeps related Manage Post values together so callers do not pass disconnected fields around.
 */
/**
 * DETAILED DECLARATION — ManageAttentionItem
 *
 * Domain/UI type for Manage Attention Item used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class ManageAttentionItem(
    val type: ManageAttentionType,
    val severity: ManageAttentionSeverity,
    val kindLabel: String,
    val title: String,
    val message: String
)
