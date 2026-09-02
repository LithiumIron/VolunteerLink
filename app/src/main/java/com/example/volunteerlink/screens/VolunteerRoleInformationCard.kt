package com.example.volunteerlink.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.data.VolunteerScheduleText
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import com.example.volunteerlink.ui.theme.*

@Composable
fun VolunteerRoleInformationCard(event: VolunteerOpportunityEvent, role: VolunteerOpportunityRole, includeTasks: Boolean = false) {
    val remote = role.roleMode.equals("REMOTE", true)
    val start = if (remote) event.eventRemoteStartDate else event.eventPhysicalStartDate
    val end = if (remote) event.eventRemoteEndDate else event.eventPhysicalEndDate
    var expanded by remember(event.eventId, role.roleId) { mutableStateOf(false) }
    val schedule = role.roleScheduleItems.sortedWith(compareBy({ it.rawDate }, { it.startTime }))
    VolunteerDetailCard("Your role arrangements", Icons.Default.DateRange) {
        VolunteerDetailField(if (remote) "Remote work period" else "Physical phase",
            VolunteerScheduleText.range(start, end), Icons.Default.DateRange)
        if (remote) {
            VolunteerDetailField("Submit before · Malaysia time", VolunteerScheduleText.deadline(end, true), Icons.Default.Schedule)
        } else {
            val dates = schedule.filter { it.assignedToRole && it.scheduleType.equals("PHYSICAL", true) }
                .map { it.rawDate }.filter(String::isNotBlank).distinct()
            if (dates.isNotEmpty()) VolunteerDetailField("Your scheduled dates", dates.joinToString { VolunteerScheduleText.date(it) })
            VolunteerDetailField("Location", event.eventFullAddress.ifBlank { event.eventLocation }, Icons.Default.LocationOn)
            if (event.eventMeetingPoint.isNotBlank()) VolunteerDetailField("Meeting point", event.eventMeetingPoint)
        }
        // My Applications shows the volunteer's work, not recruitment instructions.
        if (!includeTasks) VolunteerDetailField("Applications close · Malaysia time", VolunteerScheduleText.deadline(start))
        if (includeTasks && role.roleSpecificAssignment.isNotBlank()) {
            HorizontalDivider(color = VolunteerLinkBorderColour)
            VolunteerDetailField("Your task", role.roleSpecificAssignment)
        }
        if (role.roleSubmissionRequirement.isNotBlank()) VolunteerDetailField("Required deliverable", role.roleSubmissionRequirement)
        if (role.roleSubmissionInstruction.isNotBlank()) VolunteerDetailField("Submission instructions", role.roleSubmissionInstruction)
        if (includeTasks && schedule.isNotEmpty()) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = VolunteerLinkPrimaryGreen)) {
                Text(if (expanded) "Hide work schedule" else "View work schedule (${schedule.size})",
                    fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            if (expanded) schedule.forEach { item ->
                HorizontalDivider(color = VolunteerLinkBorderColour)
                VolunteerDetailField(item.scheduleDate.ifBlank { VolunteerScheduleText.date(item.rawDate) }, item.scheduleTime)
                VolunteerDetailText(item.scheduleActivity)
                if (item.location.isNotBlank()) VolunteerDetailField("Place", item.location)
                if (item.notes.isNotBlank() && item.notes != item.scheduleActivity) VolunteerDetailText(item.notes, secondary = true)
            }
        }
    }
}
