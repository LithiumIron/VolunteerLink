package com.example.volunteerlink.data

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class OrganisationPublicProfile(
    val organisationId: String,
    val organisationName: String,
    val organisationType: String,
    val description: String,
    val profileImageUrl: String?,
    val websiteUrl: String,
    val contactPhone: String,
    val contactEmail: String,
    val locationName: String,
    val stateRegion: String,
    val country: String,
    val isVerified: Boolean
)

@Serializable
private data class OrganisationPublicProfileRow(
    @SerialName("organisation_id")
    val organisationId: String,
    @SerialName("organisation_name")
    val organisationName: String,
    @SerialName("organisation_type")
    val organisationType: String? = null,
    val description: String? = null,
    @SerialName("profile_image_path")
    val profileImagePath: String? = null,
    @SerialName("website_url")
    val websiteUrl: String? = null,
    @SerialName("contact_phone")
    val contactPhone: String? = null,
    @SerialName("contact_email")
    val contactEmail: String? = null,
    @SerialName("location_name")
    val locationName: String? = null,
    @SerialName("state_region")
    val stateRegion: String? = null,
    val country: String? = null,
    @SerialName("verification_status")
    val verificationStatus: String? = null
)

/**
 * Read-only, volunteer-facing lookup of an organisation's public info —
 * separate from OrganisationProfileRepository (org-side, edit-capable).
 * Any signed-in volunteer can view any organisation's public profile,
 * same as they can already see the org's name on opportunity cards.
 */
object OrganisationPublicProfileRepository {

    suspend fun getPublicProfile(
        organisationId: String
    ): OrganisationPublicProfile? {
        if (organisationId.isBlank()) return null

        return try {
            val row = supabase
                .from("organisations")
                .select {
                    filter { eq("organisation_id", organisationId) }
                }
                .decodeList<OrganisationPublicProfileRow>()
                .firstOrNull() ?: return null

            OrganisationPublicProfile(
                organisationId = row.organisationId,
                organisationName = row.organisationName,
                organisationType = row.organisationType ?: "",
                description = row.description ?: "",
                profileImageUrl = row.profileImagePath,
                websiteUrl = row.websiteUrl ?: "",
                contactPhone = row.contactPhone ?: "",
                contactEmail = row.contactEmail ?: "",
                locationName = row.locationName ?: "",
                stateRegion = row.stateRegion ?: "",
                country = row.country ?: "",
                isVerified = row.verificationStatus.equals("VERIFIED", ignoreCase = true)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}