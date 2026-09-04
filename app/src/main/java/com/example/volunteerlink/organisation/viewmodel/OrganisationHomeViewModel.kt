package com.example.volunteerlink.organisation.viewmodel

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Owns the Organisation Home dashboard state and coordinates the data needed for summary cards, attention items
// and quick navigation.
//
// It loads the authenticated organisation context, refreshes application lifecycle state through the repository,
// and exposes a single Compose-friendly UI state.
//
// A successful server snapshot can be cached for read-only offline display; actions that change real
// application/post data still require a live Supabase request.
//
// Date-sensitive dashboard information is recalculated when AppClock refreshes so the dashboard follows the same
// business date used by Create/Manage flows.
//
// Architectural layer: ViewModel / workflow state layer.
// ============================================================================


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
import com.example.volunteerlink.organisation.auth.OrganisationSession
import com.example.volunteerlink.organisation.data.OrganisationLocalStorage
import com.example.volunteerlink.organisation.home.model.HomeAttentionItem
import com.example.volunteerlink.organisation.home.model.HomeAttentionSeverity
import com.example.volunteerlink.organisation.home.model.HomeAttentionType
import com.example.volunteerlink.organisation.home.model.HomePostItem
import com.example.volunteerlink.organisation.home.model.OrganisationHomePost
import com.example.volunteerlink.organisation.home.model.OrganisationHomeSnapshot
import com.example.volunteerlink.organisation.home.model.OrganisationImpactWeaveAttention
import com.example.volunteerlink.organisation.home.model.OrganisationHomeUiState
import com.example.volunteerlink.organisation.repository.OrganisationHomeRepository
import com.example.volunteerlink.organisation.repository.SupabaseOrganisationHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Prepares everything Organisation Home needs to display.
 *
 * The repository owns Supabase reads. PostTimingEvaluator owns time rules. This
 * ViewModel combines both into one read-only StateFlow for the future Home UI.
 */
/**
 * DETAILED DECLARATION — OrganisationHomeViewModel
 *
 * Lifecycle-aware state owner for Organisation Home View Model. It survives ordinary Compose recomposition and
 * coordinates asynchronous repository work.
 *
 * UI callbacks enter through methods on this class so validation, loading/error state and dependent business
 * rules remain centralised.
 */
class OrganisationHomeViewModel : ViewModel() {

    private val homeRepository: OrganisationHomeRepository =
        SupabaseOrganisationHomeRepository()

    private val _uiState = MutableStateFlow(
        OrganisationHomeUiState(isLoading = true)
    )
    val uiState = _uiState.asStateFlow()

    private var cachedSnapshot: OrganisationHomeSnapshot? = null
    private var refreshInProgress = false

    init {
        refresh()
        observeAppClock()
    }

    /** Loads saved data first, then tries to sync the latest snapshot from Supabase. */
    /**
     * DETAILED BEHAVIOUR — refresh
     *
     * Loads or refreshes the data required by refresh and writes the result into observable UI state.
     *
     * The coroutine/repository boundary is handled here so Compose only reacts to loading, success and error
     * state.
     *
     * Coordinates account-scoped local persistence only for recoverable/cached UI state; published or
     * transactional business state continues to come from Supabase.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    fun refresh() {
        if (refreshInProgress) return
        refreshInProgress = true

        viewModelScope.launch {
            val saved = runCatching {
                OrganisationLocalStorage.loadSnapshot()
            }.getOrNull()

            if (cachedSnapshot == null && saved != null) {
                cachedSnapshot = saved.snapshot
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastSyncedAtEpochMillis = saved.lastSyncedAtEpochMillis,
                    isRefreshing = _uiState.value.isShowingCachedData,
                    errorMessage = null
                )
                applySnapshot(saved.snapshot)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = cachedSnapshot == null,
                    isRefreshing = cachedSnapshot != null && _uiState.value.isShowingCachedData,
                    errorMessage = null
                )
            }

            try {
                val organisationId = resolveCurrentOrganisationId()
                val snapshot = homeRepository.loadHomeSnapshot(
                    organisationId = organisationId
                )
                val syncedAt = System.currentTimeMillis()

                runCatching {
                    OrganisationLocalStorage.saveSnapshot(
                        snapshot = snapshot,
                        syncedAtEpochMillis = syncedAt
                    )
                }

                cachedSnapshot = snapshot
                _uiState.value = _uiState.value.copy(
                    isShowingCachedData = false,
                    lastSyncedAtEpochMillis = syncedAt,
                    isRefreshing = false
                )
                applySnapshot(snapshot)
            } catch (exception: Exception) {
                Log.e(TAG, "Could not load Organisation Home data.", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isShowingCachedData = cachedSnapshot != null,
                    errorMessage = if (cachedSnapshot == null) {
                        exception.message ?: "Unable to load organisation home data."
                    } else {
                        null
                    }
                )
            } finally {
                refreshInProgress = false
            }
        }
    }

    /**
     * Resolves the ORGANISATION ID of whoever is actually signed in right now,
     * via the same two-step lookup used elsewhere (auth.uid() -> user_profiles
     * -> organisations). Keeps the authenticated organisation lookup in one reusable data path.
     */
    /**
     * DETAILED BEHAVIOUR — resolveCurrentOrganisationId
     *
     * Implements the ViewModel workflow operation for resolve current organisation id.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     */
    private suspend fun resolveCurrentOrganisationId(): String =
        OrganisationSession.requireOrganisationId()

    /**
     * AppClock refreshes every few seconds during the current test/demo setup.
     * Recalculate the cached post states immediately when its value changes;
     * there is no reason to query Supabase posts again just because time changed.
     */
    /**
     * DETAILED BEHAVIOUR — observeAppClock
     *
     * Implements the ViewModel workflow operation for observe app clock.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     */
    private fun observeAppClock() {
        viewModelScope.launch {
            AppClock.state.collect { clockState ->
                if (!clockState.isLoaded) return@collect
                cachedSnapshot?.let(::applySnapshot)
            }
        }
    }

    /**
     * Applies the snapshot used by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — applySnapshot
     *
     * Implements the ViewModel workflow operation for apply snapshot.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Uses the shared PostTimingEvaluator rather than duplicating the seven-day/timing classification inside
     * this method.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
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

        snapshot.impactWeaveAttention.forEach { impactAttention ->
            attentionItems += impactAttention.toHomeAttentionItem()
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

        val currentState = _uiState.value
        _uiState.value = OrganisationHomeUiState(
            isLoading = false,
            organisationName = snapshot.organisationName,
            profileImageUrl = snapshot.profileImageUrl,
            ongoingCount = sortedOngoing.size,
            upcomingCount = sortedUpcoming.size,
            draftCount = draftCount,
            attentionItems = sortedAttention,
            ongoingPosts = sortedOngoing,
            upcomingPosts = sortedUpcoming,
            isShowingCachedData = currentState.isShowingCachedData,
            lastSyncedAtEpochMillis = currentState.lastSyncedAtEpochMillis,
            isRefreshing = currentState.isRefreshing,
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

    /**
     * Derives the organisation impact weave attention value used by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — toHomeAttentionItem
     *
     * Implements the ViewModel workflow operation for to home attention item.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun OrganisationImpactWeaveAttention.toHomeAttentionItem(): HomeAttentionItem {
        val type = when (attentionType.uppercase(Locale.ROOT)) {
            "READY" -> HomeAttentionType.IMPACT_WEAVE_READY
            "DEADLINE_SOON" -> HomeAttentionType.IMPACT_WEAVE_DEADLINE_SOON
            "DEADLINE_PASSED" -> HomeAttentionType.IMPACT_WEAVE_DEADLINE_PASSED
            "ACTIVITY_PASSED" -> HomeAttentionType.IMPACT_WEAVE_ACTIVITY_PASSED
            "PROGRESS" -> HomeAttentionType.IMPACT_WEAVE_PROGRESS
            else -> HomeAttentionType.IMPACT_WEAVE_DEADLINE_SOON
        }
        val mappedSeverity = when (severity.uppercase(Locale.ROOT)) {
            "URGENT" -> HomeAttentionSeverity.URGENT
            "WARNING" -> HomeAttentionSeverity.WARNING
            "NEEDS_REVIEW" -> HomeAttentionSeverity.NEEDS_REVIEW
            else -> HomeAttentionSeverity.REVIEW
        }
        val readableStatus = status.lowercase(Locale.ROOT)
            .replaceFirstChar { it.titlecase(Locale.ROOT) }

        return HomeAttentionItem(
            type = type,
            severity = mappedSeverity,
            postId = draftId,
            postTitle = title,
            contextLabel = "Impact Weave · $readableStatus",
            message = message,
            daysRemaining = daysRemaining
        )
    }

    /**
     * Builds the completion review attention used by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — buildCompletionReviewAttention
     *
     * Implements the ViewModel workflow operation for build completion review attention.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
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

    /**
     * Builds the application review attention used by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — buildApplicationReviewAttention
     *
     * Implements the ViewModel workflow operation for build application review attention.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
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

    /**
     * Derives the com value used by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
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

    /**
     * Builds the draft attention used by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — buildDraftAttention
     *
     * Implements the ViewModel workflow operation for build draft attention.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses the shared PostTimingEvaluator rather than duplicating the seven-day/timing classification inside
     * this method.
     */
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

    /**
     * Derives the organisation home post value used by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — toTimingInput
     *
     * Implements the ViewModel workflow operation for to timing input.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun OrganisationHomePost.toTimingInput(): PostTimingInput? {
        val postMode = PostMode.fromDatabaseValue(mode) ?: return null
        return PostTimingInput(
            mode = postMode,
            physicalStartDate = physicalStartDate,
            physicalEndDate = physicalEndDate,
            remoteStartDate = remoteStartDate,
            remoteEndDate = effectiveRemoteEndDate
        )
    }

    /**
     * Derives the organisation home post value used by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — toHomePostItem
     *
     * Implements the ViewModel workflow operation for to home post item.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses the shared PostTimingEvaluator rather than duplicating the seven-day/timing classification inside
     * this method.
     */
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
            endDate = effectiveRemoteEndDate,
            nowMillis = nowMillis
        )

        return HomePostItem(
            postId = postId,
            title = title,
            mode = mode,
            databaseStatus = status,
            timingState = timingState,
            startDate = input?.let(PostTimingEvaluator::earliestStartDate),
            endDate = latestDate(physicalEndDate, effectiveRemoteEndDate),
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
            remoteEndDate = effectiveRemoteEndDate,
            remoteTimingState = remoteState
        )
    }

    /**
     * Derives the evaluate single period state value used by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — evaluateSinglePeriodState
     *
     * Implements the ViewModel workflow operation for evaluate single period state.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses the shared PostTimingEvaluator rather than duplicating the seven-day/timing classification inside
     * this method.
     */
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

    /**
     * Formats the home date used by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — formatHomeDate
     *
     * Implements the ViewModel workflow operation for format home date.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
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

    /**
     * Returns the latest date value required by the organisation Home dashboard flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — latestDate
     *
     * Implements the ViewModel workflow operation for latest date.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun latestDate(first: String?, second: String?): String? {
        return when {
            first.isNullOrBlank() -> second
            second.isNullOrBlank() -> first
            first >= second -> first
            else -> second
        }
    }

    companion object {
        private const val TAG = "OrganisationHomeVM"
    }
}
