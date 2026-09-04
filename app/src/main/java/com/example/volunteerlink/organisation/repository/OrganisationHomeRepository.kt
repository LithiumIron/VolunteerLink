package com.example.volunteerlink.organisation.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines the Home/Manage read contract used by Organisation dashboard ViewModels.
//
// The contract returns domain models rather than PostgREST responses, keeping table names and decoding logic out
// of Compose.
//
// It also exposes application lifecycle resolution so stale PENDING rows can be reconciled before counts are
// displayed.
//
// Architectural layer: Data/repository layer.
// ============================================================================


import com.example.volunteerlink.organisation.home.model.OrganisationHomeSnapshot

/** Data source used by the Organisation Home ViewModel. */
/**
 * DETAILED DECLARATION — OrganisationHomeRepository
 *
 * Contract for Organisation Home Repository. Callers depend on this abstraction rather than a concrete Supabase
 * implementation.
 *
 * Implementations may perform network/storage work, while ViewModels and Compose remain expressed in
 * VolunteerLink domain types.
 */
interface OrganisationHomeRepository {
    /**
     * Loads the home snapshot needed by the organisation Home dashboard flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadHomeSnapshot
     *
     * Performs the repository/data-layer operation for load home snapshot.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadHomeSnapshot(
        organisationId: String
    ): OrganisationHomeSnapshot

    /**
     * Loads the partner posts needed by the organisation Home dashboard flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadPartnerPosts
     *
     * Performs the repository/data-layer operation for load partner posts.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadPartnerPosts(): List<PartnerPostSummary>
}

/**
 * Holds the values represented by partner post summary as one strongly typed model.
 * It keeps backend-facing work behind the Home dashboard repository boundary.
 */
/**
 * DETAILED DECLARATION — PartnerPostSummary
 *
 * Domain/UI type for Partner Post Summary used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
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
