package com.example.volunteerlink.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.data.*
import com.example.volunteerlink.model.*
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException

@Composable
fun VolunteerApplicationPreviewDialog(event: VolunteerOpportunityEvent, role: VolunteerOpportunityRole,
    answers: List<String>, busy: Boolean, actionError: String?, onBack: () -> Unit, onConfirm: () -> Unit) {
    val context = LocalContext.current
    val account = supabase.auth.currentUserOrNull()?.id.orEmpty()
    var preview by remember(account) { mutableStateOf<VolunteerApplicationPreview?>(null) }
    var error by remember(account) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var evidence by remember(account) { mutableStateOf<Map<String, VolunteerPreviewEvidence>?>(null) }
    var evidenceError by remember(account) { mutableStateOf<String?>(null) }
    LaunchedEffect(preview, retry) {
        evidence = null
        evidenceError = null
        val loaded = preview ?: return@LaunchedEffect
        try { evidence = VolunteerApplicationPreviewRepository.evidence(context, account, loaded.profile.userId) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { evidenceError = "Verified experience could not be loaded. Connect and retry; this does not mean you have no experience." }
    }
    LaunchedEffect(account, retry) {
        error = null
        preview = null
        try { preview = VolunteerApplicationPreviewRepository.load(context, account) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            error = if (e is IllegalStateException) e.message
                else "Your profile could not be loaded. Check your connection and retry. Nothing has been submitted."
        }
    }
    AlertDialog(onDismissRequest = { if (!busy) onBack() }, containerColor = Color.White,
        title = { Text("Review your application", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(event.eventTitle, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                VolunteerDetailField("Role", role.roleTitle)
                VolunteerDetailText(VolunteerScheduleText.compact(event, role), secondary = true)
                HorizontalDivider()
                Text("Profile for organisation review", fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                preview?.let {
                    VolunteerDetailField("Name", it.profile.name)
                    VolunteerDetailField("City", it.profile.city?.takeIf(String::isNotBlank) ?: "Not provided")
                    VolunteerDetailField("About you", it.profile.bio?.takeIf(String::isNotBlank) ?: "Not provided")
                    VolunteerDetailText(if (it.cached) "Offline profile copy. No place is reserved until your request reaches the server."
                         else "Your current profile and answers are available for organisation review.", secondary = true)
                }
                HorizontalDivider()
                VolunteerDetailField("Relevant Skill Path", role.rolePrimarySkillPath)
                VolunteerDetailText("Required: Level ${role.roleMinimumSkillPathLevel}", secondary = true)
                val relevant = evidence?.entries?.firstOrNull { it.key.equals(role.rolePrimarySkillPath, true) }?.value
                if (relevant != null) {
                    VolunteerDetailText("Your level: ${relevant.level}")
                    VolunteerDetailText("${relevant.assignments} verified roles · ${relevant.minutes?.let { minutes -> "${minutes / 60}h ${minutes % 60}m" } ?: "Time not recorded"}")
                    VolunteerDetailText("This is your recorded experience, not skills you will earn from this role. Organisation review screens may show different profile fields.", secondary = true)
                } else if (evidence != null) VolunteerDetailText("No verified progress record was returned for this path.", secondary = true)
                else if (preview != null && evidenceError == null) VolunteerDetailText("Loading verified experience…", secondary = true)
                evidenceError?.let { VolunteerDetailText(it, secondary = true) }
                if (preview == null && error == null) { CircularProgressIndicator(); Text("Loading your profile…") }
                error?.let { Text(it, color = Color(0xFFB3261E)); TextButton(onClick = { retry++ }) { Text("Retry") } }
                HorizontalDivider()
                if (role.roleExtraApplicationQuestions.isNotEmpty()) Text("Your answers", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                role.roleExtraApplicationQuestions.forEachIndexed { index, question ->
                    Text("${index + 1}. $question", fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)
                    VolunteerDetailText(answers.getOrElse(index) { "Not answered" })
                }
                VolunteerDetailNotice(if (preview?.cached == true)
                    "Confirming saves a request on this device. Connect and Sync to submit it for review. It does not mean you have been accepted."
                    else "Confirming sends an application for review. It does not mean you have been accepted.")
                actionError?.let { Text(it, color = Color(0xFFB3261E)) }
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = preview != null && !busy,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A4A1E))) {
            Text(if (busy) "Submitting…" else "Confirm application", fontSize = 14.sp)
        } }, dismissButton = { TextButton(onClick = onBack, enabled = !busy) {
            Text(if (role.roleExtraApplicationQuestions.isEmpty()) "Back" else "Back to answers", fontSize = 14.sp)
        } })
}
