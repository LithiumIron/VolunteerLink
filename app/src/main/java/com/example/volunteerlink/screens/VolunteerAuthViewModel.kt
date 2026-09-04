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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class VolunteerAuthUiState(
    val isCheckingSession: Boolean = true,
    val isSigningIn: Boolean = false,
    val isSubmitting: Boolean = false,
    val isAuthenticated: Boolean = false,
    val needsEmailConfirmation: Boolean = false,
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

    // Must be initialized above init{} below — checkExistingSession()
    // reads this and runs as part of construction.
    private val rememberMePreferences =
        VolunteerRememberMePreferences(application)

    private val mutableUiState =
        MutableStateFlow(VolunteerAuthUiState())

    val uiState: StateFlow<VolunteerAuthUiState> =
        mutableUiState.asStateFlow()

    init {
        checkExistingSession()
    }

    fun signIn(email: String, password: String, rememberMe: Boolean) {
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
                rememberMePreferences.setRememberMeEnabled(rememberMe)
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

    fun signUp(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        locationName: String? = null,
        stateRegion: String? = null,
        country: String? = null
    ) {
        val normalizedName = fullName.trim()
        val normalizedEmail = email.trim()
        val normalizedPhone = phone.trim()

        when {
            normalizedName.isBlank() -> {
                showError("Enter your name.")
                return
            }
            normalizedEmail.isBlank() -> {
                showError("Enter an email address.")
                return
            }
            normalizedPhone.isBlank() -> {
                showError("Enter a contact phone number.")
                return
            }
            !isValidVolunteerPhoneNumber(normalizedPhone) -> {
                showError("Enter a valid phone number starting with 0.")
                return
            }
            password.length < 6 -> {
                showError("Password must be at least 6 characters.")
                return
            }
        }

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(isSubmitting = true, errorMessage = null)
            }

            try {
                supabase.auth.signUpWith(Email) {
                    this.email = normalizedEmail
                    this.password = password
                    data = buildJsonObject {
                        put("volunteerlink_account_type", "VOLUNTEER")
                        put("full_name", normalizedName)
                        put("phone", normalizedPhone)
                        locationName?.trim()?.ifBlank { null }?.let { put("city", it) }
                        stateRegion?.trim()?.ifBlank { null }?.let { put("state_region", it) }
                        country?.trim()?.ifBlank { null }?.let { put("country", it) }
                    }
                }

                val authUserId = supabase.auth.currentUserOrNull()?.id

                if (authUserId == null) {
                    mutableUiState.value = VolunteerAuthUiState(
                        isCheckingSession = false,
                        needsEmailConfirmation = true
                    )
                    return@launch
                }

                confirmVolunteerProfile()

                runCatching {
                    supabase.auth.updateUser {
                        this.phone = normalizedPhone
                    }
                }.onFailure { it.printStackTrace() }

                rememberVerifiedVolunteer()

                mutableUiState.value = VolunteerAuthUiState(
                    isCheckingSession = false,
                    isAuthenticated = true
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                runCatching { supabase.auth.signOut() }
                mutableUiState.value = VolunteerAuthUiState(
                    isCheckingSession = false,
                    errorMessage = authErrorMessage(exception)
                )
            }
        }
    }

    private fun isValidVolunteerPhoneNumber(phone: String): Boolean {
        val cleaned = phone.replace(Regex("[\\s\\-()]"), "")
        val isLocalFormat = cleaned.matches(Regex("^0\\d{8,9}$"))
        return isLocalFormat
    }

    fun clearError() {
        mutableUiState.update {
            it.copy(errorMessage = null)
        }
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            try {
                val restoredSessionStatus =
                    supabase.auth.sessionStatus
                        .first { sessionStatus ->
                            sessionStatus !=
                                    SessionStatus.Initializing
                        }

                when (restoredSessionStatus) {
                    is SessionStatus.Authenticated -> {
                        val accountType = fetchAccountType()

                        if (accountType != null &&
                            !accountType.equals("VOLUNTEER", ignoreCase = true)
                        ) {
                            // Restored session belongs to a different role
                            // (e.g. Organisation). Leave it untouched —
                            // not this screen's session to manage.
                            mutableUiState.value =
                                VolunteerAuthUiState(isCheckingSession = false)
                        } else if (rememberMePreferences.isRememberMeEnabled()) {
                            if (accountType != null) {
                                rememberVerifiedVolunteer()
                                mutableUiState.value = VolunteerAuthUiState(
                                    isCheckingSession = false,
                                    isAuthenticated = true
                                )
                            } else {
                                mutableUiState.value = VolunteerAuthUiState(
                                    isCheckingSession = false,
                                    isAuthenticated = isPreviouslyVerifiedVolunteer()
                                )
                            }
                        } else {
                            runCatching { supabase.auth.signOut() }
                            mutableUiState.value =
                                VolunteerAuthUiState(isCheckingSession = false)
                        }
                    }

                    is SessionStatus.RefreshFailure -> {
                        mutableUiState.value =
                            VolunteerAuthUiState(
                                isCheckingSession = false,
                                isAuthenticated = isPreviouslyVerifiedVolunteer()
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

    private suspend fun fetchAccountType(): String? =
        runCatching {
            val authUserId = supabase.auth.currentUserOrNull()?.id
                ?: return@runCatching null

            supabase.from("user_profiles")
                .select {
                    filter {
                        eq("auth_user_id", authUserId)
                    }
                }
                .decodeList<VolunteerAccountTypeRow>()
                .firstOrNull()
                ?.accountType
        }.getOrNull()

    private fun showError(message: String) {
        mutableUiState.update {
            it.copy(errorMessage = message)
        }
    }
}

private fun authErrorMessage(exception: Exception): String {
    val message = exception.message.orEmpty()
    return when {
        message.contains("already registered", ignoreCase = true) ||
                message.contains("already exists", ignoreCase = true) ->
            "An account with this email already exists."

        message.contains("email_address_invalid", ignoreCase = true) ||
                (message.contains("invalid", ignoreCase = true) &&
                        message.contains("email", ignoreCase = true)) ->
            "That email address isn't valid — try a different one."

        message.contains("not authorized", ignoreCase = true) ->
            "This email address can't receive messages from this project yet."

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