package com.example.volunteerlink.organisation.repository

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

data class PartnershipRequestItem(
    val needId: String,
    val supportId: String,
    val requestedAmount: Int?
)

data class PartnershipRequestResult(
    val invitationId: String,
    val conversationId: String,
    val status: String,
    val revisionNumber: Int
)

data class PartnershipResponseItem(
    val resourceName: String,
    val supportType: String,
    val requestedAmount: Int?,
    val providerResourceName: String?
)

data class PartnershipResponseResult(
    val outcome: String,
    val status: String,
    val revisionNumber: Int,
    val message: String,
    val planStatus: String? = null,
    val items: List<PartnershipResponseItem> = emptyList()
)

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

data class PartnershipMessageInvitation(
    val status: String,
    val currentRevision: Int,
    val isCurrentRevision: Boolean,
    val direction: String,
    val items: List<PartnershipInvitationItem>
)

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

object SupabasePartnershipRepository {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendPartnershipRequest(
        draftId: String,
        receiverOrganisationId: String,
        items: List<PartnershipRequestItem>
    ): PartnershipRequestResult {
        require(items.isNotEmpty()) {
            "Select at least one support item for this partnership request."
        }

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

    suspend fun respondToInvitation(
        invitationId: String,
        action: String,
        expectedRevision: Int
    ): PartnershipResponseResult {
        val normalizedAction = action.trim().uppercase(Locale.ROOT)
        require(normalizedAction == "ACCEPT" || normalizedAction == "DECLINE") {
            "Choose Accept or Decline."
        }

        val response = supabase.postgrest.rpc(
            function = "organisation_respond_partnership_invitation",
            parameters = buildJsonObject {
                put("p_invitation_id", invitationId)
                put("p_action", normalizedAction)
                put("p_expected_revision", expectedRevision)
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

    suspend fun loadImpactWeavePartnershipStates(
        draftId: String
    ): List<ImpactWeavePartnershipState> {
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

    suspend fun loadPartnershipChat(conversationId: String): PartnershipChat {
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

    suspend fun sendTextMessage(
        conversationId: String,
        messageText: String
    ): String {
        require(messageText.isNotBlank()) { "Write a message first." }

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

    suspend fun markConversationRead(conversationId: String) {
        supabase.postgrest.rpc(
            function = "organisation_mark_partnership_conversation_read",
            parameters = buildJsonObject {
                put("p_conversation_id", conversationId)
            }
        )
    }

    private suspend fun currentUserId(): String {
        val authUser = supabase.auth.currentUserOrNull()
            ?: error("You must sign in before opening partnership chat.")

        return supabase
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
private data class PartnershipUserProfileRow(
    @SerialName("user_id") val userId: String
)

@Serializable
private data class PartnershipResponseRow(
    val outcome: String,
    val status: String,
    @SerialName("revision_number") val revisionNumber: Int,
    val message: String = "",
    @SerialName("plan_status") val planStatus: String? = null,
    val items: List<PartnershipResponseItemRow> = emptyList()
)

@Serializable
private data class PartnershipResponseItemRow(
    @SerialName("resource_name") val resourceName: String,
    @SerialName("support_type") val supportType: String,
    @SerialName("requested_amount") val requestedAmount: Int? = null,
    @SerialName("provider_resource_name") val providerResourceName: String? = null
)

@Serializable
private data class ImpactWeavePartnershipStatesResponse(
    val requests: List<ImpactWeavePartnershipStateRow> = emptyList()
)

@Serializable
private data class ImpactWeavePartnershipStateRow(
    @SerialName("invitation_id") val invitationId: String,
    @SerialName("organisation_id") val organisationId: String,
    @SerialName("organisation_name") val organisationName: String,
    val status: String,
    @SerialName("revision_number") val revisionNumber: Int,
    val items: List<ImpactWeavePartnershipStateItemRow> = emptyList()
)

@Serializable
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
private data class PartnershipChatResponse(
    val conversation: PartnershipConversationDetailRow,
    @SerialName("other_organisation") val otherOrganisation: PartnershipOtherOrganisationRow,
    val messages: List<PartnershipMessageRow> = emptyList()
)

@Serializable
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
private data class PartnershipOtherOrganisationRow(
    @SerialName("organisation_id") val organisationId: String,
    @SerialName("organisation_name") val organisationName: String,
    @SerialName("profile_image_path") val profileImagePath: String? = null
)

@Serializable
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
private data class PartnershipMessageInvitationRow(
    val status: String,
    @SerialName("current_revision") val currentRevision: Int,
    @SerialName("is_current_revision") val isCurrentRevision: Boolean,
    val direction: String,
    val items: List<PartnershipInvitationItemRow> = emptyList()
)

@Serializable
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
