package com.example.volunteerlink.chat.repository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.example.volunteerlink.chat.data.ChatMember
import com.example.volunteerlink.chat.data.ChatMessage
import com.example.volunteerlink.chat.data.ChatRoom
import com.example.volunteerlink.chat.data.Role
import com.example.volunteerlink.chat.data.UserProfile
import com.example.volunteerlink.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull

@Serializable
data class PostGroupStatus(
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("eligible_count") val eligibleCount: Int = 0,
    @SerialName("active_member_count") val activeMemberCount: Int = 0,
    @SerialName("missing_count") val missingCount: Int = 0,
    @SerialName("added_count") val addedCount: Int = 0,
    @SerialName("has_started") val hasStarted: Boolean = false,
    @SerialName("can_add") val canAdd: Boolean = false
)

data class LoadedChatData(
    val profile: UserProfile,
    val chats: List<ChatRoom>
)

object SupabaseChatRepository {

    suspend fun loadForSignedInUser(
        viewerRole: Role
    ): LoadedChatData {
        val authUser = supabase.auth.currentUserOrNull()
            ?: error("You must sign in before opening chats.")

        val profileRow = supabase
            .from("user_profiles")
            .select {
                filter {
                    eq("auth_user_id", authUser.id)
                }
            }
            .decodeSingleOrNull<UserProfileRow>()
            ?: error("Your VolunteerLink profile could not be found.")

        val profile = UserProfile(
            id = profileRow.userId,
            name = profileRow.fullName ?: "VolunteerLink user",
            email = authUser.email.orEmpty(),
            bio = "",
            skills = emptyList()
        )

        val chatRows = supabase.postgrest
            .rpc("get_my_event_chat_previews")
            .decodeList<EventChatRow>()

        val chats = chatRows
            .groupBy { it.chatId }
            .map { (chatId, rows) ->
                val first = rows.first()

                val members = rows.map { row ->
                    ChatMember(
                        id = row.memberUserId,
                        name = row.memberName,
                        role = row.memberRole.toChatRole(),
                        initial = row.memberInitial
                    )
                }

                val chatMessages = mutableStateListOf<ChatMessage>()
                first.latestMessageId?.let {
                    chatMessages += first.toLatestMessage()
                }

                ChatRoom(
                    id = chatId,
                    title = first.title,
                    description = first.description,
                    members = members,
                    messages = chatMessages,
                    visibleTo = setOf(viewerRole),
                    readCounts = mutableStateMapOf(
                        viewerRole to chatMessages.size
                    ),
                    isGroup = first.isGroup
                )
            }
            .sortedByDescending { chat ->
                chat.messages.lastOrNull()?.sentAtMillis ?: 0L
            }

        return LoadedChatData(
            profile = profile,
            chats = chats
        )
    }

    suspend fun loadMessagesForChat(
        chatId: String
    ): List<ChatMessage> {
        return loadMessages(chatId)
    }

    suspend fun loadPostGroupStatus(postId: String): PostGroupStatus {
        return supabase.postgrest.rpc(
            function = "organisation_get_post_group_status",
            parameters = buildJsonObject { put("p_post_id", JsonPrimitive(postId)) }
        ).decodeAs<PostGroupStatus>()
    }

    suspend fun addAllAcceptedVolunteers(postId: String): PostGroupStatus {
        return supabase.postgrest.rpc(
            function = "organisation_add_all_to_post_group",
            parameters = buildJsonObject { put("p_post_id", JsonPrimitive(postId)) }
        ).decodeAs<PostGroupStatus>()
    }

    suspend fun openVolunteerDirectChat(postId: String, volunteerUserId: String): String {
        return supabase.postgrest.rpc(
            function = "organisation_open_volunteer_direct_chat",
            parameters = buildJsonObject {
                put("p_post_id", JsonPrimitive(postId))
                put("p_volunteer_user_id", JsonPrimitive(volunteerUserId))
            }
        ).decodeAs<String>()
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        replyToMessageId: String? = null
    ): String {
        return supabase.postgrest.rpc(
            function = "send_conversation_message",
            parameters = buildJsonObject {
                put("p_conversation_id", JsonPrimitive(conversationId))
                put("p_message_text", JsonPrimitive(text.trim()))
                put("p_message_type", JsonPrimitive("TEXT"))
                put("p_attachment_path", JsonNull)
                put("p_attachment_name", JsonNull)
                put("p_attachment_mime_type", JsonNull)
                put("p_reply_to_message_id", replyToMessageId?.let(::JsonPrimitive) ?: JsonNull)
            }
        ).decodeAs<String>()
    }

    suspend fun editMessage(messageId: String, text: String): String {
        return supabase.postgrest.rpc(
            function = "edit_conversation_message",
            parameters = buildJsonObject {
                put("p_message_id", JsonPrimitive(messageId))
                put("p_message_text", JsonPrimitive(text.trim()))
            }
        ).decodeAs<String>()
    }

    suspend fun deleteMessage(messageId: String): String {
        return supabase.postgrest.rpc(
            function = "delete_conversation_message",
            parameters = buildJsonObject { put("p_message_id", JsonPrimitive(messageId)) }
        ).decodeAs<String>()
    }

    suspend fun leaveConversation(conversationId: String): String {
        return supabase.postgrest.rpc(
            function = "leave_conversation",
            parameters = buildJsonObject {
                put("p_conversation_id", JsonPrimitive(conversationId))
            }
        ).decodeAs<String>()
    }

    suspend fun deleteConversationForMe(conversationId: String): String {
        return supabase.postgrest.rpc(
            function = "delete_conversation_for_me",
            parameters = buildJsonObject {
                put("p_conversation_id", JsonPrimitive(conversationId))
            }
        ).decodeAs<String>()
    }

    private suspend fun loadMessages(
        chatId: String
    ): List<ChatMessage> {
        return supabase.postgrest
            .rpc(
                function = "get_event_chat_messages",
                parameters = kotlinx.serialization.json.buildJsonObject {
                    put(
                        "p_chat_id",
                        kotlinx.serialization.json.JsonPrimitive(chatId)
                    )
                }
            )
            .decodeList<EventChatMessageRow>()
            .map { row ->
                val messageType = row.messageType.uppercase()

                ChatMessage(
                    id = row.messageId,
                    senderId = row.senderUserId,
                    senderName = row.senderName,
                    senderInitial = row.senderInitial,
                    senderColor = if (
                        row.senderName.contains(
                            "organisation",
                            ignoreCase = true
                        )
                    ) {
                        0xFF2F4A2E
                    } else {
                        0xFFB8B8B8
                    },
                    text = row.body,
                    sentAtMillis = row.sentAt.toEpochMillis(),
                    imageUri = if (messageType == "IMAGE") {
                        row.attachmentPath
                    } else {
                        null
                    },
                    videoUri = if (messageType == "VIDEO") {
                        row.attachmentPath
                    } else {
                        null
                    },
                    audioUri = if (messageType == "AUDIO") {
                        row.attachmentPath
                    } else {
                        null
                    },
                    fileUri = if (messageType == "FILE") {
                        row.attachmentPath
                    } else {
                        null
                    },
                    fileName = row.attachmentName,
                    fileMimeType = row.attachmentMimeType,
                    replyToId = row.replyToMessageId,
                    isEdited = row.editedAt != null
                )
            }
    }
}

@Serializable
private data class UserProfileRow(
    @SerialName("user_id")
    val userId: String,

    @SerialName("full_name")
    val fullName: String? = null
)

@Serializable
private data class EventChatRow(
    @SerialName("chat_id")
    val chatId: String,

    @SerialName("post_id")
    val postId: String? = null,

    val title: String,
    val description: String,

    @SerialName("member_user_id")
    val memberUserId: String,

    @SerialName("member_name")
    val memberName: String,

    @SerialName("member_role")
    val memberRole: String,

    @SerialName("member_initial")
    val memberInitial: String,

    @SerialName("is_group") val isGroup: Boolean = true,
    @SerialName("latest_message_id") val latestMessageId: String? = null,
    @SerialName("latest_sender_user_id") val latestSenderUserId: String? = null,
    @SerialName("latest_sender_name") val latestSenderName: String? = null,
    @SerialName("latest_sender_initial") val latestSenderInitial: String? = null,
    @SerialName("latest_body") val latestBody: String? = null,
    @SerialName("latest_message_type") val latestMessageType: String? = null,
    @SerialName("latest_attachment_path") val latestAttachmentPath: String? = null,
    @SerialName("latest_attachment_name") val latestAttachmentName: String? = null,
    @SerialName("latest_attachment_mime_type") val latestAttachmentMimeType: String? = null,
    @SerialName("latest_sent_at") val latestSentAt: String? = null,
    @SerialName("latest_edited_at") val latestEditedAt: String? = null
)

private fun EventChatRow.toLatestMessage(): ChatMessage {
    val type = latestMessageType.orEmpty().uppercase()
    return ChatMessage(
        id = latestMessageId.orEmpty(),
        senderId = latestSenderUserId.orEmpty(),
        senderName = latestSenderName ?: "VolunteerLink user",
        senderInitial = latestSenderInitial ?: "V",
        text = latestBody.orEmpty(),
        sentAtMillis = latestSentAt?.toEpochMillis() ?: 0L,
        imageUri = latestAttachmentPath.takeIf { type == "IMAGE" },
        videoUri = latestAttachmentPath.takeIf { type == "VIDEO" },
        audioUri = latestAttachmentPath.takeIf { type == "AUDIO" },
        fileUri = latestAttachmentPath.takeIf { type == "FILE" },
        fileName = latestAttachmentName,
        fileMimeType = latestAttachmentMimeType,
        isEdited = latestEditedAt != null
    )
}

@Serializable
private data class EventChatMessageRow(
    @SerialName("message_id")
    val messageId: String,

    @SerialName("sender_user_id")
    val senderUserId: String,

    @SerialName("sender_name")
    val senderName: String,

    @SerialName("sender_initial")
    val senderInitial: String,

    val body: String,

    @SerialName("message_type")
    val messageType: String,

    @SerialName("attachment_path")
    val attachmentPath: String? = null,

    @SerialName("attachment_name")
    val attachmentName: String? = null,

    @SerialName("attachment_mime_type")
    val attachmentMimeType: String? = null,

    @SerialName("reply_to_message_id")
    val replyToMessageId: String? = null,

    @SerialName("sent_at")
    val sentAt: String,

    @SerialName("edited_at")
    val editedAt: String? = null
)

private fun String.toChatRole(): Role =
    if (equals("ORGANISATION", ignoreCase = true)) {
        Role.ORGANISATION
    } else {
        Role.APPLICANT
    }

private fun String.toEpochMillis(): Long {
    val normalisedTimestamp = replace(
        Regex("(\\.\\d{3})\\d+(?=Z|[+-]\\d{2}:?\\d{2}$)"),
        "$1"
    )
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd HH:mm:ssXXX",
        "yyyy-MM-dd HH:mm:ss.SSSXXX"
    )

    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(normalisedTimestamp)?.time
        }.getOrNull()
    } ?: System.currentTimeMillis()
}
