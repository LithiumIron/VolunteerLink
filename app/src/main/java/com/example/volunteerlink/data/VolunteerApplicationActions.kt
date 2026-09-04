package com.example.volunteerlink.data

// Sends one application request at a time and keeps an outbox record until the server replies.

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.UUID

@Serializable
// Purpose: Handles the volunteer application action result rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerApplicationActionResult(
    val success: Boolean,
    @SerialName("application_status") val status: String? = null,
    val message: String
)

/** Durable account-scoped outbox. Retries reuse the same UUID, never a second application. */
object VolunteerApplicationActions {
    private val mutex = Mutex()
    private fun preferences(context: Context) = context.getSharedPreferences("volunteer_application_outbox_v1", Context.MODE_PRIVATE)
    private fun key(account: String, post: String) = "$account|$post"

    // Purpose: Handles the submit rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun submit(context: Context, post: String, role: String, answers: List<String>,
        previousRole: String?, previousStatus: String?, previousCreatedAt: String?,
        onlineOnly: Boolean): VolunteerApplicationActionResult? = mutex.withLock {
        val account = supabase.auth.currentUserOrNull()?.id ?: error("Sign in again before applying.")
        if (onlineOnly) VolunteerOnline.requireConnection(context, "change a role or apply again")
        val prefs = preferences(context)
        val storageKey = key(account, post)
        check(!prefs.contains(storageKey)) {
            "An earlier request for this event is waiting for confirmation. Sync before trying again."
        }
        // Create one idempotent payload. The request ID lets the server recognise a retry.
        val payload = buildJsonObject {
            put("p_request_id", UUID.randomUUID().toString()); put("p_post_id", post); put("p_role_id", role)
            putJsonArray("p_answers") { answers.forEach { answer -> add(buildJsonObject { put("answer", answer) }) } }
            put("p_previous_role", previousRole?.let(::JsonPrimitive) ?: JsonNull)
            put("p_previous_status", previousStatus?.let(::JsonPrimitive) ?: JsonNull)
            put("p_previous_created_at", previousCreatedAt?.let(::JsonPrimitive) ?: JsonNull)
        }
        // Save before sending so an interrupted request can be retried through Sync.
        withContext(Dispatchers.IO) { check(prefs.edit().putString(storageKey, payload.toString()).commit()) { "Unable to save your request on this device. Nothing was submitted." } }
        // Returning null tells the ViewModel to show an offline Pending item.
        if (!VolunteerOnline.available(context)) return@withLock null
        send(context, account, storageKey, payload)
    }

    // Purpose: Applies the sync data operation and returns only after local/shared state can be updated consistently.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun sync(context: Context) = mutex.withLock {
        val account = supabase.auth.currentUserOrNull()?.id ?: return@withLock
        val pending = preferences(context).all.filterKeys { it.startsWith("$account|") }
        if (pending.isNotEmpty()) VolunteerOnline.requireConnection(context, "sync your applications")
        // Reuse each stored payload instead of creating another application request.
        for ((storageKey, raw) in pending) {
            send(context, account, storageKey, Json.parseToJsonElement(raw as String).jsonObject)
        }
    }

    // Purpose: Handles the pending applications rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun pendingApplications(context: Context, dashboard: VolunteerOpportunityDashboardData): List<com.example.volunteerlink.model.VolunteerOpportunityApplication> {
        val account = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        return preferences(context).all.filterKeys { it.startsWith("$account|") }.values.mapNotNull { raw ->
            runCatching {
                val payload = Json.parseToJsonElement(raw as String).jsonObject
                val event = dashboard.events.firstOrNull { it.eventDatabaseId == payload["p_post_id"]?.jsonPrimitive?.content } ?: return@runCatching null
                val role = event.eventVolunteerRoles.firstOrNull { it.roleTemplateId == payload["p_role_id"]?.jsonPrimitive?.content } ?: return@runCatching null
                if (dashboard.applications.any { it.applicationEventId == event.eventId && it.applicationStatus in setOf(
                        com.example.volunteerlink.model.VolunteerApplicationStatus.PENDING,
                        com.example.volunteerlink.model.VolunteerApplicationStatus.ACCEPTED) }) return@runCatching null
                com.example.volunteerlink.model.VolunteerOpportunityApplication(
                    applicationId = ("offline|" + role.roleDatabaseId).hashCode(),
                    applicationEventId = event.eventId, applicationEventTitle = event.eventTitle,
                    applicationOrganisationName = event.eventOrganisationName,
                    applicationRoleTitle = role.roleTitle, applicationSubmittedDate = "",
                    applicationStatus = com.example.volunteerlink.model.VolunteerApplicationStatus.PENDING,
                    applicationRoleId = role.roleId, applicationRoleMode = role.roleMode,
                    applicationStatusMessage = "Waiting to sync. No confirmed place. Connect and Sync to check the server result.",
                    applicationDatabaseId = "offline|" + role.roleDatabaseId)
            }.getOrNull()
        }
    }

    // Purpose: Handles the send rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    private suspend fun send(context: Context, account: String, storageKey: String,
                             payload: JsonObject): VolunteerApplicationActionResult {
        check(VolunteerRemoteSubmissionRepository.readyAccountId() == account) { "Account changed. Reopen your application." }
        val result = supabase.postgrest.rpc("volunteer_application_action_v1", payload)
            .decodeAs<VolunteerApplicationActionResult>()
        check(supabase.auth.currentUserOrNull()?.id == account) { "Account changed. Reopen your application." }
        // Both success and explicit rejection are final; an exception/timeout keeps
        // the UUID so Sync can safely discover the committed server result.
        withContext(Dispatchers.IO) {
            check(preferences(context).edit().remove(storageKey).commit()) { "Result received; sync again to confirm local storage." }
        }
        return result
    }
}
