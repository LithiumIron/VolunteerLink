package com.example.volunteerlink.data

import com.example.volunteerlink.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class VolunteerAttendanceWindowTest {
    private val physical = VolunteerOpportunityRole(
        roleId = 1, roleTitle = "Registration", roleLevel = "Beginner", roleVacancies = 2,
        rolePrimarySkillPath = "Communication", roleSkillsPractised = emptyList(),
        roleExperienceRequirement = "", roleMode = "PHYSICAL")
    private val event = VolunteerOpportunityEvent(
        eventId = 1, eventTitle = "Hybrid test", eventOrganisationName = "Test organisation",
        eventIsVerifiedOrganisation = true, eventOpportunityType = "Hybrid",
        eventCategory = VolunteerOpportunityCategory.COMMUNITY, eventLocation = "Penang",
        eventDistanceKm = null, eventDate = "", eventTime = "", eventAvailableSpots = 2,
        eventApplicationCount = 0, eventDescription = "", eventVolunteerRoles = listOf(physical),
        eventPhysicalStartDate = "2026-09-20", eventPhysicalEndDate = "2026-09-22",
        eventPhysicalStartTime = "09:00:00", eventPhysicalEndTime = "13:00:00",
        eventTimeZone = "Asia/Kuala_Lumpur", eventRemoteStartDate = "2026-09-25",
        eventRemoteEndDate = "2026-10-03", eventRemoteOriginalEndDate = "2026-09-30")

    private fun at(value: String) = Instant.parse(value).toEpochMilli()

    @Test fun malaysiaDateUsesConfiguredZone() {
        assertEquals("2026-09-20", VolunteerAttendanceWindow.localDate(at("2026-09-19T16:00:00Z"), "Asia/Kuala_Lumpur"))
    }
    @Test fun checkInWindowIncludesExactStartAndEnd() {
        assertNotNull(VolunteerAttendanceWindow.reason(event, physical, at("2026-09-20T00:59:59Z")))
        assertNull(VolunteerAttendanceWindow.reason(event, physical, at("2026-09-20T01:00:00Z")))
        assertNull(VolunteerAttendanceWindow.reason(event, physical, at("2026-09-20T05:00:00Z")))
        assertNotNull(VolunteerAttendanceWindow.reason(event, physical, at("2026-09-20T05:00:00.001Z")))
    }
    @Test fun completedAndCancelledPostsCannotCheckIn() {
        for (status in listOf("COMPLETED", "CANCELLED"))
            assertNotNull(VolunteerAttendanceWindow.reason(event.copy(eventStatus = status), physical, at("2026-09-20T02:00:00Z")))
        assertNull(VolunteerAttendanceWindow.reason(event.copy(eventStatus = "CLOSED"), physical, at("2026-09-20T02:00:00Z")))
    }
    @Test fun remoteRolesNeverUseAttendance() {
        assertNotNull(VolunteerAttendanceWindow.reason(event, physical.copy(roleMode = "REMOTE"), at("2026-09-20T02:00:00Z")))
    }
    @Test fun linkedRoleDatesLimitExpectedAttendance() {
        val scheduled = physical.copy(roleScheduleItems = listOf(VolunteerRoleScheduleItem(
            scheduleTime = "10:00–11:00", scheduleActivity = "Registration", rawDate = "2026-09-21",
            startTime = "10:00:00", endTime = "11:00:00", scheduleType = "PHYSICAL", assignedToRole = true)))
        assertNotNull(VolunteerAttendanceWindow.reason(event, scheduled, at("2026-09-20T02:00:00Z")))
        // Documents the current SERVER contract: phase hours, not task hours.
        assertNull(VolunteerAttendanceWindow.reason(event, scheduled, at("2026-09-21T01:00:00Z")))
    }
    @Test fun invalidDatesAndZonesFailClosed() {
        assertNotNull(VolunteerAttendanceWindow.reason(event.copy(eventPhysicalStartDate = "2026-02-30"), physical, at("2026-09-20T02:00:00Z")))
        assertNotNull(VolunteerAttendanceWindow.reason(event.copy(eventTimeZone = "invalid"), physical, at("2026-09-20T02:00:00Z")))
    }
    @Test fun hybridDatesAndExtendedDeadlineRemainSeparate() {
        val text = VolunteerScheduleText.event(event)
        assertTrue(text.contains("20 Sep 2026"))
        assertTrue(text.contains("22 Sep 2026"))
        assertTrue(text.contains("3 Oct 2026"))
        assertTrue(text.contains("Extended from 30 Sep 2026"))
        val remoteText = VolunteerScheduleText.role(event, physical.copy(roleMode = "REMOTE"))
        assertTrue(remoteText.contains("25 Sep 2026"))
        assertTrue(remoteText.contains("Submit before 4 Oct 2026, 8:00 AM"))
        assertFalse(remoteText.contains("20 Sep 2026"))
    }
    @Test fun displayFormattingDoesNotRepeatAnIdenticalDate() {
        assertEquals("20 Sep 2026", VolunteerScheduleText.range("2026-09-20", "2026-09-20"))
        assertEquals("9:00 AM", VolunteerScheduleText.time("09:00:00"))
    }
    @Test fun utcDeadlinesAreConvertedNotRelabelled() {
        assertEquals("3 Oct 2026, 8:00 AM", VolunteerScheduleText.deadline("2026-10-03"))
        assertEquals("4 Oct 2026, 8:00 AM", VolunteerScheduleText.deadline("2026-10-03", true))
        assertEquals("1 Jan 2027, 8:00 AM", VolunteerScheduleText.deadline("2026-12-31", true))
        assertEquals("1 Mar 2028, 8:00 AM", VolunteerScheduleText.deadline("2028-02-29", true))
    }
    @Test fun invalidDeadlineDoesNotInventATime() {
        assertEquals("Not available — sync to check", VolunteerScheduleText.deadline("2026-02-30"))
        assertEquals("Not available — sync to check", VolunteerScheduleText.deadline(""))
        assertEquals("Not available — sync to check", VolunteerScheduleText.deadline("2026-10-03garbage"))
    }
    @Test fun roleCardSummaryIsNotAFullInstructionsParagraph() {
        assertEquals("Physical phase: 20 Sep 2026 – 22 Sep 2026", VolunteerScheduleText.compact(event, physical))
        assertFalse(VolunteerScheduleText.compact(event, physical).contains("UTC"))
        assertFalse(VolunteerScheduleText.compact(event, physical).contains("Apply"))
    }
}
