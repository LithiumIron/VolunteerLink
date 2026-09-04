package com.example.volunteerlink.data.location

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Supports Organisation location handling through the shared Geoapify Reverse Geocoder data-layer component.
//
// The file keeps provider/device-specific location details outside Compose and exposes normalized values to the
// Organisation ViewModels.
//
// Location helpers do not publish data by themselves; selected values only become persistent when the owning
// workflow saves through its repository.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** A GPS fix matched against VolunteerLink's own countryStates dropdown data. */
/**
 * DETAILED DECLARATION — GeoapifyLocationMatch
 *
 * Domain/UI type for Geoapify Location Match used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class GeoapifyLocationMatch(
    val country: String,
    val stateRegion: String,
    val locationName: String
)

/**
 * DETAILED DECLARATION — GeoapifyReverseGeocoder
 *
 * Single shared instance for Geoapify Reverse Geocoder so related rules/state are defined once for the
 * application process.
 */
object GeoapifyReverseGeocoder {

    /**
     * Reverse-geocodes lat/lon via Geoapify, then tries to match the result
     * against [countryStates]. Returns null on any API failure OR when
     * nothing in the response matches an existing entry — callers must
     * fall back to manual selection rather than accept an unmatched value.
     */
    /**
     * DETAILED BEHAVIOUR — matchToKnownLocation
     *
     * Implements the current VolunteerLink responsibility for match to known location in this support/model
     * layer.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun matchToKnownLocation(
        apiKey: String,
        latitude: Double,
        longitude: Double,
        countryStates: Map<String, Map<String, List<String>>>
    ): GeoapifyLocationMatch? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://api.geoapify.com/v1/geocode/reverse" +
                        "?lat=$latitude&lon=$longitude&apiKey=${Uri.encode(apiKey)}"
            )

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val body = try {
                if (connection.responseCode !in 200..299) return@withContext null
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }

            val parsed = Json { ignoreUnknownKeys = true }
                .decodeFromString<GeoapifyReverseResponse>(body)

            val properties = parsed.features.firstOrNull()?.properties
                ?: return@withContext null

            matchProperties(properties, countryStates)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * DETAILED BEHAVIOUR — matchProperties
     *
     * Implements the current VolunteerLink responsibility for match properties in this support/model layer.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     */
    private fun matchProperties(
        properties: GeoapifyProperties,
        countryStates: Map<String, Map<String, List<String>>>
    ): GeoapifyLocationMatch? {
        val candidateCities = listOfNotNull(
            properties.city, properties.county, properties.suburb, properties.district
        )

        countryStates.forEach { (countryName, states) ->
            if (!countryName.equals(properties.country, ignoreCase = true)) return@forEach

            states.forEach { (stateName, locations) ->
                val stateMatches = properties.state?.let {
                    it.equals(stateName, ignoreCase = true) || it.contains(stateName, ignoreCase = true)
                } == true

                if (stateMatches) {
                    val matchedLocation = locations.firstOrNull { location ->
                        candidateCities.any { candidate -> candidate.equals(location, ignoreCase = true) }
                    }
                    if (matchedLocation != null) {
                        return GeoapifyLocationMatch(countryName, stateName, matchedLocation)
                    }
                }
            }
        }
        return null
    }
}

@Serializable
/**
 * DETAILED DECLARATION — GeoapifyReverseResponse
 *
 * Domain/UI type for Geoapify Reverse Response used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class GeoapifyReverseResponse(val features: List<GeoapifyFeature> = emptyList())

@Serializable
/**
 * DETAILED DECLARATION — GeoapifyFeature
 *
 * Domain/UI type for Geoapify Feature used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class GeoapifyFeature(val properties: GeoapifyProperties)

@Serializable
/**
 * DETAILED DECLARATION — GeoapifyProperties
 *
 * Domain/UI type for Geoapify Properties used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class GeoapifyProperties(
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val county: String? = null,
    val suburb: String? = null,
    val district: String? = null
)