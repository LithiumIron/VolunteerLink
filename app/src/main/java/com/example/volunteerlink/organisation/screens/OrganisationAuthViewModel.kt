package com.example.volunteerlink.organisation

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Owns Organisation authentication/session state for sign-in, sign-up, email OTP verification and Remember Me
// behaviour.
//
// Supabase Auth is the source of the authenticated session; the ViewModel additionally checks the linked
// VolunteerLink account_type so a Volunteer account is not treated as an Organisation account.
//
// Registration metadata is completed through the Organisation profile workflow only after the email identity is
// verified, while passwords/OTP values are never written into Organisation JSON local storage.
//
// Remember Me stores only a small preference/account marker used to decide whether a restored valid Organisation
// session should open the Organisation area automatically.
//
// Authentication state is exposed through StateFlow so SignIn/SignUp screens render progress, OTP, error and
// success without calling Supabase directly.
//
// Architectural layer: ViewModel / workflow state layer.
// ============================================================================


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

/**
 * DETAILED DECLARATION — OrganisationAuthUiState
 *
 * Immutable snapshot of all UI-visible state required by Organisation Auth Ui State.
 *
 * Keeping loading/data/error/action flags together makes recomposition deterministic and avoids hidden mutable
 * state in individual composables.
 */
data class OrganisationAuthUiState(
    val isCheckingSession: Boolean = true,
    val isSubmitting: Boolean = false,
    val isResendingEmail: Boolean = false,
    val isAuthenticated: Boolean = false,
    val needsEmailConfirmation: Boolean = false,
    val pendingVerificationEmail: String? = null,
    val errorMessage: String? = null
)

/**
 * DETAILED DECLARATION — OrganisationAuthViewModel
 *
 * Lifecycle-aware state owner for Organisation Auth View Model. It survives ordinary Compose recomposition and
 * coordinates asynchronous repository work.
 *
 * UI callbacks enter through methods on this class so validation, loading/error state and dependent business
 * rules remain centralised.
 */
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

    /**
     * DETAILED BEHAVIOUR — observeSessionStatus
     *
     * Continuously observes Supabase Auth session status and translates SDK session states into
     * OrganisationAuthUiState.
     *
     * Authenticated sessions are accepted for the Organisation module only after the linked VolunteerLink
     * account type is confirmed as ORGANISATION; a Volunteer account is not silently reused as an Organisation
     * account.
     *
     * When Remember Me is disabled, an otherwise-restored Supabase session is signed out so the Organisation
     * sign-in screen remains the explicit entry point.
     *
     * Refresh failures can fall back to the small locally remembered verified-account marker, but this fallback
     * does not create a new Supabase session or grant database permissions.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
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

    /**
     * DETAILED BEHAVIOUR — resendVerificationEmail
     *
     * Implements the ViewModel workflow operation for resend verification email.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
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
    /**
     * DETAILED BEHAVIOUR — changeEmail
     *
     * Implements the ViewModel workflow operation for change email.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    fun changeEmail() {
        mutableUiState.value = OrganisationAuthUiState(isCheckingSession = false)
    }

    // Verifies the 6-digit code Supabase emailed after signUp() — the
    // code-entry counterpart to VolunteerAuthViewModel's verifySignUpOtp,
    // used by OrganisationSignUpScreen's OTP step instead of relying on
    // the user tapping a link in their inbox.
    /**
     * DETAILED BEHAVIOUR — verifySignUpOtp
     *
     * Verifies the six-digit signup OTP against the pending email through Supabase Auth.
     *
     * After Supabase confirms the OTP, the method completes/refreshes the Organisation profile state and
     * records the account as a verified Organisation for the app-side Remember Me/offline marker.
     *
     * An OTP is never stored as persistent local data; it is used only for the current verification request.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
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

    /**
     * DETAILED BEHAVIOUR — signUp
     *
     * Validates Organisation registration input, asks Supabase Auth to create the email/password identity, and
     * supplies the profile metadata required to finish the VolunteerLink Organisation account after email
     * verification.
     *
     * The method keeps isSubmitting/error/verification-email state in the ViewModel so the Compose sign-up
     * screen can show progress and the OTP step without performing authentication calls itself.
     *
     * Email verification remains part of the flow before the Organisation profile is treated as authenticated
     * for normal Organisation navigation.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
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
            !isValidAuthPhoneNumber(normalizedPhone) -> {
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

                // "Already registered" from Supabase most often means an
                // unconfirmed account from an earlier abandoned signup —
                // not a genuinely taken email. Resend the code instead of
                // hard-blocking the user; if the email really is already
                // confirmed, resendEmail() will fail and we fall through
                // to the normal error message below.
                val message = exception.message.orEmpty()
                val isUnconfirmedDuplicate =
                    message.contains("already registered", ignoreCase = true) ||
                            message.contains("already exists", ignoreCase = true)

                if (isUnconfirmedDuplicate) {
                    try {
                        supabase.auth.resendEmail(
                            type = OtpType.Email.SIGNUP,
                            email = normalizedEmail
                        )
                        mutableUiState.value = OrganisationAuthUiState(
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
                mutableUiState.value = OrganisationAuthUiState(
                    isCheckingSession = false,
                    errorMessage = authErrorMessage(exception)
                )
            }
        }
    }

    /**
     * DETAILED BEHAVIOUR — signIn
     *
     * Authenticates the supplied Organisation email/password with Supabase Auth and then verifies that the
     * linked VolunteerLink profile has account_type ORGANISATION.
     *
     * Remember Me changes only the app-side convenience behaviour after a valid Organisation login; the
     * password itself is not written to Organisation local storage.
     *
     * Authentication errors are converted to user-facing state while the Supabase session remains the source of
     * backend identity for later RPC/RLS checks.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
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

    /**
     * DETAILED BEHAVIOUR — clearError
     *
     * Implements the ViewModel workflow operation for clear error.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    fun clearError() {
        mutableUiState.update { it.copy(errorMessage = null) }
    }



    /**
     * DETAILED BEHAVIOUR — rememberVerifiedOrganisation
     *
     * Implements the ViewModel workflow operation for remember verified organisation.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun rememberVerifiedOrganisation() {
        val authUserId = supabase.auth.currentUserOrNull()?.id ?: return
        offlineAccountPreferences.edit()
            .putString("verified_auth_user_id", authUserId)
            .apply()
    }

    /**
     * DETAILED BEHAVIOUR — isPreviouslyVerifiedOrganisation
     *
     * Implements the ViewModel workflow operation for is previously verified organisation.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun isPreviouslyVerifiedOrganisation(): Boolean {
        val currentAuthUserId =
            supabase.auth.currentUserOrNull()?.id ?: return false
        return offlineAccountPreferences.getString(
            "verified_auth_user_id",
            null
        ) == currentAuthUserId
    }

    /**
     * DETAILED BEHAVIOUR — confirmOrganisationProfile
     *
     * Controls workflow/navigation state for confirm organisation profile while keeping step transitions and
     * confirmation rules in one place.
     *
     * The screen emits the intent, but the ViewModel decides whether the transition is currently valid for the
     * draft/post state.
     */
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

    /**
     * DETAILED BEHAVIOUR — fetchAccountType
     *
     * Implements the ViewModel workflow operation for fetch account type.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private suspend fun fetchAccountType(): String? =
        runCatching {
            supabase.postgrest
                .rpc("get_my_organisation_context")
                .decodeList<OrganisationIdentityRow>()
                .firstOrNull()
                ?.accountType
        }.getOrNull()

    /**
     * DETAILED BEHAVIOUR — showError
     *
     * Implements the ViewModel workflow operation for show error.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun showError(message: String) {
        mutableUiState.update { it.copy(errorMessage = message) }
    }
}

/**
 * DETAILED BEHAVIOUR — authErrorMessage
 *
 * Implements the ViewModel workflow operation for auth error message.
 *
 * It translates screen intent into immutable UI-state changes and/or repository work so presentation code stays
 * free of backend/business decisions.
 */
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



@Serializable
/**
 * DETAILED DECLARATION — OrganisationIdentityRow
 *
 * Domain/UI type for Organisation Identity Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class OrganisationIdentityRow(
    @SerialName("user_id") val userId: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("organisation_id") val organisationId: String? = null,
    @SerialName("organisation_name") val organisationName: String? = null,
    @SerialName("verification_status") val verificationStatus: String? = null
)