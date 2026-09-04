package com.example.volunteerlink.organisation.repository

// FILE OVERVIEW:
/*
 * OrganisationHomeRepository defines or implements data access used by the organisation Home dashboard flow.
 * Repository code keeps Supabase/RPC/storage details away from the composables and ViewModels
 * so UI code can work with application models instead of backend-specific responses.
 */


import com.example.volunteerlink.organisation.home.model.OrganisationHomeSnapshot

/** Data source used by the Organisation Home ViewModel. */
interface OrganisationHomeRepository {
    /**
     * Loads the home snapshot needed by the organisation Home dashboard flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    suspend fun loadHomeSnapshot(
        organisationId: String
    ): OrganisationHomeSnapshot

    /**
     * Loads the partner posts needed by the organisation Home dashboard flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    suspend fun loadPartnerPosts(): List<PartnerPostSummary>
}

/**
 * Holds the values represented by partner post summary as one strongly typed model.
 * It keeps backend-facing work behind the Home dashboard repository boundary.
 */
data class PartnerPostSummary(
    val postId: String,
    val ownerOrganisationName: String,
    val title: String,
    val description: String,
    val mode: String,
    val status: String,
    val startDate: String?,
    val endDate: String?,
    val locationName: String?,
    val contributionSummary: String,
    val isOwner: Boolean = false
)
