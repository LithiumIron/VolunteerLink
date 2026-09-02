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
        if (postId != parts[0] || roleId != parts[1]) {
            state.value.selected?.file?.delete()
            state.value = VolunteerRemoteUiState()
            ownerAuthId = null
        }
        postId = parts[0]
        roleId = parts[1]
        state.update { it.copy(busy = true, stage = "Checking project status…", error = null, message = null) }
        viewModelScope.launch {
            try {
                val uid = Repository.readyAccountId()
                com.example.volunteerlink.data.VolunteerOnline.requireConnection(getApplication<Application>(), "refresh submission status")
                if (ownerAuthId != null && ownerAuthId != uid) {
                    state.value.selected?.file?.delete()
                    state.update { it.copy(selected = null, context = null) }
                }
                ownerAuthId = null
                val loaded = Repository.load(postId, roleId)
                check(Repository.readyAccountId() == uid) { "Your account changed. Reopen this application." }
                ownerAuthId = uid
                state.update { it.copy(context = loaded) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                // Never show cached data as current authority after a failed check.
                state.update { it.copy(context = null, error = remoteError(e)) }
            } finally { state.update { it.copy(busy = false, stage = "") } }
        }
    }

    fun choose(uri: Uri) {
        if (state.value.busy) return
        if (!requireOnline("select a project file")) return
        state.update { it.copy(busy = true, stage = "Checking selected file…", error = null, message = null) }
        viewModelScope.launch {
            try {
                if (!checkAccount()) return@launch
                val file = Repository.prepare(getApplication<Application>(), uri)
                try {
                    if (!checkAccount()) {
                        file.file.delete()
                        return@launch
                    }
                } catch (e: Exception) {
                    file.file.delete()
                    throw e
                }
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
        if (state.value.busy) return false
        if (state.value.selected == null) {
            state.update { it.copy(error = "Choose a file before submitting.") }
            return false
        }
        return true
    }

    fun submit() {
        if (!requireOnline("submit project work")) return
        if (!validateSelection()) return
        val selected = state.value.selected ?: return
        state.update { it.copy(busy = true, stage = "Uploading file…", progress = 0f, error = null, message = null) }
        viewModelScope.launch {
            try {
                if (!checkAccount()) return@launch
                val uid = ownerAuthId ?: error("Refresh submission status before continuing.")
                Repository.submit(postId, roleId, selected, uid) { progress ->
                    state.update { it.copy(progress = progress,
                        stage = if (progress >= 1f) "Confirming submission…" else "Uploading file…") }
                }
                selected.file.delete()
                state.update { it.copy(selected = null, context = null,
                    message = "Project submitted successfully. Awaiting organisation review.",
                    successVersion = it.successVersion + 1) }
                try {
                    val loaded = Repository.load(postId, roleId)
                    check(Repository.readyAccountId() == uid) { "Your account changed. Reopen this application." }
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
        if (state.value.busy) return
        if (!requireOnline("open a submitted file")) return
        state.update { it.copy(busy = true, stage = "Opening file…", error = null) }
        viewModelScope.launch {
            try {
                if (!checkAccount()) return@launch
                val url = Repository.fileUrl(path)
                if (checkAccount()) onReady(url)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                state.update { it.copy(error = "The file could not be opened. Check your connection and try again.") }
            } finally { state.update { it.copy(busy = false, stage = "") } }
        }
    }

    private suspend fun checkAccount(): Boolean {
        val uid = Repository.readyAccountId()
        if (ownerAuthId == null) {
            // Revalidate server access before binding a session that was not ready earlier.
            val loaded = Repository.load(postId, roleId)
            check(Repository.readyAccountId() == uid) { "Your account changed. Reopen this application." }
            ownerAuthId = uid
            state.update { it.copy(context = loaded) }
        }
        if (uid != ownerAuthId) {
            state.value.selected?.file?.delete()
            state.value = VolunteerRemoteUiState(error = "Your account changed. Reopen this application.")
            return false
        }
        return true
    }

    private fun requireOnline(action: String): Boolean {
        if (com.example.volunteerlink.data.VolunteerOnline.available(getApplication<Application>())) return true
        state.update { it.copy(error = "Internet connection is required to $action. Connect and try again.") }
        return false
    }

    override fun onCleared() {
        state.value.selected?.file?.delete()
        super.onCleared()
    }
}

private fun remoteError(exception: Exception): String {
    val detail = exception.message.orEmpty()
    val knownMessages = listOf(
        "Your account changed. Reopen this application.",
        "Refresh submission status before continuing.",
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
