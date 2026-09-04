package com.example.volunteerlink.data

// Loads a read-only profile summary so the volunteer can check what an organisation will review.

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
// Purpose: Handles the volunteer application profile preview rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerApplicationProfilePreview(
    @SerialName("full_name") val name: String,
    val city: String? = null,
    val bio: String? = null,
    @SerialName("user_id") val userId: String = ""
)

@Serializable
// Purpose: Handles the volunteer preview evidence rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerPreviewEvidence(
    @SerialName("skill_path_id") val pathId: String,
    @SerialName("current_level") val level: Int,
    @SerialName("verified_assignments") val assignments: Int,
    @SerialName("verified_minutes") val minutes: Int? = null
)

@Serializable
private data class VolunteerPreviewPath(@SerialName("skill_path_id") val id: String, val name: String)

// Purpose: Handles the volunteer application preview rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerApplicationPreview(val profile: VolunteerApplicationProfilePreview, val cached: Boolean)

/** Separate, account-scoped preview cache; contains no credentials and never edits a profile. */
object VolunteerApplicationPreviewRepository {
    /** Read-only evidence: never recompute progress or turn a failed read into zero experience. */
    suspend fun evidence(context: Context, expectedAccount: String, userId: String): Map<String, VolunteerPreviewEvidence> {
        VolunteerOnline.requireConnection(context, "load verified experience")
        check(expectedAccount.isNotBlank() && userId.isNotBlank()) { "Reload your profile to see verified experience." }
        check(VolunteerRemoteSubmissionRepository.readyAccountId() == expectedAccount) { "Account changed. Reopen this application." }
        val rows = supabase.from("volunteer_skill_path_progress").select(
            columns = Columns.raw("skill_path_id,current_level,verified_assignments,verified_minutes")
        ) { filter { eq("user_id", userId) } }.decodeList<VolunteerPreviewEvidence>()
        val paths = supabase.from("skill_paths").select(columns = Columns.raw("skill_path_id,name"))
            .decodeList<VolunteerPreviewPath>().associate { it.id to it.name }
        check(supabase.auth.currentUserOrNull()?.id == expectedAccount) { "Account changed. Reopen this application." }
        return rows.associate { (paths[it.pathId] ?: it.pathId) to it }
    }
    // Purpose: Reads  from the data source and returns models that the ViewModel can expose to Compose.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun load(context: Context, expectedAccount: String): VolunteerApplicationPreview {
        check(expectedAccount.isNotBlank()) { "Sign in again before reviewing your application." }
        val preferences = context.getSharedPreferences("volunteer_application_preview_v1", Context.MODE_PRIVATE)
        if (!VolunteerOnline.available(context)) {
            val raw = preferences.getString(expectedAccount, null)
                ?: error("Internet connection is required to load your profile preview for the first time.")
            check(supabase.auth.currentUserOrNull()?.id == expectedAccount) { "Account changed. Reopen this application." }
            return VolunteerApplicationPreview(Json.decodeFromString(raw), true)
        }
        check(VolunteerRemoteSubmissionRepository.readyAccountId() == expectedAccount) { "Account changed. Reopen this application." }
        val profile = supabase.from("user_profiles").select(columns = Columns.raw("user_id,full_name,city,bio")) {
            filter { eq("auth_user_id", expectedAccount); eq("account_type", "VOLUNTEER") }
        }.decodeList<VolunteerApplicationProfilePreview>().singleOrNull()
            ?: error("Your volunteer profile is unavailable. Sign in again.")
        check(supabase.auth.currentUserOrNull()?.id == expectedAccount) { "Account changed. Reopen this application." }
        preferences.edit().putString(expectedAccount, Json.encodeToString(profile)).apply()
        return VolunteerApplicationPreview(profile, false)
    }
}
