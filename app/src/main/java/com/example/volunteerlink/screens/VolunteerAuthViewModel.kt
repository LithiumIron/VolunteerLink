package com.example.volunteerlink.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.shared.isValidAuthPhoneNumber
import io.github.jan.supabase.auth.OtpType
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

// Purpose: Stores mutually exclusive authentication progress, OTP requirement, signed-in state and user-facing errors.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
data class VolunteerAuthUiState(
    // Calculate this Boolean once so every following UI branch uses the same decision.
    val isCheckingSession: Boolean = true,
    // Calculate this Boolean once so every following UI branch uses the same decision.
    val isSigningIn: Boolean = false,
    // Calculate this Boolean once so every following UI branch uses the same decision.
    val isSubmitting: Boolean = false,
    // Calculate this Boolean once so every following UI branch uses the same decision.
    val isAuthenticated: Boolean = false,
    // Keep the calculated needs email confirmation value because later validation or Compose content reuses it.
    val needsEmailConfirmation: Boolean = false,
    // Keep the calculated pending verification email value because later validation or Compose content reuses it.
    val pendingVerificationEmail: String? = null,
    // Keep the calculated error message value because later validation or Compose content reuses it.
    val errorMessage: String? = null
)

// Purpose: Coordinates Supabase authentication, local remembered accounts and email OTP verification for volunteers.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
class VolunteerAuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Keep the calculated offline account preferences value because later validation or Compose content reuses it.
    private val offlineAccountPreferences =
        application.getSharedPreferences(
            "volunteer_offline_account",
            Context.MODE_PRIVATE
        )

    // Must be initialized above init{} below — checkExistingSession()
    // reads this and runs as part of construction.
    private val rememberMePreferences =
        VolunteerRememberMePreferences(application)

    // Keep the selected location level so the next dependent location/phone choice can be validated.
    private val mutableUiState =
        MutableStateFlow(VolunteerAuthUiState())

    // Keep the selected location level so the next dependent location/phone choice can be validated.
    val uiState: StateFlow<VolunteerAuthUiState> =
        mutableUiState.asStateFlow()

    init {
        checkExistingSession()
    }

    // Purpose: Validates credentials, signs in through Supabase and updates authentication state used by navigation.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
    fun signIn(email: String, password: String, rememberMe: Boolean) {
        // Keep the calculated normalized email value because later validation or Compose content reuses it.
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

    // Purpose: Validates registration data, creates the Supabase account and requests email verification before access.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
    fun signUp(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        locationName: String? = null,
        stateRegion: String? = null,
        country: String? = null
    ) {
        // Keep the calculated normalized name value because later validation or Compose content reuses it.
        val normalizedName = fullName.trim()
        // Keep the calculated normalized email value because later validation or Compose content reuses it.
        val normalizedEmail = email.trim()
        // Prepare the phone value and country calling code before validation and account creation.
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
            !isValidAuthPhoneNumber(normalizedPhone) -> {
                showError("Enter a valid phone number with country code (e.g. +60123456789).")
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

                // Keep the calculated auth user id value because later validation or Compose content reuses it.
                val authUserId = supabase.auth.currentUserOrNull()?.id

                if (authUserId == null) {
                    mutableUiState.value = VolunteerAuthUiState(
                        isCheckingSession = false,
                        needsEmailConfirmation = true,
                        pendingVerificationEmail = normalizedEmail
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

                // "Already registered" from Supabase most often means an
                // unconfirmed account from an earlier abandoned signup —
                // not a genuinely taken email. Resend the code instead of
                // hard-blocking the user; if the email really is already
                // confirmed, resendEmail() will fail and we fall through
                // to the normal error message below.
                val message = exception.message.orEmpty()
                // Calculate this Boolean once so every following UI branch uses the same decision.
                val isUnconfirmedDuplicate =
                    message.contains("already registered", ignoreCase = true) ||
                            message.contains("already exists", ignoreCase = true)

                if (isUnconfirmedDuplicate) {
                    try {
                        supabase.auth.resendEmail(
                            type = OtpType.Email.SIGNUP,
                            email = normalizedEmail
                        )
                        mutableUiState.value = VolunteerAuthUiState(
                            isCheckingSession = false,
                            needsEmailConfirmation = true,
                            pendingVerificationEmail = normalizedEmail
                        )
                        return@launch
                    } catch (resendException: Exception) {
                        resendException.printStackTrace()
                    }
                }

                runCatching { supabase.auth.signOut() }
                mutableUiState.value = VolunteerAuthUiState(
                    isCheckingSession = false,
                    errorMessage = authErrorMessage(exception)
                )
            }
        }
    }

    /**
     * Verifies the 6-digit code sent to [VolunteerAuthUiState.pendingVerificationEmail]
     * during signUp(). On success this completes signup the same way the
     * immediate-session path in signUp() does — the SQL trigger already
     * created the profile rows when auth.users was inserted.
     */
    // Purpose: Checks the email OTP and marks the new volunteer account authenticated only after Supabase accepts it.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
    fun verifySignUpOtp(token: String) {
        // Keep the calculated pending email value because later validation or Compose content reuses it.
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
                confirmVolunteerProfile()
                rememberVerifiedVolunteer()
                mutableUiState.value = VolunteerAuthUiState(
                    isCheckingSession = false,
                    isAuthenticated = true
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                mutableUiState.update {
                    it.copy(isSubmitting = false, errorMessage = authErrorMessage(exception))
                }
            }
        }
    }

    /** User tapped "Use a different email" on the OTP verification screen. */
    fun cancelSignUpVerification() {
        viewModelScope.launch {
            runCatching { supabase.auth.signOut() }
            mutableUiState.value = VolunteerAuthUiState(isCheckingSession = false)
        }
    }



    // Purpose: Removes the displayed authentication error before the volunteer retries or edits a field.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
    fun clearError() {
        mutableUiState.update {
            it.copy(errorMessage = null)
        }
    }

    // Purpose: Handles check existing session as a focused step shared by this Volunteer flow.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
    private fun checkExistingSession() {
        viewModelScope.launch {
            try {
                // Keep the calculated restored session status value because later validation or Compose content reuses it.
                val restoredSessionStatus =
                    supabase.auth.sessionStatus
                        .first { sessionStatus ->
                            sessionStatus !=
                                    SessionStatus.Initializing
                        }

                when (restoredSessionStatus) {
                    is SessionStatus.Authenticated -> {
                        // Keep the calculated account type value because later validation or Compose content reuses it.
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

    // Purpose: Handles remember verified volunteer as a focused step shared by this Volunteer flow.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
    private fun rememberVerifiedVolunteer() {
        // Keep the calculated auth user id value because later validation or Compose content reuses it.
        val authUserId = supabase.auth.currentUserOrNull()?.id ?: return
        offlineAccountPreferences.edit()
            .putString("verified_auth_user_id", authUserId)
            .apply()
    }

    // Purpose: Checks is previously verified volunteer before allowing the next authentication or application step.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
    private fun isPreviouslyVerifiedVolunteer(): Boolean {
        // Keep the calculated current auth user id value because later validation or Compose content reuses it.
        val currentAuthUserId =
            supabase.auth.currentUserOrNull()?.id ?: return false
        return offlineAccountPreferences.getString(
            "verified_auth_user_id",
            null
        ) == currentAuthUserId
    }

    // Purpose: Handles confirm volunteer profile as a focused step shared by this Volunteer flow.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
    private suspend fun confirmVolunteerProfile() {
        // Keep the calculated auth user id value because later validation or Compose content reuses it.
        val authUserId = supabase.auth.currentUserOrNull()?.id
            ?: error("The Supabase session is no longer available.")

        // Keep the calculated profiles value because later validation or Compose content reuses it.
        val profiles =
            supabase.from("user_profiles")
                .select {
                    filter {
                        eq("auth_user_id", authUserId)
                    }
                }
                .decodeList<VolunteerAccountTypeRow>()

        // Keep the calculated profile value because later validation or Compose content reuses it.
        val profile = profiles.firstOrNull()
            ?: error("No VolunteerLink profile is linked to this account.")

        require(profile.accountType == "VOLUNTEER") {
            "This account belongs to an organisation, not a volunteer."
        }
    }

    // Purpose: Handles fetch account type as a focused step shared by this Volunteer flow.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
    private suspend fun fetchAccountType(): String? =
        runCatching {
            // Keep the calculated auth user id value because later validation or Compose content reuses it.
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

    // Purpose: Handles show error as a focused step shared by this Volunteer flow.
    // Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
    // It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
    private fun showError(message: String) {
        mutableUiState.update {
            it.copy(errorMessage = message)
        }
    }
}

// Purpose: Handles auth error message as a focused step shared by this Volunteer flow.
// Called from this screen, its ViewModel, or the Volunteer navigation host during the related user action.
// It changes only local UI/ViewModel state; persistent changes still go through Supabase repositories.
private fun authErrorMessage(exception: Exception): String {
    // Keep the calculated message value because later validation or Compose content reuses it.
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