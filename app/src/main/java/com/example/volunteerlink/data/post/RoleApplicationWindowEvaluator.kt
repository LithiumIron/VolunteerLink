package com.example.volunteerlink.data.post

import com.example.volunteerlink.data.time.AppClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Shared role-level application cutoff rules.
 *
 * VolunteerLink closes applications at the START OF the cutoff DATE, not at an
 * exact event/training clock time. Therefore the day before the cutoff date is
 * the final day for joining/reviewing applications.
 *
 * Hybrid posts are evaluated per role:
 * - PHYSICAL role -> Physical volunteering start date
 * - REMOTE role   -> Remote volunteering start date
 *
 * A schedule linked through schedule_item_roles with
 * closes_applications_on_start = true may close that role earlier. Only the
 * schedule date matters for V1; its start_time is deliberately ignored.
 */
object RoleApplicationWindowEvaluator {

    fun evaluate(
        input: RoleApplicationWindowInput,
        nowMillis: Long = AppClock.nowMillis()
    ): RoleApplicationWindow {
        if (
            !input.postStatus.isNullOrBlank() &&
            !input.postStatus.equals("PUBLISHED", ignoreCase = true)
        ) {
            return RoleApplicationWindow(
                state = RoleApplicationWindowState.CLOSED,
                cutoffDate = null,
                cutoffReason = RoleApplicationCutoffReason.POST_STATUS
            )
        }

        val roleStartDate = when (input.roleMode.trim().uppercase(Locale.US)) {
            "PHYSICAL" -> input.physicalStartDate
            "REMOTE" -> input.remoteStartDate
            else -> null
        }

        val validRoleStart = roleStartDate?.takeIf(::isValidDate)
        val validScheduleDates = input.applicationClosingScheduleDates
            .map(String::trim)
            .filter(::isValidDate)

        val earliestScheduleDate = validScheduleDates.minByOrNull {
            parseDateAtStartOfDay(it) ?: Long.MAX_VALUE
        }

        val cutoffCandidates = buildList {
            validRoleStart?.let {
                add(
                    CutoffCandidate(
                        date = it,
                        reason = RoleApplicationCutoffReason.ROLE_START
                    )
                )
            }
            earliestScheduleDate?.let {
                add(
                    CutoffCandidate(
                        date = it,
                        reason = RoleApplicationCutoffReason.SCHEDULE_START
                    )
                )
            }
        }

        val effectiveCutoff = cutoffCandidates.minByOrNull {
            parseDateAtStartOfDay(it.date) ?: Long.MAX_VALUE
        }

        // If old/incomplete test data has no usable cutoff date, do not
        // silently hide legitimate applications. Treat the role as open until
        // a valid start date is available.
        if (effectiveCutoff == null) {
            return RoleApplicationWindow(
                state = RoleApplicationWindowState.OPEN,
                cutoffDate = null,
                cutoffReason = null
            )
        }

        val today = startOfDay(nowMillis)
        val cutoffDay = parseDateAtStartOfDay(effectiveCutoff.date)
            ?: return RoleApplicationWindow(
                state = RoleApplicationWindowState.OPEN,
                cutoffDate = null,
                cutoffReason = null
            )

        return RoleApplicationWindow(
            state = if (today < cutoffDay) {
                RoleApplicationWindowState.OPEN
            } else {
                RoleApplicationWindowState.CLOSED
            },
            cutoffDate = effectiveCutoff.date,
            cutoffReason = effectiveCutoff.reason
        )
    }

    private fun isValidDate(value: String): Boolean {
        return parseDateAtStartOfDay(value) != null
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

    private fun dateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
    }

    private data class CutoffCandidate(
        val date: String,
        val reason: RoleApplicationCutoffReason
    )
}

data class RoleApplicationWindowInput(
    val roleMode: String,
    val postStatus: String? = null,
    val physicalStartDate: String? = null,
    val remoteStartDate: String? = null,
    val applicationClosingScheduleDates: List<String> = emptyList()
)

enum class RoleApplicationWindowState {
    OPEN,
    CLOSED
}

enum class RoleApplicationCutoffReason {
    ROLE_START,
    SCHEDULE_START,
    POST_STATUS
}

data class RoleApplicationWindow(
    val state: RoleApplicationWindowState,
    val cutoffDate: String?,
    val cutoffReason: RoleApplicationCutoffReason?
) {
    val isOpen: Boolean
        get() = state == RoleApplicationWindowState.OPEN
}
