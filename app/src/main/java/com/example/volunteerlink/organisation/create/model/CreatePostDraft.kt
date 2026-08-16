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



/** Values match role_templates.role_mode in Supabase. */
enum class VolunteerRoleMode(
    val displayName: String,
    val databaseValue: String
) {
    PHYSICAL("Physical", "PHYSICAL"),
    REMOTE("Remote", "REMOTE")
}

/** Values match role_templates.default_level in Supabase. */
enum class VolunteerRoleLevel(
    val displayName: String,
    val databaseValue: String
) {
    BEGINNER("Beginner", "BEGINNER"),
    INTERMEDIATE("Intermediate", "INTERMEDIATE"),
    ADVANCED("Advanced", "ADVANCED")
}

/** One skill shown inside a role template card. */
data class CreateRoleSkill(
    val skillId: String,
    val name: String
)

/**
 * A fixed role template loaded from Supabase for Step 2.
 *
 * This is catalogue data only. The organiser does not edit these values.
 */
data class CreateRoleTemplate(
    val roleTemplateId: String,
    val roleName: String,
    val roleArea: String,
    val roleMode: VolunteerRoleMode,
    val skillPathId: String,
    val skillPathName: String,
    val description: String,
    val skillsPractised: List<CreateRoleSkill>,
    val recommendedSkills: List<CreateRoleSkill>,
    val defaultLevel: VolunteerRoleLevel
)

/** Values match post_roles.application_method in Supabase. */
enum class RoleApplicationMethod(
    val displayName: String,
    val databaseValue: String
) {
    INSTANT_JOIN(
        displayName = "Instant Join",
        databaseValue = "INSTANT_JOIN"
    ),
    REVIEW_APPLICANTS(
        displayName = "Review Applicants",
        databaseValue = "REVIEW_APPLICANTS"
    )
}

/**
 * The organiser's configuration for one selected fixed role template.
 *
 * Step 2 owns roleTemplateId + capacity. Step 3 extends the SAME object with
 * role-specific settings so there is only one source of truth for each role.
 */
data class SelectedRoleDraft(
    val roleTemplateId: String,
    val capacity: Int = 1,

    // Step 3: role settings.
    val practisedSkillIds: List<String> = emptyList(),
    val requiredSkillExperience: Map<String, Int> = emptyMap(),
    val responsibilities: List<String> = emptyList(),
    val applicationMethod: RoleApplicationMethod? = null,
    val screeningQuestions: List<String> = emptyList(),
    val roleNotes: String = "",
    val individualSubmissionRequirement: String = "",

    // Wizard-only state. This is not a Supabase column.
    val isConfigured: Boolean = false
)

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
 * moves around the Create Post flow. Later Steps 2-4 can extend this same
 * draft instead of passing many separate values through navigation.
 */
data class CreatePostDraft(
    // Shared post information
    val postType: VolunteerPostType? = null,
    val category: VolunteerPostCategory? = null,
    val title: String = "",
    val description: String = "",
    val thumbnailUri: String? = null,

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
    // Time-zone support is postponed for now; future SQL can store NULL.
    val physicalTimeZoneId: String? = null,

    // Remote details
    val remoteStartDateMillis: Long? = null,
    val remoteDueDateMillis: Long? = null,
    val remoteVolunteerCapacity: Int? = null,
    val remoteSubmissionMode: RemoteSubmissionMode? = null,
    val sharedDeliverable: String = "",

    // Hybrid uses separate physical and remote capacity totals.
    val hybridPhysicalVolunteerCapacity: Int? = null,
    val hybridRemoteVolunteerCapacity: Int? = null,

    // Steps 2-3: selected fixed roles and their organisation-specific settings.
    val selectedRoles: List<SelectedRoleDraft> = emptyList(),

    // Step 3 only. The database now keeps this ROLE... ID directly together
    // with post_id, matching the composite post_roles key.
    val sharedSubmissionResponsibleRoleTemplateId: String? = null,

    // Step 4: optional schedule kept locally until the final Publish step.
    val scheduleItems: List<ScheduleItemDraft> = emptyList()
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
                hybridRemoteVolunteerCapacity = null,
                scheduleItems = scheduleItems.filter { item ->
                    item.scheduleType != ScheduleType.REMOTE
                }
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
                hybridRemoteVolunteerCapacity = null,
                scheduleItems = scheduleItems.filter { item ->
                    item.scheduleType != ScheduleType.PHYSICAL
                }
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
                hybridRemoteVolunteerCapacity != null ||
                selectedRoles.isNotEmpty() ||
                sharedSubmissionResponsibleRoleTemplateId != null ||
                scheduleItems.isNotEmpty()
    }
}
