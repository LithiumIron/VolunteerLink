package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.auth.OrganisationSession
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
private data class UserProfileRow(
    @SerialName("user_id")
    val userId: String,
    @SerialName("avatar_path")
    val avatarPath: String? = null,
    // Bio/phone here are the sign-up-time, account-level fields edited on
    // EditOrganisationProfileScreen — same user_profiles columns the
    // volunteer side uses. Distinct from organisations.description /
    // contact_phone, which are the public-facing fields edited in Settings.
    @SerialName("bio")
    val bio: String? = null,
    @SerialName("phone")
    val phone: String? = null
)

@Serializable
private data class OrganisationRow(
    @SerialName("organisation_id")
    val organisationId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("organisation_name")
    val organisationName: String,
    @SerialName("organisation_type")
    val organisationType: String? = null,
    // Public-facing fields, edited from OrganisationSettingScreen.
    val description: String? = null,
    @SerialName("contact_phone")
    val contactPhone: String? = null,
    @SerialName("contact_email")
    val contactEmail: String? = null,
    @SerialName("website_url")
    val websiteUrl: String? = null,
    @SerialName("profile_image_path")
    val profileImagePath: String? = null,
    @SerialName("location_name")
    val locationName: String? = null,
    @SerialName("state_region")
    val stateRegion: String? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("open_to_partnership")
    val openToPartnership: Boolean = false,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
private data class VolunteerPostSummaryRow(
    @SerialName("post_id")
    val postId: String,
    @SerialName("title")
    val title: String,
    @SerialName("status")
    val status: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
private data class OrganisationSupportRow(
    @SerialName("support_id")
    val supportId: String,
    @SerialName("organisation_id")
    val organisationId: String,
    @SerialName("support_description")
    val supportDescription: String,
    @SerialName("support_type")
    val supportType: String,
    @SerialName("resource_name")
    val resourceName: String,
    @SerialName("quantity")
    val quantity: Int? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("location_name")
    val locationName: String? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null
)

@Serializable
private data class OrganisationSupportInsert(
    @SerialName("organisation_id")
    val organisationId: String,
    @SerialName("support_description")
    val supportDescription: String,
    @SerialName("support_type")
    val supportType: String,
    @SerialName("resource_name")
    val resourceName: String,
    @SerialName("quantity")
    val quantity: Int? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("location_name")
    val locationName: String? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null
)

data class RecentPostSummary(
    val postId: String,
    val title: String,
    val status: String
)

data class OrganisationSupportData(
    val supportId: String,
    val supportDescription: String,
    val supportType: String,
    val resourceName: String,
    val quantity: Int?,
    val capacity: Int?,
    val locationName: String?,
    val country: String?,
    val latitude: Double?,
    val longitude: Double?
)

data class OrganisationProfileData(
    val organisationId: String,
    val userId: String,
    val organisationName: String,
    val organisationType: String,

    // ---- Public-facing (organisations table) — edited in Settings ----
    val description: String,
    val contactPhone: String,
    val contactEmail: String,

    // ---- Account-level (user_profiles table) — edited in EditProfile,
    // same columns used at sign-up ----
    val bio: String,
    val registeredPhone: String,

    // Real Supabase Auth login email — read-only everywhere, unrelated
    // to contactEmail above.
    val loginEmail: String,

    val websiteUrl: String,
    val locationName: String,
    val stateRegion: String,
    val country: String,
    val profileImageUrl: String?,
    val verificationStatus: String,
    val memberSince: String,
    val openToPartnership: Boolean,
    val supports: List<OrganisationSupportData>,
    val recentPosts: List<RecentPostSummary>
)

object OrganisationProfileRepository {

    suspend fun loadProfile(): OrganisationProfileData? {
        return try {
            val currentUser = supabase.auth.currentUserOrNull() ?: return null
            val context = OrganisationSession.requireContext()

            val organisation = supabase
                .from("organisations")
                .select {
                    filter { eq("organisation_id", context.organisationId) }
                }
                .decodeList<OrganisationRow>()
                .firstOrNull() ?: return null

            val profile = supabase
                .from("user_profiles")
                .select {
                    filter { eq("user_id", organisation.userId) }
                }
                .decodeList<UserProfileRow>()
                .firstOrNull()

            val supports = try {
                supabase
                    .from("organisation_supports")
                    .select {
                        filter { eq("organisation_id", context.organisationId) }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<OrganisationSupportRow>()
                    .map { it.toData() }
            } catch (_: Exception) {
                emptyList()
            }

            val recentPosts = try {
                supabase
                    .from("volunteer_posts")
                    .select {
                        filter { eq("organisation_id", context.organisationId) }
                        order("created_at", Order.DESCENDING)
                        limit(5)
                    }
                    .decodeList<VolunteerPostSummaryRow>()
                    .map {
                        RecentPostSummary(
                            postId = it.postId,
                            title = it.title,
                            status = it.status
                        )
                    }
            } catch (_: Exception) {
                emptyList()
            }

            val memberSince = try {
                val datePart = organisation.createdAt.substring(0, 10)
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val date = inputFormat.parse(datePart)
                date?.let { outputFormat.format(it) } ?: "Unknown"
            } catch (_: Exception) {
                "Unknown"
            }

            OrganisationProfileData(
                organisationId = context.organisationId,
                userId = organisation.userId,
                organisationName = context.organisationName,
                organisationType = organisation.organisationType ?: "",
                description = organisation.description ?: "",
                contactPhone = organisation.contactPhone ?: "",
                contactEmail = organisation.contactEmail ?: "",
                bio = profile?.bio ?: "",
                registeredPhone = profile?.phone ?: "",
                loginEmail = currentUser.email ?: "",
                websiteUrl = organisation.websiteUrl ?: "",
                locationName = organisation.locationName ?: "",
                stateRegion = organisation.stateRegion ?: "",
                country = organisation.country ?: "",
                profileImageUrl = organisation.profileImagePath ?: profile?.avatarPath,
                verificationStatus = context.verificationStatus,
                memberSince = memberSince,
                openToPartnership = organisation.openToPartnership,
                supports = supports,
                recentPosts = recentPosts
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateOpenToPartnership(openToPartnership: Boolean): Boolean {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            supabase
                .from("organisations")
                .update({
                    set("open_to_partnership", openToPartnership)
                }) {
                    filter { eq("organisation_id", organisationId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun requestEmailChange(newEmail: String): Boolean {
        return try {
            supabase.auth.updateUser {
                email = newEmail.trim()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun refreshLoginEmail(): String? {
        return try {
            supabase.auth.refreshCurrentSession()
            supabase.auth.currentUserOrNull()?.email
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    suspend fun addSupport(
        supportDescription: String,
        supportType: String,
        resourceName: String,
        amount: Int?,
        locationName: String? = null,
        country: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): OrganisationSupportData? {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            val isVenue = supportType == "VENUE"

            val row = supabase
                .from("organisation_supports")
                .insert(
                    OrganisationSupportInsert(
                        organisationId = organisationId,
                        supportDescription = supportDescription.trim(),
                        supportType = supportType,
                        resourceName = resourceName.trim(),
                        quantity = if (isVenue) null else amount,
                        capacity = if (isVenue) amount else null,
                        locationName = if (isVenue) locationName else null,
                        country = if (isVenue) country else null,
                        latitude = if (isVenue) latitude else null,
                        longitude = if (isVenue) longitude else null
                    )
                ) {
                    select()
                }
                .decodeSingle<OrganisationSupportRow>()

            row.toData()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateSupport(
        supportId: String,
        supportDescription: String,
        supportType: String,
        resourceName: String,
        amount: Int?,
        locationName: String? = null,
        country: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): OrganisationSupportData? {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            val isVenue = supportType == "VENUE"

            val row = supabase
                .from("organisation_supports")
                .update({
                    set("support_description", supportDescription.trim())
                    set("support_type", supportType)
                    set("resource_name", resourceName.trim())
                    set("quantity", if (isVenue) null else amount)
                    set("capacity", if (isVenue) amount else null)
                    set("location_name", if (isVenue) locationName else null)
                    set("country", if (isVenue) country else null)
                    set("latitude", if (isVenue) latitude else null)
                    set("longitude", if (isVenue) longitude else null)
                }) {
                    select()
                    filter {
                        eq("support_id", supportId)
                        eq("organisation_id", organisationId)
                    }
                }
                .decodeSingle<OrganisationSupportRow>()

            row.toData()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun removeSupport(supportId: String): Boolean {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            supabase
                .from("organisation_supports")
                .delete {
                    filter {
                        eq("support_id", supportId)
                        eq("organisation_id", organisationId)
                    }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // =========================================================
    // SETTINGS SCREEN — public-facing fields on `organisations`
    // =========================================================

    suspend fun updateContactPhone(contactPhone: String): Boolean {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            supabase.from("organisations").update({
                set("contact_phone", contactPhone)
            }) {
                filter { eq("organisation_id", organisationId) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateContactEmail(contactEmail: String): Boolean {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            supabase.from("organisations").update({
                set("contact_email", contactEmail)
            }) {
                filter { eq("organisation_id", organisationId) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateDescription(description: String): Boolean {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            supabase.from("organisations").update({
                set("description", description)
            }) {
                filter { eq("organisation_id", organisationId) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // =========================================================
    // EDIT PROFILE SCREEN — account-level fields on `user_profiles`,
    // same columns populated at sign-up. Unrelated to the three
    // functions above.
    // =========================================================

    suspend fun updateProfile(
        organisationName: String,
        registeredPhone: String,
        bio: String,
        locationName: String,
        stateRegion: String,
        country: String,
        profileImageUrl: String?
    ): Boolean {
        return try {
            val currentUser = supabase.auth.currentUserOrNull() ?: return false
            val organisationId = OrganisationSession.requireOrganisationId()

            supabase
                .from("user_profiles")
                .update({
                    set("full_name", organisationName)
                    set("avatar_path", profileImageUrl)
                    set("bio", bio)
                    set("phone", registeredPhone)
                }) {
                    filter { eq("auth_user_id", currentUser.id) }
                }

            supabase
                .from("organisations")
                .update({
                    set("organisation_name", organisationName)
                    set("location_name", locationName)
                    set("state_region", stateRegion)
                    set("country", country)
                    set("profile_image_path", profileImageUrl)
                }) {
                    filter { eq("organisation_id", organisationId) }
                }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

private fun OrganisationSupportRow.toData(): OrganisationSupportData {
    return OrganisationSupportData(
        supportId = supportId,
        supportDescription = supportDescription,
        supportType = supportType,
        resourceName = resourceName,
        quantity = quantity,
        capacity = capacity,
        locationName = locationName,
        country = country,
        latitude = latitude,
        longitude = longitude
    )
}