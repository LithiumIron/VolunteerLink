package com.example.volunteerlink.organisation.impactweave.model

import com.example.volunteerlink.data.location.LocationSuggestion

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

enum class ImpactWeaveDuration(
    val displayName: String
) {
    ONE_DAY("One day"),
    MULTIPLE_DAYS("Multiple days")
}

enum class ImpactWeavePage {
    LIST,
    ACTIVITY_PLAN,
    SUPPORT_NEEDED,
    REVIEW
}

data class ImpactWeaveNeedDraft(
    val needId: Int,
    val originalText: String,
    val supportType: String,
    val resourceName: String,
    val quantityRequired: Int? = null,
    val capacityRequired: Int? = null
) {
    val amount: Int?
        get() = if (supportType == "VENUE") {
            capacityRequired
        } else {
            quantityRequired
        }
}

data class ImpactWeaveDraft(
    val draftId: Int,
    val title: String = "",
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

data class ImpactWeaveUiState(
    val page: ImpactWeavePage = ImpactWeavePage.LIST,
    val drafts: List<ImpactWeaveDraft> = emptyList(),
    val workingDraft: ImpactWeaveDraft? = null
)
