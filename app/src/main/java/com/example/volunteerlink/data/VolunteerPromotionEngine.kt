package com.example.volunteerlink.data

// Creates promotion display items from events that are currently relevant to the volunteer.

import com.example.volunteerlink.model.VolunteerOpportunityEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Public display metadata only. No payment identifiers, methods or account details. */
@Serializable
data class VolunteerPromotion(
    @SerialName("post_id") val postId: String,
    @SerialName("priority_rank") val priorityRank: Int,
    @SerialName("segment_start_ms") val segmentStartMillis: Long,
    @SerialName("segment_end_ms") val segmentEndMillis: Long,
    @SerialName("promotion_start_ms") val promotionStartMillis: Long,
    @SerialName("observed_at_ms") val observedAtMillis: Long
) {
    fun isActive(nowMillis: Long): Boolean =
        postId.isNotBlank() && priorityRank > 0 &&
            promotionStartMillis <= segmentStartMillis &&
            segmentStartMillis <= observedAtMillis && observedAtMillis < segmentEndMillis &&
            nowMillis >= observedAtMillis && nowMillis < segmentEndMillis &&
            nowMillis - observedAtMillis < MAX_SNAPSHOT_AGE_MILLIS

    companion object {
        // Revalidate every minute online; never keep promoting stale data indefinitely.
        const val MAX_SNAPSHOT_AGE_MILLIS = 15 * 60 * 1000L
    }
}

object VolunteerPromotionEngine {
    /** Input MUST already pass the normal discovery/filter rules. Paid placement cannot bypass them. */
    fun prioritize(
        eligibleEvents: List<VolunteerOpportunityEvent>,
        promotions: List<VolunteerPromotion>,
        nowMillis: Long
    ): List<VolunteerOpportunityEvent> {
        val active = activeByPost(promotions, nowMillis)
        // Stable sorting leaves the non-promoted discovery order unchanged.
        return eligibleEvents.distinctBy { it.eventDatabaseId.ifBlank { it.eventId.toString() } }
            .sortedWith(compareBy<VolunteerOpportunityEvent> {
                active[it.eventDatabaseId]?.priorityRank ?: Int.MAX_VALUE
            }.thenBy {
                active[it.eventDatabaseId]?.promotionStartMillis ?: Long.MAX_VALUE
            }.thenBy {
                if (active.containsKey(it.eventDatabaseId)) it.eventDatabaseId else ""
            })
    }

    fun activeByPost(promotions: List<VolunteerPromotion>, nowMillis: Long): Map<String, VolunteerPromotion> =
        promotions.filter { it.isActive(nowMillis) }.groupBy { it.postId }
            // A malformed/overlapping response must not give a post two ads or an arbitrary price.
            .mapNotNull { (id, rows) -> rows.singleOrNull()?.let { id to it } }.toMap()
}
