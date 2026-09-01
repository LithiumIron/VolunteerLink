package com.example.volunteerlink.data

import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Mirrors the existing RPC rule: applications close on the earliest phase's
 * start DATE (not its start time), including remote posts. Never changes a post
 * status or the shared clock. The database RPC remains the final authority.
 * UTC matches the Supabase RPC session's date conversion; display dates may use KL.
 * Older cached payloads without this date fail closed until a successful sync.
 */
object VolunteerApplicationWindow {
    fun beforeStart(event: VolunteerOpportunityEvent?, nowMillis: Long = AppClock.nowMillis()): Boolean {
        val raw = event?.eventApplicationStartDate ?: return false
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(raw)) return false
        // java.text keeps this compatible with the project's minimum API 24.
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val position = ParsePosition(0)
        val start = format.parse(raw, position) ?: return false
        if (position.index != raw.length) return false
        return Date(nowMillis).before(start)
    }

    fun canApply(event: VolunteerOpportunityEvent?, nowMillis: Long = AppClock.nowMillis()): Boolean =
        event?.eventStatus.equals("PUBLISHED", ignoreCase = true) && beforeStart(event, nowMillis)

    fun reason(event: VolunteerOpportunityEvent?): String = when {
        event == null || event.eventApplicationStartDate.isBlank() ->
            "Application dates are unavailable. Please sync before continuing."
        !beforeStart(event) -> "Applications closed: this opportunity has already started."
        else -> "This opportunity is not open for applications."
    }
}
