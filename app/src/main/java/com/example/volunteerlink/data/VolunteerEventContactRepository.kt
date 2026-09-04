package com.example.volunteerlink.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class VolunteerEventPhoneContactStatus(
    val eligible: Boolean = false,
    val enabled: Boolean = false,
    @SerialName("available_until_label")
    val availableUntilLabel: String? = null,
    val reason: String? = null
)

/**
 * Per-event phone sharing controlled by the volunteer.
 * The server is authoritative: only an accepted active participation can enable
 * sharing. Physical and Remote roles use their own end window, including Hybrid.
 */
object VolunteerEventContactRepository {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadPhoneContactStatus(
        postId: String,
        roleTemplateId: String
    ): VolunteerEventPhoneContactStatus {
        val response = supabase.postgrest.rpc(
            function = "volunteer_get_event_phone_contact",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_role_template_id", roleTemplateId)
            }
        )
        return json.decodeFromString<VolunteerEventPhoneContactStatus>(response.data)
    }

    suspend fun setPhoneContactEnabled(
        postId: String,
        roleTemplateId: String,
        enabled: Boolean
    ): VolunteerEventPhoneContactStatus {
        val response = supabase.postgrest.rpc(
            function = "volunteer_set_event_phone_contact",
            parameters = buildJsonObject {
                put("p_post_id", postId)
                put("p_role_template_id", roleTemplateId)
                put("p_enabled", enabled)
            }
        )
        return json.decodeFromString<VolunteerEventPhoneContactStatus>(response.data)
    }
}
