package com.example.volunteerlink.data

import com.example.volunteerlink.model.*
import org.junit.Assert.*
import org.junit.Test

class VolunteerPromotionEngineTest {
    private val now = java.time.Instant.parse("2026-09-02T12:00:00Z").toEpochMilli()

    @Test fun currentPurchasePriorityThenFirstComeFirstServed() {
        val rows = listOf(promo(1, 2), promo(2, 1), promo(3, 1).copy(promotionStartMillis = now - 2_000))
        assertEquals(listOf(3, 2, 1, 5, 4), order(listOf(5, 1, 2, 4, 3), rows))
    }

    @Test fun expiryIsExclusiveAndFutureOrStaleSnapshotsAreNotAds() {
        val p = promo(1)
        assertTrue(p.isActive(now))
        assertFalse(p.isActive(p.segmentEndMillis))
        assertFalse(p.copy(segmentStartMillis = now + 1).isActive(now))
        assertFalse(p.copy(observedAtMillis = now + 1).isActive(now))
        assertFalse(p.copy(observedAtMillis = now - VolunteerPromotion.MAX_SNAPSHOT_AGE_MILLIS,
            segmentStartMillis = now - VolunteerPromotion.MAX_SNAPSHOT_AGE_MILLIS - 1,
            promotionStartMillis = now - VolunteerPromotion.MAX_SNAPSHOT_AGE_MILLIS - 2).isActive(now))
    }

    @Test fun extensionNeedsNewEffectiveSegmentNotOldPriority() {
        val previous = promo(1, 1).copy(segmentEndMillis = now)
        assertEquals(listOf(2, 1), order(listOf(1, 2), listOf(previous, promo(2, 2))))
        // The server now returns the lower-priced extension with its own rank.
        val extension = promo(1, 3).copy(segmentStartMillis = now)
        assertEquals(listOf(2, 1), order(listOf(1, 2), listOf(extension, promo(2, 2))))
    }

    @Test fun duplicateResponseFailsClosedAndEventsAreNotDuplicated() {
        assertTrue(VolunteerPromotionEngine.activeByPost(listOf(promo(1), promo(1)), now).isEmpty())
        assertEquals(listOf(1, 2), order(listOf(1, 1, 2), listOf(promo(1))))
    }

    @Test fun finalTieUsesStablePostIdAndOrganicOrderIsPreserved() {
        assertEquals(listOf(1, 2, 4, 3), order(listOf(4, 2, 3, 1), listOf(promo(1), promo(2))))
        assertEquals(listOf(4, 2, 3, 1), order(listOf(4, 2, 3, 1), emptyList()))
    }

    @Test fun paidPlacementNeverRestoresFullClosedOrParticipatedEvents() {
        val source = listOf(event(1).copy(eventStatus = "COMPLETED"),
            event(2).let { it.copy(eventVolunteerRoles = it.eventVolunteerRoles.map { r -> r.copy(roleVacancies = 0) }) },
            event(3), event(4), event(5).copy(eventPhysicalStartDate = "2026-09-01"))
        val application = VolunteerOpportunityApplication(1, 3, "Event", "Organisation", "Role", "2 Sep 2026",
            VolunteerApplicationStatus.ACCEPTED)
        val eligible = VolunteerHomeFeedEngine.filter(source, VolunteerHomeFeedFilter.ALL, listOf(application), now)
        assertEquals(listOf(4), VolunteerPromotionEngine.prioritize(eligible, (1..5).map { promo(it) }, now).map { it.eventId })
    }

    @Test fun selectedFilterStillAppliesAndOrganicMatchesAreUnchanged() {
        val events = listOf(event(1), event(2).copy(eventOpportunityType = "Remote", eventRemoteStartDate = "2026-09-18",
            eventVolunteerRoles = event(2).eventVolunteerRoles.map { it.copy(roleMode = "REMOTE") }))
        val filtered = VolunteerHomeFeedEngine.filter(events, VolunteerHomeFeedFilter.REMOTE, emptyList(), now)
        assertEquals(listOf(2), VolunteerPromotionEngine.prioritize(filtered, listOf(promo(1)), now).map { it.eventId })
        val before = VolunteerHomeRecommendationEngine.recommend(events, emptyList(), nowMillis = now)
        VolunteerPromotionEngine.prioritize(events, listOf(promo(2)), now)
        assertEquals(before, VolunteerHomeRecommendationEngine.recommend(events, emptyList(), nowMillis = now))
    }

    private fun order(ids: List<Int>, promotions: List<VolunteerPromotion>) =
        VolunteerPromotionEngine.prioritize(ids.map(::event), promotions, now).map { it.eventId }

    private fun promo(id: Int, rank: Int = 1) = VolunteerPromotion(
        "POST$id", rank, now - 500, now + 86_400_000, now - 1_000, now)

    private fun event(id: Int) = VolunteerOpportunityEvent(
        eventId = id, eventTitle = "Event $id", eventOrganisationName = "Organisation",
        eventIsVerifiedOrganisation = true, eventOpportunityType = "Physical",
        eventCategory = VolunteerOpportunityCategory.COMMUNITY, eventLocation = "Penang", eventDistanceKm = 2.0,
        eventDate = "18 Sep 2026", eventTime = "9:00 AM – 6:00 PM", eventAvailableSpots = 5,
        eventApplicationCount = 0, eventDescription = "Test", eventDatabaseId = "POST$id",
        eventPhysicalStartDate = "2026-09-18", eventVolunteerRoles = listOf(
            VolunteerOpportunityRole(id, roleTitle = "Helper", roleLevel = "Beginner", roleVacancies = 5,
                rolePrimarySkillPath = "Community", roleSkillsPractised = emptyList(), roleExperienceRequirement = "None",
                roleMode = "PHYSICAL")
        )
    )
}
