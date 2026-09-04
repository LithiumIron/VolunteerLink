package com.example.volunteerlink.organisation.viewmodel

// FILE OVERVIEW:
/*
 * OrganisationManageViewModel coordinates state and user actions for the organisation Manage Post flow.
 * It translates UI events into validation/repository operations and exposes observable state
 * back to Compose so the screen can stay declarative.
 */


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
import com.example.volunteerlink.organisation.home.model.OrganisationHomePost
import com.example.volunteerlink.organisation.home.model.OrganisationHomeRole
import com.example.volunteerlink.organisation.home.model.OrganisationHomeSnapshot
import com.example.volunteerlink.organisation.manage.model.ManageAttentionItem
import com.example.volunteerlink.organisation.manage.model.ManageAttentionSeverity
import com.example.volunteerlink.organisation.manage.model.ManageAttentionType
import com.example.volunteerlink.organisation.manage.model.ManagePostItem
import com.example.volunteerlink.organisation.manage.model.OrganisationManageUiState
import com.example.volunteerlink.organisation.repository.OrganisationHomeRepository
import com.example.volunteerlink.organisation.repository.SupabaseOrganisationHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Builds the organisation's post-management lifecycle from the same normalized
 * post snapshot used by Home.
 *
 * Database status remains separate from derived time state:
 * - DRAFT -> Drafts
 * - PUBLISHED/CLOSED + Upcoming/Ongoing -> Active
 * - PUBLISHED/CLOSED + Past -> Review
 * - COMPLETED -> Completed
 */
class OrganisationManageViewModel : ViewModel() {

    private val repository: OrganisationHomeRepository =
        SupabaseOrganisationHomeRepository()

    private val _uiState = MutableStateFlow(OrganisationManageUiState())
    val uiState = _uiState.asStateFlow()

    private var cachedSnapshot: OrganisationHomeSnapshot? = null
    private var partnerPosts: List<ManagePostItem> = emptyList()
    private var ownerPartnershipSummaries: Map<String, String> = emptyMap()
    private var refreshInProgress = false

    init {
        refresh()
        observeAppClock()
    }

    /**
     * Reloads the latest data for the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
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

                // The organisation's own Volunteer Posts are the primary Manage data.
                // Partnership-post loading is deliberately isolated below: a missing or
                // temporarily failing partnership RPC must not make the whole Manage
                // screen pretend it is offline when this live snapshot succeeded.
                val snapshot = repository.loadHomeSnapshot(organisationId)

                runCatching { repository.loadPartnerPosts() }
                    .onSuccess { partnershipPosts ->
                        ownerPartnershipSummaries = partnershipPosts
                            .filter { it.isOwner }
                            .associate { it.postId to it.contributionSummary }
                        partnerPosts = partnershipPosts
                            .filterNot { it.isOwner }
                            .map { post ->
                                ManagePostItem(
                                    postId = post.postId,
                                    title = post.title,
                                    description = post.description,
                                    mode = post.mode,
                                    databaseStatus = post.status,
                                    startDate = post.startDate,
                                    endDate = post.endDate,
                                    locationName = post.locationName,
                                    isPartnerPost = true,
                                    ownerOrganisationName = post.ownerOrganisationName,
                                    contributionSummary = post.contributionSummary
                                )
                            }
                    }
                    .onFailure { partnershipException ->
                        Log.w(
                            TAG,
                            "Could not refresh partnership-post data; keeping the last partnership list.",
                            partnershipException
                        )
                    }

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
                Log.e(TAG, "Could not load Manage data.", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isShowingCachedData = cachedSnapshot != null,
                    errorMessage = if (cachedSnapshot == null) {
                        exception.message ?: "Unable to load organisation posts."
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
     * Resolves the ORGANISATION ID of whoever is actually signed in right now.
     * Same lookup used in OrganisationHomeViewModel — replaces the previous
     * hardcoded prototype organisation identity.
     */
    private suspend fun resolveCurrentOrganisationId(): String =
        OrganisationSession.requireOrganisationId()

    /**
     * Derives the observe app clock value used by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
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
     * Applies the snapshot used by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun applySnapshot(snapshot: OrganisationHomeSnapshot) {
        val nowMillis = AppClock.nowMillis()
        val active = mutableListOf<ManagePostItem>()
        val drafts = mutableListOf<ManagePostItem>()
        val review = mutableListOf<ManagePostItem>()
        val completed = mutableListOf<ManagePostItem>()

        snapshot.posts.forEach { post ->
            val status = post.status.uppercase(Locale.US)
            val timingInput = post.toTimingInput()
            val timingState = timingInput?.let {
                PostTimingEvaluator.evaluatePostTiming(it, nowMillis)
            }
            val physicalTimingState = evaluateSinglePeriod(
                PostMode.PHYSICAL,
                post.physicalStartDate,
                post.physicalEndDate,
                nowMillis
            )
            val remoteTimingState = evaluateSinglePeriod(
                PostMode.REMOTE,
                post.remoteStartDate,
                post.effectiveRemoteEndDate,
                nowMillis
            )
            val hybridHasReviewSide =
                post.mode.equals("HYBRID", ignoreCase = true) &&
                    (
                        physicalTimingState == PostTimingState.PAST ||
                            remoteTimingState == PostTimingState.PAST
                    )

            when (status) {
                "DRAFT" -> {
                    drafts += post.toManageItem(
                        timingState = null,
                        nowMillis = nowMillis,
                        attentionItems = buildDraftAttention(
                            post = post,
                            input = timingInput,
                            nowMillis = nowMillis
                        )
                    )
                }

                "PUBLISHED", "CLOSED" -> {
                    if (hybridHasReviewSide) {
                        val endedSides = buildList {
                            if (physicalTimingState == PostTimingState.PAST) add("Physical")
                            if (remoteTimingState == PostTimingState.PAST) add("Remote")
                        }
                        val stillActiveSides = buildList {
                            if (physicalTimingState in setOf(PostTimingState.ONGOING, PostTimingState.UPCOMING)) add("Physical")
                            if (remoteTimingState in setOf(PostTimingState.ONGOING, PostTimingState.UPCOMING)) add("Remote")
                        }
                        val message = when {
                            endedSides.size == 2 ->
                                "Both Physical and Remote phases have ended. Finish each side's close-out before the Hybrid post can complete."
                            stillActiveSides.isNotEmpty() ->
                                "${endedSides.joinToString()} close-out is available while ${stillActiveSides.joinToString()} is still active."
                            else ->
                                "${endedSides.joinToString()} close-out is available."
                        }

                        review += post.toManageItem(
                            timingState = timingState,
                            nowMillis = nowMillis,
                            attentionItems = listOf(
                                ManageAttentionItem(
                                    type = ManageAttentionType.COMPLETION_REVIEW,
                                    severity = ManageAttentionSeverity.NEEDS_REVIEW,
                                    kindLabel = "HYBRID CLOSE-OUT",
                                    title = when {
                                        endedSides.size == 2 -> "Physical and Remote review available"
                                        else -> "${endedSides.first()} review available"
                                    },
                                    message = message
                                )
                            )
                        )
                    } else {
                        when (timingState) {
                            PostTimingState.UPCOMING,
                            PostTimingState.ONGOING -> {
                                val attention = mutableListOf<ManageAttentionItem>()

                                if (status == "PUBLISHED") {
                                    buildApplicationAttention(
                                        post = post,
                                        nowMillis = nowMillis
                                    )?.let(attention::add)
                                }

                                active += post.toManageItem(
                                    timingState = timingState,
                                    nowMillis = nowMillis,
                                    attentionItems = attention.sortedBySeverity()
                                )
                            }

                            PostTimingState.PAST -> {
                                review += post.toManageItem(
                                    timingState = PostTimingState.PAST,
                                    nowMillis = nowMillis,
                                    attentionItems = listOf(
                                        ManageAttentionItem(
                                            type = ManageAttentionType.COMPLETION_REVIEW,
                                            severity = ManageAttentionSeverity.NEEDS_REVIEW,
                                            kindLabel = "CLOSE-OUT",
                                            title = "Completion review required",
                                            message = "The activity has ended. Finish attendance and volunteer review before marking this post completed."
                                        )
                                    )
                                )
                            }

                            null -> Unit
                        }
                    }
                }

                "COMPLETED" -> {
                    completed += post.toManageItem(
                        timingState = timingState,
                        nowMillis = nowMillis,
                        attentionItems = emptyList()
                    )
                }

                // Cancelled posts are intentionally not part of the V1 Manage
                // lifecycle tabs. A History/Archived view can be added later.
                "CANCELLED" -> Unit
            }
        }

        val currentState = _uiState.value
        _uiState.value = OrganisationManageUiState(
            isLoading = false,
            organisationName = snapshot.organisationName,
            activePosts = active.sortedWith(
                compareBy<ManagePostItem> {
                    if (it.timingState == PostTimingState.ONGOING) 0 else 1
                }.thenByDescending { it.highestSeverityRank() }
                    .thenBy { item ->
                        // Keep Ongoing before Upcoming. Inside each group, posts
                        // needing action come first, then the most relevant date.
                        if (item.timingState == PostTimingState.ONGOING) {
                            item.endDate.orEmpty()
                        } else {
                            item.startDate.orEmpty()
                        }
                    }.thenBy { it.title }
            ),
            draftPosts = drafts.sortedWith(
                compareByDescending<ManagePostItem> {
                    it.highestSeverityRank()
                }.thenBy { it.startDate.orEmpty() }
                    .thenBy { it.title }
            ),
            reviewPosts = review.sortedWith(
                compareByDescending<ManagePostItem> { it.endDate.orEmpty() }
                    .thenBy { it.title }
            ),
            completedPosts = completed.sortedWith(
                compareByDescending<ManagePostItem> { it.endDate.orEmpty() }
                    .thenBy { it.title }
            ),
            partnerPosts = partnerPosts.sortedWith(
                compareBy<ManagePostItem> { it.startDate.orEmpty() }.thenBy { it.title }
            ),
            impactWeaveAttention = snapshot.impactWeaveAttention,
            isShowingCachedData = currentState.isShowingCachedData,
            lastSyncedAtEpochMillis = currentState.lastSyncedAtEpochMillis,
            isRefreshing = currentState.isRefreshing,
            errorMessage = null
        )
    }

    /**
     * Builds the draft attention used by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun buildDraftAttention(
        post: OrganisationHomePost,
        input: PostTimingInput?,
        nowMillis: Long
    ): List<ManageAttentionItem> {
        if (input == null) return emptyList()

        val result = PostTimingEvaluator.evaluateDraftAttention(
            input = input,
            nowMillis = nowMillis
        )

        return when (result.type) {
            DraftAttentionType.NONE -> emptyList()

            DraftAttentionType.START_TOO_SOON -> listOf(
                ManageAttentionItem(
                    type = ManageAttentionType.DRAFT_START_TOO_SOON,
                    severity = ManageAttentionSeverity.WARNING,
                    kindLabel = "DRAFT",
                    title = "Start date needs updating",
                    message = buildString {
                        append("The volunteering start is less than 7 days away.")
                        result.earliestPublishableDate?.let { earliest ->
                            append(" Choose ${formatShortDate(earliest)} or later before publishing.")
                        }
                    }
                )
            )

            DraftAttentionType.START_DATE_PASSED -> listOf(
                ManageAttentionItem(
                    type = ManageAttentionType.DRAFT_START_DATE_PASSED,
                    severity = ManageAttentionSeverity.URGENT,
                    kindLabel = "DRAFT",
                    title = "Start date has passed",
                    message = "Choose a new volunteering date before publishing this draft."
                )
            )
        }
    }

    /**
     * Builds the application attention used by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun buildApplicationAttention(
        post: OrganisationHomePost,
        nowMillis: Long
    ): ManageAttentionItem? {
        val pendingByRole = post.roles.mapNotNull { role ->
            if (!role.applicationMethod.equals("REVIEW_APPLICANTS", true)) {
                return@mapNotNull null
            }
            if (!role.isReviewStillOpen(post, nowMillis)) {
                return@mapNotNull null
            }

            val pending = role.participations.count {
                it.applicationStatus.equals("PENDING", true)
            }
            if (pending > 0) role to pending else null
        }

        val count = pendingByRole.sumOf { it.second }
        if (count == 0) return null

        val context = if (pendingByRole.size == 1) {
            pendingByRole.first().first.roleName
        } else {
            "${pendingByRole.size} roles"
        }

        return ManageAttentionItem(
            type = ManageAttentionType.APPLICATIONS_TO_REVIEW,
            severity = ManageAttentionSeverity.REVIEW,
            kindLabel = "APPLICATION",
            title = context,
            message = if (count == 1) {
                "1 application is waiting for review."
            } else {
                "$count applications are waiting for review."
            }
        )
    }

    /**
     * Derives the organisation home role value used by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun OrganisationHomeRole.isReviewStillOpen(
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
     * Derives the organisation home post value used by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
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
     * Derives the organisation home post value used by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun OrganisationHomePost.toManageItem(
        timingState: PostTimingState?,
        nowMillis: Long,
        attentionItems: List<ManageAttentionItem>
    ): ManagePostItem {
        val timingInput = toTimingInput()

        return ManagePostItem(
            postId = postId,
            title = title,
            mode = mode,
            databaseStatus = status,
            timingState = timingState,
            startDate = timingInput?.let(PostTimingEvaluator::earliestStartDate),
            endDate = latestDate(physicalEndDate, effectiveRemoteEndDate),
            locationName = if (mode.equals("PHYSICAL", true)) {
                physicalLocationName
            } else {
                null
            },
            physicalStartDate = physicalStartDate,
            physicalEndDate = physicalEndDate,
            physicalLocationName = physicalLocationName,
            physicalTimingState = evaluateSinglePeriod(
                PostMode.PHYSICAL,
                physicalStartDate,
                physicalEndDate,
                nowMillis
            ),
            remoteStartDate = remoteStartDate,
            remoteEndDate = effectiveRemoteEndDate,
            remoteTimingState = evaluateSinglePeriod(
                PostMode.REMOTE,
                remoteStartDate,
                effectiveRemoteEndDate,
                nowMillis
            ),
            attentionItems = attentionItems.sortedBySeverity(),
            contributionSummary = ownerPartnershipSummaries[postId],
            ownerOrganisationName = if (ownerPartnershipSummaries.containsKey(postId)) {
                "Your organisation"
            } else null
        )
    }

    /**
     * Derives the evaluate single period value used by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun evaluateSinglePeriod(
        mode: PostMode,
        startDate: String?,
        endDate: String?,
        nowMillis: Long
    ): PostTimingState? {
        if (startDate.isNullOrBlank() || endDate.isNullOrBlank()) return null

        val input = when (mode) {
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
        }

        return PostTimingEvaluator.evaluatePostTiming(input, nowMillis)
    }

    /**
     * Returns the requested data used by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun List<ManageAttentionItem>.sortedBySeverity(): List<ManageAttentionItem> {
        return sortedWith(
            compareBy<ManageAttentionItem> {
                when (it.severity) {
                    ManageAttentionSeverity.URGENT -> 0
                    ManageAttentionSeverity.WARNING -> 1
                    ManageAttentionSeverity.NEEDS_REVIEW -> 2
                    ManageAttentionSeverity.REVIEW -> 3
                }
            }.thenBy { it.title }
        )
    }

    /**
     * Renders the manage post item item used in the organisation Manage Post flow.
     * It receives state and callbacks from its caller so presentation code stays separate from database operations.
     */
    private fun ManagePostItem.highestSeverityRank(): Int {
        return attentionItems.maxOfOrNull {
            when (it.severity) {
                ManageAttentionSeverity.URGENT -> 4
                ManageAttentionSeverity.WARNING -> 3
                ManageAttentionSeverity.NEEDS_REVIEW -> 2
                ManageAttentionSeverity.REVIEW -> 1
            }
        } ?: 0
    }

    /**
     * Returns the latest date value required by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun latestDate(first: String?, second: String?): String? {
        return when {
            first.isNullOrBlank() -> second
            second.isNullOrBlank() -> first
            first >= second -> first
            else -> second
        }
    }

    /**
     * Formats the short date used by the organisation Manage Post flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    private fun formatShortDate(value: String): String {
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

    companion object {
        private const val TAG = "OrganisationManageVM"
    }
}
