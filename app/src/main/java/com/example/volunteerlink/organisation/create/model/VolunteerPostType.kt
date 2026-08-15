package com.example.volunteerlink.organisation.create.model

/** Values match volunteer_posts.mode in Supabase. */
enum class VolunteerPostType(
    val displayName: String,
    val shortDescription: String,
    val databaseValue: String
) {
    PHYSICAL(
        displayName = "Physical Event",
        shortDescription = "At a physical location",
        databaseValue = "PHYSICAL"
    ),
    REMOTE(
        displayName = "Remote Project",
        shortDescription = "Completed remotely",
        databaseValue = "REMOTE"
    ),
    HYBRID(
        displayName = "Hybrid",
        shortDescription = "Physical and remote",
        databaseValue = "HYBRID"
    )
}
