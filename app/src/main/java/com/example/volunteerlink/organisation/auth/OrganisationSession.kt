package com.example.volunteerlink.organisation.auth

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Represents the authenticated Organisation identity that the rest of the Organisation module uses when reading or
// mutating data.
//
// The session is resolved from Supabase Auth plus VolunteerLink profile/organisation rows instead of relying on a
// hard-coded organisation id.
//
// Repositories use this context to know which organisation owns a post, which storage folder to use, and whether
// the organisation is verified.
//
// Server-side RLS/RPC checks remain authoritative; this client-side context is a convenient application model, not
// a replacement for database security.
//
// Architectural layer: Authentication/session support layer.
// ============================================================================


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.repository.OrganisationProfileData
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Single source of truth for the organisation that owns the current Supabase session.
 *
 * Keeping this lookup in one place prevents Home, Manage, Create Post and Post Management
 * from silently drifting back to a hardcoded organisation id.
 */
/**
 * DETAILED DECLARATION — OrganisationSessionContext
 *
 * Domain/UI type for Organisation Session Context used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationSessionContext(
    val organisationId: String,
    val organisationName: String,
    val verificationStatus: String
) {
    val isVerified: Boolean
        get() = verificationStatus.equals("VERIFIED", ignoreCase = true)
}

/**
 * DETAILED DECLARATION — OrganisationSession
 *
 * Single shared instance for Organisation Session so related rules/state are defined once for the application
 * process.
 */
object OrganisationSession {

    /**
     * DETAILED BEHAVIOUR — requireContext
     *
     * Implements the current VolunteerLink responsibility for require context in this support/model layer.
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     */
    suspend fun requireContext(): OrganisationSessionContext {
        supabase.auth.currentUserOrNull()
            ?: error("No signed-in organisation session.")

        val identity = supabase.postgrest
            .rpc("get_my_organisation_context")
            .decodeList<SessionIdentityRow>()
            .firstOrNull()
            ?: error("No VolunteerLink profile is linked to this account.")

        require(identity.accountType.equals("ORGANISATION", ignoreCase = true)) {
            "This account belongs to a volunteer, not an organisation."
        }

        val organisationId = identity.organisationId
            ?: error("This organisation account is incomplete. Please contact support.")
        val organisationName = identity.organisationName
            ?: error("This organisation account is incomplete. Please contact support.")
        val verificationStatus = identity.verificationStatus
            ?: error("This organisation account is incomplete. Please contact support.")

        return OrganisationSessionContext(
            organisationId = organisationId,
            organisationName = organisationName,
            verificationStatus = verificationStatus
        )
    }

    /**
     * DETAILED BEHAVIOUR — requireOrganisationId
     *
     * Implements the current VolunteerLink responsibility for require organisation id in this support/model
     * layer.
     */
    suspend fun requireOrganisationId(): String = requireContext().organisationId
}

@Serializable
/**
 * DETAILED DECLARATION — SessionIdentityRow
 *
 * Domain/UI type for Session Identity Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class SessionIdentityRow(
    @SerialName("user_id") val userId: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("organisation_id") val organisationId: String? = null,
    @SerialName("organisation_name") val organisationName: String? = null,
    @SerialName("verification_status") val verificationStatus: String? = null
)

/**
 * DETAILED DECLARATION — OrganisationSessionStore
 *
 * Single shared instance for Organisation Session Store so related rules/state are defined once for the
 * application process.
 */
object OrganisationSessionStore {

    private var _profileData by mutableStateOf<OrganisationProfileData?>(null)
    val profileData: OrganisationProfileData?
        get() = _profileData

    // Distinguishes "actively loading" from "loaded and empty/failed" so
    // OrganisationProfileScreen can tell a genuine load failure apart from
    // one that's still in flight, instead of spinning forever on failure.
    var isProfileLoading: Boolean by mutableStateOf(false)
        private set

    /**
     * DETAILED BEHAVIOUR — updateProfileLoading
     *
     * Implements the current VolunteerLink responsibility for update profile loading in this support/model
     * layer.
     */
    fun updateProfileLoading(loading: Boolean) {
        isProfileLoading = loading
    }

    /**
     * DETAILED BEHAVIOUR — setProfileData
     *
     * Implements the current VolunteerLink responsibility for set profile data in this support/model layer.
     */
    fun setProfileData(data: OrganisationProfileData) {
        _profileData = data
        isProfileLoading = false
    }

    // Called from EditOrganisationProfileScreen's onSaved so the next visit
    // to OrganisationProfileScreen refetches instead of showing what was
    // cached before the edit.
    /**
     * DETAILED BEHAVIOUR — clearProfileData
     *
     * Implements the current VolunteerLink responsibility for clear profile data in this support/model layer.
     */
    fun clearProfileData() {
        _profileData = null
    }
}