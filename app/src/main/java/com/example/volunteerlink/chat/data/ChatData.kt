package com.example.volunteerlink.chat.data

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines shared chat models/state helpers consumed by Organisation chat screens and the chat repository.
//
// Conversation/message/member models represent server data in a Compose-friendly form, while unread helpers derive
// badge counts for the bottom navigation and chat lists.
//
// Unsent Organisation text drafts may be stored locally, but sent messages and membership remain Supabase-
// authoritative.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf


/**
 * DETAILED DECLARATION — MessageListener
 *
 * Contract for Message Listener. Callers depend on this abstraction rather than a concrete Supabase
 * implementation.
 *
 * Implementations may perform network/storage work, while ViewModels and Compose remain expressed in
 * VolunteerLink domain types.
 */
interface MessageListener {
    /**
     * DETAILED BEHAVIOUR — onNewMessage
     *
     * Implements the current VolunteerLink responsibility for on new message in this support/model layer.
     */
    fun onNewMessage(chatId: String, chatTitle: String, senderName: String, text: String)
}
/**
 * DETAILED DECLARATION — ChatData
 *
 * Single shared instance for Chat Data so related rules/state are defined once for the application process.
 */
object ChatData {

    var messageListener: MessageListener? = null
    val partnershipAttention = mutableStateOf(0)

    // Which role the demo is currently running as. Toggled from the Role select screen
    // and again from each Profile / Manage screen ("Switch account" button).
    val currentRole = mutableStateOf(Role.APPLICANT)

    val currentUser = mutableStateOf(
        UserProfile(
            id = "me_applicant",
            name = "Alex Tan",
            email = "alex.tan@email.com",
            bio = "Passionate about community service and design.",
            skills = listOf("Graphic Design", "Event Planning", "First Aid")
        )
    )

    val orgProfile = mutableStateOf(
        UserProfile(
            id = "me_org",
            name = "Organisation A",
            email = "contact@organisationa.org",
            bio = "Non-profit organising charity runs and community campaigns.",
            skills = listOf("Event Management", "Fundraising")
        )
    )

    // ---------- Map / Nearby events ----------
    // Coordinates are real points around George Town, Penang so they're ready to drop
    // straight onto a real map SDK later (see MapScreen.kt for the placeholder Canvas map).
    val events = mutableStateListOf(
        VolunteerEvent(
            id = "event_charity_run",
            title = "Charity Fun Run 2026",
            organisation = "Organisation A",
            distanceKm = 1.8,
            date = "27 Jul",
            spotsLeft = 5,
            latitude = 5.4164,
            longitude = 100.3327
        ),
        VolunteerEvent(
            id = "event_poster_design",
            title = "Poster Design",
            organisation = "Organisation A",
            distanceKm = 2.3,
            date = "12 Aug",
            spotsLeft = 2,
            latitude = 5.4141,
            longitude = 100.3288
        ),
        VolunteerEvent(
            id = "event_one_day_camp",
            title = "One Day Camp",
            organisation = "Organisation B",
            distanceKm = 3.1,
            date = "18 Aug",
            spotsLeft = 8,
            latitude = 5.4108,
            longitude = 100.3352
        ),
        VolunteerEvent(
            id = "event_logo_design",
            title = "Logo Design",
            organisation = "Organisation A",
            distanceKm = 2.7,
            date = "20 Aug",
            spotsLeft = 3,
            latitude = 5.4195,
            longitude = 100.3399
        )
    )

    /**
     * DETAILED BEHAVIOUR — eventById
     *
     * Implements the current VolunteerLink responsibility for event by id in this support/model layer.
     */
    fun eventById(id: String): VolunteerEvent? = events.find { it.id == id }

    // ---------- Chats ----------
    // Single shared source of truth: a room with both roles in `visibleTo` is the SAME
    // conversation on both the applicant and organisation side (same object, same messages
    // list), so sending a message as one role is instantly visible to the other - no separate
    // "applicant copy" and "organisation copy" to keep in sync.
    val allChats = mutableStateListOf(
        ChatRoom(
            id = "chat_charity_run_2026",
            title = "Charity Fun Run 2026",
            description = "Official group for the 2026 Charity Fun Run. Organisers post event-day logistics here and volunteers can ask questions or confirm attendance.",
            members = listOf(
                ChatMember("me_org", "Organisation A", Role.ORGANISATION, "O"),
                ChatMember("me_applicant", "Alex Tan", Role.APPLICANT, "A"),
                ChatMember("member_b", "Bella Wong", Role.APPLICANT, "B"),
                ChatMember("member_c", "Chris Lee", Role.APPLICANT, "C")
            ),
            visibleTo = setOf(Role.APPLICANT, Role.ORGANISATION),
            messages = mutableStateListOf(
                ChatMessage("m1", "me_org", "Organisation A", "O", 0xFF2F4A2E, "Good morning everyone! Please arrive by 7:45 AM for tomorrow event.", pastTimestamp(daysAgo = 1, hour = 9, minute = 50)),
                ChatMessage("m2", "member_b", "Bella Wong", "B", 0xFFB8B8B8, "No Problem!", pastTimestamp(daysAgo = 1, hour = 10, minute = 14)),
                ChatMessage("m3", "me_applicant", "Alex Tan", "A", 0xFFD8C08A, "Alright!", pastTimestamp(daysAgo = 1, hour = 10, minute = 16)),
                ChatMessage("m4", "member_c", "Chris Lee", "C", 0xFF9BB89B, "Ok", pastTimestamp(daysAgo = 1, hour = 10, minute = 17))
            )
        ),
        ChatRoom(
            id = "chat_logo_design",
            title = "Logo Design",
            description = "Discussion thread for the Logo Design volunteer task - share drafts and feedback here.",
            members = listOf(
                ChatMember("me_applicant", "Alex Tan", Role.APPLICANT, "A"),
                ChatMember("member_org_a", "Organisation A", Role.ORGANISATION, "O")
            ),
            visibleTo = setOf(Role.APPLICANT), // organisation doesn't have this one open on their side yet
            messages = mutableStateListOf(
                ChatMessage("m1", "member_org_a", "Organisation A", "O", 0xFFB8B8B8, "Remember to prepare necessary design files before submission.", pastTimestamp(daysAgo = 0, hour = 8, minute = 3))
            )
        ),
        ChatRoom(
            id = "chat_charity_run_2025",
            title = "Charity Fun Run 2025",
            description = "Archive chat from the 2025 Charity Fun Run.",
            members = listOf(
                ChatMember("me_org", "Organisation A", Role.ORGANISATION, "O")
            ),
            visibleTo = setOf(Role.ORGANISATION), // last year's run - applicant side never had this thread
            messages = mutableStateListOf(
                ChatMessage("m1", "me_org", "Organisation A", "O", 0xFF2F4A2E, "Thank you for all the volunteers who joined us last year!", pastTimestamp(daysAgo = 365, hour = 18, minute = 40))
            )
        )
    )

    /**
     * DETAILED BEHAVIOUR — updateSignedInProfile
     *
     * Implements the current VolunteerLink responsibility for update signed in profile in this support/model
     * layer.
     */
    fun updateSignedInProfile(
        role: Role,
        profile: UserProfile
    ) {
        when (role) {
            Role.APPLICANT -> currentUser.value = profile
            Role.ORGANISATION -> orgProfile.value = profile
        }
    }

    /**
     * DETAILED BEHAVIOUR — replaceChats
     *
     * Implements the current VolunteerLink responsibility for replace chats in this support/model layer.
     */
    fun replaceChats(chats: List<ChatRoom>) {
        allChats.clear()
        allChats.addAll(chats)
        rebuildMessageIndex()
    }

    /**
     * DETAILED BEHAVIOUR — replaceMessages
     *
     * Implements the current VolunteerLink responsibility for replace messages in this support/model layer.
     */
    fun replaceMessages(
        chatId: String,
        messages: List<ChatMessage>
    ) {
        val chat = chatById(chatId) ?: return

        chat.messages.clear()
        chat.messages.addAll(messages)
        chat.readCounts[currentRole.value] = messages.size
        rebuildMessageIndex()
    }

    // Filters the chat with the newest message appears at the top of the list.
    // But Pinned chat always at the most top
    /**
     * DETAILED BEHAVIOUR — chatsForCurrentRole
     *
     * Implements the current VolunteerLink responsibility for chats for current role in this support/model
     * layer.
     */
    fun chatsForCurrentRole(): List<ChatRoom> {
        return allChats
            .filter { currentRole.value in it.visibleTo }
            .sortedWith(
                compareByDescending<ChatRoom> { it.isPinned }
                    .thenByDescending { it.messages.lastOrNull()?.sentAtMillis ?: 0L }
            )
    }

    /**
     * DETAILED BEHAVIOUR — chatById
     *
     * Implements the current VolunteerLink responsibility for chat by id in this support/model layer.
     */
    fun chatById(id: String): ChatRoom? = allChats.find { it.id == id }

    /**
     * DETAILED BEHAVIOUR — sendMessage
     *
     * Implements the current VolunteerLink responsibility for send message in this support/model layer.
     */
    fun sendMessage(chatId: String, text: String, replyToId: String? = null) {
        val chat = chatById(chatId) ?: return
        if (text.isBlank()) return
        val senderId = meSenderId(currentRole.value)
        val sender = chat.members.find { it.id == senderId }
        val newMessage = ChatMessage(
            id = "m${chat.messages.size + 1}_${System.currentTimeMillis()}",
            senderId = senderId,
            senderName = sender?.name ?: "Me",
            senderInitial = sender?.initial ?: "M",
            senderColor = 0xFF2F4A2E,
            text = text,
            sentAtMillis = System.currentTimeMillis(),
            replyToId = replyToId
        )
        chat.messages.add(newMessage)
        chat.readCounts[currentRole.value] = chat.messages.size
        addMessageToIndex(newMessage)
    }
    /**
     * DETAILED BEHAVIOUR — receiveMessage
     *
     * Implements the current VolunteerLink responsibility for receive message in this support/model layer.
     */
    fun receiveMessage(chatId: String, senderId: String, senderName: String, senderInitial: String, senderColor: Long, text: String) {
        val chat = chatById(chatId) ?: return
        val newMessage = ChatMessage(
            id = "m${chat.messages.size + 1}_${System.currentTimeMillis()}",
            senderId = senderId,
            senderName = senderName,
            senderInitial = senderInitial,
            senderColor = senderColor,
            text = text,
            sentAtMillis = System.currentTimeMillis()
        )
        chat.messages.add(newMessage)
        addMessageToIndex(newMessage)
        // Show notification for incoming message
        messageListener?.onNewMessage(chatId, chat.title, senderName, text)
    }

    /** Same idea as sendMessage but for a photo captured from the camera icon. */
    /**
     * DETAILED BEHAVIOUR — sendImageMessage
     *
     * Implements the current VolunteerLink responsibility for send image message in this support/model layer.
     */
    fun sendImageMessage(chatId: String, imageUri: String) {
        val chat = chatById(chatId) ?: return
        val senderId = meSenderId(currentRole.value)
        val sender = chat.members.find { it.id == senderId }
        val newMessage = ChatMessage(
            id = "m${chat.messages.size + 1}_${System.currentTimeMillis()}",
            senderId = senderId,
            senderName = sender?.name ?: "Me",
            senderInitial = sender?.initial ?: "M",
            senderColor = 0xFF2F4A2E,
            text = "📷 Photo",
            sentAtMillis = System.currentTimeMillis(),
            imageUri = imageUri
        )
        chat.messages.add(newMessage)
        chat.readCounts[currentRole.value] = chat.messages.size
        addMessageToIndex(newMessage)
    }

    /**
     * DETAILED BEHAVIOUR — sendVideoMessage
     *
     * Implements the current VolunteerLink responsibility for send video message in this support/model layer.
     */
    fun sendVideoMessage(chatId: String, videoUri: String) {
        addMediaMessage(chatId, "🎥 Video", videoUri = videoUri)
    }

    /**
     * DETAILED BEHAVIOUR — sendVoiceMessage
     *
     * Implements the current VolunteerLink responsibility for send voice message in this support/model layer.
     */
    fun sendVoiceMessage(chatId: String, audioUri: String, durationMillis: Long) {
        addMediaMessage(
            chatId = chatId,
            label = "🎤 Voice message",
            audioUri = audioUri,
            audioDurationMillis = durationMillis
        )
    }

    /**
     * DETAILED BEHAVIOUR — addMediaMessage
     *
     * Implements the current VolunteerLink responsibility for add media message in this support/model layer.
     */
    private fun addMediaMessage(
        chatId: String,
        label: String,
        videoUri: String? = null,
        audioUri: String? = null,
        audioDurationMillis: Long = 0L,
        replyToId: String? = null
    ) {
        val chat = chatById(chatId) ?: return
        val senderId = meSenderId(currentRole.value)
        val sender = chat.members.find { it.id == senderId }

        val newMessage = ChatMessage(
            id = "m${chat.messages.size + 1}_${System.currentTimeMillis()}",
            senderId = senderId,
            senderName = sender?.name ?: "Me",
            senderInitial = sender?.initial ?: "M",
            senderColor = 0xFF2F4A2E,
            text = label,
            sentAtMillis = System.currentTimeMillis(),
            videoUri = videoUri,
            audioUri = audioUri,
            audioDurationMillis = audioDurationMillis,
            replyToId = replyToId
        )
        chat.messages.add(newMessage)
        chat.readCounts[currentRole.value] = chat.messages.size
        addMessageToIndex(newMessage)   // ✅ index it
    }

    /**
     * DETAILED BEHAVIOUR — sendFileMessage
     *
     * Implements the current VolunteerLink responsibility for send file message in this support/model layer.
     */
    fun sendFileMessage(chatId: String, uri: String, mimeType: String, fileName: String?) {
        val chat = chatById(chatId) ?: return
        val senderId = meSenderId(currentRole.value)
        val sender = chat.members.find { it.id == senderId }
        val newMessage = ChatMessage(
            id = "m${chat.messages.size + 1}_${System.currentTimeMillis()}",
            senderId = senderId,
            senderName = sender?.name ?: "Me",
            senderInitial = sender?.initial ?: "M",
            senderColor = 0xFF2F4A2E,
            text = fileName ?: "📄 Document",  // use filename if available, else fallback
            sentAtMillis = System.currentTimeMillis(),
            fileUri = uri,
            fileMimeType = mimeType,
            fileName = fileName
        )
        chat.messages.add(newMessage)
        chat.readCounts[currentRole.value] = chat.messages.size
        addMessageToIndex(newMessage)
    }

    /**
     * DETAILED BEHAVIOUR — getFileNameFromUri
     *
     * Implements the current VolunteerLink responsibility for get file name from uri in this support/model
     * layer.
     */
    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var fileName = "Document"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex) ?: "Document"
                }
            }
        }
        return fileName
    }

    /**
     * DETAILED BEHAVIOUR — markChatRead
     *
     * Implements the current VolunteerLink responsibility for mark chat read in this support/model layer.
     */
    fun markChatRead(chatId: String) {
        val chat = chatById(chatId) ?: return
        chat.readCounts[currentRole.value] = chat.messages.size
    }

    /** Only the sender can delete their own message - enforce that at the call site (see ChatRoomScreen). */
    /**
     * DETAILED BEHAVIOUR — deleteMessage
     *
     * Implements the current VolunteerLink responsibility for delete message in this support/model layer.
     */
    fun deleteMessage(chatId: String, messageId: String) {
        val chat = chatById(chatId) ?: return
        val message = chat.messages.find { it.id == messageId } ?: return
        if (message.senderId == meSenderId(currentRole.value)) {
            chat.messages.removeAll { it.id == messageId }
            allMessagesById.remove(messageId)   // ✅ remove from index
        }
    }

    /** Only the sender can edit their own message - enforce that at the call site (see ChatRoomScreen). */
    /**
     * DETAILED BEHAVIOUR — editMessage
     *
     * Implements the current VolunteerLink responsibility for edit message in this support/model layer.
     */
    fun editMessage(chatId: String, messageId: String, newText: String) {
        if (newText.isBlank()) return
        val chat = chatById(chatId) ?: return
        val index = chat.messages.indexOfFirst { it.id == messageId }
        if (index >= 0 && chat.messages[index].senderId == meSenderId(currentRole.value)) {
            val old = chat.messages[index]
            val edited = old.copy(text = newText, isEdited = true)
            chat.messages[index] = edited
            allMessagesById[messageId] = edited   // ✅ update index
        }
    }

    /**
     * DETAILED BEHAVIOUR — pinMessage
     *
     * Implements the current VolunteerLink responsibility for pin message in this support/model layer.
     */
    fun pinMessage(chatId: String, messageId: String) {
        val chat = chatById(chatId) ?: return
        val index = chat.messages.indexOfFirst { it.id == messageId }
        if (index < 0 || chat.messages[index].isPinned) return

        // Unpin whatever else was pinned first, so there's only ever one obvious pinned banner.
        chat.messages.forEachIndexed { i, m ->
            if (m.isPinned) {
                chat.messages[i] = m.copy(isPinned = false, pinnedBySenderId = null)
            }
        }

        chat.messages[index] = chat.messages[index].copy(
            isPinned = true,
            pinnedBySenderId = meSenderId(currentRole.value)
        )
    }

    /**
     * DETAILED BEHAVIOUR — unpinMessage
     *
     * Implements the current VolunteerLink responsibility for unpin message in this support/model layer.
     */
    fun unpinMessage(chatId: String, messageId: String) {
        val chat = chatById(chatId) ?: return
        val index = chat.messages.indexOfFirst { it.id == messageId }
        val message = chat.messages.getOrNull(index) ?: return

        if (message.pinnedBySenderId == meSenderId(currentRole.value)) {
            chat.messages[index] = message.copy(
                isPinned = false,
                pinnedBySenderId = null
            )
        }
    }

    /**
     * DETAILED BEHAVIOUR — forwardMessage
     *
     * Implements the current VolunteerLink responsibility for forward message in this support/model layer.
     */
    fun forwardMessage(message: ChatMessage, targetChatId: String) {
        val targetChat = chatById(targetChatId) ?: return

        // Find which chat the original message belongs to (optional, for label)
        val sourceChatId = allChats.find { it.messages.any { msg -> msg.id == message.id } }?.id

        val forwarded = message.copy(
            id = "m${targetChat.messages.size + 1}_${System.currentTimeMillis()}",
            senderId = meSenderId(currentRole.value),
            senderName = meSenderName(currentRole.value),   // you may need to add this helper
            senderInitial = meSenderInitial(currentRole.value),
            senderColor = 0xFF2F4A2E,                      // or use a colour for the forwarder
            sentAtMillis = System.currentTimeMillis(),
            forwardedFromChatId = sourceChatId,            // optional, to show "forwarded from X"
            replyToId = null,                              // forwarding discards reply context
            isPinned = false,
            pinnedBySenderId = null,
            isEdited = false
        )
        targetChat.messages.add(forwarded)
        targetChat.readCounts[currentRole.value] = targetChat.messages.size
        addMessageToIndex(forwarded)
    }

    /** Builds a fixed past timestamp (e.g. "yesterday at 9:50 AM") for seed/demo messages. */
    /**
     * DETAILED BEHAVIOUR — pastTimestamp
     *
     * Implements the current VolunteerLink responsibility for past timestamp in this support/model layer.
     */
    private fun pastTimestamp(daysAgo: Int, hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * DETAILED BEHAVIOUR — toggleMuteChat
     *
     * Implements the current VolunteerLink responsibility for toggle mute chat in this support/model layer.
     */
    fun toggleMuteChat(chatId: String) {
        chatById(chatId)?.let { it.isMuted = !it.isMuted }
    }

    /**
     * DETAILED BEHAVIOUR — togglePinChat
     *
     * Implements the current VolunteerLink responsibility for toggle pin chat in this support/model layer.
     */
    fun togglePinChat(chatId: String) {
        chatById(chatId)?.let { it.isPinned = !it.isPinned }
    }

    /**
     * DETAILED BEHAVIOUR — exitGroup
     *
     * Implements the current VolunteerLink responsibility for exit group in this support/model layer.
     */
    fun exitGroup(chatId: String) {
        val chat = chatById(chatId) ?: return
        val currentUserId = meSenderId(currentRole.value)
        val exitTime = System.currentTimeMillis()

        val newMembers = chat.members.filter { it.id != currentUserId }
        val newExited = chat.exitedUserIds + currentUserId
        val newTimestamps = chat.exitedTimestamps + (currentUserId to exitTime)

        val index = allChats.indexOfFirst { it.id == chatId }
        if (index != -1) {
            allChats[index] = chat.copy(
                members = newMembers,
                exitedUserIds = newExited,
                exitedTimestamps = newTimestamps
            )
        }
    }

    /**
     * DETAILED BEHAVIOUR — deleteGroup
     *
     * Implements the current VolunteerLink responsibility for delete group in this support/model layer.
     */
    fun deleteGroup(chatId: String) {
        allChats.removeAll { it.id == chatId }
    }

    /**
     * DETAILED BEHAVIOUR — createIndividualChat
     *
     * Implements the current VolunteerLink responsibility for create individual chat in this support/model
     * layer.
     */
    fun createIndividualChat(
        participantId: String,
        participantName: String,
        participantInitial: String,
        participantRole: Role   // ✅ NEW parameter
    ) {
        val currentUserId = meSenderId(currentRole.value)
        val currentUser = when (currentRole.value) {
            Role.APPLICANT -> currentUser.value
            Role.ORGANISATION -> orgProfile.value
        }

        // Check if a chat already exists between these two users
        val existing = allChats.find { chat ->
            !chat.isGroup && chat.members.any { it.id == currentUserId } &&
                    chat.members.any { it.id == participantId }
        }
        if (existing != null) return // reuse existing

        val newChat = ChatRoom(
            id = "chat_${System.currentTimeMillis()}",
            title = participantName,
            description = "",
            members = mutableStateListOf(
                ChatMember(
                    id = currentUserId,
                    name = currentUser.name,
                    role = currentRole.value,
                    initial = currentUser.name.take(1).uppercase()
                ),
                ChatMember(
                    id = participantId,
                    name = participantName,
                    role = participantRole,   // ✅ use the passed role
                    initial = participantInitial
                )
            ),
            visibleTo = setOf(Role.APPLICANT, Role.ORGANISATION),
            messages = mutableStateListOf(),
            isGroup = false,
            isMuted = false,
            isPinned = false
        )
        allChats.add(newChat)
    }

    /**
     * DETAILED BEHAVIOUR — updateGroupDescription
     *
     * Implements the current VolunteerLink responsibility for update group description in this support/model
     * layer.
     */
    fun updateGroupDescription(chatId: String, newDescription: String) {
        val chat = chatById(chatId) ?: return
        chat.description = newDescription
    }

    // ---------- Applied events (Applicant "Skill Path" / applications) ----------
    val appliedEventIds = mutableStateListOf<String>()

    /**
     * DETAILED BEHAVIOUR — applyToEvent
     *
     * Implements the current VolunteerLink responsibility for apply to event in this support/model layer.
     */
    fun applyToEvent(eventId: String) {
        if (!appliedEventIds.contains(eventId)) appliedEventIds.add(eventId)
    }

    /**
     * DETAILED BEHAVIOUR — isApplied
     *
     * Implements the current VolunteerLink responsibility for is applied in this support/model layer.
     */
    fun isApplied(eventId: String): Boolean = appliedEventIds.contains(eventId)

    // ---------- Organisation created events ----------
    /**
     * DETAILED BEHAVIOUR — createEvent
     *
     * Implements the current VolunteerLink responsibility for create event in this support/model layer.
     */
    fun createEvent(title: String, date: String, spots: Int) {
        events.add(
            VolunteerEvent(
                id = "event_${System.currentTimeMillis()}",
                title = title,
                organisation = orgProfile.value.name,
                distanceKm = 1.0,
                date = date,
                spotsLeft = spots,
                // Drop the new pin near the centre of George Town by default;
                // in a real app you'd geocode an address the organiser types in.
                latitude = 5.4164 + (Math.random() - 0.5) * 0.01,
                longitude = 100.3327 + (Math.random() - 0.5) * 0.01
            )
        )
    }


    // Add a map to quickly retrieve a message by its id across all chats
    private val allMessagesById: MutableMap<String, ChatMessage> = mutableMapOf()

    // Ensure every message is added to the map when created
    /**
     * DETAILED BEHAVIOUR — addMessageToIndex
     *
     * Implements the current VolunteerLink responsibility for add message to index in this support/model layer.
     */
    private fun addMessageToIndex(message: ChatMessage) {
        allMessagesById[message.id] = message
    }

    /**
     * DETAILED BEHAVIOUR — rebuildMessageIndex
     *
     * Implements the current VolunteerLink responsibility for rebuild message index in this support/model
     * layer.
     */
    fun rebuildMessageIndex() {
        allMessagesById.clear()
        allChats.forEach { chat ->
            chat.messages.forEach(::addMessageToIndex)
        }
    }

    // Helper to find a message anywhere
    /**
     * DETAILED BEHAVIOUR — findMessageById
     *
     * Implements the current VolunteerLink responsibility for find message by id in this support/model layer.
     */
    fun findMessageById(messageId: String): ChatMessage? = allMessagesById[messageId]

    // Get all chats (for forwarding dialog)


    init {
        // Index all messages that already exist when the app starts
        allChats.forEach { chat ->
            chat.messages.forEach { message ->
                addMessageToIndex(message)
            }
        }
    }

}
