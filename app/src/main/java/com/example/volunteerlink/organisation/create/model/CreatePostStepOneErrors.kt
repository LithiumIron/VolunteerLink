package com.example.volunteerlink.organisation.create.model

/** Field-level validation messages for Create Post Step 1. */
data class CreatePostStepOneErrors(
    val postType: String? = null,
    val category: String? = null,
    val title: String? = null,
    val description: String? = null,
    val helpNeeded: String? = null,
    val physicalStartDate: String? = null,
    val physicalEndDate: String? = null,
    val physicalTime: String? = null,
    val physicalLocation: String? = null,
    val physicalCapacity: String? = null,
    val remoteStartDate: String? = null,
    val remoteDueDate: String? = null,
    val remoteCapacity: String? = null,
    val remoteSubmissionMode: String? = null,
    val sharedDeliverable: String? = null,
    val hybridPhysicalCapacity: String? = null,
    val hybridRemoteCapacity: String? = null
) {
    fun hasErrors(): Boolean {
        return listOf(
            postType,
            category,
            title,
            description,
            helpNeeded,
            physicalStartDate,
            physicalEndDate,
            physicalTime,
            physicalLocation,
            physicalCapacity,
            remoteStartDate,
            remoteDueDate,
            remoteCapacity,
            remoteSubmissionMode,
            sharedDeliverable,
            hybridPhysicalCapacity,
            hybridRemoteCapacity
        ).any { it != null }
    }
}

