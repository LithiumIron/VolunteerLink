package com.example.volunteerlink.organisation.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.chat.data.ChatData
import com.example.volunteerlink.chat.data.Role
import com.example.volunteerlink.chat.data.unreadCountFor
import com.example.volunteerlink.organisation.repository.PartnershipConversationPreview
import com.example.volunteerlink.organisation.repository.PartnershipInvitationSummary
import com.example.volunteerlink.organisation.repository.SupabasePartnershipRepository
import com.example.volunteerlink.ui.theme.CardBeige
import com.example.volunteerlink.ui.theme.CreamBackground
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.TextMuted
import com.example.volunteerlink.ui.theme.UnreadBadge
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private enum class OrganisationChatHubTab {
    MESSAGES,
    PARTNERSHIPS
}

@Composable
fun OrganisationChatsHubScreen(
    role: Role,
    onOpenEventChat: (String) -> Unit,
    onOpenPartnershipChat: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(OrganisationChatHubTab.MESSAGES) }
    var showNotifications by remember { mutableStateOf(false) }
    var partnershipChats by remember { mutableStateOf<List<PartnershipConversationPreview>>(emptyList()) }
    var invitations by remember { mutableStateOf<List<PartnershipInvitationSummary>>(emptyList()) }
    var isLoadingPartnership by remember { mutableStateOf(true) }
    var partnershipError by remember { mutableStateOf<String?>(null) }
    var refreshVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshVersion) {
        isLoadingPartnership = true
        partnershipError = null

        runCatching {
            val chats = SupabasePartnershipRepository.loadConversationPreviews()
            val invitationRows = SupabasePartnershipRepository.loadInvitations()
            chats to invitationRows
        }.onSuccess { (chats, invitationRows) ->
            partnershipChats = chats
            invitations = invitationRows
            ChatData.partnershipAttention.value = chats.sumOf { it.unreadCount } +
                invitationRows.count {
                    it.direction == "RECEIVED" &&
                        it.status in setOf("PENDING", "RECONFIRMATION_REQUIRED")
                }
        }.onFailure { error ->
            partnershipError = safePartnershipUiError(error.message.orEmpty())
        }

        isLoadingPartnership = false
    }

    val eventUnread = ChatData.chatsForCurrentRole().sumOf { it.unreadCountFor(role) }
    val partnershipUnread = partnershipChats.sumOf { it.unreadCount }
    val pendingInvitationCount = invitations.count {
        it.direction == "RECEIVED" &&
            it.status in setOf("PENDING", "RECONFIRMATION_REQUIRED")
    }
    val hasAttention = eventUnread + partnershipUnread + pendingInvitationCount > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreen)
                .statusBarsPadding()
                .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chats",
                color = CardBeige,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Box {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Chat notifications",
                    tint = CardBeige,
                    modifier = Modifier.clickable { showNotifications = true }
                )
                if (hasAttention) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(9.dp)
                            .background(Color(0xFFE05B4F), CircleShape)
                    )
                }

                DropdownMenu(
                    expanded = showNotifications,
                    onDismissRequest = { showNotifications = false }
                ) {
                    Text(
                        text = "Chat Notifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    val unreadEventChats = ChatData.chatsForCurrentRole()
                        .filter { it.unreadCountFor(role) > 0 }
                    val unreadPartnershipChats = partnershipChats.filter { it.unreadCount > 0 }

                    if (unreadEventChats.isEmpty() &&
                        unreadPartnershipChats.isEmpty() &&
                        pendingInvitationCount == 0
                    ) {
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
                        unreadEventChats.forEach { eventChat ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(eventChat.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            "${eventChat.unreadCountFor(role)} new event message${if (eventChat.unreadCountFor(role) == 1) "" else "s"}",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                },
                                onClick = {
                                    showNotifications = false
                                    ChatData.markChatRead(eventChat.id)
                                    onOpenEventChat(eventChat.id)
                                }
                            )
                        }

                        unreadPartnershipChats.forEach { partnershipChat ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            partnershipChat.otherOrganisationName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "${partnershipChat.unreadCount} new partnership message${if (partnershipChat.unreadCount == 1) "" else "s"}",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                },
                                onClick = {
                                    showNotifications = false
                                    onOpenPartnershipChat(partnershipChat.conversationId)
                                }
                            )
                        }

                        if (pendingInvitationCount > 0) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "$pendingInvitationCount partnership invitation${if (pendingInvitationCount == 1) "" else "s"} awaiting response",
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    showNotifications = false
                                    selectedTab = OrganisationChatHubTab.PARTNERSHIPS
                                }
                            )
                        }
                    }
                }
            }
        }

        ChatHubTabs(
            selectedTab = selectedTab,
            messageBadgeCount = eventUnread,
            partnershipBadgeCount = partnershipUnread + pendingInvitationCount,
            onSelected = { selectedTab = it }
        )

        when (selectedTab) {
            OrganisationChatHubTab.MESSAGES -> {
                OrganisationChatListScreen(
                    role = role,
                    onOpenChat = onOpenEventChat,
                    modifier = Modifier.weight(1f),
                    showHeader = false
                )
            }

            OrganisationChatHubTab.PARTNERSHIPS -> {
                PartnershipMessagesSection(
                    chats = partnershipChats,
                    isLoading = isLoadingPartnership,
                    errorMessage = partnershipError,
                    onRetry = { refreshVersion += 1 },
                    onOpenChat = onOpenPartnershipChat
                )
            }
        }
    }
}

@Composable
private fun ChatHubTabs(
    selectedTab: OrganisationChatHubTab,
    messageBadgeCount: Int,
    partnershipBadgeCount: Int,
    onSelected: (OrganisationChatHubTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF3F0E7)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            ChatHubTabButton(
                title = "Messages",
                selected = selectedTab == OrganisationChatHubTab.MESSAGES,
                badgeCount = messageBadgeCount,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(OrganisationChatHubTab.MESSAGES) }
            )
            ChatHubTabButton(
                title = "Partnerships",
                selected = selectedTab == OrganisationChatHubTab.PARTNERSHIPS,
                badgeCount = partnershipBadgeCount,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(OrganisationChatHubTab.PARTNERSHIPS) }
            )
        }
    }
}

@Composable
private fun ChatHubTabButton(
    title: String,
    selected: Boolean,
    badgeCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) DeepGreen else Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = if (selected) CardBeige else DeepGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(18.dp)
                        .background(UnreadBadge, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.coerceAtMost(99).toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PartnershipMessagesSection(
    chats: List<PartnershipConversationPreview>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    if (isLoading && chats.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = DeepGreen
            )
            Text("Loading partnership messages...", fontSize = 12.sp, color = TextMuted)
        }
        return
    }

    if (errorMessage != null && chats.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = errorMessage,
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                color = Color(0xFFB3261E)
            )
            TextButton(onClick = onRetry) {
                Text("Retry", color = DeepGreen)
            }
        }
        return
    }

    if (chats.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Partnership Messages",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 12.sp,
            color = TextMuted,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
        ) {
            items(chats, key = { it.conversationId }) { chat ->
                PartnershipConversationRow(
                    chat = chat,
                    onClick = { onOpenChat(chat.conversationId) }
                )
                HorizontalDivider(color = Color(0xFFE6E1D4))
            }
        }
    }
}

@Composable
private fun PartnershipConversationRow(
    chat: PartnershipConversationPreview,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                Icons.Filled.Group,
                contentDescription = chat.otherOrganisationName,
                tint = DeepGreen
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.otherOrganisationName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = chat.latestMessageText?.takeIf { it.isNotBlank() }
                    ?: "Partnership · ${chat.activityTitle}",
                fontSize = 12.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = chat.latestMessageAt?.let(::shortChatTime).orEmpty(),
                fontSize = 10.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(4.dp))
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(UnreadBadge, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unreadCount.coerceAtMost(99).toString(),
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PartnershipInvitationsList(
    invitations: List<PartnershipInvitationSummary>,
    partnershipChats: List<PartnershipConversationPreview>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onOpenPartnershipChat: (String) -> Unit
) {
    when {
        isLoading && invitations.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = DeepGreen)
        }

        errorMessage != null && invitations.isEmpty() -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(errorMessage, color = Color(0xFFB3261E), fontSize = 12.sp)
            TextButton(onClick = onRetry) {
                Text("Try Again", color = DeepGreen)
            }
        }

        invitations.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No partnership invitations yet.",
                color = TextMuted,
                fontSize = 13.sp
            )
        }

        else -> {
            val received = invitations.filter { it.direction == "RECEIVED" }
            val sent = invitations.filter { it.direction == "SENT" }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (received.isNotEmpty()) {
                    item(key = "received_header") {
                        InvitationSectionHeader("Received")
                    }
                    items(received, key = { it.invitationId }) { invitation ->
                        PartnershipInvitationRow(
                            invitation = invitation,
                            onClick = partnershipConversationFor(invitation, partnershipChats)
                                ?.let { conversation ->
                                    { onOpenPartnershipChat(conversation.conversationId) }
                                }
                        )
                        HorizontalDivider(color = Color(0xFFE6E1D4))
                    }
                }

                if (sent.isNotEmpty()) {
                    item(key = "sent_header") {
                        InvitationSectionHeader("Sent")
                    }
                    items(sent, key = { it.invitationId }) { invitation ->
                        PartnershipInvitationRow(
                            invitation = invitation,
                            onClick = partnershipConversationFor(invitation, partnershipChats)
                                ?.let { conversation ->
                                    { onOpenPartnershipChat(conversation.conversationId) }
                                }
                        )
                        HorizontalDivider(color = Color(0xFFE6E1D4))
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        color = DeepGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun PartnershipInvitationRow(
    invitation: PartnershipInvitationSummary,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
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
                Icons.Filled.Group,
                contentDescription = null,
                tint = DeepGreen
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = invitation.otherOrganisationName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = invitation.activityTitle,
                fontSize = 12.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${invitation.supportItemCount} support ${if (invitation.supportItemCount == 1) "item" else "items"} · ${invitation.activityStartDate}",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        InvitationStatusPill(invitation.status)
    }
}

@Composable
private fun InvitationStatusPill(status: String) {
    val normalized = status.uppercase(Locale.ROOT)
    val label = normalized
        .lowercase(Locale.ROOT)
        .replace('_', ' ')
        .replaceFirstChar { it.titlecase(Locale.ROOT) }
    val background = when (normalized) {
        "ACCEPTED" -> Color(0xFFE5F1E1)
        "DECLINED", "CANCELLED", "FULFILLED_ELSEWHERE" -> Color(0xFFF3E8E5)
        "RECONFIRMATION_REQUIRED" -> Color(0xFFFFF2D8)
        else -> Color(0xFFF2EEDB)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = background
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = DeepGreen,
            maxLines = 1
        )
    }
}

private fun partnershipConversationFor(
    invitation: PartnershipInvitationSummary,
    chats: List<PartnershipConversationPreview>
): PartnershipConversationPreview? = chats.firstOrNull { chat ->
    chat.draftId == invitation.draftId &&
        chat.otherOrganisationId == invitation.otherOrganisationId
}

private fun shortChatTime(value: String): String {
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

private fun safePartnershipUiError(raw: String): String {
    val head = raw
        .substringBefore("\nCode:")
        .substringBefore("\nHint:")
        .substringBefore("\nDetails:")
        .substringBefore("\nURL:")
        .substringBefore("\nHeaders:")
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()

    return if (head.isBlank() ||
        head.contains("Bearer ", ignoreCase = true) ||
        head.contains("Authorization", ignoreCase = true) ||
        head.contains("apikey", ignoreCase = true)
    ) {
        "Could not load partnership messages right now."
    } else {
        head.take(180)
    }
}
