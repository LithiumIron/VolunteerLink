package com.example.volunteerlink.screens.chat

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.volunteerlink.chat.data.ChatData
import com.example.volunteerlink.chat.data.ChatMessage
import com.example.volunteerlink.chat.data.Role
import com.example.volunteerlink.chat.data.meSenderId
import com.example.volunteerlink.chat.data.messageTimeLabel
import com.example.volunteerlink.chat.repository.SupabaseChatRepository
import com.example.volunteerlink.ui.theme.BubbleGreen
import com.example.volunteerlink.ui.theme.CardBeige
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.TextMuted
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VolunteerChatRoomScreen(
    chatId: String,
    onBack: () -> Unit,
    onOpenGroupInfo: () -> Unit
) {

    val chat = ChatData.chatById(chatId)
    val currentUserId = meSenderId(ChatData.currentRole.value)

    // If user has exited this chat
    val hasExited = chat?.exitedUserIds?.contains(currentUserId) == true
    // Compute the list of messages the user can see
    val visibleMessages = if (chat == null) emptyList() else {
        if (hasExited) {
            val exitTime = chat.exitedTimestamps[currentUserId] ?: 0L
            chat.messages.filter { it.sentAtMillis <= exitTime }
        } else {
            chat.messages
        }
    }
    val exitTimestamp = if (hasExited) chat?.exitedTimestamps?.get(currentUserId) else null
    val messagesToShow = if (hasExited && exitTimestamp != null) {
        chat?.messages?.filter { it.sentAtMillis <= exitTimestamp } ?: emptyList()
    } else {
        chat?.messages ?: emptyList()
    }

    var draft by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val textFieldFocusRequester = remember { FocusRequester() }

    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    var actionMenuMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var deleteConfirmationMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }
    var fullscreenVideoUri by remember { mutableStateOf<String?>(null) }
    var showCameraOptions by remember { mutableStateOf(false) }
    var recordVideoMode by remember { mutableStateOf(false) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFilePath by remember { mutableStateOf<String?>(null) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var isRecording by remember { mutableStateOf(false) }

    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showForwardChatSelector by remember { mutableStateOf(false) }

    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var highlightJob by remember { mutableStateOf<Job?>(null) }



    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            nowTick = System.currentTimeMillis()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null && chat != null) {
            ChatData.sendImageMessage(chat.id, pendingCameraUri.toString())
            scope.launch {
                listState.animateScrollToItem((chat.messages.size - 1).coerceAtLeast(0))
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createVolunteerCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && pendingVideoUri != null && chat != null) {
            ChatData.sendVideoMessage(chat.id, pendingVideoUri.toString())
            scope.launch {
                listState.animateScrollToItem((chat.messages.size - 1).coerceAtLeast(0))
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && chat != null) {
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            // Get the original filename
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
            ChatData.sendFileMessage(chat.id, uri.toString(), mimeType, fileName)
            scope.launch {
                listState.animateScrollToItem((chat.messages.size - 1).coerceAtLeast(0))
            }
        }
    }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createVolunteerCameraVideoUri(context)
            pendingVideoUri = uri
            videoLauncher.launch(uri)
        }
    }

    fun openCamera() {
        val hasCameraPermission =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            val uri = createVolunteerCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    fun openVideoCamera() {
        val hasCameraPermission =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            val uri = createVolunteerCameraVideoUri(context)
            pendingVideoUri = uri
            videoLauncher.launch(uri)
        } else {
            videoPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    fun startVoiceRecording() {
        if (chat == null || isRecording) return

        val audioFile = createVolunteerVoiceRecordingFile(context)
        val recorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(audioFile.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            recordingFilePath = audioFile.absolutePath
            recordingStartedAt = System.currentTimeMillis()
            isRecording = true
        } catch (_: Exception) {
            recorder.release()
            recordingFilePath = null
        }
    }

    fun stopVoiceRecording() {
        val recorder = mediaRecorder ?: return
        val filePath = recordingFilePath
        val duration = (System.currentTimeMillis() - recordingStartedAt).coerceAtLeast(0L)

        mediaRecorder = null
        recordingFilePath = null
        isRecording = false

        try {
            recorder.stop()
            recorder.release()

            if (filePath != null && chat != null) {
                ChatData.sendVoiceMessage(
                    chat.id,
                    Uri.fromFile(File(filePath)).toString(),
                    duration
                )
                scope.launch {
                    listState.animateScrollToItem((chat.messages.size - 1).coerceAtLeast(0))
                }
            }
        } catch (_: RuntimeException) {
            recorder.release()
            filePath?.let { File(it).delete() }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceRecording()
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.runCatching { stop() }
            mediaRecorder?.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(BubbleGreen.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreen)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = CardBeige,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(Modifier.width(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = chat?.isGroup == true) { onOpenGroupInfo() }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CardBeige, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (chat?.isGroup == false) Icons.Filled.Person else Icons.Filled.Group,
                        contentDescription = null,
                        tint = DeepGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(
                        chat?.title ?: "Chat",
                        color = CardBeige,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        if (chat?.isGroup == false) "Private message"
                        else "${chat?.members?.size ?: 0} members",
                        color = CardBeige.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (chat == null) {
            Text("Chat not found", modifier = Modifier.padding(20.dp))
            return@Column
        }

        val pinnedMessage = visibleMessages.lastOrNull { it.isPinned }

        if (pinnedMessage != null) {
            val pinnedByName =
                chat.members.find { it.id == pinnedMessage.pinnedBySenderId }?.name
                    ?: "A member"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBeige)
                    .clickable {
                        val messageIndex =
                            chat.messages.indexOfFirst { it.id == pinnedMessage.id }

                        if (messageIndex >= 0) {
                            scope.launch { listState.animateScrollToItem(messageIndex) }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Pinned message",
                    tint = DeepGreen,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        "Pinned message",
                        color = DeepGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        messageSummary(pinnedMessage),
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Text(
                    "by $pinnedByName",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
        val onReplyQuoteClick: (String) -> Unit = { replyId ->
            val index = visibleMessages.indexOfFirst { it.id == replyId }
            if (index >= 0) {
                highlightJob?.cancel()
                highlightJob = scope.launch {
                    highlightedMessageId = replyId
                    listState.animateScrollToItem(index)
                    delay(2000L)
                    highlightedMessageId = null
                }
            }
        }

        // Show banner if user has left this chat
        if (hasExited) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3CD))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    "You have left this chat. You can only read previous messages sent",
                    color = Color(0xFF856404),
                    fontSize = 13.sp
                )
            }
        }

        if (visibleMessages.isEmpty() && hasExited) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "You have left this chat. No messages to show.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        } else {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = visibleMessages,
                    key = { _, message -> message.id }
                ) { index, message ->
                    val date = volunteerChatMessageDate(message.sentAtMillis)
                    val previousDate = visibleMessages
                        .getOrNull(index - 1)
                        ?.let { volunteerChatMessageDate(it.sentAtMillis) }

                    if (index == 0 || date != previousDate) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CardBeige.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    text = date,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    MessageBubble(
                        msg = message,
                        viewerRole = ChatData.currentRole.value,
                        now = nowTick,
                        onLongPress = { actionMenuMessage = message },
                        onImageClick = { uri -> fullscreenImageUri = uri },
                        onVideoClick = { uri -> fullscreenVideoUri = uri },
                        onReplyQuoteClick = onReplyQuoteClick,
                        isHighlighted = highlightedMessageId == message.id
                    )
                }
            }
        }


        if (!hasExited) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            // ---- Reply preview chip (moved ABOVE the input row) ----
            if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Replying to ${replyingTo?.senderName}: ${messageSummary(replyingTo!!)}",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = DeepGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { replyingTo = null }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Cancel reply",
                            tint = TextMuted
                        )
                    }
                }
            }
        }

            // ---- Message input Row (Volunteer) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),  // Note: background removed, moved to Column
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.EmojiEmotions,
                    contentDescription = "Emoji",
                    tint = if (showEmojiPicker) DeepGreen else TextMuted,
                    modifier = Modifier.clickable {
                        if (showEmojiPicker) {
                            showEmojiPicker = false
                            textFieldFocusRequester.requestFocus()
                            keyboardController?.show()
                        } else {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            showEmojiPicker = true
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))

                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Write a message...") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(textFieldFocusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                showEmojiPicker = false
                                keyboardController?.show()
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    stopVoiceRecording()
                                } else {
                                    val hasAudioPermission =
                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.RECORD_AUDIO
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                    if (hasAudioPermission) {
                                        startVoiceRecording()
                                    } else {
                                        audioPermissionLauncher.launch(
                                            android.Manifest.permission.RECORD_AUDIO
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription =
                                    if (isRecording) "Stop voice recording"
                                    else "Record voice message",
                                tint = if (isRecording) Color.Red else TextMuted
                            )
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))

                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "Camera",
                    tint = TextMuted,
                    modifier = Modifier.clickable {
                        recordVideoMode = false
                        showCameraOptions = true
                    }
                )

                Spacer(Modifier.width(8.dp))

                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = DeepGreen,
                    modifier = Modifier.clickable {
                        if (draft.isNotBlank()) {
                            // ✅ IMPORTANT: Pass replyToId and clear replyingTo
                            val textToSend = draft
                            val replyId = replyingTo?.id
                            draft = ""
                            replyingTo = null

                            scope.launch {
                                runCatching {
                                    SupabaseChatRepository.sendMessage(
                                        conversationId = chat.id,
                                        text = textToSend,
                                        replyToMessageId = replyId
                                    )
                                    SupabaseChatRepository.loadMessagesForChat(chat.id)
                                }.onSuccess { messages ->
                                    ChatData.replaceMessages(chat.id, messages)
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.lastIndex)
                                    }
                                }.onFailure {
                                    draft = textToSend
                                    replyingTo = replyId?.let(ChatData::findMessageById)
                                    Toast.makeText(context, "Unable to send this message.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }
        }
        if (showCameraOptions) {
            AlertDialog(
                onDismissRequest = { showCameraOptions = false },
                title = { Text("Camera") },
                text = {
                    Column {
                        // Option 1: Take photo
                        Text(
                            "Take photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!recordVideoMode) BubbleGreen else Color.Transparent)
                                .clickable { recordVideoMode = false }
                                .padding(14.dp),
                            fontWeight = if (!recordVideoMode) FontWeight.Bold else FontWeight.Normal
                        )

                        Spacer(Modifier.height(8.dp))

                        // Option 2: Record video
                        Text(
                            "Record video",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (recordVideoMode) BubbleGreen else Color.Transparent)
                                .clickable { recordVideoMode = true }
                                .padding(14.dp),
                            fontWeight = if (recordVideoMode) FontWeight.Bold else FontWeight.Normal
                        )

                        Spacer(Modifier.height(8.dp))

                        // Option 3: Choose from device / Drive (NEW)
                        Text(
                            "Choose from device or Drive",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showCameraOptions = false
                                    filePickerLauncher.launch(arrayOf(
                                        "image/*",
                                        "video/*",
                                        "application/pdf",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "*/*"   // fallback for any file type
                                    ))
                                }
                                .padding(14.dp),
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCameraOptions = false
                            if (recordVideoMode) openVideoCamera() else openCamera()
                        }
                    ) {
                        Text(if (recordVideoMode) "Record video" else "Take photo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCameraOptions = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (isRecording) {
            Text(
                "Recording voice message… tap Stop when finished",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(bottom = 8.dp),
                color = Color.Red,
                fontSize = 12.sp
            )
        }

        if (showEmojiPicker) {
            VolunteerEmojiPicker(onEmojiSelected = { emoji -> draft += emoji })
        }
    }

    actionMenuMessage?.let { message ->
        val isMe = message.isMe(ChatData.currentRole.value)
        val canUnpin =
            message.pinnedBySenderId == meSenderId(ChatData.currentRole.value)

        AlertDialog(
            onDismissRequest = { actionMenuMessage = null },
            title = { Text("Message options") },
            text = {
                Column {
                    // Reply (available for all messages)
                    MessageActionRow(
                        icon = Icons.Filled.Reply,
                        label = "Reply"
                    ) {
                        replyingTo = message
                        actionMenuMessage = null
                        textFieldFocusRequester.requestFocus()
                        keyboardController?.show()
                    }

                    // Copy is available on every message, own or not - matches how every
                    // real chat app treats copying (unlike edit/delete/pin, it's not an
                    // ownership-restricted action).
                    if (message.imageUri == null && message.videoUri == null &&
                        message.audioUri == null && message.fileUri == null) {
                        MessageActionRow(
                            icon = Icons.Filled.ContentCopy,
                            label = "Copy"
                        ) {
                            clipboardManager.setText(AnnotatedString(messageSummary(message)))
                            actionMenuMessage = null
                        }
                    }

                    // Forward (available for all messages)
                    MessageActionRow(
                        icon = Icons.Filled.Forward,
                        label = "Forward"
                    ) {
                        forwardingMessage = message
                        actionMenuMessage = null
                        showForwardChatSelector = true
                    }

                    if (
                        isMe &&
                        message.imageUri == null &&
                        message.videoUri == null &&
                        message.audioUri == null
                    ) {
                        MessageActionRow(
                            icon = Icons.Filled.Edit,
                            label = "Edit"
                        ) {
                            editDraft = message.text
                            editingMessage = message
                            actionMenuMessage = null
                        }
                    }

                    if (!message.isPinned || canUnpin) {
                        MessageActionRow(
                            icon = Icons.Filled.PushPin,
                            label = if (message.isPinned) "Unpin" else "Pin"
                        ) {
                            if (message.isPinned) {
                                ChatData.unpinMessage(chatId, message.id)
                            } else {
                                ChatData.pinMessage(chatId, message.id)
                            }
                            actionMenuMessage = null
                        }
                    } else {
                        Text(
                            "Pinned by another member. Only they can unpin it.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (isMe) {
                        MessageActionRow(
                            icon = Icons.Filled.Delete,
                            label = "Delete"
                        ) {
                            actionMenuMessage = null
                            deleteConfirmationMessage = message
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionMenuMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deleteConfirmationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteConfirmationMessage = null },
            title = { Text("Delete message?") },
            text = {
                Text("This message will be removed from the chat. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmationMessage = null
                        scope.launch {
                            runCatching {
                                if (!message.id.startsWith("m")) {
                                    SupabaseChatRepository.deleteMessage(message.id)
                                }
                            }.onSuccess {
                                ChatData.deleteMessage(chatId, message.id)
                            }.onFailure {
                                Toast.makeText(context, "Unable to delete this message.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmationMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    editingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit message") },
            text = {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { editDraft = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val replacement = editDraft
                        editingMessage = null
                        scope.launch {
                            runCatching {
                                SupabaseChatRepository.editMessage(message.id, replacement)
                            }.onSuccess {
                                ChatData.editMessage(chatId, message.id, replacement)
                            }.onFailure {
                                Toast.makeText(context, "Unable to edit this message.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ---------- Forward chat selector ----------
    if (showForwardChatSelector) {
        AlertDialog(
            onDismissRequest = { showForwardChatSelector = false },
            title = { Text("Forward to...") },
            text = {
                Column {
                    // Only show chats the current user has access to
                    val availableChats = ChatData.chatsForCurrentRole()
                    availableChats.forEach { chat ->
                        if (chat.id != chatId) { // exclude current chat
                            Text(
                                text = chat.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val message = forwardingMessage ?: return@clickable
                                        scope.launch {
                                            runCatching {
                                                SupabaseChatRepository.sendMessage(
                                                    chat.id,
                                                    "Forwarded: ${messageSummary(message)}"
                                                )
                                                SupabaseChatRepository.loadMessagesForChat(chat.id)
                                            }.onSuccess { ChatData.replaceMessages(chat.id, it) }
                                                .onFailure {
                                                    Toast.makeText(context, "Unable to forward this message.", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                        forwardingMessage = null
                                        showForwardChatSelector = false
                                    }
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                    // If no other chats, show a message
                    if (availableChats.none { it.id != chatId }) {
                        Text("No other chats available.", color = TextMuted)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showForwardChatSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    fullscreenImageUri?.let { uri ->
        Dialog(
            onDismissRequest = { fullscreenImageUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                ZoomableFullscreenImage(uri)

                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close enlarged photo",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .clickable { fullscreenImageUri = null }
                )
            }
        }
    }

    fullscreenVideoUri?.let { uri ->
        Dialog(
            onDismissRequest = { fullscreenVideoUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                FullscreenVideoPlayer(uri)

                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close video",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .clickable { fullscreenVideoUri = null }
                )
            }
        }
    }
}

fun volunteerChatMessageDate(timestamp: Long): String =
    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        .format(java.util.Date(timestamp))

@Composable
private fun MessageActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = DeepGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp)
    }
}

private fun messageSummary(message: ChatMessage): String =
    when {
        message.imageUri != null -> "📷 Photo"
        message.videoUri != null -> "🎥 Video"
        message.audioUri != null -> "🎤 Voice message"
        message.fileUri != null -> "📄 ${message.fileName ?: "Document"}"
        else -> message.text
    }

@Composable
private fun ZoomableFullscreenImage(uri: String) {
    var scale by remember(uri) { mutableStateOf(1f) }
    var offsetX by remember(uri) { mutableStateOf(0f) }
    var offsetY by remember(uri) { mutableStateOf(0f) }

    Image(
        painter = rememberAsyncImagePainter(uri),
        contentDescription = "Enlarged photo. Pinch to zoom.",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
            .pointerInput(uri) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val updatedScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = updatedScale

                    if (updatedScale == 1f) {
                        offsetX = 0f
                        offsetY = 0f
                    } else {
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
            }
    )
}

/**
 * Purely decorative preview shown inside the bubble - NOT independently clickable.
 * The whole bubble's combinedClickable (see MessageBubble) handles opening it, exactly
 * like the image case. An interactive VideoView here was the actual bug: it fought the
 * bubble's own tap/long-press gesture detector and usually lost, so nothing happened.
 */
@Composable
private fun VideoMessageContent(videoUri: String) {
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play video",
                tint = DeepGreen,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            "Video",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        )
    }
}

@Composable
private fun FullscreenVideoPlayer(videoUri: String) {
    var videoView by remember(videoUri) { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember(videoUri) { mutableStateOf(false) }
    var currentPosition by remember(videoUri) { mutableStateOf(0) }
    var duration by remember(videoUri) { mutableStateOf(0) }
    var isUserSeeking by remember(videoUri) { mutableStateOf(false) }
    var sliderPosition by remember(videoUri) { mutableStateOf(0f) }

    // Polls playback position - VideoView has no position-changed callback of its own.
    LaunchedEffect(videoView) {
        while (true) {
            videoView?.let { view ->
                if (view.duration > 0) duration = view.duration
                if (!isUserSeeking) currentPosition = view.currentPosition.coerceAtLeast(0)
            }
            delay(200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(horizontal = 8.dp)
    ) {
        // Deliberately NOT forcing an aspect ratio here (that was the bug - it assumed
        // landscape 16:9, but a phone-camera video recorded in portrait is ~9:16).
        // VideoView already scales/letterboxes to the video's REAL aspect ratio on its
        // own, as long as it's given a big bounding box instead of a pre-shaped one -
        // weight(1f) hands it almost all the available vertical space in this dialog.
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { context ->
                VideoView(context).also { view ->
                    videoView = view
                    view.setVideoURI(Uri.parse(videoUri))
                    view.setOnPreparedListener {
                        duration = it.duration
                        it.start()
                        isPlaying = true
                    }
                    view.setOnCompletionListener {
                        isPlaying = false
                    }
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                val view = videoView ?: return@IconButton
                if (view.isPlaying) {
                    view.pause()
                    isPlaying = false
                } else {
                    view.start()
                    isPlaying = true
                }
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause video" else "Play video",
                    tint = Color.White
                )
            }

            val sliderValue = if (isUserSeeking) {
                sliderPosition
            } else if (duration > 0) {
                (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    isUserSeeking = true
                    sliderPosition = newValue
                },
                onValueChangeFinished = {
                    val targetMs = (sliderPosition * duration).toInt()
                    videoView?.seekTo(targetMs)
                    currentPosition = targetMs
                    isUserSeeking = false
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = BubbleGreen,
                    activeTrackColor = BubbleGreen,
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                )
            )
        }

        Text(
            "${formatVideoTime(if (isUserSeeking) (sliderPosition * duration).toInt() else currentPosition)} / ${formatVideoTime(duration)}",
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

private fun formatVideoTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1_000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun VoiceMessageContent(
    audioUri: String,
    durationMillis: Long
) {
    val context = LocalContext.current
    var player by remember(audioUri) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember(audioUri) { mutableStateOf(false) }
    var elapsedMillis by remember(audioUri) { mutableStateOf(0L) }
    var isUserSeeking by remember(audioUri) { mutableStateOf(false) }
    var sliderPosition by remember(audioUri) { mutableStateOf(0f) }

    val effectiveDuration = if (durationMillis > 0) durationMillis else (player?.duration?.toLong() ?: 0L)

    fun ensurePlayer(): MediaPlayer? {
        player?.let { return it }
        val created = MediaPlayer.create(context, Uri.parse(audioUri)) ?: return null
        created.setOnCompletionListener {
            elapsedMillis = 0L
            isPlaying = false
            it.seekTo(0)
        }
        player = created
        return created
    }

    LaunchedEffect(isPlaying, player) {
        while (isPlaying && player != null) {
            if (!isUserSeeking) {
                elapsedMillis = player?.currentPosition?.toLong() ?: 0L
            }
            delay(200)
        }
    }

    DisposableEffect(audioUri) {
        onDispose {
            player?.release()
        }
    }

    Column(modifier = Modifier.widthIn(min = 210.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    val activePlayer = ensurePlayer() ?: return@IconButton
                    if (activePlayer.isPlaying) {
                        activePlayer.pause()
                        isPlaying = false
                    } else {
                        elapsedMillis = activePlayer.currentPosition.toLong()
                        activePlayer.start()
                        isPlaying = true
                    }
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause voice message" else "Play voice message",
                    tint = DeepGreen
                )
            }
            Text("Voice message", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        val sliderValue = if (isUserSeeking) {
            sliderPosition
        } else if (effectiveDuration > 0) {
            (elapsedMillis.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                isUserSeeking = true
                sliderPosition = newValue
            },
            onValueChangeFinished = {
                val targetMs = (sliderPosition * effectiveDuration).toLong()
                ensurePlayer()?.seekTo(targetMs.toInt())
                elapsedMillis = targetMs
                isUserSeeking = false
            },
            modifier = Modifier
                .width(190.dp)
                .height(24.dp),
            colors = SliderDefaults.colors(thumbColor = DeepGreen, activeTrackColor = DeepGreen)
        )

        Text(
            "${formatVoiceDuration(if (isUserSeeking) (sliderPosition * effectiveDuration).toLong() else elapsedMillis)} / ${formatVoiceDuration(effectiveDuration)}",
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

private fun formatVoiceDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1_000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

// Heuristic pattern matching, not a proper parser - good enough to catch the common cases
// in a chat message. True phone-number validation really needs a library like libphonenumber;
// this regex just requires a plausible digit grouping so it doesn't fire on random short numbers.
private val urlPattern = Regex(
    "(https?://|www\\.)[\\w\\-]+(\\.[\\w\\-]+)+([/?#][\\w\\-./?%&=#]*)?",
    RegexOption.IGNORE_CASE
)
private val emailPattern = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
private val phonePattern = Regex("(\\+?\\d{1,3}[-.\\s]?)?(\\(?\\d{2,4}\\)?[-.\\s]?){2,4}\\d{2,4}")

private enum class LinkType { URL, EMAIL, PHONE }
private data class DetectedLink(val range: IntRange, val type: LinkType, val value: String)

private fun detectLinks(text: String): List<DetectedLink> {
    val found = mutableListOf<DetectedLink>()

    urlPattern.findAll(text).forEach { found += DetectedLink(it.range, LinkType.URL, it.value) }

    emailPattern.findAll(text).forEach { match ->
        if (found.none { it.range.first <= match.range.first && it.range.last >= match.range.last }) {
            found += DetectedLink(match.range, LinkType.EMAIL, match.value)
        }
    }

    phonePattern.findAll(text).forEach { match ->
        val digitCount = match.value.count { it.isDigit() }
        // require a realistic minimum digit count so things like dates/distances in a
        // message ("27 Jul", "1.8km") don't accidentally get treated as phone numbers
        if (digitCount >= 7 && found.none { it.range.first <= match.range.first && it.range.last >= match.range.last }) {
            found += DetectedLink(match.range, LinkType.PHONE, match.value)
        }
    }

    return found.sortedBy { it.range.first }
}

@Composable
private fun LinkifiedMessageText(text: String, fontSize: TextUnit) {
    val context = LocalContext.current
    val links = remember(text) { detectLinks(text) }

    if (links.isEmpty()) {
        Text(text, fontSize = fontSize)
        return
    }

    val annotated = remember(text, links) {
        buildAnnotatedString {
            var cursor = 0
            links.forEach { link ->
                if (link.range.first > cursor) {
                    append(text.substring(cursor, link.range.first))
                }
                pushStringAnnotation(tag = link.type.name, annotation = link.value)
                withStyle(SpanStyle(color = Color(0xFF3B6FD6), textDecoration = TextDecoration.Underline)) {
                    append(text.substring(link.range.first, link.range.last + 1))
                }
                pop()
                cursor = link.range.last + 1
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
    }

    ClickableText(
        text = annotated,
        style = TextStyle(fontSize = fontSize, color = Color.Black),
        onClick = { offset ->
            val annotation = annotated.getStringAnnotations(start = offset, end = offset).firstOrNull() ?: return@ClickableText
            val intent = when (LinkType.valueOf(annotation.tag)) {
                LinkType.URL -> {
                    val target = if (annotation.item.startsWith("http", ignoreCase = true)) {
                        annotation.item
                    } else {
                        "http://${annotation.item}"
                    }
                    Intent(Intent.ACTION_VIEW, Uri.parse(target))
                }
                LinkType.EMAIL -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${annotation.item}"))
                LinkType.PHONE -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${annotation.item}"))
            }
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // No app on the device can handle it (e.g. no browser) - silently ignore for
                // this demo; a production app would show a Snackbar/Toast here instead.
            }
        }
    )
}





@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    msg: ChatMessage,
    viewerRole: Role,
    now: Long,
    onLongPress: () -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onReplyQuoteClick: (String) -> Unit,
    isHighlighted: Boolean
) {
    val context = LocalContext.current
    val isMe = msg.isMe(viewerRole)
    val alignment = if (isMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Sender info (only for others)
        if (!isMe) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color(msg.senderColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(msg.senderInitial, fontSize = 11.sp, color = Color.White)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    msg.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = DeepGreen
                )
            }
            Spacer(Modifier.height(2.dp))
        }

        // Bubble wrapper – contains background, click handling, padding, and highlight
        Box(
            modifier = Modifier
                .background(
                    if (isMe) BubbleGreen else Color.White,
                    RoundedCornerShape(16.dp)
                )
                .combinedClickable(
                    onClick = {
                        when {
                            msg.imageUri != null -> onImageClick(msg.imageUri)
                            msg.videoUri != null -> onVideoClick(msg.videoUri)
                            msg.fileUri != null -> {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(msg.fileUri), msg.fileMimeType ?: "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try { context.startActivity(intent) }
                                catch (_: ActivityNotFoundException) { /* ignore */ }
                            }
                        }
                    },
                    onLongClick = onLongPress
                )
                .padding(horizontal = 15.dp, vertical = 11.dp)
                .widthIn(max = 280.dp)
        ) {
            // Highlight overlay (only for the replied-to message)
            if (isHighlighted) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Color.Yellow.copy(alpha = 0.25f),
                            RoundedCornerShape(16.dp)
                        )
                )
            }

            // Actual message content (no extra padding – Box already has it)
            Column {
                if (msg.isPinned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = DeepGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "Pinned",
                            fontSize = 10.sp,
                            color = DeepGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                }

                if (msg.forwardedFromChatId != null) {
                    Text(
                        "↪ Forwarded",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Reply quote (clickable)
                if (msg.replyToId != null) {
                    val repliedMsg = ChatData.findMessageById(msg.replyToId)
                    if (repliedMsg != null) {
                        Text(
                            "↩ Replying to ${repliedMsg.senderName}: ${messageSummary(repliedMsg)}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .clickable { onReplyQuoteClick(msg.replyToId) }
                        )
                    }
                }

                // Message content (image / video / audio / file / text)
                when {
                    msg.imageUri != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(msg.imageUri),
                            contentDescription = "Photo message - tap to enlarge",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                    msg.videoUri != null -> {
                        VideoMessageContent(videoUri = msg.videoUri)
                    }
                    msg.audioUri != null -> {
                        VoiceMessageContent(
                            audioUri = msg.audioUri,
                            durationMillis = msg.audioDurationMillis
                        )
                    }
                    msg.fileUri != null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .widthIn(max = 230.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "Document",
                                tint = DeepGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = msg.fileName ?: "Document",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepGreen,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("Attachment", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                    else -> LinkifiedMessageText(text = msg.text, fontSize = 15.sp)
                }
            }
        }

        // Timestamp row (below the bubble)
        Row {
            Text(
                messageTimeLabel(msg.sentAtMillis, now),
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 3.dp)
            )
            if (msg.isEdited) {
                Spacer(Modifier.width(4.dp))
                Text(
                    "(edited)",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}
