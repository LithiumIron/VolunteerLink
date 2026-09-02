package com.example.volunteerlink.screens.chat

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView

@Composable
fun VolunteerEmojiPicker(onEmojiSelected: (String) -> Unit) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        factory = { context ->
            EmojiPickerView(context).apply {
                setOnEmojiPickedListener { emojiViewItem ->
                    onEmojiSelected(emojiViewItem.emoji)
                }
            }
        }
    )
}