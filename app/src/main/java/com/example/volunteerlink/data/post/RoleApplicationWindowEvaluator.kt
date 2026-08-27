package com.example.volunteerlink.data.post

import com.example.volunteerlink.data.time.AppClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Shared role-level application cutoff rules.
 *
 * Applications stay open until the volunteering phase for that role starts.
 * Hybrid posts are evaluated per role:
 * - PHYSICAL role -> Physical volunteering start date
 * - REMOTE role   -> Remote volunteering start date
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

        val cutoffDate = roleStartDate?.takeIf(::isValidDate)

        // Incomplete legacy/test data with no usable start date stays open
        // rather than silently hiding legitimate applications.
        if (cutoffDate == null) {
            return RoleApplicationWindow(
                state = RoleApplicationWindowState.OPEN,
                cutoffDate = null,
                cutoffReason = null
            )
        }

        val today = startOfDay(nowMillis)
        val cutoffDay = parseDateAtStartOfDay(cutoffDate)
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
            cutoffDate = cutoffDate,
            cutoffReason = RoleApplicationCutoffReason.ROLE_START
        )
    }

    private fun isValidDate(value: String): Boolean =
        parseDateAtStartOfDay(value) != null

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
}

data class RoleApplicationWindowInput(
    val roleMode: String,
    val postStatus: String? = null,
    val physicalStartDate: String? = null,
    val remoteStartDate: String? = null
)

enum class RoleApplicationWindowState {
    OPEN,
    CLOSED
}

enum class RoleApplicationCutoffReason {
    ROLE_START,
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
