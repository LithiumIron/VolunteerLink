package com.example.volunteerlink.chat.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements shared Supabase chat operations used by Organisation conversation screens.
//
// The repository loads conversations/messages/members and sends real messages through authenticated backend paths;
// Compose only calls repository/ViewModel-facing methods.
//
// Server membership and row policies determine what an account can read or send, while device local storage is
// limited to unsent text convenience.
//
// Architectural layer: Data/repository layer.
// ============================================================================


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

@Serializable
/**
 * DETAILED DECLARATION — PostGroupStatus
 *
 * Domain/UI type for Post Group Status used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class PostGroupStatus(
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("eligible_count") val eligibleCount: Int = 0,
    @SerialName("active_member_count") val activeMemberCount: Int = 0,
    @SerialName("missing_count") val missingCount: Int = 0,
    @SerialName("added_count") val addedCount: Int = 0,
    @SerialName("has_started") val hasStarted: Boolean = false,
    @SerialName("can_add") val canAdd: Boolean = false
)

/**
 * DETAILED DECLARATION — LoadedChatData
 *
 * Domain/UI type for Loaded Chat Data used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class LoadedChatData(
    val profile: UserProfile,
    val chats: List<ChatRoom>
)

private const val FORWARDED_MARKER = "\u2063"

/**
 * DETAILED DECLARATION — SupabaseChatRepository
 *
 * Data-access implementation/contract for Supabase Chat Repository, isolating backend details from the screen
 * and ViewModel layers.
 *
 * Protected server state still relies on authenticated Supabase authorization and database rules rather than
 * trusting client-side checks alone.
 *
 * This implementation translates VolunteerLink models to PostgREST/RPC/Storage operations and maps backend
 * responses back into domain models.
 */
object SupabaseChatRepository {

    /**
     * DETAILED BEHAVIOUR — loadForSignedInUser
     *
     * Performs the repository/data-layer operation for load for signed in user.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Reads/maps Supabase table data from `user_profiles` (account-level profile identity such as
     * volunteer/organisation user id, name and public profile fields).
     */
    suspend fun loadForSignedInUser(
        viewerRole: Role
    ): LoadedChatData {
        val authUser = supabase.auth.currentUserOrNull()
            ?: error("You must sign in before opening chats.")

        val profileRow = supabase
            // SUPABASE TABLE: user_profiles — account-level profile identity such as volunteer/organisation user id, name and public profile fields.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
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

    /**
     * DETAILED BEHAVIOUR — loadMessagesForChat
     *
     * Performs the repository/data-layer operation for load messages for chat.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadMessagesForChat(
        chatId: String
    ): List<ChatMessage> {
        return loadMessages(chatId)
    }

    /**
     * DETAILED BEHAVIOUR — loadPostGroupStatus
     *
     * Performs the repository/data-layer operation for load post group status.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_get_post_group_status`: Executes this authenticated database operation; the
     * server remains authoritative for ownership and state changes.
     */
    suspend fun loadPostGroupStatus(postId: String): PostGroupStatus {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_get_post_group_status
        // Executes an authenticated server operation in the VolunteerLink database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        return supabase.postgrest.rpc(
            function = "organisation_get_post_group_status",
            parameters = buildJsonObject { put("p_post_id", JsonPrimitive(postId)) }
        ).decodeAs<PostGroupStatus>()
    }

    /**
     * DETAILED BEHAVIOUR — addAllAcceptedVolunteers
     *
     * Performs the repository/data-layer operation for add all accepted volunteers.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_add_all_to_post_group`: Executes this authenticated database operation; the
     * server remains authoritative for ownership and state changes.
     */
    suspend fun addAllAcceptedVolunteers(postId: String): PostGroupStatus {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_add_all_to_post_group
        // Executes an authenticated server operation in the VolunteerLink database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        return supabase.postgrest.rpc(
            function = "organisation_add_all_to_post_group",
            parameters = buildJsonObject { put("p_post_id", JsonPrimitive(postId)) }
        ).decodeAs<PostGroupStatus>()
    }

    /**
     * DETAILED BEHAVIOUR — joinMyInstantEventChat
     *
     * Performs the repository/data-layer operation for join my instant event chat.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `join_my_instant_event_chat`: Executes this authenticated database operation; the server
     * remains authoritative for ownership and state changes.
     */
    suspend fun joinMyInstantEventChat(
        postId: String
    ): String {
        require(postId.isNotBlank()) {
            "This event does not have a database ID yet."
        }

        // ------------------------------------------------------------------------
        // SUPABASE RPC: join_my_instant_event_chat
        // Executes an authenticated server operation in the VolunteerLink database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        return supabase.postgrest.rpc(
            function = "join_my_instant_event_chat",
            parameters = buildJsonObject {
                put("p_post_id", JsonPrimitive(postId))
            }
        ).decodeAs<String>()
    }

    /**
     * DETAILED BEHAVIOUR — openVolunteerDirectChat
     *
     * Performs the repository/data-layer operation for open volunteer direct chat.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_open_volunteer_direct_chat`: Executes this authenticated database operation;
     * the server remains authoritative for ownership and state changes.
     */
    suspend fun openVolunteerDirectChat(postId: String, volunteerUserId: String): String {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_open_volunteer_direct_chat
        // Executes an authenticated server operation in the VolunteerLink database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        return supabase.postgrest.rpc(
            function = "organisation_open_volunteer_direct_chat",
            parameters = buildJsonObject {
                put("p_post_id", JsonPrimitive(postId))
                put("p_volunteer_user_id", JsonPrimitive(volunteerUserId))
            }
        ).decodeAs<String>()
    }

    /**
     * DETAILED BEHAVIOUR — sendMessage
     *
     * Performs the repository/data-layer operation for send message.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `send_conversation_message`: Executes this authenticated database operation; the server
     * remains authoritative for ownership and state changes.
     */
    suspend fun sendMessage(
        conversationId: String,
        text: String,
        replyToMessageId: String? = null
    ): String {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: send_conversation_message
        // Executes an authenticated server operation in the VolunteerLink database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        return supabase.postgrest.rpc(
            function = "send_conversation_message",
            parameters = buildJsonObject {
                put("p_conversation_id", JsonPrimitive(conversationId))
                put("p_message_text", JsonPrimitive(text.trim()))
                put("p_message_type", JsonPrimitive("TEXT"))
                replyToMessageId?.let {
                    put("p_reply_to_message_id", JsonPrimitive(it))
                }
            }
        ).decodeAs<String>()
    }

    /**
     * DETAILED BEHAVIOUR — editMessage
     *
     * Performs the repository/data-layer operation for edit message.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `edit_conversation_message`: Executes this authenticated database operation; the server
     * remains authoritative for ownership and state changes.
     */
    suspend fun editMessage(messageId: String, text: String): String {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: edit_conversation_message
        // Executes an authenticated server operation in the VolunteerLink database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        return supabase.postgrest.rpc(
            function = "edit_conversation_message",
            parameters = buildJsonObject {
                put("p_message_id", JsonPrimitive(messageId))
                put("p_message_text", JsonPrimitive(text.trim()))
            }
        ).decodeAs<String>()
    }

    /**
     * DETAILED BEHAVIOUR — deleteMessage
     *
     * Performs the repository/data-layer operation for delete message.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `delete_conversation_message`: Executes this authenticated database operation; the server
     * remains authoritative for ownership and state changes.
     */
    suspend fun deleteMessage(messageId: String): String {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: delete_conversation_message
        // Executes an authenticated server operation in the VolunteerLink database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        return supabase.postgrest.rpc(
            function = "delete_conversation_message",
            parameters = buildJsonObject { put("p_message_id", JsonPrimitive(messageId)) }
        ).decodeAs<String>()
    }

    /**
     * DETAILED BEHAVIOUR — forwardMessage
     *
     * Performs the repository/data-layer operation for forward message.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `forward_conversation_message`: Executes this authenticated database operation; the server
     * remains authoritative for ownership and state changes.
     */
    suspend fun forwardMessage(
        sourceMessageId: String,
        targetConversationId: String
    ): String {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: forward_conversation_message
        // Executes an authenticated server operation in the VolunteerLink database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        return supabase.postgrest.rpc(
            function = "forward_conversation_message",
            parameters = buildJsonObject {
                put(
                    "p_source_message_id",
                    JsonPrimitive(sourceMessageId)
                )

                put(
                    "p_target_conversation_id",
                    JsonPrimitive(targetConversationId)
                )
            }
        ).decodeAs<String>()
    }

    /**
     * DETAILED BEHAVIOUR — leaveConversation
     *
     * Performs the repository/data-layer operation for leave conversation.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `leave_conversation`: Executes this authenticated database operation; the server remains
     * authoritative for ownership and state changes.
     */
    suspend fun leaveConversation(
        conversationId: String
    ): String {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: leave_conversation
        // Executes an authenticated server operation in the VolunteerLink database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        return supabase.postgrest.rpc(
            function = "leave_conversation",
            parameters = buildJsonObject {
                put(
                    "p_conversation_id",
                    JsonPrimitive(conversationId)
                )
            }
        ).decodeAs<String>()
    }

    /**
     * DETAILED BEHAVIOUR — deleteConversationForMe
     *
     * Performs the repository/data-layer operation for delete conversation for me.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `delete_conversation_for_me`: Executes this authenticated database operation; the server
     * remains authoritative for ownership and state changes.
     */
    suspend fun deleteConversationForMe(
        conversationId: String
    ): String {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: delete_conversation_for_me
        // Executes an authenticated server operation in the VolunteerLink database.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        return supabase.postgrest.rpc(
            function = "delete_conversation_for_me",
            parameters = buildJsonObject {
                put(
                    "p_conversation_id",
                    JsonPrimitive(conversationId)
                )
            }
        ).decodeAs<String>()
    }

    /**
     * DETAILED BEHAVIOUR — loadMessages
     *
     * Performs the repository/data-layer operation for load messages.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `get_event_chat_messages`: Executes this authenticated database operation; the server
     * remains authoritative for ownership and state changes.
     */
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

                val isForwarded = row.body.startsWith(FORWARDED_MARKER)

                val visibleBody = if (isForwarded) {
                    row.body.removePrefix(FORWARDED_MARKER)
                } else {
                    row.body
                }

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
                    text = visibleBody,
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
                    forwardedFromChatId = if (isForwarded) {
                        "forwarded"
                    } else {
                        null
                    },
                    isEdited = row.editedAt != null,

                )
            }
    }
}

@Serializable
/**
 * DETAILED DECLARATION — UserProfileRow
 *
 * Domain/UI type for User Profile Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class UserProfileRow(
    @SerialName("user_id")
    val userId: String,

    @SerialName("full_name")
    val fullName: String? = null
)

@Serializable
/**
 * DETAILED DECLARATION — EventChatRow
 *
 * Domain/UI type for Event Chat Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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

/**
 * DETAILED BEHAVIOUR — toLatestMessage
 *
 * Performs the repository/data-layer operation for to latest message.
 *
 * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
 * decoding and backend-specific errors.
 */
private fun EventChatRow.toLatestMessage(): ChatMessage {
    val isForwarded =
        latestBody.orEmpty().startsWith(FORWARDED_MARKER)

    val visibleBody = if (isForwarded) {
        latestBody.orEmpty().removePrefix(FORWARDED_MARKER)
    } else {
        latestBody.orEmpty()
    }
    val type = latestMessageType.orEmpty().uppercase()
    return ChatMessage(
        id = latestMessageId.orEmpty(),
        senderId = latestSenderUserId.orEmpty(),
        senderName = latestSenderName ?: "VolunteerLink user",
        senderInitial = latestSenderInitial ?: "V",
        text = visibleBody,
        sentAtMillis = latestSentAt?.toEpochMillis()
            ?: System.currentTimeMillis(),
        imageUri = latestAttachmentPath.takeIf { type == "IMAGE" },
        videoUri = latestAttachmentPath.takeIf { type == "VIDEO" },
        audioUri = latestAttachmentPath.takeIf { type == "AUDIO" },
        fileUri = latestAttachmentPath.takeIf { type == "FILE" },
        fileName = latestAttachmentName,
        fileMimeType = latestAttachmentMimeType,
        isEdited = latestEditedAt != null,
        forwardedFromChatId = if (isForwarded) {
            "forwarded"
        } else {
            null
        }
    )
}

@Serializable
/**
 * DETAILED DECLARATION — EventChatMessageRow
 *
 * Domain/UI type for Event Chat Message Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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

/**
 * DETAILED BEHAVIOUR — toChatRole
 *
 * Performs the repository/data-layer operation for to chat role.
 *
 * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
 * decoding and backend-specific errors.
 */
private fun String.toChatRole(): Role =
    if (equals("ORGANISATION", ignoreCase = true)) {
        Role.ORGANISATION
    } else {
        Role.APPLICANT
    }

/**
 * DETAILED BEHAVIOUR — toEpochMillis
 *
 * Performs the repository/data-layer operation for to epoch millis.
 *
 * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
 * decoding and backend-specific errors.
 *
 * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without leaving
 * the UI in an assumed-success state.
 */
private fun String.toEpochMillis(): Long {
    val timestampWithOffset = trim()
        .replace(
            Regex("""Z$"""),
            "+0000"
        )
        .replace(
            Regex("""([+-]\d{2}):(\d{2})$"""),
            "$1$2"
        )

    val normalizedTimestamp = timestampWithOffset.replace(
        Regex("""\.(\d{1,9})(?=[+-]\d{4}$)""")
    ) { match ->
        "." +
                match.groupValues[1]
                    .take(3)
                    .padEnd(3, '0')
    }

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd HH:mm:ss.SSSZ",
        "yyyy-MM-dd HH:mm:ssZ"
    )

    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(
                pattern,
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(normalizedTimestamp)?.time
        }.getOrNull()
    } ?: System.currentTimeMillis()
}
