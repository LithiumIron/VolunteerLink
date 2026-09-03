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
                    isGroup = true
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
    val postId: String,

    val title: String,
    val description: String,

    @SerialName("member_user_id")
    val memberUserId: String,

    @SerialName("member_name")
    val memberName: String,

    @SerialName("member_role")
    val memberRole: String,

    @SerialName("member_initial")
    val memberInitial: String
)

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
            }.parse(this)?.time
        }.getOrNull()
    } ?: System.currentTimeMillis()
}