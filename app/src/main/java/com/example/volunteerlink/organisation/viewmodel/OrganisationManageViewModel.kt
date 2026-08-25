package com.example.volunteerlink.organisation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.post.DraftAttentionType
import com.example.volunteerlink.data.post.MissingTrainingDetail
import com.example.volunteerlink.data.post.PostMode
import com.example.volunteerlink.data.post.PostTimingEvaluator
import com.example.volunteerlink.data.post.PostTimingInput
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.data.post.RoleApplicationWindowEvaluator
import com.example.volunteerlink.data.post.RoleApplicationWindowInput
import com.example.volunteerlink.data.post.TrainingAttentionType
import com.example.volunteerlink.data.post.TrainingLocationMode
import com.example.volunteerlink.data.post.TrainingMode
import com.example.volunteerlink.data.post.TrainingTimingInput
import com.example.volunteerlink.data.time.AppClock
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

    init {
        refresh()
        observeAppClock()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val snapshot = repository.loadHomeSnapshot(TEST_ORGANISATION_ID)
                cachedSnapshot = snapshot
                applySnapshot(snapshot)
            } catch (exception: Exception) {
                Log.e(TAG, "Could not load Manage data.", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message
                        ?: "Unable to load organisation posts."
                )
            }
        }
    }

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

            when (status) {
                "DRAFT" -> {
                    drafts += post.toManageItem(
                        timingState = null,
                        nowMillis = nowMillis,
                        attentionItems = buildDraftAttention(
                            post = post,
                            input = timingInput,
                            nowMillis = nowMillis
                        ) + buildTrainingAttention(
                            post = post,
                            nowMillis = nowMillis,
                            allowOutdated = true
                        )
                    )
                }

                "PUBLISHED", "CLOSED" -> when (timingState) {
                    PostTimingState.UPCOMING,
                    PostTimingState.ONGOING -> {
                        val attention = mutableListOf<ManageAttentionItem>()

                        if (status == "PUBLISHED") {
                            buildApplicationAttention(
                                post = post,
                                nowMillis = nowMillis
                            )?.let(attention::add)
                        }

                        attention += buildTrainingAttention(
                            post = post,
                            nowMillis = nowMillis,
                            allowOutdated = false
                        )

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
            errorMessage = null
        )
    }

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

    private fun buildTrainingAttention(
        post: OrganisationHomePost,
        nowMillis: Long,
        allowOutdated: Boolean
    ): List<ManageAttentionItem> {
        return post.schedules
            .filter { it.scheduleType.equals("TRAINING", ignoreCase = true) }
            .mapNotNull { schedule ->
                val trainingMode = TrainingMode.fromDatabaseValue(
                    schedule.trainingMode
                ) ?: return@mapNotNull null

                val attention = PostTimingEvaluator.evaluateTrainingAttention(
                    input = TrainingTimingInput(
                        scheduleDate = schedule.scheduleDate,
                        startTime = schedule.startTime,
                        trainingMode = trainingMode,
                        meetingLink = schedule.meetingLink,
                        trainingLocationMode = TrainingLocationMode.fromDatabaseValue(
                            schedule.trainingLocationMode
                        ),
                        trainingLocationName = schedule.trainingLocationName
                    ),
                    nowMillis = nowMillis
                )

                if (!attention.needsAttention) return@mapNotNull null
                if (
                    attention.type == TrainingAttentionType.OUTDATED &&
                    !allowOutdated
                ) {
                    return@mapNotNull null
                }

                val missing = when (attention.missingDetail) {
                    MissingTrainingDetail.MEETING_LINK -> "meeting link"
                    MissingTrainingDetail.LOCATION -> "on-site location"
                    null -> "training details"
                }

                when (attention.type) {
                    TrainingAttentionType.WARNING -> ManageAttentionItem(
                        type = ManageAttentionType.TRAINING_DETAILS_WARNING,
                        severity = ManageAttentionSeverity.WARNING,
                        kindLabel = "TRAINING",
                        title = schedule.title,
                        message = "${formatShortDate(schedule.scheduleDate)} · Add the $missing before volunteers attend."
                    )

                    TrainingAttentionType.URGENT -> ManageAttentionItem(
                        type = ManageAttentionType.TRAINING_DETAILS_URGENT,
                        severity = ManageAttentionSeverity.URGENT,
                        kindLabel = "TRAINING",
                        title = schedule.title,
                        message = when (attention.daysUntilTraining) {
                            0 -> "Training is today. Add the $missing now."
                            1 -> "Training is tomorrow. Add the $missing now."
                            else -> "Training is in ${attention.daysUntilTraining} days. Add the $missing now."
                        }
                    )

                    TrainingAttentionType.OUTDATED -> ManageAttentionItem(
                        type = ManageAttentionType.TRAINING_OUTDATED,
                        severity = ManageAttentionSeverity.URGENT,
                        kindLabel = "TRAINING",
                        title = schedule.title,
                        message = "This draft contains a passed training date with incomplete details."
                    )

                    TrainingAttentionType.NONE -> null
                }
            }
            .sortedBySeverity()
    }

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

    private fun OrganisationHomeRole.isReviewStillOpen(
        post: OrganisationHomePost,
        nowMillis: Long
    ): Boolean {
        return RoleApplicationWindowEvaluator.evaluate(
            input = RoleApplicationWindowInput(
                roleMode = roleMode,
                postStatus = post.status,
                physicalStartDate = post.physicalStartDate,
                remoteStartDate = post.remoteStartDate,
                applicationClosingScheduleDates = applicationClosingSchedules
                    .map { it.scheduleDate }
            ),
            nowMillis = nowMillis
        ).isOpen
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
            endDate = latestDate(physicalEndDate, remoteEndDate),
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
            remoteEndDate = remoteEndDate,
            remoteTimingState = evaluateSinglePeriod(
                PostMode.REMOTE,
                remoteStartDate,
                remoteEndDate,
                nowMillis
            ),
            attentionItems = attentionItems.sortedBySeverity()
        )
    }

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

    private fun latestDate(first: String?, second: String?): String? {
        return when {
            first.isNullOrBlank() -> second
            second.isNullOrBlank() -> first
            first >= second -> first
            else -> second
        }
    }

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
        private const val TEST_ORGANISATION_ID = "ORG0001"
        private const val TAG = "OrganisationManageVM"
    }
}
