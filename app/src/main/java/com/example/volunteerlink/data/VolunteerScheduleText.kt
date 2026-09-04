package com.example.volunteerlink.data

// Keeps date and time formatting consistent across volunteer event screens.

import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar

/** Display only: never infer an application/submission deadline from a checkpoint. */
object VolunteerScheduleText {
    fun date(raw: String): String = runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }
        val position = ParsePosition(0)
        val parsed = parser.parse(raw, position)
        require(parsed != null && position.index == raw.length)
        SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).apply { timeZone = parser.timeZone }.format(parsed)
    }.getOrDefault(raw.ifBlank { "Not specified" })

    // Purpose: Handles the time rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun time(raw: String): String = runCatching {
        require(raw.matches(Regex("\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?")))
        val parser = SimpleDateFormat("HH:mm", Locale.ENGLISH).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }
        val parsed = requireNotNull(parser.parse(raw.take(5)))
        SimpleDateFormat("h:mm a", Locale.ENGLISH).apply { timeZone = parser.timeZone }.format(parsed)
    }.getOrDefault(raw.ifBlank { "Not specified" })

    // Purpose: Handles the range rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun range(start: String, end: String): String =
        if (start == end && start.isNotBlank()) date(start) else "${date(start)} – ${date(end)}"

    /** Backend date-only boundaries are UTC. Display the same instant in Malaysia,
     * never silently reinterpret a UTC midnight as a local midnight. */
    fun deadline(raw: String, inclusiveDay: Boolean = false): String = runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
            isLenient = false; timeZone = TimeZone.getTimeZone("UTC")
        }
        val position = ParsePosition(0)
        val parsed = parser.parse(raw, position)
        require(parsed != null && position.index == raw.length)
        val instant = Calendar.getInstance(parser.timeZone).apply {
            time = parsed
            if (inclusiveDay) add(Calendar.DATE, 1)
        }.time
        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        }.format(instant)
    }.getOrDefault("Not available — sync to check")

    // Purpose: Handles the compact rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun compact(event: VolunteerOpportunityEvent, role: VolunteerOpportunityRole): String {
        val remote = role.roleMode.equals("REMOTE", true)
        return "${if (remote) "Remote work" else "Physical phase"}: " + range(
            if (remote) event.eventRemoteStartDate else event.eventPhysicalStartDate,
            if (remote) event.eventRemoteEndDate else event.eventPhysicalEndDate)
    }

    // Purpose: Handles the event rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun event(event: VolunteerOpportunityEvent): String = buildList {
        if (event.eventPhysicalStartDate.isNotBlank()) add(
            "Physical phase: ${range(event.eventPhysicalStartDate, event.eventPhysicalEndDate)}\n" +
                "${time(event.eventPhysicalStartTime)} – ${time(event.eventPhysicalEndTime)}" +
                if (event.eventTimeZone != "Asia/Kuala_Lumpur") " (${event.eventTimeZone})" else ""
        )
        if (event.eventRemoteStartDate.isNotBlank()) add(
            "Remote phase: ${range(event.eventRemoteStartDate, event.eventRemoteEndDate)}\n" +
                "Submit before ${deadline(event.eventRemoteEndDate, true)} (Malaysia time)" +
                if (event.eventRemoteOriginalEndDate.isNotBlank() &&
                    event.eventRemoteOriginalEndDate != event.eventRemoteEndDate)
                    "\nExtended from ${date(event.eventRemoteOriginalEndDate)}" else ""
        )
        if (isEmpty()) add("Phase dates are not available in this copy. Sync online to check the latest schedule.")
    }.joinToString("\n\n")

    // Purpose: Handles the role rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun role(event: VolunteerOpportunityEvent, role: VolunteerOpportunityRole): String {
        val remote = role.roleMode.equals("REMOTE", true)
        val stageStart = if (remote) event.eventRemoteStartDate else event.eventPhysicalStartDate
        val stageEnd = if (remote) event.eventRemoteEndDate else event.eventPhysicalEndDate
        val days = role.roleScheduleItems.filter {
            it.scheduleType.equals(role.roleMode, true) && it.rawDate.isNotBlank()
        }.map { it.rawDate }.distinct().sorted()
        return buildList {
            add(compact(event, role))
            if (days.isNotEmpty()) add("Scheduled dates: ${days.joinToString { date(it) }}")
            if (remote) {
                add("Submit before ${deadline(stageEnd, true)} (Malaysia time)")
            } else {
                add("Check-in hours: ${time(event.eventPhysicalStartTime)} – ${time(event.eventPhysicalEndTime)}")
            }
            add("Apply before ${deadline(stageStart)} (Malaysia time)")
        }.joinToString("\n")
    }
}
