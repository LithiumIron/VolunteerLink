package com.example.volunteerlink.organisation.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Owns the Supabase-facing partnership invitation and partnership-chat operations used by Impact Weave.
//
// Requests and chat messages are sent through authenticated RPCs so sender organisation, receiver organisation,
// invitation revision and plan ownership are validated on the server.
//
// The repository exposes structured partnership state to the UI rather than allowing the screen to infer status
// from message text.
//
// Read markers are also server-backed so unread attention remains consistent across sessions/devices.
//
// Architectural layer: Data/repository layer.
// ============================================================================


import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePartnershipItemState
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePartnershipState
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.Locale

/**
 * Holds the values represented by partnership request item as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipRequestItem
 *
 * Domain/UI type for Partnership Request Item used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PartnershipRequestItem(
    val needId: String,
    val supportId: String,
    val requestedAmount: Int?
)

/**
 * Holds the values represented by partnership request result as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipRequestResult
 *
 * Domain/UI type for Partnership Request Result used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PartnershipRequestResult(
    val invitationId: String,
    val conversationId: String,
    val status: String,
    val revisionNumber: Int
)

/**
 * Holds the values represented by partnership response item as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipResponseItem
 *
 * Domain/UI type for Partnership Response Item used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PartnershipResponseItem(
    val resourceName: String,
    val supportType: String,
    val requestedAmount: Int?,
    val providerResourceName: String?
)

/**
 * Holds the values represented by partnership response result as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipResponseResult
 *
 * Domain/UI type for Partnership Response Result used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PartnershipResponseResult(
    val outcome: String,
    val status: String,
    val revisionNumber: Int,
    val message: String,
    val planStatus: String? = null,
    val items: List<PartnershipResponseItem> = emptyList()
)

/**
 * Holds the values represented by partnership invitation summary as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipInvitationSummary
 *
 * Domain/UI type for Partnership Invitation Summary used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PartnershipInvitationSummary(
    val invitationId: String,
    val direction: String,
    val draftId: String,
    val activityTitle: String,
    val activityStartDate: String,
    val areaName: String,
    val otherOrganisationId: String,
    val otherOrganisationName: String,
    val status: String,
    val revisionNumber: Int,
    val supportItemCount: Int,
    val createdAt: String,
    val respondedAt: String? = null
)

/**
 * Holds the values represented by partnership conversation preview as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipConversationPreview
 *
 * Domain/UI type for Partnership Conversation Preview used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PartnershipConversationPreview(
    val conversationId: String,
    val draftId: String,
    val activityTitle: String,
    val otherOrganisationId: String,
    val otherOrganisationName: String,
    val latestMessageType: String? = null,
    val latestMessageText: String? = null,
    val latestMessageAt: String? = null,
    val unreadCount: Int = 0
)

/**
 * Holds the values represented by partnership invitation item as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipInvitationItem
 *
 * Domain/UI type for Partnership Invitation Item used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PartnershipInvitationItem(
    val contributionId: String,
    val needId: String,
    val supportId: String?,
    val supportType: String,
    val resourceName: String,
    val originalText: String,
    val quantityProvided: Int? = null,
    val capacityProvided: Int? = null,
    val quantityRequired: Int? = null,
    val capacityRequired: Int? = null,
    val providerResourceName: String? = null,
    val providerSupportDescription: String? = null,
    val providerQuantity: Int? = null,
    val providerCapacity: Int? = null
)

/**
 * Holds the values represented by partnership message invitation as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipMessageInvitation
 *
 * Domain/UI type for Partnership Message Invitation used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PartnershipMessageInvitation(
    val status: String,
    val currentRevision: Int,
    val isCurrentRevision: Boolean,
    val direction: String,
    val items: List<PartnershipInvitationItem>
)

/**
 * Holds the values represented by partnership chat message as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipChatMessage
 *
 * Domain/UI type for Partnership Chat Message used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PartnershipChatMessage(
    val messageId: String,
    val senderUserId: String?,
    val senderName: String?,
    val messageType: String,
    val messageText: String?,
    val attachmentPath: String?,
    val attachmentName: String?,
    val attachmentMimeType: String?,
    val createdAt: String,
    val invitationId: String?,
    val invitationRevision: Int?,
    val invitation: PartnershipMessageInvitation?
)

/**
 * Holds the values represented by partnership chat as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipChat
 *
 * Domain/UI type for Partnership Chat used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PartnershipChat(
    val conversationId: String,
    val draftId: String,
    val activityCategory: String?,
    val activityTitle: String,
    val activityDescription: String,
    val activityMode: String,
    val activityStatus: String,
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val endTime: String,
    val areaName: String,
    val country: String,
    val activityLocation: String,
    val otherOrganisationId: String,
    val otherOrganisationName: String,
    val otherOrganisationImagePath: String?,
    val currentUserId: String,
    val messages: List<PartnershipChatMessage>
)

/**
 * Groups the shared values and helper behaviour represented by supabase partnership repository.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — SupabasePartnershipRepository
 *
 * Data-access implementation/contract for Supabase Partnership Repository, isolating backend details from the
 * screen and ViewModel layers.
 *
 * Protected server state still relies on authenticated Supabase authorization and database rules rather than
 * trusting client-side checks alone.
 *
 * This implementation translates VolunteerLink models to PostgREST/RPC/Storage operations and maps backend
 * responses back into domain models.
 */
object SupabasePartnershipRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Sends the partnership request for the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — sendPartnershipRequest
     *
     * Performs the repository/data-layer operation for send partnership request.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_send_partnership_request`: Creates a partnership invitation for selected plan
     * needs/supports after validating sender ownership and receiver eligibility.
     */
    suspend fun sendPartnershipRequest(
        draftId: String,
        receiverOrganisationId: String,
        items: List<PartnershipRequestItem>
    ): PartnershipRequestResult {
        require(items.isNotEmpty()) {
            "Select at least one support item for this partnership request."
        }

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_send_partnership_request
        // Creates a partnership invitation for selected plan needs/supports after validating sender ownership
        // and receiver eligibility.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        val response = supabase.postgrest.rpc(
            function = "organisation_send_partnership_request",
            parameters = buildJsonObject {
                put("p_draft_id", draftId)
                put("p_receiver_organisation_id", receiverOrganisationId)
                put("p_need_ids", buildJsonArray {
                    items.forEach { add(JsonPrimitive(it.needId)) }
                })
                put("p_support_ids", buildJsonArray {
                    items.forEach { add(JsonPrimitive(it.supportId)) }
                })
                put("p_requested_amounts", buildJsonArray {
                    items.forEach { item ->
                        item.requestedAmount?.let { add(JsonPrimitive(it)) } ?: add(JsonNull)
                    }
                })
            }
        )

        val result = Json.parseToJsonElement(response.data).jsonObject
        return PartnershipRequestResult(
            invitationId = result["invitation_id"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?: error("Partnership request was sent but no invitation ID was returned."),
            conversationId = result["conversation_id"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?: error("Partnership request was sent but no conversation ID was returned."),
            status = result["status"]?.jsonPrimitive?.contentOrNull ?: "PENDING",
            revisionNumber = result["revision_number"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.toIntOrNull()
                ?: 1
        )
    }

    /**
     * Derives the respond to invitation value used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — respondToInvitation
     *
     * Performs the repository/data-layer operation for respond to invitation.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun respondToInvitation(
        invitationId: String,
        action: String,
        expectedRevision: Int
    ): PartnershipResponseResult {
        val normalizedAction = action.trim().uppercase(Locale.ROOT)
        require(normalizedAction in setOf("ACCEPT", "DECLINE", "RECONFIRM", "DECLINE_RECONFIRMATION")) {
            "Choose Accept, Reconfirm or Decline."
        }

        val response = supabase.postgrest.rpc(
            function = when (normalizedAction) {
                "RECONFIRM" -> "organisation_reconfirm_partnership_invitation"
                "DECLINE_RECONFIRMATION" -> "organisation_decline_partnership_reconfirmation"
                else -> "organisation_respond_partnership_invitation"
            },
            parameters = buildJsonObject {
                put("p_invitation_id", invitationId)
                put("p_expected_revision", expectedRevision)
                if (normalizedAction !in setOf("RECONFIRM", "DECLINE_RECONFIRMATION")) {
                    put("p_action", normalizedAction)
                }
            }
        )

        val decoded = json.decodeFromString<PartnershipResponseRow>(response.data)
        return PartnershipResponseResult(
            outcome = decoded.outcome,
            status = decoded.status,
            revisionNumber = decoded.revisionNumber,
            message = decoded.message,
            planStatus = decoded.planStatus,
            items = decoded.items.map { item ->
                PartnershipResponseItem(
                    resourceName = item.resourceName,
                    supportType = item.supportType,
                    requestedAmount = item.requestedAmount,
                    providerResourceName = item.providerResourceName
                )
            }
        )
    }

    /**
     * Loads the impact weave partnership states needed by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadImpactWeavePartnershipStates
     *
     * Performs the repository/data-layer operation for load impact weave partnership states.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_get_impact_weave_partnership_states`: Returns server-derived invitation/need
     * progress for the current Impact Weave partnership UI.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    suspend fun loadImpactWeavePartnershipStates(
        draftId: String
    ): List<ImpactWeavePartnershipState> {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_get_impact_weave_partnership_states
        // Returns server-derived invitation/need progress for the current Impact Weave partnership UI.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        val response = supabase.postgrest.rpc(
            function = "organisation_get_impact_weave_partnership_states",
            parameters = buildJsonObject {
                put("p_draft_id", draftId)
            }
        )

        val decoded = json.decodeFromString<ImpactWeavePartnershipStatesResponse>(response.data)
        return decoded.requests.map { request ->
            ImpactWeavePartnershipState(
                invitationId = request.invitationId,
                organisationId = request.organisationId,
                organisationName = request.organisationName,
                status = request.status,
                revisionNumber = request.revisionNumber,
                items = request.items.map { item ->
                    ImpactWeavePartnershipItemState(
                        needId = item.needId,
                        supportId = item.supportId,
                        supportType = item.supportType,
                        resourceName = item.resourceName,
                        providerResourceName = item.providerResourceName,
                        quantityProvided = item.quantityProvided,
                        capacityProvided = item.capacityProvided
                    )
                }
            )
        }
    }

    /**
     * Loads the invitations needed by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadInvitations
     *
     * Performs the repository/data-layer operation for load invitations.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadInvitations(): List<PartnershipInvitationSummary> {
        return supabase.postgrest
            .rpc("organisation_list_partnership_invitations")
            .decodeList<PartnershipInvitationRow>()
            .map { row ->
                PartnershipInvitationSummary(
                    invitationId = row.invitationId,
                    direction = row.direction,
                    draftId = row.draftId,
                    activityTitle = row.activityTitle,
                    activityStartDate = row.activityStartDate,
                    areaName = row.areaName,
                    otherOrganisationId = row.otherOrganisationId,
                    otherOrganisationName = row.otherOrganisationName,
                    status = row.status,
                    revisionNumber = row.revisionNumber,
                    supportItemCount = row.supportItemCount,
                    createdAt = row.createdAt,
                    respondedAt = row.respondedAt
                )
            }
    }

    /**
     * Loads the conversation previews needed by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadConversationPreviews
     *
     * Performs the repository/data-layer operation for load conversation previews.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadConversationPreviews(): List<PartnershipConversationPreview> {
        return supabase.postgrest
            .rpc("organisation_list_partnership_conversations")
            .decodeList<PartnershipConversationRow>()
            .map { row ->
                PartnershipConversationPreview(
                    conversationId = row.conversationId,
                    draftId = row.draftId,
                    activityTitle = row.activityTitle,
                    otherOrganisationId = row.otherOrganisationId,
                    otherOrganisationName = row.otherOrganisationName,
                    latestMessageType = row.latestMessageType,
                    latestMessageText = row.latestMessageText,
                    latestMessageAt = row.latestMessageAt,
                    unreadCount = row.unreadCount
                )
            }
    }

    /**
     * Loads the partnership chat needed by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadPartnershipChat
     *
     * Performs the repository/data-layer operation for load partnership chat.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_get_partnership_chat`: Loads the authenticated organisation's partnership
     * conversation with structured invitation/message context.
     */
    suspend fun loadPartnershipChat(conversationId: String): PartnershipChat {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_get_partnership_chat
        // Loads the authenticated organisation's partnership conversation with structured invitation/message
        // context.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        val response = supabase.postgrest.rpc(
            function = "organisation_get_partnership_chat",
            parameters = buildJsonObject {
                put("p_conversation_id", conversationId)
            }
        )

        val decoded = json.decodeFromString<PartnershipChatResponse>(response.data)
        val currentUserId = currentUserId()

        return PartnershipChat(
            conversationId = decoded.conversation.conversationId,
            draftId = decoded.conversation.draftId,
            activityCategory = decoded.conversation.activityCategory,
            activityTitle = decoded.conversation.activityTitle,
            activityDescription = decoded.conversation.activityDescription.orEmpty(),
            activityMode = decoded.conversation.activityMode,
            activityStatus = decoded.conversation.activityStatus,
            startDate = decoded.conversation.startDate,
            endDate = decoded.conversation.endDate,
            startTime = decoded.conversation.startTime,
            endTime = decoded.conversation.endTime,
            areaName = decoded.conversation.areaName,
            country = decoded.conversation.country,
            activityLocation = decoded.conversation.activityLocation.ifBlank { decoded.conversation.areaName },
            otherOrganisationId = decoded.otherOrganisation.organisationId,
            otherOrganisationName = decoded.otherOrganisation.organisationName,
            otherOrganisationImagePath = decoded.otherOrganisation.profileImagePath,
            currentUserId = currentUserId,
            messages = decoded.messages.map { message ->
                PartnershipChatMessage(
                    messageId = message.messageId,
                    senderUserId = message.senderUserId,
                    senderName = message.senderName,
                    messageType = message.messageType,
                    messageText = message.messageText,
                    attachmentPath = message.attachmentPath,
                    attachmentName = message.attachmentName,
                    attachmentMimeType = message.attachmentMimeType,
                    createdAt = message.createdAt,
                    invitationId = message.invitationId,
                    invitationRevision = message.invitationRevision,
                    invitation = message.invitation?.let { invitation ->
                        PartnershipMessageInvitation(
                            status = invitation.status,
                            currentRevision = invitation.currentRevision,
                            isCurrentRevision = invitation.isCurrentRevision,
                            direction = invitation.direction,
                            items = invitation.items.map { item ->
                                PartnershipInvitationItem(
                                    contributionId = item.contributionId,
                                    needId = item.needId,
                                    supportId = item.supportId,
                                    supportType = item.supportType,
                                    resourceName = item.resourceName,
                                    originalText = item.originalText,
                                    quantityProvided = item.quantityProvided,
                                    capacityProvided = item.capacityProvided,
                                    quantityRequired = item.quantityRequired,
                                    capacityRequired = item.capacityRequired,
                                    providerResourceName = item.providerResourceName,
                                    providerSupportDescription = item.providerSupportDescription,
                                    providerQuantity = item.providerQuantity,
                                    providerCapacity = item.providerCapacity
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    /**
     * Sends the text message for the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — sendTextMessage
     *
     * Performs the repository/data-layer operation for send text message.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_send_partnership_message`: Adds an authenticated message to a partnership
     * conversation after server-side membership/plan checks.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun sendTextMessage(
        conversationId: String,
        messageText: String
    ): String {
        require(messageText.isNotBlank()) { "Write a message first." }

        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_send_partnership_message
        // Adds an authenticated message to a partnership conversation after server-side membership/plan checks.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        val response = supabase.postgrest.rpc(
            function = "organisation_send_partnership_message",
            parameters = buildJsonObject {
                put("p_conversation_id", conversationId)
                put("p_message_type", "TEXT")
                put("p_message_text", messageText.trim())
                put("p_attachment_path", JsonNull)
                put("p_attachment_name", JsonNull)
                put("p_attachment_mime_type", JsonNull)
            }
        )

        return runCatching {
            Json.parseToJsonElement(response.data).jsonPrimitive.content
        }.getOrElse {
            response.data.trim().trim('"')
        }
    }

    /**
     * Marks the conversation read with its new state in the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — markConversationRead
     *
     * Performs the repository/data-layer operation for mark conversation read.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_mark_partnership_conversation_read`: Updates the server read marker for a
     * partnership conversation so unread attention is consistent across sessions.
     */
    suspend fun markConversationRead(conversationId: String) {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_mark_partnership_conversation_read
        // Updates the server read marker for a partnership conversation so unread attention is consistent
        // across sessions.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_mark_partnership_conversation_read",
            parameters = buildJsonObject {
                put("p_conversation_id", conversationId)
            }
        )
    }

    /**
     * Returns the current user id value required by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — currentUserId
     *
     * Performs the repository/data-layer operation for current user id.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `user_profiles` (account-level profile identity such as
     * volunteer/organisation user id, name and public profile fields).
     */
    private suspend fun currentUserId(): String {
        val authUser = supabase.auth.currentUserOrNull()
            ?: error("You must sign in before opening partnership chat.")

        return supabase
            // SUPABASE TABLE: user_profiles — account-level profile identity such as volunteer/organisation user id, name and public profile fields.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
            .from("user_profiles")
            .select {
                filter {
                    eq("auth_user_id", authUser.id)
                }
            }
            .decodeSingleOrNull<PartnershipUserProfileRow>()
            ?.userId
            ?: error("Your VolunteerLink profile could not be found.")
    }
}

@Serializable
/**
 * Holds the values represented by partnership user profile row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipUserProfileRow
 *
 * Domain/UI type for Partnership User Profile Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipUserProfileRow(
    @SerialName("user_id") val userId: String
)

@Serializable
/**
 * Holds the values represented by partnership response row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipResponseRow
 *
 * Domain/UI type for Partnership Response Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipResponseRow(
    val outcome: String,
    val status: String,
    @SerialName("revision_number") val revisionNumber: Int,
    val message: String = "",
    @SerialName("plan_status") val planStatus: String? = null,
    val items: List<PartnershipResponseItemRow> = emptyList()
)

@Serializable
/**
 * Holds the values represented by partnership response item row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipResponseItemRow
 *
 * Domain/UI type for Partnership Response Item Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipResponseItemRow(
    @SerialName("resource_name") val resourceName: String,
    @SerialName("support_type") val supportType: String,
    @SerialName("requested_amount") val requestedAmount: Int? = null,
    @SerialName("provider_resource_name") val providerResourceName: String? = null
)

@Serializable
/**
 * Holds the values represented by impact weave partnership states response as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — ImpactWeavePartnershipStatesResponse
 *
 * Domain/UI type for Impact Weave Partnership States Response used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class ImpactWeavePartnershipStatesResponse(
    val requests: List<ImpactWeavePartnershipStateRow> = emptyList()
)

@Serializable
/**
 * Holds the values represented by impact weave partnership state row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — ImpactWeavePartnershipStateRow
 *
 * Domain/UI type for Impact Weave Partnership State Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class ImpactWeavePartnershipStateRow(
    @SerialName("invitation_id") val invitationId: String,
    @SerialName("organisation_id") val organisationId: String,
    @SerialName("organisation_name") val organisationName: String,
    val status: String,
    @SerialName("revision_number") val revisionNumber: Int,
    val items: List<ImpactWeavePartnershipStateItemRow> = emptyList()
)

@Serializable
/**
 * Holds the values represented by impact weave partnership state item row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — ImpactWeavePartnershipStateItemRow
 *
 * Domain/UI type for Impact Weave Partnership State Item Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class ImpactWeavePartnershipStateItemRow(
    @SerialName("need_id") val needId: String,
    @SerialName("support_id") val supportId: String? = null,
    @SerialName("support_type") val supportType: String,
    @SerialName("resource_name") val resourceName: String,
    @SerialName("provider_resource_name") val providerResourceName: String? = null,
    @SerialName("quantity_provided") val quantityProvided: Int? = null,
    @SerialName("capacity_provided") val capacityProvided: Int? = null
)

@Serializable
/**
 * Holds the values represented by partnership invitation row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipInvitationRow
 *
 * Domain/UI type for Partnership Invitation Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipInvitationRow(
    @SerialName("invitation_id") val invitationId: String,
    val direction: String,
    @SerialName("draft_id") val draftId: String,
    @SerialName("activity_title") val activityTitle: String,
    @SerialName("activity_start_date") val activityStartDate: String,
    @SerialName("area_name") val areaName: String,
    @SerialName("other_organisation_id") val otherOrganisationId: String,
    @SerialName("other_organisation_name") val otherOrganisationName: String,
    val status: String,
    @SerialName("revision_number") val revisionNumber: Int,
    @SerialName("support_item_count") val supportItemCount: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("responded_at") val respondedAt: String? = null
)

@Serializable
/**
 * Holds the values represented by partnership conversation row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipConversationRow
 *
 * Domain/UI type for Partnership Conversation Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipConversationRow(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("draft_id") val draftId: String,
    @SerialName("activity_title") val activityTitle: String,
    @SerialName("other_organisation_id") val otherOrganisationId: String,
    @SerialName("other_organisation_name") val otherOrganisationName: String,
    @SerialName("latest_message_type") val latestMessageType: String? = null,
    @SerialName("latest_message_text") val latestMessageText: String? = null,
    @SerialName("latest_message_at") val latestMessageAt: String? = null,
    @SerialName("unread_count") val unreadCount: Int = 0
)

@Serializable
/**
 * Holds the values represented by partnership chat response as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipChatResponse
 *
 * Domain/UI type for Partnership Chat Response used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipChatResponse(
    val conversation: PartnershipConversationDetailRow,
    @SerialName("other_organisation") val otherOrganisation: PartnershipOtherOrganisationRow,
    val messages: List<PartnershipMessageRow> = emptyList()
)

@Serializable
/**
 * Holds the values represented by partnership conversation detail row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipConversationDetailRow
 *
 * Domain/UI type for Partnership Conversation Detail Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipConversationDetailRow(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("draft_id") val draftId: String,
    @SerialName("activity_category") val activityCategory: String? = null,
    @SerialName("activity_title") val activityTitle: String,
    @SerialName("activity_description") val activityDescription: String? = null,
    @SerialName("activity_mode") val activityMode: String = "",
    @SerialName("activity_status") val activityStatus: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("area_name") val areaName: String,
    val country: String,
    @SerialName("activity_location") val activityLocation: String = ""
)

@Serializable
/**
 * Holds the values represented by partnership other organisation row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipOtherOrganisationRow
 *
 * Domain/UI type for Partnership Other Organisation Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipOtherOrganisationRow(
    @SerialName("organisation_id") val organisationId: String,
    @SerialName("organisation_name") val organisationName: String,
    @SerialName("profile_image_path") val profileImagePath: String? = null
)

@Serializable
/**
 * Holds the values represented by partnership message row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipMessageRow
 *
 * Domain/UI type for Partnership Message Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipMessageRow(
    @SerialName("message_id") val messageId: String,
    @SerialName("sender_user_id") val senderUserId: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("message_type") val messageType: String,
    @SerialName("message_text") val messageText: String? = null,
    @SerialName("attachment_path") val attachmentPath: String? = null,
    @SerialName("attachment_name") val attachmentName: String? = null,
    @SerialName("attachment_mime_type") val attachmentMimeType: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("invitation_id") val invitationId: String? = null,
    @SerialName("invitation_revision") val invitationRevision: Int? = null,
    val invitation: PartnershipMessageInvitationRow? = null
)

@Serializable
/**
 * Holds the values represented by partnership message invitation row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipMessageInvitationRow
 *
 * Domain/UI type for Partnership Message Invitation Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipMessageInvitationRow(
    val status: String,
    @SerialName("current_revision") val currentRevision: Int,
    @SerialName("is_current_revision") val isCurrentRevision: Boolean,
    val direction: String,
    val items: List<PartnershipInvitationItemRow> = emptyList()
)

@Serializable
/**
 * Holds the values represented by partnership invitation item row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnershipInvitationItemRow
 *
 * Domain/UI type for Partnership Invitation Item Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PartnershipInvitationItemRow(
    @SerialName("contribution_id") val contributionId: String,
    @SerialName("need_id") val needId: String,
    @SerialName("support_id") val supportId: String? = null,
    @SerialName("support_type") val supportType: String,
    @SerialName("resource_name") val resourceName: String,
    @SerialName("original_text") val originalText: String = "",
    @SerialName("quantity_provided") val quantityProvided: Int? = null,
    @SerialName("capacity_provided") val capacityProvided: Int? = null,
    @SerialName("quantity_required") val quantityRequired: Int? = null,
    @SerialName("capacity_required") val capacityRequired: Int? = null,
    @SerialName("provider_resource_name") val providerResourceName: String? = null,
    @SerialName("provider_support_description") val providerSupportDescription: String? = null,
    @SerialName("provider_quantity") val providerQuantity: Int? = null,
    @SerialName("provider_capacity") val providerCapacity: Int? = null
)
