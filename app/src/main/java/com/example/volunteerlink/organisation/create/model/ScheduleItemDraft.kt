package com.example.volunteerlink.organisation.create.model

/** The two optional Step 4 schedule sections kept in the reduced project scope. */
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
    )
}

/**
 * One Step 4 item kept in the Create Post draft until final Publish.
 *
 * draftId is local wizard state, not schedule_items.schedule_item_id.
 * targetRoleTemplateIds uses ROLE... IDs while creating the post and those
 * same template IDs are stored through schedule_item_roles for the post.
 */
data class ScheduleItemDraft(
    val draftId: String,
    val scheduleType: ScheduleType,
    val scheduleDateMillis: Long? = null,
    val title: String = "",
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,

    // PHYSICAL may use a simple location override. Blank = main event venue.
    val location: String = "",

    val appliesToAllRoles: Boolean = true,
    val targetRoleTemplateIds: List<String> = emptyList(),
    val notes: String = ""
)
