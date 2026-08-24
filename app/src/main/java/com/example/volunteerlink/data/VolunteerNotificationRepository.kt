package com.example.volunteerlink.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VolunteerNotification(
    @SerialName("notification_id")
    val notificationId: Long,
    @SerialName("notification_type")
    val notificationType: String,
    val title: String,
    val message: String,
    @SerialName("related_post_id")
    val relatedPostId: String? = null,
    @SerialName("related_participation_id")
    val relatedParticipationId: String? = null,
    @SerialName("is_read")
    val isRead: Boolean,
    @SerialName("created_at")
    val createdAt: String
)

object VolunteerNotificationRepository {
    suspend fun load(): List<VolunteerNotification> =
        supabase.from("volunteer_notifications")
            .select()
            .decodeList<VolunteerNotification>()
            .sortedByDescending { it.createdAt }

    suspend fun markAllRead() {
        supabase.postgrest.rpc("mark_my_notifications_read")
    }
}
