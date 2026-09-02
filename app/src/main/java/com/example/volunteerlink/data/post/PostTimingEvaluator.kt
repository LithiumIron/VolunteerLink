package com.example.volunteerlink.data.post

import com.example.volunteerlink.data.time.AppClock
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Shared date/time rules for VolunteerLink posts.
 *
 * Database status (DRAFT, PUBLISHED, CLOSED, COMPLETED, CANCELLED) is kept
 * separate from these derived timing states. This object only answers things
 * that depend on what VolunteerLink currently considers to be "now".
 */
object PostTimingEvaluator {

    private const val MIN_PUBLISH_LEAD_DAYS = 7

    /**
     * Calculates whether an opportunity is Upcoming, Ongoing or Past.
     *
     * Physical and Remote periods are both inclusive of their end date.
     * For Hybrid posts:
     * - either side ongoing -> ONGOING
     * - otherwise, either side still future -> UPCOMING
     * - otherwise -> PAST
     */
    fun evaluatePostTiming(
        input: PostTimingInput,
        nowMillis: Long = AppClock.nowMillis()
    ): PostTimingState {
        return when (input.mode) {
            PostMode.PHYSICAL -> evaluatePeriod(
                startDate = input.physicalStartDate,
                endDate = input.physicalEndDate,
                nowMillis = nowMillis
            )

            PostMode.REMOTE -> evaluatePeriod(
                startDate = input.remoteStartDate,
                endDate = input.remoteEndDate,
                nowMillis = nowMillis
            )

            PostMode.HYBRID -> {
                val physical = evaluatePeriodOrNull(
                    startDate = input.physicalStartDate,
                    endDate = input.physicalEndDate,
                    nowMillis = nowMillis
                )
                val remote = evaluatePeriodOrNull(
                    startDate = input.remoteStartDate,
                    endDate = input.remoteEndDate,
                    nowMillis = nowMillis
                )

                val availableStates = listOfNotNull(physical, remote)
                when {
                    PostTimingState.ONGOING in availableStates -> PostTimingState.ONGOING
                    PostTimingState.UPCOMING in availableStates -> PostTimingState.UPCOMING
                    else -> PostTimingState.PAST
                }
            }
        }
    }

    /**
     * Checks whether a saved Draft still satisfies the 7-day publishing rule.
     *
     * The Draft is never deleted or changed here. This only returns an attention
     * result that Home/Manage/Publish can display or enforce.
     */
    fun evaluateDraftAttention(
        input: PostTimingInput,
        nowMillis: Long = AppClock.nowMillis()
    ): DraftAttention {
        val startDate = earliestStartDate(input)
            ?: return DraftAttention.none()

        val startDay = parseDateAtStartOfDay(startDate)
            ?: return DraftAttention.none()
        val today = startOfDay(nowMillis)
        val daysUntilStart = daysBetween(today, startDay)
        val earliestPublishable = addDays(today, MIN_PUBLISH_LEAD_DAYS)

        return when {
            daysUntilStart < 0 -> DraftAttention(
                type = DraftAttentionType.START_DATE_PASSED,
                startDate = startDate,
                earliestPublishableDate = formatDate(earliestPublishable),
                daysUntilStart = daysUntilStart
            )

            daysUntilStart < MIN_PUBLISH_LEAD_DAYS -> DraftAttention(
                type = DraftAttentionType.START_TOO_SOON,
                startDate = startDate,
                earliestPublishableDate = formatDate(earliestPublishable),
                daysUntilStart = daysUntilStart
            )

            else -> DraftAttention.none(
                startDate = startDate,
                daysUntilStart = daysUntilStart
            )
        }
    }

    /** Earliest actual volunteering start for the post mode. */
    fun earliestStartDate(input: PostTimingInput): String? {
        return when (input.mode) {
            PostMode.PHYSICAL -> input.physicalStartDate
            PostMode.REMOTE -> input.remoteStartDate
            PostMode.HYBRID -> earliestDate(
                input.physicalStartDate,
                input.remoteStartDate
            )
        }
    }

    private fun evaluatePeriod(
        startDate: String?,
        endDate: String?,
        nowMillis: Long
    ): PostTimingState {
        return evaluatePeriodOrNull(startDate, endDate, nowMillis)
            ?: PostTimingState.PAST
    }

    private fun evaluatePeriodOrNull(
        startDate: String?,
        endDate: String?,
        nowMillis: Long
    ): PostTimingState? {
        val start = startDate?.let(::parseDateAtStartOfDay) ?: return null
        val end = endDate?.let(::parseDateAtStartOfDay) ?: return null
        val today = startOfDay(nowMillis)

        return when {
            today < start -> PostTimingState.UPCOMING
            today > end -> PostTimingState.PAST
            else -> PostTimingState.ONGOING
        }
    }

    private fun earliestDate(first: String?, second: String?): String? {
        val firstMillis = first?.let(::parseDateAtStartOfDay)
        val secondMillis = second?.let(::parseDateAtStartOfDay)

        return when {
            firstMillis == null -> second
            secondMillis == null -> first
            firstMillis <= secondMillis -> first
            else -> second
        }
    }

    private fun parseDateAtStartOfDay(value: String): Long? {
        val parsed = runCatching {
            dateFormat().parse(value.trim())
        }.getOrNull() ?: return null

        return startOfDay(parsed.time)
    }

    private fun startOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun addDays(startDayMillis: Long, days: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startDayMillis
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
    }

    private fun daysBetween(startDayMillis: Long, endDayMillis: Long): Int {
        // Calendar dates are used instead of raw millisecond division so the
        // result remains correct across daylight-saving changes on other devices.
        val start = Calendar.getInstance().apply { timeInMillis = startDayMillis }
        val end = Calendar.getInstance().apply { timeInMillis = endDayMillis }
        var days = 0

        if (startDayMillis <= endDayMillis) {
            while (
                start.get(Calendar.YEAR) != end.get(Calendar.YEAR) ||
                start.get(Calendar.DAY_OF_YEAR) != end.get(Calendar.DAY_OF_YEAR)
            ) {
                start.add(Calendar.DAY_OF_YEAR, 1)
                days++
            }
            return days
        }

        while (
            start.get(Calendar.YEAR) != end.get(Calendar.YEAR) ||
            start.get(Calendar.DAY_OF_YEAR) != end.get(Calendar.DAY_OF_YEAR)
        ) {
            start.add(Calendar.DAY_OF_YEAR, -1)
            days--
        }
        return days
    }

    private fun formatDate(timeMillis: Long): String {
        return dateFormat().format(Date(timeMillis))
    }

    private fun dateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
    }
}

enum class PostMode {
    PHYSICAL,
    REMOTE,
    HYBRID;

    companion object {
        fun fromDatabaseValue(value: String): PostMode? {
            return entries.firstOrNull {
                it.name.equals(value.trim(), ignoreCase = true)
            }
        }
    }
}

@Serializable
enum class PostTimingState {
    UPCOMING,
    ONGOING,
    PAST
}

/** Only the date fields needed for timing calculations. */
data class PostTimingInput(
    val mode: PostMode,
    val physicalStartDate: String? = null,
    val physicalEndDate: String? = null,
    val remoteStartDate: String? = null,
    val remoteEndDate: String? = null
)

enum class DraftAttentionType {
    NONE,
    START_TOO_SOON,
    START_DATE_PASSED
}

data class DraftAttention(
    val type: DraftAttentionType,
    val startDate: String? = null,
    val earliestPublishableDate: String? = null,
    val daysUntilStart: Int? = null
) {
    val needsAttention: Boolean
        get() = type != DraftAttentionType.NONE

    companion object {
        fun none(
            startDate: String? = null,
            daysUntilStart: Int? = null
        ) = DraftAttention(
            type = DraftAttentionType.NONE,
            startDate = startDate,
            daysUntilStart = daysUntilStart
        )
    }
}
