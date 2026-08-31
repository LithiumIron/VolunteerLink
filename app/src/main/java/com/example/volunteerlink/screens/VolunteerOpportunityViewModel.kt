
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

data class VolunteerOpportunityUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isApplicationActionRunning: Boolean = false,
    val errorMessage: String? = null,
    val applicationActionError: String? = null,
    val isShowingCachedData: Boolean = false,
    val lastSyncedAtEpochMillis: Long? = null,
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
                                "Saved opportunities could not be updated.",
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
        onSuccess: () -> Unit
    ) {
        val role =
            VolunteerOpportunitySessionStore.findRoleById(
                eventId = eventId,
                roleId = roleId
            )

        if (role == null) {
            mutableUiState.update {
                it.copy(
                    applicationActionError =
                        "The selected role could not be found."
                )
            }
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
                val priorApplication = VolunteerOpportunitySessionStore
                    .volunteerApplications.firstOrNull {
                        it.applicationEventId == eventId &&
                            it.applicationRoleId == roleId &&
                            it.applicationStatus in setOf(
                                VolunteerApplicationStatus.CANCELLED,
                                VolunteerApplicationStatus.REJECTED
                            )
                    }
                if (priorApplication != null) {
                    VolunteerOpportunityRepository.reapplyForRole(
                        roleDatabaseId = role.roleDatabaseId,
                        answers = answers
                    )
                } else {
                    VolunteerOpportunityRepository.submitApplication(
                        roleDatabaseId = role.roleDatabaseId,
                        questions = role.roleExtraApplicationQuestions,
                        answers = answers
                    )
                }
                refreshAfterAction()
                onSuccess()
            } catch (exception: Exception) {
                exception.printStackTrace()
                if (exception.isConnectivityFailure()) {
                    val isReapply = VolunteerOpportunitySessionStore
                        .volunteerApplications.any {
                            it.applicationEventId == eventId &&
                                it.applicationRoleId == roleId &&
                                it.applicationStatus in setOf(
                                    VolunteerApplicationStatus.CANCELLED,
                                    VolunteerApplicationStatus.REJECTED
                                )
                        }
                    if (isReapply) {
                        mutableUiState.update {
                            it.copy(
                                isApplicationActionRunning = false,
                                applicationActionError =
                                    "Internet connection is required to apply again."
                            )
                        }
                        return@launch
                    }
                    val event =
                        VolunteerOpportunitySessionStore
                            .findEventById(eventId)
                    if (event != null) {
                        val payload = buildJsonObject {
                            putJsonArray("answers") {
                                role.roleExtraApplicationQuestions
                                    .forEachIndexed { index, question ->
                                        add(
                                            buildJsonObject {
                                                put("question", question)
                                                put(
                                                    "answer",
                                                    answers.getOrElse(index) { "" }
                                                )
                                            }
                                        )
                                    }
                            }
                        }.toString()
                        VolunteerDashboardDataSource.enqueuePendingAction(
                            actionType = "SUBMIT",
                            targetId = role.roleDatabaseId,
                            payloadJson = payload
                        )
                        VolunteerOpportunitySessionStore
                            .addOfflinePendingApplication(
                                VolunteerOpportunityApplication(
                                    applicationId =
                                        ("offline|" + role.roleDatabaseId)
                                            .hashCode(),
                                    applicationEventId = event.eventId,
                                    applicationEventTitle = event.eventTitle,
                                    applicationOrganisationName =
                                        event.eventOrganisationName,
                                    applicationRoleTitle = role.roleTitle,
                                    applicationSubmittedDate =
                                        DateFormat.getDateInstance(
                                            DateFormat.MEDIUM
                                        ).format(Date()),
                                    applicationStatus =
                                        VolunteerApplicationStatus.PENDING,
                                    applicationRoleId = role.roleId,
                                    applicationStatusMessage =
                                        "Waiting for internet connection to sync.",
                                    applicationEventDate = event.eventDate,
                                    applicationEventTime = event.eventTime,
                                    applicationEventLocation =
                                        event.eventFullAddress,
                                    applicationPrimarySkillPath =
                                        role.rolePrimarySkillPath,
                                    applicationPractisedSkills =
                                        role.roleSkillsPractised,
                                    applicationDatabaseId =
                                        "offline|" + role.roleDatabaseId
                                )
                            )
                        VolunteerDashboardDataSource.cacheCurrentSession()
                        mutableUiState.update {
                            it.copy(
                                isApplicationActionRunning = false,
                                applicationActionError = null,
                                isShowingCachedData = true,
                                dataVersion = it.dataVersion + 1
                            )
                        }
                        onSuccess()
                        return@launch
                    }
                }
                mutableUiState.update {
                    it.copy(
                        isApplicationActionRunning = false,
                        applicationActionError =
                            exception.toVolunteerMessage(
                                fallback =
                                    "Application could not be submitted."
                            )
                    )
                }
            }
        }
    }

    fun cancelApplication(
        applicationId: Int,
        reason: String,
        details: String,
        onSuccess: () -> Unit = {}
    ) {
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
                if (exception.isConnectivityFailure()) {
                    VolunteerDashboardDataSource.enqueuePendingAction(
                        actionType = "CANCEL_V2",
                        targetId = application.applicationDatabaseId,
                        payloadJson = buildJsonObject {
                            put("reason", reason)
                            put("details", details)
                        }.toString()
                    )
                    VolunteerOpportunitySessionStore.replaceApplication(
                        application.copy(
                            applicationStatus =
                                VolunteerApplicationStatus.CANCELLED,
                            applicationStatusMessage =
                                "Cancellation is waiting to sync.",
                            applicationRejectionReason =
                                "Cancelled by volunteer: $reason. " + details
                        )
                    )
                    VolunteerDashboardDataSource.cacheCurrentSession()
                    mutableUiState.update {
                        it.copy(
                            isApplicationActionRunning = false,
                            applicationActionError = null,
                            isShowingCachedData = true,
                            dataVersion = it.dataVersion + 1
                        )
                    }
                    onSuccess()
                    return@launch
                }
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
        } catch (exception: Exception) {
            if (exception.isConnectivityFailure()) {
                VolunteerDashboardDataSource.enqueuePendingAction(
                    "UPDATE_APPLICATION",
                    application.applicationDatabaseId,
                    buildJsonObject {
                        putJsonArray("answers") {
                            answers.forEach { answer -> add(JsonPrimitive(answer)) }
                        }
                    }.toString()
                )
                VolunteerOpportunitySessionStore.replaceApplication(
                    application.copy(
                        applicationScreeningAnswers = answers,
                        applicationStatusMessage =
                            "Your edits are waiting to sync."
                    )
                )
                VolunteerDashboardDataSource.cacheCurrentSession()
                finishLocalAction()
                onSuccess()
            } else failApplicationAction(exception, "Application changes could not be saved.")
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
                VolunteerDashboardDataSource.syncPendingActions()
                val data = VolunteerDashboardDataSource.refreshFromCloud()
                VolunteerOpportunitySessionStore.replaceWith(data)

                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isApplicationActionRunning = false,
                        errorMessage = null,
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
        rawMessage.contains("JWT", ignoreCase = true) ||
            rawMessage.contains("not authenticated", ignoreCase = true) ->
            "Your session has expired. Please sign in again."

        rawMessage.contains("duplicate", ignoreCase = true) ->
            "You already have an application for this role."

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
