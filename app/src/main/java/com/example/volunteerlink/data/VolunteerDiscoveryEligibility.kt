package com.example.volunteerlink.data

// Applies all discovery rules before a role is shown as available to the volunteer.

import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole

/** Discovery is narrower than browsing. Never use this to remove application history. */
object VolunteerDiscoveryEligibility {
    // Treat a pending/accepted/completed application as participation so the same
    // event is not suggested again. A cancelled/rejected application is excluded
    // because the volunteer may still be able to choose another role later.
    fun hasParticipation(
        eventId: Int,
        applications: List<VolunteerOpportunityApplication>
    ): Boolean = applications.any {
        it.applicationEventId == eventId && (
            it.applicationStatus == VolunteerApplicationStatus.PENDING ||
                it.applicationStatus == VolunteerApplicationStatus.ACCEPTED ||
                it.applicationStatus == VolunteerApplicationStatus.COMPLETED ||
                it.applicationStatus == VolunteerApplicationStatus.NOT_COMPLETED ||
                it.applicationDatabaseId.startsWith("offline|")
            )
    }

    /**
     * Returns true only when a role can appear in the personalised recommendation feed.
     * This is intentionally stricter than opening an event details page: volunteers may
     * still browse closed, full, or previously rejected roles to understand the event.
     */
    fun canRecommendRole(
        event: VolunteerOpportunityEvent,
        role: VolunteerOpportunityRole,
        applications: List<VolunteerOpportunityApplication>,
        nowMillis: Long = AppClock.nowMillis()
    ): Boolean =
        !hasParticipation(event.eventId, applications) &&
            VolunteerApplicationWindow.canApply(event, role, nowMillis) &&
            role.roleVacancies > 0 &&
            !VolunteerPhysicalScheduleConflictEvaluator.hasConflict(
                candidateEvent = event,
                candidateRole = role,
                applications = applications,
                events = VolunteerOpportunitySessionStore.volunteerOpportunityEvents
            ) &&
            applications.none {
                it.applicationEventId == event.eventId &&
                    it.applicationRoleId == role.roleId &&
                    it.applicationStatus == VolunteerApplicationStatus.REJECTED
            }
}
