package com.example.volunteerlink.organisation.create.model

import com.example.volunteerlink.data.location.LocationSuggestion

/** The three Step 4 schedule sections. */
enum class ScheduleType(
    val displayName: String,
    val databaseValue: String
) {
    PHYSICAL(
        displayName = "Physical Schedule",
        databaseValue = "PHYSICAL"
    ),
    REMOTE(
        displayName = "Remote Schedule",
        databaseValue = "REMOTE"
    ),
    TRAINING(
        displayName = "Training & Briefing",
        databaseValue = "TRAINING"
    )
}

/** Format used only by Training / Briefing items. */
enum class TrainingMode(
    val displayName: String,
    val databaseValue: String
) {
    ONLINE("Online", "ONLINE"),
    ONSITE("On-site", "ONSITE")
}

/**
 * Wizard-only choice for an on-site training location.
 *
 * The final schedule_items row can still store a resolved location string or
 * NULL. This enum only prevents "same event venue" and "TBA" from becoming
 * ambiguous while the organiser is editing Step 4.
 */
enum class TrainingLocationMode {
    EVENT_LOCATION,
    CUSTOM,
    TBA
}

/**
 * One Step 4 item kept in the Create Post draft until final Publish.
 *
 * draftId is local wizard state, not schedule_items.schedule_item_id.
 * targetRoleTemplateIds uses ROLE... IDs while creating the post. Publish can
 * later map them to generated PROLE... IDs before inserting schedule_items.
 */
data class ScheduleItemDraft(
    val draftId: String,
    val scheduleType: ScheduleType,
    val scheduleDateMillis: Long? = null,
    val title: String = "",
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,

    // PHYSICAL may use a simple location override. Blank = main event venue.
    // TRAINING resolves this from its location mode before it is saved.
    val location: String = "",

    val appliesToAllRoles: Boolean = true,
    val targetRoleTemplateIds: List<String> = emptyList(),
    val notes: String = "",

    // TRAINING-only values.
    val trainingMode: TrainingMode? = null,
    val trainingLocationMode: TrainingLocationMode? = null,
    val trainingLocationQuery: String = "",
    val trainingLocation: LocationSuggestion? = null,
    val onlinePlatform: String = "",
    val meetingLink: String = "",

    // Time-zone support is intentionally postponed. Keep this nullable so a
    // future SQL insert can send NULL until the lecturer confirms it is needed.
    val trainingTimeZoneId: String? = null,

    val allowApplicationsAfterStart: Boolean? = null
)
