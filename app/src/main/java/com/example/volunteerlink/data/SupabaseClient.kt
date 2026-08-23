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
    install(Auth)

    install(Postgrest) {
        defaultSchema = "v1_erd_test"
    }

    install(Storage)
}

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
