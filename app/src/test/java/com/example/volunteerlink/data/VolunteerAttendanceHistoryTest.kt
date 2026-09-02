package com.example.volunteerlink.data

import com.example.volunteerlink.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class VolunteerAttendanceHistoryTest {
    private val role = VolunteerOpportunityRole(1, roleTitle = "Welcome assistant", roleLevel = "Beginner",
        roleVacancies = 5, rolePrimarySkillPath = "Communication", roleSkillsPractised = emptyList(),
        roleExperienceRequirement = "", roleMode = "PHYSICAL")
    private val event = VolunteerOpportunityEvent(1, "Three day event", "Test organisation",
        eventIsVerifiedOrganisation = true, eventOpportunityType = "Physical",
        eventCategory = VolunteerOpportunityCategory.COMMUNITY, eventLocation = "Penang", eventDistanceKm = null,
        eventDate = "", eventTime = "", eventAvailableSpots = 5, eventApplicationCount = 0, eventDescription = "",
        eventVolunteerRoles = listOf(role), eventPhysicalStartDate = "2026-09-18", eventPhysicalEndDate = "2026-09-20",
        eventPhysicalStartTime = "09:00:00", eventPhysicalEndTime = "18:00:00")
    private fun at(raw: String) = Instant.parse(raw).toEpochMilli()
    private fun build(now: String, records: List<VolunteerAttendanceRecord>? = emptyList()) =
        VolunteerAttendanceHistory.build(event, role, records, emptyList(), at(now))

    @Test fun threeDayTimelineIncludesDaysWithoutRecords() {
        val rows = build("2026-09-19T11:00:00Z", listOf(VolunteerAttendanceRecord("2026-09-18", "PRESENT", "2026-09-18T02:17:00Z")))
        assertEquals(3, rows.size)
        assertEquals(VolunteerAttendanceDayState.PRESENT, rows[0].state)
        assertEquals(VolunteerAttendanceDayState.NO_RECORD, rows[1].state)
        assertEquals(VolunteerAttendanceDayState.NOT_STARTED, rows[2].state)
        assertEquals(listOf(1, 2, 3), rows.map { it.dayNumber })
    }
    @Test fun storedTimeConvertsToLocalTime() {
        assertEquals("18 Sep 2026, 10:17 AM", VolunteerAttendanceHistory.recordedTime("2026-09-18T02:17:00.123456+00:00", "Asia/Kuala_Lumpur"))
        assertEquals("Time not recorded", VolunteerAttendanceHistory.recordedTime(null, "Asia/Kuala_Lumpur"))
    }
    @Test fun proposedWindowStartsAfterEndAndClosesExactlyAt48Hours() {
        assertFalse(build("2026-09-19T10:00:00Z")[1].withinReviewWindow)
        assertTrue(build("2026-09-19T10:00:00.001Z")[1].withinReviewWindow)
        assertTrue(build("2026-09-21T09:59:59Z")[1].withinReviewWindow)
        assertFalse(build("2026-09-21T10:00:00Z")[1].withinReviewWindow)
    }
    @Test fun presentCannotRequestReview() {
        assertFalse(build("2026-09-19T11:00:00Z", listOf(VolunteerAttendanceRecord("2026-09-19", "PRESENT")))[1].withinReviewWindow)
    }
    @Test fun explicitAbsentIsDifferentFromMissingRecord() {
        assertEquals(VolunteerAttendanceDayState.ABSENT,
            build("2026-09-19T11:00:00Z", listOf(VolunteerAttendanceRecord("2026-09-19", "ABSENT")))[1].state)
    }
    @Test fun failedLoadDoesNotBecomeAbsent() {
        assertEquals(VolunteerAttendanceDayState.UNKNOWN, build("2026-09-19T11:00:00Z", null)[1].state)
    }
    @Test fun futureTestRecordsAreFlaggedNotPresentedAsNormalAttendance() {
        assertEquals(VolunteerAttendanceDayState.DATE_REVIEW,
            build("2026-09-02T11:00:00Z", listOf(VolunteerAttendanceRecord("2026-09-18", "PRESENT")))[0].state)
    }
    @Test fun unassignedDayDoesNotBecomeAbsent() {
        val selected = role.copy(roleScheduleItems = listOf(VolunteerRoleScheduleItem(scheduleTime = "9–18",
            scheduleActivity = "Welcome", rawDate = "2026-09-18", scheduleType = "PHYSICAL", assignedToRole = true)))
        val rows = VolunteerAttendanceHistory.build(event, selected, emptyList(), emptyList(), at("2026-09-21T11:00:00Z"))
        assertEquals(VolunteerAttendanceDayState.NOT_SCHEDULED, rows[1].state)
        assertFalse(rows[1].withinReviewWindow)
    }
    @Test fun completedPostDoesNotOfferReviewEligibility() {
        val rows = VolunteerAttendanceHistory.build(event.copy(eventStatus = "COMPLETED"), role, emptyList(), emptyList(), at("2026-09-19T11:00:00Z"))
        assertFalse(rows[1].withinReviewWindow)
    }
    @Test fun overnightScheduleIsNotSilentlyGuessed() {
        assertTrue(runCatching { VolunteerAttendanceHistory.build(event.copy(eventPhysicalStartTime = "22:00:00", eventPhysicalEndTime = "06:00:00"),
            role, emptyList(), emptyList(), at("2026-09-19T11:00:00Z")) }.isFailure)
    }
    @Test fun invalidTimezoneFailsClosed() {
        assertTrue(runCatching { VolunteerAttendanceHistory.build(event.copy(eventTimeZone = "invalid"),
            role, emptyList(), emptyList(), at("2026-09-19T11:00:00Z")) }.isFailure)
    }
    @Test fun threeDaysAreVisibleWithoutExpandingHistory() {
        val rows = build("2026-09-19T11:00:00Z")
        assertEquals(rows, VolunteerAttendanceHistory.preview(rows, "2026-09-19"))
    }
}
