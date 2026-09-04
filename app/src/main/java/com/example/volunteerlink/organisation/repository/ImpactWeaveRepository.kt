package com.example.volunteerlink.organisation.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines the Organisation-side Impact Weave data operations independently from Supabase implementation details.
//
// The contract covers plan creation/matching, active plan history, semantic matching input, rescheduling,
// disposal, Create Post prefill and conversion completion.
//
// Impact Weave planning is kept separate from ordinary Volunteer Post persistence until the organisation
// explicitly converts an eligible plan into a post.
//
// Architectural layer: Data/repository layer.
// ============================================================================


import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveActivePlan
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMatchingInput
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePostPrefill

/**
 * Holds the values represented by started impact weave matching result as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — StartedImpactWeaveMatchingResult
 *
 * Domain/UI type for Started Impact Weave Matching Result used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class StartedImpactWeaveMatchingResult(
    val draftId: String
)

/** Database access used by the current Impact Weave planning + matching flow. */
/**
 * DETAILED DECLARATION — ImpactWeaveRepository
 *
 * Contract for Impact Weave Repository. Callers depend on this abstraction rather than a concrete Supabase
 * implementation.
 *
 * Implementations may perform network/storage work, while ViewModels and Compose remain expressed in
 * VolunteerLink domain types.
 */
interface ImpactWeaveRepository {
    /** Returns persisted active plans that may be reopened to view matching results. */
    /**
     * DETAILED BEHAVIOUR — loadActivePlans
     *
     * Performs the repository/data-layer operation for load active plans.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadActivePlans(): List<ImpactWeaveActivePlan>

    /** Atomically persists Step 1 + Step 2 and starts the plan in MATCHING. */
    /**
     * DETAILED BEHAVIOUR — startMatching
     *
     * Performs the repository/data-layer operation for start matching.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun startMatching(draft: ImpactWeaveDraft): StartedImpactWeaveMatchingResult

    /** Returns real partner support rows that Groq is allowed to rank. */
    /**
     * DETAILED BEHAVIOUR — loadMatchingInput
     *
     * Performs the repository/data-layer operation for load matching input.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadMatchingInput(draftId: String): ImpactWeaveMatchingInput

    /**
     * Updates the basic details used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — updateBasicDetails
     *
     * Performs the repository/data-layer operation for update basic details.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun updateBasicDetails(
        draftId: String,
        title: String,
        category: String,
        description: String
    )

    /**
     * Derives the reschedule value used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — reschedule
     *
     * Performs the repository/data-layer operation for reschedule.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun reschedule(
        draftId: String,
        startDateMillis: Long,
        endDateMillis: Long,
        startTimeMinutes: Int,
        endTimeMinutes: Int
    )

    /**
     * Derives the dispose value used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — dispose
     *
     * Performs the repository/data-layer operation for dispose.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun dispose(draftId: String)

    /**
     * Loads the post prefill needed by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadPostPrefill
     *
     * Performs the repository/data-layer operation for load post prefill.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadPostPrefill(draftId: String): ImpactWeavePostPrefill

    /**
     * Confirms the conversion in the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — completeConversion
     *
     * Performs the repository/data-layer operation for complete conversion.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun completeConversion(draftId: String, postId: String)
}
