package com.example.volunteerlink.organisation.screens.chat

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements Organisation chat presentation/interaction associated with Organisation Emoji Picker.
//
// The screen/component renders shared chat models and emits repository-facing actions through callbacks or
// coroutine calls rather than editing database tables directly.
//
// Sent messages, membership and read state remain Supabase-authoritative; only unsent text may be kept in account-
// scoped local storage as a convenience.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView

@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationEmojiPicker
 *
 * Renders the reusable Organisation Emoji Picker portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun OrganisationEmojiPicker(onEmojiSelected: (String) -> Unit) {
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