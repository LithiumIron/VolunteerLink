package com.example.volunteerlink.data

// Converts attendance records into display states for the volunteer history screens.

import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class VolunteerAttendanceDayState {
    NOT_STARTED, NOT_SCHEDULED, OPEN, NOT_OPENED, NO_RECORD, PRESENT, ABSENT, UNKNOWN, DATE_REVIEW
}

data class VolunteerAttendanceHistoryDay(
    val date: String,
    val dayNumber: Int,
    val state: VolunteerAttendanceDayState,
    val recordedAt: String?,
    val ended: Boolean,
    val reviewDeadlineMillis: Long,
    // This is a proposed eligibility calculation, NOT a server permission to submit.
    val withinReviewWindow: Boolean
)

/** Mirrors the current per-day Physical phase window. Does not write or infer database ABSENT rows. */
object VolunteerAttendanceHistory {
    private const val REVIEW_HOURS_MILLIS = 48L * 60 * 60 * 1000

    private fun parse(raw: String, pattern: String, zone: TimeZone): Date {
        val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false; timeZone = zone }
        val position = ParsePosition(0)
        val date = parser.parse(raw, position)
        require(date != null && position.index == raw.length) { "Invalid attendance date or time" }
        return date
    }

    private fun hours(raw: String): String {
        require(raw.matches(Regex("\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?")))
        return if (raw.length == 5) "$raw:00" else raw.substringBefore('.')
    }

    fun build(event: VolunteerOpportunityEvent, role: VolunteerOpportunityRole,
              records: List<VolunteerAttendanceRecord>?, days: List<VolunteerAttendanceDay>,
              now: Long): List<VolunteerAttendanceHistoryDay> {
        require(role.roleMode.equals("PHYSICAL", true))
        require(event.eventTimeZone in TimeZone.getAvailableIDs())
        val zone = TimeZone.getTimeZone(event.eventTimeZone)
        val first = parse(event.eventPhysicalStartDate, "yyyy-MM-dd", zone)
        val last = parse(event.eventPhysicalEndDate, "yyyy-MM-dd", zone)
        require(last >= first)
        val calendar = Calendar.getInstance(zone).apply { time = first }
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }
        val dates = mutableListOf<String>()
        while (calendar.time <= last) {
            require(dates.size < 3660) { "Attendance period is too long" }
            dates += formatter.format(calendar.time)
            calendar.add(Calendar.DATE, 1)
        }
        val assigned = role.roleScheduleItems.filter { it.assignedToRole && it.scheduleType.equals("PHYSICAL", true) }
            .map { it.rawDate }.toSet()
        require(assigned.all { it in dates }) { "Assigned dates are outside the Physical phase" }
        val today = formatter.format(Date(now))
        return dates.mapIndexed { index, date ->
            val start = parse("$date ${hours(event.eventPhysicalStartTime)}", "yyyy-MM-dd HH:mm:ss", zone).time
            val end = parse("$date ${hours(event.eventPhysicalEndTime)}", "yyyy-MM-dd HH:mm:ss", zone).time
            // The existing backend has same-day phase hours. Never invent an overnight shift.
            require(end > start) { "Overnight or invalid attendance hours need organisation confirmation" }
            val expected = assigned.isEmpty() || date in assigned
            val matching = records?.filter { it.date == date }.orEmpty()
            val record = matching.singleOrNull()
            val recordedInstant = record?.recordedAt?.let(::timestampMillis)
            val anomalous = matching.size > 1 || (record != null &&
                (date > today || !expected || (record?.status == "PRESENT" && now < start) ||
                    (recordedInstant != null && recordedInstant > now + 300000L)))
            val state = when {
                anomalous -> VolunteerAttendanceDayState.DATE_REVIEW
                !expected -> VolunteerAttendanceDayState.NOT_SCHEDULED
                record?.status == "PRESENT" -> VolunteerAttendanceDayState.PRESENT
                record?.status == "ABSENT" -> VolunteerAttendanceDayState.ABSENT
                record != null -> VolunteerAttendanceDayState.UNKNOWN
                now < start -> VolunteerAttendanceDayState.NOT_STARTED
                records == null -> VolunteerAttendanceDayState.UNKNOWN
                now > end -> VolunteerAttendanceDayState.NO_RECORD
                event.eventStatus !in setOf("PUBLISHED", "CLOSED") -> VolunteerAttendanceDayState.UNKNOWN
                days.none { it.date == date && it.active } -> VolunteerAttendanceDayState.NOT_OPENED
                else -> VolunteerAttendanceDayState.OPEN
            }
            val deadline = end + REVIEW_HOURS_MILLIS
            VolunteerAttendanceHistoryDay(date, index + 1, state, record?.recordedAt,
                expected && now > end, deadline,
                expected && now > end && now < deadline &&
                    state in setOf(VolunteerAttendanceDayState.NO_RECORD, VolunteerAttendanceDayState.ABSENT) &&
                    event.eventStatus in setOf("PUBLISHED", "CLOSED"))
        }
    }

    fun timestampMillis(raw: String): Long? = runCatching {
        val normal = raw.replace(' ', 'T').replace(Regex("\\.(\\d+)(?=Z|[+-])")) {
            "." + it.groupValues[1].take(3).padEnd(3, '0')
        }.let { if (it.matches(Regex(".*[+-]\\d{2}$"))) "$it:00" else it }
        parse(normal, if (normal.contains('.')) "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" else "yyyy-MM-dd'T'HH:mm:ssXXX",
            TimeZone.getTimeZone("UTC")).time
    }.getOrNull()

    fun recordedTime(raw: String?, zoneId: String): String {
        val millis = raw?.let(::timestampMillis) ?: return "Time not recorded"
        if (zoneId !in TimeZone.getAvailableIDs()) return "Time unavailable"
        return SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone(zoneId)
        }.format(Date(millis))
    }

    fun preview(rows: List<VolunteerAttendanceHistoryDay>, today: String): List<VolunteerAttendanceHistoryDay> {
        if (rows.size <= 3) return rows
        val current = rows.indexOfLast { it.date <= today }.coerceAtLeast(0)
        val start = (current - 1).coerceIn(0, rows.size - 3)
        return rows.subList(start, start + 3)
    }
}
