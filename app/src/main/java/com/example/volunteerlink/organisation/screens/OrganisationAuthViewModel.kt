package com.example.volunteerlink.organisation

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
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

class OrganisationAuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Caches the auth.uid() of the last account that successfully passed
    // confirmOrganisationProfile() while online. This is a UX convenience
    // for offline continuity ONLY — it grants no data access by itself.
    // Every real Supabase query still requires a valid session token and
    // is gated by RLS regardless of this flag.
    private val offlineAccountPreferences =
        application.getSharedPreferences(
            "organisation_offline_account",
            Context.MODE_PRIVATE
        )

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
        contactPhone: String?,
        locationName: String?,
        stateRegion: String?,
        country: String?,
        organisationType: String
    ) {
        val normalizedEmail = email.trim()
        val normalizedOrgName = organisationName.trim()
        val normalizedOrgType = organisationType.trim()

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
                // Send the organisation details as Supabase Auth metadata.
                // 07_organisation_authenticated_access.sql has an auth.users
                // trigger that atomically creates user_profiles + organisations.
                // This also works when email confirmation is enabled, because
                // the database profile is created when auth.users is inserted.
                supabase.auth.signUpWith(Email) {
                    this.email = normalizedEmail
                    this.password = password
                    data = buildJsonObject {
                        put("volunteerlink_account_type", "ORGANISATION")
                        put("organisation_name", normalizedOrgName)
                        put("organisation_type", normalizedOrgType)
                        contactPhone?.trim()?.ifBlank { null }?.let { phone ->
                            put("contact_phone", phone)
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

                val authUserId = supabase.auth.currentUserOrNull()?.id

                if (authUserId == null) {
                    // Email confirmation is enabled. The SQL trigger has already
                    // created the VolunteerLink organisation rows, so after the
                    // email is confirmed the user can sign in normally.
                    mutableUiState.value = OrganisationAuthUiState(
                        isCheckingSession = false,
                        needsEmailConfirmation = true
                    )
                    return@launch
                }

                // When confirmation is disabled we already have a session.
                // Verify that the SQL trigger created the matching profile.
                confirmOrganisationProfile()
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
                rememberVerifiedOrganisation()
                mutableUiState.value = OrganisationAuthUiState(
                    isCheckingSession = false,
                    isAuthenticated = true
                )
            } catch (exception: Exception) {
                exception.printStackTrace()

                // The confirm check itself failed because there's no
                // network right now (not because the account was rejected).
                // If this exact account was verified the last time we had
                // a connection, let them continue rather than locking them
                // out purely because signal dropped.
                if (isNetworkRelated(exception) && isPreviouslyVerifiedOrganisation()) {
                    mutableUiState.value = OrganisationAuthUiState(
                        isCheckingSession = false,
                        isAuthenticated = true
                    )
                    return@launch
                }

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

                    is SessionStatus.RefreshFailure -> {
                        // A session token exists but couldn't be refreshed —
                        // almost always means no network right now. There's
                        // no connection to show a meaningful email in a
                        // dialog or to re-verify the profile, so fall back
                        // directly to the cached offline flag instead.
                        mutableUiState.value = OrganisationAuthUiState(
                            isCheckingSession = false,
                            isAuthenticated = isPreviouslyVerifiedOrganisation()
                        )
                    }

                    is SessionStatus.NotAuthenticated,
                    SessionStatus.Initializing -> {
                        mutableUiState.value =
                            OrganisationAuthUiState(isCheckingSession = false)
                    }
                }
            } catch (_: Exception) {
                mutableUiState.value =
                    OrganisationAuthUiState(isCheckingSession = false)
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

        // Resolve the signed-in VolunteerLink identity through one authenticated
        // RPC. This avoids a false "no profile" result from a direct table SELECT
        // if RLS policy state was changed by a rollback/migration mismatch.
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

    private fun showError(message: String) {
        mutableUiState.update { it.copy(errorMessage = message) }
    }
}

private fun isNetworkRelated(exception: Exception): Boolean {
    val message = exception.message.orEmpty()
    return message.contains("network", ignoreCase = true) ||
            message.contains("connect", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("unable to resolve host", ignoreCase = true)
}

private fun authErrorMessage(exception: Exception): String {
    val message = exception.message.orEmpty()
    return when {
        message.contains("already registered", ignoreCase = true) ||
                message.contains("already exists", ignoreCase = true) ->
            "An account with this email already exists."

        // Must come before the generic "invalid" check below — Supabase's
        // rejected-domain error (e.g. example.com) also contains "invalid",
        // and would otherwise get mislabelled as a wrong-password error.
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
private data class OrganisationIdentityRow(
    @SerialName("user_id") val userId: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("organisation_id") val organisationId: String? = null,
    @SerialName("organisation_name") val organisationName: String? = null,
    @SerialName("verification_status") val verificationStatus: String? = null
)