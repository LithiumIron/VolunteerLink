package com.example.volunteerlink.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.VolunteerRemoteContext
import com.example.volunteerlink.data.VolunteerRemoteSelectedFile
import com.example.volunteerlink.data.VolunteerRemoteSubmissionRepository as Repository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VolunteerRemoteUiState(
    val context: VolunteerRemoteContext? = null,
    val selected: VolunteerRemoteSelectedFile? = null,
    val busy: Boolean = false,
    val stage: String = "",
    val progress: Float = 0f,
    val error: String? = null,
    val message: String? = null,
    val successVersion: Int = 0
)

class VolunteerRemoteSubmissionViewModel(application: Application) : AndroidViewModel(application) {
    private val state = MutableStateFlow(VolunteerRemoteUiState())
    val uiState = state.asStateFlow()
    private var postId = ""
    private var roleId = ""
    private var ownerAuthId: String? = null

    fun load(participationId: String) {
        if (state.value.busy) return
        val parts = participationId.split('|')
        if (parts.size != 3 || parts.any(String::isBlank)) {
            state.update { it.copy(error = "Sync your applications before opening the submission.") }
            return
        }
        val uid = Repository.signedInId()
        if (ownerAuthId != uid || postId != parts[0] || roleId != parts[1]) {
            state.value.selected?.file?.delete()
            state.value = VolunteerRemoteUiState()
        }
        ownerAuthId = uid
        postId = parts[0]
        roleId = parts[1]
        state.update { it.copy(busy = true, stage = "Checking project status…", error = null, message = null) }
        viewModelScope.launch {
            try {
                val loaded = Repository.load(postId, roleId)
                state.update { it.copy(context = loaded) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                // Never show cached data as current authority after a failed check.
                state.update { it.copy(context = null, error = remoteError(e)) }
            } finally { state.update { it.copy(busy = false, stage = "") } }
        }
    }

    fun choose(uri: Uri) {
        if (state.value.busy || !checkAccount()) return
        state.update { it.copy(busy = true, stage = "Checking selected file…", error = null, message = null) }
        viewModelScope.launch {
            try {
                val file = Repository.prepare(getApplication<Application>(), uri)
                state.value.selected?.file?.delete()
                state.update { it.copy(selected = file) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { state.update { it.copy(error = remoteError(e)) }
            } finally { state.update { it.copy(busy = false, stage = "") } }
        }
    }

    fun removeFile() {
        if (state.value.busy) return
        state.value.selected?.file?.delete()
        state.update { it.copy(selected = null, error = null, message = null) }
    }

    fun validateSelection(): Boolean {
        if (state.value.busy || !checkAccount()) return false
        if (state.value.selected == null) {
            state.update { it.copy(error = "Choose a file before submitting.") }
            return false
        }
        return true
    }

    fun submit() {
        if (!validateSelection()) return
        val selected = state.value.selected ?: return
        state.update { it.copy(busy = true, stage = "Uploading file…", progress = 0f, error = null, message = null) }
        viewModelScope.launch {
            try {
                Repository.submit(postId, roleId, selected) { progress ->
                    state.update { it.copy(progress = progress,
                        stage = if (progress >= 1f) "Confirming submission…" else "Uploading file…") }
                }
                selected.file.delete()
                state.update { it.copy(selected = null, context = null,
                    message = "Project submitted successfully. Awaiting organisation review.",
                    successVersion = it.successVersion + 1) }
                try {
                    val loaded = Repository.load(postId, roleId)
                    state.update { it.copy(context = loaded) }
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) {
                    state.update { it.copy(error = "Submission was saved. Refresh to load the latest status.") }
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                state.update { it.copy(error = remoteError(e) + " Your file selection is kept; retry to confirm the result.") }
            } finally { state.update { it.copy(busy = false, stage = "") } }
        }
    }

    fun openFile(path: String, onReady: (String) -> Unit) {
        if (state.value.busy || !checkAccount()) return
        state.update { it.copy(busy = true, stage = "Opening file…", error = null) }
        viewModelScope.launch {
            try { onReady(Repository.fileUrl(path))
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                state.update { it.copy(error = "The file could not be opened. Check your connection and try again.") }
            } finally { state.update { it.copy(busy = false, stage = "") } }
        }
    }

    private fun checkAccount(): Boolean {
        if (ownerAuthId == null || Repository.signedInId() != ownerAuthId) {
            state.value.selected?.file?.delete()
            state.value = VolunteerRemoteUiState(error = "Your account changed. Reopen this application.")
            return false
        }
        return true
    }

    override fun onCleared() {
        state.value.selected?.file?.delete()
        super.onCleared()
    }
}

private fun remoteError(exception: Exception): String {
    val detail = exception.message.orEmpty()
    val knownMessages = listOf(
        "Choose a PDF, JPG, PNG, Word, Excel or PowerPoint file.",
        "The selected file is empty.", "The selected file exceeds the 20 MB limit.",
        "The selected file could not be opened. Choose it again.",
        "Only accepted volunteers can submit project work.",
        "Your participation has been finalized. No further submission is allowed.",
        "This project is no longer accepting submissions.",
        "The designated responsible role submits the shared team deliverable.",
        "Project submissions open on the project start date.",
        "The submission deadline has passed. Wait for the organisation to review or extend it.",
        "Your submitted work is awaiting organisation review.",
        "This work has been reviewed. Further submission is not allowed.",
        "This Remote participation was not found.",
        "File must be between 1 byte and 20 MB.",
        "The file type does not match its extension.", "Sign in before submitting."
    )
    knownMessages.firstOrNull { detail.contains(it, ignoreCase = true) }?.let { return it }
    if (detail.contains("PGRST202") || detail.contains("Could not find the function")) {
        return "Remote submission setup is not installed on the server yet."
    }
    // SDK exceptions may include request headers. Do not expose/log raw messages.
    return "Unable to complete the request. Check your internet connection and refresh the project status."
}
