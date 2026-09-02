package com.example.volunteerlink.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import com.example.volunteerlink.data.VolunteerScheduleText
import com.example.volunteerlink.data.VolunteerOnline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.data.VolunteerRemoteFileRules
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import java.util.Locale
import java.text.SimpleDateFormat

@Composable
fun VolunteerRemoteSubmissionCard(
    participationId: String,
    dataVersion: Int,
    onRefreshApplications: () -> Unit
) {
    val model: VolunteerRemoteSubmissionViewModel = viewModel(key = "remote-submission-$participationId")
    val state by model.uiState.collectAsStateWithLifecycle()
    val androidContext = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var confirm by remember(participationId) { mutableStateOf(false) }
    var showHistory by remember(participationId) { mutableStateOf(false) }
    var connectionMessage by remember(participationId) { mutableStateOf<String?>(null) }
    var pendingFileUri by remember(participationId) { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingFileUri = uri
    }

    // A resume-triggered refresh and the picker result can arrive together.
    // Queue the selection instead of silently dropping it while loading.
    LaunchedEffect(pendingFileUri, state.busy) {
        val uri = pendingFileUri
        if (uri != null && !state.busy) {
            pendingFileUri = null
            model.choose(uri)
        }
    }

    LaunchedEffect(participationId, dataVersion) { model.load(participationId) }
    LaunchedEffect(state.successVersion) {
        if (state.successVersion > 0) onRefreshApplications()
    }
    DisposableEffect(lifecycle, participationId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) model.load(participationId)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)) {
        VolunteerDetailCard("Project submission", Icons.Default.Description) {
            val project = state.context
            if (project != null) {
                VolunteerDetailField("Submit before · Malaysia time", VolunteerScheduleText.deadline(project.deadline, true), Icons.Default.Schedule)
                Text(if (project.mode == "SHARED_TEAM") "Shared team deliverable" else "Individual deliverable",
                    fontWeight = FontWeight.SemiBold, color = VolunteerLinkTextSecondary)
                if (project.requirement.isNotBlank()) Text(project.requirement, color = VolunteerLinkTextPrimary)
                if (project.completionStatus in listOf("COMPLETED", "NOT_COMPLETED")) {
                    Text(if (project.completionStatus == "COMPLETED") "Participation completed" else "Participation not completed",
                        fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
                }
                if (project.reason.isNotBlank()) Text(project.reason, color = VolunteerLinkTextSecondary)
                if (project.canSubmit) {
                    VolunteerDetailText("1 file · Maximum 20 MB · Internet required", secondary = true)
                    VolunteerDetailText("PDF, images (JPG/PNG), Word, Excel or PowerPoint.", secondary = true)
                    project.history.firstOrNull()?.takeIf { it.status == "REVISION_REQUESTED" }?.let { latest ->
                        Text("Revision requested", fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
                        Text(latest.feedback.orEmpty(), color = VolunteerLinkTextPrimary)
                    }
                    OutlinedButton(
                        onClick = {
                            if (!VolunteerOnline.available(androidContext)) connectionMessage = "Connect to the internet before choosing and submitting your file."
                            else { connectionMessage = null; picker.launch(VolunteerRemoteFileRules.mimeTypes.values.distinct().toTypedArray()) }
                        },
                        enabled = !state.busy, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VolunteerLinkPrimaryGreen)
                    ) { Text(if (state.selected == null) "Choose file" else "Choose another file") }
                }
            }
            state.selected?.let { selected ->
                Text(selected.displayName, fontWeight = FontWeight.SemiBold, color = VolunteerLinkTextPrimary)
                Text(String.format(Locale.US, "%.2f MB", selected.file.length() / 1_000_000.0), color = VolunteerLinkTextSecondary)
                TextButton(onClick = model::removeFile, enabled = !state.busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = VolunteerLinkPrimaryGreen)) { Text("Remove selected file") }
            }
            state.error?.let { Text(it, color = VolunteerLinkError) }
            connectionMessage?.let { VolunteerDetailNotice(it) }
            state.message?.let { Text(it, color = VolunteerLinkPrimaryGreen) }
            if (state.busy) {
                Text(state.stage, color = VolunteerLinkTextSecondary)
                if (state.stage == "Uploading file…") {
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth(), color = VolunteerLinkPrimaryGreen)
                    Text("${(state.progress * 100).toInt()}%", color = VolunteerLinkTextSecondary)
                } else LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = VolunteerLinkPrimaryGreen)
                VolunteerDetailText("Keep the app open until confirmation.", secondary = true)
            }
            if (project?.canSubmit == true) {
                VolunteerDetailText("You can resubmit only if the organiser requests a revision.", secondary = true)
                Button(
                    onClick = { if (model.validateSelection()) confirm = true }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen, contentColor = Color.White)
                ) { Text(if (project.history.firstOrNull()?.status == "REVISION_REQUESTED") "Resubmit project" else "Submit project") }
            }
            TextButton(onClick = { model.load(participationId); onRefreshApplications() }, enabled = !state.busy,
                colors = ButtonDefaults.textButtonColors(contentColor = VolunteerLinkPrimaryGreen)) { Text("Refresh submission status") }

            if (!project?.history.isNullOrEmpty()) {
                HorizontalDivider(color = VolunteerLinkBorderColour)
                TextButton(onClick = { showHistory = !showHistory }) { Text(if (showHistory) "Hide previous submissions" else "View previous submissions") }
                project?.history?.take(if (showHistory) Int.MAX_VALUE else 1)?.forEachIndexed { index, record ->
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text((if (index == 0) "Latest · " else "Earlier · ") + when (record.status) {
                            "PENDING_REVIEW" -> "Awaiting review"
                            "REVISION_REQUESTED" -> "Revision requested"
                            "ACCEPTED" -> "Work accepted"
                            "NOT_ACCEPTED" -> "Work not accepted"
                            else -> record.status
                        }, fontWeight = FontWeight.SemiBold, color = VolunteerLinkTextPrimary)
                        record.submittedAt?.let { Text("Submitted: ${remoteSubmissionTime(it)}", fontSize = 12.sp, color = VolunteerLinkTextSecondary) }
                        record.feedback?.takeIf(String::isNotBlank)?.let { Text(it, color = VolunteerLinkTextPrimary) }
                        record.filePath?.takeIf(String::isNotBlank)?.let { path ->
                            Text(path.substringAfterLast('/'), color = VolunteerLinkTextSecondary)
                            TextButton(onClick = {
                                model.openFile(path) { url ->
                                    runCatching { androidContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                        .onFailure { connectionMessage = "No app could open this file. Install a compatible viewer and try again." }
                                }
                            }, enabled = !state.busy,
                                colors = ButtonDefaults.textButtonColors(contentColor = VolunteerLinkPrimaryGreen)) { Text("Open file (online)") }
                        }
                    }
                }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false }, containerColor = Color.White,
            titleContentColor = VolunteerLinkTextPrimary, textContentColor = VolunteerLinkTextSecondary,
            title = { Text("Submit project?") },
            text = { Text("${state.selected?.displayName.orEmpty()}\n\nThis file will be sent for organisation review. Keep the app open until submission is confirmed.") },
            confirmButton = {
                TextButton(onClick = { confirm = false; model.submit() },
                    colors = ButtonDefaults.textButtonColors(contentColor = VolunteerLinkPrimaryGreen)) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = VolunteerLinkTextSecondary)) { Text("Back") }
            }
        )
    }
}

private fun remoteSubmissionTime(value: String): String = runCatching {
    // PostgreSQL may emit microseconds; java.text expects exactly milliseconds.
    val normalized = value.replace(Regex("\\.(\\d+)(?=Z|[+-])")) {
        "." + it.groupValues[1].take(3).padEnd(3, '0')
    }
    val pattern = if (normalized.contains('.')) "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" else "yyyy-MM-dd'T'HH:mm:ssXXX"
    val instant = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(normalized)
        ?: return@runCatching value
    SimpleDateFormat("dd MMM yyyy, h:mm a z", Locale.getDefault()).format(instant)
}.getOrDefault(value)
