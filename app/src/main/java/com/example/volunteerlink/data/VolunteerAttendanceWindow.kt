package com.example.volunteerlink.data

// Calculates whether check-in or attendance review is available at the current app time.

import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.*

// Purpose: Handles the volunteer attendance window rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
object VolunteerAttendanceWindow {
    fun localDate(nowMillis: Long, zone: String): String {
        require(zone in TimeZone.getAvailableIDs())
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(zone)
        }.format(Date(nowMillis))
    }

    // Purpose: Handles the valid date rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    private fun validDate(raw: String): String {
        require(raw.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }
        val position = ParsePosition(0)
        require(parser.parse(raw, position) != null && position.index == raw.length)
        return raw
    }

    // Purpose: Handles the seconds rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    private fun seconds(raw: String): Double {
        val parts = raw.split(':')
        require(parts.size in 2..3)
        val hour = parts[0].toInt(); val minute = parts[1].toInt()
        val second = parts.getOrNull(2)?.toDouble() ?: 0.0
        require(hour in 0..23 && minute in 0..59 && second >= 0 && second < 60)
        return hour * 3600 + minute * 60 + second
    }

    // Mirrors the existing server's PHASE-hours rule, not a role-hours replacement.
    // Purpose: Handles the reason rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun reason(event: VolunteerOpportunityEvent, role: VolunteerOpportunityRole, nowMillis: Long): String? = runCatching {
        val today = localDate(nowMillis, event.eventTimeZone)
        val start = validDate(event.eventPhysicalStartDate)
        val end = validDate(event.eventPhysicalEndDate)
        require(end >= start)
        val from = seconds(event.eventPhysicalStartTime)
        val to = seconds(event.eventPhysicalEndTime)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(event.eventTimeZone)).apply { timeInMillis = nowMillis }
        val now = calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 +
            calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000.0
        val roleDays = role.roleScheduleItems.filter {
            it.assignedToRole && it.scheduleType.equals("PHYSICAL", true)
        }.map { validDate(it.rawDate) }
        when {
            event.eventStatus !in setOf("PUBLISHED", "CLOSED") -> "This event is not accepting attendance."
            !role.roleMode.equals("PHYSICAL", true) -> "This role does not use Physical attendance."
            today < start -> "Attendance starts on ${VolunteerScheduleText.date(start)}."
            today > end -> "The Physical phase has ended. Contact the organisation if you missed check-in."
            roleDays.isNotEmpty() && today !in roleDays -> "Your role is not scheduled today. See your role schedule."
            now < from -> "Check-in opens at ${VolunteerScheduleText.time(event.eventPhysicalStartTime)} today."
            now > to -> "Check-in closed at ${VolunteerScheduleText.time(event.eventPhysicalEndTime)} today."
            else -> null
        }
    }.getOrElse { "The attendance window is unavailable. Sync online to load the phase dates and times." }
}
