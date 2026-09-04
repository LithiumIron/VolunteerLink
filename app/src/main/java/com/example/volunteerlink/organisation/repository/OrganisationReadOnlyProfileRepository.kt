package com.example.volunteerlink.organisation.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Provides read-only profile access used when an Organisation views a volunteer applicant, a volunteer
// certificate, or a partner organisation.
//
// Sensitive visibility rules are enforced by dedicated authenticated RPCs instead of broad direct-table reads from
// the client.
//
// Volunteer email/private details are not treated as generally public; opportunity-specific contact access is
// resolved separately through the event-phone-contact RPC.
//
// Architectural layer: Data/repository layer.
// ============================================================================


import com.example.volunteerlink.data.supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Public/read-only profile data shown when one account views another account.
 *
 * Volunteer email and normal phone are intentionally absent. The only phone
 * value that can be added is returned by the separate temporary event-contact
 * RPC after the volunteer explicitly opts in for that opportunity.
 */
@Serializable
/**
 * DETAILED DECLARATION — OrganisationViewedVolunteerProfile
 *
 * Domain/UI type for Organisation Viewed Volunteer Profile used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationViewedVolunteerProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String,
    val bio: String = "",
    val city: String = "",
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("member_since") val memberSince: String = "",
    @SerialName("verified_minutes") val verifiedMinutes: Int = 0,
    @SerialName("completed_event_count") val completedEventCount: Int = 0,
    @SerialName("certificate_count") val certificateCount: Int = 0,
    @SerialName("shared_phone") val sharedPhone: String = "",
    @SerialName("phone_contact_until_label") val phoneContactUntilLabel: String? = null,
    @SerialName("completed_events") val completedEvents: List<OrganisationViewedCompletedEvent> = emptyList(),
    val certificates: List<OrganisationViewedVolunteerCertificate> = emptyList(),
    @SerialName("skill_paths") val skillPaths: List<OrganisationViewedSkillPath> = emptyList()
)

@Serializable
/**
 * DETAILED DECLARATION — OrganisationViewedCompletedEvent
 *
 * Domain/UI type for Organisation Viewed Completed Event used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationViewedCompletedEvent(
    @SerialName("post_id") val postId: String,
    val title: String,
    @SerialName("role_name") val roleName: String,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("verified_minutes") val verifiedMinutes: Int? = null
)

@Serializable
/**
 * DETAILED DECLARATION — OrganisationViewedVolunteerCertificate
 *
 * Domain/UI type for Organisation Viewed Volunteer Certificate used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationViewedVolunteerCertificate(
    @SerialName("certificate_id") val certificateId: String,
    @SerialName("post_id") val postId: String,
    @SerialName("role_template_id") val roleTemplateId: String,
    @SerialName("event_title") val eventTitle: String,
    @SerialName("organisation_name") val organisationName: String,
    @SerialName("role_name") val roleName: String,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("verified_minutes") val verifiedMinutes: Int? = null,
    @SerialName("volunteer_name") val volunteerName: String = "VolunteerLink Volunteer"
)

@Serializable
/**
 * DETAILED DECLARATION — OrganisationViewedSkillPath
 *
 * Domain/UI type for Organisation Viewed Skill Path used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationViewedSkillPath(
    @SerialName("skill_path_id") val skillPathId: String,
    val name: String,
    @SerialName("current_level") val currentLevel: Int,
    @SerialName("verified_assignments") val verifiedAssignments: Int,
    @SerialName("verified_minutes") val verifiedMinutes: Int? = null
)

@Serializable
/**
 * DETAILED DECLARATION — OrganisationViewedPartnerProfile
 *
 * Domain/UI type for Organisation Viewed Partner Profile used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationViewedPartnerProfile(
    @SerialName("organisation_id") val organisationId: String,
    @SerialName("organisation_name") val organisationName: String,
    @SerialName("organisation_type") val organisationType: String = "",
    val description: String = "",
    @SerialName("profile_image_path") val profileImagePath: String? = null,
    @SerialName("website_url") val websiteUrl: String = "",
    @SerialName("contact_phone") val contactPhone: String = "",
    @SerialName("contact_email") val contactEmail: String = "",
    @SerialName("location_name") val locationName: String = "",
    @SerialName("state_region") val stateRegion: String = "",
    val country: String = "",
    @SerialName("verification_status") val verificationStatus: String = "",
    @SerialName("open_to_partnership") val openToPartnership: Boolean = false,
    @SerialName("member_since") val memberSince: String = "",
    val supports: List<OrganisationViewedPartnerSupport> = emptyList(),
    @SerialName("recent_posts") val recentPosts: List<OrganisationViewedPartnerPost> = emptyList()
)

@Serializable
/**
 * DETAILED DECLARATION — OrganisationViewedPartnerSupport
 *
 * Domain/UI type for Organisation Viewed Partner Support used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationViewedPartnerSupport(
    @SerialName("support_id") val supportId: String,
    @SerialName("support_type") val supportType: String,
    @SerialName("resource_name") val resourceName: String,
    @SerialName("support_description") val supportDescription: String,
    val quantity: Int? = null,
    val capacity: Int? = null,
    @SerialName("location_name") val locationName: String? = null,
    val country: String? = null
)

@Serializable
/**
 * DETAILED DECLARATION — OrganisationViewedPartnerPost
 *
 * Domain/UI type for Organisation Viewed Partner Post used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationViewedPartnerPost(
    @SerialName("post_id") val postId: String,
    val title: String,
    val status: String
)

@Serializable
/**
 * DETAILED DECLARATION — OrganisationVolunteerEventPhoneContact
 *
 * Domain/UI type for Organisation Volunteer Event Phone Contact used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class OrganisationVolunteerEventPhoneContact(
    @SerialName("shared_phone") val sharedPhone: String = "",
    @SerialName("phone_contact_until_label") val phoneContactUntilLabel: String? = null
)

/**
 * DETAILED DECLARATION — OrganisationReadOnlyProfileRepository
 *
 * Data-access implementation/contract for Organisation Read Only Profile Repository, isolating backend details
 * from the screen and ViewModel layers.
 *
 * Protected server state still relies on authenticated Supabase authorization and database rules rather than
 * trusting client-side checks alone.
 */
object OrganisationReadOnlyProfileRepository {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * DETAILED BEHAVIOUR — loadVolunteerProfile
     *
     * Loads the volunteer profile that this organisation is permitted to inspect through the dedicated
     * organisation_view_volunteer_profile RPC.
     *
     * Using a server function prevents the client from broadly selecting private volunteer rows and lets the
     * backend decide exactly which profile/evidence fields are visible for the organisation-review
     * relationship.
     *
     * Supabase RPC `organisation_view_volunteer_profile`: Returns the organisation-reviewable volunteer profile
     * through a dedicated visibility-controlled server function.
     *
     * Supabase RPC `organisation_get_volunteer_event_phone_contact`: Returns a volunteer phone number only when
     * the opportunity-specific sharing consent is currently active.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun loadVolunteerProfile(
        userId: String,
        postId: String
    ): OrganisationViewedVolunteerProfile? {
        if (userId.isBlank()) return null

        return try {
            // Keep the portfolio lookup independent from temporary phone access.
            // A contact lookup failure must never make the whole profile disappear.
            // ------------------------------------------------------------------------
            // SUPABASE RPC: organisation_view_volunteer_profile
            // Returns the organisation-reviewable volunteer profile through a dedicated visibility-controlled
            // server function.
            // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
            // consistency checks belong on the server for this operation.
            // ------------------------------------------------------------------------
            val profileResponse = supabase.postgrest.rpc(
                function = "organisation_view_volunteer_profile",
                parameters = buildJsonObject { put("p_user_id", userId) }
            )
            val profile = json.decodeFromString<OrganisationViewedVolunteerProfile>(
                profileResponse.data
            )

            if (postId.isBlank()) return profile

            val contact = runCatching {
                // ------------------------------------------------------------------------
                // SUPABASE RPC: organisation_get_volunteer_event_phone_contact
                // Returns a volunteer phone number only when the opportunity-specific sharing consent is
                // currently active.
                // The client sends parameters and waits for the database result; ownership, lifecycle and
                // multi-row consistency checks belong on the server for this operation.
                // ------------------------------------------------------------------------
                val contactResponse = supabase.postgrest.rpc(
                    function = "organisation_get_volunteer_event_phone_contact",
                    parameters = buildJsonObject {
                        put("p_user_id", userId)
                        put("p_post_id", postId)
                    }
                )
                json.decodeFromString<OrganisationVolunteerEventPhoneContact>(
                    contactResponse.data
                )
            }.onFailure { it.printStackTrace() }.getOrNull()

            profile.copy(
                sharedPhone = contact?.sharedPhone.orEmpty(),
                phoneContactUntilLabel = contact?.phoneContactUntilLabel
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * DETAILED BEHAVIOUR — loadVolunteerCertificate
     *
     * Loads one volunteer certificate through a dedicated visibility-controlled RPC for Organisation read-only
     * viewing.
     *
     * The screen receives certificate metadata/content references only after the backend confirms the
     * organisation is allowed to view that volunteer evidence.
     *
     * Supabase RPC `organisation_view_volunteer_certificate`: Returns certificate details only when the
     * organisation has a permitted relationship to the volunteer/application.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun loadVolunteerCertificate(
        userId: String,
        postId: String,
        roleTemplateId: String
    ): OrganisationViewedVolunteerCertificate? {
        if (userId.isBlank() || postId.isBlank() || roleTemplateId.isBlank()) return null

        return try {
            // ------------------------------------------------------------------------
            // SUPABASE RPC: organisation_view_volunteer_certificate
            // Returns certificate details only when the organisation has a permitted relationship to the
            // volunteer/application.
            // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
            // consistency checks belong on the server for this operation.
            // ------------------------------------------------------------------------
            val response = supabase.postgrest.rpc(
                function = "organisation_view_volunteer_certificate",
                parameters = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_post_id", postId)
                    put("p_role_template_id", roleTemplateId)
                }
            )
            json.decodeFromString<OrganisationViewedVolunteerCertificate>(response.data)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * DETAILED BEHAVIOUR — loadPartnerProfile
     *
     * Loads the public read-only profile of an organisation relevant to the current Impact Weave/partnership
     * context through a dedicated RPC.
     *
     * The viewer receives public organisation contact/about/support information without gaining edit access to
     * the partner organisation record.
     *
     * Supabase RPC `organisation_view_partner_profile`: Returns the public/read-only organisation profile that
     * a confirmed/relevant partnership viewer is allowed to see.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun loadPartnerProfile(organisationId: String): OrganisationViewedPartnerProfile? {
        if (organisationId.isBlank()) return null

        return try {
            // ------------------------------------------------------------------------
            // SUPABASE RPC: organisation_view_partner_profile
            // Returns the public/read-only organisation profile that a confirmed/relevant partnership viewer is
            // allowed to see.
            // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
            // consistency checks belong on the server for this operation.
            // ------------------------------------------------------------------------
            val response = supabase.postgrest.rpc(
                function = "organisation_view_partner_profile",
                parameters = buildJsonObject { put("p_organisation_id", organisationId) }
            )
            json.decodeFromString<OrganisationViewedPartnerProfile>(response.data)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
