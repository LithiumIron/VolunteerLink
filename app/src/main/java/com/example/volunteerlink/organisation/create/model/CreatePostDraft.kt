package com.example.volunteerlink.organisation.create.model

import com.example.volunteerlink.data.location.LocationSuggestion

/**
 * Holds the organiser's Create Post data while moving through the wizard.
 * Important form data stays in this object and is owned by CreatePostViewModel.
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

    // Hybrid keeps the two capacities separate.
    val hybridPhysicalVolunteerCapacity: Int? = null,
    val hybridRemoteVolunteerCapacity: Int? = null
) {
    /** True when the organiser has entered or selected anything meaningful. */
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
                if (physical != null && remote != null) physical + remote else null
            }
            null -> null
        }

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
