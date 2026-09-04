package com.example.volunteerlink.organisation

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.supabase
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class OrganisationAuthUiState(
    val isCheckingSession: Boolean = true,
    val isSubmitting: Boolean = false,
    val isResendingEmail: Boolean = false,
    val isAuthenticated: Boolean = false,
    val needsEmailConfirmation: Boolean = false,
    val pendingVerificationEmail: String? = null,
    val errorMessage: String? = null
)

class OrganisationAuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val offlineAccountPreferences =
        application.getSharedPreferences(
            "organisation_offline_account",
            Context.MODE_PRIVATE
        )

    // Must be initialized above init{} below — observeSessionStatus()
    // reads this and can run synchronously during construction.
    private val rememberMePreferences =
        OrganisationRememberMePreferences(application)

    private val mutableUiState =
        MutableStateFlow(OrganisationAuthUiState())

    val uiState: StateFlow<OrganisationAuthUiState> =
        mutableUiState.asStateFlow()

    init {
        observeSessionStatus()
    }

    private fun observeSessionStatus() {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                val current = mutableUiState.value

                when (status) {
                    SessionStatus.Initializing -> Unit

                    is SessionStatus.Authenticated -> {
                        when {
                            current.isSubmitting || current.isAuthenticated -> Unit

                            current.pendingVerificationEmail != null &&
                                    status.session.user?.email?.equals(
                                        current.pendingVerificationEmail,
                                        ignoreCase = true
                                    ) == true -> {
                                try {
                                    confirmOrganisationProfile()
                                    rememberVerifiedOrganisation()
                                    mutableUiState.value = OrganisationAuthUiState(
                                        isCheckingSession = false,
                                        isAuthenticated = true
                                    )
                                } catch (exception: Exception) {
                                    exception.printStackTrace()
                                    mutableUiState.value = OrganisationAuthUiState(
                                        isCheckingSession = false,
                                        errorMessage = authErrorMessage(exception)
                                    )
                                }
                            }

                            else -> {
                                val accountType = fetchAccountType()

                                if (accountType != null &&
                                    !accountType.equals("ORGANISATION", ignoreCase = true)
                                ) {
                                    // Belongs to a different role (e.g.
                                    // Volunteer) — leave it untouched.
                                    mutableUiState.value =
                                        OrganisationAuthUiState(isCheckingSession = false)
                                } else if (rememberMePreferences.isRememberMeEnabled()) {
                                    if (accountType != null) {
                                        rememberVerifiedOrganisation()
                                        mutableUiState.value = OrganisationAuthUiState(
                                            isCheckingSession = false,
                                            isAuthenticated = true
                                        )
                                    } else {
                                        mutableUiState.value = OrganisationAuthUiState(
                                            isCheckingSession = false,
                                            isAuthenticated = isPreviouslyVerifiedOrganisation()
                                        )
                                    }
                                } else {
                                    runCatching { supabase.auth.signOut() }
                                    mutableUiState.value =
                                        OrganisationAuthUiState(isCheckingSession = false)
                                }
                            }
                        }
                    }

                    is SessionStatus.RefreshFailure -> {
                        mutableUiState.value = OrganisationAuthUiState(
                            isCheckingSession = false,
                            isAuthenticated = isPreviouslyVerifiedOrganisation()
                        )
                    }

                    is SessionStatus.NotAuthenticated -> {
                        if (current.isCheckingSession) {
                            mutableUiState.value = OrganisationAuthUiState(isCheckingSession = false)
                        }
                    }
                }
            }
        }
    }

    fun resendVerificationEmail() {
        val pendingEmail = mutableUiState.value.pendingVerificationEmail ?: return
        if (mutableUiState.value.isResendingEmail) return

        viewModelScope.launch {
            mutableUiState.update { it.copy(isResendingEmail = true, errorMessage = null) }
            try {
                supabase.auth.resendEmail(
                    type = OtpType.Email.SIGNUP,
                    email = pendingEmail
                )
                mutableUiState.update { it.copy(isResendingEmail = false) }
            } catch (exception: Exception) {
                exception.printStackTrace()
                mutableUiState.update {
                    it.copy(isResendingEmail = false, errorMessage = authErrorMessage(exception))
                }
            }
        }
    }

    /** User wants to sign up with a different email instead. */
    fun changeEmail() {
        mutableUiState.value = OrganisationAuthUiState(isCheckingSession = false)
    }

    fun signUp(
        email: String,
        password: String,
        organisationName: String,
        contactPhone: String?,
        locationName: String?,
        stateRegion: String?,
        country: String?,
        organisationType: String
    ) {
        val normalizedEmail = email.trim()
        val normalizedOrgName = organisationName.trim()
        val normalizedOrgType = organisationType.trim()
        val normalizedPhone = contactPhone?.trim().orEmpty()

        when {
            normalizedOrgName.isBlank() -> {
                showError("Enter your organisation name.")
                return
            }
            normalizedOrgType.isBlank() -> {
                showError("Select an organisation type.")
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
            !isValidPhoneNumber(normalizedPhone) -> {
                showError("Enter a valid phone number (eg. must start with 0, 9-10 digits).")
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
                supabase.auth.signUpWith(Email, redirectUrl = "com.example.volunteerlink://auth/callback") {
                    this.email = normalizedEmail
                    this.password = password
                    data = buildJsonObject {
                        put("volunteerlink_account_type", "ORGANISATION")
                        put("organisation_name", normalizedOrgName)
                        put("organisation_type", normalizedOrgType)
                        contactPhone?.trim()?.ifBlank { null }?.let {
                            put("contact_phone", normalizedPhone)
                        }
                        locationName?.trim()?.ifBlank { null }?.let { location ->
                            put("location_name", location)
                        }
                        stateRegion?.trim()?.ifBlank { null }?.let { region ->
                            put("state_region", region)
                        }
                        country?.trim()?.ifBlank { null }?.let { countryValue ->
                            put("country", countryValue)
                        }
                    }
                }

                val currentSession = supabase.auth.currentSessionOrNull()

                if (currentSession == null) {
                    mutableUiState.value = OrganisationAuthUiState(
                        isCheckingSession = false,
                        isSubmitting = false,
                        needsEmailConfirmation = true,
                        pendingVerificationEmail = normalizedEmail
                    )
                    return@launch
                }

                confirmOrganisationProfile()

                contactPhone?.trim()?.ifBlank { null }?.let {
                    runCatching {
                        supabase.auth.updateUser {
                            this.phone = normalizedPhone
                        }
                    }.onFailure { phoneException ->
                        phoneException.printStackTrace()
                    }
                }

                rememberVerifiedOrganisation()

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
                it.copy(isSubmitting = true, errorMessage = null)
            }

            try {
                supabase.auth.signInWith(Email) {
                    this.email = normalizedEmail
                    this.password = password
                }
                confirmOrganisationProfile()
                rememberVerifiedOrganisation()
                rememberMePreferences.setRememberMeEnabled(rememberMe)
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

    fun verifySignUpOtp(token: String) {
        val pendingEmail = mutableUiState.value.pendingVerificationEmail
        if (pendingEmail.isNullOrBlank()) {
            showError("No email is pending verification.")
            return
        }
        if (token.isBlank()) {
            showError("Enter the code sent to your email.")
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                supabase.auth.verifyEmailOtp(
                    type = OtpType.Email.SIGNUP,
                    email = pendingEmail,
                    token = token
                )
                confirmOrganisationProfile()
                rememberVerifiedOrganisation()
                mutableUiState.value = OrganisationAuthUiState(
                    isCheckingSession = false,
                    isAuthenticated = true
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                mutableUiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = authErrorMessage(exception)
                    )
                }
            }
        }
    }

    private fun rememberVerifiedOrganisation() {
        val authUserId = supabase.auth.currentUserOrNull()?.id ?: return
        offlineAccountPreferences.edit()
            .putString("verified_auth_user_id", authUserId)
            .apply()
    }

    private fun isPreviouslyVerifiedOrganisation(): Boolean {
        val currentAuthUserId =
            supabase.auth.currentUserOrNull()?.id ?: return false
        return offlineAccountPreferences.getString(
            "verified_auth_user_id",
            null
        ) == currentAuthUserId
    }

    private suspend fun confirmOrganisationProfile() {
        supabase.auth.currentUserOrNull()
            ?: error("The Supabase session is no longer available.")

        val identity = supabase.postgrest
            .rpc("get_my_organisation_context")
            .decodeList<OrganisationIdentityRow>()
            .firstOrNull()
            ?: error("No VolunteerLink profile is linked to this account.")

        require(identity.accountType.equals("ORGANISATION", ignoreCase = true)) {
            "This account belongs to a volunteer, not an organisation."
        }

        require(!identity.organisationId.isNullOrBlank()) {
            "This organisation account is incomplete. Please contact support."
        }
    }

    private suspend fun fetchAccountType(): String? =
        runCatching {
            supabase.postgrest
                .rpc("get_my_organisation_context")
                .decodeList<OrganisationIdentityRow>()
                .firstOrNull()
                ?.accountType
        }.getOrNull()

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

        message.contains("email_address_invalid", ignoreCase = true) ||
                (message.contains("invalid", ignoreCase = true) &&
                        message.contains("email", ignoreCase = true)) ->
            "That email address isn't valid, try a different one."

        message.contains("not authorized", ignoreCase = true) ->
            "This email address can't receive messages from this project yet."

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

private fun isValidPhoneNumber(phone: String): Boolean {
    val cleaned = phone.replace(Regex("[\\s\\-()]"), "")

    val isLocalFormat =
        cleaned.matches(Regex("^0\\d{8,9}$"))

    val isCountryCodeFormat =
        cleaned.matches(Regex("^\\+?60\\d{8,9}$"))

    return isLocalFormat || isCountryCodeFormat
}

@Serializable
private data class OrganisationIdentityRow(
    @SerialName("user_id") val userId: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("organisation_id") val organisationId: String? = null,
    @SerialName("organisation_name") val organisationName: String? = null,
    @SerialName("verification_status") val verificationStatus: String? = null
)