package com.example.volunteerlink.data

// In-memory session store shared by Volunteer screens after dashboard data has been loaded.

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.volunteerlink.data.location.VolunteerDistanceCalculator
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import com.example.volunteerlink.model.VolunteerSkillPath

// Purpose: Handles the volunteer profile data rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerProfileData(
    val userId: String,
    val fullName: String,
    val email: String,
    val city: String,
    val stateRegion: String,
    val country: String,
    val bio: String = "",
    val phone: String = "",
    val availability: List<String> = emptyList(),
    val memberSince: String = "",
    val profileImageUrl: String? = null,
    val verifiedHours: Int = 0,
    val verifiedMinutes: Int = 0,
    // Changed from List<String> — carrying full application objects gives
    // the profile screen an applicationId to navigate with per item.
    val completedEvents: List<VolunteerOpportunityApplication> = emptyList(),
    val certificates: List<VolunteerOpportunityApplication> = emptyList(),
    val skillPaths: List<VolunteerSkillPath> = emptyList()
)

/**
 * Observable in-memory session cache populated only by Supabase repositories.
 * It keeps existing screen contracts small while removing SampleData as the
 * runtime source of truth.
 */
object VolunteerOpportunitySessionStore {
    var mapFocusEventId: Int? = null
    private var deviceLatitude: Double? = null
    private var deviceLongitude: Double? = null
    val volunteerOpportunityEvents =
        mutableStateListOf<VolunteerOpportunityEvent>()

    val volunteerApplications =
        mutableStateListOf<VolunteerOpportunityApplication>()

    // Cached profile data, shared across VolunteerProfileScreen visits so it
    // doesn't refetch on every navigation. null means "not loaded yet" —
    // VolunteerProfileScreen treats that as its cue to call onRefresh().
    private var _profileData by mutableStateOf<VolunteerProfileData?>(null)

    val profileData: VolunteerProfileData?
        get() = _profileData

    var isProfileLoading by mutableStateOf(false)
        private set
    // Purpose: Applies the update profile loading data operation and returns only after local/shared state can be updated consistently.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun updateProfileLoading(loading: Boolean) {
        isProfileLoading = loading
    }

    // Purpose: Applies the set profile data data operation and returns only after local/shared state can be updated consistently.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun setProfileData(data: VolunteerProfileData) {
        _profileData = data
        isProfileLoading = false
    }

    // Called from EditVolunteerProfileScreen's onSaved so the next visit to
    // VolunteerProfileScreen refetches instead of showing stale cached data.
    // Purpose: Handles the clear profile data rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun clearProfileData() {
        _profileData = null
    }

    // Purpose: Handles the replace with rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun replaceWith(data: VolunteerOpportunityDashboardData) {
        volunteerOpportunityEvents.clear()
        volunteerOpportunityEvents.addAll(data.events)

        volunteerApplications.clear()
        volunteerApplications.addAll(data.applications)

        val latitude = deviceLatitude
        val longitude = deviceLongitude
        if (latitude != null && longitude != null) {
            updateDistancesFromDevice(latitude, longitude)
        }
    }

    /**
     * Recalculates every physical opportunity from the real device location.
     * The coordinate is kept only in memory; it is not uploaded or persisted.
     */
    fun updateDistancesFromDevice(
        latitude: Double,
        longitude: Double
    ) {
        deviceLatitude = latitude
        deviceLongitude = longitude

        val updatedEvents = volunteerOpportunityEvents.map { event ->
            val eventLatitude = event.eventLatitude
            val eventLongitude = event.eventLongitude

            if (eventLatitude == null || eventLongitude == null) {
                event.copy(eventDistanceKm = null)
            } else {
                event.copy(
                    eventDistanceKm =
                        VolunteerDistanceCalculator.kilometres(
                            fromLatitude = latitude,
                            fromLongitude = longitude,
                            toLatitude = eventLatitude,
                            toLongitude = eventLongitude
                        )
                )
            }
        }

        volunteerOpportunityEvents.clear()
        volunteerOpportunityEvents.addAll(updatedEvents)
    }

    // Purpose: Handles the find event by id rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun findEventById(eventId: Int): VolunteerOpportunityEvent? =
        volunteerOpportunityEvents.firstOrNull {
            it.eventId == eventId
        }

    // Purpose: Applies the set event saved data operation and returns only after local/shared state can be updated consistently.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun setEventSaved(eventId: Int, isSaved: Boolean) {
        val index = volunteerOpportunityEvents.indexOfFirst {
            it.eventId == eventId
        }
        if (index >= 0) {
            volunteerOpportunityEvents[index] =
                volunteerOpportunityEvents[index].copy(
                    eventIsSaved = isSaved
                )
        }
    }

    // Purpose: Handles the find role by id rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun findRoleById(
        eventId: Int,
        roleId: Int
    ): VolunteerOpportunityRole? =
        findEventById(eventId)
            ?.eventVolunteerRoles
            ?.firstOrNull { it.roleId == roleId }

    // Purpose: Handles the find application by id rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun findApplicationById(
        applicationId: Int
    ): VolunteerOpportunityApplication? =
        volunteerApplications.firstOrNull {
            it.applicationId == applicationId
        }

    // Purpose: Handles the has application for role rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun hasApplicationForRole(
        eventId: Int,
        roleId: Int
    ): Boolean =
        volunteerApplications.any {
            it.applicationEventId == eventId &&
                    it.applicationRoleId == roleId &&
                    it.applicationStatus !in setOf(
                VolunteerApplicationStatus.CANCELLED,
                VolunteerApplicationStatus.REJECTED
            )
        }

    // Purpose: Handles the active application for event rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun activeApplicationForEvent(
        eventId: Int
    ): VolunteerOpportunityApplication? =
        volunteerApplications
            .filter { application ->
                application.applicationEventId == eventId &&
                    application.applicationStatus in setOf(
                        VolunteerApplicationStatus.PENDING,
                        VolunteerApplicationStatus.ACCEPTED
                    )
            }
            // ACCEPTED is authoritative if legacy/offline data temporarily contains both.
            .sortedBy { application ->
                if (application.applicationStatus == VolunteerApplicationStatus.ACCEPTED) 0 else 1
            }
            .firstOrNull()

    // Purpose: Handles the pending application for event rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun pendingApplicationForEvent(
        eventId: Int
    ): VolunteerOpportunityApplication? =
        volunteerApplications.firstOrNull { application ->
            application.applicationEventId == eventId &&
                application.applicationStatus == VolunteerApplicationStatus.PENDING
        }

    // Purpose: Handles the snapshot rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun snapshot(): VolunteerOpportunityDashboardData =
        VolunteerOpportunityDashboardData(
            events = volunteerOpportunityEvents.toList(),
            applications = volunteerApplications.toList()
        )

    // Purpose: Handles the add offline pending application rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun addOfflinePendingApplication(
        application: VolunteerOpportunityApplication
    ) {
        volunteerApplications.removeAll {
            it.applicationEventId == application.applicationEventId &&
                    it.applicationRoleId == application.applicationRoleId
        }
        volunteerApplications.add(0, application)
    }


}
