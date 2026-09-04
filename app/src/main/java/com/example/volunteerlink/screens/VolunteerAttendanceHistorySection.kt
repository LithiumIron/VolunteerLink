package com.example.volunteerlink.screens

// Displays recorded attendance and explains the 48-hour attendance-review rule.

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.data.*
import com.example.volunteerlink.model.*
import com.example.volunteerlink.ui.theme.*

@Composable
internal fun VolunteerAttendanceHistorySection(event: VolunteerOpportunityEvent, role: VolunteerOpportunityRole,
    data: VolunteerAttendanceData?, now: Long, onHelp: (String) -> Unit) {
    // Rebuild the attendance view only when its input data or the app time changes.
    var expanded by remember(event.eventId, role.roleId) { mutableStateOf(false) }
    val result = remember(event, role, data, now) {
        runCatching { VolunteerAttendanceHistory.build(event, role, data?.records, data?.days.orEmpty(), now) }
    }
    val rows = result.getOrNull()
    HorizontalDivider(color = VolunteerLinkBorderColour)
    Text("Daily attendance", fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = VolunteerLinkTextPrimary)
    // Do not guess attendance when the schedule data is invalid or unavailable.
    if (rows == null) {
        VolunteerDetailNotice("The daily schedule could not be verified. Refresh online or ask the organiser to check the dates and hours.")
        return
    }
    if (data == null) VolunteerDetailText("Attendance records are not loaded. Refresh online before checking your results.", secondary = true)
    val anomalies = rows.count { it.state == VolunteerAttendanceDayState.DATE_REVIEW }
    val outsideDates = data?.records.orEmpty().count { record -> rows.none { it.date == record.date } }
    if (anomalies + outsideDates > 0) VolunteerDetailNotice(
        "${anomalies + outsideDates} attendance date(s) need checking. Future or out-of-schedule records are not proof of attendance. No records have been changed.")
    val today = VolunteerAttendanceWindow.localDate(now, event.eventTimeZone)
    val visible = if (expanded) rows else VolunteerAttendanceHistory.preview(rows, today)
    visible.forEach { day ->
        val present = day.state == VolunteerAttendanceDayState.PRESENT
        val warning = day.state in setOf(VolunteerAttendanceDayState.ABSENT, VolunteerAttendanceDayState.DATE_REVIEW)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (present) Icons.Default.CheckCircle else if (warning) Icons.Default.Info else Icons.Default.Schedule,
                null, Modifier.size(20.dp), tint = if (present) VolunteerLinkSuccess else if (warning) VolunteerLinkWarning else VolunteerLinkTextSecondary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${VolunteerScheduleText.date(day.date)} · Day ${day.dayNumber}", fontSize = 14.sp,
                    lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = VolunteerLinkTextPrimary)
                VolunteerDetailText(when (day.state) {
                    VolunteerAttendanceDayState.PRESENT -> "Present"
                    VolunteerAttendanceDayState.ABSENT -> "Marked absent by organiser"
                    VolunteerAttendanceDayState.NOT_STARTED -> "Not started · Opens at ${VolunteerScheduleText.time(event.eventPhysicalStartTime)}"
                    VolunteerAttendanceDayState.NOT_SCHEDULED -> "Not scheduled for your role"
                    VolunteerAttendanceDayState.NO_RECORD -> "No check-in recorded"
                    VolunteerAttendanceDayState.NOT_OPENED -> "The organiser has not opened check-in"
                    VolunteerAttendanceDayState.OPEN -> "Check-in open · Enter the code above"
                    VolunteerAttendanceDayState.DATE_REVIEW -> "Date needs checking"
                    VolunteerAttendanceDayState.UNKNOWN -> "Status unavailable · Refresh online"
                })
                if (present || day.state == VolunteerAttendanceDayState.DATE_REVIEW) {
                    VolunteerDetailText("Recorded: ${VolunteerAttendanceHistory.recordedTime(day.recordedAt, event.eventTimeZone)}", secondary = true)
                }
                // A missing record can be appealed only during the 48-hour review period.
                if (day.ended && day.state in setOf(VolunteerAttendanceDayState.NO_RECORD, VolunteerAttendanceDayState.ABSENT)) {
                    if (day.withinReviewWindow) {
                        VolunteerDetailText("No check-in was recorded. Contact the organiser within 48 hours if this is an attendance error.", secondary = true)
                        TextButton(onClick = { onHelp(day.date) }, contentPadding = PaddingValues(vertical = 0.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = VolunteerLinkPrimaryGreen)) { Text("Contact organiser", fontSize = 14.sp) }
                    } else {
                        VolunteerDetailText("Review window closed. Only the organiser can correct an attendance error.", secondary = true)
                    }
                }
            }
        }
    }
    if (rows.size > 3 || outsideDates > 0) TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Show fewer days" else if (outsideDates > 0) "View all days and flagged records" else "View all ${rows.size} days", fontSize = 14.sp)
    }
    if (expanded && outsideDates > 0) data?.records.orEmpty().filter { record -> rows.none { it.date == record.date } }.forEach { record ->
        VolunteerDetailField("Outside current schedule · ${VolunteerScheduleText.date(record.date)}",
            "${record.status} · Recorded: ${VolunteerAttendanceHistory.recordedTime(record.recordedAt, event.eventTimeZone)}")
    }
    if (data?.records?.any { it.status == "PRESENT" } == true) VolunteerDetailText(
        "Recorded time may be a check-in or an organiser correction. Attendance does not automatically mean Completed.", secondary = true)
}
