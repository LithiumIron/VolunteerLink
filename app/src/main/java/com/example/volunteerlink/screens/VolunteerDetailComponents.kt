package com.example.volunteerlink.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.ui.theme.*
import com.example.volunteerlink.data.VolunteerScheduleText
import com.example.volunteerlink.model.VolunteerOpportunityEvent

/** Explicit type hierarchy: unaffected by an inherited bold LocalTextStyle. */
@Composable
internal fun VolunteerDetailCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VolunteerLinkSurface),
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = VolunteerLinkPrimaryGreen, modifier = Modifier.size(22.dp))
                Text(title, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkTextPrimary, modifier = Modifier.weight(1f))
            }
            content()
        }
    }
}

@Composable
// Purpose: Handles volunteer detail text as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
internal fun VolunteerDetailText(text: String, secondary: Boolean = false) {
    Text(text, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal,
        color = if (secondary) VolunteerLinkTextSecondary else VolunteerLinkTextPrimary)
}

@Composable
// Purpose: Handles volunteer detail field as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
internal fun VolunteerDetailField(label: String, value: String, icon: ImageVector? = null) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (icon != null) Icon(icon, null, Modifier.padding(top = 2.dp).size(18.dp), tint = VolunteerLinkTextSecondary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium, color = VolunteerLinkTextSecondary)
            VolunteerDetailText(value)
        }
    }
}

@Composable
// Purpose: Handles volunteer detail notice as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
internal fun VolunteerDetailNotice(text: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = VolunteerLinkSoftGreenSurface) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = VolunteerLinkPrimaryGreen)
            Column(Modifier.weight(1f)) { VolunteerDetailText(text) }
        }
    }
}

@Composable
// Purpose: Handles volunteer event schedule as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
internal fun VolunteerEventSchedule(event: VolunteerOpportunityEvent) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (event.eventPhysicalStartDate.isNotBlank()) {
            VolunteerDetailField("Physical phase", VolunteerScheduleText.range(event.eventPhysicalStartDate, event.eventPhysicalEndDate), Icons.Default.DateRange)
            VolunteerDetailField("Physical phase hours", "${VolunteerScheduleText.time(event.eventPhysicalStartTime)} – ${VolunteerScheduleText.time(event.eventPhysicalEndTime)}" +
                if (event.eventTimeZone != "Asia/Kuala_Lumpur") " (${event.eventTimeZone})" else "", Icons.Default.Schedule)
        }
        if (event.eventRemoteStartDate.isNotBlank()) {
            VolunteerDetailField("Remote work period", VolunteerScheduleText.range(event.eventRemoteStartDate, event.eventRemoteEndDate), Icons.Default.DateRange)
            VolunteerDetailField("Submit before · Malaysia time", VolunteerScheduleText.deadline(event.eventRemoteEndDate, true), Icons.Default.Schedule)
            if (event.eventRemoteOriginalEndDate.isNotBlank() && event.eventRemoteOriginalEndDate != event.eventRemoteEndDate)
                VolunteerDetailText("Extended from ${VolunteerScheduleText.date(event.eventRemoteOriginalEndDate)}", secondary = true)
        }
        if (event.eventPhysicalStartDate.isBlank() && event.eventRemoteStartDate.isBlank())
            VolunteerDetailNotice("Dates are unavailable. Sync online to check the latest schedule.")
        else VolunteerDetailText("Check each role for its assigned work dates and tasks.", secondary = true)
    }
}
