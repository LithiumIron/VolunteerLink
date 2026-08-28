package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceSnapshot
import com.example.volunteerlink.organisation.manage.model.PostManagementPost

/** Loads and updates the normalized data needed to manage one Volunteer Post. */
interface OrganisationPostManagementRepository {
    suspend fun loadPost(postId: String): PostManagementPost

    /**
     * Lightweight read used while Physical attendance is visible.
     * Only attendance_days and attendance_records are fetched.
     */
    suspend fun loadPhysicalAttendance(postId: String): PostManagementAttendanceSnapshot

    /** Shortlists or unshortlists one pending application for comparison. */
    suspend fun setApplicantShortlisted(
        postId: String,
        roleTemplateId: String,
        userId: String,
        isShortlisted: Boolean
    )

    /**
     * Lazily opens today's Physical attendance session.
     * The database function validates ownership, lifecycle, live time window and volunteer count,
     * and returns the existing session rather than generating another PIN.
     */
    suspend fun startPhysicalAttendance(postId: String)

    /** Organisation correction authority: mark an accepted Physical volunteer present. */
    suspend fun markVolunteerPresent(
        postId: String,
        eventDate: String,
        roleTemplateId: String,
        userId: String
    )

    /**
     * Organisation correction authority: persist an explicit ABSENT decision.
     * That decision blocks another volunteer PIN check-in for the same role/date
     * until the organisation explicitly uses Mark Present.
     */
    suspend fun markVolunteerAbsent(
        postId: String,
        eventDate: String,
        roleTemplateId: String,
        userId: String
    )
}
