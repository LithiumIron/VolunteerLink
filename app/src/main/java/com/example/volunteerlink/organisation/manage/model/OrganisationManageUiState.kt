package com.example.volunteerlink.organisation.manage.model

import com.example.volunteerlink.data.post.PostTimingState

/** Top-level state shared by the Manage landing page and Volunteer Posts list. */
data class OrganisationManageUiState(
    val isLoading: Boolean = true,
    val organisationName: String = "",
    val activePosts: List<ManagePostItem> = emptyList(),
    val draftPosts: List<ManagePostItem> = emptyList(),
    val reviewPosts: List<ManagePostItem> = emptyList(),
    val completedPosts: List<ManagePostItem> = emptyList(),
    val errorMessage: String? = null
) {
    /**
     * Counts live draft/training/application attention items. Ended posts that
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

    /**
     * Manage treats unfinished post-event close-out as attention too. It is
     * deliberately labelled Needs Review rather than ordinary Review.
     */
    val managementAttentionPostCount: Int
        get() = attentionPostCount + reviewAttentionPostCount

    private val actionablePosts: List<ManagePostItem>
        get() = activePosts + draftPosts
}

enum class ManagePostSection {
    ACTIVE,
    DRAFTS,
    REVIEW,
    COMPLETED
}

data class ManagePostItem(
    val postId: String,
    val title: String,
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
    val attentionItems: List<ManageAttentionItem> = emptyList()
)

enum class ManageAttentionSeverity {
    URGENT,
    WARNING,
    NEEDS_REVIEW,
    REVIEW
}

enum class ManageAttentionType {
    APPLICATIONS_TO_REVIEW,
    DRAFT_START_TOO_SOON,
    DRAFT_START_DATE_PASSED,
    TRAINING_DETAILS_WARNING,
    TRAINING_DETAILS_URGENT,
    TRAINING_OUTDATED,
    COMPLETION_REVIEW
}

data class ManageAttentionItem(
    val type: ManageAttentionType,
    val severity: ManageAttentionSeverity,
    val kindLabel: String,
    val title: String,
    val message: String
)
