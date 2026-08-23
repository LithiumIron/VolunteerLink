package com.example.volunteerlink.data.time

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
object AppClock {

    private const val CLOCK_NAME = "APP"
    private const val TAG = "AppClock"

    private val _state = MutableStateFlow(AppClockState())
    val state = _state.asStateFlow()

    /**
     * Returns the time the rest of the app should treat as "now".
     * Test time is intentionally frozen so date-dependent behaviour is easy to preview.
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
    suspend fun refreshFromDatabase() {
        try {
            val row = supabase
                .from("app_test_clock")
                .select {
                    filter {
                        eq("clock_name", CLOCK_NAME)
                    }
                }
                .decodeList<JsonObject>()
                .firstOrNull()

            if (row == null) {
                updateState(
                    useTestTime = false,
                    testTimeMillis = null
                )
                Log.w(TAG, "APP clock row not found. Using phone time.")
                return
            }

            val useTestTime = row.optionalText("use_test_time")
                ?.toBooleanStrictOrNull()
                ?: false

            val testTimeMillis = row.optionalText("test_datetime")
                ?.let(::parseSupabaseTimestamp)

            updateState(
                useTestTime = useTestTime,
                testTimeMillis = testTimeMillis
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
            updateState(
                useTestTime = false,
                testTimeMillis = null
            )
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
    private fun updateState(
        useTestTime: Boolean,
        testTimeMillis: Long?
    ) {
        _state.value = AppClockState(
            useTestTime = useTestTime,
            testTimeMillis = testTimeMillis,
            isLoaded = true,
            refreshVersion = _state.value.refreshVersion + 1L
        )
    }

    /**
     * PostgREST returns timestamptz values as ISO-8601 text such as:
     * 2026-08-23T07:30:00+00:00
     *
     * Fractional seconds can contain more than three digits, so trim them to
     * milliseconds before SimpleDateFormat parses the value.
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

    private fun JsonObject.optionalText(key: String): String? {
        return this[key]
            ?.jsonPrimitive
            ?.contentOrNull
    }
}

/** Small observable state so a future debug indicator can show clock mode if needed. */
data class AppClockState(
    val useTestTime: Boolean = false,
    val testTimeMillis: Long? = null,
    val isLoaded: Boolean = false,
    val refreshVersion: Long = 0L
)
