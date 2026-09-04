package com.example.volunteerlink.organisation.create.model

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines the Create Post state/model structures associated with Schedule Item Draft.
//
// These models are UI/business-layer data: they let the five-step wizard hold incomplete user input before it is
// converted to a validated repository payload.
//
// Database ids/rows are introduced only when a real saved draft or published post is persisted; local autosave
// serializes the same draft state for recovery without making it authoritative.
//
// Architectural layer: Domain/UI model layer.
// ============================================================================


import kotlinx.serialization.Serializable

/** The two optional Step 4 schedule sections kept in the reduced project scope. */
@Serializable
/**
 * DETAILED DECLARATION — ScheduleType
 *
 * Domain/UI type for Schedule Type used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
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
@Serializable
/**
 * DETAILED DECLARATION — ScheduleItemDraft
 *
 * Represents editable/incomplete user input for Schedule Item Draft before it becomes a server-authoritative
 * record.
 *
 * The draft can contain temporarily incomplete values because validation is applied at step transitions and
 * final persistence boundaries.
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
