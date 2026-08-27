package com.example.volunteerlink.organisation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.post.DraftAttentionType
import com.example.volunteerlink.data.post.PostMode
import com.example.volunteerlink.data.post.PostTimingEvaluator
import com.example.volunteerlink.data.post.PostTimingInput
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.data.post.RoleApplicationWindowEvaluator
import com.example.volunteerlink.data.post.RoleApplicationWindowInput
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.home.model.HomeAttentionItem
import com.example.volunteerlink.organisation.home.model.HomeAttentionSeverity
import com.example.volunteerlink.organisation.home.model.HomeAttentionType
import com.example.volunteerlink.organisation.home.model.HomePostItem
import com.example.volunteerlink.organisation.home.model.OrganisationHomePost
import com.example.volunteerlink.organisation.home.model.OrganisationHomeSnapshot
import com.example.volunteerlink.organisation.home.model.OrganisationHomeUiState
import com.example.volunteerlink.organisation.repository.OrganisationHomeRepository
import com.example.volunteerlink.organisation.repository.SupabaseOrganisationHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Prepares everything Organisation Home needs to display.
 *
 * The repository owns Supabase reads. PostTimingEvaluator owns time rules. This
 * ViewModel combines both into one read-only StateFlow for the future Home UI.
 */
class OrganisationHomeViewModel : ViewModel() {

    private val homeRepository: OrganisationHomeRepository =
        SupabaseOrganisationHomeRepository()

    private val _uiState = MutableStateFlow(
        OrganisationHomeUiState(isLoading = true)
    )
    val uiState = _uiState.asStateFlow()

    private var cachedSnapshot: OrganisationHomeSnapshot? = null

    init {
        refresh()
        observeAppClock()
    }

    /** Reloads organisation/post data from Supabase. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val snapshot = homeRepository.loadHomeSnapshot(
                    organisationId = TEST_ORGANISATION_ID
                )
                cachedSnapshot = snapshot
                applySnapshot(snapshot)
            } catch (exception: Exception) {
                Log.e(TAG, "Could not load Organisation Home data.", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message
                        ?: "Unable to load organisation home data."
                )
            }
        }
    }

    /**
     * AppClock refreshes every few seconds during the current test/demo setup.
     * Recalculate the cached post states immediately when its value changes;
     * there is no reason to query Supabase posts again just because time changed.
     */
    private fun observeAppClock() {
        viewModelScope.launch {
            AppClock.state.collect { clockState ->
                if (!clockState.isLoaded) return@collect
                cachedSnapshot?.let(::applySnapshot)
            }
        }
    }

    private fun applySnapshot(snapshot: OrganisationHomeSnapshot) {
        val nowMillis = AppClock.nowMillis()

        val ongoingPosts = mutableListOf<HomePostItem>()
        val upcomingPosts = mutableListOf<HomePostItem>()
        val attentionItems = mutableListOf<HomeAttentionItem>()
        var draftCount = 0

        snapshot.posts.forEach { post ->
            val timingInput = post.toTimingInput()
            val normalizedStatus = post.status.uppercase()

            // Only published/closed posts have a derived activity state on Home.
            // CLOSED means applications are closed, not that volunteering ended.
            val timingState = if (
                normalizedStatus in setOf("PUBLISHED", "CLOSED") &&
                timingInput != null
            ) {
                PostTimingEvaluator.evaluatePostTiming(
                    input = timingInput,
                    nowMillis = nowMillis
                )
            } else {
                null
            }

            when (normalizedStatus) {
                "DRAFT" -> {
                    draftCount++
                    if (timingInput != null) {
                        buildDraftAttention(
                            post = post,
                            input = timingInput,
                            nowMillis = nowMillis
                        )?.let(attentionItems::add)
                    }
                }

                "PUBLISHED",
                "CLOSED" -> when (timingState) {
                    PostTimingState.ONGOING -> ongoingPosts +=
                        post.toHomePostItem(PostTimingState.ONGOING, nowMillis)

                    PostTimingState.UPCOMING -> upcomingPosts +=
                        post.toHomePostItem(PostTimingState.UPCOMING, nowMillis)

                    PostTimingState.PAST,
                    null -> Unit
                }
            }

            // A published/closed activity that has already ended still needs
            // organisation close-out work. Keep it visible on Home as Review
            // until the post is explicitly marked COMPLETED.
            if (
                normalizedStatus in setOf("PUBLISHED", "CLOSED") &&
                timingState == PostTimingState.PAST
            ) {
                attentionItems += buildCompletionReviewAttention(post)
            }

            // Pending rows only need organisation action when the role actually
            // uses REVIEW_APPLICANTS. Instant Join participations are ignored.
            // Once the whole opportunity is Past, Home stops nagging about old
            // pending applications; historical cleanup belongs in Manage later.
            if (
                normalizedStatus == "PUBLISHED" &&
                timingState != null &&
                timingState != PostTimingState.PAST
            ) {
                buildApplicationReviewAttention(
                    post = post,
                    nowMillis = nowMillis
                )?.let(attentionItems::add)
            }

        }

        val sortedOngoing = ongoingPosts.sortedWith(
            compareBy<HomePostItem> { it.endDate.orEmpty() }
                .thenBy { it.title }
        )
        val sortedUpcoming = upcomingPosts.sortedWith(
            compareBy<HomePostItem> { it.startDate.orEmpty() }
                .thenBy { it.title }
        )
        // One priority order is used everywhere:
        // Urgent -> Warning -> Needs Review -> Review.
        // "Needs Review" means an ended activity still needs close-out;
        // "Review" means a live human action such as an application review.
        val sortedAttention = attentionItems.sortedWith(
            compareBy<HomeAttentionItem> {
                when (it.severity) {
                    HomeAttentionSeverity.URGENT -> 0
                    HomeAttentionSeverity.WARNING -> 1
                    HomeAttentionSeverity.NEEDS_REVIEW -> 2
                    HomeAttentionSeverity.REVIEW -> 3
                }
            }.thenBy { it.daysRemaining ?: Int.MAX_VALUE }
                .thenBy {
                    when (it.type) {
                        HomeAttentionType.APPLICATIONS_TO_REVIEW -> 0
                        HomeAttentionType.POST_COMPLETION_REVIEW -> 1
                        else -> 0
                    }
                }
                .thenBy { it.postTitle }
        )

        _uiState.value = OrganisationHomeUiState(
            isLoading = false,
            organisationName = snapshot.organisationName,
            ongoingCount = sortedOngoing.size,
            upcomingCount = sortedUpcoming.size,
            draftCount = draftCount,
            attentionItems = sortedAttention,
            ongoingPosts = sortedOngoing,
            upcomingPosts = sortedUpcoming,
            errorMessage = null
        )

        val reviewAttentionCount = sortedAttention.count {
            it.type == HomeAttentionType.APPLICATIONS_TO_REVIEW
        }

        Log.d(
            TAG,
            "Home data: ongoing=${sortedOngoing.size}, " +
                    "upcoming=${sortedUpcoming.size}, drafts=$draftCount, " +
                    "attention=${sortedAttention.size}, " +
                    "applicationReview=$reviewAttentionCount"
        )
    }

    private fun buildCompletionReviewAttention(
        post: OrganisationHomePost
    ): HomeAttentionItem {
        return HomeAttentionItem(
            type = HomeAttentionType.POST_COMPLETION_REVIEW,
            severity = HomeAttentionSeverity.NEEDS_REVIEW,
            postId = post.postId,
            postTitle = post.title,
            contextLabel = "Post-event close-out",
            message = "The activity has ended. Finish attendance and volunteer review before marking this post completed."
        )
    }

    private fun buildApplicationReviewAttention(
        post: OrganisationHomePost,
        nowMillis: Long
    ): HomeAttentionItem? {
        // Review attention is role-specific, especially for Hybrid posts.
        // A Physical role follows the Physical start while a Remote role follows
        // the Remote start. Once that role's volunteering phase starts, new
        // applications are closed for that role.
        val reviewableRoles = post.roles.filter { role ->
            role.applicationMethod.equals(
                "REVIEW_APPLICANTS",
                ignoreCase = true
            ) && role.isApplicationReviewStillOpen(
                post = post,
                nowMillis = nowMillis
            )
        }

        val pendingByRole = reviewableRoles.mapNotNull { role ->
            val count = role.participations.count { participation ->
                participation.applicationStatus.equals(
                    "PENDING",
                    ignoreCase = true
                )
            }
            if (count > 0) role to count else null
        }

        val pendingReviewCount = pendingByRole.sumOf { it.second }
        if (pendingReviewCount == 0) return null

        val roleContext = when {
            pendingByRole.size == 1 -> pendingByRole.first().first.roleName
            pendingByRole.size > 1 -> "${pendingByRole.size} roles have pending applications"
            else -> null
        }

        return HomeAttentionItem(
            type = HomeAttentionType.APPLICATIONS_TO_REVIEW,
            severity = HomeAttentionSeverity.REVIEW,
            postId = post.postId,
            postTitle = post.title,
            contextLabel = roleContext,
            message = if (pendingReviewCount == 1) {
                "1 application is waiting for review."
            } else {
                "$pendingReviewCount applications are waiting for review."
            }
        )
    }

    private fun com.example.volunteerlink.organisation.home.model.OrganisationHomeRole
        .isApplicationReviewStillOpen(
            post: OrganisationHomePost,
            nowMillis: Long
        ): Boolean {
        return RoleApplicationWindowEvaluator.evaluate(
            input = RoleApplicationWindowInput(
                roleMode = roleMode,
                postStatus = post.status,
                physicalStartDate = post.physicalStartDate,
                remoteStartDate = post.remoteStartDate
            ),
            nowMillis = nowMillis
        ).isOpen
    }

    private fun buildDraftAttention(
        post: OrganisationHomePost,
        input: PostTimingInput,
        nowMillis: Long
    ): HomeAttentionItem? {
        val attention = PostTimingEvaluator.evaluateDraftAttention(
            input = input,
            nowMillis = nowMillis
        )

        return when (attention.type) {
            DraftAttentionType.NONE -> null

            DraftAttentionType.START_TOO_SOON -> HomeAttentionItem(
                type = HomeAttentionType.DRAFT_START_TOO_SOON,
                severity = HomeAttentionSeverity.WARNING,
                postId = post.postId,
                postTitle = post.title,
                daysRemaining = attention.daysUntilStart,
                message = buildString {
                    append("Start date is less than 7 days away.")
                    attention.earliestPublishableDate?.let { earliest ->
                        append(" Choose ${formatHomeDate(earliest)} or later before publishing.")
                    }
                }
            )

            DraftAttentionType.START_DATE_PASSED -> HomeAttentionItem(
                type = HomeAttentionType.DRAFT_START_DATE_PASSED,
                severity = HomeAttentionSeverity.URGENT,
                postId = post.postId,
                postTitle = post.title,
                daysRemaining = attention.daysUntilStart,
                message = "Start date has passed. Choose a new date before publishing."
            )
        }
    }

    private fun OrganisationHomePost.toTimingInput(): PostTimingInput? {
        val postMode = PostMode.fromDatabaseValue(mode) ?: return null
        return PostTimingInput(
            mode = postMode,
            physicalStartDate = physicalStartDate,
            physicalEndDate = physicalEndDate,
            remoteStartDate = remoteStartDate,
            remoteEndDate = remoteEndDate
        )
    }

    private fun OrganisationHomePost.toHomePostItem(
        timingState: PostTimingState,
        nowMillis: Long
    ): HomePostItem {
        val input = toTimingInput()
        val physicalState = evaluateSinglePeriodState(
            mode = PostMode.PHYSICAL,
            startDate = physicalStartDate,
            endDate = physicalEndDate,
            nowMillis = nowMillis
        )
        val remoteState = evaluateSinglePeriodState(
            mode = PostMode.REMOTE,
            startDate = remoteStartDate,
            endDate = remoteEndDate,
            nowMillis = nowMillis
        )

        return HomePostItem(
            postId = postId,
            title = title,
            mode = mode,
            databaseStatus = status,
            timingState = timingState,
            startDate = input?.let(PostTimingEvaluator::earliestStartDate),
            endDate = latestDate(physicalEndDate, remoteEndDate),
            locationName = if (mode.equals("PHYSICAL", ignoreCase = true)) {
                physicalLocationName
            } else {
                null
            },
            physicalStartDate = physicalStartDate,
            physicalEndDate = physicalEndDate,
            physicalLocationName = physicalLocationName,
            physicalTimingState = physicalState,
            remoteStartDate = remoteStartDate,
            remoteEndDate = remoteEndDate,
            remoteTimingState = remoteState
        )
    }

    private fun evaluateSinglePeriodState(
        mode: PostMode,
        startDate: String?,
        endDate: String?,
        nowMillis: Long
    ): PostTimingState? {
        if (startDate.isNullOrBlank() || endDate.isNullOrBlank()) return null

        return PostTimingEvaluator.evaluatePostTiming(
            input = when (mode) {
                PostMode.PHYSICAL -> PostTimingInput(
                    mode = PostMode.PHYSICAL,
                    physicalStartDate = startDate,
                    physicalEndDate = endDate
                )
                PostMode.REMOTE -> PostTimingInput(
                    mode = PostMode.REMOTE,
                    remoteStartDate = startDate,
                    remoteEndDate = endDate
                )
                PostMode.HYBRID -> return null
            },
            nowMillis = nowMillis
        )
    }

    private fun formatHomeDate(value: String): String {
        val parts = value.split("-")
        if (parts.size != 3) return value
        val month = when (parts[1]) {
            "01" -> "Jan"
            "02" -> "Feb"
            "03" -> "Mar"
            "04" -> "Apr"
            "05" -> "May"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Aug"
            "09" -> "Sep"
            "10" -> "Oct"
            "11" -> "Nov"
            "12" -> "Dec"
            else -> return value
        }
        val day = parts[2].toIntOrNull() ?: return value
        return "$day $month"
    }

    private fun latestDate(first: String?, second: String?): String? {
        return when {
            first.isNullOrBlank() -> second
            second.isNullOrBlank() -> first
            first >= second -> first
            else -> second
        }
    }

    companion object {
        // Authentication/organisation identity is not integrated yet. Keep the
        // same temporary organisation used by Create Post until that work begins.
        private const val TEST_ORGANISATION_ID = "ORG0001"
        private const val TAG = "OrganisationHomeVM"
    }
}
