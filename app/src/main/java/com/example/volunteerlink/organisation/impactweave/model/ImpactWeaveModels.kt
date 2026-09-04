package com.example.volunteerlink.organisation.impactweave.model

// FILE OVERVIEW:
/*
 * ImpactWeaveModels groups the data structures used by the organisation Impact Weave and partnership flow.
 * These models make state explicit and allow the UI, ViewModel and repository layers to exchange
 * strongly typed values instead of passing unrelated parameters throughout the feature.
 */


import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory

/**
 * Lists the supported values represented by impact weave mode.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
enum class ImpactWeaveMode(
    val displayName: String,
    val description: String
) {
    PHYSICAL(
        displayName = "Physical",
        description = "Partnership support for an in-person activity"
    ),
    HYBRID(
        displayName = "Hybrid",
        description = "Partnership support for the physical side of a hybrid activity"
    )
}

/**
 * Lists the supported values represented by impact weave duration.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
enum class ImpactWeaveDuration(
    val displayName: String
) {
    ONE_DAY("One day"),
    MULTIPLE_DAYS("Multiple days")
}

/**
 * Lists the supported values represented by impact weave page.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
enum class ImpactWeavePage {
    LIST,
    ACTIVITY_PLAN,
    SUPPORT_NEEDED,
    REVIEW,
    MATCH_RESULTS
}

/**
 * Holds the values represented by impact weave need draft as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeaveNeedDraft(
    val needId: Int,
    val originalText: String,
    val supportType: String,
    val resourceName: String,
    val quantityRequired: Int? = null,
    val capacityRequired: Int? = null,
    /** Accepted partnership contributions only. Pending invitations never increase this value. */
    val confirmedQuantity: Int = 0,
    /** For VENUE this means an accepted venue exists; for countable needs it means target reached. */
    val isFulfilled: Boolean = false
) {
    val amount: Int?
        get() = if (supportType == "VENUE") {
            capacityRequired
        } else {
            quantityRequired
        }
}

/**
 * Holds the values represented by impact weave draft as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeaveDraft(
    val draftId: Int,
    val databaseDraftId: String? = null,
    val persistedStatus: String? = null,
    val category: VolunteerPostCategory? = null,
    val title: String = "",
    val description: String = "",
    val mode: ImpactWeaveMode? = null,
    val duration: ImpactWeaveDuration = ImpactWeaveDuration.ONE_DAY,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    // When no exact venue exists, this is the preferred general locality.
    // When a venue exists, it is derived automatically from that venue.
    val areaQuery: String = "",
    val areaLocation: LocationSuggestion? = null,
    val hasExistingVenue: Boolean? = null,
    val venueQuery: String = "",
    val existingVenueLocation: LocationSuggestion? = null,
    val needs: List<ImpactWeaveNeedDraft> = emptyList(),
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

/** A persisted need returned by the matching input RPC. */
data class ImpactWeaveDatabaseNeed(
    val needId: String,
    val originalText: String,
    val supportType: String,
    val resourceName: String,
    val quantityRequired: Int? = null,
    val capacityRequired: Int? = null,
    /** Accepted partnership contributions only. Pending invitations do not increase this value. */
    val confirmedQuantity: Int = 0,
    /** VENUE is fulfilled only when one suitable venue has been accepted. */
    val isFulfilled: Boolean = false
) {
    val requiredAmount: Int?
        get() = if (supportType == "VENUE") capacityRequired else quantityRequired
}

/** A real support record provided by another organisation. */
data class ImpactWeaveSupportCandidate(
    val supportId: String,
    val organisationId: String,
    val organisationName: String,
    val supportDescription: String,
    val supportType: String,
    val resourceName: String,
    val quantity: Int? = null,
    val capacity: Int? = null,
    val locationName: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceKm: Double? = null
)

/**
 * Holds the values represented by impact weave matching input as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeaveMatchingInput(
    val needs: List<ImpactWeaveDatabaseNeed>,
    val candidates: List<ImpactWeaveSupportCandidate>
)

/**
 * Holds the values represented by impact weave need match result as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeaveNeedMatchResult(
    val need: ImpactWeaveDatabaseNeed,
    /** Direct matches that are actually counted toward potential coverage. */
    val directMatches: List<ImpactWeaveSupportCandidate>,
    /** Related or unsuitable-capacity options. These never increase progress. */
    val alternativeMatches: List<ImpactWeaveSupportCandidate>,
    val potentialFraction: Float,
    val potentialCoveredAmount: Int? = null,
    val usesWiderVenueArea: Boolean = false
)

/**
 * Holds the values represented by impact weave match results as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeaveMatchResults(
    val draftId: String,
    val needResults: List<ImpactWeaveNeedMatchResult>,
    val overallPotentialFraction: Float
) {
    val needsWithPotentialSupport: Int
        get() = needResults.count { it.potentialFraction > 0f }
}



/**
 * Holds the values represented by impact weave active plan as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeaveActivePlan(
    val draftId: String,
    val category: VolunteerPostCategory? = null,
    val title: String,
    val description: String = "",
    val mode: ImpactWeaveMode,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
    val hasExistingVenue: Boolean,
    val areaName: String,
    val areaLocation: LocationSuggestion,
    val status: String,
    val needsCount: Int
)


/**
 * Holds the values represented by impact weave partnership item state as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeavePartnershipItemState(
    val needId: String,
    val supportId: String?,
    val supportType: String,
    val resourceName: String,
    val providerResourceName: String?,
    val quantityProvided: Int? = null,
    val capacityProvided: Int? = null
)

/**
 * Holds the values represented by impact weave partnership state as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeavePartnershipState(
    val invitationId: String,
    val organisationId: String,
    val organisationName: String,
    val status: String,
    val revisionNumber: Int,
    val items: List<ImpactWeavePartnershipItemState> = emptyList()
)

/**
 * Holds the values represented by impact weave ui state as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeaveUiState(
    val page: ImpactWeavePage = ImpactWeavePage.LIST,
    val activePlans: List<ImpactWeaveActivePlan> = emptyList(),
    val isLoadingActivePlans: Boolean = false,
    val activePlansError: String? = null,
    val workingDraft: ImpactWeaveDraft? = null,
    val isFindingPartners: Boolean = false,
    val findPartnersError: String? = null,
    val matchResults: ImpactWeaveMatchResults? = null,
    val matchResultsError: String? = null,
    val sentPartnershipOrganisationIds: Set<String> = emptySet(),
    val partnershipStates: Map<String, ImpactWeavePartnershipState> = emptyMap(),
    val sendingPartnershipOrganisationId: String? = null,
    val partnershipRequestError: String? = null,
    val partnershipRequestSuccess: String? = null,
    val isSavingPlanChange: Boolean = false,
    val planChangeError: String? = null,
    val planChangeSuccess: String? = null
)

/**
 * Holds the values represented by impact weave post prefill as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeavePostPrefill(
    val draftId: String,
    val category: VolunteerPostCategory,
    val title: String,
    val description: String,
    val mode: ImpactWeaveMode,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
    val location: LocationSuggestion,
    val partners: List<ImpactWeavePostPartner> = emptyList()
)

/**
 * Holds the values represented by impact weave post partner as one strongly typed model.
 * It keeps related Impact Weave and partnership values together so callers do not pass disconnected fields around.
 */
data class ImpactWeavePostPartner(
    val organisationName: String,
    val contributionSummary: String
)
