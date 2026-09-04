package com.example.volunteerlink.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.VolunteerNotification
import com.example.volunteerlink.data.VolunteerNotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VolunteerNotificationUiState(
    val isLoading: Boolean = true,
    val isMarkingRead: Boolean = false,
    val isClearing: Boolean = false,
    val notifications: List<VolunteerNotification> = emptyList(),
    val errorMessage: String? = null
) {
    // Calculated from the current list so the badge updates after refresh, read and clear actions.
    val unreadCount: Int
        get() = notifications.count { !it.isRead }
}

/**
 * Keeps notification network actions out of the Composable and exposes one observable
 * state object. AndroidViewModel is used here because online checks need application context.
 */
class VolunteerNotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableUiState =
        MutableStateFlow(VolunteerNotificationUiState())

    val uiState: StateFlow<VolunteerNotificationUiState> =
        mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        // Notifications are online-only; do not pretend cached/empty data is a successful refresh.
        if (!checkOnline()) return
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            runCatching {
                VolunteerNotificationRepository.load()
            }.onSuccess { notifications ->
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        notifications = notifications,
                        errorMessage = null
                    )
                }
            }.onFailure { exception ->
                exception.printStackTrace()
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage =
                            "Notifications could not be loaded. " +
                                "Check the connection and notification policies."
                    )
                }
            }
        }
    }

    fun markAllRead() {
        // Avoid an unnecessary RPC when every notification is already read.
        if (!checkOnline()) return
        if (mutableUiState.value.unreadCount == 0) return

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isMarkingRead = true,
                    errorMessage = null
                )
            }

            runCatching {
                VolunteerNotificationRepository.markAllRead()
            }.onSuccess {
                mutableUiState.update {
                    it.copy(
                        isMarkingRead = false,
                        notifications =
                            it.notifications.map { notification ->
                                notification.copy(isRead = true)
                            }
                    )
                }
            }.onFailure { exception ->
                exception.printStackTrace()
                mutableUiState.update {
                    it.copy(
                        isMarkingRead = false,
                        errorMessage =
                            "Notifications could not be marked as read."
                    )
                }
            }
        }
    }

    fun dismiss(notification: VolunteerNotification) {
        // stableKey identifies one notification consistently across a refreshed list.
        if (!checkOnline()) return
        if (mutableUiState.value.isClearing) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isClearing = true, errorMessage = null) }
            runCatching {
                VolunteerNotificationRepository.dismiss(notification.stableKey)
            }.onSuccess {
                mutableUiState.update {
                    it.copy(
                        isClearing = false,
                        notifications = it.notifications.filterNot { current ->
                            current.stableKey == notification.stableKey
                        }
                    )
                }
            }.onFailure { exception ->
                exception.printStackTrace()
                mutableUiState.update {
                    it.copy(
                        isClearing = false,
                        errorMessage = "Notification could not be cleared."
                    )
                }
            }
        }
    }

    fun dismissAll() {
        // Save the exact keys before the request. The success handler removes only the
        // notifications that were present when the volunteer pressed "Clear all".
        if (!checkOnline()) return
        if (mutableUiState.value.isClearing) return
        val current = mutableUiState.value.notifications
        if (current.isEmpty()) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isClearing = true, errorMessage = null) }
            runCatching {
                VolunteerNotificationRepository.dismissAll(
                    current.map { it.stableKey }
                )
            }.onSuccess {
                mutableUiState.update {
                    it.copy(isClearing = false, notifications = it.notifications.filterNot { item ->
                        current.any { cleared -> cleared.stableKey == item.stableKey }
                    })
                }
            }.onFailure { exception ->
                exception.printStackTrace()
                mutableUiState.update {
                    it.copy(
                        isClearing = false,
                        errorMessage = "Notifications could not be cleared."
                    )
                }
            }
        }
    }

    private fun checkOnline(): Boolean {
        // Central guard gives every action the same user-facing offline explanation.
        if (com.example.volunteerlink.data.VolunteerOnline.available(getApplication<Application>())) return true
        mutableUiState.update { it.copy(isLoading = false, errorMessage =
            "Internet connection is required to refresh or clear notifications. Connect and try again.") }
        return false
    }
}
