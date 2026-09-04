package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveActivePlan
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMatchingInput
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePostPrefill

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

    suspend fun updateBasicDetails(
        draftId: String,
        title: String,
        category: String,
        description: String
    )

    suspend fun reschedule(
        draftId: String,
        startDateMillis: Long,
        endDateMillis: Long,
        startTimeMinutes: Int,
        endTimeMinutes: Int
    )

    suspend fun dispose(draftId: String)

    suspend fun loadPostPrefill(draftId: String): ImpactWeavePostPrefill

    suspend fun completeConversion(draftId: String, postId: String)
}
