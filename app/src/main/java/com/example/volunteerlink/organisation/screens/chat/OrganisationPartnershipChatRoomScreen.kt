package com.example.volunteerlink.organisation.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.organisation.repository.PartnershipChat
import com.example.volunteerlink.organisation.repository.PartnershipChatMessage
import com.example.volunteerlink.organisation.repository.PartnershipInvitationItem
import com.example.volunteerlink.organisation.repository.PartnershipMessageInvitation
import com.example.volunteerlink.organisation.repository.PartnershipResponseResult
import com.example.volunteerlink.organisation.repository.SupabasePartnershipRepository
import com.example.volunteerlink.ui.theme.BubbleGreen
import com.example.volunteerlink.ui.theme.CardBeige
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun OrganisationPartnershipChatRoomScreen(
    conversationId: String,
    onBack: () -> Unit
) {
    var chat by remember { mutableStateOf<PartnershipChat?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var respondingInvitationId by remember { mutableStateOf<String?>(null) }
    var responseNotice by remember { mutableStateOf<String?>(null) }
    var updatedRequest by remember { mutableStateOf<Pair<String, PartnershipResponseResult>?>(null) }
    var declineTarget by remember { mutableStateOf<Triple<String, Int, Boolean>?>(null) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val respondToInvitation: (String, Int, String) -> Unit = { invitationId, revision, action ->
        if (respondingInvitationId == null) {
            respondingInvitationId = invitationId
            responseNotice = null
            errorMessage = null

            scope.launch {
                runCatching {
                    SupabasePartnershipRepository.respondToInvitation(
                        invitationId = invitationId,
                        action = action,
                        expectedRevision = revision
                    )
                }.onSuccess { result ->
                    refreshVersion += 1
                    when (result.outcome.uppercase(Locale.ROOT)) {
                        "UPDATED" -> {
                            updatedRequest = invitationId to result
                            responseNotice = "VolunteerLink recalculated the support that is still needed."
                        }
                        "ACCEPTED", "DECLINED", "FULFILLED_ELSEWHERE" -> {
                            updatedRequest = null
                            responseNotice = result.message
                        }
                        else -> {
                            responseNotice = result.message
                        }
                    }
                }.onFailure { error ->
                    errorMessage = safePartnershipChatError(error.message.orEmpty())
                }
                respondingInvitationId = null
            }
        }
    }

    LaunchedEffect(conversationId, refreshVersion) {
        isLoading = true
        errorMessage = null

        runCatching {
            val loaded = SupabasePartnershipRepository.loadPartnershipChat(conversationId)
            runCatching {
                SupabasePartnershipRepository.markConversationRead(conversationId)
            }
            loaded
        }.onSuccess { loaded ->
            chat = loaded
        }.onFailure { error ->
            errorMessage = safePartnershipChatError(error.message.orEmpty())
        }

        isLoading = false
    }

    LaunchedEffect(chat?.messages?.size) {
        val size = chat?.messages?.size ?: 0
        if (size > 0) {
            listState.scrollToItem(size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BubbleGreen.copy(alpha = 0.35f))
            .imePadding()
    ) {
        PartnershipChatHeader(
            chat = chat,
            onBack = onBack
        )

        when {
            isLoading && chat == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DeepGreen)
            }

            errorMessage != null && chat == null -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = Color(0xFFB3261E),
                    fontSize = 12.sp
                )
                Spacer(Modifier.size(8.dp))
                Button(
                    onClick = { refreshVersion += 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
                ) {
                    Text("Try Again")
                }
            }

            chat != null -> {
                val currentChat = chat!!

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = currentChat.messages,
                        key = { it.messageId }
                    ) { message ->
                        PartnershipMessageRow(
                            message = message,
                            currentUserId = currentChat.currentUserId,
                            chat = currentChat,
                            respondingInvitationId = respondingInvitationId,
                            onAccept = { invitationId, revision ->
                                val currentStatus = message.invitation?.status.orEmpty()
                                respondToInvitation(
                                    invitationId,
                                    revision,
                                    if (currentStatus.equals("RECONFIRMATION_REQUIRED", true)) {
                                        "RECONFIRM"
                                    } else {
                                        "ACCEPT"
                                    }
                                )
                            },
                            onDecline = { invitationId, revision ->
                                declineTarget = Triple(
                                    invitationId,
                                    revision,
                                    message.invitation?.status.equals(
                                        "RECONFIRMATION_REQUIRED",
                                        true
                                    )
                                )
                            }
                        )
                    }

                    if (currentChat.messages.isEmpty()) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No partnership messages yet.",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                responseNotice?.let { notice ->
                    Text(
                        text = notice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEAF4E6))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        color = DeepGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFEDEA))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        color = Color(0xFFB3261E),
                        fontSize = 11.sp
                    )
                }

                PartnershipMessageInput(
                    draft = draft,
                    isSending = isSending,
                    onDraftChanged = { draft = it },
                    onSend = {
                        if (draft.isNotBlank() && !isSending) {
                            val text = draft.trim()
                            isSending = true
                            errorMessage = null

                            scope.launch {
                                runCatching {
                                    SupabasePartnershipRepository.sendTextMessage(
                                        conversationId = conversationId,
                                        messageText = text
                                    )
                                }.onSuccess {
                                    draft = ""
                                    refreshVersion += 1
                                }.onFailure { error ->
                                    errorMessage = safePartnershipChatError(error.message.orEmpty())
                                }
                                isSending = false
                            }
                        }
                    }
                )
            }
        }
    }

    declineTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                if (respondingInvitationId == null) declineTarget = null
            },
            title = { Text("Decline partnership request?") },
            text = {
                Text("The proposed support will not count toward this activity. The sender can continue finding other partners.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        declineTarget = null
                        respondToInvitation(
                            target.first,
                            target.second,
                            if (target.third) "DECLINE_RECONFIRMATION" else "DECLINE"
                        )
                    },
                    enabled = respondingInvitationId == null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A3F36))
                ) {
                    Text("Decline")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { declineTarget = null },
                    enabled = respondingInvitationId == null
                ) {
                    Text("Keep Request", color = DeepGreen)
                }
            }
        )
    }

    updatedRequest?.let { pending ->
        val result = pending.second
        AlertDialog(
            onDismissRequest = {
                if (respondingInvitationId == null) updatedRequest = null
            },
            title = { Text("The request changed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Other accepted support changed what this activity still needs. Review the updated request before accepting.",
                        fontSize = 13.sp
                    )

                    result.items.take(4).forEach { item ->
                        val amountText = if (item.supportType.equals("VENUE", ignoreCase = true)) {
                            item.requestedAmount?.let { "Capacity $it" } ?: "Venue support"
                        } else {
                            item.requestedAmount?.let { "Request $it" } ?: "Support"
                        }
                        Text(
                            text = "• ${item.resourceName}: $amountText",
                            color = DeepGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (result.items.size > 4) {
                        Text(
                            text = "+ ${result.items.size - 4} more updated item${if (result.items.size - 4 == 1) "" else "s"}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        updatedRequest = null
                        respondToInvitation(pending.first, result.revisionNumber, "ACCEPT")
                    },
                    enabled = respondingInvitationId == null,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
                ) {
                    Text("Accept Updated Request")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        updatedRequest = null
                        declineTarget = Triple(pending.first, result.revisionNumber, false)
                    },
                    enabled = respondingInvitationId == null
                ) {
                    Text("Decline", color = Color(0xFF9A3F36))
                }
            }
        )
    }
}

@Composable
private fun PartnershipChatHeader(
    chat: PartnershipChat?,
    onBack: () -> Unit
) {
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
            modifier = Modifier.clickable(onClick = onBack)
        )

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(CardBeige, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Group,
                contentDescription = null,
                tint = DeepGreen,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat?.otherOrganisationName ?: "Partnership",
                color = CardBeige,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = chat?.activityTitle ?: "Impact Weave partnership",
                color = CardBeige.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PartnershipMessageRow(
    message: PartnershipChatMessage,
    currentUserId: String,
    chat: PartnershipChat,
    respondingInvitationId: String?,
    onAccept: (String, Int) -> Unit,
    onDecline: (String, Int) -> Unit
) {
    when (message.messageType.uppercase(Locale.ROOT)) {
        "PARTNERSHIP_INVITATION" -> PartnershipInvitationMessageCard(
            message = message,
            chat = chat,
            isResponding = respondingInvitationId == message.invitationId,
            onAccept = onAccept,
            onDecline = onDecline
        )

        "PARTNERSHIP_UPDATE", "SYSTEM" -> PartnershipSystemMessage(message)

        else -> PartnershipTextMessageBubble(
            message = message,
            isMe = message.senderUserId == currentUserId
        )
    }
}

@Composable
private fun PartnershipTextMessageBubble(
    message: PartnershipChatMessage,
    isMe: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe && !message.senderName.isNullOrBlank()) {
            Text(
                text = message.senderName,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                color = DeepGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isMe) BubbleGreen else Color.White
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(horizontal = 15.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!message.messageText.isNullOrBlank()) {
                    Text(
                        text = message.messageText,
                        fontSize = 14.sp,
                        color = Color(0xFF202020)
                    )
                }

                if (!message.attachmentName.isNullOrBlank()) {
                    Text(
                        text = "File: ${message.attachmentName}",
                        fontSize = 12.sp,
                        color = DeepGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = partnershipMessageTime(message.createdAt),
                    fontSize = 9.sp,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun PartnershipInvitationMessageCard(
    message: PartnershipChatMessage,
    chat: PartnershipChat,
    isResponding: Boolean,
    onAccept: (String, Int) -> Unit,
    onDecline: (String, Int) -> Unit
) {
    val invitation = message.invitation
    var showAllItems by remember(message.messageId) { mutableStateOf(false) }
    var showFullDescription by remember(message.messageId) { mutableStateOf(false) }

    val itemCount = invitation?.items?.size ?: 0
    val visibleItems = if (showAllItems) {
        invitation?.items.orEmpty()
    } else {
        invitation?.items.orEmpty().take(1)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFDDE5D9))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            (message.invitationRevision ?: 1) > 1 && invitation?.direction == "RECEIVED" ->
                                "Updated Partnership Request"
                            (message.invitationRevision ?: 1) > 1 ->
                                "Partnership Request Updated"
                            invitation?.direction == "RECEIVED" ->
                                "Partnership Request"
                            else ->
                                "Partnership Request Sent"
                        },
                        color = DeepGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = chat.activityTitle,
                        color = Color(0xFF202020),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                invitation?.let {
                    Spacer(Modifier.width(10.dp))
                    PartnershipChatStatusPill(it.status)
                }
            }

            PartnershipActivitySummary(
                chat = chat,
                showFullDescription = showFullDescription,
                onToggleDescription = { showFullDescription = !showFullDescription }
            )

            if (invitation == null) {
                Text(
                    text = "Partnership request details are unavailable.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            } else if (!invitation.isCurrentRevision) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF4F2EA)
                ) {
                    Text(
                        text = "This request has been updated. Open the newest partnership update for the current details.",
                        modifier = Modifier.padding(11.dp),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = TextMuted
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Support requested",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF202020)
                        )
                        Text(
                            text = "What this activity needs and what this partner can currently provide.",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = TextMuted
                        )
                    }

                    if (itemCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BubbleGreen.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = "$itemCount ${if (itemCount == 1) "item" else "items"}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = DeepGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (visibleItems.isEmpty()) {
                    Text(
                        text = "No active support items remain on this request.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        visibleItems.forEachIndexed { index, item ->
                            PartnershipInvitationItemCard(
                                item = item,
                                position = index + 1
                            )
                        }
                    }
                }

                if (itemCount > 1) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAllItems = !showAllItems },
                        shape = RoundedCornerShape(12.dp),
                        color = BubbleGreen.copy(alpha = 0.34f)
                    ) {
                        Text(
                            text = if (showAllItems) {
                                "Show fewer items"
                            } else {
                                "View all $itemCount support items"
                            },
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = DeepGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                val canRespond = invitation.direction == "RECEIVED" &&
                    invitation.isCurrentRevision &&
                    invitation.status.uppercase(Locale.ROOT) in setOf(
                        "PENDING",
                        "RECONFIRMATION_REQUIRED"
                    ) &&
                    !message.invitationId.isNullOrBlank()

                if (canRespond) {
                    HorizontalDivider(color = Color(0xFFE1E6DE))

                    Text(
                            text = if (invitation.status.equals("RECONFIRMATION_REQUIRED", true)) {
                                "The activity schedule changed. Reconfirm that your organisation can still provide this support."
                            } else {
                                "Confirm whether your organisation can provide the support shown above."
                            },
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDecline(
                                    message.invitationId.orEmpty(),
                                    invitation.currentRevision
                                )
                            },
                            enabled = !isResponding,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Decline", color = Color(0xFF9A3F36))
                        }

                        Button(
                            onClick = {
                                onAccept(
                                    message.invitationId.orEmpty(),
                                    invitation.currentRevision
                                )
                            },
                            enabled = !isResponding,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
                        ) {
                            if (isResponding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(7.dp))
                            }
                            Text(
                                if (invitation.status.equals("RECONFIRMATION_REQUIRED", true)) {
                                    "Reconfirm"
                                } else if ((message.invitationRevision ?: 1) > 1) {
                                    "Accept Updated"
                                } else {
                                    "Accept"
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = partnershipMessageTime(message.createdAt),
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun PartnershipActivitySummary(
    chat: PartnershipChat,
    showFullDescription: Boolean,
    onToggleDescription: () -> Unit
) {
    val categoryMode = buildString {
        chat.activityCategory?.takeIf { it.isNotBlank() }?.let {
            append(readablePartnershipLabel(it))
            append(" · ")
        }
        append(readablePartnershipLabel(chat.activityMode))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = BubbleGreen.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = categoryMode,
                color = DeepGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            if (chat.activityDescription.isNotBlank()) {
                Text(
                    text = chat.activityDescription,
                    color = Color(0xFF303430),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = if (showFullDescription) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (chat.activityDescription.length > 90) {
                    Text(
                        text = if (showFullDescription) "Show less" else "Read more",
                        modifier = Modifier.clickable(onClick = onToggleDescription),
                        color = DeepGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = DeepGreen.copy(alpha = 0.14f))

            PartnershipActivityMetaRow(
                label = "When",
                value = partnershipActivitySchedule(chat)
            )
            PartnershipActivityMetaRow(
                label = "Where",
                value = chat.activityLocation
            )
        }
    }
}

@Composable
private fun PartnershipActivityMetaRow(
    label: String,
    value: String
) {
    if (value.isBlank()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(44.dp),
            color = DeepGreen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = Color(0xFF4B514B),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PartnershipInvitationItemCard(
    item: PartnershipInvitationItem,
    position: Int
) {
    val isVenue = item.supportType == "VENUE"

    val requestedText = if (isVenue) {
        item.capacityProvided?.let { "Capacity $it" } ?: "Venue support"
    } else {
        "${item.quantityProvided ?: 0}"
    }

    val needText = if (isVenue) {
        item.capacityRequired?.let { "Need capacity $it" } ?: "Venue required"
    } else {
        "Need ${item.quantityRequired ?: 0}"
    }

    val availabilityText = if (isVenue) {
        item.providerCapacity?.let { "Partner capacity $it" } ?: "Capacity not listed"
    } else {
        "Partner has ${item.providerQuantity ?: 0}"
    }

    val matchedResource = item.providerResourceName
        ?.takeIf { it.isNotBlank() }
        ?: item.resourceName

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F9F7),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E6DE))
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = BubbleGreen.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = position.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = DeepGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = item.resourceName,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF202020),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DeepGreen
                ) {
                    Text(
                        text = if (isVenue) requestedText else "Request $requestedText",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "$needText  ·  $availabilityText",
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            Text(
                text = "Matched with: $matchedResource",
                color = DeepGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            val description = item.providerSupportDescription?.takeIf { it.isNotBlank() }
            if (description != null && !description.equals(matchedResource, ignoreCase = true)) {
                Text(
                    text = description,
                    color = Color(0xFF4B514B),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun readablePartnershipLabel(value: String): String = value
    .lowercase(Locale.ROOT)
    .replace('_', ' ')
    .replaceFirstChar { it.titlecase(Locale.getDefault()) }

private fun partnershipActivitySchedule(chat: PartnershipChat): String {
    val startDate = chat.startDate
    val endDate = chat.endDate
    val dateLabel = if (startDate == endDate) startDate else "$startDate to $endDate"
    return "$dateLabel · ${chat.startTime.take(5)} - ${chat.endTime.take(5)}"
}

@Composable
private fun PartnershipSystemMessage(message: PartnershipChatMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CardBeige
        ) {
            Text(
                text = message.messageText?.takeIf { it.isNotBlank() }
                    ?: if (message.messageType == "PARTNERSHIP_UPDATE") {
                        "Partnership request updated"
                    } else {
                        "Partnership update"
                    },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                fontSize = 11.sp,
                color = DeepGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PartnershipChatStatusPill(status: String) {
    val text = status
        .lowercase(Locale.ROOT)
        .replace('_', ' ')
        .replaceFirstChar { it.titlecase(Locale.ROOT) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when (status.uppercase(Locale.ROOT)) {
            "ACCEPTED" -> Color(0xFFE5F1E1)
            "RECONFIRMATION_REQUIRED" -> Color(0xFFFFF2D8)
            "DECLINED", "CANCELLED", "FULFILLED_ELSEWHERE" -> Color(0xFFF4E8E5)
            else -> Color(0xFFF2EEDB)
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = DeepGreen,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PartnershipMessageInput(
    draft: String,
    isSending: Boolean,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            enabled = !isSending,
            placeholder = { Text("Write a message...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            singleLine = true
        )

        Spacer(Modifier.width(10.dp))

        if (isSending) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = DeepGreen
            )
        } else {
            Icon(
                Icons.Filled.Send,
                contentDescription = "Send",
                tint = if (draft.isBlank()) TextMuted else DeepGreen,
                modifier = Modifier.clickable(enabled = draft.isNotBlank(), onClick = onSend)
            )
        }
    }
}

private fun partnershipMessageTime(value: String): String {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd HH:mm:ssXXX",
        "yyyy-MM-dd HH:mm:ss.SSSXXX"
    )

    val parsed = patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(value)
        }.getOrNull()
    } ?: return ""

    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed)
}

private fun safePartnershipChatError(raw: String): String {
    val safeHead = raw
        .substringBefore("\nCode:")
        .substringBefore("\nHint:")
        .substringBefore("\nDetails:")
        .substringBefore("\nURL:")
        .substringBefore("\nHeaders:")
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()

    return if (safeHead.isBlank() ||
        safeHead.contains("Bearer ", ignoreCase = true) ||
        safeHead.contains("Authorization", ignoreCase = true) ||
        safeHead.contains("apikey", ignoreCase = true)
    ) {
        "Could not load partnership chat right now."
    } else {
        safeHead.take(180)
    }
}
