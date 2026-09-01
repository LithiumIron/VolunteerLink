package com.example.volunteerlink.organisation

import androidx.lifecycle.ViewModel
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

data class OrganisationAuthUiState(
    val isCheckingSession: Boolean = true,
    val isSubmitting: Boolean = false,
    val isAuthenticated: Boolean = false,
    val needsEmailConfirmation: Boolean = false,
    // Set only when a SAVED session was found on launch. The screen should
    // show a "Continue as <email>?" prompt instead of signing in silently,
    // so a different account can be used for testing without needing to
    // manually sign out first.
    val pendingAccountEmail: String? = null,
    val errorMessage: String? = null
)

class OrganisationAuthViewModel : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(OrganisationAuthUiState())

    val uiState: StateFlow<OrganisationAuthUiState> =
        mutableUiState.asStateFlow()

    init {
        checkExistingSession()
    }

    fun signUp(
        email: String,
        password: String,
        organisationName: String,
        contactPhone: String?
    ) {
        val normalizedEmail = email.trim()
        val normalizedOrgName = organisationName.trim()

        when {
            normalizedOrgName.isBlank() -> {
                showError("Enter your organisation name.")
                return
            }
            normalizedEmail.isBlank() -> {
                showError("Enter an email address.")
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
                }

                val authUserId = supabase.auth.currentUserOrNull()?.id

                if (authUserId == null) {
                    // "Confirm email" is enabled in Supabase Auth settings —
                    // no session exists yet, so we can't create the profile
                    // rows (RLS requires auth.uid() to match). The user must
                    // confirm via email, then sign in normally.
                    mutableUiState.value = OrganisationAuthUiState(
                        isCheckingSession = false,
                        needsEmailConfirmation = true
                    )
                    return@launch
                }

                createOrganisationProfile(
                    authUserId = authUserId,
                    organisationName = normalizedOrgName,
                    contactPhone = contactPhone?.trim()?.ifBlank { null }
                )

                mutableUiState.value = OrganisationAuthUiState(
                    isCheckingSession = false,
                    isAuthenticated = true
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                runCatching { supabase.auth.signOut() }
                mutableUiState.value = OrganisationAuthUiState(
                    isCheckingSession = false,
                    errorMessage = authErrorMessage(exception)
                )
            }
        }
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
                it.copy(isSubmitting = true, errorMessage = null)
            }

            try {
                supabase.auth.signInWith(Email) {
                    this.email = normalizedEmail
                    this.password = password
                }
                confirmOrganisationProfile()
                mutableUiState.value = OrganisationAuthUiState(
                    isCheckingSession = false,
                    isAuthenticated = true
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                runCatching { supabase.auth.signOut() }
                mutableUiState.value = OrganisationAuthUiState(
                    isCheckingSession = false,
                    errorMessage = authErrorMessage(exception)
                )
            }
        }
    }

    fun clearError() {
        mutableUiState.update { it.copy(errorMessage = null) }
    }

    /** User tapped "Continue" on the restored-session prompt. */
    fun continueWithRestoredSession() {
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(isSubmitting = true, errorMessage = null)
            }
            try {
                confirmOrganisationProfile()
                mutableUiState.value = OrganisationAuthUiState(
                    isCheckingSession = false,
                    isAuthenticated = true
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                runCatching { supabase.auth.signOut() }
                mutableUiState.value = OrganisationAuthUiState(
                    isCheckingSession = false,
                    errorMessage = authErrorMessage(exception)
                )
            }
        }
    }

    /** User tapped "Use a different account" on the restored-session prompt. */
    fun useDifferentAccount() {
        viewModelScope.launch {
            runCatching { supabase.auth.signOut() }
            mutableUiState.value = OrganisationAuthUiState(isCheckingSession = false)
        }
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            try {
                val restoredSessionStatus =
                    supabase.auth.sessionStatus
                        .first { it != SessionStatus.Initializing }

                when (restoredSessionStatus) {
                    is SessionStatus.Authenticated -> {
                        // A session was restored from local storage — do NOT
                        // sign the user in automatically. Ask first, so a
                        // different account can be used instead without
                        // needing to sign out manually.
                        val restoredEmail =
                            supabase.auth.currentUserOrNull()?.email
                        mutableUiState.value = OrganisationAuthUiState(
                            isCheckingSession = false,
                            pendingAccountEmail = restoredEmail ?: "this account"
                        )
                    }
                    is SessionStatus.NotAuthenticated,
                    is SessionStatus.RefreshFailure,
                    SessionStatus.Initializing -> {
                        mutableUiState.value =
                            OrganisationAuthUiState(isCheckingSession = false)
                    }
                }
            } catch (_: Exception) {
                runCatching { supabase.auth.signOut() }
                mutableUiState.value =
                    OrganisationAuthUiState(isCheckingSession = false)
            }
        }
    }

    private suspend fun createOrganisationProfile(
        authUserId: String,
        organisationName: String,
        contactPhone: String?
    ) {
        // 1. Base account row, marked as an ORGANISATION account type.
        supabase.from("user_profiles").insert(
            NewUserProfileRow(
                authUserId = authUserId,
                fullName = organisationName,
                accountType = "ORGANISATION"
            )
        )

        // 2. Read back the generated user_id (e.g. "USER003") to link the
        // organisations row to it.
        val profile = supabase.from("user_profiles")
            .select {
                filter { eq("auth_user_id", authUserId) }
            }
            .decodeSingle<UserProfileIdRow>()

        // 3. The organisation-specific row. contact_email is intentionally
        // left null here — it's a nullable column, and the org sets/edits it
        // later from their Profile screen rather than at sign-up.
        supabase.from("organisations").insert(
            NewOrganisationRow(
                userId = profile.userId,
                organisationName = organisationName,
                contactPhone = contactPhone
            )
        )
    }

    private suspend fun confirmOrganisationProfile() {
        val authUserId = supabase.auth.currentUserOrNull()?.id
            ?: error("The Supabase session is no longer available.")

        val profiles = supabase.from("user_profiles")
            .select {
                filter { eq("auth_user_id", authUserId) }
            }
            .decodeList<VolunteerAccountTypeRowForOrg>()

        val profile = profiles.firstOrNull()
            ?: error("No VolunteerLink profile is linked to this account.")

        require(profile.accountType == "ORGANISATION") {
            "This account belongs to a volunteer, not an organisation."
        }
    }

    private fun showError(message: String) {
        mutableUiState.update { it.copy(errorMessage = message) }
    }
}

private fun authErrorMessage(exception: Exception): String {
    val message = exception.message.orEmpty()
    return when {
        message.contains("already registered", ignoreCase = true) ||
                message.contains("already exists", ignoreCase = true) ->
            "An account with this email already exists."

        message.contains("invalid", ignoreCase = true) ||
                message.contains("credentials", ignoreCase = true) ->
            "The email or password is incorrect."

        message.contains("volunteer", ignoreCase = true) ->
            "This is a volunteer account. Use the volunteer sign-in."

        message.contains("profile", ignoreCase = true) ->
            "This account does not have a VolunteerLink profile."

        message.contains("network", ignoreCase = true) ||
                message.contains("connect", ignoreCase = true) ->
            "Unable to reach Supabase. Check the internet connection."

        else -> "Something went wrong. Please check the details and retry."
    }
}

@Serializable
private data class NewUserProfileRow(
    @SerialName("auth_user_id") val authUserId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("account_type") val accountType: String
)

@Serializable
private data class UserProfileIdRow(
    @SerialName("user_id") val userId: String
)

@Serializable
private data class NewOrganisationRow(
    @SerialName("user_id") val userId: String,
    @SerialName("organisation_name") val organisationName: String,
    @SerialName("contact_phone") val contactPhone: String?
)

@Serializable
private data class VolunteerAccountTypeRowForOrg(
    @SerialName("account_type") val accountType: String
)