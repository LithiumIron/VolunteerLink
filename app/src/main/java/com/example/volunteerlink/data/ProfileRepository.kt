package com.example.volunteerlink.data

import com.example.volunteerlink.model.VolunteerApplicationStatus
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class UserProfileRow(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    val city: String? = null,
    @SerialName("state_region")
    val stateRegion: String? = null,
    val country: String? = null,
    @SerialName("avatar_path")
    val avatarPath: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)


object VolunteerProfileRepository {

    suspend fun requestVolunteerEmailChange(newEmail: String): Result<Unit> {
        return try {
            supabase.auth.updateUser { email = newEmail.trim() }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun verifyVolunteerEmailChangeOtp(newEmail: String, token: String): Result<String> {
        return try {
            try {
                supabase.auth.verifyEmailOtp(
                    type = OtpType.Email.EMAIL_CHANGE,
                    email = newEmail,
                    token = token
                )
            } catch (e: Exception) {
                if (e.message?.contains("UserSession") != true) throw e
            }
            supabase.auth.refreshCurrentSession()
            Result.success(supabase.auth.currentUserOrNull()?.email ?: newEmail)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }


    suspend fun loadProfile():  VolunteerProfileData? {
        val currentUser = supabase.auth.currentUserOrNull() ?: return null

        return try {
            val profile = supabase
                .from("user_profiles")
                .select {
                    filter { eq("auth_user_id", currentUser.id) }
                }
                .decodeSingleOrNull<UserProfileRow>()
                ?: return null

            // Sourced from the same in-memory store Home/SkillPath screens
            // already use for their stats — avoids a duplicate DB round trip
            // and keeps every screen's "completed" definition in sync.
            val completedApplications = VolunteerOpportunitySessionStore
                .volunteerApplications
                .filter { it.applicationStatus == VolunteerApplicationStatus.COMPLETED }

            val certificatedApplications = completedApplications
                .filter { !it.applicationCertificateId.isNullOrBlank() }

            val totalMinutes = completedApplications
                .sumOf { it.applicationVerifiedMinutes ?: 0 }

            val skillPaths = runCatching {
                VolunteerSkillPathRepository.getSkillPaths()
            }.getOrDefault(emptyList())
                .filter { it.verifiedAssignments > 0 }

            VolunteerProfileData(
                userId = currentUser.id,
                fullName = profile.fullName ?: "",
                email = currentUser.email ?: "",
                bio = profile.bio ?: "",
                city = profile.city ?: "",
                stateRegion = profile.stateRegion ?: "",
                country = profile.country ?: "",
                phone = profile.phone ?: "",
                memberSince = profile.createdAt?.take(10) ?: "",
                profileImageUrl = profile.avatarPath,
                verifiedHours = totalMinutes / 60,
                verifiedMinutes = totalMinutes % 60,
                completedEvents = completedApplications,
                certificates = certificatedApplications,
                skillPaths = skillPaths
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateProfile(
        name: String,
        phone: String,
        bio: String,
        city: String,
        stateRegion: String,
        country: String,
        profileImageUrl: String?
    ): Boolean {
        val currentUser = supabase.auth.currentUserOrNull()
        if (currentUser == null) {
            android.util.Log.e("VL_UPDATE", "No current user — not logged in?")
            return false
        }

        return try {
            val updatedRows = supabase
                .from("user_profiles")
                .update({
                    set("full_name", name)
                    set("phone", phone)
                    set("bio", bio)
                    set("city", city)
                    set("state_region", stateRegion)
                    set("country", country)
                    set("avatar_path", profileImageUrl)
                }) {
                    filter { eq("auth_user_id", currentUser.id) }
                    select()
                }
                .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()

            if (updatedRows.isEmpty()) {
                return false
            }

            val existingProfile = VolunteerOpportunitySessionStore.profileData
            if (existingProfile != null) {
                VolunteerOpportunitySessionStore.setProfileData(
                    existingProfile.copy(
                        fullName = name,
                        phone = phone,
                        bio = bio,
                        city = city,
                        stateRegion = stateRegion,
                        country = country,
                        profileImageUrl = profileImageUrl
                    )
                )
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("VL_UPDATE", "Exception during update", e)
            false
        }
    }
}