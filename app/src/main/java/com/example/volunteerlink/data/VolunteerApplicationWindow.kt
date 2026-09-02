package com.example.volunteerlink.data

import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Volunteer-side application window rules.
 *
 * The database remains the final authority, but Android mirrors the same rule so
 * Hybrid opportunities do not close every role when only one phase starts:
 * - PHYSICAL role -> eventPhysicalStartDate
 * - REMOTE role   -> eventRemoteStartDate
 *
 * Event-level checks mean "at least one role is still open".
 */
object VolunteerApplicationWindow {

    fun beforeStart(
        event: VolunteerOpportunityEvent?,
        role: VolunteerOpportunityRole?,
        nowMillis: Long = AppClock.nowMillis()
    ): Boolean {
        val raw = roleStartDate(event, role)
        return raw?.let { isBeforeDate(it, nowMillis) } ?: false
    }

    fun canApply(
        event: VolunteerOpportunityEvent?,
        role: VolunteerOpportunityRole?,
        nowMillis: Long = AppClock.nowMillis()
    ): Boolean =
        event?.eventStatus.equals("PUBLISHED", ignoreCase = true) &&
            beforeStart(event, role, nowMillis)

    fun reason(
        event: VolunteerOpportunityEvent?,
        role: VolunteerOpportunityRole?
    ): String = when {
        event == null || role == null ->
            "Application dates are unavailable. Please sync before continuing."

        roleStartDate(event, role)?.let(::parseDate) == null ->
            "This role has no valid start date. Please sync before continuing."

        !beforeStart(event, role) ->
            "Applications closed: this role has already started."

        else ->
            "This opportunity is not open for applications."
    }

    /** Event-level compatibility helper: true while any role is still open. */
    fun beforeStart(
        event: VolunteerOpportunityEvent?,
        nowMillis: Long = AppClock.nowMillis()
    ): Boolean = event?.eventVolunteerRoles.orEmpty().any { role ->
        beforeStart(event, role, nowMillis)
    }

    /** Event-level compatibility helper: true while any role is still open. */
    fun canApply(
        event: VolunteerOpportunityEvent?,
        nowMillis: Long = AppClock.nowMillis()
    ): Boolean =
        event?.eventStatus.equals("PUBLISHED", ignoreCase = true) &&
            beforeStart(event, nowMillis)

    fun reason(event: VolunteerOpportunityEvent?): String = when {
        event == null ->
            "Application dates are unavailable. Please sync before continuing."

        event.eventStatus.equals("PUBLISHED", ignoreCase = true) &&
            !beforeStart(event) ->
            "Applications closed: all roles in this opportunity have already started."

        else ->
            "This opportunity is not open for applications."
    }

    private fun roleStartDate(
        event: VolunteerOpportunityEvent?,
        role: VolunteerOpportunityRole?
    ): String? {
        if (event == null || role == null) return null

        val raw = when (role.roleMode.trim().uppercase(Locale.US)) {
            "PHYSICAL" -> event.eventPhysicalStartDate
            "REMOTE" -> event.eventRemoteStartDate
            else -> ""
        }.takeIf { it.isNotBlank() }

        // Older cached payloads do not have the two raw role-mode dates yet.
        // Keep them usable only when the event is single-phase/legacy.
        if (raw != null) return raw
        if (event.eventOpportunityType.equals("Hybrid", ignoreCase = true)) return null
        return event.eventApplicationStartDate.takeIf { it.isNotBlank() }
    }

    private fun isBeforeDate(raw: String, nowMillis: Long): Boolean {
        val start = parseDate(raw) ?: return false
        return Date(nowMillis).before(start)
    }

    private fun parseDate(raw: String): Date? {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(raw)) return null

        // Match Supabase's date-level rule. A role closes at the start of its date.
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val position = ParsePosition(0)
        val start = format.parse(raw, position) ?: return null
        if (position.index != raw.length) return null
        return start
    }
}
