
package com.example.volunteerlink.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class VolunteerAuthUiState(
    val isCheckingSession: Boolean = true,
    val isSigningIn: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null
)

class VolunteerAuthViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val offlineAccountPreferences =
        application.getSharedPreferences(
            "volunteer_offline_account",
            Context.MODE_PRIVATE
        )
    private val mutableUiState =
        MutableStateFlow(VolunteerAuthUiState())

    val uiState: StateFlow<VolunteerAuthUiState> =
        mutableUiState.asStateFlow()

    init {
        checkExistingSession()
    }

    fun signIn(email: String, password: String) {
        val normalizedEmail = email.trim()
        when {
            normalizedEmail.isBlank() -> {
                showError("Enter your email address.")
                return
            }

            password.isBlank() -> {
                showError("Enter your password.")
                return
            }
        }

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isSigningIn = true,
                    errorMessage = null
                )
            }

            try {
                supabase.auth.signInWith(Email) {
                    this.email = normalizedEmail
                    this.password = password
                }
                confirmVolunteerProfile()
                rememberVerifiedVolunteer()
                mutableUiState.value =
                    VolunteerAuthUiState(
                        isCheckingSession = false,
                        isAuthenticated = true
                    )
            } catch (exception: Exception) {
                exception.printStackTrace()
                runCatching { supabase.auth.signOut() }
                mutableUiState.value =
                    VolunteerAuthUiState(
                        isCheckingSession = false,
                        errorMessage = authErrorMessage(exception)
                    )
            }
        }
    }

    fun clearError() {
        mutableUiState.update {
            it.copy(errorMessage = null)
        }
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            try {
                // On Android, Auth first restores the persisted session from
                // local storage. Reading currentUserOrNull() before that work
                // finishes incorrectly sends an already signed-in volunteer
                // back to the password form.
                val restoredSessionStatus =
                    supabase.auth.sessionStatus
                        .first { sessionStatus ->
                            sessionStatus !=
                                SessionStatus.Initializing
                        }

                when (restoredSessionStatus) {
                    is SessionStatus.Authenticated -> {
                        try {
                            confirmVolunteerProfile()
                            rememberVerifiedVolunteer()
                        } catch (exception: Exception) {
                            if (!isPreviouslyVerifiedVolunteer()) {
                                throw exception
                            }
                        }
                        mutableUiState.value =
                            VolunteerAuthUiState(
                                isCheckingSession = false,
                                isAuthenticated = true
                            )
                    }

                    is SessionStatus.RefreshFailure -> {
                        mutableUiState.value =
                            VolunteerAuthUiState(
                                isCheckingSession = false,
                                isAuthenticated =
                                    isPreviouslyVerifiedVolunteer()
                            )
                    }

                    is SessionStatus.NotAuthenticated,
                    SessionStatus.Initializing -> {
                        mutableUiState.value =
                            VolunteerAuthUiState(
                                isCheckingSession = false
                            )
                    }
                }
            } catch (_: Exception) {
                mutableUiState.value =
                    VolunteerAuthUiState(
                        isCheckingSession = false
                    )
            }
        }
    }

    private fun rememberVerifiedVolunteer() {
        val authUserId = supabase.auth.currentUserOrNull()?.id ?: return
        offlineAccountPreferences.edit()
            .putString("verified_auth_user_id", authUserId)
            .apply()
    }

    private fun isPreviouslyVerifiedVolunteer(): Boolean {
        val currentAuthUserId =
            supabase.auth.currentUserOrNull()?.id ?: return false
        return offlineAccountPreferences.getString(
            "verified_auth_user_id",
            null
        ) == currentAuthUserId
    }

    private suspend fun confirmVolunteerProfile() {
        val authUserId = supabase.auth.currentUserOrNull()?.id
            ?: error("The Supabase session is no longer available.")

        val profiles =
            supabase.from("user_profiles")
                .select {
                    filter {
                        eq("auth_user_id", authUserId)
                    }
                }
                .decodeList<VolunteerAccountTypeRow>()

        val profile = profiles.firstOrNull()
            ?: error("No VolunteerLink profile is linked to this account.")

        require(profile.accountType == "VOLUNTEER") {
            "This account belongs to an organisation, not a volunteer."
        }
    }

    private fun showError(message: String) {
        mutableUiState.update {
            it.copy(errorMessage = message)
        }
    }
}

private fun authErrorMessage(exception: Exception): String {
    val message = exception.message.orEmpty()
    return when {
        message.contains("invalid", ignoreCase = true) ||
            message.contains("credentials", ignoreCase = true) ->
            "The email or password is incorrect."

        message.contains("organisation", ignoreCase = true) ->
            "This is an organisation account. Use the organisation sign-in."

        message.contains("profile", ignoreCase = true) ->
            "This account does not have a VolunteerLink profile."

        message.contains("network", ignoreCase = true) ||
            message.contains("connect", ignoreCase = true) ->
            "Unable to reach Supabase. Check the internet connection."

        else -> "Sign in failed. Please check the details and retry."
    }
}

@Serializable
private data class VolunteerAccountTypeRow(
    @SerialName("account_type")
    val accountType: String
)
