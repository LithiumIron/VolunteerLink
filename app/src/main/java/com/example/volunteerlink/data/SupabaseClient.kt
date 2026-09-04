package com.example.volunteerlink.data

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Creates the single Supabase client shared by both Volunteer and Organisation features.
//
// Auth is installed so authenticated sessions and auth.uid()-based RPC/RLS rules work; Postgrest is installed with
// v1_erd_test as the default schema; Storage is installed for thumbnails, submissions and profile media.
//
// Repositories import this client instead of constructing their own connection, which keeps URL/key/schema/plugin
// configuration consistent across the app.
//
// The publishable client key identifies the Supabase project but does not grant unrestricted database access:
// database RLS, function grants, authenticated sessions and server-side ownership checks still control protected
// data.
//
// testSupabaseConnection performs a lightweight PostgREST read so the app can distinguish an available backend
// from a network/configuration failure.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


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
/**
 * DETAILED BEHAVIOUR — testSupabaseConnection
 *
 * Implements the current VolunteerLink responsibility for test supabase connection in this support/model layer.
 *
 * Reads/maps Supabase table data from `skill_paths` (fixed Skill Path catalogue data used for volunteer
 * progression and role requirements).
 *
 * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without leaving
 * the UI in an assumed-success state.
 */
suspend fun testSupabaseConnection(): Boolean {
    return try {
        supabase
            // SUPABASE TABLE: skill_paths — fixed Skill Path catalogue data used for volunteer progression and role requirements.
            // This is a data-layer read/write; Compose receives the mapped result rather than the raw PostgREST row.
            .from("skill_paths")
            .select()

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
