package com.example.volunteerlink.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import com.example.volunteerlink.ui.theme.*
import com.example.volunteerlink.data.*
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.model.*
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VolunteerAttendanceCard(event: VolunteerOpportunityEvent, role: VolunteerOpportunityRole,
                            application: VolunteerOpportunityApplication) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = supabase.auth.currentUserOrNull()?.id.orEmpty()
    var data by remember(event.eventId, role.roleId, account) { mutableStateOf<VolunteerAttendanceData?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pin by remember(event.eventId, role.roleId, account) { mutableStateOf("") }
    var message by remember(event.eventId, role.roleId, account) { mutableStateOf<String?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    var helpDate by remember(event.eventId, role.roleId, account) { mutableStateOf("") }
    val now by produceState(AppClock.nowMillis()) { while (true) { value = AppClock.nowMillis(); delay(2000) } }
    val online by produceState(VolunteerOnline.available(context)) {
        while (true) { value = VolunteerOnline.available(context); delay(2000) }
    }
    val today = runCatching { VolunteerAttendanceWindow.localDate(now, event.eventTimeZone) }.getOrDefault("")
    suspend fun reload() {
        VolunteerOnline.requireConnection(context, "refresh attendance")
        data = VolunteerAttendanceRepository.load(event.eventDatabaseId, role.roleTemplateId, account)
    }
    fun refresh() {
        if (busy) return
        busy = true
        scope.launch {
            try { reload(); message = null }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { data = null; message = VolunteerAttendanceRepository.message(e) }
            finally { busy = false }
        }
    }
    LaunchedEffect(event.eventId, role.roleId, account, online) { if (online) refresh() }
    val existing = data?.records?.firstOrNull { it.date == today }
    val block = when {
        !online -> "Internet connection is required to check in. Offline check-in is not recorded or queued."
        application.applicationStatus != VolunteerApplicationStatus.ACCEPTED -> "Check-in is only available for an accepted, unfinished Physical role."
        existing?.status == "ABSENT" -> "The organisation marked you absent today. Contact them to request a correction."
        existing?.status == "PRESENT" -> "Your attendance is recorded for today. Completion still requires organisation review."
        VolunteerAttendanceWindow.reason(event, role, now) != null -> VolunteerAttendanceWindow.reason(event, role, now)
        data == null -> "Refresh attendance online before checking in."
        data?.days?.none { it.date == today && it.active } == true -> "The organisation has not opened today's attendance. Ask them, then refresh."
        else -> null
    }
    ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)) {
        VolunteerDetailCard("Attendance", Icons.Default.CheckCircle) {
            VolunteerDetailField("Today", VolunteerScheduleText.date(today))
            if (AppClock.isUsingTestTime()) VolunteerDetailNotice("Test time is active. This is not today's real date. Check-in uses the test clock.")
            VolunteerDetailField("Check-in hours", "${VolunteerScheduleText.time(event.eventPhysicalStartTime)} – ${VolunteerScheduleText.time(event.eventPhysicalEndTime)}" +
                if (event.eventTimeZone != "Asia/Kuala_Lumpur") " (${event.eventTimeZone})" else "", Icons.Default.Schedule)
            block?.let { VolunteerDetailNotice(it) }
            if (block == null) {
                OutlinedTextField(value = pin, onValueChange = { pin = it.filter { digit -> digit in '0'..'9' }.take(6); message = null },
                    label = { Text("Six-digit attendance code") }, singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                VolunteerDetailText("Get the code from your organiser. Internet is required.", secondary = true)
                Button(onClick = {
                    if (pin.length != 6) message = "Enter all six digits of the attendance code."
                    else if (!busy) {
                        busy = true
                        scope.launch {
                            var recorded = false
                            try {
                                VolunteerOnline.requireConnection(context, "check in")
                                VolunteerAttendanceRepository.checkIn(event.eventDatabaseId, role.roleTemplateId, pin, account)
                                recorded = true
                                pin = ""
                                message = "Attendance recorded. Completion still requires organisation review."
                                reload()
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) {
                                message = if (recorded)
                                    "Attendance was recorded, but the latest list could not be loaded. Refresh attendance. Completion still requires organisation review."
                                    else VolunteerAttendanceRepository.message(e)
                                data = null
                            }
                            finally { busy = false }
                        }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A4A1E))) { Text("Confirm check-in") }
                VolunteerDetailText("Check-in records attendance. Your organiser confirms role completion later.", secondary = true)
            }
            if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Checking attendance…") }
            message?.let { Text(it, color = Color(0xFF1A1A1A)) }
            TextButton(onClick = ::refresh, enabled = !busy,
                colors = ButtonDefaults.textButtonColors(contentColor = VolunteerLinkPrimaryGreen)) { Text("Refresh status") }
            VolunteerAttendanceHistorySection(event, role, data, now) { date ->
                helpDate = date
                showHelp = true
            }
        }
    }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, containerColor = Color.White,
        title = { Text("Contact your organiser", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
        text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VolunteerDetailText("Tell them your name, role, missed date and reason. They must check whether you attended.")
            VolunteerDetailField("Event", event.eventTitle)
            VolunteerDetailField("Role", role.roleTitle)
            VolunteerDetailField("Attendance date", VolunteerScheduleText.date(helpDate))
            VolunteerDetailNotice("This opens your email or phone app. It does not submit an in-app request or change your attendance. Finalised records may be locked.")
            VolunteerDetailField("Email", event.eventContactEmail.ifBlank { "Not provided" })
            VolunteerDetailField("Phone", event.eventContactPhone.ifBlank { "Not provided" })
        } },
        confirmButton = {
            TextButton(onClick = {
                val intent = when {
                    event.eventContactEmail.isNotBlank() -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${event.eventContactEmail}"))
                    event.eventContactPhone.isNotBlank() -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${event.eventContactPhone}"))
                    else -> null
                }
                if (intent == null) message = "No contact details were provided. Contact the event coordinator directly."
                else runCatching { context.startActivity(intent) }.onFailure { message = "No compatible contact app is available on this device." }
                showHelp = false
            }) { Text("Contact") }
        }, dismissButton = { TextButton(onClick = { showHelp = false }) { Text("Back") } })
}
