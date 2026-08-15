package com.example.volunteerlink.organisation.create.model

/** Time validation used by the keyboard-first time input. */
object CreatePostTimeRules {
    fun endTimeError(
        startTimeMinutes: Int?,
        endTimeMinutes: Int
    ): String? {
        return when {
            startTimeMinutes == null -> "Select the start time first."
            endTimeMinutes <= startTimeMinutes ->
                "End time must be later than the start time."
            else -> null
        }
    }
}
