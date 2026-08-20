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
    val longitude: Double
) {
    val displayName: String
        get() = name.ifBlank { address }
}
