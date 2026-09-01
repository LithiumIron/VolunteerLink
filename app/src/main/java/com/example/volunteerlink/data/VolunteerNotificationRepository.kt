
package com.example.volunteerlink.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.math.absoluteValue

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
) {
    val stableKey: String
        get() = listOf(
            notificationType.uppercase(),
            relatedParticipationId ?: relatedPostId ?: notificationId.toString()
        ).joinToString("|")
}

@Serializable
private data class DismissedNotificationKeyRow(
    @SerialName("notification_key") val notificationKey: String
)

object VolunteerNotificationRepository {
    suspend fun load(): List<VolunteerNotification> {
        val cloudNotifications = runCatching {
            supabase.from("volunteer_notifications")
                .select()
                .decodeList<VolunteerNotification>()
        }.getOrDefault(emptyList())

        val notifications = if (cloudNotifications.isNotEmpty()) {
            cloudNotifications
        } else VolunteerOpportunitySessionStore.volunteerApplications
            .map { application ->
                val type = when (application.applicationStatus) {
                    com.example.volunteerlink.model.VolunteerApplicationStatus.PENDING ->
                        "APPLICATION_SUBMITTED"
                    com.example.volunteerlink.model.VolunteerApplicationStatus.ACCEPTED ->
                        "APPLICATION_ACCEPTED"
                    com.example.volunteerlink.model.VolunteerApplicationStatus.REJECTED ->
                        "APPLICATION_REJECTED"
                    com.example.volunteerlink.model.VolunteerApplicationStatus.COMPLETED ->
                        "CERTIFICATE_ISSUED"
                    com.example.volunteerlink.model.VolunteerApplicationStatus.NOT_COMPLETED ->
                        "APPLICATION_NOT_COMPLETED"
                    com.example.volunteerlink.model.VolunteerApplicationStatus.CANCELLED ->
                        "APPLICATION_CANCELLED"
                }

                VolunteerNotification(
                    notificationId =
                        application.applicationDatabaseId.hashCode()
                            .toLong().absoluteValue,
                    notificationType = type,
                    title = when (type) {
                        "APPLICATION_SUBMITTED" -> "Application submitted"
                        "APPLICATION_ACCEPTED" -> "Place confirmed"
                        "APPLICATION_REJECTED" -> "Application update"
                        "CERTIFICATE_ISSUED" -> "Verified completion"
                        else -> "Application changed"
                    },
                    message = application.applicationStatusMessage.ifBlank {
                        "${application.applicationEventTitle} · " +
                            application.applicationRoleTitle
                    },
                    relatedPostId =
                        VolunteerOpportunitySessionStore
                            .findEventById(application.applicationEventId)
                            ?.eventDatabaseId,
                    relatedParticipationId =
                        application.applicationDatabaseId,
                    isRead = true,
                    createdAt = application.applicationSubmittedDate
                )
            }
        val dismissedKeys = runCatching {
            supabase.postgrest
                .rpc("get_my_dismissed_notification_keys")
                .decodeList<DismissedNotificationKeyRow>()
                .map { it.notificationKey }
                .toSet()
        }.getOrDefault(emptySet())

        return notifications
            .filterNot { it.stableKey in dismissedKeys }
            .sortedByDescending { it.createdAt }
    }

    suspend fun markAllRead() {
        // Let the ViewModel display failure instead of reporting false success.
        supabase.postgrest.rpc("mark_my_notifications_read")
    }

    suspend fun dismiss(notificationKey: String) {
        supabase.postgrest.rpc(
            function = "dismiss_my_volunteer_notification",
            parameters = kotlinx.serialization.json.buildJsonObject {
                put("target_notification_key", notificationKey)
            }
        )
    }

    suspend fun dismissAll(notificationKeys: List<String>) {
        if (notificationKeys.isEmpty()) return
        supabase.postgrest.rpc(
            function = "dismiss_my_volunteer_notifications",
            parameters = kotlinx.serialization.json.buildJsonObject {
                putJsonArray("target_notification_keys") {
                    notificationKeys.forEach { add(JsonPrimitive(it)) }
                }
            }
        )
    }
}
