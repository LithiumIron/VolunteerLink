package com.example.volunteerlink.organisation.create.model

/** Values match remote_details.submission_mode in Supabase. */
enum class RemoteSubmissionMode(
    val displayName: String,
    val databaseValue: String
) {
    SHARED_TEAM(
        displayName = "Shared Team Deliverable",
        databaseValue = "SHARED_TEAM"
    ),
    INDIVIDUAL(
        displayName = "Individual Deliverables",
        databaseValue = "INDIVIDUAL"
    )
}
