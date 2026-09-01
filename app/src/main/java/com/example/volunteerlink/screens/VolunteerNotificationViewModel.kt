package com.example.volunteerlink.screens

import androidx.lifecycle.ViewModel
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
    val unreadCount: Int
        get() = notifications.count { !it.isRead }
}

class VolunteerNotificationViewModel : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(VolunteerNotificationUiState())

    val uiState: StateFlow<VolunteerNotificationUiState> =
        mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
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
}
