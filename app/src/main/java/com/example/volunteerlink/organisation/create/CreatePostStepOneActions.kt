package com.example.volunteerlink.organisation.create

import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import com.example.volunteerlink.organisation.create.model.VolunteerPostType

/**
 * Actions Step 1 is allowed to send to its ViewModel.
 * The Composable depends on this small contract instead of owning form logic.
 */
interface CreatePostStepOneActions {
    fun updatePostType(type: VolunteerPostType)
    fun updateCategory(category: VolunteerPostCategory)
    fun updateTitle(title: String)
    fun updateDescription(description: String)
    fun updateThumbnailUri(uri: String?)

    fun updateHelpNeededInput(text: String)
    fun addHelpNeeded()
    fun removeHelpNeeded(item: String)

    fun updateIsMultiDay(isMultiDay: Boolean)
    fun updatePhysicalStartDate(dateMillis: Long)
    fun updatePhysicalEndDate(dateMillis: Long)
    fun updatePhysicalStartTime(hour: Int, minute: Int)
    fun updatePhysicalEndTime(hour: Int, minute: Int): String?
    fun clearPhysicalTimeError()
    fun updateMeetingPoint(text: String)
    fun updatePhysicalVolunteerCapacity(text: String)

    fun onLocationQueryChanged(query: String)
    fun onLocationSelected(location: LocationSuggestion)
    fun clearLocation()

    fun updateRemoteStartDate(dateMillis: Long)
    fun updateRemoteDueDate(dateMillis: Long)
    fun updateRemoteVolunteerCapacity(text: String)
    fun updateRemoteSubmissionMode(mode: RemoteSubmissionMode)
    fun updateSharedDeliverable(text: String)

    fun updateHybridPhysicalVolunteerCapacity(text: String)
    fun updateHybridRemoteVolunteerCapacity(text: String)

    fun continueFromStepOne(): Boolean
}
