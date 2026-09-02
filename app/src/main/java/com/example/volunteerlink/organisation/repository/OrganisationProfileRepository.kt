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
    val avatarPath: String? = null
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
    @SerialName("description")
    val description: String? = null,
    @SerialName("contact_email")
    val contactEmail: String? = null,
    @SerialName("contact_phone")
    val contactPhone: String? = null,
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

data class RecentPostSummary(
    val postId: String,
    val title: String,
    val status: String
)

data class OrganisationProfileData(
    val organisationId: String,
    val userId: String,
    val organisationName: String,
    val organisationType: String,
    val description: String,
    val loginEmail: String,
    val contactEmail: String,
    val contactPhone: String,
    val websiteUrl: String,
    val locationName: String,
    val stateRegion: String,
    val country: String,
    val profileImageUrl: String?,
    val verificationStatus: String,
    val memberSince: String,
    val recentPosts: List<RecentPostSummary>
)

object OrganisationProfileRepository {

    // Wrapped end-to-end in try/catch. This function runs inside a
    // LaunchedEffect on the Profile screen — an uncaught exception here has
    // nowhere to go and crashes the whole app rather than just failing to
    // load the screen. OrganisationSession.requireContext() throws
    // IllegalStateException on an incomplete/missing profile, which this
    // catch converts into a plain "couldn't load" null instead of a crash.
    suspend fun loadProfile(): OrganisationProfileData? {
        return try {
            val currentUser = supabase.auth.currentUserOrNull() ?: return null

            // Canonical organisation lookup — the same source Home, Manage,
            // Create Post and Post Management use, so this screen can't
            // silently drift onto a different organisation id than the rest
            // of the app.
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

            // Recently posted events — newest first, capped to a short
            // preview. This replaces "Completed Events" from the
            // volunteer profile, since organisations post events rather
            // than complete them.
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
                // organisationId/organisationName/verificationStatus come
                // from OrganisationSession — the authoritative values — not
                // from the organisations row, even though the row also
                // carries them. One source of truth, no chance of the two
                // disagreeing.
                organisationId = context.organisationId,
                userId = organisation.userId,
                organisationName = context.organisationName,
                organisationType = organisation.organisationType ?: "",
                description = organisation.description ?: "",
                loginEmail = currentUser.email ?: "",
                contactEmail = organisation.contactEmail ?: "",
                contactPhone = organisation.contactPhone ?: "",
                websiteUrl = organisation.websiteUrl ?: "",
                locationName = organisation.locationName ?: "",
                stateRegion = organisation.stateRegion ?: "",
                country = organisation.country ?: "",
                profileImageUrl = organisation.profileImagePath ?: profile?.avatarPath,
                verificationStatus = context.verificationStatus,
                memberSince = memberSince,
                recentPosts = recentPosts
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateProfile(
        organisationName: String,
        contactPhone: String,
        contactEmail: String,
        description: String,
        locationName: String,
        stateRegion: String,
        country: String,
        profileImageUrl: String?
    ): Boolean {
        return try {
            val currentUser = supabase.auth.currentUserOrNull() ?: return false

            // Same canonical lookup as loadProfile() — updates target the
            // organisation OrganisationSession says is current, not one
            // re-derived here independently.
            val organisationId = OrganisationSession.requireOrganisationId()

            // Keep user_profiles.full_name in sync with organisation_name —
            // signup already writes both as the same value, so letting
            // them drift apart here would be confusing anywhere full_name
            // is shown instead of organisation_name (e.g. applicant lists,
            // messages).
            supabase
                .from("user_profiles")
                .update({
                    set("full_name", organisationName)
                    set("avatar_path", profileImageUrl)
                }) {
                    filter { eq("auth_user_id", currentUser.id) }
                }

            supabase
                .from("organisations")
                .update({
                    set("organisation_name", organisationName)
                    set("contact_phone", contactPhone)
                    set("contact_email", contactEmail)
                    set("description", description)
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