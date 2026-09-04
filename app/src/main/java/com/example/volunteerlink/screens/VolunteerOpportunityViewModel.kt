
package com.example.volunteerlink.screens

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.data.VolunteerDashboardDataSource
import com.example.volunteerlink.data.VolunteerOpportunityRepository
import com.example.volunteerlink.data.VolunteerEventContactRepository
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VolunteerEventPhoneContactUiState(
    val postId: String = "",
    val roleTemplateId: String = "",
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val eligible: Boolean = false,
    val enabled: Boolean = false,
    val availableUntilLabel: String? = null,
    val errorMessage: String? = null,
    val reason: String? = null
)

data class VolunteerOpportunityUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isApplicationActionRunning: Boolean = false,
    val errorMessage: String? = null,
    val applicationActionError: String? = null,
    val lastApplicationResult: String? = null,
    val syncWarning: String? = null,
    val isShowingCachedData: Boolean = false,
    val lastSyncedAtEpochMillis: Long? = null,
    val eventPhoneContact: VolunteerEventPhoneContactUiState = VolunteerEventPhoneContactUiState(),
    val dataVersion: Int = 0
)

class VolunteerOpportunityViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val mutableUiState =
        MutableStateFlow(VolunteerOpportunityUiState())

    val uiState: StateFlow<VolunteerOpportunityUiState> =
        mutableUiState.asStateFlow()

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
    private var initialNetworkCallbackReceived = false
    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (initialNetworkCallbackReceived) {
                    refresh()
                } else {
                    initialNetworkCallbackReceived = true
                }
            }
        }

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        loadDashboard()
    }

    override fun onCleared() {
        runCatching {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
        super.onCleared()
    }

    fun retry() {
        loadDashboard()
    }

    fun refresh() {
        loadDashboard(isRefresh = true)
    }

    fun loadEventPhoneContact(
        postId: String,
        roleTemplateId: String
    ) {
        if (postId.isBlank() || roleTemplateId.isBlank()) return

        mutableUiState.update {
            it.copy(
                eventPhoneContact = VolunteerEventPhoneContactUiState(
                    postId = postId,
                    roleTemplateId = roleTemplateId,
                    isLoading = true
                )
            )
        }

        viewModelScope.launch {
            try {
                val status = VolunteerEventContactRepository.loadPhoneContactStatus(
                    postId = postId,
                    roleTemplateId = roleTemplateId
                )
                mutableUiState.update { current ->
                    if (
                        current.eventPhoneContact.postId != postId ||
                        current.eventPhoneContact.roleTemplateId != roleTemplateId
                    ) {
                        current
                    } else {
                        current.copy(
                            eventPhoneContact = current.eventPhoneContact.copy(
                                isLoading = false,
                                eligible = status.eligible,
                                enabled = status.enabled,
                                availableUntilLabel = status.availableUntilLabel,
                                errorMessage = null,
                                reason = status.reason
                            )
                        )
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
                mutableUiState.update { current ->
                    if (
                        current.eventPhoneContact.postId != postId ||
                        current.eventPhoneContact.roleTemplateId != roleTemplateId
                    ) {
                        current
                    } else {
                        current.copy(
                            eventPhoneContact = current.eventPhoneContact.copy(
                                isLoading = false,
                                errorMessage = "Phone contact preference could not be loaded. Please try again."
                            )
                        )
                    }
                }
            }
        }
    }

    fun setEventPhoneContactEnabled(
        postId: String,
        roleTemplateId: String,
        enabled: Boolean
    ) {
        val contactState = mutableUiState.value.eventPhoneContact
        if (
            contactState.postId != postId ||
            contactState.roleTemplateId != roleTemplateId ||
            contactState.isUpdating
        ) return

        mutableUiState.update {
            it.copy(
                eventPhoneContact = it.eventPhoneContact.copy(
                    isUpdating = true,
                    errorMessage = null
                )
            )
        }

        viewModelScope.launch {
            try {
                val status = VolunteerEventContactRepository.setPhoneContactEnabled(
                    postId = postId,
                    roleTemplateId = roleTemplateId,
                    enabled = enabled
                )
                mutableUiState.update { current ->
                    if (
                        current.eventPhoneContact.postId != postId ||
                        current.eventPhoneContact.roleTemplateId != roleTemplateId
                    ) {
                        current
                    } else {
                        current.copy(
                            eventPhoneContact = current.eventPhoneContact.copy(
                                isUpdating = false,
                                eligible = status.eligible,
                                enabled = status.enabled,
                                availableUntilLabel = status.availableUntilLabel,
                                errorMessage = null,
                                reason = status.reason
                            )
                        )
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
                mutableUiState.update { current ->
                    if (
                        current.eventPhoneContact.postId != postId ||
                        current.eventPhoneContact.roleTemplateId != roleTemplateId
                    ) {
                        current
                    } else {
                        current.copy(
                            eventPhoneContact = current.eventPhoneContact.copy(
                                isUpdating = false,
                                errorMessage = exception.message
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "Phone contact preference could not be updated."
                            )
                        )
                    }
                }
            }
        }
    }

    fun clearEventPhoneContact() {
        mutableUiState.update {
            it.copy(eventPhoneContact = VolunteerEventPhoneContactUiState())
        }
    }

    fun setOpportunitySaved(eventId: Int, shouldSave: Boolean) {
        val event = VolunteerOpportunitySessionStore.findEventById(eventId)
        if (event == null) {
            mutableUiState.update {
                it.copy(applicationActionError = "Opportunity could not be found.")
            }
            return
        }

        viewModelScope.launch {
            VolunteerOpportunitySessionStore.setEventSaved(eventId, shouldSave)
            VolunteerDashboardDataSource.cacheCurrentSession()
            mutableUiState.update {
                it.copy(
                    applicationActionError = null,
                    dataVersion = it.dataVersion + 1
                )
            }

            try {
                VolunteerOpportunityRepository.setOpportunitySaved(
                    event.eventDatabaseId,
                    shouldSave
                )
            } catch (exception: Exception) {
                if (exception.isConnectivityFailure()) {
                    VolunteerDashboardDataSource.enqueuePendingAction(
                        actionType = "SET_SAVED",
                        targetId = event.eventDatabaseId,
                        payloadJson = buildJsonObject {
                            put("should_save", shouldSave)
                        }.toString()
                    )
                } else {
                    VolunteerOpportunitySessionStore.setEventSaved(
                        eventId,
                        !shouldSave
                    )
                    VolunteerDashboardDataSource.cacheCurrentSession()
                    mutableUiState.update {
                        it.copy(
                            applicationActionError =
                                "Favourites could not be updated. Please retry.",
                            dataVersion = it.dataVersion + 1
                        )
                    }
                }
            }
        }
    }

    fun submitApplication(
        eventId: Int,
        roleId: Int,
        answers: List<String>,
        confirmedPrevious: VolunteerOpportunityApplication? = null,
        onSuccess: () -> Unit
    ) {
        if (mutableUiState.value.isApplicationActionRunning) return
        val event = VolunteerOpportunitySessionStore.findEventById(eventId)
        val role = VolunteerOpportunitySessionStore.findRoleById(eventId, roleId)
        if (event == null || role == null) {
            mutableUiState.update { it.copy(applicationActionError = "Role details are unavailable. Sync and try again.") }
            return
        }
        if (!com.example.volunteerlink.data.VolunteerApplicationWindow.canApply(event, role)) {
            mutableUiState.update { it.copy(applicationActionError =
                com.example.volunteerlink.data.VolunteerApplicationWindow.reason(event, role)) }
            return
        }
        val other = VolunteerOpportunitySessionStore.activeApplicationForEvent(eventId)
            ?.takeIf { it.applicationRoleId != roleId }
        if (other != confirmedPrevious) {
            mutableUiState.update { it.copy(applicationActionError = "Your current role changed. Reopen this role and review the change before confirming.") }
            return
        }
        if (other != null && (other.applicationCreatedAtRaw.isBlank() ||
            other.applicationDatabaseId.startsWith("offline|"))) {
            mutableUiState.update { it.copy(applicationActionError = "Sync your current application before changing roles.") }
            return
        }
        val previousRole = other?.applicationRoleId?.let { VolunteerOpportunitySessionStore.findRoleById(eventId, it) }
        if (other != null && !com.example.volunteerlink.data.VolunteerApplicationWindow.beforeStart(event, previousRole)) {
            mutableUiState.update { it.copy(applicationActionError = "Your current role has started. It can no longer be cancelled or changed.") }
            return
        }
        val reapply = VolunteerOpportunitySessionStore.volunteerApplications.any {
            it.applicationEventId == eventId && it.applicationRoleId == roleId &&
                it.applicationStatus == VolunteerApplicationStatus.CANCELLED
        }
        val onlineOnly = other != null || reapply
        mutableUiState.update { it.copy(isApplicationActionRunning = true, applicationActionError = null, lastApplicationResult = null) }
        viewModelScope.launch {
            try {
                val result = com.example.volunteerlink.data.VolunteerApplicationActions.submit(
                    getApplication<Application>(), event.eventDatabaseId, role.roleTemplateId, answers,
                    previousRole?.roleTemplateId, other?.applicationStatus?.name, other?.applicationCreatedAtRaw,
                    onlineOnly
                )
                if (result == null) {
                    VolunteerOpportunitySessionStore.addOfflinePendingApplication(
                        VolunteerOpportunityApplication(
                            applicationId = ("offline|" + role.roleDatabaseId).hashCode(),
                            applicationEventId = eventId, applicationEventTitle = event.eventTitle,
                            applicationOrganisationName = event.eventOrganisationName,
                            applicationRoleTitle = role.roleTitle,
                            applicationSubmittedDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date()),
                            applicationStatus = VolunteerApplicationStatus.PENDING,
                            applicationRoleId = roleId, applicationRoleMode = role.roleMode,
                            applicationStatusMessage = "Waiting to sync. Not sent for review and no place reserved. Connect and Sync to receive the server result.",
                            applicationScreeningQuestions = role.roleExtraApplicationQuestions,
                            applicationScreeningAnswers = answers,
                            applicationDatabaseId = "offline|" + role.roleDatabaseId
                        )
                    )
                    VolunteerDashboardDataSource.cacheCurrentSession()
                    finishLocalAction()
                    mutableUiState.update { it.copy(lastApplicationResult = "Waiting to sync. Nothing has been accepted yet. No place is reserved. Connect and Sync to receive your application result.") }
                    onSuccess()
                } else if (!result.success) {
                    mutableUiState.update { it.copy(isApplicationActionRunning = false, applicationActionError =
                        result.message + if (other != null) " This attempt did not cancel your previous role." else "") }
                } else {
                    mutableUiState.update { it.copy(lastApplicationResult = result.message) }
                    try { refreshAfterAction() }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) {
                        mutableUiState.update { it.copy(isApplicationActionRunning = false,
                            applicationActionError = result.message + " Refresh to load your latest application.", isShowingCachedData = true) }
                    }
                    onSuccess()
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                val detail = if (e is IllegalStateException) e.message.orEmpty()
                    else "We could not confirm the server result. Connect and Sync before trying again. Do not assume the role was joined or cancelled."
                mutableUiState.update { it.copy(isApplicationActionRunning = false, applicationActionError = detail) }
            }
        }
    }

    fun cancelApplication(
        applicationId: Int,
        reason: String,
        details: String,
        onSuccess: () -> Unit = {}
    ) {
        if (mutableUiState.value.isApplicationActionRunning) return
        if (!com.example.volunteerlink.data.VolunteerOnline.available(getApplication<Application>())) {
            mutableUiState.update { it.copy(applicationActionError = "Internet connection is required to cancel an application. Your place has not been released.") }
            return
        }
        if (reason.isBlank() || (reason == "Other" && details.isBlank())) {
            mutableUiState.update { it.copy(applicationActionError =
                if (reason.isBlank()) "Please select a cancellation reason." else "Please enter your reason.") }
            return
        }
        val application =
            VolunteerOpportunitySessionStore
                .findApplicationById(applicationId)

        if (application == null) {
            mutableUiState.update {
                it.copy(
                    applicationActionError =
                        "The selected application could not be found."
                )
            }
            return
        }

        val event = VolunteerOpportunitySessionStore.findEventById(application.applicationEventId)
        val role = application.applicationRoleId?.let { roleId ->
            VolunteerOpportunitySessionStore.findRoleById(
                application.applicationEventId,
                roleId
            )
        }
        if (!com.example.volunteerlink.data.VolunteerApplicationWindow.beforeStart(event, role)) {
            mutableUiState.update { it.copy(applicationActionError =
                "Cancellation is unavailable: this role has started or its dates need syncing.") }
            return
        }

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isApplicationActionRunning = true,
                    applicationActionError = null
                )
            }

            try {
                VolunteerOpportunityRepository.cancelApplication(
                    application.applicationDatabaseId,
                    reason,
                    details
                )
                refreshAfterAction()
                onSuccess()
            } catch (exception: Exception) {
                exception.printStackTrace()
                mutableUiState.update {
                    it.copy(
                        isApplicationActionRunning = false,
                        applicationActionError =
                            exception.toVolunteerMessage(
                                fallback =
                                    "Application could not be cancelled."
                            )
                    )
                }
            }
        }
    }

    fun updatePendingApplication(
        applicationId: Int,
        answers: List<String>,
        onSuccess: () -> Unit = {}
    ) = runApplicationAction(applicationId) { application ->
        try {
            VolunteerOpportunityRepository.updatePendingApplication(
                application.applicationDatabaseId,
                answers
            )
            refreshAfterAction()
            onSuccess()
        } catch (exception: kotlinx.coroutines.CancellationException) { throw exception
        } catch (exception: Exception) {
            failApplicationAction(exception, "Unable to confirm your changes. Sync before retrying.")
        }
    }

    fun deleteApplication(
        applicationId: Int,
        onSuccess: () -> Unit = {}
    ) = runApplicationAction(applicationId) { application ->
        try {
            VolunteerOpportunityRepository.deleteApplication(
                application.applicationDatabaseId
            )
            refreshAfterAction()
            onSuccess()
        } catch (exception: Exception) {
            failApplicationAction(
                exception,
                if (exception.isConnectivityFailure())
                    "Internet connection is required to delete an application record."
                else "Application record could not be deleted."
            )
        }
    }

    fun reapplyForRole(
        applicationId: Int,
        answers: List<String>,
        onSuccess: () -> Unit = {}
    ) = runApplicationAction(applicationId) { application ->
        val event = VolunteerOpportunitySessionStore.findEventById(
            application.applicationEventId
        )
        val role = application.applicationRoleId?.let {
            VolunteerOpportunitySessionStore.findRoleById(
                application.applicationEventId,
                it
            )
        }
        if (event == null || role == null) {
            failApplicationAction(IllegalStateException(), "Role details could not be found.")
            return@runApplicationAction
        }
        if (application.applicationStatus != VolunteerApplicationStatus.CANCELLED) {
            failApplicationAction(
                IllegalStateException(),
                if (application.applicationStatus == VolunteerApplicationStatus.REJECTED) {
                    "You were not selected for this role. You may choose another open role in this opportunity."
                } else {
                    "Only a cancelled application can be submitted again."
                }
            )
            return@runApplicationAction
        }
        if (!com.example.volunteerlink.data.VolunteerApplicationWindow.canApply(event, role)) {
            failApplicationAction(IllegalStateException(),
                com.example.volunteerlink.data.VolunteerApplicationWindow.reason(event, role))
            return@runApplicationAction
        }

        val otherActiveApplication =
            VolunteerOpportunitySessionStore.activeApplicationForEvent(
                application.applicationEventId
            )?.takeIf { active -> active.applicationRoleId != application.applicationRoleId }

        if (otherActiveApplication != null) {
            val message = if (
                otherActiveApplication.applicationStatus == VolunteerApplicationStatus.ACCEPTED
            ) {
                "You already joined ${otherActiveApplication.applicationRoleTitle} in this opportunity."
            } else {
                "You already have a pending application for ${otherActiveApplication.applicationRoleTitle}."
            }
            failApplicationAction(IllegalStateException(), message)
            return@runApplicationAction
        }

        try {
            VolunteerOpportunityRepository.reapplyForRole(
                role.roleDatabaseId,
                answers
            )
            refreshAfterAction()
            onSuccess()
        } catch (exception: Exception) {
            failApplicationAction(
                exception,
                if (exception.isConnectivityFailure())
                    "Internet connection is required to apply again."
                else "This application could not be submitted again."
            )
        }
    }

    private fun runApplicationAction(
        applicationId: Int,
        block: suspend (VolunteerOpportunityApplication) -> Unit
    ) {
        if (mutableUiState.value.isApplicationActionRunning) return
        if (!com.example.volunteerlink.data.VolunteerOnline.available(getApplication<Application>())) {
            mutableUiState.update { it.copy(applicationActionError = "Internet connection is required for this action. Connect and try again.") }
            return
        }
        val application = VolunteerOpportunitySessionStore.findApplicationById(applicationId)
        if (application == null) {
            mutableUiState.update {
                it.copy(applicationActionError = "The selected application could not be found.")
            }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(isApplicationActionRunning = true, applicationActionError = null)
            }
            block(application)
        }
    }

    private fun finishLocalAction() {
        mutableUiState.update {
            it.copy(
                isApplicationActionRunning = false,
                applicationActionError = null,
                isShowingCachedData = true,
                dataVersion = it.dataVersion + 1
            )
        }
    }

    private fun failApplicationAction(exception: Exception, fallback: String) {
        exception.printStackTrace()
        mutableUiState.update {
            it.copy(
                isApplicationActionRunning = false,
                applicationActionError = exception.toVolunteerMessage(fallback)
            )
        }
    }

    fun clearApplicationActionError() {
        mutableUiState.update {
            it.copy(applicationActionError = null)
        }
    }

    private fun loadDashboard(
        isRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    errorMessage = null
                )
            }

            val cachedDashboard =
                runCatching {
                    VolunteerDashboardDataSource.readCached()
                }.getOrNull()

            if (!isRefresh && cachedDashboard != null) {
                VolunteerOpportunitySessionStore.replaceWith(
                    cachedDashboard.data
                )
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        isShowingCachedData = true,
                        lastSyncedAtEpochMillis =
                            cachedDashboard.lastSyncedAtEpochMillis,
                        dataVersion = it.dataVersion + 1
                    )
                }
            }

            try {
                val requestWarning = try {
                    com.example.volunteerlink.data.VolunteerApplicationActions.sync(getApplication<Application>())
                    null
                } catch (e: kotlinx.coroutines.CancellationException) { throw e
                } catch (_: Exception) {
                    "An application request is still unconfirmed. Do not assume a role was joined or cancelled. Connect and Sync again to receive the result."
                }
                val syncWarnings = VolunteerDashboardDataSource.syncPendingActions() + listOfNotNull(requestWarning)
                val data = VolunteerDashboardDataSource.refreshFromCloud()
                VolunteerOpportunitySessionStore.replaceWith(data)

                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isApplicationActionRunning = false,
                        errorMessage = null,
                        syncWarning = syncWarnings.takeIf { it.isNotEmpty() }?.distinct()?.joinToString("\n"),
                        isShowingCachedData = false,
                        lastSyncedAtEpochMillis = System.currentTimeMillis(),
                        dataVersion = it.dataVersion + 1
                    )
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isApplicationActionRunning = false,
                        isShowingCachedData = cachedDashboard != null,
                        errorMessage = if (cachedDashboard == null) {
                            exception.toVolunteerMessage(
                                fallback =
                                    "Volunteer opportunities could not be loaded from Supabase."
                            )
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private suspend fun refreshAfterAction() {
        val data = VolunteerDashboardDataSource.refreshFromCloud()
        VolunteerOpportunitySessionStore.replaceWith(data)
        mutableUiState.update {
            it.copy(
                isApplicationActionRunning = false,
                applicationActionError = null,
                dataVersion = it.dataVersion + 1
            )
        }
    }
}

private fun Exception.toVolunteerMessage(
    fallback: String
): String {
    val rawMessage = message.orEmpty()
    return when {
        rawMessage.contains("Applications are closed", ignoreCase = true) ->
            "Applications closed: this opportunity has already started."
        rawMessage.contains("can no longer be cancelled", ignoreCase = true) ->
            "This activity has already started. Your application can no longer be cancelled."
        rawMessage.contains("no valid start date", ignoreCase = true) ->
            "This opportunity has no valid start date. Please contact the organisation."
        rawMessage.contains("selected role is unavailable", ignoreCase = true) ->
            "This role is no longer open for applications. Please sync to refresh its details."
        rawMessage.contains("JWT", ignoreCase = true) ||
            rawMessage.contains("not authenticated", ignoreCase = true) ->
            "Your session has expired. Please sign in again."

        rawMessage.contains("ALREADY_ACCEPTED_IN_POST", ignoreCase = true) ->
            "You already joined another role in this opportunity."

        rawMessage.contains("PENDING_APPLICATION_EXISTS", ignoreCase = true) ->
            "You already have a pending application for another role in this opportunity."

        rawMessage.contains("duplicate", ignoreCase = true) ->
            "You already have an active role or application in this opportunity."

        rawMessage.contains("capacity", ignoreCase = true) ||
            rawMessage.contains("full", ignoreCase = true) ->
            "This role no longer has an available place."

        rawMessage.contains("network", ignoreCase = true) ||
            rawMessage.contains("connect", ignoreCase = true) ->
            "Unable to reach Supabase. Check the internet connection and retry."

        else -> fallback
    }
}

private fun Exception.isConnectivityFailure(): Boolean {
    val raw = message.orEmpty()
    return raw.contains("network", true) ||
        raw.contains("connect", true) ||
        raw.contains("resolve host", true) ||
        raw.contains("timeout", true) ||
        this is java.io.IOException
}
