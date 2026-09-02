package com.example.volunteerlink.organisation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.post.PostMode
import com.example.volunteerlink.data.post.PostTimingEvaluator
import com.example.volunteerlink.data.post.PostTimingInput
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.data.post.RoleApplicationWindowEvaluator
import com.example.volunteerlink.data.post.RoleApplicationWindowInput
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.manage.model.OrganisationPostManagementUiState
import com.example.volunteerlink.organisation.manage.model.PostManagementFeedbackGroup
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingDecisionSource
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingDecisionType
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingReviewDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReviewSession
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReviewStage
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReview
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalReviewEntry
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalAttendance
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteMissingAction
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteMissingDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReview
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewItem
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewSession
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteReviewStage
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmission
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmissionDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmissionDecisionType
import com.example.volunteerlink.organisation.manage.model.remoteReviewParticipationKey
import com.example.volunteerlink.organisation.manage.model.PostManagementVolunteerAttendanceDateStatus
import com.example.volunteerlink.organisation.manage.model.PostManagementVolunteerAttendanceSummary
import com.example.volunteerlink.organisation.repository.OrganisationPostManagementRepository
import com.example.volunteerlink.organisation.repository.SupabaseOrganisationPostManagementRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** ViewModel for one post opened from the Organisation Manage module. */
class OrganisationPostManagementViewModel : ViewModel() {

    private val repository: OrganisationPostManagementRepository =
        SupabaseOrganisationPostManagementRepository()

    private val _uiState = MutableStateFlow(OrganisationPostManagementUiState())
    val uiState = _uiState.asStateFlow()

    private var loadedPostId: String? = null
    private var cachedPost: PostManagementPost? = null

    private var attendancePollingJob: Job? = null
    private val attendanceRefreshMutex = Mutex()

    init {
        observeAppClock()
    }

    fun load(postId: String) {
        if (loadedPostId == postId && cachedPost != null) return
        stopAttendancePolling()
        loadedPostId = postId
        cachedPost = null
        _uiState.value = OrganisationPostManagementUiState()
        refresh()
    }

    /**
     * Lightweight polling for live attendance only. It is started by the
     * Physical Volunteers UI and stopped as soon as that UI is no longer active.
     */
    fun startAttendancePolling() {
        if (attendancePollingJob?.isActive == true) return
        val postId = loadedPostId ?: return
        val currentPost = cachedPost ?: return
        if (currentPost.physical == null) return

        attendancePollingJob = viewModelScope.launch {
            // Refresh once immediately, then at the small project-friendly interval.
            while (true) {
                if (loadedPostId != postId) break

                if (
                    !_uiState.value.isStartingAttendance &&
                    !_uiState.value.isUpdatingAttendance
                ) {
                    refreshAttendanceOnly(postId)
                }

                delay(ATTENDANCE_POLL_INTERVAL_MS)
            }
        }
    }

    fun stopAttendancePolling() {
        attendancePollingJob?.cancel()
        attendancePollingJob = null
    }

    /** Downloads a Remote submission through the management repository. */
    suspend fun downloadRemoteSubmission(
        submission: PostManagementRemoteSubmission
    ): ByteArray {
        val currentPost = cachedPost
            ?: error("Open a Volunteer Post before viewing its submission.")
        val filePath = submission.filePath
            ?.takeIf { it.isNotBlank() }
            ?: error("This submission does not contain an uploaded file.")

        return repository.downloadRemoteSubmission(
            postId = currentPost.postId,
            filePath = filePath
        )
    }

    /**
     * Accepts a Remote submission or requests revision, then reloads the post so
     * the organisation immediately sees the new submission state.
     */
    suspend fun reviewRemoteSubmission(
        submission: PostManagementRemoteSubmission,
        action: String,
        feedback: String? = null
    ) {
        val currentPost = cachedPost
            ?: error("Open a Volunteer Post before reviewing its submission.")

        repository.reviewRemoteSubmission(
            postId = currentPost.postId,
            submissionId = submission.submissionId,
            action = action,
            feedback = feedback
        )

        val refreshedPost = repository.loadPost(currentPost.postId)
        cachedPost = refreshedPost
        applyTiming(refreshedPost)
    }

    /** Stores a Needs Review submission decision locally until the whole Submission stage is saved. */
    fun setRemoteSubmissionDecision(
        submission: PostManagementRemoteSubmission,
        action: String,
        feedback: String? = null
    ) {
        val review = _uiState.value.post?.remoteReview ?: return
        if (!review.canEdit) return
        val item = review.items.firstOrNull {
            it.latestSubmission?.submissionId == submission.submissionId
        } ?: return
        if (!submission.status.equals("PENDING_REVIEW", ignoreCase = true)) return

        val type = when (action.trim().uppercase(Locale.US)) {
            "ACCEPT" -> PostManagementRemoteSubmissionDecisionType.ACCEPT
            "REQUEST_REVISION" -> PostManagementRemoteSubmissionDecisionType.REQUEST_REVISION
            "NOT_ACCEPT" -> PostManagementRemoteSubmissionDecisionType.NOT_ACCEPT
            else -> return
        }
        val cleanFeedback = feedback?.trim()?.takeIf { it.isNotEmpty() }
        if (type == PostManagementRemoteSubmissionDecisionType.REQUEST_REVISION && cleanFeedback == null) {
            _uiState.value = _uiState.value.copy(
                remoteReviewActionMessage = "Please explain what needs to be revised."
            )
            return
        }

        val session = _uiState.value.remoteReviewSession
        val updatedDecisions = session.submissionDecisions.filterNot {
            it.itemKey == item.itemKey
        } + PostManagementRemoteSubmissionDecision(
            itemKey = item.itemKey,
            submissionId = submission.submissionId,
            decision = type,
            feedback = if (type == PostManagementRemoteSubmissionDecisionType.REQUEST_REVISION) {
                cleanFeedback
            } else {
                null
            }
        )
        var updated = session.copy(
            submissionDecisions = updatedDecisions,
            missingActions = session.missingActions - item.itemKey,
            touched = false
        )
        updated = when {
            remoteExtensionRequired(review, updated) && updated.newEndDate.isNullOrBlank() ->
                updated.copy(newEndDate = minimumRemoteExtensionDate(review))

            !remoteExtensionRequired(review, updated) ->
                updated.copy(newEndDate = null)

            else -> updated
        }
        updateRemoteReviewSession(updated)
        _uiState.value = _uiState.value.copy(remoteReviewActionMessage = null)
    }

    fun clearRemoteSubmissionDecision(itemKey: String) {
        val review = _uiState.value.post?.remoteReview ?: return
        val session = _uiState.value.remoteReviewSession
        var updated = session.copy(
            submissionDecisions = session.submissionDecisions.filterNot { it.itemKey == itemKey },
            touched = false
        )
        if (!remoteExtensionRequired(review, updated)) updated = updated.copy(newEndDate = null)
        updateRemoteReviewSession(updated)
    }

    /** Missing Remote work can either stay open under one extension or be finalized as Not Completed. */
    fun setRemoteMissingAction(
        itemKey: String,
        giveMoreTime: Boolean
    ) {
        val review = _uiState.value.post?.remoteReview ?: return
        if (!review.canEdit || review.items.none { it.itemKey == itemKey }) return
        val session = _uiState.value.remoteReviewSession
        val updatedActions = session.missingActions.toMutableMap().apply {
            put(
                itemKey,
                if (giveMoreTime) {
                    PostManagementRemoteMissingAction.GIVE_MORE_TIME
                } else {
                    PostManagementRemoteMissingAction.CONTINUE_WITHOUT_WORK
                }
            )
        }
        var updated = session.copy(missingActions = updatedActions, touched = false)
        updated = when {
            remoteExtensionRequired(review, updated) && updated.newEndDate.isNullOrBlank() ->
                updated.copy(newEndDate = minimumRemoteExtensionDate(review))

            !remoteExtensionRequired(review, updated) ->
                updated.copy(newEndDate = null)

            else -> updated
        }
        updateRemoteReviewSession(updated)
        _uiState.value = _uiState.value.copy(remoteReviewActionMessage = null)
    }

    fun setRemoteReviewNewEndDate(value: String?) {
        val session = _uiState.value.remoteReviewSession
        updateRemoteReviewSession(session.copy(newEndDate = value?.trim()?.takeIf { it.isNotEmpty() }))
        _uiState.value = _uiState.value.copy(remoteReviewActionMessage = null)
    }

    /**
     * Applies every draft submission decision together. If extra time is needed,
     * one new Remote deadline is written for the whole project in the same RPC.
     */
    fun saveRemoteSubmissionReviewStage() {
        val currentPost = cachedPost ?: return
        val review = _uiState.value.post?.remoteReview ?: return
        val session = _uiState.value.remoteReviewSession
        if (!review.canEdit || _uiState.value.isUpdatingRemoteReview) return

        val unresolvedItem = review.items.firstOrNull { item ->
            when (item.currentStatus.uppercase(Locale.US)) {
                "ACCEPTED", "NOT_ACCEPTED" -> false
                "PENDING_REVIEW" -> session.submissionDecisionFor(item.itemKey) == null
                "REVISION_REQUESTED", "NOT_SUBMITTED" -> session.missingActionFor(item.itemKey) == null
                else -> true
            }
        }
        if (unresolvedItem != null) {
            val unresolvedName = unresolvedItem.person?.fullName
                ?: if (unresolvedItem.isShared) "the Shared Team deliverable" else unresolvedItem.roleName
            _uiState.value = _uiState.value.copy(
                remoteReviewActionMessage =
                    "Still unresolved: $unresolvedName. Choose a submission decision or a missing-work action first."
            )
            return
        }

        val requiresExtension = remoteExtensionRequired(review, session)
        if (requiresExtension) {
            val newDate = session.newEndDate
            if (!isValidRemoteExtensionDate(
                    currentDeadline = review.currentDeadline,
                    todayDate = review.todayDate,
                    candidate = newDate
                )
            ) {
                _uiState.value = _uiState.value.copy(
                    remoteReviewActionMessage =
                        "Choose one new project deadline later than both the current deadline and today."
                )
                return
            }
        }

        val missingDecisions = review.items.mapNotNull { item ->
            val action = session.missingActionFor(item.itemKey) ?: return@mapNotNull null
            PostManagementRemoteMissingDecision(
                // SHARED_TEAM mode is already stored in remote_details, so the
                // RPC receives no duplicated submission-type value.
                roleTemplateId = if (item.isShared) null else item.roleTemplateId,
                userId = if (item.isShared) null else item.userId,
                action = action
            )
        }

        // If every deliverable was already resolved in the database there is
        // nothing to write in the Submission stage.
        if (
            review.items.isEmpty() &&
            session.submissionDecisions.isEmpty() &&
            missingDecisions.isEmpty() &&
            !requiresExtension
        ) {
            updateRemoteReviewSession(
                session.copy(stage = PostManagementRemoteReviewStage.FEEDBACK)
            )
            _uiState.value = _uiState.value.copy(remoteReviewActionMessage = null)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingRemoteReview = true,
                remoteReviewActionMessage = null
            )

            try {
                repository.saveRemoteSubmissionReviewStage(
                    postId = currentPost.postId,
                    decisions = session.submissionDecisions,
                    missingDecisions = missingDecisions,
                    newEndDate = if (requiresExtension) session.newEndDate else null
                )

                val nextSession = if (requiresExtension) {
                    PostManagementRemoteReviewSession()
                } else {
                    session.copy(
                        stage = PostManagementRemoteReviewStage.FEEDBACK,
                        submissionDecisions = emptyList(),
                        missingActions = emptyMap(),
                        newEndDate = null,
                        touched = false
                    )
                }
                _uiState.value = _uiState.value.copy(remoteReviewSession = nextSession)

                val refreshedPost = repository.loadPost(currentPost.postId)
                cachedPost = refreshedPost
                applyTiming(
                    post = refreshedPost,
                    isUpdatingReview = _uiState.value.isUpdatingReview,
                    reviewActionMessage = _uiState.value.reviewActionMessage
                )
                val continuedWithoutWork = missingDecisions.any {
                    it.action == PostManagementRemoteMissingAction.CONTINUE_WITHOUT_WORK
                }
                _uiState.value = _uiState.value.copy(
                    isUpdatingRemoteReview = false,
                    remoteReviewActionMessage = when {
                        requiresExtension && continuedWithoutWork ->
                            "Deadline extended for Remote work kept open. Continue Without Submission/Revision outcomes were saved as Not Completed."
                        requiresExtension ->
                            "Deadline extended successfully. The Remote project is Ongoing again until the new deadline."
                        continuedWithoutWork ->
                            "Submission outcomes saved. Accepted work is Completed automatically; missing or rejected work is Not Completed."
                        else ->
                            "Submission outcomes saved. Accepted work is Completed automatically."
                    }
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Could not save Remote submission review stage.", exception)
                _uiState.value = _uiState.value.copy(
                    isUpdatingRemoteReview = false,
                    remoteReviewActionMessage = friendlyReviewError(exception)
                )
            }
        }
    }

    fun setRemoteFeedback(person: PostManagementPerson, feedback: String) {
        val review = _uiState.value.post?.remoteReview ?: return
        if (!review.canEdit) return
        if (!person.completionStatus.equals("COMPLETED", ignoreCase = true)) return

        val session = _uiState.value.remoteReviewSession
        val key = remoteReviewParticipationKey(person.roleTemplateId, person.userId)
        val clean = feedback.trim()
        val updated = session.feedbackByParticipation.toMutableMap().apply {
            if (clean.isBlank()) remove(key) else put(key, clean)
        }
        updateRemoteReviewSession(session.copy(feedbackByParticipation = updated, touched = false))
    }

    fun setRemoteReviewStage(stage: PostManagementRemoteReviewStage) {
        val session = _uiState.value.remoteReviewSession
        updateRemoteReviewSession(session.copy(stage = stage, touched = false))
        _uiState.value = _uiState.value.copy(remoteReviewActionMessage = null)
    }

    fun discardRemoteReviewSession() {
        updateRemoteReviewSession(PostManagementRemoteReviewSession())
    }

    fun discardReviewSessions() {
        _uiState.value = _uiState.value.copy(
            physicalReviewSession = PostManagementPhysicalReviewSession(),
            remoteReviewSession = PostManagementRemoteReviewSession()
        )
    }

    fun dismissRemoteReviewFinalizeSuccess() {
        _uiState.value = _uiState.value.copy(remoteReviewFinalizeSucceeded = false)
    }

    fun finalizeRemoteReviewPost() {
        val currentPost = cachedPost ?: return
        val review = _uiState.value.post?.remoteReview ?: return
        val session = _uiState.value.remoteReviewSession
        if (!review.canEdit || _uiState.value.isUpdatingRemoteReview) return

        val unresolved = review.participants.firstOrNull { person ->
            person.completionStatus.uppercase(Locale.US) in setOf("IN_PROGRESS", "NEEDS_REVIEW")
        }
        if (unresolved != null) {
            _uiState.value = _uiState.value.copy(
                remoteReviewSession = session.copy(stage = PostManagementRemoteReviewStage.SUBMISSION),
                remoteReviewActionMessage =
                    "Some Remote work is still unresolved. Return to Submission Review first."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingRemoteReview = true,
                remoteReviewActionMessage = null,
                remoteReviewFinalizeSucceeded = false
            )
            try {
                repository.finalizeRemoteReviewBatch(
                    postId = currentPost.postId,
                    feedbackByParticipation = session.feedbackByParticipation
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Could not finalize Remote review.", exception)
                _uiState.value = _uiState.value.copy(
                    isUpdatingRemoteReview = false,
                    remoteReviewActionMessage = friendlyReviewError(exception)
                )
                return@launch
            }

            val locallyCompletedPost = currentPost.copy(databaseStatus = "COMPLETED")
            cachedPost = locallyCompletedPost
            _uiState.value = _uiState.value.copy(
                remoteReviewSession = PostManagementRemoteReviewSession(),
                isUpdatingRemoteReview = false
            )
            applyTiming(locallyCompletedPost)
            _uiState.value = _uiState.value.copy(
                remoteReviewFinalizeSucceeded = true,
                remoteReviewActionMessage = "Remote project review finalized successfully."
            )

            try {
                val refreshedPost = repository.loadPost(currentPost.postId)
                cachedPost = refreshedPost
                applyTiming(refreshedPost)
                _uiState.value = _uiState.value.copy(
                    remoteReviewFinalizeSucceeded = true,
                    remoteReviewActionMessage = "Remote project review finalized successfully.",
                    remoteReviewSession = PostManagementRemoteReviewSession()
                )
            } catch (reloadException: Exception) {
                Log.w(TAG, "Remote review committed but reload failed.", reloadException)
            }
        }
    }

    private fun updateRemoteReviewSession(session: PostManagementRemoteReviewSession) {
        _uiState.value = _uiState.value.copy(remoteReviewSession = session)
    }

    private fun remoteExtensionRequired(
        review: PostManagementRemoteReview,
        session: PostManagementRemoteReviewSession
    ): Boolean {
        return session.submissionDecisions.any {
            it.decision == PostManagementRemoteSubmissionDecisionType.REQUEST_REVISION
        } || review.items.any { item ->
            session.missingActionFor(item.itemKey) == PostManagementRemoteMissingAction.GIVE_MORE_TIME
        }
    }

    private fun minimumRemoteExtensionDate(review: PostManagementRemoteReview): String {
        val parser = SimpleDateFormat(DATE_PATTERN, Locale.US).apply { isLenient = false }
        val current = runCatching { parser.parse(review.currentDeadline) }.getOrNull()
        val today = runCatching { parser.parse(review.todayDate) }.getOrNull()
        val base = listOfNotNull(current, today).maxByOrNull { it.time } ?: Date()
        return Calendar.getInstance().apply {
            time = base
            add(Calendar.DAY_OF_YEAR, 1)
        }.let { parser.format(it.time) }
    }

    private fun isValidRemoteExtensionDate(
        currentDeadline: String,
        todayDate: String,
        candidate: String?
    ): Boolean {
        val parser = SimpleDateFormat(DATE_PATTERN, Locale.US).apply { isLenient = false }
        val current = runCatching { parser.parse(currentDeadline) }.getOrNull() ?: return false
        val today = runCatching { parser.parse(todayDate) }.getOrNull() ?: return false
        val selected = runCatching { parser.parse(candidate.orEmpty()) }.getOrNull() ?: return false
        return selected.after(current) && selected.after(today)
    }

    fun toggleApplicantShortlist(person: PostManagementPerson) {
        if (!person.applicationStatus.equals("PENDING", ignoreCase = true)) return

        val currentPost = cachedPost ?: return
        val newValue = !person.isShortlisted

        viewModelScope.launch {
            try {
                repository.setApplicantShortlisted(
                    postId = currentPost.postId,
                    roleTemplateId = person.roleTemplateId,
                    userId = person.userId,
                    isShortlisted = newValue
                )

                val updatedPost = currentPost.copy(
                    people = currentPost.people.map { currentPerson ->
                        if (
                            currentPerson.userId == person.userId &&
                            currentPerson.roleTemplateId == person.roleTemplateId
                        ) {
                            currentPerson.copy(isShortlisted = newValue)
                        } else {
                            currentPerson
                        }
                    }
                )

                cachedPost = updatedPost
                applyTiming(updatedPost)
            } catch (exception: Exception) {
                Log.e(TAG, "Could not update applicant shortlist.", exception)
            }
        }
    }

    fun reviewApplicant(
        person: PostManagementPerson,
        decision: String,
        onSuccess: () -> Unit = {}
    ) {
        val post = cachedPost ?: return
        val normalizedDecision = decision.trim().uppercase(Locale.US)
        if (normalizedDecision !in setOf("ACCEPT", "DECLINE")) return
        if (!person.applicationStatus.equals("PENDING", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(
                applicantActionMessage = "This application is no longer pending."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingApplicant = true,
                applicantActionMessage = null
            )
            try {
                repository.reviewApplicant(
                    postId = post.postId,
                    roleTemplateId = person.roleTemplateId,
                    userId = person.userId,
                    decision = normalizedDecision
                )

                val refreshedPost = repository.loadPost(post.postId)
                cachedPost = refreshedPost
                applyTiming(refreshedPost)
                _uiState.value = _uiState.value.copy(
                    isUpdatingApplicant = false,
                    applicantActionMessage = null
                )
                onSuccess()
            } catch (exception: Exception) {
                Log.e(TAG, "Could not review applicant.", exception)
                _uiState.value = _uiState.value.copy(
                    isUpdatingApplicant = false,
                    applicantActionMessage = exception.message
                        ?.substringAfter("message=")
                        ?.substringBefore(",")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: exception.message
                        ?: "Unable to save this applicant decision."
                )
            }
        }
    }

    /**
     * Opens today's Physical attendance lazily. The SQL function is the final
     * authority for the current event date, live time window, ownership and
     * eligible volunteer count.
     */
    fun startPhysicalAttendance() {
        val currentPost = cachedPost ?: return
        val attendance = _uiState.value.post?.physicalAttendance ?: return
        if (!attendance.canStartAttendance || _uiState.value.isStartingAttendance) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isStartingAttendance = true,
                attendanceActionMessage = null
            )

            try {
                attendanceRefreshMutex.withLock {
                    // Keep the Android-side day aligned with the database-side AppClock
                    // before an RPC decides which attendance day to open.
                    AppClock.refreshFromDatabase()
                    repository.startPhysicalAttendance(currentPost.postId)
                    AppClock.refreshFromDatabase()
                    reloadAfterAttendanceAction(currentPost.postId)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Could not start Physical attendance.", exception)
                applyTiming(
                    post = currentPost,
                    isStartingAttendance = false,
                    isUpdatingAttendance = false,
                    attendanceActionMessage = exception.message
                        ?: "Unable to start attendance."
                )
            }
        }
    }

    /** Organisation may correct current or past valid Physical attendance. */
    fun markVolunteerPresent(
        person: PostManagementPerson,
        eventDate: String
    ) {
        val currentPost = cachedPost ?: return
        if (!person.applicationStatus.equals("ACCEPTED", ignoreCase = true)) return
        if (!person.roleMode.equals("PHYSICAL", ignoreCase = true)) return
        if (_uiState.value.isUpdatingAttendance) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingAttendance = true,
                attendanceActionMessage = null
            )

            try {
                attendanceRefreshMutex.withLock {
                    repository.markVolunteerPresent(
                        postId = currentPost.postId,
                        eventDate = eventDate,
                        roleTemplateId = person.roleTemplateId,
                        userId = person.userId
                    )
                    reloadAfterAttendanceAction(currentPost.postId)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Could not mark volunteer present.", exception)
                applyTiming(
                    post = currentPost,
                    isStartingAttendance = false,
                    isUpdatingAttendance = false,
                    attendanceActionMessage = exception.message
                        ?: "Unable to update attendance."
                )
            }
        }
    }

    /**
     * Persists an explicit ABSENT attendance decision. The confirmation dialog is
     * an organisation UI responsibility; SQL still validates ownership and date.
     */
    fun markVolunteerAbsent(
        person: PostManagementPerson,
        eventDate: String
    ) {
        val currentPost = cachedPost ?: return
        if (!person.applicationStatus.equals("ACCEPTED", ignoreCase = true)) return
        if (!person.roleMode.equals("PHYSICAL", ignoreCase = true)) return
        if (_uiState.value.isUpdatingAttendance) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingAttendance = true,
                attendanceActionMessage = null
            )

            try {
                attendanceRefreshMutex.withLock {
                    repository.markVolunteerAbsent(
                        postId = currentPost.postId,
                        eventDate = eventDate,
                        roleTemplateId = person.roleTemplateId,
                        userId = person.userId
                    )
                    reloadAfterAttendanceAction(currentPost.postId)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Could not mark volunteer absent.", exception)
                applyTiming(
                    post = currentPost,
                    isStartingAttendance = false,
                    isUpdatingAttendance = false,
                    attendanceActionMessage = exception.message
                        ?: "Unable to update attendance."
                )
            }
        }
    }

    /**
     * Physical review classification is now derived entirely from attendance and the
     * local review session. Entering Review must not mutate role_participations.
     */
    fun preparePhysicalReview() = Unit

    /** Selects every currently full-attendance Ready volunteer for completion locally. */
    fun completeAllReadyPhysical() {
        val review = _uiState.value.post?.physicalReview ?: return
        if (review.ready.isEmpty()) return

        val session = _uiState.value.physicalReviewSession
        val existingKeys = session.decisions
            .map { it.roleTemplateId to it.userId }
            .toSet()
        val selected: List<PostManagementPendingReviewDecision> = review.ready
            .filter { entry -> (entry.person.roleTemplateId to entry.person.userId) !in existingKeys }
            .map { entry ->
                PostManagementPendingReviewDecision(
                    roleTemplateId = entry.person.roleTemplateId,
                    userId = entry.person.userId,
                    decision = PostManagementPendingDecisionType.COMPLETED,
                    reason = null,
                    source = PostManagementPendingDecisionSource.FULL_ATTENDANCE
                )
            }
        if (selected.isEmpty()) return
        updateReviewSession(
            session.copy(
                decisions = session.decisions + selected,
                touched = false
            )
        )
    }

    /**
     * A reported work issue is already the organisation's Not Completed decision.
     * It is temporary until Finalize Event and can still be changed before then.
     */
    fun reportPhysicalReviewIssue(
        person: PostManagementPerson,
        reason: String
    ) {
        val cleanReason = reason.trim()
        if (cleanReason.isBlank()) return
        setPendingDecision(
            person = person,
            decision = PostManagementPendingDecisionType.NOT_COMPLETED,
            reason = cleanReason,
            source = PostManagementPendingDecisionSource.WORK_ISSUE
        )
    }

    /** Stores a temporary individual decision. Nothing is written to Supabase yet. */
    fun finalizePhysicalVolunteer(
        person: PostManagementPerson,
        completed: Boolean,
        note: String?
    ) {
        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
        if (!completed && cleanNote == null) return
        setPendingDecision(
            person = person,
            decision = if (completed) {
                PostManagementPendingDecisionType.COMPLETED
            } else {
                PostManagementPendingDecisionType.NOT_COMPLETED
            },
            reason = cleanNote,
            source = PostManagementPendingDecisionSource.PARTIAL_ATTENDANCE
        )
    }

    /** Removes a temporary choice and returns the volunteer to their natural review bucket. */
    fun changePhysicalReviewDecision(person: PostManagementPerson) {
        val session = _uiState.value.physicalReviewSession
        val updated = session.decisions.filterNot {
            it.roleTemplateId == person.roleTemplateId && it.userId == person.userId
        }
        updateReviewSession(
            session.copy(
                decisions = updated,
                feedbackByUserId = session.feedbackByUserId - person.userId,
                touched = false
            )
        )
    }

    /** Local feedback only. The database receives it together with final decisions. */
    fun savePhysicalFeedback(
        userIds: List<String>,
        feedback: String,
        previousFeedback: String?
    ) {
        val text = feedback.trim()
        if (userIds.isEmpty() || text.isBlank()) return
        val session = _uiState.value.physicalReviewSession
        var map = session.feedbackByUserId
        if (!previousFeedback.isNullOrBlank()) {
            map = map.filterValues { it != previousFeedback }.toMutableMap()
        }
        val mutable = map.toMutableMap()
        userIds.distinct().forEach { mutable[it] = text }
        updateReviewSession(
            session.copy(
                feedbackByUserId = mutable.toMap(),
                touched = false
            )
        )
    }

    /**
     * Tracks only uncommitted review-form text (for example a Not Completed reason
     * or a feedback message that has not been applied yet). Attendance is persisted
     * immediately and must never mark the review as discardable.
     */
    fun setPhysicalReviewDraftDirty(hasDraft: Boolean) {
        val session = _uiState.value.physicalReviewSession
        if (session.touched != hasDraft) {
            updateReviewSession(session.copy(touched = hasDraft))
        }
    }

    fun setPhysicalReviewStage(stage: PostManagementPhysicalReviewStage) {
        val session = _uiState.value.physicalReviewSession
        updateReviewSession(session.copy(stage = stage))
    }

    fun discardPhysicalReviewSession() {
        updateReviewSession(PostManagementPhysicalReviewSession())
    }

    /** Dismisses the one-time success confirmation shown after a committed review. */
    fun dismissPhysicalReviewFinalizeSuccess() {
        _uiState.value = _uiState.value.copy(reviewFinalizeSucceeded = false)
    }

    private fun setPendingDecision(
        person: PostManagementPerson,
        decision: PostManagementPendingDecisionType,
        reason: String?,
        source: PostManagementPendingDecisionSource
    ) {
        val session = _uiState.value.physicalReviewSession
        val withoutCurrent = session.decisions.filterNot {
            it.roleTemplateId == person.roleTemplateId && it.userId == person.userId
        }
        val pending = PostManagementPendingReviewDecision(
            roleTemplateId = person.roleTemplateId,
            userId = person.userId,
            decision = decision,
            reason = reason,
            source = source
        )
        updateReviewSession(
            session.copy(
                decisions = withoutCurrent + pending,
                feedbackByUserId = if (decision == PostManagementPendingDecisionType.NOT_COMPLETED) {
                    session.feedbackByUserId - person.userId
                } else {
                    session.feedbackByUserId
                },
                touched = false
            )
        )
    }

    private fun updateReviewSession(session: PostManagementPhysicalReviewSession) {
        _uiState.value = _uiState.value.copy(physicalReviewSession = session)
    }

    fun refresh() {
        val postId = loadedPostId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                attendanceActionMessage = null,
                reviewActionMessage = null,
                remoteReviewActionMessage = null
            )

            try {
                val post = repository.loadPost(postId)
                cachedPost = post
                applyTiming(post)
            } catch (exception: Exception) {
                Log.e(TAG, "Could not load post management data.", exception)
                _uiState.value = OrganisationPostManagementUiState(
                    isLoading = false,
                    errorMessage = exception.message
                        ?: "Unable to load this Volunteer Post."
                )
            }
        }
    }

    /**
     * Poll target: only the two normalized attendance tables are fetched.
     * Poll failures are logged but never replace a working screen with an error.
     */
    private suspend fun refreshAttendanceOnly(postId: String) {
        attendanceRefreshMutex.withLock {
            if (loadedPostId != postId) return@withLock

            val currentPost = cachedPost ?: return@withLock
            if (currentPost.postId != postId) return@withLock

            try {
                val attendance = repository.loadPhysicalAttendance(postId)
                val updatedPost = currentPost.copy(
                    attendanceDays = attendance.attendanceDays,
                    attendanceRecords = attendance.attendanceRecords
                )

                cachedPost = updatedPost
                applyTiming(updatedPost)
            } catch (exception: Exception) {
                Log.w(TAG, "Attendance-only polling refresh failed.", exception)
            }
        }
    }

    private suspend fun reloadAfterAttendanceAction(postId: String) {
        // Attendance corrections are already final database writes. Review grouping is
        // derived locally from attendance, so correcting attendance must not mutate
        // role_participations to NEEDS_REVIEW or any other completion status.
        val refreshedPost = repository.loadPost(postId)
        cachedPost = refreshedPost
        applyTiming(
            post = refreshedPost,
            isStartingAttendance = false,
            isUpdatingAttendance = false,
            attendanceActionMessage = null
        )
    }

    private suspend fun reloadAfterReviewAction(postId: String) {
        val refreshedPost = repository.loadPost(postId)
        cachedPost = refreshedPost
        applyTiming(
            post = refreshedPost,
            isStartingAttendance = false,
            isUpdatingAttendance = false,
            attendanceActionMessage = null,
            isUpdatingReview = false,
            reviewActionMessage = null
        )
    }


    /**
     * Final commit. All temporary decisions and feedback are sent in one RPC so
     * the database either finalizes the whole event or changes nothing.
     */
    fun finalizePhysicalReviewPost() {
        val currentPost = cachedPost ?: return
        val review = _uiState.value.post?.physicalReview ?: return
        val session = _uiState.value.physicalReviewSession
        if (!review.canEdit || _uiState.value.isUpdatingReview) return

        val unresolved = (review.ready + review.needsReview).distinctBy {
            it.person.roleTemplateId to it.person.userId
        }
        val allDecided = unresolved.all { entry ->
            session.decisionFor(entry.person.roleTemplateId, entry.person.userId) != null
        }
        if (!allDecided) {
            _uiState.value = _uiState.value.copy(
                reviewActionMessage = "Every volunteer needs a completion decision before finalizing.",
                reviewFinalizeSucceeded = false
            )
            return
        }

        val invalidNotCompletedDecision = session.decisions.firstOrNull { decision ->
            decision.decision == PostManagementPendingDecisionType.NOT_COMPLETED &&
                decision.reason.isNullOrBlank()
        }
        if (invalidNotCompletedDecision != null) {
            val volunteerName = unresolved.firstOrNull { entry ->
                entry.person.roleTemplateId == invalidNotCompletedDecision.roleTemplateId &&
                    entry.person.userId == invalidNotCompletedDecision.userId
            }?.person?.fullName

            _uiState.value = _uiState.value.copy(
                physicalReviewSession = session.copy(
                    stage = PostManagementPhysicalReviewStage.COMPLETION
                ),
                reviewActionMessage = if (volunteerName == null) {
                    "A Not Completed volunteer needs a reason before finalizing."
                } else {
                    "$volunteerName needs a reason for Not Completed before finalizing."
                },
                reviewFinalizeSucceeded = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingReview = true,
                reviewActionMessage = null,
                reviewFinalizeSucceeded = false
            )

            try {
                // One database transaction: either every decision/feedback item and the
                // post status are committed, or PostgreSQL rolls the whole call back.
                repository.finalizePhysicalReviewBatch(
                    postId = currentPost.postId,
                    decisions = session.decisions,
                    feedbackByUserId = session.feedbackByUserId
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Could not finalize Physical review.", exception)
                applyTiming(
                    post = currentPost,
                    isUpdatingReview = false,
                    reviewActionMessage = friendlyReviewError(exception)
                )
                return@launch
            }

            // The RPC returned successfully, so the database commit is complete. Move the
            // current screen to Completed immediately instead of leaving a stale Needs Review
            // state while the follow-up read is happening.
            val decisionByKey = session.decisions.associateBy {
                it.roleTemplateId to it.userId
            }
            val locallyCompletedPost = currentPost.copy(
                databaseStatus = "COMPLETED",
                people = currentPost.people.map { person ->
                    val decision = decisionByKey[person.roleTemplateId to person.userId]
                    if (decision == null) {
                        person
                    } else {
                        person.copy(
                            completionStatus = decision.decision.name
                        )
                    }
                }
            )

            cachedPost = locallyCompletedPost
            applyTiming(
                post = locallyCompletedPost,
                isStartingAttendance = false,
                isUpdatingAttendance = false,
                attendanceActionMessage = null,
                isUpdatingReview = false,
                reviewActionMessage = "Event review finalized successfully."
            )
            _uiState.value = _uiState.value.copy(reviewFinalizeSucceeded = true)

            // Refresh once from Supabase so certificates, evaluations, feedback and the
            // database COMPLETED status shown by the screen are the authoritative values.
            // A refresh failure must not turn a successful commit into a false failure.
            try {
                val refreshedPost = repository.loadPost(currentPost.postId)
                cachedPost = refreshedPost
                _uiState.value = _uiState.value.copy(
                    physicalReviewSession = PostManagementPhysicalReviewSession()
                )
                applyTiming(
                    post = refreshedPost,
                    isStartingAttendance = false,
                    isUpdatingAttendance = false,
                    attendanceActionMessage = null,
                    isUpdatingReview = false,
                    reviewActionMessage = "Event review finalized successfully."
                )
                _uiState.value = _uiState.value.copy(reviewFinalizeSucceeded = true)
            } catch (reloadException: Exception) {
                Log.w(
                    TAG,
                    "Physical review was committed but the completed post could not be reloaded yet.",
                    reloadException
                )
            }
        }
    }

    private fun observeAppClock() {
        viewModelScope.launch {
            AppClock.state.collect { clockState ->
                if (!clockState.isLoaded) return@collect
                cachedPost?.let(::applyTiming)
            }
        }
    }

    private fun applyTiming(
        post: PostManagementPost,
        isStartingAttendance: Boolean = _uiState.value.isStartingAttendance,
        isUpdatingAttendance: Boolean = _uiState.value.isUpdatingAttendance,
        attendanceActionMessage: String? = _uiState.value.attendanceActionMessage,
        isUpdatingReview: Boolean = _uiState.value.isUpdatingReview,
        reviewActionMessage: String? = _uiState.value.reviewActionMessage
    ) {
        val nowMillis = AppClock.nowMillis()
        val mode = PostMode.fromDatabaseValue(post.mode)

        val overall = mode?.let {
            PostTimingEvaluator.evaluatePostTiming(
                PostTimingInput(
                    mode = it,
                    physicalStartDate = post.physical?.startDate,
                    physicalEndDate = post.physical?.endDate,
                    remoteStartDate = post.remote?.startDate,
                    remoteEndDate = post.remote?.effectiveEndDate
                ),
                nowMillis
            )
        }

        val physicalTiming = post.physical?.let { physical ->
            PostTimingEvaluator.evaluatePostTiming(
                PostTimingInput(
                    mode = PostMode.PHYSICAL,
                    physicalStartDate = physical.startDate,
                    physicalEndDate = physical.endDate
                ),
                nowMillis
            )
        }

        val remoteTiming = post.remote?.let { remote ->
            PostTimingEvaluator.evaluatePostTiming(
                PostTimingInput(
                    mode = PostMode.REMOTE,
                    remoteStartDate = remote.startDate,
                    remoteEndDate = remote.effectiveEndDate
                ),
                nowMillis
            )
        }

        val rolesWithApplicationWindows = post.roles.map { role ->
            val applicationWindow = RoleApplicationWindowEvaluator.evaluate(
                input = RoleApplicationWindowInput(
                    roleMode = role.roleMode,
                    postStatus = post.databaseStatus,
                    physicalStartDate = post.physical?.startDate,
                    remoteStartDate = post.remote?.startDate
                ),
                nowMillis = nowMillis
            )

            role.copy(
                applicationWindowState = applicationWindow.state,
                applicationCutoffDate = applicationWindow.cutoffDate,
                applicationCutoffReason = applicationWindow.cutoffReason
            )
        }

        val timedPost = post.copy(
            timingState = overall,
            physicalTimingState = physicalTiming,
            remoteTimingState = remoteTiming,
            roles = rolesWithApplicationWindows
        )

        val physicalAttendance = buildPhysicalAttendance(
            post = timedPost,
            nowMillis = nowMillis
        )
        val physicalReview = buildPhysicalReview(
            post = timedPost,
            attendance = physicalAttendance
        )
        val remoteReview = buildRemoteReview(
            post = timedPost,
            nowMillis = nowMillis
        )

        _uiState.value = OrganisationPostManagementUiState(
            isLoading = false,
            post = timedPost.copy(
                physicalAttendance = physicalAttendance,
                physicalReview = physicalReview,
                remoteReview = remoteReview
            ),
            isStartingAttendance = isStartingAttendance,
            isUpdatingAttendance = isUpdatingAttendance,
            attendanceActionMessage = attendanceActionMessage,
            isUpdatingReview = isUpdatingReview,
            reviewActionMessage = reviewActionMessage,
            reviewFinalizeSucceeded = _uiState.value.reviewFinalizeSucceeded,
            physicalReviewSession = _uiState.value.physicalReviewSession,
            isUpdatingRemoteReview = _uiState.value.isUpdatingRemoteReview,
            remoteReviewActionMessage = _uiState.value.remoteReviewActionMessage,
            remoteReviewFinalizeSucceeded = _uiState.value.remoteReviewFinalizeSucceeded,
            remoteReviewSession = _uiState.value.remoteReviewSession,
            isUpdatingApplicant = _uiState.value.isUpdatingApplicant,
            applicantActionMessage = _uiState.value.applicantActionMessage
        )
    }

    private fun buildRemoteReview(
        post: PostManagementPost,
        nowMillis: Long
    ): PostManagementRemoteReview? {
        val remote = post.remote ?: return null
        if (!post.mode.equals("REMOTE", ignoreCase = true)) return null
        if (post.databaseStatus.uppercase(Locale.US) !in setOf("PUBLISHED", "CLOSED", "COMPLETED")) {
            return null
        }
        if (
            post.remoteTimingState != PostTimingState.PAST &&
            !post.databaseStatus.equals("COMPLETED", ignoreCase = true)
        ) return null

        val participants = post.volunteers
            .filter { person -> person.roleMode.equals("REMOTE", ignoreCase = true) }
            .sortedWith(compareBy<PostManagementPerson> { it.roleName }.thenBy { it.fullName })

        val unresolvedParticipants = participants.filter { person ->
            person.completionStatus.uppercase(Locale.US) in setOf("IN_PROGRESS", "NEEDS_REVIEW")
        }

        fun latest(rows: List<PostManagementRemoteSubmission>): PostManagementRemoteSubmission? =
            rows.maxWithOrNull(
                compareBy<PostManagementRemoteSubmission> { it.submittedAt.orEmpty() }
                    .thenBy { it.submissionId }
            )

        fun isResubmission(submission: PostManagementRemoteSubmission): Boolean {
            if (!submission.status.equals("PENDING_REVIEW", ignoreCase = true)) return false
            return post.remoteSubmissions.any { previous ->
                previous.submissionId != submission.submissionId &&
                    previous.status.equals("REVISION_REQUESTED", ignoreCase = true) &&
                    previous.submissionType.equals(submission.submissionType, ignoreCase = true) &&
                    when {
                        submission.submissionType.equals("SHARED", ignoreCase = true) -> true
                        submission.submissionType.equals("INDIVIDUAL", ignoreCase = true) ->
                            previous.roleTemplateId == submission.roleTemplateId &&
                                previous.userId == submission.userId
                        else -> false
                    }
            }
        }

        val items = if (remote.submissionMode.equals("SHARED_TEAM", ignoreCase = true)) {
            if (unresolvedParticipants.isEmpty()) {
                emptyList()
            } else {
                val shared = latest(
                    post.remoteSubmissions.filter {
                        it.submissionType.equals("SHARED", ignoreCase = true)
                    }
                )
                val responsibleRole = post.roles.firstOrNull {
                    it.roleTemplateId == remote.responsibleRoleTemplateId
                }
                listOf(
                    PostManagementRemoteReviewItem(
                        itemKey = "SHARED",
                        submissionType = "SHARED",
                        roleTemplateId = remote.responsibleRoleTemplateId,
                        userId = null,
                        person = null,
                        roleName = responsibleRole?.roleName ?: "Submitting role",
                        requirement = remote.sharedDeliverable,
                        latestSubmission = shared,
                        submittedByName = shared?.userId?.let { submitterId ->
                            participants.firstOrNull { it.userId == submitterId }?.fullName
                        },
                        isResubmission = shared?.let(::isResubmission) == true
                    )
                )
            }
        } else {
            unresolvedParticipants.map { person ->
                val role = post.roles.firstOrNull { it.roleTemplateId == person.roleTemplateId }
                val individual = latest(
                    post.remoteSubmissions.filter { submission ->
                        submission.submissionType.equals("INDIVIDUAL", ignoreCase = true) &&
                            submission.roleTemplateId == person.roleTemplateId &&
                            submission.userId == person.userId
                    }
                )
                PostManagementRemoteReviewItem(
                    itemKey = remoteReviewParticipationKey(person.roleTemplateId, person.userId),
                    submissionType = "INDIVIDUAL",
                    roleTemplateId = person.roleTemplateId,
                    userId = person.userId,
                    person = person,
                    roleName = person.roleName,
                    requirement = role?.individualSubmissionRequirement,
                    latestSubmission = individual,
                    submittedByName = person.fullName,
                    isResubmission = individual?.let(::isResubmission) == true
                )
            }
        }

        return PostManagementRemoteReview(
            todayDate = SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date(nowMillis)),
            currentDeadline = remote.effectiveEndDate,
            submissionMode = remote.submissionMode,
            items = items,
            participants = participants,
            canEdit = !post.databaseStatus.equals("COMPLETED", ignoreCase = true) &&
                !post.databaseStatus.equals("CANCELLED", ignoreCase = true)
        )
    }

    private fun buildPhysicalAttendance(
        post: PostManagementPost,
        nowMillis: Long
    ): PostManagementPhysicalAttendance? {
        val physical = post.physical ?: return null
        val today = formatDateForPhysicalPost(
            nowMillis = nowMillis,
            timeZoneId = physical.timeZone
        )

        val acceptedPhysicalPeople = post.volunteers.filter { person ->
            person.roleMode.equals("PHYSICAL", ignoreCase = true)
        }
        val todaySession = post.attendanceDays.firstOrNull {
            it.eventDate == today && it.isActive
        }

        val allPhysicalDates = calendarDatesInclusive(
            startDate = physical.startDate,
            endDate = physical.endDate,
            timeZoneId = physical.timeZone
        )

        val summaries = acceptedPhysicalPeople.map { person ->
            val scheduledRoleDates = post.schedules
                .filter { schedule ->
                    schedule.scheduleType.equals("PHYSICAL", ignoreCase = true) &&
                        person.roleTemplateId in schedule.roleTemplateIds
                }
                .map { it.scheduleDate }
                .distinct()
                .sorted()

            // Step 4 is optional. If the organisation did not build a timetable
            // for this role, the Physical event date range remains the fallback.
            val expectedDates = scheduledRoleDates.ifEmpty { allPhysicalDates }
                .toSet()

            val recordsByDate = post.attendanceRecords
                .filter { record ->
                    record.userId == person.userId &&
                        record.roleTemplateId == person.roleTemplateId &&
                        record.eventDate in expectedDates
                }
                .associateBy { it.eventDate }

            val presentRecordsByDate = recordsByDate.filterValues { record ->
                record.attendanceStatus.equals("PRESENT", ignoreCase = true)
            }

            val dateStatuses = allPhysicalDates.map { eventDate ->
                val record = recordsByDate[eventDate]
                val isPresent = record?.attendanceStatus
                    ?.equals("PRESENT", ignoreCase = true) == true
                val isMarkedAbsent = record?.attendanceStatus
                    ?.equals("ABSENT", ignoreCase = true) == true

                // A missing row is only "pending" while that attendance date can still happen.
                // Once the expected date has passed, VolunteerLink treats no check-in as
                // Absent · 0h without manufacturing an ABSENT database row.
                val isInferredAbsent = record == null &&
                    eventDate in expectedDates &&
                    (post.physicalTimingState == PostTimingState.PAST || eventDate < today)

                PostManagementVolunteerAttendanceDateStatus(
                    eventDate = eventDate,
                    expected = eventDate in expectedDates,
                    present = isPresent,
                    markedAbsent = isMarkedAbsent,
                    inferredAbsent = isInferredAbsent,
                    checkedInAt = if (isPresent) record?.checkedInAt else null,
                    verifiedMinutes = if (isPresent) record?.verifiedMinutes ?: 0 else 0
                )
            }

            PostManagementVolunteerAttendanceSummary(
                userId = person.userId,
                roleTemplateId = person.roleTemplateId,
                attendedDays = presentRecordsByDate.keys.size,
                expectedDays = expectedDates.size,
                verifiedMinutes = presentRecordsByDate.values.sumOf { it.verifiedMinutes },
                expectedToday = today in expectedDates,
                checkedInToday = presentRecordsByDate.containsKey(today),
                dateStatuses = dateStatuses
            )
        }

        val todayEligibleSummaries = summaries.filter { it.expectedToday }
        val eligiblePhysicalVolunteerCount = todayEligibleSummaries
            .map { it.userId }
            .distinct()
            .size
        val checkedInTodayCount = todayEligibleSummaries
            .filter { it.checkedInToday }
            .map { it.userId }
            .distinct()
            .size

        val statusAllowsAttendance = post.databaseStatus.uppercase(Locale.US) in
            setOf("PUBLISHED", "CLOSED")
        val physicalIsOngoing = post.physicalTimingState == PostTimingState.ONGOING
        val liveWindow = evaluateLiveWindow(
            nowMillis = nowMillis,
            today = today,
            startTime = physical.startTime,
            endTime = physical.endTime,
            timeZoneId = physical.timeZone
        )

        val blockedReason = when {
            !physicalIsOngoing -> "Physical attendance is only available while the event is ongoing."
            !statusAllowsAttendance -> "Attendance cannot be started for this post status."
            !liveWindow.isOpen -> liveWindow.message
            eligiblePhysicalVolunteerCount == 0 ->
                "No Physical volunteers are scheduled for today."
            else -> null
        }

        val availableDates = allPhysicalDates.filter { eventDate ->
            eventDate <= today
        }
        val defaultSelectedDate = when {
            today in availableDates -> today
            else -> availableDates.lastOrNull()
        }

        return PostManagementPhysicalAttendance(
            todayDate = today,
            todaySession = todaySession,
            eligiblePhysicalVolunteerCount = eligiblePhysicalVolunteerCount,
            checkedInTodayCount = checkedInTodayCount,
            canStartAttendance = todaySession == null && blockedReason == null,
            canCorrectAttendance = statusAllowsAttendance,
            isLiveWindowOpen = liveWindow.isOpen,
            attendanceWindowLabel = "${physical.startTime.toDisplayTime()} – ${physical.endTime.toDisplayTime()}",
            startBlockedReason = blockedReason,
            availableDates = availableDates,
            defaultSelectedDate = defaultSelectedDate,
            volunteerSummaries = summaries
        )
    }

    private fun buildPhysicalReview(
        post: PostManagementPost,
        attendance: PostManagementPhysicalAttendance?
    ): PostManagementPhysicalReview? {
        if (!post.mode.equals("PHYSICAL", ignoreCase = true)) return null
        if (post.physical == null || attendance == null) return null
        if (post.databaseStatus.uppercase(Locale.US) !in setOf("PUBLISHED", "CLOSED", "COMPLETED")) {
            return null
        }
        if (
            post.physicalTimingState != PostTimingState.PAST &&
            !post.databaseStatus.equals("COMPLETED", ignoreCase = true)
        ) return null

        val evaluationByParticipation = post.evaluations.associateBy { evaluation ->
            evaluation.roleTemplateId to evaluation.userId
        }

        val entries = post.volunteers
            .filter { it.roleMode.equals("PHYSICAL", ignoreCase = true) }
            .mapNotNull { person ->
                val summary = attendance.summaryFor(person) ?: return@mapNotNull null
                val expectedStatuses = summary.dateStatuses.filter { it.expected }
                val absentDays = expectedStatuses.count {
                    it.markedAbsent || it.inferredAbsent
                }
                val missingCheckInDays = expectedStatuses.count { it.inferredAbsent }
                val rawIssue = person.decisionNote.orEmpty()
                val hasPerformanceIssue = rawIssue.startsWith(
                    PERFORMANCE_ISSUE_PREFIX,
                    ignoreCase = true
                )
                val evaluation = evaluationByParticipation[
                    person.roleTemplateId to person.userId
                ]

                PostManagementPhysicalReviewEntry(
                    person = person,
                    attendanceSummary = summary,
                    absentDays = absentDays,
                    missingCheckInDays = missingCheckInDays,
                    hasPerformanceIssue = hasPerformanceIssue,
                    performanceIssueText = rawIssue
                        .takeIf { hasPerformanceIssue }
                        ?.substringAfter(":", "")
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() },
                    feedback = evaluation?.feedback,
                    completionReason = evaluation?.completionReason,
                    verifiedMinutes = evaluation?.verifiedMinutes
                )
            }

        val completed = entries.filter {
            it.person.completionStatus.equals("COMPLETED", ignoreCase = true)
        }
        val notCompleted = entries.filter {
            it.person.completionStatus.equals("NOT_COMPLETED", ignoreCase = true)
        }
        val unresolved = entries.filterNot { it.isFinalized }

        // Attendance is evidence, not an automatic completion decision.
        // Full attendance only makes a volunteer Ready for the organisation's
        // one-tap batch confirmation. Any exception stays in Needs Review.
        val needsReview = unresolved.filter { entry ->
            entry.absentDays > 0 ||
                entry.hasPerformanceIssue ||
                entry.person.completionStatus.equals("NEEDS_REVIEW", ignoreCase = true)
        }
        val needsReviewKeys = needsReview.map {
            it.person.roleTemplateId to it.person.userId
        }.toSet()
        val ready = unresolved.filter { entry ->
            (entry.person.roleTemplateId to entry.person.userId) !in needsReviewKeys
        }

        val feedbackGroups = completed
            .filter { !it.feedback.isNullOrBlank() }
            .groupBy { it.feedback!!.trim() }
            .map { (feedbackText, groupEntries) ->
                PostManagementFeedbackGroup(
                    feedback = feedbackText,
                    userIds = groupEntries.map { it.person.userId }.distinct(),
                    recipientNames = groupEntries.map { it.person.fullName }.distinct()
                )
            }
            .sortedByDescending { it.recipientCount }

        return PostManagementPhysicalReview(
            ready = ready.sortedBy { it.person.fullName },
            needsReview = needsReview.sortedWith(
                compareByDescending<PostManagementPhysicalReviewEntry> { it.missingCheckInDays }
                    .thenByDescending { it.absentDays }
                    .thenBy { it.person.fullName }
            ),
            completed = completed.sortedBy { it.person.fullName },
            notCompleted = notCompleted.sortedBy { it.person.fullName },
            feedbackGroups = feedbackGroups,
            completedWithoutFeedback = completed
                .filterNot { it.hasFeedback }
                .sortedBy { it.person.fullName },
            canEdit = !post.databaseStatus.equals("CANCELLED", ignoreCase = true) &&
                !post.databaseStatus.equals("COMPLETED", ignoreCase = true)
        )
    }

    /** Keeps PostgREST request metadata out of user-facing review errors. */
    private fun friendlyReviewError(exception: Exception): String {
        val raw = exception.message?.trim().orEmpty()
        if (raw.isBlank()) return "Unable to complete this review."

        return raw
            .substringBefore("\nCode:")
            .substringBefore("\r\nCode:")
            .trim()
            .ifBlank { "Unable to complete this review." }
    }

    private fun evaluateLiveWindow(
        nowMillis: Long,
        today: String,
        startTime: String,
        endTime: String,
        timeZoneId: String?
    ): LiveWindowResult {
        val timeZone = resolveTimeZone(timeZoneId)
        val parser = SimpleDateFormat("$DATE_PATTERN HH:mm:ss", Locale.US).apply {
            isLenient = false
            this.timeZone = timeZone
        }

        fun normalizeTime(value: String): String {
            val parts = value.split(":")
            return when (parts.size) {
                2 -> "$value:00"
                else -> value
            }
        }

        val start = runCatching {
            parser.parse("$today ${normalizeTime(startTime)}")
        }.getOrNull()
        val end = runCatching {
            parser.parse("$today ${normalizeTime(endTime)}")
        }.getOrNull()

        if (start == null || end == null || end.before(start)) {
            return LiveWindowResult(
                isOpen = false,
                message = "Attendance time is not configured correctly."
            )
        }

        return when {
            nowMillis < start.time -> LiveWindowResult(
                isOpen = false,
                message = "Attendance opens at ${startTime.toDisplayTime()}."
            )
            nowMillis > end.time -> LiveWindowResult(
                isOpen = false,
                message = "Attendance closed at ${endTime.toDisplayTime()}."
            )
            else -> LiveWindowResult(isOpen = true, message = "")
        }
    }

    private fun formatDateForPhysicalPost(
        nowMillis: Long,
        timeZoneId: String?
    ): String {
        val timeZone = resolveTimeZone(timeZoneId)

        return SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            isLenient = false
            this.timeZone = timeZone
        }.format(Date(nowMillis))
    }

    private fun calendarDatesInclusive(
        startDate: String,
        endDate: String,
        timeZoneId: String?
    ): List<String> {
        val timeZone = resolveTimeZone(timeZoneId)
        val parser = SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            isLenient = false
            this.timeZone = timeZone
        }
        val start = runCatching { parser.parse(startDate) }.getOrNull() ?: return emptyList()
        val end = runCatching { parser.parse(endDate) }.getOrNull() ?: return emptyList()
        if (start.after(end)) return emptyList()

        val calendar = Calendar.getInstance(timeZone).apply { time = start }
        val endCalendar = Calendar.getInstance(timeZone).apply { time = end }
        val dates = mutableListOf<String>()

        while (!calendar.after(endCalendar)) {
            dates += parser.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return dates
    }

    private fun resolveTimeZone(timeZoneId: String?): TimeZone {
        return timeZoneId
            ?.takeIf { it.isNotBlank() }
            ?.let { TimeZone.getTimeZone(it) }
            ?: TimeZone.getDefault()
    }

    private fun String.toDisplayTime(): String {
        val parts = split(":")
        if (parts.size < 2) return this
        val hour = parts[0].toIntOrNull() ?: return this
        val minute = parts[1].take(2)
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val suffix = if (hour >= 12) "PM" else "AM"
        return "$displayHour:$minute $suffix"
    }

    private data class LiveWindowResult(
        val isOpen: Boolean,
        val message: String
    )

    companion object {
        private const val TAG = "OrgPostManagementVM"
        private const val DATE_PATTERN = "yyyy-MM-dd"
        private const val ATTENDANCE_POLL_INTERVAL_MS = 2_000L
        private const val PERFORMANCE_ISSUE_PREFIX = "Performance issue:"
    }
}
