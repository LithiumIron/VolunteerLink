package com.example.volunteerlink.data

// Retrieves promotion data and maps it into safe UI models.

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

object VolunteerPromotionRepository {
    suspend fun load(expectedAccount: String): List<VolunteerPromotion> {
        check(expectedAccount.isNotBlank() && supabase.auth.currentUserOrNull()?.id == expectedAccount) {
            "Sign in to refresh promoted opportunities."
        }
        val rows = supabase.postgrest.rpc("volunteer_promotion_feed_v1").decodeList<VolunteerPromotion>()
        check(supabase.auth.currentUserOrNull()?.id == expectedAccount) {
            "Account changed. Reopen Home."
        }
        return rows
    }
}
