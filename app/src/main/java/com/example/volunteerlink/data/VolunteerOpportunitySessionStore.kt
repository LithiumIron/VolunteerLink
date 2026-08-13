package com.example.volunteerlink.data

import androidx.compose.runtime.mutableStateListOf
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole

/**
 * Observable in-memory session cache populated only by Supabase repositories.
 * It keeps existing screen contracts small while removing SampleData as the
 * runtime source of truth.
 */
object VolunteerOpportunitySessionStore {
    val volunteerOpportunityEvents =
        mutableStateListOf<VolunteerOpportunityEvent>()

    val volunteerApplications =
        mutableStateListOf<VolunteerOpportunityApplication>()

    fun replaceWith(data: VolunteerOpportunityDashboardData) {
        volunteerOpportunityEvents.clear()
        volunteerOpportunityEvents.addAll(data.events)

        volunteerApplications.clear()
        volunteerApplications.addAll(data.applications)
    }

    fun findEventById(eventId: Int): VolunteerOpportunityEvent? =
        volunteerOpportunityEvents.firstOrNull {
            it.eventId == eventId
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
                it.applicationRoleId == roleId
        }
}
