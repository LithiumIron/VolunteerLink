package com.example.volunteerlink.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.example.volunteerlink.data.time.AppClock
import kotlinx.coroutines.delay

/** Rechecks visible time gates; server validation remains authoritative at the click. */
@Composable
internal fun volunteerBusinessTime(): Long {
    val now by produceState(AppClock.nowMillis()) {
        while (true) { value = AppClock.nowMillis(); delay(5_000) }
    }
    return now
}
