package com.example.volunteerlink.organisation.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.chat.data.ChatData
import com.example.volunteerlink.chat.data.ChatMember
import com.example.volunteerlink.chat.data.Role
import com.example.volunteerlink.chat.data.meSenderId
import com.example.volunteerlink.ui.theme.*

@Composable
fun OrganisationGroupInfoScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val chat = ChatData.chatById(chatId)
    val currentUserId = meSenderId(ChatData.currentRole.value)

    // --- Null check first ---
    if (chat == null) {
        // Show error and go back
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // Use these non-null values for the rest of the composable
    val chatTitle = chat.title
    val chatDescription = chat.description
    val members = chat.members
    val memberCount = members.size

    // State for edit description dialog
    var showEditDescriptionDialog by remember { mutableStateOf(false) }
    var descriptionInput by remember { mutableStateOf(chatDescription) }

    Column(modifier = Modifier.fillMaxSize().background(CreamBackground)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreen)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = CardBeige,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(12.dp))
            Text("Group Info", color = CardBeige, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        // Group avatar and title
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(84.dp).background(CardBeige, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Group, contentDescription = null, tint = DeepGreen, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(chatTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("$memberCount members", fontSize = 12.sp, color = TextMuted)
        }

        // Description section with edit button (organiser only)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Description", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                // Only show edit button if the current user is the organiser
                val organiser = members.find { it.role == Role.ORGANISATION }
                if (organiser?.id == currentUserId) {
                    IconButton(
                        onClick = { showEditDescriptionDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit description", tint = DeepGreen)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(chatDescription, fontSize = 13.sp, color = TextDark)
            Spacer(Modifier.height(20.dp))
            Text("Members ($memberCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(Modifier.height(4.dp))

        // Member list
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(members) { member ->
                MemberRow(
                    member = member,
                    isCurrentUser = member.id == currentUserId,
                    onMessageClick = {
                        ChatData.createIndividualChat(
                            participantId = member.id,
                            participantName = member.name,
                            participantInitial = member.initial,
                            participantRole = member.role
                        )
                        val newChat = ChatData.allChats.find {
                            !it.isGroup && it.members.any { it.id == member.id }
                        }
                        newChat?.let { onOpenChat(it.id) }
                    }
                )
            }
        }
    }

    // Edit Description Dialog
    if (showEditDescriptionDialog) {
        AlertDialog(
            onDismissRequest = { showEditDescriptionDialog = false },
            title = { Text("Edit Description") },
            text = {
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ChatData.updateGroupDescription(chatId, descriptionInput)
                        showEditDescriptionDialog = false
                        // Update the local description to reflect changes
                        // (optional, could also be done via state hoisting)
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDescriptionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MemberRow(
    member: ChatMember,
    isCurrentUser: Boolean,
    onMessageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(DeepGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(member.initial, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(
                if (member.role == Role.ORGANISATION) "Organisation" else "Volunteer",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        if (!isCurrentUser) {
            IconButton(
                onClick = onMessageClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Message,
                    contentDescription = "Message",
                    tint = DeepGreen
                )
            }
        }
    }
}