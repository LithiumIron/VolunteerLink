package com.example.volunteerlink.organisation.auth

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
data class OrganisationSessionContext(
    val organisationId: String,
    val organisationName: String,
    val verificationStatus: String
) {
    val isVerified: Boolean
        get() = verificationStatus.equals("VERIFIED", ignoreCase = true)
}

object OrganisationSession {

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

    suspend fun requireOrganisationId(): String = requireContext().organisationId
}

@Serializable
private data class SessionIdentityRow(
    @SerialName("user_id") val userId: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("organisation_id") val organisationId: String? = null,
    @SerialName("organisation_name") val organisationName: String? = null,
    @SerialName("verification_status") val verificationStatus: String? = null
)

object OrganisationSessionStore {

    private var _profileData by mutableStateOf<OrganisationProfileData?>(null)
    val profileData: OrganisationProfileData?
        get() = _profileData

    // Distinguishes "actively loading" from "loaded and empty/failed" so
    // OrganisationProfileScreen can tell a genuine load failure apart from
    // one that's still in flight, instead of spinning forever on failure.
    var isProfileLoading: Boolean by mutableStateOf(false)
        private set

    fun updateProfileLoading(loading: Boolean) {
        isProfileLoading = loading
    }

    fun setProfileData(data: OrganisationProfileData) {
        _profileData = data
        isProfileLoading = false
    }

    // Called from EditOrganisationProfileScreen's onSaved so the next visit
    // to OrganisationProfileScreen refetches instead of showing what was
    // cached before the edit.
    fun clearProfileData() {
        _profileData = null
    }
}