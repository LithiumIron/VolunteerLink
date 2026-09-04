package com.example.volunteerlink.data

import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class VolunteerPhysicalScheduleConflict(
    val application: VolunteerOpportunityApplication,
    val event: VolunteerOpportunityEvent,
    val role: VolunteerOpportunityRole,
    val date: String,
    val startTime: String,
    val endTime: String
) {
    val isAccepted: Boolean
        get() = application.applicationStatus == VolunteerApplicationStatus.ACCEPTED

    fun message(): String =
        "This Physical activity overlaps with your ${if (isAccepted) "accepted role" else "pending application"} " +
            "in \"${event.eventTitle}\" on ${VolunteerScheduleText.date(date)}, " +
            "${VolunteerScheduleText.time(startTime)}–${VolunteerScheduleText.time(endTime)}."
}

/** One source of truth for Physical-role schedule conflicts. Remote and Hybrid roles remain eligible. */
object VolunteerPhysicalScheduleConflictEvaluator {
    private data class Window(val date: String, val start: String, val end: String)

    fun hasConflict(
        candidateEvent: VolunteerOpportunityEvent,
        candidateRole: VolunteerOpportunityRole,
        applications: List<VolunteerOpportunityApplication>,
        events: List<VolunteerOpportunityEvent>
    ): Boolean = firstFor(candidateEvent, candidateRole, applications, events) != null

    fun firstFor(
        candidateEvent: VolunteerOpportunityEvent,
        candidateRole: VolunteerOpportunityRole,
        applications: List<VolunteerOpportunityApplication> = VolunteerOpportunitySessionStore.volunteerApplications,
        events: List<VolunteerOpportunityEvent> = VolunteerOpportunitySessionStore.volunteerOpportunityEvents
    ): VolunteerPhysicalScheduleConflict? {
        if (!candidateRole.roleMode.equals("PHYSICAL", ignoreCase = true)) return null
        val candidateWindows = windows(candidateEvent, candidateRole)
        if (candidateWindows.isEmpty()) return null
        return applications.asSequence()
            .filter { it.applicationStatus in setOf(VolunteerApplicationStatus.PENDING, VolunteerApplicationStatus.ACCEPTED) }
            .filterNot { it.applicationEventId == candidateEvent.eventId && it.applicationRoleId == candidateRole.roleId }
            .mapNotNull { application ->
                val event = events.firstOrNull { it.eventId == application.applicationEventId } ?: return@mapNotNull null
                val role = application.applicationRoleId?.let { id -> event.eventVolunteerRoles.firstOrNull { it.roleId == id } }
                    ?: return@mapNotNull null
                if (!role.roleMode.equals("PHYSICAL", ignoreCase = true)) return@mapNotNull null
                val matched = candidateWindows.firstNotNullOfOrNull { proposed ->
                    windows(event, role).firstOrNull { existing ->
                        proposed.date == existing.date && proposed.start < existing.end && proposed.end > existing.start
                    }
                } ?: return@mapNotNull null
                VolunteerPhysicalScheduleConflict(application, event, role, matched.date, matched.start, matched.end)
            }
            .sortedByDescending { it.isAccepted }
            .firstOrNull()
    }

    private fun windows(event: VolunteerOpportunityEvent, role: VolunteerOpportunityRole): List<Window> {
        val assigned = role.roleScheduleItems.filter {
            it.assignedToRole && it.scheduleType.equals("PHYSICAL", ignoreCase = true) && it.rawDate.isNotBlank()
        }.mapNotNull {
            val start = it.startTime.ifBlank { event.eventPhysicalStartTime }.take(5)
            val end = it.endTime.ifBlank { event.eventPhysicalEndTime }.take(5)
            if (start.matches(Regex("\\d{2}:\\d{2}")) && end.matches(Regex("\\d{2}:\\d{2}")) && start < end) Window(it.rawDate, start, end) else null
        }
        if (assigned.isNotEmpty()) return assigned
        val start = event.eventPhysicalStartTime.take(5)
        val end = event.eventPhysicalEndTime.take(5)
        if (!start.matches(Regex("\\d{2}:\\d{2}")) || !end.matches(Regex("\\d{2}:\\d{2}")) || start >= end) return emptyList()
        return runCatching {
            val zone = TimeZone.getTimeZone(event.eventTimeZone)
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false; timeZone = zone }
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }
            val endDate = requireNotNull(parser.parse(event.eventPhysicalEndDate))
            val cursor = Calendar.getInstance(zone).apply { time = requireNotNull(parser.parse(event.eventPhysicalStartDate)) }
            buildList {
                while (!cursor.time.after(endDate)) {
                    require(size < 3660)
                    add(Window(formatter.format(cursor.time), start, end))
                    cursor.add(Calendar.DATE, 1)
                }
            }
        }.getOrDefault(emptyList())
    }
}
