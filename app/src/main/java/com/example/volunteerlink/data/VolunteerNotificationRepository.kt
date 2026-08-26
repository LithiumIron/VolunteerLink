
package com.example.volunteerlink.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
)

object VolunteerNotificationRepository {
    suspend fun load(): List<VolunteerNotification> {
        val cloudNotifications = runCatching {
            supabase.from("volunteer_notifications")
                .select()
                .decodeList<VolunteerNotification>()
        }.getOrDefault(emptyList())

        if (cloudNotifications.isNotEmpty()) {
            return cloudNotifications.sortedByDescending { it.createdAt }
        }

        // Compatibility fallback while a team database is being migrated:
        // application lifecycle notifications still come from real Supabase
        // dashboard records and never from invented sample data.
        return VolunteerOpportunitySessionStore.volunteerApplications
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
                    com.example.volunteerlink.model.VolunteerApplicationStatus.CANCELLED ->
                        "SCHEDULE_CHANGED"
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
            .sortedByDescending { it.createdAt }
    }

    suspend fun markAllRead() {
        runCatching {
            supabase.postgrest.rpc("mark_my_notifications_read")
        }
    }
}


