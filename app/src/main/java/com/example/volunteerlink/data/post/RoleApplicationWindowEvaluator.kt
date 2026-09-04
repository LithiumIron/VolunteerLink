package com.example.volunteerlink.data.post

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Contains pure rules for deciding whether a volunteer role is still open for applications relative to the post
// mode, role side, start dates and current AppClock time.
//
// Organisation applicant/review UI can use the same application-window interpretation as the Volunteer apply flow,
// reducing inconsistent edge cases for Hybrid posts.
//
// No database state is mutated here; authoritative lifecycle cleanup is still performed by server RPCs when
// required.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import com.example.volunteerlink.data.time.AppClock
import kotlinx.serialization.Serializable
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
/**
 * DETAILED DECLARATION — RoleApplicationWindowEvaluator
 *
 * Single shared instance for Role Application Window Evaluator so related rules/state are defined once for the
 * application process.
 */
object RoleApplicationWindowEvaluator {

    /**
     * DETAILED BEHAVIOUR — evaluate
     *
     * Evaluates the pure business rule represented by evaluate from the values supplied by the caller.
     *
     * The function does not perform UI or network side effects, which keeps the rule deterministic and reusable
     * from multiple screens/workflows.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     */
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

    /**
     * DETAILED BEHAVIOUR — isValidDate
     *
     * Evaluates the pure business rule represented by is valid date from the values supplied by the caller.
     *
     * The function does not perform UI or network side effects, which keeps the rule deterministic and reusable
     * from multiple screens/workflows.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private fun isValidDate(value: String): Boolean =
        parseDateAtStartOfDay(value) != null

    /**
     * DETAILED BEHAVIOUR — parseDateAtStartOfDay
     *
     * Evaluates the pure business rule represented by parse date at start of day from the values supplied by
     * the caller.
     *
     * The function does not perform UI or network side effects, which keeps the rule deterministic and reusable
     * from multiple screens/workflows.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private fun parseDateAtStartOfDay(value: String): Long? {
        val parsed = runCatching {
            dateFormat().parse(value.trim())
        }.getOrNull() ?: return null

        return startOfDay(parsed.time)
    }

    /**
     * DETAILED BEHAVIOUR — startOfDay
     *
     * Evaluates the pure business rule represented by start of day from the values supplied by the caller.
     *
     * The function does not perform UI or network side effects, which keeps the rule deterministic and reusable
     * from multiple screens/workflows.
     */
    private fun startOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * DETAILED BEHAVIOUR — dateFormat
     *
     * Evaluates the pure business rule represented by date format from the values supplied by the caller.
     *
     * The function does not perform UI or network side effects, which keeps the rule deterministic and reusable
     * from multiple screens/workflows.
     */
    private fun dateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
    }
}

/**
 * DETAILED DECLARATION — RoleApplicationWindowInput
 *
 * Domain/UI type for Role Application Window Input used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class RoleApplicationWindowInput(
    val roleMode: String,
    val postStatus: String? = null,
    val physicalStartDate: String? = null,
    val remoteStartDate: String? = null
)

@Serializable
/**
 * DETAILED DECLARATION — RoleApplicationWindowState
 *
 * Domain/UI type for Role Application Window State used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
enum class RoleApplicationWindowState {
    OPEN,
    CLOSED
}

@Serializable
/**
 * DETAILED DECLARATION — RoleApplicationCutoffReason
 *
 * Domain/UI type for Role Application Cutoff Reason used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
enum class RoleApplicationCutoffReason {
    ROLE_START,
    POST_STATUS
}

/**
 * DETAILED DECLARATION — RoleApplicationWindow
 *
 * Domain/UI type for Role Application Window used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class RoleApplicationWindow(
    val state: RoleApplicationWindowState,
    val cutoffDate: String?,
    val cutoffReason: RoleApplicationCutoffReason?
) {
    val isOpen: Boolean
        get() = state == RoleApplicationWindowState.OPEN
}
