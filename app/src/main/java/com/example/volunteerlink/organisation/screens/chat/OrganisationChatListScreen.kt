package com.example.volunteerlink.organisation.screens.chat

import android.widget.Toast

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.chat.data.ChatData
import com.example.volunteerlink.chat.data.ChatRoom
import com.example.volunteerlink.chat.data.Role
import com.example.volunteerlink.chat.data.meSenderId
import com.example.volunteerlink.chat.data.previewText
import com.example.volunteerlink.chat.data.previewTime
import com.example.volunteerlink.chat.data.unreadCountFor
import com.example.volunteerlink.chat.repository.SupabaseChatRepository
import com.example.volunteerlink.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OrganisationChatListScreen(
    role: Role,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val chats = ChatData.chatsForCurrentRole()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showNotifications by remember { mutableStateOf(false) }
    val unreadChats = chats.filter { it.unreadCountFor(role) > 0 }

    // State for long-press actions
    var selectedChatForAction by remember { mutableStateOf<ChatRoom?>(null) }
    var showChatActions by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<ChatRoom?>(null) }

    Column(modifier = modifier.fillMaxSize().background(CreamBackground)) {
        // Top bar with notifications. The Hub screen can hide only this header while
        // preserving the teammate-owned event-chat list/actions below it.
        if (showHeader) Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreen)
                .statusBarsPadding()
                .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Messages", color = CardBeige, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)

            Box {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Chat Notifications",
                    tint = CardBeige,
                    modifier = Modifier.clickable { showNotifications = true }
                )
                if (unreadChats.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(9.dp)
                            .background(Color(0xFFE05B4F), CircleShape)
                    )
                }

                DropdownMenu(expanded = showNotifications, onDismissRequest = { showNotifications = false }) {
                    Text(
                        "Chat Notifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    if (unreadChats.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                Icons.Filled.NotificationsNone,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("You're all caught up", fontSize = 12.sp, color = TextMuted)
                        }
                    } else {
                        unreadChats.forEach { chat ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(chat.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            "${chat.unreadCountFor(role)} new message${if (chat.unreadCountFor(role) == 1) "" else "s"} · ${chat.previewText}",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                onClick = {
                                    showNotifications = false
                                    ChatData.markChatRead(chat.id)
                                    onOpenChat(chat.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Chat list
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(chats) { chat ->
                ChatRow(
                    chat = chat,
                    role = role,
                    onClick = {
                        ChatData.markChatRead(chat.id)
                        onOpenChat(chat.id)
                    },
                    onLongClick = {
                        selectedChatForAction = chat
                        showChatActions = true
                    }
                )
                Divider(color = Color(0xFFE6E1D4))
            }
        }
    }

    // ---------- Action dialog for long-press ----------
    if (showChatActions && selectedChatForAction != null) {
        val chat = selectedChatForAction!!
        AlertDialog(
            onDismissRequest = { showChatActions = false },
            title = { Text(chat.title) },
            text = {
                Column {
                    // Exit Group – only if it's a group and the user hasn't already exited
                    if (chat.isGroup && !chat.exitedUserIds.contains(meSenderId(role))) {
                        Text(
                            "Exit Group",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showChatActions = false
                                    scope.launch {
                                        runCatching {
                                            SupabaseChatRepository.leaveConversation(chat.id)
                                        }.onSuccess {
                                            ChatData.deleteGroup(chat.id)
                                        }.onFailure {
                                            Toast.makeText(
                                                context,
                                                chatActionError(it, "Unable to leave this group."),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                    // Mute / Unmute
                    Text(
                        if (chat.isMuted) "Unmute" else "Mute",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ChatData.toggleMuteChat(chat.id)
                                showChatActions = false
                            }
                            .padding(vertical = 10.dp)
                    )
                    // Pin / Unpin
                    Text(
                        if (chat.isPinned) "Unpin" else "Pin",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ChatData.togglePinChat(chat.id)
                                showChatActions = false
                            }
                            .padding(vertical = 10.dp)
                    )
                    // Delete Chat – always show (with confirmation later)
                    Text(
                        "Delete Chat",
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                chatToDelete = chat
                                showDeleteConfirmation = true
                                showChatActions = false
                            }
                            .padding(vertical = 10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showChatActions = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ---------- Delete confirmation dialog ----------
    if (showDeleteConfirmation && chatToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                chatToDelete = null
            },
            title = { Text("Delete Chat?") },
            text = {
                Text("This removes the conversation only from your account. Event groups can be removed after the event is completed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val conversationId = chatToDelete!!.id
                        showDeleteConfirmation = false
                        chatToDelete = null
                        scope.launch {
                            runCatching {
                                SupabaseChatRepository.deleteConversationForMe(conversationId)
                            }.onSuccess {
                                ChatData.deleteGroup(conversationId)
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    chatActionError(it, "Unable to remove this conversation."),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    chatToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun chatActionError(error: Throwable, fallback: String): String {
    val message = error.message.orEmpty()
    return when {
        message.contains("EVENT_GROUP_CAN_ONLY_BE_REMOVED_AFTER_COMPLETION", true) ->
            "This event group can only be removed after the event is completed."
        message.contains("EVENT_ORGANISER_CANNOT_LEAVE", true) ->
            "The organiser cannot leave an active event group."
        else -> fallback
    }
}

// ---------- Helper composables (defined outside ChatListScreen) ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(
    chat: ChatRoom,
    role: Role,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(CardBeige, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (chat.isGroup) Icons.Filled.Group else Icons.Filled.Person,
                contentDescription = chat.title,
                tint = DeepGreen
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Chat title
                Text(
                    chat.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Pin icon (if pinned) – appears next to title
                if (chat.isPinned) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(14.dp),
                        tint = DeepGreen
                    )
                }

                // Mute icon (if muted) – also next to title
                if (chat.isMuted) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.VolumeOff,
                        contentDescription = "Muted",
                        modifier = Modifier.size(14.dp),
                        tint = TextMuted
                    )
                }
            }

            // Preview text (unchanged)
            Text(
                chat.previewText,
                fontSize = 12.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (chat.isGroup) "Group chat" else "Private message",
                color = DeepGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Right side (timestamp + unread badge) – unchanged
        Column(horizontalAlignment = Alignment.End) {
            Text(chat.previewTime, fontSize = 10.sp, color = TextMuted)
            Spacer(Modifier.height(4.dp))
            val unread = chat.unreadCountFor(role)
            if (unread > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(UnreadBadge, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(unread.toString(), fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }
}
