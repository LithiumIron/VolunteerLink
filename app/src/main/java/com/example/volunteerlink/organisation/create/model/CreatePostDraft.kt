package com.example.volunteerlink.organisation.create.model

import com.example.volunteerlink.data.location.LocationSuggestion

/** Values match volunteer_posts.mode in Supabase. */
enum class VolunteerPostType(
    val displayName: String,
    val shortDescription: String,
    val databaseValue: String
) {
    PHYSICAL(
        displayName = "Physical Event",
        shortDescription = "At a physical location",
        databaseValue = "PHYSICAL"
    ),
    REMOTE(
        displayName = "Remote Project",
        shortDescription = "Completed remotely",
        databaseValue = "REMOTE"
    ),
    HYBRID(
        displayName = "Hybrid",
        shortDescription = "Physical and remote",
        databaseValue = "HYBRID"
    )
}

/** Values match volunteer_posts.category in Supabase. */
enum class VolunteerPostCategory(
    val displayName: String,
    val databaseValue: String
) {
    SPORTS("Sports", "SPORTS"),
    COMMUNITY("Community", "COMMUNITY"),
    EDUCATION("Education", "EDUCATION"),
    ENVIRONMENT("Environment", "ENVIRONMENT"),
    HEALTH("Health", "HEALTH"),
    ANIMALS("Animals", "ANIMALS"),
    ARTS("Arts", "ARTS")
}

/** Values match remote_details.submission_mode in Supabase. */
enum class RemoteSubmissionMode(
    val displayName: String,
    val databaseValue: String
) {
    SHARED_TEAM(
        displayName = "Shared Team Deliverable",
        databaseValue = "SHARED_TEAM"
    ),
    INDIVIDUAL(
        displayName = "Individual Deliverables",
        databaseValue = "INDIVIDUAL"
    )
}

/**
 * One object containing the organiser's Create Post data.
 *
 * The ViewModel owns this object, so the data survives while the organiser
 * moves around the Create Post flow. Later Steps 2-5 can extend this same
 * draft instead of passing many separate values through navigation.
 */
data class CreatePostDraft(
    // Shared post information
    val postType: VolunteerPostType? = null,
    val category: VolunteerPostCategory? = null,
    val title: String = "",
    val description: String = "",
    val thumbnailUri: String? = null,
    val helpNeeded: List<String> = emptyList(),

    // Physical details
    val isMultiDayPhysicalEvent: Boolean = false,
    val physicalStartDateMillis: Long? = null,
    val physicalEndDateMillis: Long? = null,
    val physicalStartTimeMinutes: Int? = null,
    val physicalEndTimeMinutes: Int? = null,
    val physicalLocationQuery: String = "",
    val physicalLocation: LocationSuggestion? = null,
    val meetingPoint: String = "",
    val physicalVolunteerCapacity: Int? = null,

    // Remote details
    val remoteStartDateMillis: Long? = null,
    val remoteDueDateMillis: Long? = null,
    val remoteVolunteerCapacity: Int? = null,
    val remoteSubmissionMode: RemoteSubmissionMode? = null,
    val sharedDeliverable: String = "",

    // Hybrid uses separate physical and remote capacity totals.
    val hybridPhysicalVolunteerCapacity: Int? = null,
    val hybridRemoteVolunteerCapacity: Int? = null
) {
    val requiredPhysicalVolunteerTotal: Int?
        get() = when (postType) {
            VolunteerPostType.PHYSICAL -> physicalVolunteerCapacity
            VolunteerPostType.HYBRID -> hybridPhysicalVolunteerCapacity
            else -> null
        }

    val requiredRemoteVolunteerTotal: Int?
        get() = when (postType) {
            VolunteerPostType.REMOTE -> remoteVolunteerCapacity
            VolunteerPostType.HYBRID -> hybridRemoteVolunteerCapacity
            else -> null
        }

    val requiredVolunteerTotal: Int?
        get() = when (postType) {
            VolunteerPostType.PHYSICAL -> physicalVolunteerCapacity
            VolunteerPostType.REMOTE -> remoteVolunteerCapacity
            VolunteerPostType.HYBRID -> {
                val physical = hybridPhysicalVolunteerCapacity
                val remote = hybridRemoteVolunteerCapacity

                if (physical != null && remote != null) {
                    physical + remote
                } else {
                    null
                }
            }
            null -> null
        }

    /**
     * Returns true when the organiser has entered information that belongs to
     * the currently selected post type. Shared fields such as title/category
     * are intentionally ignored here.
     */
    fun hasModeSpecificInput(type: VolunteerPostType?): Boolean {
        return when (type) {
            VolunteerPostType.PHYSICAL -> {
                isMultiDayPhysicalEvent ||
                        physicalStartDateMillis != null ||
                        physicalEndDateMillis != null ||
                        physicalStartTimeMinutes != null ||
                        physicalEndTimeMinutes != null ||
                        physicalLocationQuery.isNotBlank() ||
                        physicalLocation != null ||
                        meetingPoint.isNotBlank() ||
                        physicalVolunteerCapacity != null
            }

            VolunteerPostType.REMOTE -> {
                remoteStartDateMillis != null ||
                        remoteDueDateMillis != null ||
                        remoteVolunteerCapacity != null ||
                        remoteSubmissionMode != null ||
                        sharedDeliverable.isNotBlank()
            }

            VolunteerPostType.HYBRID -> {
                isMultiDayPhysicalEvent ||
                        physicalStartDateMillis != null ||
                        physicalEndDateMillis != null ||
                        physicalStartTimeMinutes != null ||
                        physicalEndTimeMinutes != null ||
                        physicalLocationQuery.isNotBlank() ||
                        physicalLocation != null ||
                        meetingPoint.isNotBlank() ||
                        remoteStartDateMillis != null ||
                        remoteDueDateMillis != null ||
                        remoteSubmissionMode != null ||
                        sharedDeliverable.isNotBlank() ||
                        hybridPhysicalVolunteerCapacity != null ||
                        hybridRemoteVolunteerCapacity != null
            }

            null -> false
        }
    }

    /**
     * Called only after Step 1 has passed validation.
     * Any temporary data belonging to an unused mode is removed here.
     */
    fun keepOnlySelectedModeData(): CreatePostDraft {
        return when (postType) {
            VolunteerPostType.PHYSICAL -> copy(
                remoteStartDateMillis = null,
                remoteDueDateMillis = null,
                remoteVolunteerCapacity = null,
                remoteSubmissionMode = null,
                sharedDeliverable = "",
                hybridPhysicalVolunteerCapacity = null,
                hybridRemoteVolunteerCapacity = null
            )

            VolunteerPostType.REMOTE -> copy(
                isMultiDayPhysicalEvent = false,
                physicalStartDateMillis = null,
                physicalEndDateMillis = null,
                physicalStartTimeMinutes = null,
                physicalEndTimeMinutes = null,
                physicalLocationQuery = "",
                physicalLocation = null,
                meetingPoint = "",
                physicalVolunteerCapacity = null,
                hybridPhysicalVolunteerCapacity = null,
                hybridRemoteVolunteerCapacity = null
            )

            VolunteerPostType.HYBRID -> copy(
                // Hybrid uses its own Physical/Remote capacity fields.
                physicalVolunteerCapacity = null,
                remoteVolunteerCapacity = null
            )

            null -> this
        }
    }

    /** Used by the Back button to decide whether a discard warning is needed. */
    fun hasMeaningfulContent(): Boolean {
        return postType != null ||
                category != null ||
                title.isNotBlank() ||
                description.isNotBlank() ||
                thumbnailUri != null ||
                helpNeeded.isNotEmpty() ||
                physicalStartDateMillis != null ||
                physicalEndDateMillis != null ||
                physicalStartTimeMinutes != null ||
                physicalEndTimeMinutes != null ||
                physicalLocationQuery.isNotBlank() ||
                physicalLocation != null ||
                meetingPoint.isNotBlank() ||
                physicalVolunteerCapacity != null ||
                remoteStartDateMillis != null ||
                remoteDueDateMillis != null ||
                remoteVolunteerCapacity != null ||
                remoteSubmissionMode != null ||
                sharedDeliverable.isNotBlank() ||
                hybridPhysicalVolunteerCapacity != null ||
                hybridRemoteVolunteerCapacity != null
    }
}
