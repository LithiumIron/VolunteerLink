
package com.example.volunteerlink.data

import androidx.compose.runtime.mutableStateListOf
import com.example.volunteerlink.data.location.VolunteerDistanceCalculator
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole

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

    fun findEventById(eventId: Int): VolunteerOpportunityEvent? =
        volunteerOpportunityEvents.firstOrNull {
            it.eventId == eventId
        }

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

    fun findRoleById(
        eventId: Int,
        roleId: Int
    ): VolunteerOpportunityRole? =
        findEventById(eventId)
            ?.eventVolunteerRoles
            ?.firstOrNull { it.roleId == roleId }

    fun findApplicationById(
        applicationId: Int
    ): VolunteerOpportunityApplication? =
        volunteerApplications.firstOrNull {
            it.applicationId == applicationId
        }

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

    fun snapshot(): VolunteerOpportunityDashboardData =
        VolunteerOpportunityDashboardData(
            events = volunteerOpportunityEvents.toList(),
            applications = volunteerApplications.toList()
        )

    fun addOfflinePendingApplication(
        application: VolunteerOpportunityApplication
    ) {
        volunteerApplications.removeAll {
            it.applicationEventId == application.applicationEventId &&
                it.applicationRoleId == application.applicationRoleId
        }
        volunteerApplications.add(0, application)
    }

    fun replaceApplication(
        application: VolunteerOpportunityApplication
    ) {
        val index = volunteerApplications.indexOfFirst {
            it.applicationId == application.applicationId
        }
        if (index >= 0) volunteerApplications[index] = application
    }
}
