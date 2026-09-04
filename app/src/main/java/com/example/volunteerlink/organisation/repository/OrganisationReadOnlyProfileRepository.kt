package com.example.volunteerlink.organisation.repository

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
data class OrganisationViewedCompletedEvent(
    @SerialName("post_id") val postId: String,
    val title: String,
    @SerialName("role_name") val roleName: String,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("verified_minutes") val verifiedMinutes: Int? = null
)

@Serializable
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
data class OrganisationViewedSkillPath(
    @SerialName("skill_path_id") val skillPathId: String,
    val name: String,
    @SerialName("current_level") val currentLevel: Int,
    @SerialName("verified_assignments") val verifiedAssignments: Int,
    @SerialName("verified_minutes") val verifiedMinutes: Int? = null
)

@Serializable
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
data class OrganisationViewedPartnerPost(
    @SerialName("post_id") val postId: String,
    val title: String,
    val status: String
)

@Serializable
private data class OrganisationVolunteerEventPhoneContact(
    @SerialName("shared_phone") val sharedPhone: String = "",
    @SerialName("phone_contact_until_label") val phoneContactUntilLabel: String? = null
)

object OrganisationReadOnlyProfileRepository {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadVolunteerProfile(
        userId: String,
        postId: String
    ): OrganisationViewedVolunteerProfile? {
        if (userId.isBlank()) return null

        return try {
            // Keep the portfolio lookup independent from temporary phone access.
            // A contact lookup failure must never make the whole profile disappear.
            val profileResponse = supabase.postgrest.rpc(
                function = "organisation_view_volunteer_profile",
                parameters = buildJsonObject { put("p_user_id", userId) }
            )
            val profile = json.decodeFromString<OrganisationViewedVolunteerProfile>(
                profileResponse.data
            )

            if (postId.isBlank()) return profile

            val contact = runCatching {
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

    suspend fun loadVolunteerCertificate(
        userId: String,
        postId: String,
        roleTemplateId: String
    ): OrganisationViewedVolunteerCertificate? {
        if (userId.isBlank() || postId.isBlank() || roleTemplateId.isBlank()) return null

        return try {
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

    suspend fun loadPartnerProfile(organisationId: String): OrganisationViewedPartnerProfile? {
        if (organisationId.isBlank()) return null

        return try {
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
