package com.example.volunteerlink.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage

val supabase = createSupabaseClient(
    supabaseUrl = "https://ruioybgexfgcrwfeubik.supabase.co",
    supabaseKey = "sb_publishable_ccTYUCLum9NuD2JYLyDHFQ_MhoSwGN8"
) {
    // Volunteer profile and authenticated RPC calls require Auth.
    install(Auth)

    // Organisation Create and Volunteer reads share the normalized schema.
    install(Postgrest) {
        defaultSchema = "v1_erd_test"
    }

    // Organisation thumbnails and Volunteer opportunity images use Storage.
    install(Storage)
}

// Purpose: Handles the test supabase connection rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
suspend fun testSupabaseConnection(): Boolean {
    return try {
        supabase
            .from("skill_paths")
            .select()

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
