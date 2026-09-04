package com.example.volunteerlink.organisation.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Reads and updates the authenticated organisation's own public/profile information using normalized
// user_profiles, organisations, organisation_supports and volunteer_posts data.
//
// Profile editing is separate from Volunteer Post management: it updates organisation identity/contact/support
// metadata, while verification and ownership remain backend-controlled.
//
// The repository maps Supabase rows into profile models so Compose does not depend on raw JSON or table schemas.
//
// Architectural layer: Data/repository layer.
// ============================================================================


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
/**
 * DETAILED DECLARATION — UserProfileRow
 *
 * Domain/UI type for User Profile Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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
/**
 * DETAILED DECLARATION — OrganisationRow
 *
 * Domain/UI type for Organisation Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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
/**
 * DETAILED DECLARATION — VolunteerPostSummaryRow
 *
 * Domain/UI type for Volunteer Post Summary Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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
/**
 * DETAILED DECLARATION — OrganisationSupportRow
 *
 * Domain/UI type for Organisation Support Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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
/**
 * DETAILED DECLARATION — OrganisationSupportInsert
 *
 * Domain/UI type for Organisation Support Insert used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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

/**
 * DETAILED DECLARATION — RecentPostSummary
 *
 * Domain/UI type for Recent Post Summary used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class RecentPostSummary(
    val postId: String,
    val title: String,
    val status: String
)

/**
 * DETAILED DECLARATION — OrganisationSupportData
 *
 * Domain/UI type for Organisation Support Data used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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

/**
 * DETAILED DECLARATION — OrganisationProfileData
 *
 * Domain/UI type for Organisation Profile Data used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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

/**
 * DETAILED DECLARATION — OrganisationProfileRepository
 *
 * Data-access implementation/contract for Organisation Profile Repository, isolating backend details from the
 * screen and ViewModel layers.
 *
 * Protected server state still relies on authenticated Supabase authorization and database rules rather than
 * trusting client-side checks alone.
 */
object OrganisationProfileRepository {

    /**
     * DETAILED BEHAVIOUR — loadProfile
     *
     * Loads the signed-in organisation profile by joining the account-level user profile with organisation-
     * specific public information, support records and recent post data.
     *
     * The method returns one profile model for Compose rather than exposing four separate Supabase table result
     * shapes.
     *
     * Profile visibility/editability is still governed by the authenticated account and database policies.
     *
     * Reads/maps Supabase table data from `organisations` (organisation-specific identity, verification, public
     * contact/location and partnership availability); `user_profiles` (account-level profile identity such as
     * volunteer/organisation user id, name and public profile fields); `organisation_supports`
     * (resources/services an organisation has declared it can provide to partnership activities);
     * `volunteer_posts` (the parent Volunteer Post record, including owner, mode, lifecycle status, category
     * and publication metadata).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun loadProfile(): OrganisationProfileData? {
        return try {
            val currentUser = supabase.auth.currentUserOrNull() ?: return null
            val context = OrganisationSession.requireContext()

            val organisation = supabase
                // SUPABASE TABLE: organisations — organisation-specific identity, verification, public contact/location and partnership availability.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
                .from("organisations")
                .select {
                    filter { eq("organisation_id", context.organisationId) }
                }
                .decodeList<OrganisationRow>()
                .firstOrNull() ?: return null

            val profile = supabase
                // SUPABASE TABLE: user_profiles — account-level profile identity such as volunteer/organisation user id, name and public profile fields.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
                .from("user_profiles")
                .select {
                    filter { eq("user_id", organisation.userId) }
                }
                .decodeList<UserProfileRow>()
                .firstOrNull()

            val supports = try {
                supabase
                    // SUPABASE TABLE: organisation_supports — resources/services an organisation has declared it can provide to partnership activities.
                    // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
                    // SUPABASE TABLE: volunteer_posts — the parent Volunteer Post record, including owner, mode, lifecycle status, category and publication metadata.
                    // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

    /**
     * DETAILED BEHAVIOUR — updateOpenToPartnership
     *
     * Performs the repository/data-layer operation for update open to partnership.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `organisations` (organisation-specific identity, verification, public
     * contact/location and partnership availability).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun updateOpenToPartnership(openToPartnership: Boolean): Boolean {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            supabase
                // SUPABASE TABLE: organisations — organisation-specific identity, verification, public contact/location and partnership availability.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

    /**
     * DETAILED BEHAVIOUR — requestEmailChange
     *
     * Performs the repository/data-layer operation for request email change.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
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

    /**
     * DETAILED BEHAVIOUR — refreshLoginEmail
     *
     * Performs the repository/data-layer operation for refresh login email.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun refreshLoginEmail(): String? {
        return try {
            supabase.auth.refreshCurrentSession()
            supabase.auth.currentUserOrNull()?.email
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    /**
     * DETAILED BEHAVIOUR — addSupport
     *
     * Performs the repository/data-layer operation for add support.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `organisation_supports` (resources/services an organisation has
     * declared it can provide to partnership activities).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
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
                // SUPABASE TABLE: organisation_supports — resources/services an organisation has declared it can provide to partnership activities.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

    /**
     * DETAILED BEHAVIOUR — updateSupport
     *
     * Performs the repository/data-layer operation for update support.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
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
                // SUPABASE TABLE: organisation_supports — resources/services an organisation has declared it can provide to partnership activities.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

    /**
     * DETAILED BEHAVIOUR — removeSupport
     *
     * Performs the repository/data-layer operation for remove support.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `organisation_supports` (resources/services an organisation has
     * declared it can provide to partnership activities).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun removeSupport(supportId: String): Boolean {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            supabase
                // SUPABASE TABLE: organisation_supports — resources/services an organisation has declared it can provide to partnership activities.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

    /**
     * DETAILED BEHAVIOUR — updateContactPhone
     *
     * Performs the repository/data-layer operation for update contact phone.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `organisations` (organisation-specific identity, verification, public
     * contact/location and partnership availability).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun updateContactPhone(contactPhone: String): Boolean {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            // SUPABASE TABLE: organisations — organisation-specific identity, verification, public contact/location and partnership availability.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

    /**
     * DETAILED BEHAVIOUR — updateContactEmail
     *
     * Performs the repository/data-layer operation for update contact email.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `organisations` (organisation-specific identity, verification, public
     * contact/location and partnership availability).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun updateContactEmail(contactEmail: String): Boolean {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            // SUPABASE TABLE: organisations — organisation-specific identity, verification, public contact/location and partnership availability.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

    /**
     * DETAILED BEHAVIOUR — updateDescription
     *
     * Performs the repository/data-layer operation for update description.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `organisations` (organisation-specific identity, verification, public
     * contact/location and partnership availability).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun updateDescription(description: String): Boolean {
        return try {
            val organisationId = OrganisationSession.requireOrganisationId()
            // SUPABASE TABLE: organisations — organisation-specific identity, verification, public contact/location and partnership availability.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

    /**
     * DETAILED BEHAVIOUR — updateProfile
     *
     * Performs the repository/data-layer operation for update profile.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `user_profiles` (account-level profile identity such as
     * volunteer/organisation user id, name and public profile fields); `organisations` (organisation-specific
     * identity, verification, public contact/location and partnership availability).
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
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
                // SUPABASE TABLE: user_profiles — account-level profile identity such as volunteer/organisation user id, name and public profile fields.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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
                // SUPABASE TABLE: organisations — organisation-specific identity, verification, public contact/location and partnership availability.
                // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

/**
 * DETAILED BEHAVIOUR — toData
 *
 * Performs the repository/data-layer operation for to data.
 *
 * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
 * decoding and backend-specific errors.
 */
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