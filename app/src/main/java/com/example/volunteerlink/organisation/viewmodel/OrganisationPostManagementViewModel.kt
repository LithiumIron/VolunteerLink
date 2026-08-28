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
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPhysicalAttendance
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
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

    fun refresh() {
        val postId = loadedPostId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                attendanceActionMessage = null
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
        val refreshedPost = repository.loadPost(postId)
        cachedPost = refreshedPost
        applyTiming(
            post = refreshedPost,
            isStartingAttendance = false,
            isUpdatingAttendance = false,
            attendanceActionMessage = null
        )
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
        attendanceActionMessage: String? = _uiState.value.attendanceActionMessage
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
                    remoteEndDate = post.remote?.endDate
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
                    remoteEndDate = remote.endDate
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

        _uiState.value = OrganisationPostManagementUiState(
            isLoading = false,
            post = timedPost.copy(physicalAttendance = physicalAttendance),
            isStartingAttendance = isStartingAttendance,
            isUpdatingAttendance = isUpdatingAttendance,
            attendanceActionMessage = attendanceActionMessage
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

                PostManagementVolunteerAttendanceDateStatus(
                    eventDate = eventDate,
                    expected = eventDate in expectedDates,
                    present = isPresent,
                    markedAbsent = isMarkedAbsent,
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
    }
}
