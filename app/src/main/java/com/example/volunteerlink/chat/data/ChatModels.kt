package com.example.volunteerlink.chat.data

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.example.volunteerlink.chat.data.ChatData.currentUser
import com.example.volunteerlink.chat.data.ChatData.orgProfile

/** The two account types the app supports. Determines which bottom nav / home screen is shown. */
enum class Role { APPLICANT, ORGANISATION }

data class VolunteerEvent(
    val id: String,
    val title: String,
    val organisation: String,
    val distanceKm: Double,
    val date: String,
    val spotsLeft: Int,
    val latitude: Double,
    val longitude: Double,
    val description: String = "Join us and make a difference in your community. Full details and meeting point will be shared in the event chat room."
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderInitial: String,
    val senderColor: Long = 0xFFB8B8B8,
    val text: String,
    val sentAtMillis: Long,
    val imageUri: String? = null,
    val videoUri: String? = null,
    val audioUri: String? = null,
    val audioDurationMillis: Long = 0L,
    val isPinned: Boolean = false,
    val pinnedBySenderId: String? = null,
    val isEdited: Boolean = false,
    val replyToId: String? = null,
    val forwardedFromChatId: String? = null,
    val fileUri: String? = null,
    val fileMimeType: String? = null,
    val fileName: String? = null
) {

    /** Whether this message should render on the right (sent by whoever is currently viewing the chat). */
    fun isMe(viewerRole: Role): Boolean = senderId == meSenderId(viewerRole)
}

private val clockFormatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())

/** Plain "10:42 AM" style clock time for a timestamp. */
fun clockTime(epochMillis: Long): String = clockFormatter.format(java.util.Date(epochMillis))

/**
 * "Now · 10:42 AM" for the first minute after a message is sent, then just "10:42 AM" once
 * more than a minute has passed. Pass a ticking [nowMillis] from the caller (see ChatRoomScreen's
 * periodic tick) so a message rolls over automatically without needing any other recomposition.
 */
fun messageTimeLabel(
    sentAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis()
): String {
    return clockTime(sentAtMillis)
}

/** A person in a chat room - used to render the member list on the Group Info screen. */
data class ChatMember(
    val id: String,
    val name: String,
    val role: Role,
    val initial: String
)

data class ChatRoom(
    val id: String,
    val title: String,
    var  description: String, // make it mutable
    val members: List<ChatMember>,
    val messages: MutableList<ChatMessage>,
    /** Which role(s) this room shows up in on the Messages list - a room with both roles here is
     *  the SAME shared conversation for the applicant and the organisation side (see MockRepository). */
    val visibleTo: Set<Role>,
    /** How many messages each role has "read so far" - messages.size minus this is the unread badge. */
    val readCounts: SnapshotStateMap<Role, Int> = mutableStateMapOf(),
    val isGroup: Boolean = true,
    var isMuted: Boolean = false,
    var isPinned: Boolean = false,
    val exitedUserIds: Set<String> = emptySet(),
    val exitedTimestamps: Map<String, Long> = emptyMap()
    )

fun meSenderId(role: Role): String = when (role) {
    Role.APPLICANT -> currentUser.value.id
    Role.ORGANISATION -> orgProfile.value.id
}

fun meSenderName(role: Role): String = when (role) {
    Role.APPLICANT -> currentUser.value.name
    Role.ORGANISATION -> orgProfile.value.name
}

fun meSenderInitial(role: Role): String = when (role) {
    Role.APPLICANT -> currentUser.value.name
    Role.ORGANISATION -> orgProfile.value.name
}.firstOrNull()?.uppercase() ?: "?"

val ChatRoom.previewText: String
    get() = messages.lastOrNull()?.let {
        when {
            it.imageUri != null -> "📷 Photo"
            it.videoUri != null -> "🎥 Video"
            it.audioUri != null -> "🎤 Voice message"
            else -> it.text
        }
    } ?: "No messages yet"

val ChatRoom.previewTime: String
    get() = messages.lastOrNull()?.let { clockTime(it.sentAtMillis) } ?: ""

fun ChatRoom.unreadCountFor(role: Role): Int =
    (messages.size - (readCounts[role] ?: 0)).coerceAtLeast(0)

data class UserProfile(
    val id: String = "",
    val name: String,
    val email: String,
    val bio: String,
    val skills: List<String>
)
