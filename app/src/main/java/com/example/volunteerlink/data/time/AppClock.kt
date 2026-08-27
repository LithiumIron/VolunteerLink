package com.example.volunteerlink.data.time

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production clock for VolunteerLink. Date-dependent screens always use the
 * phone's real time; Supabase fixture clocks cannot override user timelines.
 */
object AppClock {
    private val mutableState =
        MutableStateFlow(AppClockState(isLoaded = true, refreshVersion = 1L))

    val state = mutableState.asStateFlow()

    fun initialise(context: Context) {
        context.applicationContext
        mutableState.value = AppClockState(
            isLoaded = true,
            refreshVersion = mutableState.value.refreshVersion + 1L
        )
    }

    fun nowMillis(): Long = System.currentTimeMillis()

    fun isUsingTestTime(): Boolean = false

    suspend fun refreshFromDatabase() {
        mutableState.value = AppClockState(
            isLoaded = true,
            refreshVersion = mutableState.value.refreshVersion + 1L
        )
    }
}

data class AppClockState(
    val useTestTime: Boolean = false,
    val testTimeMillis: Long? = null,
    val isLoaded: Boolean = true,
    val refreshVersion: Long = 0L
)
