package com.example.volunteerlink.data.location

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines the normalized location result shared between Geoapify data code and Organisation location UI.
//
// The model carries the user-facing label plus address/locality/country/coordinate fields required to save a
// Physical post or Impact Weave location.
//
// Using one model avoids coupling CreatePostViewModel to the exact JSON shape returned by the external geocoding
// service.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import kotlinx.serialization.Serializable

/** One structured location returned by Geoapify autocomplete. */
@Serializable
/**
 * DETAILED DECLARATION — LocationSuggestion
 *
 * Domain/UI type for Location Suggestion used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class LocationSuggestion(
    val placeId: String,
    val name: String,
    val address: String,
    val city: String?,
    val state: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val suburb: String? = null,
    val district: String? = null,
    val county: String? = null,
    val resultType: String? = null
) {
    val displayName: String
        get() = name.ifBlank { address }

    /**
     * Builds a broad locality label without carrying a building or venue name.
     * Examples: Setapak, Kuala Lumpur, Malaysia / Petaling Jaya, Selangor, Malaysia.
     */
    val generalAreaName: String
        get() {
            val locality = suburb
                ?.takeIf { it.isNotBlank() }
                ?: district?.takeIf { it.isNotBlank() }
                ?: city?.takeIf { it.isNotBlank() }
                ?: county?.takeIf { it.isNotBlank() }

            return listOfNotNull(
                locality,
                state?.takeIf { it.isNotBlank() },
                country?.takeIf { it.isNotBlank() }
            )
                .distinctBy { it.lowercase() }
                .joinToString(", ")
                .ifBlank { displayName }
        }

    /**
     * DETAILED BEHAVIOUR — asGeneralArea
     *
     * Implements the current VolunteerLink responsibility for as general area in this support/model layer.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     */
    fun asGeneralArea(): LocationSuggestion {
        val generalName = generalAreaName
        return copy(
            name = generalName,
            address = generalName
        )
    }
}
