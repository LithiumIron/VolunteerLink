package com.example.volunteerlink.organisation.create.model

/** Values match volunteer_posts.category in Supabase. */
enum class VolunteerPostCategory(
    val displayName: String,
    val databaseValue: String
) {
    SPORTS("Sports", "SPORTS"),
    COMMUNITY("Community", "COMMUNITY"),
    EDUCATION("Education", "EDUCATION"),
    ENVIRONMENT("Environment", "ENVIRONMENT"),
    HEALTH("Health", "HEALTH"),
    ANIMALS("Animals", "ANIMALS"),
    ARTS("Arts", "ARTS")
}
