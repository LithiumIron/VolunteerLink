package com.example.volunteerlink.organisation.repository

// FILE OVERVIEW:
/*
 * ImpactWeaveRepository defines or implements data access used by the organisation Impact Weave and partnership flow.
 * Repository code keeps Supabase/RPC/storage details away from the composables and ViewModels
 * so UI code can work with application models instead of backend-specific responses.
 */


import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveActivePlan
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMatchingInput
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePostPrefill

/**
 * Holds the values represented by started impact weave matching result as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
data class StartedImpactWeaveMatchingResult(
    val draftId: String
)

/** Database access used by the current Impact Weave planning + matching flow. */
interface ImpactWeaveRepository {
    /** Returns persisted active plans that may be reopened to view matching results. */
    suspend fun loadActivePlans(): List<ImpactWeaveActivePlan>

    /** Atomically persists Step 1 + Step 2 and starts the plan in MATCHING. */
    suspend fun startMatching(draft: ImpactWeaveDraft): StartedImpactWeaveMatchingResult

    /** Returns real partner support rows that Groq is allowed to rank. */
    suspend fun loadMatchingInput(draftId: String): ImpactWeaveMatchingInput

    /**
     * Updates the basic details used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
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
    suspend fun dispose(draftId: String)

    /**
     * Loads the post prefill needed by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    suspend fun loadPostPrefill(draftId: String): ImpactWeavePostPrefill

    /**
     * Confirms the conversion in the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    suspend fun completeConversion(draftId: String, postId: String)
}
