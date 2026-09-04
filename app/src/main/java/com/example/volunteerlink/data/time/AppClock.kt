
package com.example.volunteerlink.data.time

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Provides one application-wide business-time source for date-sensitive VolunteerLink rules.
//
// When app_test_clock.APP enables test time, nowMillis returns the frozen Supabase test timestamp; otherwise it
// returns the device/system time.
//
// The last successfully loaded clock choice is cached in SharedPreferences so a temporary offline restart does not
// silently switch the business date during testing.
//
// Create Post timing checks, application windows, Manage status and other business-date calculations should read
// AppClock rather than calling System.currentTimeMillis directly.
//
// The database has a matching volunteer_app_now() concept so client and server can evaluate the same test
// scenario.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import android.content.Context
import android.util.Log
import com.example.volunteerlink.data.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * One shared source of "now" for VolunteerLink.
 *
 * - When app_test_clock.APP.use_test_time is true, the fixed test_datetime is used.
 * - When it is false (or the row cannot be read), the phone's real local time is used.
 *
 * The database timestamp is converted to epoch milliseconds. Existing Calendar/
 * SimpleDateFormat code then automatically interprets those milliseconds using the
 * phone's local time zone.
 */
/**
 * DETAILED DECLARATION — AppClock
 *
 * Single shared instance for App Clock so related rules/state are defined once for the application process.
 */
object AppClock {

    private const val CLOCK_NAME = "APP"
    private const val TAG = "AppClock"
    private const val PREFERENCES_NAME = "volunteerlink_app_clock"
    private const val KEY_HAS_CACHED_CLOCK = "has_cached_clock"
    private const val KEY_USE_TEST_TIME = "use_test_time"
    private const val KEY_TEST_TIME_MILLIS = "test_time_millis"

    private var applicationContext: Context? = null

    private val _state = MutableStateFlow(AppClockState())
    val state = _state.asStateFlow()

    /**
     * Restores the last successfully downloaded clock before Compose starts.
     * Offline mode therefore uses the same date as the cached dashboard instead
     * of silently changing to the phone clock after every process restart.
     */
    /**
     * DETAILED BEHAVIOUR — initialise
     *
     * Implements the current VolunteerLink responsibility for initialise in this support/model layer.
     */
    fun initialise(context: Context) {
        applicationContext = context.applicationContext
        val preferences = context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

        if (!preferences.getBoolean(KEY_HAS_CACHED_CLOCK, false)) return

        val useTestTime = preferences.getBoolean(KEY_USE_TEST_TIME, false)
        val storedMillis = preferences.getLong(KEY_TEST_TIME_MILLIS, Long.MIN_VALUE)
        updateState(
            useTestTime = useTestTime,
            testTimeMillis = storedMillis.takeUnless { it == Long.MIN_VALUE },
            persist = false
        )
    }

    /**
     * Returns the time the rest of the app should treat as "now".
     * Test time is intentionally frozen so date-dependent behaviour is easy to preview.
     */
    /**
     * DETAILED BEHAVIOUR — nowMillis
     *
     * Implements the current VolunteerLink responsibility for now millis in this support/model layer.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun nowMillis(): Long {
        val current = _state.value
        return if (current.useTestTime && current.testTimeMillis != null) {
            current.testTimeMillis
        } else {
            System.currentTimeMillis()
        }
    }

    /** True only when a usable database test time is currently active. */
    /**
     * DETAILED BEHAVIOUR — isUsingTestTime
     *
     * Implements the current VolunteerLink responsibility for is using test time in this support/model layer.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun isUsingTestTime(): Boolean {
        val current = _state.value
        return current.useTestTime && current.testTimeMillis != null
    }

    /**
     * Reloads the APP clock row from Supabase.
     *
     * This is called whenever MainActivity resumes, so during a demo you can change
     * use_test_time/test_datetime in Supabase, return to the app, and the new value
     * will be picked up without changing code.
     *
     * Any database/network problem safely falls back to the phone's real time.
     */
    /**
     * DETAILED BEHAVIOUR — refreshFromDatabase
     *
     * Implements the current VolunteerLink responsibility for refresh from database in this support/model
     * layer.
     *
     * Reads/maps Supabase table data from `app_test_clock` (the single shared application test-clock row used
     * to control VolunteerLink business time during testing).
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun refreshFromDatabase() {
        try {
            val row = supabase
                // SUPABASE TABLE: app_test_clock — the single shared application test-clock row used to control VolunteerLink business time during testing.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
                .from("app_test_clock")
                .select {
                    filter {
                        eq("clock_name", CLOCK_NAME)
                    }
                }
                .decodeList<JsonObject>()
                .firstOrNull()

            if (row == null) {
                if (!_state.value.isLoaded) {
                    updateState(
                        useTestTime = false,
                        testTimeMillis = null,
                        persist = false
                    )
                }
                Log.w(TAG, "APP clock row not found. Keeping cached clock when available.")
                return
            }

            val useTestTime = row.optionalText("use_test_time")
                ?.toBooleanStrictOrNull()
                ?: false

            val testTimeMillis = row.optionalText("test_datetime")
                ?.let(::parseSupabaseTimestamp)

            updateState(
                useTestTime = useTestTime,
                testTimeMillis = testTimeMillis,
                persist = true
            )

            when {
                useTestTime && testTimeMillis != null ->
                    Log.d(TAG, "Test clock enabled: $testTimeMillis")

                useTestTime ->
                    Log.w(TAG, "Test clock enabled but test_datetime is invalid/null. Using phone time.")

                else ->
                    Log.d(TAG, "Test clock disabled. Using phone time.")
            }
        } catch (exception: Exception) {
            // Keep the last successful cloud clock when offline. Only fall
            // back to phone time when this installation has never synced it.
            if (!_state.value.isLoaded) {
                updateState(
                    useTestTime = false,
                    testTimeMillis = null,
                    persist = false
                )
            }
            Log.w(
                TAG,
                "Could not read app_test_clock. Using phone time instead.",
                exception
            )
        }
    }

    /**
     * Writes a fresh observable state every time the app clock is refreshed.
     *
     * refreshVersion is incremented even when test mode is OFF so screens that
     * depend on "today" can recalculate after the app resumes.
     */
    /**
     * DETAILED BEHAVIOUR — updateState
     *
     * Implements the current VolunteerLink responsibility for update state in this support/model layer.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    private fun updateState(
        useTestTime: Boolean,
        testTimeMillis: Long?,
        persist: Boolean = false
    ) {
        _state.value = AppClockState(
            useTestTime = useTestTime,
            testTimeMillis = testTimeMillis,
            isLoaded = true,
            refreshVersion = _state.value.refreshVersion + 1L
        )

        if (persist) {
            applicationContext
                ?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putBoolean(KEY_HAS_CACHED_CLOCK, true)
                ?.putBoolean(KEY_USE_TEST_TIME, useTestTime)
                ?.apply {
                    if (testTimeMillis == null) {
                        remove(KEY_TEST_TIME_MILLIS)
                    } else {
                        putLong(KEY_TEST_TIME_MILLIS, testTimeMillis)
                    }
                }
                ?.apply()
        }
    }

    /**
     * PostgREST returns timestamptz values as ISO-8601 text such as:
     * 2026-08-23T07:30:00+00:00
     *
     * Fractional seconds can contain more than three digits, so trim them to
     * milliseconds before SimpleDateFormat parses the value.
     */
    /**
     * DETAILED BEHAVIOUR — parseSupabaseTimestamp
     *
     * Implements the current VolunteerLink responsibility for parse supabase timestamp in this support/model
     * layer.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private fun parseSupabaseTimestamp(value: String): Long? {
        val normalized = value.trim()
            .replace(
                Regex("""(\.\d{3})\d+(?=Z|[+-]\d{2}:?\d{2}$)"""),
                "$1"
            )

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss.SSSXXX",
            "yyyy-MM-dd HH:mm:ssXXX"
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }.parse(normalized)?.time
            }.getOrNull()
        }
    }

    /**
     * DETAILED BEHAVIOUR — optionalText
     *
     * Implements the current VolunteerLink responsibility for optional text in this support/model layer.
     */
    private fun JsonObject.optionalText(key: String): String? {
        return this[key]
            ?.jsonPrimitive
            ?.contentOrNull
    }
}

/** Small observable state so a future debug indicator can show clock mode if needed. */
/**
 * DETAILED DECLARATION — AppClockState
 *
 * Domain/UI type for App Clock State used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class AppClockState(
    val useTestTime: Boolean = false,
    val testTimeMillis: Long? = null,
    val isLoaded: Boolean = false,
    val refreshVersion: Long = 0L
)


