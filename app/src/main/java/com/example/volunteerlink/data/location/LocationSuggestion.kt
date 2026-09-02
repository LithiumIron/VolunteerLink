package com.example.volunteerlink.data.location

/** One structured location returned by Geoapify autocomplete. */
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

    fun asGeneralArea(): LocationSuggestion {
        val generalName = generalAreaName
        return copy(
            name = generalName,
            address = generalName
        )
    }
}
